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

import static com.github.mreutegg.laszip4j.laszip.Common_v2.number_return_level;
import static com.github.mreutegg.laszip4j.laszip.Common_v2.number_return_map;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_ZERO_BIT_0;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;
import static com.github.mreutegg.laszip4j.laszip.StreamingMedian5.newStreamingMedian5;

public class LASreadItemCompressed_POINT10_v2 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private PointDataRecordPoint10 last_item = null;
    private int[] last_intensity = new int[16];
    private StreamingMedian5[] last_x_diff_median5 = newStreamingMedian5(16);
    private StreamingMedian5[] last_y_diff_median5 = newStreamingMedian5(16);
    private long[] last_height = new long[8]; // signed

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

    @Override	
    public void init(PointDataRecord seedItem, int notUsed)
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
        last_item = (PointDataRecordPoint10)seedItem;

        /* but set intensity to zero (for the algorith)*/
        last_item.Intensity = 0;
    }

    @Override	
    public PointDataRecord read(int notUsed)
    {
        int r, n, m, l; // unsigned
        int k_bits; // unsigned
        int median, diff; // signed

        // decompress which other values have changed
        int changed_values = dec.decodeSymbol(m_changed_values);

        if (changed_values != 0)
        {
            // decompress the flags, if changed
            if ((changed_values & 32) != 0)
            {
                int idx = Byte.toUnsignedInt((byte)last_item.Flags);
                if (m_bit_byte[idx] == null)
                {
                    m_bit_byte[idx] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_bit_byte[idx]);
                }
                last_item.Flags = (byte) dec.decodeSymbol(m_bit_byte[idx]);
            }

            r = last_item.getReturnNumber();
            n = last_item.getNumberOfReturns();
            m = number_return_map[n][r];
            l = number_return_level[n][r];

            // decompress the intensity if it has changed
            if ((changed_values & 16) != 0)
            {
                last_item.Intensity = (char) ic_intensity.decompress(last_intensity[m], (m < 3 ? m : 3));
                last_intensity[m] = last_item.Intensity;
            }
            else
            {
                last_item.Intensity = (char)last_intensity[m];
            }

            // decompress the classification ... if it has changed
            if ((changed_values & 8) != 0)
            {
                if (m_classification[last_item.Classification] == null)
                {
                    m_classification[last_item.Classification] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_classification[last_item.Classification]);
                }
                last_item.Classification = (short) dec.decodeSymbol(m_classification[last_item.Classification]);
            }

            // decompress the scan_angle_rank ... if it has changed
            if ((changed_values & 4) != 0)
            {
                int val = dec.decodeSymbol(m_scan_angle_rank[last_item.hasScanFlag(ScanFlag.ScanDirection)?1:0]);
                last_item.ScanAngleRank = U8_FOLD(val + last_item.ScanAngleRank);
            }

            // decompress the user_data ... if it has changed
            if ((changed_values & 2) != 0)
            {
                if (m_user_data[last_item.UserData] == null)
                {
                    m_user_data[last_item.UserData] = dec.createSymbolModel(256);
                    dec.initSymbolModel(m_user_data[last_item.UserData]);
                }
                last_item.UserData = (short) dec.decodeSymbol(m_user_data[last_item.UserData]);
            }

            // decompress the point_source_ID ... if it has changed
            if ((changed_values & 1) != 0)
            {
                last_item.PointSourceID = (char) ic_point_source_ID.decompress(last_item.PointSourceID);
            }
        }
        else
        {
            r = last_item.getReturnNumber();
            n = last_item.getNumberOfReturns();
            m = number_return_map[n][r];
            l = number_return_level[n][r];
        }

        // decompress x coordinate
        median = last_x_diff_median5[m].get();
        diff = ic_dx.decompress(median, n==1 ? 1 : 0);
        last_item.X += diff;
        last_x_diff_median5[m].add(diff);

        // decompress y coordinate
        median = last_y_diff_median5[m].get();
        k_bits = ic_dx.getK();
        diff = ic_dy.decompress(median, (n==1 ? 1 : 0) + ( k_bits < 20 ? U32_ZERO_BIT_0(k_bits) : 20 ));
        last_item.Y += diff;
        last_y_diff_median5[m].add(diff);

        // decompress z coordinate
        k_bits = (ic_dx.getK() + ic_dy.getK()) / 2;
        last_item.Z = ic_z.decompress((int)last_height[l], (n==1 ? 1 : 0) + (k_bits < 18 ? U32_ZERO_BIT_0(k_bits) : 18));
        last_height[l] = last_item.Z;

        PointDataRecordPoint10 result = new PointDataRecordPoint10(last_item);

        return result;
    }

    @Override	
    public boolean chunk_sizes() {	
        return false;	
    }
}
