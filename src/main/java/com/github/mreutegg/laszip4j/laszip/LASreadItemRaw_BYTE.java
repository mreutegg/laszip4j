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

public class LASreadItemRaw_BYTE extends LASreadItemRaw {

    private final int byteCount;

    public LASreadItemRaw_BYTE(int byteCount) {
        this.byteCount = byteCount;
    }

    @Override
    public PointDataRecordBytes read(MutableInteger notUsed) {

        PointDataRecordBytes result = new PointDataRecordBytes(byteCount);
        instream.getBytes(result.Bytes, byteCount);
        return result;
    }
}
