/*
 * Copyright 2005-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstring.memset;

public class LASvlr_wave_packet_descr {

    private ByteBuffer data = ByteBuffer.allocate(26).order(ByteOrder.LITTLE_ENDIAN);

    LASvlr_wave_packet_descr() {clean();};
    void clean() {memset(data, (byte) 0, 26);};
    public byte getBitsPerSample() {return data.get(0);};
    public byte getCompressionType() {return data.get(1);};
    public int getNumberOfSamples() {return data.getInt(2);};
    public int getTemporalSpacing() {return data.getInt(6);};
    public double getDigitizerGain() {return data.getDouble(10);};
    public double getDigitizerOffset() {return data.getDouble(18);};
    public void setBitsPerSample(byte bps) {data.put(0, bps);};
    public void setCompressionType(byte compression) {data.put(1, compression);};
    public void setNumberOfSamples(int samples) {data.putInt(2, samples);};
    public void setTemporalSpacing(int spacing) {data.putInt(6, spacing);};
    public void setDigitizerGain(double gain) {data.putDouble(10, gain);};
    public void setDigitizerOffset(double offset) {data.putDouble(18, offset);};

    public static LASvlr_wave_packet_descr fromByteArray(byte[] data) {
        LASvlr_wave_packet_descr descr = new LASvlr_wave_packet_descr();
        descr.data.put(data);
        return descr;
    }
}
