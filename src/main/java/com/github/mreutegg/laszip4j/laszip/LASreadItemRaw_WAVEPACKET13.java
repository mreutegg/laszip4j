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

public class LASreadItemRaw_WAVEPACKET13 extends LASreadItemRaw {

    private ByteBuffer bb = ByteBuffer.allocate(29).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    public void read(byte[] item) {
        bb.clear();
        instream.getBytes(bb.array(), 29);
        // write native big endian into item
        ByteBuffer itemBB = ByteBuffer.wrap(item);
        itemBB.put(bb.get());           // wavepacket descriptor index
        itemBB.putLong(bb.getLong());   // byte offset to waveform data
        itemBB.putInt(bb.getInt());     // waveform packet size in bytes
        itemBB.putInt(bb.getInt());     // return point waveform location
        itemBB.putInt(bb.getInt());     // X(t)
        itemBB.putInt(bb.getInt());     // Y(t)
        itemBB.putInt(bb.getInt());     // Z(t)
    }
}
