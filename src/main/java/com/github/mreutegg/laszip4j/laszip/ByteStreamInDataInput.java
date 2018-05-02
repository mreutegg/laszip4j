/*
 * Copyright 2016 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.io.DataInput;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

public abstract class ByteStreamInDataInput extends ByteStreamIn {

    private final ByteBuffer buffer;
    protected DataInput dataIn;

    public ByteStreamInDataInput(DataInput in) {
        super();
        this.dataIn = in;
        this.buffer = ByteBuffer.allocate(8);
        this.buffer.order(LITTLE_ENDIAN);
    }

    @Override
    public byte getByte() {
        try {
            return dataIn.readByte();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void getBytes(byte[] bytes, int u_num_bytes) {
        try {
            dataIn.readFully(bytes, 0, u_num_bytes);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public char get16bitsLE() {
        try {
            dataIn.readFully(buffer.array(), 0, 2);
            return bufferLE().getChar();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int get32bitsLE() {
        try {
            dataIn.readFully(buffer.array(), 0, 4);
            return bufferLE().getInt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public long get64bitsLE() {
        try {
            dataIn.readFully(buffer.array(), 0, 8);
            return bufferLE().getLong();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public char get16bitsBE() {
        try {
            return dataIn.readChar();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public int get32bitsBE() {
        try {
            return dataIn.readInt();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public long get64bitsBE() {
        try {
            return dataIn.readLong();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private ByteBuffer bufferLE() {
        buffer.rewind();
        return buffer;
    }
}
