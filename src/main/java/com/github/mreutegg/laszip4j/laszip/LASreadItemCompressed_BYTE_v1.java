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

import static com.github.mreutegg.laszip4j.clib.Cstring.memcpy;
import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_BYTE_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private int number; // unsigned
    private byte[] last_item;

    private IntegerCompressor ic_byte;

    public LASreadItemCompressed_BYTE_v1(ArithmeticDecoder dec, int number)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;
        assert(number != 0);
        this.number = number;

        /* create models and integer compressors */
        ic_byte = new IntegerCompressor(dec, 8, number);

        /* create last item */
        last_item = new byte[number];
    }

    public boolean init(byte[] item)
    {
        /* init state */

        /* init models and integer compressors */
        ic_byte.initDecompressor();

        /* init last item */
        memcpy(last_item, item, number);
        return TRUE;
    }

    public void read(byte[] item)
    {
        int i;
        for (i = 0; i < number; i++)
        {
            item[i] = (byte)(ic_byte.decompress(last_item[i], i));
        }
        memcpy(last_item, item, number);
    }
}
