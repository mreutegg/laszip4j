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

public class LASreadItemCompressed_BYTE_v1 extends LASreadItemCompressed {

    private int number; // unsigned
    private PointDataRecordBytes last_item;

    private IntegerCompressor ic_byte;

    public LASreadItemCompressed_BYTE_v1(ArithmeticDecoder dec, int number)
    {
        /* set decoder */
        assert(dec != null);
        assert(number != 0);
        this.number = number;

        /* create models and integer compressors */
        ic_byte = new IntegerCompressor(dec, 8, number);
    }

    @Override	
    public void init(PointDataRecord seedItem, int notUsed)
    {
        /* init state */

        /* init models and integer compressors */
        ic_byte.initDecompressor();

        /* init last item */
        last_item = (PointDataRecordBytes)seedItem;
    }

    @Override	
    public PointDataRecord read(int notUsed)
    {
        PointDataRecordBytes result = new PointDataRecordBytes(number);

        for (int i = 0; i < number; i++)
        {
            result.Bytes[i] = (byte)(ic_byte.decompress(last_item.Bytes[i], i));
        }

        return result;
    }

    @Override	
    public boolean chunk_sizes() {	
        return false;	
    }
}
