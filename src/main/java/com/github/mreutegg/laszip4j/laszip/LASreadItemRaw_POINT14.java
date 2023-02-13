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

public class LASreadItemRaw_POINT14 extends LASreadItemRaw {

    private ByteBuffer bb = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    public PointDataRecordPoint14 read(int notUsed) {
        bb.clear();
        instream.getBytes(bb.array(), 30);

        PointDataRecordPoint14 result = new PointDataRecordPoint14();
        result.X = bb.getInt();
        result.Y = bb.getInt();
        result.Z = bb.getInt();
        result.Intensity = bb.getChar();
        result.ReturnFlags = bb.get();
        result.ScanFlags = bb.get();
        result.Classification = (short)Byte.toUnsignedInt(bb.get());
        result.UserData = (short)Byte.toUnsignedInt(bb.get());
        result.ScanAngle = bb.getShort();
        result.PointSourceID = bb.getChar();
        result.GPSTime = bb.getLong();

        return result;
    }
}
