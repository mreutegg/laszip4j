/*
 * Copyright 2023 Marcel Reutegger
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

import com.github.mreutegg.laszip4j.laslib.LASreader;
import com.github.mreutegg.laszip4j.laslib.LASwriteOpener;
import com.github.mreutegg.laszip4j.laslib.LASwriter;

import java.io.File;

/**
 * Utility for writing a LAS file.
 */
public final class LASWriter {

    private final LASReader reader;

    /**
     * Create a new writer that will read point data from the given reader.
     *
     * @param reader the source of the points to write.
     */
    public LASWriter(LASReader reader) {
        this.reader = reader;
    }

    /**
     * Write the points to the given output file.
     *
     * @param out the output file.
     */
    public void write(File out) {
        try (LASreader r = reader.openReader()) {
            LASwriteOpener opener = new LASwriteOpener();
            opener.set_file_name(out.getAbsolutePath());
            LASwriter w = opener.open(r.header);
            try {
                while (r.read_point()) {
                    w.write_point(r.point);
                }
            } finally {
                w.close();
            }
        }
    }
}
