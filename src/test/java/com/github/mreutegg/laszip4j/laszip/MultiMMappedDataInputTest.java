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
package com.github.mreutegg.laszip4j.laszip;

import com.github.mreutegg.laszip4j.laszip.ByteStreamInFile.MultiMMappedDataInput;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.RandomAccessFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MultiMMappedDataInputTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder(new File("target"));

    private RandomAccessFile file;

    @Before
    public void setup() throws Exception {
        file = new RandomAccessFile(tempFolder.newFile(MultiMMappedDataInputTest.class.getSimpleName()), "rw");
        file.writeInt(42);
        file.writeBoolean(true);
        file.writeLong(0xcafe);
        file.writeChars("foo");
    }

    @Test
    public void position() throws Exception {
        MultiMMappedDataInput input = new MultiMMappedDataInput(file, 7);
        assertEquals(0, input.position());
        int[] positions = new int[] {6, 7, 8};
        for (int p : positions) {
            input.position(p);
            assertEquals(p, input.position());
        }
        input.position(file.length());
        assertEquals(file.length(), input.position());
        try {
            input.position(file.length() + 1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    @Test
    public void skip() throws Exception {
        MultiMMappedDataInput input = new MultiMMappedDataInput(file, 7);
        input.skipBytes(3);
        assertEquals(3, input.position());
        input.position(0);
        assertEquals(0, input.position());
        assertEquals(file.length(), input.skipBytes((int) file.length() + 1));
    }

    @Test
    public void read() throws Exception {
        MultiMMappedDataInput input = new MultiMMappedDataInput(file, 7);
        assertEquals(42, input.readInt());
        assertTrue(input.readBoolean());
        assertEquals(0xcafe, input.readLong());
        String expected = "foo";
        for (char c : expected.toCharArray()) {
            assertEquals(c, input.readChar());
        }
    }

    @Test
    public void readFully() throws Exception {
        MultiMMappedDataInput input = new MultiMMappedDataInput(file, 7);
        byte[] data = new byte[100];
        try {
            input.readFully(data, 0, data.length);
            fail("UncheckedEOFException expected");
        } catch (UncheckedEOFException e) {
            // expected
        }
    }
}
