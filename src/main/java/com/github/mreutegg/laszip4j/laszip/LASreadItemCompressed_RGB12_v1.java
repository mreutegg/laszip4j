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

public class LASreadItemCompressed_RGB12_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private ByteBuffer last_item;

    private ArithmeticModel m_byte_used;
    private IntegerCompressor ic_rgb;

    LASreadItemCompressed_RGB12_v1(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        m_byte_used = dec.createSymbolModel(64);
        ic_rgb = new IntegerCompressor(dec, 8, 6);

        /* create last item */
        last_item = ByteBuffer.allocate(6);
    }

    public boolean init(byte[] item)
    {
        /* init state */

        /* init models and integer compressors */
        dec.initSymbolModel(m_byte_used);
        ic_rgb.initDecompressor();

        /* init last item */
        memcpy(last_item.array(), item, 6);
        return TRUE;
    }

    public void read(byte[] itemBytes)
    {
        ByteBuffer item = ByteBuffer.wrap(itemBytes);
        
        int sym = dec.decodeSymbol(m_byte_used);
        if ((sym & (1 << 0)) != 0) (item).putChar(0, (char)ic_rgb.decompress((last_item).getChar(0)&255, 0));
        else (item).putChar(0, (char)((last_item).getChar(0)&0xFF));
        if ((sym & (1 << 1)) != 0) (item).putChar(0, (char) (item.getChar(0) | (((char)ic_rgb.decompress((last_item).getChar(0)>>8, 1)) << 8)));
        else (item).putChar(0, (char) (item.getChar(0) | ((last_item).getChar(0)&0xFF00)));
        if ((sym & (1 << 2)) != 0) (item).putChar(2, (char)ic_rgb.decompress((last_item).getChar(2)&255, 2));
        else (item).putChar(2, (char)((last_item).getChar(2)&0xFF));
        if ((sym & (1 << 3)) != 0) (item).putChar(2, (char) (item.getChar(2) | (((char)ic_rgb.decompress((last_item).getChar(2)>>8, 3)) << 8)));
        else (item).putChar(2, (char) (item.getChar(2) | ((last_item).getChar(2)&0xFF00)));
        if ((sym & (1 << 4)) != 0) (item).putChar(4, (char)ic_rgb.decompress((last_item).getChar(4)&255, 4));
        else (item).putChar(4, (char)((last_item).getChar(4)&0xFF));
        if ((sym & (1 << 5)) != 0) (item).putChar(4, (char) (item.getChar(4) | (((char)ic_rgb.decompress((last_item).getChar(4)>>8, 5)) << 8)));
        else (item).putChar(4, (char) (item.getChar(4) | ((last_item).getChar(4)&0xFF00)));
        memcpy(last_item.array(), item.array(), 6);
    }
}
