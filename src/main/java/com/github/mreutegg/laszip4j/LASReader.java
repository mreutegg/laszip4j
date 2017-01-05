/*
 * Copyright 2017 Marcel Reutegger
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
package com.github.mreutegg.laszip4j;

import com.github.mreutegg.laszip4j.laslib.LASreadOpener;
import com.github.mreutegg.laszip4j.laslib.LASreader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A utility for reading LAS/LAZ files.
 */
public final class LASReader {

    private final File file;

    /**
     * Constructs a new reader for the given file. The file may refer to a raw
     * LAS or compressed LAZ file.
     *
     * @param file the file to read from.
     */
    public LASReader(File file) {
        this.file = file;
    }

    /**
     * @return the LAS points.
     */
    public Iterable<LASPoint> getPoints() {
        return LASPointIterator::new;
    }

    /**
     * @return the LAS header.
     */
    public LASHeader getHeader() {
        try (LASreader r = openReader()) {
            return new LASHeader(r.header);
        }
    }

    //--------------------------------< internal >-----------------------------

    private LASreader openReader() {
        if (!file.exists() || !file.isFile()) {
            throw new UncheckedIOException(
                    new FileNotFoundException(file.getAbsolutePath()));
        }
        return new LASreadOpener().open(file.getAbsolutePath());
    }

    private class LASPointIterator implements Iterator<LASPoint> {

        private final LASreader r = openReader();
        private LASPoint next = readNext();

        @Override
        public boolean hasNext() {
            return next != null;
        }

        @Override
        public LASPoint next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            LASPoint p = next;
            next = readNext();
            return p;
        }

        private LASPoint readNext() {
            LASPoint p = r.read_point() ? new LASPoint(r.point) : null;
            if (p == null) {
                r.close();
            }
            return p;
        }
    }
}
