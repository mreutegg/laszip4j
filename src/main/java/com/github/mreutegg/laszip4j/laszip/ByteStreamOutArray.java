/*
 * Copyright 2007-2015, martin isenburg, rapidlasso - fast tools to catch reality
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

public class ByteStreamOutArray extends ByteStreamOut {

    private ByteBuffer data;
    private int size;

    public ByteStreamOutArray() {
        this(1024);
    }

    public ByteStreamOutArray(long alloc) {
        if (alloc > Integer.MAX_VALUE) {
            throw new IllegalArgumentException();
        }
        this.data = ByteBuffer.allocate((int) alloc);
        this.data.order(LITTLE_ENDIAN);
        this.size = 0;
    }

    @Override
    public boolean putByte(byte b) {
        ensureCapacity(1);
        data.put(b);
        updateSize();
        return true;
    }

    @Override
    public boolean putBytes(byte[] bytes, int u_num_bytes) {
        ensureCapacity(u_num_bytes);
        data.put(bytes, 0, u_num_bytes);
        updateSize();
        return true;
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
        if ((0 <= position) && (position <= size))
        {
            data.position((int) position);
            return true;
        }
        return false;
    }

    @Override
    public boolean seekEnd() {
        data.position(size);
        return true;
    }

    @Override
    public boolean put16bitsLE(char bytes) {
        ensureCapacity(2);
        data.putChar(bytes);
        updateSize();
        return true;
    }

    @Override
    public boolean put32bitsLE(int bytes) {
        ensureCapacity(4);
        data.putInt(bytes);
        updateSize();
        return true;
    }

    @Override
    public boolean put64bitsLE(long bytes) {
        ensureCapacity(8);
        data.putLong(bytes);
        updateSize();
        return true;
    }

    @Override
    boolean put16bitsBE(char bytes) {
        ensureCapacity(2);
        data.order(BIG_ENDIAN);
        try {
            data.putChar(bytes);
        } finally {
            data.order(LITTLE_ENDIAN);
        }
        updateSize();
        return true;
    }

    @Override
    boolean put32bitsBE(int bytes) {
        ensureCapacity(4);
        data.order(BIG_ENDIAN);
        try {
            data.putInt(bytes);
        } finally {
            data.order(LITTLE_ENDIAN);
        }
        updateSize();
        return true;
    }

    @Override
    boolean put64bitsBE(long bytes) {
        ensureCapacity(8);
        data.order(BIG_ENDIAN);
        try {
            data.putLong(bytes);
        } finally {
            data.order(LITTLE_ENDIAN);
        }
        updateSize();
        return true;
    }

    @Override
    public void close() {
    }

    private void ensureCapacity(int u_num_bytes) {
        if (data.remaining() < u_num_bytes) {
            data = realloc(data, data.capacity() + Math.max(u_num_bytes, 1024));
        }
    }

    private void updateSize() {
        size = Math.max(size, data.position());
    }

    private static ByteBuffer realloc(ByteBuffer data, int u_num_bytes) {
        byte[] array = data.array();
        if (array.length < u_num_bytes) {
            byte[] newArray = new byte[u_num_bytes];
            System.arraycopy(array, 0, newArray, 0, array.length);
            array = newArray;
        }
        ByteBuffer newBuffer = ByteBuffer.wrap(array);
        newBuffer.position(data.position());
        newBuffer.order(data.order());
        return newBuffer;
    }

    /* get access to data                                        */
    protected long getSize() { return size; };
    protected ByteBuffer getData() { return data; };
    public ByteBuffer takeData() { ByteBuffer d = data; data = ByteBuffer.allocate(0).order(d.order()); size = 0; return d; };
}
