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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LASReaderTest {

    private static final String LAZ_NAME = "26690_12570.laz";
    private static final String LAZ_BASE_URL = "http://maps.zh.ch/download/hoehen/2014/lidar";
    private static final int LAZ_NUM_POINT_RECORDS = 265401;

    private static final String LAS_NAME = "2312.las";
    private static final String LAS_BASE_URL = "https://dc-lidar-2018.s3.amazonaws.com/Classified_LAS";
    private static final int LAS_NUM_POINT_RECORDS = 1653361;

    private File target = new File("target");
    private File laz = new File(target, LAZ_NAME);
    private File las = new File(target, LAS_NAME);

    @Before
    public void before() throws Exception {
        if (!laz.exists()) {
            URI url = new URI(LAZ_BASE_URL + "/" + LAZ_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = new FileOutputStream(laz)) {
                        response.getEntity().writeTo(out);
                    }
                }
            }
        }
        if (!las.exists()) {
            URI url = new URI(LAS_BASE_URL + "/" + LAS_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = new FileOutputStream(las)) {
                        response.getEntity().writeTo(out);
                    }
                }
            }
        }
    }

    @Test
    public void read() {
        LASReader reader = new LASReader(laz);
        LASHeader header = reader.getHeader();

        long[] classifications = new long[Byte.MAX_VALUE];
        assertEquals("las_reframe.exe", header.getGeneratingSoftware());
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            classifications[p.getClassification()]++;
            numPoints++;
        }
        assertEquals(LAZ_NUM_POINT_RECORDS, header.getLegacyNumberOfPointRecords());
        assertEquals(LAZ_NUM_POINT_RECORDS, numPoints);
        assertEquals(0, classifications[1]);        // unclassified
        assertEquals(49355, classifications[2]);    // ground
        assertEquals(3895, classifications[3]);     // low vegetation
        assertEquals(11792, classifications[4]);    // medium vegetation
        assertEquals(141722, classifications[5]);   // high vegetation
        assertEquals(58609, classifications[12]);   // overlap/reserved
    }

    @Test
    public void readLAS() {
        LASReader reader = new LASReader(las);
        LASHeader header = reader.getHeader();

        long[] classifications = new long[Byte.MAX_VALUE];
        assertEquals("LASF", header.getFileSignature());
        assertEquals(7, header.getFileSourceID());
        assertEquals(17, header.getGlobalEncoding());
        assertEquals("ALS80", header.getSystemIdentifier());
        assertEquals("TerraScan", header.getGeneratingSoftware());
        assertEquals(375, header.getHeaderSize());
        assertEquals(1555, header.getOffsetToPointData());
        assertEquals(3, header.getNumberOfVariableLengthRecords());
        assertEquals(6, header.getPointDataRecordFormat());
        assertEquals(30, header.getPointDataRecordLength());
        assertArrayEquals(new long[]{1575188, 72407, 5218, 460, 88, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                header.getNumberOfPointsByReturn());
        assertEquals(0.01, header.getXScaleFactor(), 0.00001);
        assertEquals(0.01, header.getYScaleFactor(), 0.00001);
        assertEquals(0.01, header.getZScaleFactor(), 0.00001);
        assertEquals(399000.00, header.getMinX(), 0.00001);
        assertEquals(133000.00, header.getMinY(), 0.00001);
        assertEquals(-19.30, header.getMinZ(), 0.00001);
        assertEquals(399799.99, header.getMaxX(), 0.00001);
        assertEquals(133799.99, header.getMaxY(), 0.00001);
        assertEquals(45.41, header.getMaxZ(), 0.00001);
        assertEquals(1, header.getVersionMajor());
        assertEquals(4, header.getVersionMinor());
        assertEquals(267, header.getFileCreationDayOfYear());
        assertEquals(2018, header.getFileCreationYear());

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        char minIntensity = Character.MAX_VALUE;
        char maxIntensity = Character.MIN_VALUE;
        double maxGpsTime = Double.MIN_VALUE;
        double minGpsTime = Double.MAX_VALUE;
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            classifications[p.getClassification()]++;
            minX = Math.min(minX, p.getX());
            maxX = Math.max(maxX, p.getX());
            minY = Math.min(minY, p.getY());
            maxY = Math.max(maxY, p.getY());
            minZ = Math.min(minZ, p.getZ());
            maxZ = Math.max(maxZ, p.getZ());
            minIntensity = (char) Math.min(minIntensity, p.getIntensity());
            maxIntensity = (char) Math.max(maxIntensity, p.getIntensity());
            maxGpsTime = Math.max(maxGpsTime, p.getGPSTime());
            minGpsTime = Math.min(minGpsTime, p.getGPSTime());
            numPoints++;
        }
        assertEquals(LAS_NUM_POINT_RECORDS, header.getNumberOfPointRecords());
        assertEquals(LAS_NUM_POINT_RECORDS, numPoints);
        assertEquals(110571, classifications[1]);       // unclassified
        assertEquals(867935, classifications[2]);       // ground
        assertEquals(44095, classifications[3]);        // low vegetation
        assertEquals(26831, classifications[4]);        // medium vegetation
        assertEquals(56595, classifications[5]);        // high vegetation
        assertEquals(190318, classifications[6]);       // building
        assertEquals(14373, classifications[7]);        // noise
        assertEquals(296748, classifications[9]);       // water
        assertEquals(44199, classifications[17]);       // bridge deck
        assertEquals(1696, classifications[20]);        // reserved
        assertEquals(39900000, minX);
        assertEquals(39979999, maxX);
        assertEquals(13300000, minY);
        assertEquals(13379999, maxY);
        assertEquals(-1930, minZ);
        assertEquals(4541, maxZ);
        assertEquals(0, minIntensity);
        assertEquals(65535, maxIntensity);
        assertEquals(207010949.553468, minGpsTime, 0.000001);
        assertEquals(207011954.826499, maxGpsTime, 0.000001);
    }

    @Test
    public void insideTile() {
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
    public void insideRectangle() {
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
    public void insideCircle() {
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
