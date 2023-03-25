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

import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.math.BigInteger;

/**
 * Represents some extra bytes as described in the VLR LASF_Spec/4.
 */
public final class LASExtraBytes {

    private final LASpoint point;

    private final LASExtraBytesDescription description;

    LASExtraBytes(LASpoint point,
                  LASExtraBytesDescription description) {
        this.point = point;
        this.description = description;
    }

    /**
     * @return the value of the described extra bytes as a double after applying
     *          a potential offset and scale operation.
     */
    public Double getValue() {
        Number v = getRawValue();
        LASExtraBytesType t = description.getType();
        return translateRawToDouble(v, t, 0);
    }

    /**
     * Returns all values of the described extra bytes in an array. The length
     * of the array is determined by the cardinality of the type.
     * See {@link LASExtraBytesType#getCardinality()}
     *
     * @return the values of the described extra bytes as an array of double
     *          after applying a potential offset and scale operation.
     */
    public Double[] getValues() {
        Number[] raws = getRawValues();
        Double[] values = new Double[raws.length];
        LASExtraBytesType t = description.getType();
        for (int i = 0; i < raws.length; i++) {
            values[i] = translateRawToDouble(raws[i], t, i);
        }
        return values;
    }

    private Double translateRawToDouble(Number v,
                                        LASExtraBytesType t,
                                        int i) {
        if (t.isUnsigned()) {
            Class<?> type = t.getClazz();
            if (type == Byte.class) {
                v = Byte.toUnsignedInt(v.byteValue());
            } else if (type == Short.class) {
                v = Short.toUnsignedInt(v.shortValue());
            } else if (type == Integer.class) {
                v = Integer.toUnsignedLong(v.intValue());
            } else if (type == Long.class) {
                v = new BigInteger(Long.toUnsignedString(v.longValue()));
            }
        }
        if (description.hasScaleValue()) {
            v = v.doubleValue() * description.getScale(i);
        }
        if (description.hasOffsetValue()) {
            v = v.doubleValue() + description.getOffset(i);
        }
        return v.doubleValue();
    }

    /**
     * @return the raw value as stored in the described extra bytes.
     */
    public Number getRawValue() {
        Class<?> type = description.getType().getClazz();
        int start = description.getOffset();
        return getRawValue(type, start);
    }

    /**
     * @return the raw values as stored in the described extra bytes.
     */
    public Number[] getRawValues() {
        LASExtraBytesType t = description.getType();
        Number[] values = new Number[t.getCardinality()];
        int offset = description.getOffset();
        for (int i = 0; i < values.length; i++) {
            values[i] = getRawValue(t.getClazz(), offset);
            offset += description.getTypeSize();
        }
        return values;
    }

    private Number getRawValue(Class<?> type, int offset) {
        if (type == Byte.class) {
            return point.get_attributeByte(offset);
        } else if (type == Short.class) {
            return point.get_attributeShort(offset);
        } else if (type == Integer.class) {
            return point.get_attributeInt(offset);
        } else if (type == Long.class) {
            return point.get_attributeLong(offset);
        } else if (type == Float.class) {
            return point.get_attributeFloat(offset);
        } else if (type == Double.class) {
            return point.get_attributeDouble(offset);
        } else {
            throw new IllegalStateException("Unsupported type: " + type.getName());
        }
    }
}
