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
import com.github.mreutegg.laszip4j.laszip.LASpoint;

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

    private Constraint constraint = new None();

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
     * Only return points that fall into the specified tile.
     * The specification of the tile refers to point data
     * adjusted with scale and offset from the LAS header.
     *
     * @return this reader.
     */
    public LASReader insideTile(float ll_x, float ll_y, float size) {
        constraint = reader -> reader.inside_tile(ll_x, ll_y, size);
        return this;
    }

    /**
     * Only return points that fall into the specified tile.
     * The specification of the circle refers to point data
     * adjusted with scale and offset from the LAS header.
     *
     * @return this reader.
     */
    public LASReader insideCircle(double center_x, double center_y, double radius) {
        constraint = reader -> reader.inside_circle(center_x, center_y, radius);
        return this;
    }

    /**
     * Only return points that fall into the specified rectangle.
     * The specification of the rectangle refers to point data
     * adjusted with scale and offset from the LAS header.
     *
     * @return this reader.
     */
    public LASReader insideRectangle(double min_x, double min_y, double max_x, double max_y) {
        constraint = reader -> reader.inside_rectangle(min_x, min_y, max_x, max_y);
        return this;
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
        LASreader reader = new LASreadOpener().open(file.getAbsolutePath());
        constraint.apply(reader);
        return reader;
    }

    private class LASPointIterator implements Iterator<LASPoint> {

        private final LASPoint end = new LASPoint(new LASpoint());

        private final LASreader r = openReader();
        private LASPoint next = null;

        @Override
        public boolean hasNext() {
            if (next == end) {
                return false;
            } else if (next != null) {
                return true;
            } else {
                next = readNext();
                return next != end;
            }
        }

        @Override
        public LASPoint next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            LASPoint p = next;
            next = null;
            return p;
        }

        private LASPoint readNext() {
            LASPoint p = r.read_point() ? new LASPoint(r.point) : null;
            if (p == null) {
                r.close();
                p = end;
            }
            return p;
        }
    }

    private interface Constraint {

        boolean apply(LASreader reader);
    }

    private final class None implements Constraint {

        @Override
        public boolean apply(LASreader reader) {
            return reader.inside_none();
        }
    }
}
