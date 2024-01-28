/*
 * Copyright 2005-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.nio.charset.StandardCharsets;

public interface MyDefs {

    char U16_MAX = Character.MAX_VALUE;

    int U32_MAX = 0xFFFFFFFF;

    int I32_MIN = Integer.MIN_VALUE;

    byte U8_MIN = 0x0;
    byte I8_MIN = Byte.MIN_VALUE;

    byte U8_MAX = (byte) 0xFF; // 255
    byte I8_MAX = Byte.MAX_VALUE;
    byte U8_MAX_MINUS_ONE = (byte) 0xFE; // 254
    char U8_MAX_PLUS_ONE = 0x0100;     // 256


    static byte U8_FOLD(int n) {
        return (byte) ((n < U8_MIN) ? (n+U8_MAX_PLUS_ONE) : (((n) > Byte.toUnsignedInt(U8_MAX)) ? (n-U8_MAX_PLUS_ONE) : (n)));
    }

    static int U8_CLAMP(int n) {
        return (((n) <= U8_MIN) ? U8_MIN : (((n) >= Byte.toUnsignedInt(U8_MAX)) ? U8_MAX : ((byte)(n))));
    }

    static int I8_CLAMP(int n) {
        return (((n) <= I8_MIN) ? I8_MIN : (((n) >= I8_MAX) ? I8_MAX : ((byte)(n))));
    }

    static short I16_QUANTIZE(float n) {
        return ((n) >= 0) ? (short)((n)+0.5f) : (short)((n)-0.5f);
    }

    static int I32_QUANTIZE(double n) {
        return n >= 0 ? (int)((n)+0.5f) : (int)((n)-0.5f);
    }

    static int U32_QUANTIZE(double n) {
        return ((n) >= 0) ? (int)((n)+0.5f) : 0;
    }

    static int I32_FLOOR(double value) {
        return (int) Math.floor(value);
    }

    static long I64_FLOOR(double value) {
        return (long) Math.floor(value);
    }

    static int U32_ZERO_BIT_0(int n) {
        return n & 0xFFFFFFFE;
    }

    static boolean IS_LITTLE_ENDIAN() {
        return false;
    }

    static int sizeof(Class<LASattribute> attributeClass) {
        return LASattribute.getMemory();
    }

    static byte[] asByteArray(char[] chars) {
        return asByteArray(new String(chars));
    }

    static byte[] asByteArray(String s) {
        return s.getBytes(StandardCharsets.US_ASCII);
    }

    static String stringFromByteArray(byte[] bytes) {
        int idx = -1;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\0') {
                idx = i;
                break;
            }
        }
        if (idx != -1) {
            return new String(bytes, 0, idx, StandardCharsets.US_ASCII);
        } else {
            return new String(bytes, StandardCharsets.US_ASCII);
        }
    }

    static int[] realloc(int[] data, int size) {
        if (data.length >= size) {
            return data;
        }
        int[] tmp = new int[size];
        System.arraycopy(data, 0, tmp, 0, data.length);
        return tmp;
    }

    static long[] realloc(long[] data, int size) {
        if (data == null) {
            return new long[size];
        } else if (data.length >= size) {
            return data;
        }
        long[] tmp = new long[size];
        System.arraycopy(data, 0, tmp, 0, data.length);
        return tmp;
    }

    static short setBit(short value, int bit) {
        return (short) (value | (short) (1 << bit));
    }

    static short clearBit(short value, int bit) {
        return (short) (value & (~ (1 << bit)));
    }

    static byte setBit(byte value, int bit) {
        return (byte) (value | (byte) (1 << bit));
    }

    static byte clearBit(byte value, int bit) {
        return (byte) (value & (~ (1 << bit)));
    }
}
