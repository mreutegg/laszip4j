/*
 * Copyright 2007-2014, martin isenburg, rapidlasso - fast tools to catch reality
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
import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_POINT10_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private byte[] last_item = new byte[20];

    private int[] last_x_diff = new int[3];
    private int[] last_y_diff = new int[3];
    private int last_incr;
    private IntegerCompressor ic_dx;
    private IntegerCompressor ic_dy;
    private IntegerCompressor ic_z;
    private IntegerCompressor ic_intensity;
    private IntegerCompressor ic_scan_angle_rank;
    private IntegerCompressor ic_point_source_ID;
    private ArithmeticModel m_changed_values;
    private ArithmeticModel[] m_bit_byte = new ArithmeticModel[256];
    private ArithmeticModel[] m_classification = new ArithmeticModel[256];
    private ArithmeticModel[] m_user_data = new ArithmeticModel[256];
    
    public LASreadItemCompressed_POINT10_v1(ArithmeticDecoder dec) {
        int i;

        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        ic_dx = new IntegerCompressor(dec, 32);  // 32 bits, 1 context
        ic_dy = new IntegerCompressor(dec, 32, 20); // 32 bits, 20 contexts
        ic_z = new IntegerCompressor(dec, 32, 20);  // 32 bits, 20 contexts
        ic_intensity = new IntegerCompressor(dec, 16);
        ic_scan_angle_rank = new IntegerCompressor(dec, 8, 2);
        ic_point_source_ID = new IntegerCompressor(dec, 16);
        m_changed_values = dec.createSymbolModel(64);
        for (i = 0; i < 256; i++)
        {
            m_bit_byte[i] = null;
            m_classification[i] = null;
            m_user_data[i] = null;
        }
    }

    @Override
    public boolean init(byte[] item) {
        int i;

        /* init state */
        last_x_diff[0] = last_x_diff[1] = last_x_diff[2] = 0;
        last_y_diff[0] = last_y_diff[1] = last_y_diff[2] = 0;
        last_incr = 0;

        /* init models and integer compressors */
        ic_dx.initDecompressor();
        ic_dy.initDecompressor();
        ic_z.initDecompressor();
        ic_intensity.initDecompressor();
        ic_scan_angle_rank.initDecompressor();
        ic_point_source_ID.initDecompressor();
        dec.initSymbolModel(m_changed_values);
        for (i = 0; i < 256; i++)
        {
            if (m_bit_byte[i] != null) dec.initSymbolModel(m_bit_byte[i]);
            if (m_classification[i] != null) dec.initSymbolModel(m_classification[i]);
            if (m_user_data[i] != null) dec.initSymbolModel(m_user_data[i]);
        }

        /* init last item */
        memcpy(last_item, item, 20);

        return TRUE;
    }

    @Override
    public void read(byte[] item) {
        // find median difference for x and y from 3 preceding differences
        int median_x;
        if (last_x_diff[0] < last_x_diff[1])
        {
            if (last_x_diff[1] < last_x_diff[2])
                median_x = last_x_diff[1];
            else if (last_x_diff[0] < last_x_diff[2])
                median_x = last_x_diff[2];
            else
                median_x = last_x_diff[0];
        }
        else
        {
            if (last_x_diff[0] < last_x_diff[2])
                median_x = last_x_diff[0];
            else if (last_x_diff[1] < last_x_diff[2])
                median_x = last_x_diff[2];
            else
                median_x = last_x_diff[1];
        }

        int median_y;
        if (last_y_diff[0] < last_y_diff[1])
        {
            if (last_y_diff[1] < last_y_diff[2])
                median_y = last_y_diff[1];
            else if (last_y_diff[0] < last_y_diff[2])
                median_y = last_y_diff[2];
            else
                median_y = last_y_diff[0];
        }
        else
        {
            if (last_y_diff[0] < last_y_diff[2])
                median_y = last_y_diff[0];
            else if (last_y_diff[1] < last_y_diff[2])
                median_y = last_y_diff[2];
            else
                median_y = last_y_diff[1];
        }

        // decompress x y z coordinates
        int x_diff = ic_dx.decompress(median_x);
        LASpoint10 lp = LASpoint10.wrap(last_item);
        lp.setX(lp.getX() + x_diff);
        // we use the number k of bits corrector bits to switch contexts
        int k_bits = ic_dx.getK(); // unsigned
        int y_diff = ic_dy.decompress(median_y, (k_bits < 19 ? k_bits : 19));
        lp.setY(lp.getY() + y_diff);
        k_bits = (k_bits + ic_dy.getK())/2;
        lp.setZ(ic_z.decompress(lp.getZ(), (k_bits < 19 ? k_bits : 19)));

        // decompress which other values have changed
        int changed_values = dec.decodeSymbol(m_changed_values);

        if (changed_values != 0)
        {
            // decompress the intensity if it has changed
            if ((changed_values & 32) != 0)
            {
                lp.setIntensity((char) ic_intensity.decompress(lp.getIntensity()));
            }

            // decompress the edge_of_flight_line, scan_direction_flag, ... if it has changed
            if ((changed_values & 16) != 0)
            {
                if (m_bit_byte[last_item[14]] == null)
                {
                    m_bit_byte[last_item[14]] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_bit_byte[last_item[14]]);
                }
                last_item[14] = (byte) dec.decodeSymbol(m_bit_byte[last_item[14]]);
            }

            // decompress the classification ... if it has changed
            if ((changed_values & 8) != 0)
            {
                if (m_classification[last_item[15]] == null)
                {
                    m_classification[last_item[15]] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_classification[last_item[15]]);
                }
                last_item[15] = (byte) dec.decodeSymbol(m_classification[last_item[15]]);
            }

            // decompress the scan_angle_rank ... if it has changed
            if ((changed_values & 4) != 0)
            {
                last_item[16] = (byte) ic_scan_angle_rank.decompress(last_item[16], k_bits < 3 ? 1 : 0);
            }

            // decompress the user_data ... if it has changed
            if ((changed_values & 2) != 0)
            {
                if (m_user_data[last_item[17]] == null)
                {
                    m_user_data[last_item[17]] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_user_data[last_item[17]]);
                }
                last_item[17] = (byte) dec.decodeSymbol(m_user_data[last_item[17]]);
            }

            // decompress the point_source_ID ... if it has changed
            if ((changed_values & 1) != 0)
            {
                lp.setPoint_source_ID((char) ic_point_source_ID.decompress(lp.getPoint_source_ID()));
            }
        }

        // record the difference
        last_x_diff[last_incr] = x_diff;
        last_y_diff[last_incr] = y_diff;
        last_incr++;
        if (last_incr > 2) last_incr = 0;

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

        /*
        int x;                                              // 0
        int y;                                              // 4
        int z;                                              // 8
        char intensity;                                     // 12
        byte return_number = 3;                             // 14
        byte number_of_returns_of_given_pulse = 3;          // 14
        byte scan_direction_flag = 1;                       // 14
        byte edge_of_flight_line = 1;                       // 14
        byte classification;                                // 15
        byte scan_angle_rank;                               // 16
        byte user_data;                                     // 17
        char point_source_ID;                               // 18
        */
    };
}
