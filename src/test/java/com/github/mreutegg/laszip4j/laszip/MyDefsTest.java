/*
 * Copyright 2024 Marcel Reutegger
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

import org.junit.Test;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.clearBit;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.setBit;
import static org.junit.Assert.assertEquals;

public class MyDefsTest {

    @Test
    public void setBitByte() {
        byte value = 0;
        for (int i = 0; i < Byte.SIZE; i++) {
            assertEquals(i, bitCount(value));
            value = setBit(value, i);
            assertEquals(i + 1, bitCount(value));
        }
        for (int i = 0; i < Byte.SIZE; i++) {
            assertEquals(Byte.SIZE - i, bitCount(value));
            value = clearBit(value, i);
            assertEquals(Byte.SIZE - 1 - i, bitCount(value));
        }
    }

    @Test
    public void setBitShort() {
        short value = 0;
        for (int i = 0; i < Short.SIZE; i++) {
            assertEquals(i, bitCount(value));
            value = setBit(value, i);
            assertEquals(i + 1, bitCount(value));
        }
        for (int i = 0; i < Short.SIZE; i++) {
            assertEquals(Short.SIZE - i, bitCount(value));
            value = clearBit(value, i);
            assertEquals(Short.SIZE - 1 - i, bitCount(value));
        }
    }

    private static int bitCount(short value) {
        return Integer.bitCount(Short.toUnsignedInt(value));
    }

    private static int bitCount(byte value) {
        return Integer.bitCount(Byte.toUnsignedInt(value));
    }
}
