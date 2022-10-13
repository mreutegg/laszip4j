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

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    private static final String LAZ_14_NAME = "autzen-classified.copc.laz";
    private static final String LAZ_14_BASE_URL = "https://github.com/PDAL/data/raw/master/autzen";
    private static final int LAZ_14_NUM_POINT_RECORDS = 10653336;

    private final File target = new File("target");
    private final File laz = new File(target, LAZ_NAME);
    private final File las = new File(target, LAS_NAME);

    private final File laz14 = new File(target, LAZ_14_NAME);

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
        if (!laz14.exists()) {
            URI url = new URI(LAZ_14_BASE_URL + "/" + LAZ_14_NAME);
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                try (CloseableHttpResponse response = client.execute(new HttpGet(url))) {
                    try (OutputStream out = new FileOutputStream(laz14)) {
                        response.getEntity().writeTo(out);
                    }
                }
            }
        }
    }

    @Test
    public void readLaz14() {
        LASReader reader = new LASReader(laz14);
        LASHeader header = reader.getHeader();

        assertEquals(header.getNumberOfVariableLengthRecords(), StreamSupport.stream(header.getVariableLengthRecords().spliterator(), false).count());

        long[] classifications = new long[Byte.MAX_VALUE];
        assertEquals("LASF", header.getFileSignature());
        List<LASExtendedVariableLengthRecord> evlrs = StreamSupport.stream(header.getExtendedVariableLengthRecords().spliterator(), false).collect(Collectors.toList());
        assertEquals(header.getNumberOfExtendedVariableLengthRecords(), evlrs.size());
        assertEquals(0, evlrs.get(0).getReserved());
        assertEquals("copc", evlrs.get(0).getUserID());
        assertEquals(1000, evlrs.get(0).getRecordID());
        assertEquals(8896, evlrs.get(0).getRecordLength());
        assertEquals("EPT Hierarchy", evlrs.get(0).getDescription());
        long numPoints = 0;
        for (LASPoint p : reader.getPoints()) {
            classifications[p.getClassification()]++;
            numPoints++;
        }
        assertEquals(LAZ_14_NUM_POINT_RECORDS, header.getLegacyNumberOfPointRecords());
        assertEquals(LAZ_14_NUM_POINT_RECORDS, numPoints);
        assertEquals(390708, classifications[0]);   // never classified
        assertEquals(6909405, classifications[2]);  // ground
        assertEquals(2613807, classifications[5]);  // high vegetation
        assertEquals(343181, classifications[6]);   // building
        assertEquals(355339, classifications[9]);   // water
        assertEquals(51, classifications[15]);      // tower
        assertEquals(6790, classifications[17]);    // bridge deck
        assertEquals(4224, classifications[64]);    // extended classification 64
        assertEquals(18774, classifications[65]);   // extended classification 65
        assertEquals(92, classifications[66]);      // extended classification 66
        assertEquals(1233, classifications[68]);    // extended classification 68
        assertEquals(5882, classifications[73]);    // extended classification 73
        assertEquals(221, classifications[76]);     // extended classification 76
        assertEquals(24, classifications[77]);      // extended classification 77
    }

    @Test
    public void read() {
        LASReader reader = new LASReader(laz);
        LASHeader header = reader.getHeader();

        assertEquals(header.getNumberOfVariableLengthRecords(), StreamSupport.stream(header.getVariableLengthRecords().spliterator(), false).count());

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

        List<LASVariableLengthRecord> vlrs = new ArrayList<>();
        for (LASVariableLengthRecord vlr : header.getVariableLengthRecords()) {
            vlrs.add(vlr);
        }
        assertEquals(3, vlrs.size());
        LASVariableLengthRecord vlr = vlrs.get(0);
        assertEquals("LASF_Projection", vlr.getUserID());
        assertEquals(2112, vlr.getRecordID());
        assertEquals("OGC WKT Coordinate System", vlr.getDescription());
        assertEquals("COMPD_CS[\"NAD83 / Maryland + NAVD88 height - Geoid12B (metre)\",PROJCS[\"NAD83 / Maryland\",GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]],PROJECTION[\"Lambert_Conformal_Conic_2SP\"],PARAMETER[\"standard_parallel_1\",39.45],PARAMETER[\"standard_parallel_2\",38.3],PARAMETER[\"latitude_of_origin\",37.66666666666666],PARAMETER[\"central_meridian\",-77],PARAMETER[\"false_easting\",400000],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH],AUTHORITY[\"EPSG\",\"26985\"]],VERT_CS[\"NAVD88 height - Geoid12B (metre)\",VERT_DATUM[\"North American Vertical Datum 1988\",2005,AUTHORITY[\"EPSG\",\"5103\"]],HEIGHT_MODEL[\"US Geoid Model 2012B\"],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AUTHORITY[\"EPSG\",\"5703\"]]]", vlr.getDataAsString());
        vlr = vlrs.get(1);
        assertEquals("NIIRS10", vlr.getUserID());
        assertEquals(4, vlr.getRecordID());
        assertEquals("NIIRS10 Timestamp", vlr.getDescription());
        assertEquals(10, vlr.getRecordLength());
        vlr = vlrs.get(2);
        assertEquals("NIIRS10", vlr.getUserID());
        assertEquals(1, vlr.getRecordID());
        assertEquals("NIIRS10 Tile Index", vlr.getDescription());
        assertEquals(26, vlr.getRecordLength());

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
    public void readLASStream() throws Exception {
        LASHeader header;
        try (InputStream is = new FileInputStream(las)) {
            header = LASReader.getHeader(is);
        }

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

        List<LASVariableLengthRecord> vlrs = new ArrayList<>();
        for (LASVariableLengthRecord vlr : header.getVariableLengthRecords()) {
            vlrs.add(vlr);
        }
        assertEquals(3, vlrs.size());
        LASVariableLengthRecord vlr = vlrs.get(0);
        assertEquals("LASF_Projection", vlr.getUserID());
        assertEquals(2112, vlr.getRecordID());
        assertEquals("OGC WKT Coordinate System", vlr.getDescription());
        assertEquals("COMPD_CS[\"NAD83 / Maryland + NAVD88 height - Geoid12B (metre)\",PROJCS[\"NAD83 / Maryland\",GEOGCS[\"NAD83\",DATUM[\"North_American_Datum_1983\",SPHEROID[\"GRS 1980\",6378137,298.257222101,AUTHORITY[\"EPSG\",\"7019\"]],TOWGS84[0,0,0,0,0,0,0],AUTHORITY[\"EPSG\",\"6269\"]],PRIMEM[\"Greenwich\",0,AUTHORITY[\"EPSG\",\"8901\"]],UNIT[\"degree\",0.0174532925199433,AUTHORITY[\"EPSG\",\"9122\"]],AUTHORITY[\"EPSG\",\"4269\"]],PROJECTION[\"Lambert_Conformal_Conic_2SP\"],PARAMETER[\"standard_parallel_1\",39.45],PARAMETER[\"standard_parallel_2\",38.3],PARAMETER[\"latitude_of_origin\",37.66666666666666],PARAMETER[\"central_meridian\",-77],PARAMETER[\"false_easting\",400000],PARAMETER[\"false_northing\",0],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AXIS[\"X\",EAST],AXIS[\"Y\",NORTH],AUTHORITY[\"EPSG\",\"26985\"]],VERT_CS[\"NAVD88 height - Geoid12B (metre)\",VERT_DATUM[\"North American Vertical Datum 1988\",2005,AUTHORITY[\"EPSG\",\"5103\"]],HEIGHT_MODEL[\"US Geoid Model 2012B\"],UNIT[\"metre\",1,AUTHORITY[\"EPSG\",\"9001\"]],AUTHORITY[\"EPSG\",\"5703\"]]]", vlr.getDataAsString());
        vlr = vlrs.get(1);
        assertEquals("NIIRS10", vlr.getUserID());
        assertEquals(4, vlr.getRecordID());
        assertEquals("NIIRS10 Timestamp", vlr.getDescription());
        assertEquals(10, vlr.getRecordLength());
        vlr = vlrs.get(2);
        assertEquals("NIIRS10", vlr.getUserID());
        assertEquals(1, vlr.getRecordID());
        assertEquals("NIIRS10 Tile Index", vlr.getDescription());
        assertEquals(26, vlr.getRecordLength());

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
        try (InputStream is = new FileInputStream(las)) {
            for (LASPoint p : LASReader.getPoints(is)) {
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
