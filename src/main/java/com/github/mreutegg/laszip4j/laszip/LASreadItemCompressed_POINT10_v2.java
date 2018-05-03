/*
 * Copyright 2007-2012, martin isenburg, rapidlasso - fast tools to catch reality
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

import static com.github.mreutegg.laszip4j.clib.Cstring.memcpy;
import static com.github.mreutegg.laszip4j.laszip.Common_v2.number_return_level;
import static com.github.mreutegg.laszip4j.laszip.Common_v2.number_return_map;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_ZERO_BIT_0;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;
import static com.github.mreutegg.laszip4j.laszip.StreamingMedian5.newStreamingMedian5;
import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_POINT10_v2 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private byte[] last_item = new byte[20];
    private char[] last_intensity = new char[16];
    private StreamingMedian5[] last_x_diff_median5 = newStreamingMedian5(16);
    private StreamingMedian5[] last_y_diff_median5 = newStreamingMedian5(16);
    private int[] last_height = new int[8]; // signed

    private ArithmeticModel m_changed_values;
    private IntegerCompressor ic_intensity;
    private ArithmeticModel[] m_scan_angle_rank = new ArithmeticModel[2];
    private IntegerCompressor ic_point_source_ID;
    private ArithmeticModel[] m_bit_byte = new ArithmeticModel[256];
    private ArithmeticModel[] m_classification = new ArithmeticModel[256];
    private ArithmeticModel[] m_user_data = new ArithmeticModel[256];
    private IntegerCompressor ic_dx;
    private IntegerCompressor ic_dy;
    private IntegerCompressor ic_z;

    public LASreadItemCompressed_POINT10_v2(ArithmeticDecoder dec)
    {
        int i; // unsigned

        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        m_changed_values = dec.createSymbolModel(64);
        ic_intensity = new IntegerCompressor(dec, 16, 4);
        m_scan_angle_rank[0] = dec.createSymbolModel(256);
        m_scan_angle_rank[1] = dec.createSymbolModel(256);
        ic_point_source_ID = new IntegerCompressor(dec, 16);
        for (i = 0; i < 256; i++)
        {
            m_bit_byte[i] = null;
            m_classification[i] = null;
            m_user_data[i] = null;
        }
        ic_dx = new IntegerCompressor(dec, 32, 2);  // 32 bits, 2 context
        ic_dy = new IntegerCompressor(dec, 32, 22); // 32 bits, 22 contexts
        ic_z = new IntegerCompressor(dec, 32, 20);  // 32 bits, 20 contexts
    }

    public boolean init(byte[] item)
    {
        int i; // unsigned

        /* init state */
        for (i=0; i < 16; i++)
        {
            last_x_diff_median5[i].init();
            last_y_diff_median5[i].init();
            last_intensity[i] = 0;
            last_height[i/2] = 0;
        }

        /* init models and integer compressors */
        dec.initSymbolModel(m_changed_values);
        ic_intensity.initDecompressor();
        dec.initSymbolModel(m_scan_angle_rank[0]);
        dec.initSymbolModel(m_scan_angle_rank[1]);
        ic_point_source_ID.initDecompressor();
        for (i = 0; i < 256; i++)
        {
            if (m_bit_byte[i] != null) dec.initSymbolModel(m_bit_byte[i]);
            if (m_classification[i] != null) dec.initSymbolModel(m_classification[i]);
            if (m_user_data[i] != null) dec.initSymbolModel(m_user_data[i]);
        }
        ic_dx.initDecompressor();
        ic_dy.initDecompressor();
        ic_z.initDecompressor();

        /* init last item */
        memcpy(last_item, item, 20);

        /* but set intensity to zero */
        last_item[12] = 0;
        last_item[13] = 0;

        return TRUE;
    }

    public void read(byte[] item)
    {
        int r, n, m, l; // unsigned
        int k_bits; // unsigned
        int median, diff; // signed

        // decompress which other values have changed
        int changed_values = dec.decodeSymbol(m_changed_values);

        LASpoint10 lp = LASpoint10.wrap(last_item);
        if (changed_values != 0)
        {
            // decompress the edge_of_flight_line, scan_direction_flag, ... if it has changed
            if ((changed_values & 32) != 0)
            {
                if (m_bit_byte[Byte.toUnsignedInt(last_item[14])] == null)
                {
                    m_bit_byte[Byte.toUnsignedInt(last_item[14])] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_bit_byte[Byte.toUnsignedInt(last_item[14])]);
                }
                last_item[14] = (byte) dec.decodeSymbol(m_bit_byte[Byte.toUnsignedInt(last_item[14])]);
            }

            r = lp.getReturn_number();
            n = lp.getNumber_of_returns_of_given_pulse();
            m = number_return_map[n][r];
            l = number_return_level[n][r];

            // decompress the intensity if it has changed
            if ((changed_values & 16) != 0)
            {
                lp.setIntensity((char) ic_intensity.decompress(last_intensity[m], (m < 3 ? m : 3)));
                last_intensity[m] = lp.getIntensity();
            }
            else
            {
                lp.setIntensity(last_intensity[m]);
            }

            // decompress the classification ... if it has changed
            if ((changed_values & 8) != 0)
            {
                if (m_classification[Byte.toUnsignedInt(last_item[15])] == null)
                {
                    m_classification[Byte.toUnsignedInt(last_item[15])] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_classification[Byte.toUnsignedInt(last_item[15])]);
                }
                last_item[15] = (byte) dec.decodeSymbol(m_classification[Byte.toUnsignedInt(last_item[15])]);
            }

            // decompress the scan_angle_rank ... if it has changed
            if ((changed_values & 4) != 0)
            {
                int val = dec.decodeSymbol(m_scan_angle_rank[lp.getScan_direction_flag()]);
                last_item[16] = U8_FOLD(val + last_item[16]);
            }

            // decompress the user_data ... if it has changed
            if ((changed_values & 2) != 0)
            {
                if (m_user_data[Byte.toUnsignedInt(last_item[17])] == null)
                {
                    m_user_data[Byte.toUnsignedInt(last_item[17])] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_user_data[Byte.toUnsignedInt(last_item[17])]);
                }
                last_item[17] = (byte) dec.decodeSymbol(m_user_data[Byte.toUnsignedInt(last_item[17])]);
            }

            // decompress the point_source_ID ... if it has changed
            if ((changed_values & 1) != 0)
            {
                lp.setPoint_source_ID((char) ic_point_source_ID.decompress(lp.getPoint_source_ID()));
            }
        }
        else
        {
            r = lp.getReturn_number();
            n = lp.getNumber_of_returns_of_given_pulse();
            m = number_return_map[n][r];
            l = number_return_level[n][r];
        }

        // decompress x coordinate
        median = last_x_diff_median5[m].get();
        diff = ic_dx.decompress(median, n==1 ? 1 : 0);
        lp.setX(lp.getX() + diff);
        last_x_diff_median5[m].add(diff);

        // decompress y coordinate
        median = last_y_diff_median5[m].get();
        k_bits = ic_dx.getK();
        diff = ic_dy.decompress(median, (n==1 ? 1 : 0) + ( k_bits < 20 ? U32_ZERO_BIT_0(k_bits) : 20 ));
        lp.setY(lp.getY() + diff);
        last_y_diff_median5[m].add(diff);

        // decompress z coordinate
        k_bits = (ic_dx.getK() + ic_dy.getK()) / 2;
        lp.setZ(ic_z.decompress(last_height[l], (n==1 ? 1 : 0) + (k_bits < 18 ? U32_ZERO_BIT_0(k_bits) : 18)));
        last_height[l] = lp.getZ();

        // copy the last point
        memcpy(item, last_item, 20);
    }

    static class LASpoint10
    {
        private final ByteBuffer bb;

        private LASpoint10(ByteBuffer bb) {
            this.bb = bb;
        }

        public LASpoint10() {
            this(ByteBuffer.allocate(20));
        }

        static LASpoint10 wrap(byte[] data) {
            return new LASpoint10(ByteBuffer.wrap(data));
        }

        int getX() {
            return bb.getInt(0);
        }

        void setX(int x) {
            bb.putInt(0, x);
        }

        int getY() {
            return bb.getInt(4);
        }

        void setY(int y) {
            bb.putInt(4, y);
        }

        int getZ() {
            return bb.getInt(8);
        }

        void setZ(int z) {
            bb.putInt(8, z);
        }

        char getIntensity() {
            return bb.getChar(12);
        }

        void setIntensity(char i) {
            bb.putChar(12, i);
        }

        char getPoint_source_ID() {
            return bb.getChar(18);
        }

        void setPoint_source_ID(char id) {
            bb.putChar(18, id);
        }

        int getReturn_number() {
            byte b = bb.get(14);
            return b & 0x7;
        }

        int getNumber_of_returns_of_given_pulse() {
            byte b = bb.get(14);
            return (b >>> 3) & 0x7;
        }

        int getScan_direction_flag() {
            byte b = bb.get(14);
            return (b >>> 6) & 0x1;
        }

        /*
        int x;                                              // 0
        int y;                                              // 4
        int z;                                              // 8
        char intensity;                                     // 12
        byte return_number : 3;                             // 14
        byte number_of_returns_of_given_pulse : 3;          // 14
        byte scan_direction_flag : 1;                       // 14
        byte edge_of_flight_line : 1;                       // 14
        byte classification;                                // 15
        byte scan_angle_rank;                               // 16
        byte user_data;                                     // 17
        char point_source_ID;                               // 18
        */
    };
}
