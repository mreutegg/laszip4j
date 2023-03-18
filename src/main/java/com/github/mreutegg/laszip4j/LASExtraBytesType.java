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

import java.util.HashMap;
import java.util.Map;

public final class LASExtraBytesType {

    private static final Map<Integer, LASExtraBytesType> TYPES = new HashMap<>();

    public static final LASExtraBytesType TYPE_UCHAR = create(1, Byte.class, 1, true);
    public static final LASExtraBytesType TYPE_CHAR = create(2, Byte.class, 1, false);
    public static final LASExtraBytesType TYPE_USHORT = create(3, Short.class, 1, true);
    public static final LASExtraBytesType TYPE_SHORT = create(4, Short.class, 1, false);
    public static final LASExtraBytesType TYPE_ULONG = create(5, Integer.class, 1, true);
    public static final LASExtraBytesType TYPE_LONG = create(6, Integer.class, 1, false);
    public static final LASExtraBytesType TYPE_ULONGLONG = create(7, Long.class, 1, true);
    public static final LASExtraBytesType TYPE_LONGLONG = create(8, Long.class, 1, false);
    public static final LASExtraBytesType TYPE_FLOAT = create(9, Float.class, 1, false);
    public static final LASExtraBytesType TYPE_DOUBLE = create(10, Double.class, 1, false);
    public static final LASExtraBytesType TYPE_UCHAR2 = create(11, Byte.class, 2, true);
    public static final LASExtraBytesType TYPE_CHAR2 = create(12, Byte.class, 2, false);
    public static final LASExtraBytesType TYPE_USHORT2 = create(13, Short.class, 2, true);
    public static final LASExtraBytesType TYPE_SHORT2 = create(14, Short.class, 2, false);
    public static final LASExtraBytesType TYPE_ULONG2 = create(15, Integer.class, 2, true);
    public static final LASExtraBytesType TYPE_LONG2 = create(16, Integer.class, 2, false);
    public static final LASExtraBytesType TYPE_ULONGLONG2 = create(17, Long.class, 2, true);
    public static final LASExtraBytesType TYPE_LONGLONG2 = create(18, Long.class, 2, false);
    public static final LASExtraBytesType TYPE_FLOAT2 = create(19, Float.class, 2, false);
    public static final LASExtraBytesType TYPE_DOUBLE2 = create(20, Double.class, 2, false);
    public static final LASExtraBytesType TYPE_UCHAR3 = create(21, Byte.class, 3, true);
    public static final LASExtraBytesType TYPE_CHAR3 = create(22, Byte.class, 3, false);
    public static final LASExtraBytesType TYPE_USHORT3 = create(23, Short.class, 3, true);
    public static final LASExtraBytesType TYPE_SHORT3 = create(24, Short.class, 3, false);
    public static final LASExtraBytesType TYPE_ULONG3 = create(25, Integer.class, 3, true);
    public static final LASExtraBytesType TYPE_LONG3 = create(26, Integer.class, 3, false);
    public static final LASExtraBytesType TYPE_ULONGLONG3 = create(27, Long.class, 3, true);
    public static final LASExtraBytesType TYPE_LONGLONG3 = create(28, Long.class, 3, false);
    public static final LASExtraBytesType TYPE_FLOAT3 = create(29, Float.class, 3, false);
    public static final LASExtraBytesType TYPE_DOUBLE3 = create(30, Double.class, 3, false);

    private final int ordinal;

    private final Class<?> clazz;

    private final int length;

    private final boolean unsigned;

    private LASExtraBytesType(int ordinal, Class<?> clazz, int length, boolean unsigned) {
        this.ordinal = ordinal;
        this.clazz = clazz;
        this.length = length;
        this.unsigned = unsigned;
    }

    private static LASExtraBytesType create(int ordinal, Class<?> clazz, int length, boolean unsigned) {
        LASExtraBytesType t = new LASExtraBytesType(ordinal, clazz, length, unsigned);
        TYPES.put(ordinal, t);
        return t;
    }

    static LASExtraBytesType fromOrdinal(int ordinal) {
        LASExtraBytesType t = TYPES.get(ordinal);
        if (t == null) {
            throw new IllegalArgumentException("Invalid data type: " + ordinal);
        }
        return t;
    }

    public int getLength() {
        return length;
    }

    public boolean isUnsigned() {
        return unsigned;
    }

    Class<?> getClazz() {
        return clazz;
    }
}
