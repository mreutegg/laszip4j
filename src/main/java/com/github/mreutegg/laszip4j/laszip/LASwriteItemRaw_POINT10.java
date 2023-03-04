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

public class LASwriteItemRaw_POINT10 extends LASwriteItemRaw<PointDataRecordPoint10> {

    @Override
    public boolean write(PointDataRecordPoint10 point, int /* unsigned */ context) {
        outstream.put32bitsLE(point.X);
        outstream.put32bitsLE(point.Y);
        outstream.put32bitsLE(point.Z);
        outstream.put16bitsLE(point.Intensity);
        outstream.putByte(point.Flags);
        outstream.putByte((byte) point.Classification);
        outstream.putByte(point.ScanAngleRank);
        outstream.putByte((byte) point.UserData);
        outstream.put16bitsLE(point.PointSourceID);
        return true;
    }
}
