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

import com.github.mreutegg.laszip4j.laszip.LASattribute;
import com.github.mreutegg.laszip4j.laszip.MyDefs;

import static com.github.mreutegg.laszip4j.LASExtraBytesType.fromOrdinal;
import static java.util.Objects.requireNonNull;

/**
 * A description of extra bytes appended to point data.
 */
public final class LASExtraBytesDescription {

    private final LASattribute attribute;

    /**
     * The byte offset for the described extra bytes.
     */
    private final int offset;

    /**
     * Create a new extra bytes description starting at the given offset.
     *
     * @param attribute the underlying attribute.
     * @param offset byte offsite within extra bytes.
     */
    LASExtraBytesDescription(LASattribute attribute,
                             int offset) {
        this.attribute = requireNonNull(attribute);
        this.offset = offset;
    }

    /**
     * @return byte offset for the described extra bytes.
     */
    int getOffset() {
        return offset;
    }

    /**
     * Size in bytes of described type.
     * @return size in bytes.
     */
    int getTypeSize() {
        return attribute.get_size();
    }

    /**
     * @return the data byte for the described extra bytes.
     */
    public LASExtraBytesType getDataType() {
        return fromOrdinal(attribute.data_type);
    }

    /**
     * @return the name for the described extra bytes.
     */
    public String getName() {
        return MyDefs.stringFromByteArray(attribute.name);
    }

    /**
     * @return the description for the described extra bytes.
     */
    public String getDescription() {
        return MyDefs.stringFromByteArray(attribute.description);
    }

    /**
     * @return whether the described extra bytes has a no-data value.
     */
    public boolean hasNoDataValue() {
        return attribute.has_no_data();
    }

    /**
     * @return whether the described extra bytes has a min value.
     */
    public boolean hasMinValue() {
        return attribute.has_min();
    }

    /**
     * @return whether the described extra bytes has a max value.
     */
    public boolean hasMaxValue() {
        return attribute.has_max();
    }

    /**
     * @return whether the described extra bytes has a scale value.
     */
    public boolean hasScaleValue() {
        return attribute.has_scale();
    }

    /**
     * @return whether the described extra bytes has an offset value.
     */
    public boolean hasOffsetValue() {
        return attribute.has_offset();
    }

    /**
     * Returns the scale value for a given index.
     * @param i index
     * @return the scale value for a given index.
     */
    public double getScale(int i) {
        if (i < 0 || i >= attribute.scale.length) {
            throw new IllegalStateException("" + i);
        }
        return attribute.scale[i];
    }

    /**
     * Returns the offset value for a given index.
     * @param i index
     * @return the offset value for a given index.
     */
    public double getOffset(int i) {
        if (i < 0 || i >= attribute.offset.length) {
            throw new IllegalStateException("" + i);
        }
        return attribute.offset[i];
    }
}
