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

public class U32I32F32 {

    private final ByteBuffer data;

    private U32I32F32(ByteBuffer buffer) {
        this.data = buffer.duplicate();
    }

    public U32I32F32() {
        this(ByteBuffer.allocate(4));
    }

    public static U32I32F32 wrap(ByteBuffer buffer) {
        return new U32I32F32(buffer);
    }

    public void setU32(int value) {
        data.putInt(0, value);
    }

    public int getU32() {
        return data.getInt(0);
    }

    public void setI32(int value) {
        data.putInt(0, value);
    }

    public int getI32() {
        return data.getInt(0);
    }

    public void setF32(float value) {
        data.putFloat(0, value);
    }

    public float getFloat() {
        return data.getFloat(0);
    }
}
