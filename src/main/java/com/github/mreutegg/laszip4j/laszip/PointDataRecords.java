/*
 * Copyright 2022 Marcel Reutegger
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
package com.github.mreutegg.laszip4j.laszip;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.I16_QUANTIZE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I8_CLAMP;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

abstract class PointDataRecord {

    // For internal use only
    public int CompressionContext = 0;
}

enum ClassificationFlag
{
    Synthetic,
    KeyPoint,
    Withheld,
    Overlap
}

enum ScanFlag
{
    ScanDirection,
    EdgeOfFlightLine
}

abstract class PointDataRecordXYZBase extends PointDataRecord {

    public int X = 0;
    public int Y = 0;
    public int Z = 0;
    public char Intensity = 0;
    public short Classification = 0;
    public short UserData = 0;
    public char PointSourceID = 0;

    abstract byte getReturnNumber();
    abstract void setReturnNumber(byte returnNumber);
    abstract void setNumberOfReturns(byte numberOfReturns);
    abstract byte getNumberOfReturns();
    
    abstract void setScanDirection(boolean isPositive);
    abstract void setEdgeOfFlightLine(boolean isAtEndOfScan);

    abstract byte getScanAngleRank();
    abstract float getScanAngle();

    abstract boolean hasClassificationFlag(ClassificationFlag flag);
    abstract boolean hasScanFlag(ScanFlag flag);
}

interface IGpsTimeProvider {
    double getGpsTime();
    void setGpsTime(double val);
}

class PointDataRecordPoint10 extends PointDataRecordXYZBase {

    public PointDataRecordPoint10() {}

    public PointDataRecordPoint10(PointDataRecordPoint10 other) {

        this.X = other.X;
        this.Y = other.Y;
        this.Z = other.Z;
        this.Intensity = other.Intensity;
        this.Flags = other.Flags;
        this.Classification = other.Classification;
        this.ScanAngleRank = other.ScanAngleRank;
        this.UserData = other.UserData;
        this.PointSourceID =other.PointSourceID;    
    }

    public byte Flags = 0;
    public byte ScanAngleRank = 0;

    @Override
    public void setReturnNumber(byte returnNumber) {
        Flags &= (~ 0b00000111);  // 3 bits, 0..2
        Flags |= returnNumber & 0b0111;
    }

    @Override
    public byte getReturnNumber() {
        return (byte)(Flags & 0b0111);
    }

    @Override
    public void setNumberOfReturns(byte numberOfReturns) {
        Flags &= (~ 0b00111000);   // 3 bits, 3..5
        Flags |= ((int)numberOfReturns << 3) & 0b00111000;
    }

    @Override
    public byte getNumberOfReturns() {
        return (byte)((Flags >>> 3) & 0b00000111);
    }    

    @Override
    public void setScanDirection(boolean isPositive) {
        Flags &= (~ 0b01000000);
        Flags |= (isPositive?1:0) << 6;  // 0b00000001 => 0b01000000
    }

    @Override
    public void setEdgeOfFlightLine(boolean isAtEndOfScan) {
        Flags &= (~ 0b10000000);
        Flags |= (isAtEndOfScan?1:0) << 7;  // 0b00000001 => 0b10000000
    }

    @Override
    public byte getScanAngleRank(){return this.ScanAngleRank;}

    @Override
    public float getScanAngle()
    {
        return I16_QUANTIZE( (float)this.ScanAngleRank / 0.006f );
    }

    @Override
    public String toString()
    {
        return String.format("Point10: %d %d %d %d %d %d %d %d %d ", X, Y, Z, (int)Intensity, Classification, ScanAngleRank, UserData, Flags, (int)PointSourceID);    
    }

    /* In the Point10 format, the classification flags are in bits 5, 6, and 7 of the Classification byte. */
    @Override
    public boolean hasClassificationFlag(ClassificationFlag flag)
    {
        switch(flag)
        {
            case Synthetic: return (Classification & 0b00100000) == 0b00100000; // true if bit 5 is set
            case KeyPoint: return (Classification & 0b01000000) == 0b01000000; // true if bit 6 is set;
            case Withheld: return (Classification & 0b10000000) == 0b10000000; // true if bit 7 is set;
            case Overlap: return false; // This flag did not exist in this version
            default:
                return false;
        }
    }

    /* In the Point10 format, the scan flags are in bits 6 and 7 of the flags byte (byte 15). */
    @Override
    public boolean hasScanFlag(ScanFlag flag)
    {
        switch(flag)
        {
            case ScanDirection: return (Flags & 0b01000000) == 0b01000000; // true if bit 6 is set
            case EdgeOfFlightLine: return (Flags & 0b10000000) == 0b10000000; // true if bit 7 is set;
            default:
                return false;
        }
    }
}

class PointDataRecordGpsTime extends PointDataRecord implements IGpsTimeProvider {

    public long GPSTime = 0;

    public double getGPSTimeAsDouble() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putLong(this.GPSTime);

        return byteBuffer.getDouble(0);
    }

    @Override
    public double getGpsTime() { return getGPSTimeAsDouble(); }

    @Override
    public void setGpsTime(double val) { setGPSTimeFromDouble(val); }

    void setGPSTimeFromDouble(double fromDouble) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putDouble(fromDouble);

        this.GPSTime = byteBuffer.getLong(0);
    }
    
    @Override
    public String toString()
    {
        return String.format("GpsTime: %f", getGPSTimeAsDouble());    
    }
}

class PointDataRecordRGB extends PointDataRecord {

    public char R = 0;
    public char G = 0;
    public char B = 0;

    public PointDataRecordRGB() {
    }

    public PointDataRecordRGB(PointDataRecordRGB other) {
        this.R = other.R;
        this.G = other.G;
        this.B = other.B;
    }

    public char[] getRGB() {
        return new char[]{(char)R,(char)G,(char)B};
    }

    @Override
    public String toString()
    {
        return String.format("RGB: %d %d %d", (int)R, (int)G, (int)B);
    }
}

class PointDataRecordWavepacket extends PointDataRecord {

    public PointDataRecordWavepacket(){}

    public PointDataRecordWavepacket(PointDataRecordWavepacket other) {
        this.DescriptorIndex = other.DescriptorIndex;
        this.OffsetToWaveformData = other.OffsetToWaveformData;
        this.PacketSize = other.PacketSize;
        this.ReturnPointWaveformLocation = other.ReturnPointWaveformLocation;
        this.ParametricDx = other.ParametricDx;
        this.ParametricDy = other.ParametricDy;
        this.ParametricDz = other.ParametricDz;
    }

    public short DescriptorIndex = 0;
    public long OffsetToWaveformData = 0;
    public long PacketSize = 0;
    public float ReturnPointWaveformLocation = 0.0f;
    public float ParametricDx = 0.0f;
    public float ParametricDy = 0.0f;
    public float ParametricDz = 0.0f;

    public int getReturnPointWaveformLocationAsInt() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putFloat(this.ReturnPointWaveformLocation);

        return byteBuffer.getInt(0);
    }

    public void setReturnPointWaveformLocation(int value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(value);

        ReturnPointWaveformLocation =  byteBuffer.getFloat(0);
    }

    public int getDxAsInt() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putFloat(this.ParametricDx);

        return byteBuffer.getInt(0);
    }

    public int getDyAsInt() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putFloat(this.ParametricDy);

        return byteBuffer.getInt(0);
    }

    public int getDzAsInt() {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Float.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putFloat(this.ParametricDz);

        return byteBuffer.getInt(0);
    }

    public void setDx(int value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(value);

        ParametricDx =  byteBuffer.getFloat(0);
    }    
    public void setDy(int value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(value);

        ParametricDy =  byteBuffer.getFloat(0);
    }    
    public void setDz(int value) {

        ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        byteBuffer.putInt(value);

        ParametricDz =  byteBuffer.getFloat(0);
    }    

    @Override
    public String toString()
    {
        return String.format("wave:%d %d %d %f %f %f %f", DescriptorIndex, OffsetToWaveformData, PacketSize, ReturnPointWaveformLocation, ParametricDx, ParametricDy, ParametricDz);
    }

}

class PointDataRecordPoint14 extends PointDataRecordXYZBase implements IGpsTimeProvider {
    
    public PointDataRecordPoint14() {}

    public PointDataRecordPoint14(PointDataRecordPoint14 other) {

        this.X = other.X;
        this.Y = other.Y;
        this.Z = other.Z;
        this.Intensity = other.Intensity;
        this.ReturnFlags = other.ReturnFlags;
        this.ScanFlags = other.ScanFlags;
        this.Classification = other.Classification;
        this.UserData = other.UserData;
        this.ScanAngle = other.ScanAngle;
        this.PointSourceID =other.PointSourceID;    
        this.GPSTime = other.GPSTime;
    }

    public byte ReturnFlags = 0;
    public byte ScanFlags = 0;
    public short ScanAngle = 0;
    public long GPSTime = 0;

    // For internal use only
    public boolean gps_time_change = false;

    @Override
    public byte getReturnNumber() {
        return (byte)(ReturnFlags & 0b00001111);
    }

    @Override
    void setReturnNumber(byte numba) {
        ReturnFlags &= (~ 0b00001111);
        ReturnFlags |= numba & 0b00001111;
    }

    @Override
    byte getNumberOfReturns() {
        return (byte)((ReturnFlags >>> 4) & 0b00001111);
    }

    @Override
    void setNumberOfReturns(byte numberOfReturns) {
        ReturnFlags &= (~ 0b11110000);
        ReturnFlags |= (((int)numberOfReturns << 4) & 0b11110000);
    }

    byte getClassificationFlags() {
        return (byte)(ScanFlags & 0b00001111);
    }

    void setClassificationFlags(byte flags) {
        ScanFlags &= (~ 0b00001111);
        ScanFlags |= (flags & 0b00001111);
    }

    byte getScannerChannel() {
        return (byte)((ScanFlags >>> 4) & 0b00000011);
    }

    void setScannerChannel(byte channel) {
        ScanFlags &= (~ 0b00110000);
        ScanFlags |= ((channel << 4) & 0b00110000);
    }

    @Override
    void setScanDirection(boolean isPositive ) {
        ScanFlags &= (~ 0b01000000);
        ScanFlags |= ((isPositive?1:0) << 6) & 0b01000000;
    }

    @Override
    void setEdgeOfFlightLine(boolean isAtEndOfScan ) {
        ScanFlags &= (~ 0b10000000);
        ScanFlags |= ((isAtEndOfScan?1:0) << 7) & 0b10000000;
    }

    @Override
    public byte getScanAngleRank(){ return ((byte) I8_CLAMP(I16_QUANTIZE(0.006f*ScanAngle)));}

    @Override
    public float getScanAngle()
    {
        return this.ScanAngle;
    }

    /* In the Point14 format, the classification flags are in bits 0..3 of the Classification Flags bits (byte 16). */
    @Override
    public boolean hasClassificationFlag(ClassificationFlag flag)
    {
        switch(flag)
        {
            case Synthetic: return (ScanFlags & 0b00000001) == 0b00000001; // true if bit 0 is set
            case KeyPoint: return  (ScanFlags & 0b00000010) == 0b00000010; // true if bit 1 is set;
            case Withheld: return  (ScanFlags & 0b00000100) == 0b00000100; // true if bit 2 is set;
            case Overlap: return   (ScanFlags & 0b00001000) == 0b00001000; // true if bit 3 is set;
            default:
                return false;
        }
    }

    /* In the Point10 format, the scan flags are in bits 6 and 7 of the 'scan flags' byte (byte 16). */
    @Override
    public boolean hasScanFlag(ScanFlag flag)
    {
        switch(flag)
        {
            case ScanDirection: return (ScanFlags & 0b01000000) == 0b01000000; // true if bit 6 is set
            case EdgeOfFlightLine: return (ScanFlags & 0b10000000) == 0b10000000; // true if bit 7 is set;
            default:
                return false;
        }
    }

    @Override
    public String toString() {
        return String.format("Point14(%d): %.14g %d %d %d %d %d %d %d\n", 
          this.CompressionContext, 
          this.getGpsTime(), 
          this.X, 
          this.Y, 
          this.Z, 
          (int)this.Intensity, 
          this.getReturnNumber(), 
          this.getNumberOfReturns(),
          this.UserData);
    }

    @Override
    public double getGpsTime() 
    { 
        ByteBuffer buf = ByteBuffer.allocate(Long.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buf.putLong(this.GPSTime);
        return buf.getDouble(0); 
    }

    @Override
    public void setGpsTime(double val) 
    { 
        ByteBuffer buf = ByteBuffer.allocate(Double.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buf.putDouble(val);
        this.GPSTime = buf.getLong(0); 
    }
}

class PointDataRecordRgbNIR extends PointDataRecordRGB {

    public char NIR = 0;

    public PointDataRecordRgbNIR() {

    }

    public PointDataRecordRgbNIR(PointDataRecordRgbNIR other) {
        super(other);
        this.NIR = other.NIR;
    }

    @Override
    public String toString()
    {
        return String.format("%s %d", super.toString(), (int)NIR);
    }
}

class PointDataRecordBytes extends PointDataRecord {

    public PointDataRecordBytes(int byteCount) {
        Bytes = new byte[byteCount];
    }

    public PointDataRecordBytes(PointDataRecordBytes other) {
        this.Bytes = new byte[other.Bytes.length];
        System.arraycopy(other.Bytes, 0, this.Bytes, 0, other.Bytes.length);
    }
    public byte[] Bytes;

    @Override
    public String toString()
    {
        return new String(Bytes);
    }    
}