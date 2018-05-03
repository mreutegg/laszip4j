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

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URI;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LASReaderTest {

    private static final String LAZ_NAME = "libLAS_1.2.laz";
    private static final String BASE_URL = "https://web.archive.org/web/20160328153147if_/http://www.liblas.org/samples";
    private static final int NUM_POINT_RECORDS = 497536;

    private File target = new File("target");
    private File laz = new File(target, LAZ_NAME);

    @Before
    public void before() throws Exception {
        if (!laz.exists()) {
            URI url = new URI(BASE_URL + "/" + LAZ_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = new FileOutputStream(laz)) {
                        response.getEntity().writeTo(out);
                    }
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

    @Test
    public void insideTile() throws Exception {
        LASReader reader = new LASReader(laz)
                .insideTile(1442000, 378000, 1000);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            minX = Math.min(minX, p.getX());
            assertTrue(minX >= 144200000);
            maxX = Math.max(maxX, p.getX());
            assertTrue(maxX <= 144300000);
            minY = Math.min(minY, p.getY());
            assertTrue(minY >= 37800000);
            maxY = Math.max(maxY, p.getY());
            assertTrue(maxY <= 37900000);
            numPoints++;
        }
        assertEquals(16032, numPoints);
        assertEquals(144200003, minX);
        assertEquals(144299990, maxX);
        assertEquals(37800009, minY);
        assertEquals(37899987, maxY);
    }

    @Test
    public void insideRectangle() throws Exception {
        LASReader reader = new LASReader(laz)
                .insideRectangle(1441000, 376000, 1443000, 377000);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            minX = Math.min(minX, p.getX());
            assertTrue(minX >= 144100000);
            maxX = Math.max(maxX, p.getX());
            assertTrue(maxX <= 144300000);
            minY = Math.min(minY, p.getY());
            assertTrue(minY >= 37600000);
            maxY = Math.max(maxY, p.getY());
            assertTrue(maxY <= 37700000);
            numPoints++;
        }
        assertEquals(33725, numPoints);
        assertEquals(144100002, minX);
        assertEquals(144299997, maxX);
        assertEquals(37600000, minY);
        assertEquals(37699993, maxY);
    }

    @Test
    public void insideCircle() throws Exception {
        LASReader reader = new LASReader(laz)
                .insideCircle(1442000, 378000, 500);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            numPoints++;
        }
        assertEquals(11876, numPoints);
        assertEquals(144150129, minX);
        assertEquals(144249977, maxX);
        assertEquals(37750346, minY);
        assertEquals(37849678, maxY);
    }
}
