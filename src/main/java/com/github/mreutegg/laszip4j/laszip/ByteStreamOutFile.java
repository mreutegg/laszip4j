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

public class ByteStreamOutFile extends ByteStreamOutDataOutput {

    private RandomAccessFile file;

    public ByteStreamOutFile(RandomAccessFile file) {
        super(file);
        this.file = file;
    }

    public boolean refile(RandomAccessFile file) {
        if (file == null) {
            return false;
        }
        this.file = file;
        this.dataOut = file;
        return true;
    }

    @Override
    public boolean isSeekable() {
        return true;
    }

    @Override
    public long tell() {
        try {
            return file.getFilePointer();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public boolean seek(long position) {
        try {
            file.seek(position);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean seekEnd() {
        try {
            file.seek(file.length());
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        file.close();
    }
}
