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

import org.junit.Rule;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;

public class LASWriterTest {

    @Rule
    public final DataFiles files = new DataFiles();

    private final String outputFileName = LASWriterTest.class.getSimpleName() + files.las.getName();

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
        File out = new File("target", outputFileName);
        writer.write(out);

        assertEquals(1696, numModified.get());

        LASReader result = new LASReader(out);
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
}
