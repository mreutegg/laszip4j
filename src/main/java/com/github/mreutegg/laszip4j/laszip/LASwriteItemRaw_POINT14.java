/*
 * (c) 2007-2022, rapidlasso GmbH - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the Apache Public License 2.0 published by the Apache Software
 * Foundation. See the COPYING file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

public class LASwriteItemRaw_POINT14 extends LASwriteItemRaw<PointDataRecordPoint14> {
    @Override
    public boolean write(PointDataRecordPoint14 point, int context) {
        outstream.put32bitsLE(point.X);
        outstream.put32bitsLE(point.Y);
        outstream.put32bitsLE(point.Z);
        outstream.put16bitsLE(point.Intensity);
        outstream.putByte(point.ReturnFlags);
        outstream.putByte(point.ScanFlags);
        outstream.putByte((byte) point.Classification);
        outstream.putByte((byte) point.UserData);
        outstream.put16bitsLE(point.ScanAngle);
        outstream.put16bitsLE(point.PointSourceID);
        outstream.put64bitsLE(point.GPSTime);
        return true;
    }
}
