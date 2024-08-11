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

import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;

public class LASreadItemCompressed_BYTE_v2 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private int number; // unsigned
    private PointDataRecordBytes last_item;

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
    }

    @Override	
    public void init(PointDataRecord seedItem, MutableInteger notUsed)
    {
        int i;
        /* init state */

        /* init models and integer compressors */
        for (i = 0; i < number; i++)
        {
            dec.initSymbolModel(m_byte[i]);
        }

        last_item = new PointDataRecordBytes((PointDataRecordBytes) seedItem);
    }

    @Override	
    public PointDataRecord read(MutableInteger notUsed)
    {
        PointDataRecordBytes result = new PointDataRecordBytes(number);

        for (int i = 0; i < number; i++)
        {
            int value = last_item.Bytes[i] + dec.decodeSymbol(m_byte[i]);
            result.Bytes[i] = U8_FOLD(value);
        }

        return result;
    }

    @Override	
    public boolean chunk_sizes() {	
        return false;	
    }
}
