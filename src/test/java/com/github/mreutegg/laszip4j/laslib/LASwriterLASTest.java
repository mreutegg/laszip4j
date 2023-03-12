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
import com.github.mreutegg.laszip4j.LASReaderTest;
import com.github.mreutegg.laszip4j.lastools.Laszip;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

public class LASwriterLASTest {

    private final DataFiles files = new DataFiles();

    private final File las = new File("target", files.laz.getName() + LASwriterLASTest.class.getName() + ".las");

    @Before
    public void before() throws Exception {
        files.download();
    }

    @Test
    public void writeLas() {
        Laszip.run(new String[]{"-i", files.laz.getPath(), "-o", las.getPath()});
        LASReaderTest.verifyLaz(las);
    }
}
