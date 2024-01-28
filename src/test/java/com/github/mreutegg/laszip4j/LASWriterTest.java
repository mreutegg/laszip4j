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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LASWriterTest {

    @Rule
    public final DataFiles files = new DataFiles();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder(new File("target"));

    private File outputFile;

    @Before
    public void before() throws IOException {
        outputFile = temporaryFolder.newFile("output.las");
    }

    @Test
    public void write() {
        final short unclassified = 1;
        final short lowPoint = 20;
        final AtomicInteger numModified = new AtomicInteger();
        LASReader reader = new LASReader(files.las).transform((point, modifier) -> {
            if (point.getClassification() == lowPoint) {
                modifier.setClassification(unclassified);
                numModified.incrementAndGet();
            }
        });

        LASWriter writer = new LASWriter(reader);
        writer.write(outputFile);

        assertEquals(1696, numModified.get());

        LASReader result = new LASReader(outputFile);
        int numUnclassified = 0;
        int numLowPoint = 0;
        for (LASPoint p : result.getPoints()) {
            if (p.getClassification() == unclassified) {
                numUnclassified++;
            } else if (p.getClassification() == lowPoint) {
                numLowPoint++;
            }
        }
        assertEquals(0, numLowPoint);
        assertEquals(112267, numUnclassified);
    }

    @Test
    public void writeInsideRectangle() {
        LASReader reader = new LASReader(files.laz)
                .insideRectangle(2669450, 1257390, 2669480, 1257450);

        LASWriter writer = new LASWriter(reader);
        writer.write(outputFile);
        LASReader result = new LASReader(outputFile);

        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        long numPoints = 0;
        for (LASPoint p : result.getPoints()) {
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
    public void setWithheld() {
        final short lowPoint = 20;
        final AtomicInteger numWithheld = new AtomicInteger();
        LASReader reader = new LASReader(files.las).transform((point, modifier) -> {
            if (point.getClassification() == lowPoint) {
                modifier.setWithheld(true);
                numWithheld.incrementAndGet();
            }
        });

        LASWriter writer = new LASWriter(reader);
        writer.write(outputFile);

        assertEquals(1696, numWithheld.get());

        numWithheld.set(0);
        LASReader result = new LASReader(outputFile);
        for (LASPoint p : result.getPoints()) {
            if (p.isWithheld()) {
                numWithheld.incrementAndGet();
            }
        }
        assertEquals(1696, numWithheld.get());
    }
}
