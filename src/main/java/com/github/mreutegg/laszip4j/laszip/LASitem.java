/*
 * Copyright 2007-2016, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

public class LASitem {

    public enum Type {
        BYTE,
        SHORT, 
        INT, 
        LONG, 
        FLOAT, 
        DOUBLE, 
        POINT10, 
        GPSTIME11, 
        RGB12, 
        WAVEPACKET13, 
        POINT14, 
        RGB14, 
        RGBNIR14, 
        WAVEPACKET14, 
        BYTE14 ;

        private static final Type[] TYPES = Type.values();

        public static Type fromOrdinal(int i) {
            return TYPES[i];
        }
    }

    public LASitem() {

    }
    
    public LASitem(Type type, int size, int version) {
        this.type = type;
        this.size = (char)size;
        this.version = (char)version;
    }

    public Type type;
    public char size; // unsigned
    public char version; // unsigned

    public static LASitem Point10(int ver) { return new LASitem(Type.POINT10, 20, ver); }
    public static LASitem GpsTime11(int ver) { return new LASitem(Type.GPSTIME11, 8, ver); }
    public static LASitem Rgb12(int ver) { return new LASitem(Type.RGB12, 6, ver); }
    public static LASitem WavePacket13(int ver) { return new LASitem(Type.WAVEPACKET13, 29, ver); }
    public static LASitem Point14(int ver) { return new LASitem(Type.POINT14, 30, ver); }
    public static LASitem RgbNIR14(int ver) { return new LASitem(Type.RGBNIR14, 8, ver); }
    public static LASitem ExtraBytes(int size, int ver) { return new LASitem(Type.BYTE, size, ver); }
    public static LASitem Rgb14(int ver) { return new LASitem(Type.RGB14, 6, ver); }
    public static LASitem WavePacket14(int ver) { return new LASitem(Type.WAVEPACKET14, 29, ver); }
    public static LASitem ExtraBytes14(int size, int ver) { return new LASitem(Type.BYTE14, size, ver); }

    boolean is_type(Type t)
    {
        if (t != type) return false;
        switch (t)
        {
            case POINT10:
                if (size != 20) return false;
                break;
            case POINT14:
                if (size != 30) return false;
                break;
            case GPSTIME11:
                if (size != 8) return false;
                break;
            case RGB12:
            case RGB14:
                if (size != 6) return false;
                break;
            case RGBNIR14:
                if (size != 8) return false;
                break;
            case WAVEPACKET13:
            case WAVEPACKET14:
                if (size != 29) return false;
                break;
            case BYTE:
            case BYTE14:
                if (size < 1) return false;
                break;
            default:
                return false;
        }
        return true;
    }

    String get_name()
    {
        switch (type)
        {
            case POINT10:
                return "POINT10";
            case POINT14:
                return "POINT14";
            case GPSTIME11:
                return "GPSTIME11";
            case RGB12:
                return "RGB12";
            case BYTE:
                return "BYTE";
            case RGB14:
                return "RGB14";
            case RGBNIR14:
                return "RGBNIR14";
            case BYTE14:
                return "BYTE14";
            case WAVEPACKET13:
                return "WAVEPACKET13";
            case WAVEPACKET14:
                return "WAVEPACKET14";
            default:
                break;
        }
        return null;
    }
}
