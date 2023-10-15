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
import java.util.ArrayList;
import java.util.List;

import static java.nio.channels.FileChannel.MapMode.READ_ONLY;

public class ByteStreamInFile extends ByteStreamInDataInput {

    private static final int MMAP_BUFFER_SIZE = Integer.getInteger("laszip4j.mmap.buffer.size", Integer.MAX_VALUE);

    private final RandomAccessFile file;

    private final RandomAccessDataInput in;

    public ByteStreamInFile(RandomAccessFile file) {
        super(createRandomAccessDataInput(file));
        this.file = file;
        this.in = (RandomAccessDataInput) super.dataIn;
    }

    private static RandomAccessDataInput createRandomAccessDataInput(RandomAccessFile file) {
        try {
            long length = file.length();
            if (length > MMAP_BUFFER_SIZE) {
                return new MultiMMappedDataInput(file, MMAP_BUFFER_SIZE);
            } else {
                return new MMappedDataInput(file);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    static class MultiMMappedDataInput extends RandomAccessDataInput {

        private final List<MappedByteBuffer> buffers = new ArrayList<>();

        private final byte[] one_byte = new byte[1];

        private final long length;

        private final int bufferSize;

        private int currentBufferIndex = 0;

        MultiMMappedDataInput(RandomAccessFile file, int bufferSize)
                throws IOException {
            this.bufferSize = bufferSize;
            this.length = file.length();
            long offset = 0;
            long remainingLength = length;
            while (remainingLength > 0) {
                long size = Math.min(remainingLength, bufferSize);
                buffers.add(file.getChannel().map(READ_ONLY, offset, size));
                offset += size;
                remainingLength -= size;
            }
        }

        @Override
        public void readFully(byte[] b, int off, int len) {
            while (len > 0) {
                MappedByteBuffer buffer = maybeTransitionToNextBuffer();
                int remainingInBuffer = Math.min(buffer.remaining(), len);
                buffer.get(b, off, remainingInBuffer);
                len -= remainingInBuffer;
                off += remainingInBuffer;
            }
        }

        @Override
        public int skipBytes(int n) {
            long remaining = length - position();
            int skip = (int) Math.min(n, remaining);
            position(position() + skip);
            return skip;
        }

        @Override
        public byte readByte() {
            readFully(one_byte, 0, 1);
            return one_byte[0];
        }

        public long position() {
            return getCurrentBuffer().position() + (long) currentBufferIndex * bufferSize;
        }

        public void position(long position) {
            if (position > length) {
                throw new IllegalArgumentException("position > " + length + ": " + position);
            }
            currentBufferIndex = (int) (position / bufferSize);
            getCurrentBuffer().position((int) (position % bufferSize));
        }

        private MappedByteBuffer maybeTransitionToNextBuffer()
                throws UncheckedEOFException {
            MappedByteBuffer current = getCurrentBuffer();
            if (current.remaining() > 0) {
                return current;
            }
            if (currentBufferIndex + 1 < buffers.size()) {
                currentBufferIndex++;
            } else {
                throw new UncheckedEOFException();
            }
            current = getCurrentBuffer();
            current.position(0);
            return current;
        }

        private MappedByteBuffer getCurrentBuffer() {
            return buffers.get(currentBufferIndex);
        }
    }
}
