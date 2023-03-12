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

import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

abstract class ByteStreamOutDataOutput extends ByteStreamOut {

    private final ByteBuffer buffer;
    protected DataOutput dataOut;

    ByteStreamOutDataOutput(DataOutput out) {
        this.dataOut = out;
        this.buffer  = ByteBuffer.allocate(8);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }

    @Override
    public boolean putByte(byte b) {
        try {
            dataOut.writeByte(b);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean putBytes(byte[] bytes, int u_num_bytes) {
        try {
            dataOut.write(bytes, 0, u_num_bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean put16bitsLE(char bytes) {
        bufferLE().putChar(bytes);
        return putBytes(buffer.array(), 2);
    }

    public boolean put16bitsLE(short bytes) {
        bufferLE().putShort(bytes);
        return putBytes(buffer.array(), 2);
    }

    @Override
    public boolean put32bitsLE(int bytes) {
        bufferLE().putInt(bytes);
        return putBytes(buffer.array(), 4);
    }

    @Override
    public boolean put64bitsLE(long bytes) {
        bufferLE().putLong(bytes);
        return putBytes(buffer.array(), 8);
    }

    @Override
    boolean put16bitsBE(char bytes) {
        try {
            dataOut.writeShort(bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    boolean put32bitsBE(int bytes) {
        try {
            dataOut.writeInt(bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    boolean put64bitsBE(long bytes) {
        try {
            dataOut.writeLong(bytes);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private ByteBuffer bufferLE() {
        buffer.rewind();
        return buffer;
    }
}
