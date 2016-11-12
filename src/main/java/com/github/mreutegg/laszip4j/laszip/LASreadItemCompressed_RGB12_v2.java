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

import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_CLAMP;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;
import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_RGB12_v2 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private char[] last_item = new char[3];

    private ArithmeticModel m_byte_used;
    private ArithmeticModel m_rgb_diff_0;
    private ArithmeticModel m_rgb_diff_1;
    private ArithmeticModel m_rgb_diff_2;
    private ArithmeticModel m_rgb_diff_3;
    private ArithmeticModel m_rgb_diff_4;
    private ArithmeticModel m_rgb_diff_5;

    LASreadItemCompressed_RGB12_v2(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        m_byte_used = dec.createSymbolModel(128);
        m_rgb_diff_0 = dec.createSymbolModel(256);
        m_rgb_diff_1 = dec.createSymbolModel(256);
        m_rgb_diff_2 = dec.createSymbolModel(256);
        m_rgb_diff_3 = dec.createSymbolModel(256);
        m_rgb_diff_4 = dec.createSymbolModel(256);
        m_rgb_diff_5 = dec.createSymbolModel(256);
    }

    public boolean init(byte[] item)
    {
        /* init state */

        /* init models and integer compressors */
        dec.initSymbolModel(m_byte_used);
        dec.initSymbolModel(m_rgb_diff_0);
        dec.initSymbolModel(m_rgb_diff_1);
        dec.initSymbolModel(m_rgb_diff_2);
        dec.initSymbolModel(m_rgb_diff_3);
        dec.initSymbolModel(m_rgb_diff_4);
        dec.initSymbolModel(m_rgb_diff_5);

        /* init last item */
        memcpy(last_item, item, 6);
        return TRUE;
    }

    public void read(byte[] itemBytes)
    {
        ByteBuffer item = ByteBuffer.wrap(itemBytes);
        byte corr;
        int diff = 0;
        int sym = dec.decodeSymbol(m_byte_used);
        if ((sym & (1 << 0)) != 0)
        {
            corr = (byte) dec.decodeSymbol(m_rgb_diff_0);
            item.putChar(0, (char)U8_FOLD(corr + (last_item[0]&255)));
        }
        else
        {
            item.putChar(0, (char) (last_item[0]&0xFF));
        }
        if ((sym & (1 << 1)) != 0)
        {
            corr = (byte) dec.decodeSymbol(m_rgb_diff_1);
            item.putChar(0, (char) (item.getChar(0) | (((char)U8_FOLD(corr + (last_item[0]>>>8))) << 8)));
        }
        else
        {
            item.putChar(0, (char) (item.getChar(0) | (last_item[0]&0xFF00)));
        }
        if ((sym & (1 << 6)) != 0)
        {
            diff = ((item.getChar(0)&0x00FF) - (last_item[0]&0x00FF));
            if ((sym & (1 << 2)) != 0)
            {
                corr = (byte) dec.decodeSymbol(m_rgb_diff_2);
                item.putChar(2, (char)U8_FOLD(corr + U8_CLAMP(diff+(last_item[1]&255))));
            }
            else
            {
                item.putChar(2, (char)(last_item[1]&0xFF));
            }
            if ((sym & (1 << 4)) != 0)
            {
                corr = (byte) dec.decodeSymbol(m_rgb_diff_4);
                diff = (diff + ((item.getChar(2)&0x00FF) - (last_item[1]&0x00FF))) / 2;
                item.putChar(4, (char)U8_FOLD(corr + U8_CLAMP(diff+(last_item[2]&255))));
            }
            else
            {
                item.putChar(4, (char) (last_item[2]&0xFF));
            }
            diff = (item.getChar(0)>>>8) - (last_item[0]>>>8);
            if ((sym & (1 << 3)) != 0)
            {
                corr = (byte) dec.decodeSymbol(m_rgb_diff_3);
                item.putChar(2, (char) (item.getChar(2) | (((char)U8_FOLD(corr + U8_CLAMP(diff+(last_item[1]>>>8))))<<8)));
            }
            else
            {
                item.putChar(2, (char) (item.getChar(2) | (last_item[1]&0xFF00)));
            }
            if ((sym & (1 << 5)) != 0)
            {
                corr = (byte) dec.decodeSymbol(m_rgb_diff_5);
                diff = (diff + ((item.getChar(2)>>>8) - (last_item[1]>>>8))) / 2;
                item.putChar(4, (char) (item.getChar(4) | (((char)U8_FOLD(corr + U8_CLAMP(diff+(last_item[2]>>>8))))<<8)));
            }
            else
            {
                item.putChar(4, (char) (item.getChar(4) | (last_item[2]&0xFF00)));
            }
        }
        else
        {
            item.putChar(2, item.getChar(0));
            item.putChar(4, item.getChar(0));
        }
        memcpy(last_item, item.array(), 6);
    }

    private static void memcpy(char[] dest, byte[] src, int num) {
        ByteBuffer srcBuffer = ByteBuffer.wrap(src);
        for (int i = 0; i < num / 2; i++) {
            dest[i] = srcBuffer.getChar(i * 2);
        }
    }
}
