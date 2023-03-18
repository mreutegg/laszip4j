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

public final class LASExtraBytesDescription {


    private final LASattribute attribute;

    private final int start;

    public LASExtraBytesDescription(LASattribute attribute,
                                    int start) {
        this.attribute = requireNonNull(attribute);
        this.start = start;
    }

    int getStart() {
        return start;
    }

    int getTypeSize() {
        return attribute.get_size();
    }

    public LASExtraBytesType getDataType() {
        return fromOrdinal(attribute.data_type);
    }

    public String getName() {
        return MyDefs.stringFromByteArray(attribute.name);
    }

    public String getDescription() {
        return MyDefs.stringFromByteArray(attribute.description);
    }

    public boolean hasNoDataValue() {
        return attribute.has_no_data();
    }

    public boolean hasMinValue() {
        return attribute.has_min();
    }

    public boolean hasMaxValue() {
        return attribute.has_max();
    }

    public boolean hasScaleValue() {
        return attribute.has_scale();
    }

    public boolean hasOffsetValue() {
        return attribute.has_offset();
    }

    public double getScale(int i) {
        if (i < 0 || i >= attribute.scale.length) {
            throw new IllegalStateException("" + i);
        }
        return attribute.scale[i];
    }

    public double getOffset(int i) {
        if (i < 0 || i >= attribute.offset.length) {
            throw new IllegalStateException("" + i);
        }
        return attribute.offset[i];
    }
}
