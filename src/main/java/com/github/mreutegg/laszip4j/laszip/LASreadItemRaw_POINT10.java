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

public class LASreadItemRaw_POINT10 extends LASreadItemRaw {

    private ByteBuffer bb = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    public PointDataRecordPoint10 read(int notUsed) {
        bb.clear();
        instream.getBytes(bb.array(), 20);

        PointDataRecordPoint10 result = new PointDataRecordPoint10();
        result.X = bb.getInt();
        result.Y = bb.getInt();
        result.Z = bb.getInt();
        result.Intensity = bb.getChar();
        result.Flags = bb.get();
        result.Classification = bb.get();
        result.ScanAngleRank = bb.get();
        result.UserData = (short)Byte.toUnsignedInt(bb.get());
        result.PointSourceID = bb.getChar();
        return result;
    }
}
