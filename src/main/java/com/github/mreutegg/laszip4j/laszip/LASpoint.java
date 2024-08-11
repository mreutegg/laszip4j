/*
 * Copyright 2007-2015, martin isenburg, rapidlasso - fast tools to catch reality
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

import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_NONE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASpoint {

    public PointDataRecord[] PointRecords;
    public LASquantizer quantizer;
    public LASattributer attributer;

    public char num_items; // unsigned
    public LASitem[] items;

    public LASpoint()
    {
    }
    
    public LASpoint(LASpoint other)
    {
        this.PointRecords = other.PointRecords;
    }

    // these functions set the desired point format (and maybe add on attributes in extra bytes)

    public boolean init(LASquantizer quantizer, byte point_type, char point_size, LASattributer attributer)
    {
        items = LasItemsFactory.getItems(point_type, point_size, LASZIP_COMPRESSOR_NONE);
        num_items = (char)items.length;
        return init(quantizer, num_items, items, attributer);
    };

    public boolean init(LASquantizer quantizer, int u_num_items, LASitem[] items, LASattributer attributer)
    {
        this.num_items = (char)u_num_items;
        this.items = items;

        PointRecords = new PointDataRecord[num_items];

        for (int i = 0; i < num_items; i++)
        {
            switch (items[i].type)
            {
                case POINT10:
                    PointRecords[i] = new PointDataRecordPoint10();
                    break;
                case POINT14:
                    PointRecords[i] = new PointDataRecordPoint14();
                    break;
                case GPSTIME11:
                    PointRecords[i] = new PointDataRecordGpsTime();
                    break;
                case RGBNIR14:
                    PointRecords[i] = new PointDataRecordRgbNIR();
                    break;
                case RGB12:
                case RGB14:
                    PointRecords[i] = new PointDataRecordRGB();
                    break;
                case WAVEPACKET13:
                case WAVEPACKET14:
                    PointRecords[i] = new PointDataRecordWavepacket();
                    break;
                case BYTE:
                case BYTE14:
                    PointRecords[i] = new PointDataRecordBytes(items[i].size);
                    break;
                default:
                    return FALSE;
            }
        }
        this.quantizer = quantizer;
        this.attributer = attributer;
        return TRUE;
    }

    public boolean inside_rectangle(double r_min_x, double r_min_y, double r_max_x, double r_max_y)
    {
        double xy;
        xy = get_x();
        if (xy < r_min_x || xy >= r_max_x) return FALSE;
        xy = get_y();
        if (xy < r_min_y || xy >= r_max_y) return FALSE;
        return TRUE;
    }

    public boolean inside_tile(float ll_x, float ll_y, float ur_x, float ur_y)
    {
        double xy;
        xy = get_x();
        if (xy < ll_x || xy >= ur_x) return FALSE;
        xy = get_y();
        if (xy < ll_y || xy >= ur_y) return FALSE;
        return TRUE;
    }

    public boolean inside_circle(double center_x, double center_y, double squared_radius)
    {
        double dx = center_x - get_x();
        double dy = center_y - get_y();
        return ((dx*dx+dy*dy) < squared_radius);
    }

    public boolean inside_box(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z)
    {
        double xyz;
        xyz = get_x();
        if (xyz < min_x || xyz >= max_x) return FALSE;
        xyz = get_y();
        if (xyz < min_y || xyz >= max_y) return FALSE;
        xyz = get_z();
        if (xyz < min_z || xyz >= max_z) return FALSE;
        return TRUE;
    }

    boolean inside_bounding_box(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z)
    {
        double xyz;
        xyz = get_x();
        if (xyz < min_x || xyz > max_x) return FALSE;
        xyz = get_y();
        if (xyz < min_y || xyz > max_y) return FALSE;
        xyz = get_z();
        if (xyz < min_z || xyz > max_z) return FALSE;
        return TRUE;
    }

    public boolean is_first() { return get_return_number() <= 1; };
    public boolean is_intermediate() { return (!is_first() && !is_last()); };
    public boolean is_last() { return get_return_number() >= get_number_of_returns(); };
    public boolean is_single() { return get_number_of_returns() <= 1; };

    public boolean is_first_of_many() { return !is_single() && is_first(); };
    public boolean is_last_of_many() { return !is_single() && is_last(); };

    public int get_X() { return getX(); };
    public int get_Y() { return getY(); };
    public int get_Z() { return getZ(); };
    public int get_intensity() { return getIntensity(); }; // unsigned
    public short get_return_number() { return getReturn_number(); };
    public short get_number_of_returns() { return getNumber_of_returns(); };
    public byte get_scan_direction_flag() { return getScan_direction_flag(); };
    public byte get_edge_of_flight_line() { return getEdge_of_flight_line(); };
    public short get_classification() { return getClassification(); };
    public byte get_synthetic_flag() { return getSynthetic_flag(); };
    public byte get_keypoint_flag() { return getKeypoint_flag(); };
    public byte get_withheld_flag() { return getWithheld_flag(); };
    public byte get_extended_overlap_flag() { return getOverlap_flag();}
    public byte get_scan_angle_rank() { return getScan_angle_rank(); };
    public short get_user_data() { return getUser_data(); };
    public int get_point_source_ID() { return getPoint_source_ID(); }; // unsigned
    public double get_gps_time() { return getGps_time(); };
    public char[] get_rgb() { return getRgb(); };
    public char get_R() { return getRgb(0); }; // unsigned
    public char get_G() { return getRgb(1); }; // unsigned
    public char get_B() { return getRgb(2); }; // unsigned
    public char get_I() { return getRgb(3); }; // unsigned

    public void set_X(int X) { this.setX(X); };
    public void set_Y(int Y) { this.setY(Y); };
    public void set_Z(int Z) { this.setZ(Z); };
    public void set_intensity(char intensity) { this.setIntensity(intensity); }; // unsigned
    public void set_return_number(byte return_number) { this.setReturn_number((return_number > 7 ? 7 : return_number)); };
    public void set_number_of_returns(byte number_of_returns) { this.setNumber_of_returns((number_of_returns > 7 ? 7 : number_of_returns)); };
    public void set_scan_direction_flag(byte scan_direction_flag) { this.setScan_direction_flag(scan_direction_flag); };
    public void set_edge_of_flight_line(byte edge_of_flight_line) { this.setEdge_of_flight_line(edge_of_flight_line); };
    public void set_classification(byte classification) { this.setClassification((byte) (classification & 31)); };
    public void set_user_data(short user_data) { this.setUser_data(user_data); };
    public void set_point_source_ID(char point_source_ID) { this.setPoint_source_ID(point_source_ID); }; // unsigned
    public void set_gps_time(double gps_time) { this.setGps_time(gps_time); };
    public void set_RGB(char[] rgb) { this.setRgb(rgb); };
    public void set_RGBI(char[] rgb) { this.setRgb(rgb); };
    public void set_R(char R) { this.setRgb(0, R); };
    public void set_G(char G) { this.setRgb(1, G); };
    public void set_B(char B) { this.setRgb(2, B); };
    public void set_I(char I) { this.setRgb(3, I); };

    public double get_x() { return quantizer.get_x(getX()); };
    public double get_y() { return quantizer.get_y(getY()); };
    public double get_z() { return quantizer.get_z(getZ()); };

    public void set_x(double x) { this.setX(quantizer.get_X(x)); };
    public void set_y(double y) { this.setY(quantizer.get_Y(y)); };
    public void set_z(double z) { this.setZ(quantizer.get_Z(z)); };

    // generic functions for attributes in extra bytes

    public boolean has_attribute(int index)
    {
        if (attributer != null)
        {
            if (index < attributer.number_attributes)
            {
                return TRUE;
            }
        }
        return FALSE;
    };

    public boolean get_attribute(int index, byte[] data)
    {
        PointDataRecord bytesRecord = getPointDataRecord(PointDataRecordBytes.class);
        if (has_attribute(index) && null != bytesRecord)
        {
            System.arraycopy(((PointDataRecordBytes)bytesRecord).Bytes, attributer.attribute_starts.get(index), data, 0, attributer.attribute_sizes.get(index));
            return TRUE;
        }
        return FALSE;
    };

    public boolean set_attribute(int index, byte[] data)
    {
        PointDataRecord bytesRecord = getPointDataRecord(PointDataRecordBytes.class);
        if (has_attribute(index) && null != bytesRecord)
        {
            System.arraycopy(data, 0, ((PointDataRecordBytes)bytesRecord).Bytes, attributer.attribute_starts.get(index), attributer.attribute_sizes.get(index));
            return TRUE;
        }
        return FALSE;
    };

    // typed and offset functions for attributes in extra bytes (more efficient)

    public byte get_attributeUByte(int start) { return getExtraBytes()[start]; };
    public void set_attributeU(int start, byte data) { getExtraBytes()[start] = data; };
    public byte get_attributeByte(int start) { return getExtraBytes()[start]; };
    public void set_attribute(int start, byte data) { getExtraBytes()[start] = data; };
    public char get_attributeChar(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getChar(start); };
    public void set_attribute(int start, char data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putChar(start, data); };
    public short get_attributeShort(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getShort(start); };
    public void set_attribute(int start, short data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putShort(start, data); };
    public int get_attributeUInt(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getInt(start); };
    public void set_attributeU(int start, int data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putInt(start, data); };
    public int get_attributeInt(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getInt(start); };
    public void set_attribute(int start, int data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putInt(start, data); };
    public long get_attributeULong(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getLong(start); };
    public void set_attributeU(int start, long data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putLong(start, data); };
    public long get_attributeLong(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getLong(start); };
    public void set_attribute(int start, long data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putLong(start, data); };
    public float get_attributeFloat(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getFloat(start); };
    public void set_attribute(int start, float data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putFloat(start, data); };
    public double get_attributeDouble(int start) { return ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).getDouble(start); };
    public void set_attribute(int start, double data) { ByteBuffer.wrap(getExtraBytes()).order(ByteOrder.LITTLE_ENDIAN).putDouble(start, data); };

    public int getX() { return getPointXYZ().X; }
    public void setX(int x) { getPointXYZ().X = x; }
    public int getY() { return getPointXYZ().Y; };
    public void setY(int y) { getPointXYZ().Y = y; }
    public int getZ() { return getPointXYZ().Z; }
    public void setZ(int z) { getPointXYZ().Z = z; }
    public char getIntensity() { return getPointXYZ().Intensity; }
    public void setIntensity(char intensity) { getPointXYZ().Intensity = intensity; }
    public byte getReturn_number() { return getPointXYZ().getReturnNumber(); }
    public void setReturn_number(byte return_number) { getPointXYZ().setReturnNumber(return_number); }
    public byte getNumber_of_returns() { return getPointXYZ().getNumberOfReturns(); }
    public void setNumber_of_returns(byte number_of_returns) { getPointXYZ().setNumberOfReturns(number_of_returns); }
    public byte getScan_direction_flag() { return getPointXYZ().hasScanFlag( ScanFlag.ScanDirection )?(byte)1:(byte)0;}
    public void setScan_direction_flag(byte scan_direction_flag) { getPointXYZ().setScanDirection(scan_direction_flag == 1); }
    public byte getEdge_of_flight_line() { return getPointXYZ().hasScanFlag(ScanFlag.EdgeOfFlightLine)?(byte)1:(byte)0; }
    public void setEdge_of_flight_line(byte edge_of_flight_line) { getPointXYZ().setEdgeOfFlightLine(edge_of_flight_line == 1); }
    public short getClassification() { return getPointXYZ().Classification; }
    public void setClassification(short classification) { getPointXYZ().Classification = classification; }
    public byte getSynthetic_flag() { return getPointXYZ().hasClassificationFlag(ClassificationFlag.Synthetic)?(byte)1:(byte)0; }
    public byte getKeypoint_flag() { return getPointXYZ().hasClassificationFlag(ClassificationFlag.KeyPoint)?(byte)1:(byte)0; }
    public byte getWithheld_flag() { return getPointXYZ().hasClassificationFlag(ClassificationFlag.Withheld)?(byte)1:(byte)0; }
    public void setWithheld_flag(boolean withheld) { getPointXYZ().setClassificationFlag(ClassificationFlag.Withheld, withheld); }
    public byte getOverlap_flag() { return getPointXYZ().hasClassificationFlag(ClassificationFlag.Overlap)?(byte)1:(byte)0; }
    public byte getScan_angle_rank() { return getPointXYZ().getScanAngleRank(); }
    public float getScan_angle() { return getPointXYZ().getScanAngle(); }
    public short getUser_data() { return getPointXYZ().UserData; }
    public void setUser_data(short user_data) { getPointXYZ().UserData = user_data; }
    public char getPoint_source_ID() { return getPointXYZ().PointSourceID; }
    public void setPoint_source_ID(char point_source_ID) { getPointXYZ().PointSourceID = point_source_ID; }
    public boolean haveGpsTime() { return null != getGpsTimeProvider(); }
    public double getGps_time() { return getGpsTimeProvider().getGpsTime(); }
    public void setGps_time(double gps_time) { getGpsTimeProvider().setGpsTime(gps_time); }

    public char getRgb(int index) {

        PointDataRecordRGB rgbRecord = getPointRGB();
        switch(index) {
            case 0: return rgbRecord.R;
            case 1: return rgbRecord.G;
            case 2: return rgbRecord.B;
            default: return 0;
        }
    }

    public char[] getRgb() { return getPointRGB().getRGB(); }

    public void setRgb(int index, char value) {

        switch(index) {
            case 0: getPointRGB().R = value; break;
            case 1: getPointRGB().G = value; break;
            case 2: getPointRGB().B = value; break;
            default:
            break;
        }
    }

    public void setRgb(char[] rgb) {

        PointDataRecordRGB rgbRecord = getPointRGB();
        rgbRecord.R = rgb[0];
        rgbRecord.G = rgb[1];
        rgbRecord.B = rgb[2];
    }

    public boolean haveRgb() { return null != getPointRGB(); }
    public boolean haveNIR() { return null != getPointRGBNIR(); }

    public short getWavepacketDescriptorIndex() 
    { 
        PointDataRecordWavepacket wp = getWavepacket();
        return null == wp ? 0 : wp.DescriptorIndex; 
    }
    public long getWavepacketOffsetToWaveformData() { return getWavepacket().OffsetToWaveformData; }
    public long getWavepacketPacketSize() { return getWavepacket().PacketSize; }
    public float getWavepacketReturnPointWaveformLocation(){ return getWavepacket().ReturnPointWaveformLocation; }
    public float getWavepacketParametricDx() { return getWavepacket().ParametricDx;}
    public float getWavepacketParametricDy() { return getWavepacket().ParametricDy;}
    public float getWavepacketParametricDz() { return getWavepacket().ParametricDz;}

    public void setWavepacketOffsetToWaveformData(long offset) { getWavepacket().OffsetToWaveformData = offset;}
    public void setWavepacketPacketSize(long size) { getWavepacket().PacketSize = size; }
    public void setWavepacketReturnPointWaveformLocation(float val) { getWavepacket().ReturnPointWaveformLocation = val; }
    public void setWavepacketParametricDx( float val ){ getWavepacket().ParametricDx = val; }
    public void setWavepacketParametricDy( float val ){ getWavepacket().ParametricDy = val; }
    public void setWavepacketParametricDz( float val ){ getWavepacket().ParametricDz = val; }

    public boolean haveWavepacket() { return null != getWavepacket(); }

    @Override
    public String toString() 
    {
        String result = "";
        for(PointDataRecord r : PointRecords)
        {
            result += r.toString();
        }
        return result;
    }
    


    private PointDataRecord getPointDataRecord(Class<? extends PointDataRecord> ofThisType )
    {
        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == ofThisType)
                return r;
        }

        return null;
    }

    private PointDataRecordXYZBase getPointXYZ()
    {
        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == PointDataRecordPoint14.class)
                return (PointDataRecordXYZBase)r;
        }

        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == PointDataRecordPoint10.class)
                return (PointDataRecordXYZBase)r;
        }

        return null;
    }

    private PointDataRecordRGB getPointRGB()
    {
        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == PointDataRecordRgbNIR.class)
                return (PointDataRecordRGB)r;
        }

        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == PointDataRecordRGB.class)
                return (PointDataRecordRGB)r;
        }

        return null;
    }

    private PointDataRecordRgbNIR getPointRGBNIR()
    {
        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == PointDataRecordRgbNIR.class)
                return (PointDataRecordRgbNIR)r;
        }

        return null;
    }
    
    private IGpsTimeProvider getGpsTimeProvider()
    {
        for(PointDataRecord  r : PointRecords)
        {
            if (r instanceof IGpsTimeProvider)
                return (IGpsTimeProvider)r;
        }

        return null;
    }

    private PointDataRecordWavepacket getWavepacket()
    {
        for(PointDataRecord  r : PointRecords)
        {
            if (null != r && r.getClass() == PointDataRecordWavepacket.class)
                return (PointDataRecordWavepacket)r;
        }

        return null;
    }

    private byte[] getExtraBytes() {

        PointDataRecord bytesRecord = getPointDataRecord(PointDataRecordBytes.class);

        return null == bytesRecord ? null : ((PointDataRecordBytes)bytesRecord).Bytes;
    }

}
