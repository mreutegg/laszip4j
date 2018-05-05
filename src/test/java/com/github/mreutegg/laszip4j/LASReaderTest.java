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

    private static final String LAZ_NAME = "26690_12570.laz";
    private static final String BASE_URL = "http://maps.zh.ch/download/hoehen/2014/lidar";
    private static final int NUM_POINT_RECORDS = 265401;

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
        assertEquals("las_reframe.exe", header.getGeneratingSoftware());
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            classifications[p.getClassification()]++;
            numPoints++;
        }
        assertEquals(NUM_POINT_RECORDS, header.getLegacyNumberOfPointRecords());
        assertEquals(NUM_POINT_RECORDS, numPoints);
        assertEquals(0, classifications[1]);        // unclassified
        assertEquals(49355, classifications[2]);    // ground
        assertEquals(3895, classifications[3]);     // low vegetation
        assertEquals(11792, classifications[4]);    // medium vegetation
        assertEquals(141722, classifications[5]);   // high vegetation
        assertEquals(58609, classifications[12]);   // overlap/reserved
    }

    @Test
    public void insideTile() throws Exception {
        LASReader reader = new LASReader(laz)
                .insideTile(2669450, 1257400, 40);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            minX = Math.min(minX, p.getX());
            assertTrue(minX >= 266945000);
            maxX = Math.max(maxX, p.getX());
            assertTrue(maxX <= 266949000);
            minY = Math.min(minY, p.getY());
            assertTrue(minY >= 125740000);
            maxY = Math.max(maxY, p.getY());
            assertTrue(maxY <= 125744000);
            numPoints++;
        }
        assertEquals(49901, numPoints);
        assertEquals(266945817, minX);
        assertEquals(266948999, maxX);
        assertEquals(125740000, minY);
        assertEquals(125743999, maxY);
    }

    @Test
    public void insideRectangle() throws Exception {
        LASReader reader = new LASReader(laz)
                .insideRectangle(2669450, 1257390, 2669480, 1257450);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            minX = Math.min(minX, p.getX());
            assertTrue(minX >= 266945000);
            maxX = Math.max(maxX, p.getX());
            assertTrue(maxX <= 266948000);
            minY = Math.min(minY, p.getY());
            assertTrue(minY >= 125739000);
            maxY = Math.max(maxY, p.getY());
            assertTrue(maxY <= 125745000);
            numPoints++;
        }
        assertEquals(38411, numPoints);
        assertEquals(266945287, minX);
        assertEquals(266947999, maxX);
        assertEquals(125740433, minY);
        assertEquals(125744999, maxY);
    }

    @Test
    public void insideCircle() throws Exception {
        LASReader reader = new LASReader(laz)
                .insideCircle(2669465, 1257440, 20);

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
        assertEquals(53230, numPoints);
        assertEquals(266945079, minX);
        assertEquals(266948497, maxX);
        assertEquals(125742060, minY);
        assertEquals(125745999, maxY);
    }
}
