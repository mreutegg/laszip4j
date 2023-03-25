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

/**
 * The data type of some extra bytes.
 */
public final class LASExtraBytesType {

    private static final Map<Integer, LASExtraBytesType> TYPES = new HashMap<>();

    static {
        create(1, Byte.class, 1, true);
        create(2, Byte.class, 1, false);
        create(3, Short.class, 1, true);
        create(4, Short.class, 1, false);
        create(5, Integer.class, 1, true);
        create(6, Integer.class, 1, false);
        create(7, Long.class, 1, true);
        create(8, Long.class, 1, false);
        create(9, Float.class, 1, false);
        create(10, Double.class, 1, false);
        create(11, Byte.class, 2, true);
        create(12, Byte.class, 2, false);
        create(13, Short.class, 2, true);
        create(14, Short.class, 2, false);
        create(15, Integer.class, 2, true);
        create(16, Integer.class, 2, false);
        create(17, Long.class, 2, true);
        create(18, Long.class, 2, false);
        create(19, Float.class, 2, false);
        create(20, Double.class, 2, false);
        create(21, Byte.class, 3, true);
        create(22, Byte.class, 3, false);
        create(23, Short.class, 3, true);
        create(24, Short.class, 3, false);
        create(25, Integer.class, 3, true);
        create(26, Integer.class, 3, false);
        create(27, Long.class, 3, true);
        create(28, Long.class, 3, false);
        create(29, Float.class, 3, false);
        create(30, Double.class, 3, false);
    }

    private final int dataType;

    private final Class<?> clazz;

    private final int cardinality;

    private final boolean unsigned;

    private LASExtraBytesType(int dataType, Class<?> clazz, int cardinality, boolean unsigned) {
        this.dataType = dataType;
        this.clazz = clazz;
        this.cardinality = cardinality;
        this.unsigned = unsigned;
    }

    private static void create(int dataType, Class<?> clazz, int cardinality, boolean unsigned) {
        LASExtraBytesType t = new LASExtraBytesType(dataType, clazz, cardinality, unsigned);
        TYPES.put(dataType, t);
    }

    static LASExtraBytesType fromOrdinal(int ordinal) {
        LASExtraBytesType t = TYPES.get(ordinal);
        if (t == null) {
            throw new IllegalArgumentException("Invalid data type: " + ordinal);
        }
        return t;
    }

    /**
     * @return the data type as defined in LAS 1.4 specification for extra bytes.
     */
    int getDataType() {
        return dataType;
    }

    /**
     * The cardinality of this extra bytes type. Depending on the type this
     * method return 1, 2 or 3.
     *
     * @return the cardinality of this extra bytes.
     */
    public int getCardinality() {
        return cardinality;
    }

    /**
     * @return {@code true} if this is an unsigned type, {@code false} otherwise.
     */
    public boolean isUnsigned() {
        return unsigned;
    }

    /**
     * @return the Java primitive type class used by this extra bytes.
     */
    Class<?> getClazz() {
        return clazz;
    }
}
