/*
 * Copyright 2005-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.nio.ByteBuffer;

public class U64I64F64 {

    private final ByteBuffer data = ByteBuffer.allocate(8);

    public static U64I64F64[] newU64I64F64Array(int size) {
        U64I64F64[] array = new U64I64F64[size];
        for (int i = 0; i < size; i++) {
            array[i] = new U64I64F64();
        }
        return array;
    }

    public void setU64(long value) {
        data.putLong(0, value);
    }

    public void setI64(long value) {
        data.putLong(0, value);
    }

    public void setF64(double value) {
        data.putDouble(0, value);
    }

    public long getU64() {
        return data.getLong(0);
    }

    public long getI64() {
        return data.getLong(0);
    }

    public double getF64() {
        return data.getDouble(0);
    }
}
