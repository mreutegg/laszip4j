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

import static com.github.mreutegg.laszip4j.clib.Cstring.memcpy;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;
import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_BYTE_v2 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private int number; // unsigned
    private byte[] last_item;

    private ArithmeticModel[] m_byte;

    public LASreadItemCompressed_BYTE_v2(ArithmeticDecoder dec, int number)
    {
        int i;

        /* set decoder */
        assert(dec != null);
        this.dec = dec;
        assert(number != 0);
        this.number = number;

        /* create models and integer compressors */
        m_byte = new ArithmeticModel[number];
        for (i = 0; i < number; i++)
        {
            m_byte[i] = dec.createSymbolModel(256);
        }

        /* create last item */
        last_item = new byte[number];
    }

    public boolean init(byte[] item)
    {
        int i;
        /* init state */

        /* init models and integer compressors */
        for (i = 0; i < number; i++)
        {
            dec.initSymbolModel(m_byte[i]);
        }

        /* init last item */
        memcpy(last_item, item, number);
        return TRUE;
    }

    public void read(byte[] item)
    {
        int i;
        int value;
        for (i = 0; i < number; i++)
        {
            value = last_item[i] + dec.decodeSymbol(m_byte[i]);
            item[i] = U8_FOLD(value);
        }
        memcpy(last_item, item, number);
    }
}
