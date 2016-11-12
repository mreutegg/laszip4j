/*
 * Copyright 2007-2013, martin isenburg, rapidlasso - fast tools to catch reality
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

public abstract class ByteStreamOut implements Closeable {

    private long u_bit_buffer;
    private int u_num_buffer;


    /* write single bits                                         */
    protected final boolean putBits(int u_bits, int u_num_bits)
    {
        long u_new_bits = Integer.toUnsignedLong(u_bits);
        u_bit_buffer |= (u_new_bits << u_num_buffer);
        u_num_buffer += u_num_bits;
        if (Integer.compareUnsigned(u_num_buffer, 32) >= 0)
        {
            int u_output_bits = (int) u_bit_buffer;
            u_bit_buffer = u_bit_buffer >>> 32;
            u_num_buffer = u_num_buffer - 32;
            return put32bitsLE(u_output_bits);
        }
        return true;
    };
    /* called after writing bits before closing or writing bytes */
    protected final boolean flushBits()
    {
        if (Integer.compareUnsigned(u_num_buffer, 0) > 0)
        {
            int u_num_zero_bits = 32 - u_num_buffer;
            int u_output_bits = (int)(u_bit_buffer >>> u_num_zero_bits);
            u_bit_buffer = 0;
            u_num_buffer = 0;
            return put32bitsLE(u_output_bits);
        }
        return true;
    };
    /* write a single byte                                       */
    public abstract boolean putByte(byte b);
    /* write an array of bytes                                   */
    public abstract boolean putBytes(byte[] bytes, int u_num_bytes);
    /* write 16 bit low-endian field                             */
    public abstract boolean put16bitsLE(char bytes);
    /* write 32 bit low-endian field                             */
    abstract boolean put32bitsLE(int bytes);
    /* write 64 bit low-endian field                             */
    public abstract boolean put64bitsLE(long bytes);
    /* write 16 bit big-endian field                             */
    abstract boolean put16bitsBE(char bytes);
    /* write 32 bit big-endian field                             */
    abstract boolean put32bitsBE(int bytes);
    /* write 64 bit big-endian field                             */
    abstract boolean put64bitsBE(long bytes);
    /* is the stream seekable (e.g. standard out is not)         */
    public abstract boolean isSeekable();
    /* get current position of stream                            */
    public abstract long tell();
    /* seek to this position in the stream                       */
    public abstract boolean seek(long position);
    /* seek to the end of the file                               */
    public abstract boolean seekEnd();
    /* constructor                                               */
    protected ByteStreamOut() { u_bit_buffer = 0; u_num_buffer = 0; };
}
