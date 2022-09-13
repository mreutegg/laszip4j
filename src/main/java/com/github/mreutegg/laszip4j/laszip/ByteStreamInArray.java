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

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.BIG_ENDIAN;
import static java.nio.ByteOrder.LITTLE_ENDIAN;

public class ByteStreamInArray extends ByteStreamIn {

    private ByteBuffer data;

    public ByteStreamInArray()
    {
        data = null;
    }
    
    public ByteStreamInArray(byte[] bytes, long size) {
        super();

        init(bytes,size);
    }

    public boolean init(byte[] bytes, long size)
    {
        if (size > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }

        if ( null != bytes )
        {
            this.data = ByteBuffer.wrap(bytes, 0, (int) size);
            this.data.order(LITTLE_ENDIAN);
        }
        else
        {
            this.data = null;
        }

        return true;
    }
    
    @Override
    public byte getByte() {
        if (!data.hasRemaining()) {
            throw new UncheckedEOFException();
        }
        return data.get();
    }

    @Override
    public void getBytes(byte[] bytes, int u_num_bytes) {
        if (data.remaining() < u_num_bytes) {
            throw new UncheckedEOFException();
        }
        data.get(bytes, 0, u_num_bytes);
    }

    @Override
    public char get16bitsLE() {
        return data.getChar();
    }

    @Override
    public int get32bitsLE() {
        return data.getInt();
    }

    @Override
    public long get64bitsLE() {
        return data.getLong();
    }

    @Override
    public char get16bitsBE() {
        data.order(BIG_ENDIAN);
        try {
            return data.getChar();
        } finally {
            data.order(LITTLE_ENDIAN);
        }
    }

    @Override
    public int get32bitsBE() {
        data.order(BIG_ENDIAN);
        try {
            return data.getInt();
        } finally {
            data.order(LITTLE_ENDIAN);
        }
    }

    @Override
    public long get64bitsBE() {
        data.order(BIG_ENDIAN);
        try {
            return data.getLong();
        } finally {
            data.order(LITTLE_ENDIAN);
        }
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public long tell() {
        return data.position();
    }

    @Override
    public boolean seek(long position) {
        if (position > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        data.position((int) position);
        return true;
    }

    @Override
    public boolean seekEnd(long distance) {
        if (distance > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        if ((0 <= distance) && (distance <= data.capacity()))
        {
            data.position(data.capacity() - (int) distance);
            return true;
        }
        return false;
    }

    @Override
    public void close() {
    }
}
