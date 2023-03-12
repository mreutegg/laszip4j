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

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class ByteStreamOutFile extends ByteStreamOut {

    private final RandomAccessFile file;
    private final ByteBuffer buffer = ByteBuffer.allocate(256 * 1024).order(ByteOrder.LITTLE_ENDIAN);

    public ByteStreamOutFile(RandomAccessFile file) {
        this.file = file;
    }

    @Override
    public boolean putByte(byte b) {
        try {
            ensureRemainingBufferSize(1);
            buffer.put(b);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean putBytes(byte[] bytes, int u_num_bytes) {
        try {
            if (u_num_bytes > buffer.capacity()) {
                flushBuffer();
                file.write(bytes, 0, u_num_bytes);
            } else {
                ensureRemainingBufferSize(u_num_bytes);
                buffer.put(bytes, 0, u_num_bytes);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean put16bitsLE(char bytes) {
        try {
            ensureRemainingBufferSize(Character.BYTES);
            buffer.putChar(bytes);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean put16bitsLE(short bytes) {
        try {
            ensureRemainingBufferSize(Short.BYTES);
            buffer.putShort(bytes);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean put32bitsLE(int bytes) {
        try {
            ensureRemainingBufferSize(Integer.BYTES);
            buffer.putInt(bytes);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    public boolean put64bitsLE(long bytes) {
        try {
            ensureRemainingBufferSize(Long.BYTES);
            buffer.putLong(bytes);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    @Override
    boolean put16bitsBE(char bytes) {
        try {
            ensureRemainingBufferSize(Character.BYTES);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putChar(bytes);
        } catch (IOException e) {
            return false;
        } finally {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        return true;
    }

    @Override
    boolean put32bitsBE(int bytes) {
        try {
            ensureRemainingBufferSize(Integer.BYTES);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putInt(bytes);
        } catch (IOException e) {
            return false;
        } finally {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        return true;
    }

    @Override
    boolean put64bitsBE(long bytes) {
        try {
            ensureRemainingBufferSize(Long.BYTES);
            buffer.order(ByteOrder.BIG_ENDIAN);
            buffer.putLong(bytes);
        } catch (IOException e) {
            return false;
        } finally {
            buffer.order(ByteOrder.LITTLE_ENDIAN);
        }
        return true;
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public long tell() {
        try {
            return file.getFilePointer() + buffer.position();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean seek(long position) {
        try {
            flushBuffer();
            file.seek(position);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean seekEnd() {
        try {
            flushBuffer();
            file.seek(file.length());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        flushBuffer();
        file.close();
    }

    private void ensureRemainingBufferSize(int numBytes) throws IOException {
        if (buffer.remaining() < numBytes) {
            flushBuffer();
        }
    }

    private void flushBuffer() throws IOException {
        if (buffer.position() > 0) {
            file.write(buffer.array(), 0, buffer.position());
            buffer.rewind();
        }
    }
}
