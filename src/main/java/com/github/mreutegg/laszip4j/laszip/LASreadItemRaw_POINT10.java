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
    public void read(byte[] item) {
        bb.clear();
        instream.getBytes(bb.array(), 20);
        // write native big endian into item
        ByteBuffer itemBB = ByteBuffer.wrap(item);
        itemBB.putInt(bb.getInt());     // x
        itemBB.putInt(bb.getInt());     // y
        itemBB.putInt(bb.getInt());     // z
        itemBB.putChar(bb.getChar());   // intensity
        itemBB.put(bb.get());           // return_number, number_of_returns, scan_direction, edge_of_flight
        itemBB.put(bb.get());           // classification, synthetic, keypoint, withheld
        itemBB.put(bb.get());           // scan_angle
        itemBB.put(bb.get());           // userdata
        itemBB.putChar(bb.getChar());   // point_source_ID
    }
}
