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

import java.io.DataOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ByteStreamOutOstream extends ByteStreamOutDataOutput {

    private final CountingOutputStream cOut;

    public ByteStreamOutOstream(OutputStream out) {
        super(null);
        this.cOut = new CountingOutputStream(out);
        this.dataOut = new DataOutputStream(cOut);
    }

    @Override
    public boolean isSeekable() {
        return false;
    }

    @Override
    public long tell() {
        return cOut.getCount();
    }

    @Override
    public boolean seek(long position) {
        return false;
    }

    @Override
    public boolean seekEnd() {
        return false;
    }

    @Override
    public void close() throws IOException {
        cOut.close();
    }

    private static class CountingOutputStream extends FilterOutputStream {

        private long count = 0;

        CountingOutputStream(OutputStream out) {
            super(out);
        }

        long getCount() {
            return count;
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            count++;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            super.write(b, off, len);
            count += len;
        }
    }
}
