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
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.DataFiles;
import com.github.mreutegg.laszip4j.LASReader;
import com.github.mreutegg.laszip4j.LASReaderTest;
import com.github.mreutegg.laszip4j.lastools.Laszip;
import org.junit.Rule;
import org.junit.Test;

import java.io.File;

public class LASwriterLASTest {

    @Rule
    public final DataFiles files = new DataFiles();

    private final File las = new File("target", files.laz.getName() + LASwriterLASTest.class.getSimpleName() + ".las");

    private final File las14 = new File("target", files.laz14.getName() + LASwriterLASTest.class.getSimpleName() + ".las");

    @Test
    public void writeLas() {
        Laszip.run(new String[]{"-i", files.laz.getPath(), "-o", las.getPath()});
        LASReader reader = new LASReader(las);
        LASReaderTest.verifyLaz(reader.getPoints(), reader.getHeader());
    }

    @Test
    public void writeLas14() {
        Laszip.run(new String[]{"-i", files.laz14.getPath(), "-o", las14.getPath()});
        LASReaderTest.verifyLaz14(las14);
    }
}
