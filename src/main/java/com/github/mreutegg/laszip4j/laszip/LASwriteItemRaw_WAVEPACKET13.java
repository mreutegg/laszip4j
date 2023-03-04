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

public class LASwriteItemRaw_WAVEPACKET13 extends LASwriteItemRaw<PointDataRecordWavepacket> {
    @Override
    public boolean write(PointDataRecordWavepacket point, int context) {
        outstream.putByte((byte) point.DescriptorIndex);
        outstream.put64bitsLE(point.OffsetToWaveformData);
        outstream.put32bitsLE((int) point.PacketSize);
        outstream.put32bitsLE(Float.floatToIntBits(point.ReturnPointWaveformLocation));
        outstream.put32bitsLE(Float.floatToIntBits(point.ParametricDx));
        outstream.put32bitsLE(Float.floatToIntBits(point.ParametricDy));
        outstream.put32bitsLE(Float.floatToIntBits(point.ParametricDz));
        return true;
    }
}
