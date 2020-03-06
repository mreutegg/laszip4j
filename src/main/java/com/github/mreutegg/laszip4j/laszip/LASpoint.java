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

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_NONE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Integer.compareUnsigned;

public class LASpoint {

    private static final PrintStream stderr = System.err;

    // these fields contain the data that describe each point

    /**
     * I32 X;                                   0
     * I32 Y;                                   4
     * I32 Z;                                   8
     * U16 intensity;                           12
     * U8 return_number : 3;                    14
     * U8 number_of_returns : 3;                14
     * U8 scan_direction_flag : 1;              14
     * U8 edge_of_flight_line : 1;              14
     * U8 classification : 5;                   15
     * U8 synthetic_flag : 1;                   15
     * U8 keypoint_flag  : 1;                   15
     * U8 withheld_flag  : 1;                   15
     * I8 scan_angle_rank;                      16
     * U8 user_data;                            17
     * U16 point_source_ID;                     18
     * // LAS 1.4 only
     * I16 extended_scan_angle;                 20
     * U8 extended_point_type : 2;              22
     * U8 extended_scanner_channel : 2;         22
     * U8 extended_classification_flags : 4;    22
     * U8 extended_classification;              23
     * U8 extended_return_number : 4;           24
     * U8 extended_number_of_returns : 4;       24
     * // for 8 byte alignment of the GPS time
     * U8 dummy[3];                             25
     * // LASlib only
     * U32 deleted_flag;                        28
     * F64 gps_time;                            32
     */
    private final ByteBuffer point10 = ByteBuffer.allocate(40);

    private ByteBuffer gps_time = ByteBuffer.allocate(8); // double
    // 3 rgb values & 1 nir value
    private ByteBuffer rgb = ByteBuffer.allocate(8); // 4 U16 (char)
    public LASwavepacket wavepacket = new LASwavepacket();

    public byte[] extra_bytes;

    // for converting between x,y,z integers and scaled/translated coordinates

    public LASquantizer quantizer;
    public double[] coordinates = new double[3];

    // for attributed access to the extra bytes

    public LASattributer attributer;

    // this field provides generic access to the point data

    public ByteBuffer[] point;

    // these fields describe the point format LAS specific

    public boolean have_gps_time;
    public boolean have_gps_time11;
    public boolean have_rgb;
    public boolean have_nir;
    public boolean have_wavepacket;
    public int extra_bytes_number;
    public int total_point_size; // unsigned

    // these fields describe the point format terms of generic items

    public char num_items; // unsigned
    public LASitem[] items;

    // copy functions

    public LASpoint(LASpoint other)
    {
        setX(other.getX());
        setY(other.getY());
        setZ(other.getZ());
        setIntensity(other.getIntensity());
        setReturn_number(other.getReturn_number());
        setNumber_of_returns(other.getNumber_of_returns());
        setScan_direction_flag(other.getScan_direction_flag());
        setEdge_of_flight_line(other.getEdge_of_flight_line());
        setClassification(other.getClassification());
        setSynthetic_flag(other.getSynthetic_flag());
        setKeypoint_flag(other.getKeypoint_flag());
        setWithheld_flag(other.getWithheld_flag());
        setScan_angle_rank(other.getScan_angle_rank());
        setUser_data(other.getUser_data());
        setPoint_source_ID(other.getPoint_source_ID());
        setDeleted_flag(other.getDeleted_flag());

        if (other.have_gps_time)
        {
            setGps_time(other.getGps_time());
        }
        if (other.have_rgb)
        {
            setRgb(0, other.getRgb(0));
            setRgb(1, other.getRgb(1));
            setRgb(2, other.getRgb(2));
            if (other.have_nir)
            {
                setRgb(3, other.getRgb(3));
            }
        }
        if (other.have_wavepacket)
        {
            wavepacket = other.wavepacket;
        }
        if (other.extra_bytes != null)
        {
            extra_bytes = new byte[other.extra_bytes.length];
            System.arraycopy(other.extra_bytes, 0, extra_bytes, 0, other.extra_bytes_number);
            extra_bytes_number = other.extra_bytes_number;
        }
        if (other.getExtended_point_type() != 0)
        {
            setExtended_classification(other.getExtended_classification());
            setExtended_classification_flags(other.getExtended_classification_flags());
            setExtended_number_of_returns(other.getExtended_number_of_returns());
            setExtended_return_number(other.getExtended_return_number());
            setExtended_scan_angle(other.getExtended_scan_angle());
            setExtended_scanner_channel(other.getExtended_scanner_channel());
        }
    };

    void copy_to(byte[] buffer)
    {
        int i;
        int b = 0;
        for (i = 0; i < num_items; i++)
        {
            System.arraycopy(point[i].array(), 0, buffer, b, items[i].size);
            b += items[i].size;
        }
    };

    void copy_from(byte[] buffer)
    {
        int i;
        int b = 0;
        for (i = 0; i < num_items; i++)
        {
            point[i] = ByteBuffer.allocate(items[i].size);
            System.arraycopy(buffer, b, point[i].array(), 0, point[i].capacity());
            b += items[i].size;
        }
    };

// these functions set the desired point format (and maybe add on attributes in extra bytes)

    public boolean init(LASquantizer quantizer, byte point_type, char point_size, LASattributer attributer)
    {
        // clean the point

        clean();

        // switch over the point types we know
        char[] _num_items = new char[1];
        LASitem[][] _items = new LASitem[1][];
        if (!new LASzip().setup(_num_items, _items, point_type, point_size, LASZIP_COMPRESSOR_NONE))
        {
            fprintf(stderr,"ERROR: unknown point type %d with point size %d\n", (int)point_type, (int)point_size);
            return FALSE;
        }
        num_items = _num_items[0];
        items = _items[0];

        // create point's item pointers

        point = new ByteBuffer[num_items];

        int i;
        for (i = 0; i < num_items; i++)
        {
            total_point_size += items[i].size;
            switch (items[i].type)
            {
                case POINT14:
                    have_gps_time = TRUE;
                    setExtended_point_type((byte) 1);
                case POINT10:
                    this.point[i] = ByteBuffer.wrap(this.point10.array());
                    break;
                case GPSTIME11:
                    have_gps_time = TRUE;
                    have_gps_time11 = TRUE;
                    this.point[i] = ByteBuffer.wrap(this.gps_time.array());
                    break;
                case RGBNIR14:
                    have_nir = TRUE;
                case RGB12:
                    have_rgb = TRUE;
                    this.point[i] = ByteBuffer.wrap(this.rgb.array());
                    break;
                case WAVEPACKET13:
                    have_wavepacket = TRUE;
                    this.point[i] = ByteBuffer.wrap(this.wavepacket.asByteArray());
                    break;
                case BYTE:
                    extra_bytes_number = items[i].size;
                    extra_bytes = new byte[extra_bytes_number];
                    this.point[i] = ByteBuffer.wrap(extra_bytes);
                    break;
                default:
                    return FALSE;
            }
        }
        this.quantizer = quantizer;
        this.attributer = attributer;
        return TRUE;
    };

    public boolean init(LASquantizer quantizer, int u_num_items, LASitem[] items, LASattributer attributer)
    {
        int u_i;

        // clean the point

        clean();

        // create item description

        this.num_items = (char) u_num_items;
        this.items = new LASitem[u_num_items];
        this.point = new ByteBuffer[u_num_items];

        for (u_i = 0; compareUnsigned(u_i, u_num_items) < 0; u_i++)
        {
            this.items[u_i] = items[u_i];
            total_point_size += items[u_i].size;
            switch (items[u_i].type)
            {
                case POINT14:
                    have_gps_time = TRUE;
                    setExtended_point_type((byte) 1);
                case POINT10:
                    this.point[u_i] = ByteBuffer.wrap(this.point10.array());
                    break;
                case GPSTIME11:
                    have_gps_time = TRUE;
                    have_gps_time11 = TRUE;
                    this.point[u_i] = ByteBuffer.wrap(this.gps_time.array());
                    break;
                case RGBNIR14:
                    have_nir = TRUE;
                case RGB12:
                    have_rgb = TRUE;
                    this.point[u_i] = ByteBuffer.wrap(this.rgb.array());
                    break;
                case WAVEPACKET13:
                    have_wavepacket = TRUE;
                    this.point[u_i] = ByteBuffer.wrap(this.wavepacket.asByteArray());
                    break;
                case BYTE:
                    extra_bytes_number = items[u_i].size;
                    extra_bytes = new byte[extra_bytes_number];
                    this.point[u_i] = ByteBuffer.wrap(extra_bytes);
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

    boolean is_zero()
    {
        if (this.getX() != 0)
        {
            return FALSE;
        }
        if (have_gps_time)
        {
            if (this.getGps_time() != 0.0)
            {
                return FALSE;
            }
        }
        if (have_rgb)
        {
            if (this.getRgb(0) != 0 || this.getRgb(1) != 0 || this.getRgb(2) != 0)
            {
                return FALSE;
            }
            if (have_nir)
            {
                if (this.getRgb(3) != 0)
                {
                    return FALSE;
                }
            }
        }
        return TRUE;
    }

    void zero()
    {
        setX(0);
        setY(0);
        setZ(0);
        setIntensity((char) 0);
        setReturn_number((byte) 1);
        setNumber_of_returns((byte) 1);
        setScan_direction_flag((byte) 0);
        setEdge_of_flight_line((byte) 0);
        setClassification((byte) 0);
        setSynthetic_flag((byte) 0);
        setKeypoint_flag((byte) 0);
        setWithheld_flag((byte) 0);
        setScan_angle_rank((byte) 0);
        setUser_data((byte) 0);
        setPoint_source_ID((char) 0);

        // LAS 1.4 only
        setExtended_scan_angle((short) 0);
        setExtended_scanner_channel((byte) 0);
        setExtended_classification_flags((byte) 0);
        setExtended_classification((byte) 0);
        setExtended_return_number((byte) 1);
        setExtended_number_of_returns((byte) 1);

        // LASlib only
        setDeleted_flag(0);

        setGps_time(0.0);
        setRgb(0, (char) 0);
        setRgb(1, (char) 0);
        setRgb(2, (char) 0);
        setRgb(3, (char) 0);
        wavepacket.zero();
    };

    void clean()
    {
        zero();

        extra_bytes = null;

        point = null;

        have_gps_time = FALSE;
        have_gps_time11 = FALSE;
        have_rgb = FALSE;
        have_wavepacket = FALSE;
        have_nir = FALSE;
        extra_bytes_number = 0;
        total_point_size = 0;

        num_items = 0;
        items = null;

        // LAS 1.4 only
        setExtended_point_type((byte) 0);

    };

    public LASpoint()
    {
        extra_bytes = null;
        point = null;
        items = null;
        clean();
    };

    public boolean is_first() { return get_return_number() <= 1; };
    public boolean is_intermediate() { return (!is_first() && !is_last()); };
    public boolean is_last() { return get_return_number() >= get_number_of_returns(); };
    public boolean is_single() { return get_number_of_returns() <= 1; };

    public boolean is_first_of_many() { return !is_single() && is_first(); };
    public boolean is_last_of_many() { return !is_single() && is_last(); };

    public int get_X() { return getX(); };
    public int get_Y() { return getY(); };
    public int get_Z() { return getZ(); };
    public char get_intensity() { return getIntensity(); }; // unsigned
    public byte get_return_number() { return getReturn_number(); };
    public byte get_number_of_returns() { return getNumber_of_returns(); };
    public byte get_scan_direction_flag() { return getScan_direction_flag(); };
    public byte get_edge_of_flight_line() { return getEdge_of_flight_line(); };
    public byte get_classification() { return getClassification(); };
    public byte get_synthetic_flag() { return getSynthetic_flag(); };
    public byte get_keypoint_flag() { return getKeypoint_flag(); };
    public byte get_withheld_flag() { return getWithheld_flag(); };
    public byte get_scan_angle_rank() { return getScan_angle_rank(); };
    public byte get_user_data() { return getUser_data(); };
    public char get_point_source_ID() { return getPoint_source_ID(); }; // unsigned
    public int get_deleted_flag() { return getDeleted_flag(); }; // unsigned
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
    public void set_synthetic_flag(byte synthetic_flag) { this.setSynthetic_flag(synthetic_flag); };
    public void set_keypoint_flag(byte keypoint_flag) { this.setKeypoint_flag(keypoint_flag); };
    public void set_withheld_flag(byte withheld_flag) { this.setWithheld_flag(withheld_flag); };
    public void set_scan_angle_rank(byte scan_angle_rank) { this.setScan_angle_rank(scan_angle_rank); };
    public void set_user_data(byte user_data) { this.setUser_data(user_data); };
    public void set_point_source_ID(char point_source_ID) { this.setPoint_source_ID(point_source_ID); }; // unsigned
    public void set_deleted_flag(byte deleted_flag) { this.setDeleted_flag(Byte.toUnsignedInt(deleted_flag)); };
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

    public byte get_extended_classification() { return getExtended_classification(); };
    public byte get_extended_return_number() { return getExtended_return_number(); };
    public byte get_extended_number_of_returns() { return getExtended_number_of_returns(); };
    public short get_extended_scan_angle() { return getExtended_scan_angle(); };
    public byte get_extended_overlap_flag() { return (byte) (getExtended_classification_flags() >>> 3); };
    public byte get_extended_scanner_channel() { return getExtended_scanner_channel(); };

    public void set_extended_classification(byte u_extended_classification) { this.setExtended_classification(u_extended_classification); };
    public void set_extended_scan_angle(short u_extended_scan_angle) { this.setExtended_scan_angle(u_extended_scan_angle); };
    public void set_extended_overlap_flag(byte u_extended_overlap_flag) { this.setExtended_classification_flags((byte)((u_extended_overlap_flag << 3) | (this.getExtended_classification_flags() & 7))); };
    public void set_extended_scanner_channel(byte u_extended_scanner_channel) { this.setExtended_scanner_channel(u_extended_scanner_channel); };

    public float get_scan_angle() { if (getExtended_point_type() != 0) return 0.006f* getExtended_scan_angle(); else return (float) getScan_angle_rank(); };
    public float get_abs_scan_angle() { if (getExtended_point_type() != 0) return (getExtended_scan_angle() < 0 ? -0.006f* getExtended_scan_angle() : 0.006f* getExtended_scan_angle()) ; else return (getScan_angle_rank() < 0 ? (float)-getScan_angle_rank() : (float) getScan_angle_rank()); };

    public void compute_coordinates()
    {
        coordinates[0] = get_x();
        coordinates[1] = get_y();
        coordinates[2] = get_z();
    };

    public void compute_XYZ()
    {
        set_x(coordinates[0]);
        set_y(coordinates[1]);
        set_z(coordinates[2]);
    };

    public void compute_XYZ(LASquantizer quantizer)
    {
        setX(quantizer.get_X(coordinates[0]));
        setY(quantizer.get_Y(coordinates[1]));
        setZ(quantizer.get_Z(coordinates[2]));
    };

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
        if (has_attribute(index))
        {
            System.arraycopy(extra_bytes, attributer.attribute_starts.get(index), data, 0, attributer.attribute_sizes.get(index));
            return TRUE;
        }
        return FALSE;
    };

    public boolean set_attribute(int index, byte[] data)
    {
        if (has_attribute(index))
        {
            System.arraycopy(data, 0, extra_bytes, attributer.attribute_starts.get(index), attributer.attribute_sizes.get(index));
            return TRUE;
        }
        return FALSE;
    };

    public String get_attribute_name(int index)
    {
        if (has_attribute(index))
        {
            return MyDefs.stringFromByteArray(attributer.attributes.get(index).name);
        }
        return null;
    };

    public double get_attribute_as_float(int index)
    {
        if (has_attribute(index))
        {
            int start = attributer.attribute_starts.get(index);
            byte[] data = new byte[Math.min(8, extra_bytes.length - start)];
            ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).get(data, start, data.length);
            return attributer.attributes.get(index).get_value_as_float(data);
        }
        return 0.0;
    };

    // typed and offset functions for attributes in extra bytes (more efficient)

    public byte get_attributeUByte(int start) { return extra_bytes[start]; };
    public void set_attributeU(int start, byte data) { extra_bytes[start] = data; };
    public byte get_attributeByte(int start) { return extra_bytes[start]; };
    public void set_attribute(int start, byte data) { extra_bytes[start] = data; };
    public char get_attributeChar(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getChar(start); };
    public void set_attribute(int start, char data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putChar(start, data); };
    public short get_attributeShort(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getShort(start); };
    public void set_attribute(int start, short data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putShort(start, data); };
    public int get_attributeUInt(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(start); };
    public void set_attributeU(int start, int data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(start, data); };
    public int get_attributeInt(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getInt(start); };
    public void set_attribute(int start, int data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putInt(start, data); };
    public long get_attributeULong(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getLong(start); };
    public void set_attributeU(int start, long data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(start, data); };
    public long get_attributeLong(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getLong(start); };
    public void set_attribute(int start, long data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putLong(start, data); };
    public float get_attributeFloat(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getFloat(start); };
    public void set_attribute(int start, float data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putFloat(start, data); };
    public double get_attributeDouble(int start) { return ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).getDouble(start); };
    public void set_attribute(int start, double data) { ByteBuffer.wrap(extra_bytes).order(ByteOrder.LITTLE_ENDIAN).putDouble(start, data); };

    public byte[] asTwentyBytes() {

        ByteBuffer buffer = ByteBuffer.allocate(20).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(getX());
        buffer.putInt(getY());
        buffer.putInt(getZ());
        buffer.putChar(getIntensity());
        buffer.put((byte) (getReturn_number() & (getNumber_of_returns() << 3) & (getScan_direction_flag() << 6) & (getEdge_of_flight_line() << 7)));
        buffer.put((byte) (getClassification() & (getSynthetic_flag() << 5) & (getKeypoint_flag() << 6) & (getWithheld_flag() << 7)));
        buffer.put(getScan_angle_rank());
        buffer.put(getUser_data());
        buffer.putChar(getPoint_source_ID());
        return buffer.array();
    }

    private static byte[] toByteArray(char[] data) {
        byte[] bytes = new byte[data.length * 2];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        for (char c : data) {
            buffer.putChar(c);
        }
        return bytes;
    }

    private byte[] toByteArray(double data) {
        byte[] bytes = new byte[8];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putDouble(data);
        return bytes;
    }

    private byte[] toByteArray(int data) {
        byte[] bytes = new byte[4];
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(data);
        return bytes;
    }

    public int getX() {
        return point10.getInt(0);
    }

    public void setX(int x) {
        point10.putInt(0, x);
    }

    public int getY() {
        return point10.getInt(4);
    }

    public void setY(int y) {
        point10.putInt(4, y);
    }

    public int getZ() {
        return point10.getInt(8);
    }

    public void setZ(int z) {
        point10.putInt(8, z);
    }

    public char getIntensity() {
        return point10.getChar(12);
    }

    public void setIntensity(char intensity) {
        point10.putChar(12, intensity);
    }

    public byte getReturn_number() {
        byte b = point10.get(14);
        return (byte) (b & 0b0111);
    }

    public void setReturn_number(byte return_number) {
        byte b = point10.get(14);
        b &= (~ 0b0111);
        b |= (return_number & 0b0111);
        point10.put(14, b);
    }

    public byte getNumber_of_returns() {
        byte b = point10.get(14);
        return (byte) ((b >>> 3) & 0b0111);
    }

    public void setNumber_of_returns(byte number_of_returns) {
        byte b = point10.get(14);
        b &= (~ 0b00111000);
        b |= ((number_of_returns << 3) & 0b00111000);
        point10.put(14, b);
    }

    public byte getScan_direction_flag() {
        byte b = point10.get(14);
        return (byte) ((b >>> 6) & 0x1);
    }

    public void setScan_direction_flag(byte scan_direction_flag) {
        byte b = point10.get(14);
        b &= (~ 0b01000000);
        b |= ((scan_direction_flag << 6) & 0b01000000);
        point10.put(14, b);
    }

    public byte getEdge_of_flight_line() {
        byte b = point10.get(14);
        return (byte) ((b >>> 7) & 0x1);
    }

    public void setEdge_of_flight_line(byte edge_of_flight_line) {
        byte b = point10.get(14);
        b &= (~ 0b10000000);
        b |= ((edge_of_flight_line << 7) & 0b10000000);
        point10.put(14, b);
    }

    public byte getClassification() {
        byte b = point10.get(15);
        return (byte) (b & 0b00011111);
    }

    public void setClassification(byte classification) {
        byte b = point10.get(15);
        b &= (~ 0b00011111);
        b |= (classification & 0b00011111);
        point10.put(15, b);
    }

    public byte getSynthetic_flag() {
        byte b = point10.get(15);
        return (byte) ((b >>> 5) & 0x1);
    }

    public void setSynthetic_flag(byte synthetic_flag) {
        byte b = point10.get(15);
        b &= (~ 0b00100000);
        b |= ((synthetic_flag << 5) & 0b00100000);
        point10.put(15, b);
    }

    public byte getKeypoint_flag() {
        byte b = point10.get(15);
        return (byte) ((b >>> 6) & 0x1);
    }

    public void setKeypoint_flag(byte keypoint_flag) {
        byte b = point10.get(15);
        b &= (~ 0b01000000);
        b |= ((keypoint_flag << 6) & 0b01000000);
        point10.put(15, b);
    }

    public byte getWithheld_flag() {
        byte b = point10.get(15);
        return (byte) ((b >>> 7) & 0x1);
    }

    public void setWithheld_flag(byte withheld_flag) {
        byte b = point10.get(15);
        b &= (~ 0b10000000);
        b |= ((withheld_flag << 7) & 0b10000000);
        point10.put(15, b);
    }

    public byte getScan_angle_rank() {
        return point10.get(16);
    }

    public void setScan_angle_rank(byte scan_angle_rank) {
        point10.put(16, scan_angle_rank);
    }

    public byte getUser_data() {
        return point10.get(17);
    }

    public void setUser_data(byte user_data) {
        point10.put(17, user_data);
    }

    public char getPoint_source_ID() {
        return point10.getChar(18);
    }

    public void setPoint_source_ID(char point_source_ID) {
        point10.putChar(18, point_source_ID);
    }

    public short getExtended_scan_angle() {
        return point10.getShort(20);
    }

    public void setExtended_scan_angle(short extended_scan_angle) {
        point10.putShort(20, extended_scan_angle);
    }

    public byte getExtended_point_type() {
        byte b = point10.get(22);
        return (byte) (b & 0b0011);
    }

    public void setExtended_point_type(byte extended_point_type) {
        byte b = point10.get(22);
        b &= (~ 0b0011);
        b |= (extended_point_type & 0b0011);
        point10.put(22, b);
    }

    public byte getExtended_scanner_channel() {
        byte b = point10.get(22);
        return (byte) ((b >>> 2) & 0b0011);
    }

    public void setExtended_scanner_channel(byte extended_scanner_channel) {
        byte b = point10.get(22);
        b &= (~ 0b1100);
        b |= ((extended_scanner_channel << 2) & 0b1100);
        point10.put(22, b);
    }

    public byte getExtended_classification_flags() {
        byte b = point10.get(22);
        return (byte) ((b >>> 4) & 0b1111);
    }

    public void setExtended_classification_flags(byte extended_classification_flags) {
        byte b = point10.get(22);
        b &= (~ 0b11110000);
        b |= ((extended_classification_flags << 4) & 0b11110000);
        point10.put(22, b);
    }

    public byte getExtended_classification() {
        return point10.get(23);
    }

    public void setExtended_classification(byte extended_classification) {
        point10.put(23, extended_classification);
    }

    public byte getExtended_return_number() {
        byte b = point10.get(24);
        return (byte) (b & 0b1111);
    }

    public void setExtended_return_number(byte extended_return_number) {
        byte b = point10.get(24);
        b &= (~ 0b1111);
        b |= (extended_return_number & 0b1111);
        point10.put(24, b);
    }

    public byte getExtended_number_of_returns() {
        byte b = point10.get(24);
        return (byte) ((b >>> 4) & 0b1111);
    }

    public void setExtended_number_of_returns(byte extended_number_of_returns) {
        byte b = point10.get(24);
        b &= (~ 0b11110000);
        b |= ((extended_number_of_returns << 4) & 0b11110000);
        point10.put(24, b);
    }

    public int getDeleted_flag() {
        return point10.getInt(28);
    }

    public void setDeleted_flag(int deleted_flag) {
        point10.putInt(28, deleted_flag);
    }

    public double getGps_time() {
        if (have_gps_time11) {
            return gps_time.getDouble(0);
        } else {
            return point10.getDouble(32);
        }
    }

    public void setGps_time(double gps_time) {
        this.gps_time.putDouble(0, gps_time);
        point10.putDouble(32, gps_time);
    }

    public char getRgb(int index) {
        return rgb.getChar(index * 2);
    }

    public char[] getRgb() {
        char[] rgb = new char[4];
        for (int i = 0; i < rgb.length; i++) {
            rgb[i] = this.rgb.getChar(i * 2);
        }
        return rgb;
    }

    public void setRgb(int index, char value) {
        this.rgb.putChar(index * 2, value);
    }

    public void setRgb(char[] rgb) {
        for (int i = 0; i < rgb.length; i++) {
            this.rgb.putChar(i * 2, rgb[i]);
        }
    }
}
