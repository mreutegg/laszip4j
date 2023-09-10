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
import java.util.Arrays;

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
            int[] byReturn = r.header.number_of_points_by_return;
            Arrays.fill(byReturn, 0);
            long[] extByReturn = r.header.extended_number_of_points_by_return;
            Arrays.fill(extByReturn, 0);
            r.header.number_of_point_records = 0;
            try {
                while (r.read_point()) {
                    w.write_point(r.point);
                    short returnNumber = r.point.get_return_number();
                    returnNumber--;
                    if (returnNumber < byReturn.length) {
                        byReturn[returnNumber]++;
                    }
                    if (returnNumber < extByReturn.length) {
                        extByReturn[returnNumber]++;
                    }
                    r.header.number_of_point_records++;
                }
                w.update_header(r.header);
            } finally {
                w.close();
            }
        }
    }
}
