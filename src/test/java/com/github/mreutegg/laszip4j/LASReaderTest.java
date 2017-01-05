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

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.BitSet;

import static org.apache.commons.io.IOUtils.copy;
import static org.junit.Assert.assertEquals;

public class LASReaderTest {

    private static final String LAZ_NAME = "libLAS_1.2.laz";
    private static final String BASE_URL = "http://www.liblas.org/samples";
    private static final int NUM_POINT_RECORDS = 497536;

    private File target = new File("target");
    private File laz = new File(target, LAZ_NAME);

    @Before
    public void before() throws Exception {
        if (!laz.exists()) {
            URL url = new URL(BASE_URL + "/" + LAZ_NAME);
            try (InputStream in = url.openStream()) {
                try (OutputStream out = new FileOutputStream(laz)) {
                    copy(in, out);
                }
            }
        }
    }

    @Test
    public void read() throws Exception {
        LASReader reader = new LASReader(laz);
        LASHeader header = reader.getHeader();

        long[] classifications = new long[Byte.MAX_VALUE];
        assertEquals("TerraScan", header.getGeneratingSoftware());
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            classifications[p.getClassification()]++;
            numPoints++;
        }
        assertEquals(NUM_POINT_RECORDS, header.getLegacyNumberOfPointRecords());
        assertEquals(NUM_POINT_RECORDS, numPoints);
        assertEquals(19675, classifications[1]);    // unclassified
        assertEquals(402812, classifications[2]);   // ground
        assertEquals(75049, classifications[5]);    // high vegetation
    }
}
