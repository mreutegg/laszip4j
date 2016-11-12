/*
 * Copyright 2005-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import static com.github.mreutegg.laszip4j.laszip.ArithmeticModel.AC_BUFFER_SIZE;
import static com.github.mreutegg.laszip4j.laszip.ArithmeticModel.AC__MaxLength;
import static com.github.mreutegg.laszip4j.laszip.ArithmeticModel.AC__MinLength;
import static com.github.mreutegg.laszip4j.laszip.ArithmeticModel.BM__LengthShift;
import static com.github.mreutegg.laszip4j.laszip.ArithmeticModel.DM__LengthShift;
import static java.lang.Integer.compareUnsigned;

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
// Fast arithmetic coding implementation                                     -
// -> 32-bit variables, 32-bit product, periodic updates, table decoding     -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
// Version 1.00  -  April 25, 2004                                           -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
//                                  WARNING                                  -
//                                 =========                                 -
//                                                                           -
// The only purpose of this program is to demonstrate the basic principles   -
// of arithmetic coding. It is provided as is, without any express or        -
// implied warranty, without even the warranty of fitness for any particular -
// purpose, or that the implementations are correct.                         -
//                                                                           -
// Permission to copy and redistribute this code is hereby granted, provided -
// that this warning and copyright notices are not removed or altered.       -
//                                                                           -
// Copyright (c) 2004 by Amir Said (said@ieee.org) &                         -
//                       William A. Pearlman (pearlw@ecse.rpi.edu)           -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
// A description of the arithmetic coding method used here is available in   -
//                                                                           -
// Lossless Compression Handbook, ed. K. Sayood                              -
// Chapter 5: Arithmetic Coding (A. Said), pp. 101-152, Academic Press, 2003 -
//                                                                           -
// A. Said, Introduction to Arithetic Coding Theory and Practice             -
// HP Labs report HPL-2004-76  -  http://www.hpl.hp.com/techreports/         -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

public class ArithmeticEncoder {

    private static final byte ZERO = 0;

    private ByteStreamOut outstream;

    private byte[] outbuffer;
    private int endbuffer;
    private int outbyte;
    private int endbyte;
    private int u_base, u_value, u_length;

    public ArithmeticEncoder() {
        outbuffer = new byte[AC_BUFFER_SIZE];
        endbuffer = outbuffer.length;
    }

    public boolean init(ByteStreamOut outstream) {
        if (outstream == null) return false;
        this.outstream = outstream;
        u_base = 0;
        u_length = AC__MaxLength;
        outbyte = 0;
        endbyte = endbuffer;
        return true;
    }

    public void done() {
        int u_init_base = u_base;                 // done encoding: set final data bytes
        boolean another_byte = true;

        if (Integer.compareUnsigned(u_length, 2 * AC__MinLength) > 0) {
            u_base += AC__MinLength;                                     // base offset
            u_length = AC__MinLength >>> 1;             // set new length for 1 more byte
        }
        else {
            u_base += AC__MinLength >>> 1;                                // base offset
            u_length = AC__MinLength >>> 9;            // set new length for 2 more bytes
            another_byte = false;
        }

        if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();                 // overflow = carry
        renorm_enc_interval();                // renormalization = output last bytes

        /* TODO doesn't make sense. is this really needed?
        if (endbyte != endbuffer)
        {
            assert(outbyte < AC_BUFFER_SIZE);
            outstream.putBytes(outbuffer + AC_BUFFER_SIZE, AC_BUFFER_SIZE);
        }
        */
        int buffer_size = outbyte;
        if (buffer_size > 0) outstream.putBytes(outbuffer, buffer_size);

        // write two or three zero bytes to be in sync with the decoder's byte reads
        outstream.putByte(ZERO);
        outstream.putByte(ZERO);
        if (another_byte) outstream.putByte(ZERO);

        outstream = null;
    }

    public ArithmeticBitModel createBitModel() {
        return new ArithmeticBitModel();
    }

    void initBitModel(ArithmeticBitModel m) {
        m.init();
    }

    ArithmeticModel createSymbolModel(int u_symbols) {
        return new ArithmeticModel(u_symbols, true);
    }

    void initSymbolModel(ArithmeticModel m) {
        initSymbolModel(m, null);
    }

    void initSymbolModel(ArithmeticModel m, int[] u_table) {
        m.init(u_table);
    }

    void encodeBit(ArithmeticBitModel m, int sym) {
        assert(m != null && (sym <= 1));

        int u_x = m.u_bit_0_prob * (u_length >>> BM__LengthShift);       // product l x p0
        // update interval
        if (sym == 0) {
            u_length = u_x;
            ++m.u_bit_0_count;
        }
        else {
            int u_init_base = u_base;
            u_base += u_x;
            u_length -= u_x;
            if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();               // overflow = carry
        }

        if (compareUnsigned(u_length, AC__MinLength) < 0) renorm_enc_interval();        // renormalization
        if (--m.u_bits_until_update == 0) m.update();       // periodic model update
    }

    void encodeSymbol(ArithmeticModel m, int u_sym) {
        assert(m != null && (compareUnsigned(u_sym, m.u_last_symbol) <= 0));

        int u_x, u_init_base = u_base;
        // compute products
        if (u_sym == m.u_last_symbol) {
            u_x = m.u_distribution[u_sym] * (u_length >>> DM__LengthShift);
            u_base += u_x;                                            // update interval
            u_length -= u_x;                                          // no product needed
        }
        else {
            u_x = m.u_distribution[u_sym] * (u_length >>>= DM__LengthShift);
            u_base += u_x;                                            // update interval
            u_length = m.u_distribution[u_sym+1] * u_length - u_x;
        }

        if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();                 // overflow = carry
        if (compareUnsigned(u_length, AC__MinLength) < 0) renorm_enc_interval();        // renormalization

        ++m.u_symbol_count[u_sym];
        if (--m.u_symbols_until_update == 0) m.update();    // periodic model update
    }

    void writeBit(int sym) {
        assert(sym < 2);

        int u_init_base = u_base;
        u_base += sym * (u_length >>>= 1);                // new interval base and length

        if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();                 // overflow = carry
        if (compareUnsigned(u_length, AC__MinLength) < 0) renorm_enc_interval();        // renormalization
    }

    void writeBits(int bits, int u_sym) {
        assert(bits != 0 && (bits <= 32) && (u_sym < (1<<bits)));

        if (bits > 19)
        {
            writeShort((short) (u_sym & 0xFFFF));
            u_sym = u_sym >>> 16;
            bits = bits - 16;
        }

        int u_init_base = u_base;
        u_base += u_sym * (u_length >>>= bits);             // new interval base and length

        if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();                 // overflow = carry
        if (compareUnsigned(u_length, AC__MinLength) < 0) renorm_enc_interval();        // renormalization
    }

    void writeByte(byte sym) {
        int u_init_base = u_base;
        u_base += (int)(sym) * (u_length >>>= 8);           // new interval base and length

        if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();                 // overflow = carry
        if (compareUnsigned(u_length, AC__MinLength) < 0) renorm_enc_interval();        // renormalization
    }

    void writeShort(short u_sym) {
        int u_init_base = u_base;
        u_base += (int)(u_sym) * (u_length >>>= 16);          // new interval base and length

        if (compareUnsigned(u_init_base, u_base) > 0) propagate_carry();                 // overflow = carry
        if (compareUnsigned(u_length, AC__MinLength) < 0) renorm_enc_interval();        // renormalization
    }

    void writeInt(int u_sym)
    {
        writeShort((short)(u_sym & 0xFFFF)); // lower 16 bits
        writeShort((short)(u_sym >>> 16));    // UPPER 16 bits
    }

    void writeFloat(float sym) /* danger in float reinterpretation */
    {
        writeInt(Float.floatToIntBits(sym));
    }

    void writeInt64(long u_sym)
    {
        writeInt((int)(u_sym & 0xFFFFFFFF)); // lower 32 bits
        writeInt((int)(u_sym >>> 32));        // UPPER 32 bits
    }

    void writeDouble(double sym) /* danger in float reinterpretation */
    {
        writeInt64(Double.doubleToLongBits(sym));
    }

    private void propagate_carry() {
        int p;
        if (outbyte == 0)
            p = endbuffer - 1;
        else
            p = outbyte - 1;
        while (outbuffer[p] == 0xFF) {
            outbuffer[p] = 0;
            if (p == 0)
                p = endbuffer - 1;
            else
                p--;
            assert(0 <= p);
            assert(p < endbuffer);
            assert(outbyte < endbuffer);
        }
        ++outbuffer[p];
    }

    private void renorm_enc_interval() {
        do {                                          // output and discard top byte
            assert(0 <= outbyte);
            assert(outbyte < endbuffer);
            assert(outbyte < endbyte);
            outbuffer[outbyte++] = (byte)(u_base >>> 24);
            if (outbyte == endbyte) manage_outbuffer();
            u_base <<= 8;
        } while (Integer.compareUnsigned((u_length <<= 8), AC__MinLength) < 0);        // length multiplied by 256
    }

    private void manage_outbuffer() {
        if (outbyte == endbuffer) outbyte = 0;
        outstream.putBytes(outbuffer, AC_BUFFER_SIZE);
        endbyte = outbyte + AC_BUFFER_SIZE;
        assert(endbyte > outbyte);
        assert(outbyte < endbuffer);
    }
}
