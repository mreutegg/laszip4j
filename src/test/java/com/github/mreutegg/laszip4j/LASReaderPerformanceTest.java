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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;

public class LASReaderPerformanceTest {

    @Rule
    public final DataFiles files = new DataFiles();

    private final File las = new File("target", LASReaderPerformanceTest.class.getSimpleName() + ".las");

    @Before
    public void prepareLas() {
        System.setProperty("laszip4j.mmap.buffer.size", String.valueOf(100 * 1024 * 1024));
        if (!las.exists()) {
            LASWriter writer = new LASWriter(new LASReader(files.laz14));
            writer.write(las);
        }
    }

    @After
    public void removeProperty() {
        System.clearProperty("laszip4j.mmap.buffer.size");
    }

    @Test
    public void readLasFile() {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            LASReader reader = new LASReader(las);
            try (CloseablePointIterable points = reader.getCloseablePoints()) {
                points.forEach(LASPoint::getClassification);
            }
            time = System.currentTimeMillis() - time;
            // ignore first run
            if (i > 0) {
                stats.addValue(time);
            }
        }
        System.out.println(stats);
    }

    @Test
    public void readLasStream() throws Exception {
        DescriptiveStatistics stats = new DescriptiveStatistics();
        for (int i = 0; i < 10; i++) {
            long time = System.currentTimeMillis();
            try (InputStream in = Files.newInputStream(las.toPath())) {
                LASReader.getPoints(in).forEach(LASPoint::getClassification);
            }
            time = System.currentTimeMillis() - time;
            // ignore first run
            if (i > 0) {
                stats.addValue(time);
            }
        }
        System.out.println(stats);
    }
}
