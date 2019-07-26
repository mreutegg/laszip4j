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
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

public class ByteStreamInFile extends ByteStreamInDataInput {

    private final RandomAccessFile file;

    private final RandomAccessDataInput in;

    public ByteStreamInFile(RandomAccessFile file) {
        super(createRandomAccessDataInput(file));
        this.file = file;
        this.in = (RandomAccessDataInput) super.dataIn;
    }

    private static RandomAccessDataInput createRandomAccessDataInput(RandomAccessFile file) {
        long length;
        try {
            length = file.length();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (length > Integer.MAX_VALUE) {
            return new RandomAccessFileDataInput(file);
        } else {
            return new MMappedDataInput(file);
        }
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public long tell() {
        return in.position();
    }

    @Override
    public boolean seek(long position) {
        in.position(position);
        return true;
    }

    @Override
    public boolean seekEnd(long distance) {

        try {
            long len = file.length();
            if ((0 <= distance) && (distance <= len))
            {
                in.position(len - distance);
                return true;
            }
        } catch (IOException e) {
            // ignore and return false
        }
        return false;
    }

    @Override
    public void close() throws IOException {
        file.close();
    }

    private static abstract class RandomAccessDataInput implements DataInput {

        @Override
        public void readFully(byte[] b) throws IOException {
            readFully(b, 0, b.length);
        }

        @Override
        public boolean readBoolean() throws IOException {
            return readByte() != 0;
        }

        @Override
        public float readFloat() {
            throw new UnsupportedOperationException();
        }

        @Override
        public double readDouble() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String readLine() {
            throw new UnsupportedOperationException();
        }

        @Override
        public String readUTF() {
            throw new UnsupportedOperationException();
        }

        @Override
        public short readShort() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int readUnsignedShort() {
            throw new UnsupportedOperationException();
        }

        @Override
        public int readUnsignedByte() throws IOException {
            return Byte.toUnsignedInt(readByte());
        }

        @Override
        public char readChar() throws IOException {
            int ch1 = this.readUnsignedByte();
            int ch2 = this.readUnsignedByte();
            return (char)((ch1 << 8) + ch2);
        }

        @Override
        public int readInt() throws IOException {
            int ch1 = this.readUnsignedByte();
            int ch2 = this.readUnsignedByte();
            int ch3 = this.readUnsignedByte();
            int ch4 = this.readUnsignedByte();
            return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + ch4);
        }

        @Override
        public long readLong() throws IOException {
            return ((long)(readInt()) << 32) + (readInt() & 0xFFFFFFFFL);
        }

        abstract long position();

        abstract void position(long position);
    }

    private static class MMappedDataInput extends RandomAccessDataInput {

        private final MappedByteBuffer buffer;

        MMappedDataInput (RandomAccessFile file) {
            try {
                this.buffer = file.getChannel().map(READ_ONLY, 0, file.length());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void readFully(byte[] b, int off, int len) {
            buffer.get(b, off, len);
        }

        @Override
        public int skipBytes(int n) {
            int skip = Math.min(buffer.remaining(), n);
            buffer.position(buffer.position() + skip);
            return skip;
        }

        @Override
        public byte readByte() {
            return buffer.get();
        }

        public long position() {
            return buffer.position();
        }

        public void position(long position) {
            if (position > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("position > " + Integer.MAX_VALUE + ": " + position);
            }
            buffer.position((int) position);
        }
    }

    private static class RandomAccessFileDataInput extends RandomAccessDataInput {

        private final RandomAccessFile file;

        RandomAccessFileDataInput(RandomAccessFile file) {
            this.file = file;
        }

        @Override
        public void readFully(byte[] b, int off, int len) throws IOException {
            file.readFully(b, off, len);
        }

        @Override
        public int skipBytes(int n) throws IOException {
            long pos = file.getFilePointer();
            long remaining = file.length() - pos;
            long skip = Math.min(remaining, n);
            position(pos + skip);
            return (int) skip;
        }

        @Override
        public byte readByte() throws IOException {
            return file.readByte();
        }

        @Override
        public long position() {
            try {
                return file.getFilePointer();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public void position(long position) {
            try {
                file.seek(position);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
