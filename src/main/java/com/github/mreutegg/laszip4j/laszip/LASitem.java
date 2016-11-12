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
        SHORT, INT,
        LONG,
        FLOAT,
        DOUBLE,
        POINT10,
        GPSTIME11,
        RGB12,
        WAVEPACKET13,
        POINT14,
        RGBNIR14;

        private static final Type[] TYPES = Type.values();

        public static Type fromOrdinal(int i) {
            return TYPES[i];
        }
    }

    public Type type;
    public char size; // unsigned
    public char version; // unsigned

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
                if (size != 6) return false;
                break;
            case WAVEPACKET13:
                if (size != 29) return false;
                break;
            case BYTE:
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
            case WAVEPACKET13:
                return "WAVEPACKET13";
            case BYTE:
                return "BYTE";
            default:
                break;
        }
        return null;
    }
}
