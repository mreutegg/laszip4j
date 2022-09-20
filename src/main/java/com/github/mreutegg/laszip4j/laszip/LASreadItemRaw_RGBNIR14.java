/*
 * Copyright 2007-2013, martin isenburg, rapidlasso - fast tools to catch reality
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
import java.nio.ByteOrder;

public class LASreadItemRaw_RGBNIR14 extends LASreadItemRaw {

    private ByteBuffer bb = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    public PointDataRecordRgbNIR read(int notUsed) {
        bb.clear();
        instream.getBytes(bb.array(), 8);

        PointDataRecordRgbNIR result = new PointDataRecordRgbNIR();
        result.R = bb.getChar();
        result.G = bb.getChar();
        result.B = bb.getChar();
        result.NIR = bb.getChar();
        return result;
    }
}
