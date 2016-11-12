/*
 * Copyright 2007-2012, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.io.Closeable;

public abstract class ByteStreamIn implements Closeable {

    private long u_bit_buffer;
    private int u_num_buffer;

    /* write single bits                                         */
    protected final int getBits(int u_num_bits)
    {
        if (u_num_buffer < u_num_bits)
        {
            int u_input_bits = get32bitsLE();
            u_bit_buffer = u_bit_buffer | ((Integer.toUnsignedLong(u_input_bits)) << u_num_buffer);
            u_num_buffer = u_num_buffer + 32;
        }
        int u_new_bits = (int)(u_bit_buffer & ((1 << u_num_bits) - 1));
        u_bit_buffer = u_bit_buffer >>> u_num_bits;
        u_num_buffer = u_num_buffer - u_num_bits;
        return u_new_bits;
    }

    /* read a single byte                                        */
    public abstract byte getByte();
    /* read an array of bytes                                    */
    public abstract void getBytes(byte[] bytes, int u_num_bytes);
    /* read 16 bit low-endian field                              */
    public abstract char get16bitsLE();
    /* read 32 bit low-endian field                              */
    public abstract int get32bitsLE();
    /* read 64 bit low-endian field                              */
    public abstract long get64bitsLE();
    /* read 16 bit big-endian field                              */
    public abstract char get16bitsBE();
    /* read 32 bit big-endian field                              */
    public abstract int get32bitsBE();
    /* read 64 bit big-endian field                              */
    public abstract long get64bitsBE();
    /* is the stream seekable (e.g. stdin is not)                */
    public abstract boolean isSeekable();
    /* get current position of stream                            */
    public abstract long tell();
    /* seek to this position in the stream                       */
    public abstract boolean seek(long position);
    /* seek to the end of the file                               */
    public abstract boolean seekEnd(long distance);
    public boolean seekEnd() {return seekEnd(0);}
    /* constructor                                               */
    public ByteStreamIn() { u_bit_buffer = 0; u_num_buffer = 0; };

}
