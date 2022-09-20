/*
 * Copyright 2017 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j;

import com.github.mreutegg.laszip4j.laszip.LASpoint;

/**
 * A LAS point data record.
 */
public final class LASPoint {

    private final LASpoint p;

    LASPoint(LASpoint p) {
        this.p = p;
    }

    /**
     * @return "X" as a signed int.
     */
    public int getX() {
        return p.getX();
    }

    /**
     * @return "Y" as a signed int.
     */
    public int getY() {
        return p.getY();
    }

    /**
     * @return "Z" as a signed int.
     */
    public int getZ() {
        return p.getZ();
    }

    /**
     * @return "Intensity" as an unsigned short (char).
     */
    public char getIntensity() {
        return p.getIntensity();
    }

    /**
     * @return "Return Number" as a byte (range 1..15).
     */
    public byte getReturnNumber() {
        return p.getReturn_number();
    }

    /**
     * @return "Number of Returns (given pulse)" as a byte (range 1..15)
     */
    public byte getNumberOfReturns() {
        return p.getNumber_of_returns();
    }

    /**
     * @return "Scan Direction Flag" as a byte (1 bit).
     */
    public byte getScanDirectionFlag() {
        return p.getScan_direction_flag();
    }

    /**
     * @return "Edge of Flight Line" as a byte (1 bit).
     */
    public byte getEdgeOfFlightLine() {
        return p.getEdge_of_flight_line();
    }

    /**
     * @return "Classification" as a short (range is 0..31 for point10, and 0..255 for point14).
     */
    public short getClassification() {
        return p.getClassification();
    }

    /**
     * @return "Synthetic" flag.
     */
    public boolean isSynthetic() {
        return p.getSynthetic_flag() == 1;
    }

    /**
     * @return "Key-point" flag.
     */
    public boolean isKeyPoint() {
        return p.getSynthetic_flag() == 1;
    }

    /**
     * @return "Withheld" flag.
     */
    public boolean isWithheld() {
        return p.getWithheld_flag() == 1;
    }

    /**
     * @return "Scan Angle Rank (-90 to +90) - Left side" as a signed byte.
     */
    public byte getScanAngleRank() {
        return p.getScan_angle_rank();
    }

    /**
     * @return "User Data" as a unsigned byte.
     */
    public short getUserData() {
        return p.getUser_data();
    }

    /**
     * @return "Point Source ID" as an unsigned short (range 0..65535).
     */
    public char getPointSourceID() {
        return p.getPoint_source_ID();
    }

    /**
     * @return "GPS Time" as a double.
     */
    public double getGPSTime() {
        return p.getGps_time();
    }

    /**
     * @return "Red" as an unsigned short (char).
     */
    public char getRed() {
        return p.get_R();
    }

    /**
     * @return "Green" as an unsigned short (char).
     */
    public char getGreen() {
        return p.get_G();
    }

    /**
     * @return "Blue" as an unsigned short (char).
     */
    public char getBlue() {
        return p.get_B();
    }

    /**
     * @return "NIR" as an unsigned short (char).
     */
    public char getNIR() {
        return p.get_I();
    }

    /**
     * @return {@code true} if this point has a wave packet.
     */
    public boolean hasWavePacket() {
        return p.haveWavepacket();
    }

    /**
     * @return "Wave Packet Descriptor Index"
     */
    public byte getWavePacketDescriptorIndex() { return (byte)p.getWavepacketDescriptorIndex(); }

    /**
     * @return "Byte offset to waveform data" as an unsigned long.
     */
    public long getByteOffsetToWaveformData() { return p.getWavepacketOffsetToWaveformData(); }

    /**
     * @return "Waveform packet size in bytes" as an unsigned int.
     */
    public int getWaveformPacketSizeInBytes() { return (int)p.getWavepacketPacketSize(); }

    /**
     * @return "Return Point Waveform Location" as a float.
     */
    public float getReturnPointWaveformLocation() { return p.getWavepacketReturnPointWaveformLocation();}

    /**
     * @return "X(t)" as a float.
     */
    public float getXt() { return p.getWavepacketParametricDx(); }

    /**
     * @return "Y(t)" as a float.
     */
    public float getYt() { return p.getWavepacketParametricDy(); }

    /**
     * @return "Z(t)" as a float.
     */
    public float getZt() { return p.getWavepacketParametricDz(); }
}
