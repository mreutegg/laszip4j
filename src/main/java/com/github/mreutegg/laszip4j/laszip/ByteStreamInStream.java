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

import java.io.DataInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ByteStreamInStream extends ByteStreamInDataInput {

    private final CountingInputStream cIn;

    public ByteStreamInStream(InputStream in) {
        super(null);
        this.cIn = new CountingInputStream(in);
        this.dataIn = new DataInputStream(cIn);
    }

    @Override
    public boolean isSeekable() {
        return false;
    }

    @Override
    public long tell() {
        return cIn.getCount();
    }

    @Override
    public boolean seek(long position) {
        return false;
    }

    @Override
    public boolean seekEnd(long distance) {
        return false;
    }

    @Override
    public void close() throws IOException {
        cIn.close();
    }

    private static class CountingInputStream extends FilterInputStream {

        private long count = 0;

        CountingInputStream(InputStream in) {
            super(in);
        }

        long getCount() {
            return count;
        }

        @Override
        public int read() throws IOException {
            int c = super.read();
            if (c != -1) {
                count++;
            }
            return c;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int num = super.read(b, off, len);
            if (num != -1) {
                count += num;
            }
            return num;
        }
    }
}
