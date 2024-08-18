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

public class LASreadItemCompressed_RGB12_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private PointDataRecordRGB last_item;

    private ArithmeticModel m_byte_used;
    private IntegerCompressor ic_rgb;

    public LASreadItemCompressed_RGB12_v1(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        m_byte_used = dec.createSymbolModel(64);
        ic_rgb = new IntegerCompressor(dec, 8, 6);

        last_item = null;
    }

    @Override	
    public void init(PointDataRecord seedItem, MutableInteger notUsed)
    {
        /* init state */

        /* init models and integer compressors */
        dec.initSymbolModel(m_byte_used);
        ic_rgb.initDecompressor();

        last_item = new PointDataRecordRGB((PointDataRecordRGB)seedItem);
    }

    @Override	
    public PointDataRecord read(MutableInteger notUsed)
    {       
        PointDataRecordRGB result = new PointDataRecordRGB();

        int sym = dec.decodeSymbol(m_byte_used);
        if ((sym & (1 << 0)) != 0) 
            result.R = (char)ic_rgb.decompress(last_item.R&255, 0);
        else 
            result.R = (char)(last_item.R&0xFF);
        if ((sym & (1 << 1)) != 0) 
            result.R |= (((char)ic_rgb.decompress(last_item.R>>8, 1)) << 8);
        else 
            result.R |= (last_item.R&0xFF00);
        if ((sym & (1 << 2)) != 0) 
            result.G = (char)ic_rgb.decompress(last_item.G&255, 2);
        else 
            result.G = (char)(last_item.G&0xFF);
        if ((sym & (1 << 3)) != 0) 
            result.G |= (((char)ic_rgb.decompress(last_item.G>>8, 3)) << 8);
        else 
            result.G |= (last_item.G&0xFF00);
        if ((sym & (1 << 4)) != 0) 
            result.B = (char)ic_rgb.decompress(last_item.B&255, 4);
        else 
            result.B = (char)(last_item.B&0xFF);
        if ((sym & (1 << 5)) != 0) 
            result.B |= (((char)ic_rgb.decompress(last_item.B>>8, 5)) << 8);
        else 
            result.B |= (last_item.B&0xFF00);

        last_item.R = result.R;
        last_item.G = result.G;
        last_item.B = result.B;

        return result;
    }

    @Override	
    public boolean chunk_sizes() {	
        return false;	
    }
}
