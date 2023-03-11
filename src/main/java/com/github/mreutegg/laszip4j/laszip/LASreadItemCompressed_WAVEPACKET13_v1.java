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

public class LASreadItemCompressed_WAVEPACKET13_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private PointDataRecordWavepacket last_item;

    private int last_diff_32;
    private int sym_last_offset_diff; // unsigned
    private ArithmeticModel m_packet_index;
    private ArithmeticModel[] m_offset_diff = new ArithmeticModel[4];
    private IntegerCompressor ic_offset_diff;
    private IntegerCompressor ic_packet_size;
    private IntegerCompressor ic_return_point;
    private IntegerCompressor ic_xyz;

    public LASreadItemCompressed_WAVEPACKET13_v1(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;

        /* create models and integer compressors */
        m_packet_index = dec.createSymbolModel(256);
        m_offset_diff[0] = dec.createSymbolModel(4);
        m_offset_diff[1] = dec.createSymbolModel(4);
        m_offset_diff[2] = dec.createSymbolModel(4);
        m_offset_diff[3] = dec.createSymbolModel(4);
        ic_offset_diff = new IntegerCompressor(dec, 32);
        ic_packet_size = new IntegerCompressor(dec, 32);
        ic_return_point = new IntegerCompressor(dec, 32);
        ic_xyz = new IntegerCompressor(dec, 32, 3);

        /* create last item */
        last_item = null;
    }

    public void init(PointDataRecord seedItem, int notUsed)
    {
        /* init state */
        last_diff_32 = 0;
        sym_last_offset_diff = 0;

        /* init models and integer compressors */
        dec.initSymbolModel(m_packet_index);
        dec.initSymbolModel(m_offset_diff[0]);
        dec.initSymbolModel(m_offset_diff[1]);
        dec.initSymbolModel(m_offset_diff[2]);
        dec.initSymbolModel(m_offset_diff[3]);
        ic_offset_diff.initDecompressor();
        ic_packet_size.initDecompressor();
        ic_return_point.initDecompressor();
        ic_xyz.initDecompressor();

        /* init last item */
        last_item = new PointDataRecordWavepacket((PointDataRecordWavepacket)seedItem);
    }

    public PointDataRecord read(int notUsed)
    {
        PointDataRecordWavepacket result = new PointDataRecordWavepacket();
        result.DescriptorIndex = (short)dec.decodeSymbol(m_packet_index);
            
        sym_last_offset_diff = dec.decodeSymbol(m_offset_diff[sym_last_offset_diff]);
      
        if (sym_last_offset_diff == 0)
        {
            result.OffsetToWaveformData = last_item.OffsetToWaveformData;
        }
        else if (sym_last_offset_diff == 1)
        {
            result.OffsetToWaveformData = last_item.OffsetToWaveformData + last_item.PacketSize;
        }
        else if (sym_last_offset_diff == 2)
        {
            last_diff_32 = ic_offset_diff.decompress(last_diff_32);
            result.OffsetToWaveformData = last_item.OffsetToWaveformData + last_diff_32;
        }
        else
        {
            result.OffsetToWaveformData = dec.readInt64();
        }
      
        result.PacketSize = ic_packet_size.decompress((int)last_item.PacketSize);
        result.setReturnPointWaveformLocation( ic_return_point.decompress(last_item.getReturnPointWaveformLocationAsInt()) );
        result.setDx( ic_xyz.decompress(last_item.getDxAsInt(), 0) );
        result.setDy( ic_xyz.decompress(last_item.getDyAsInt(), 1) );
        result.setDz( ic_xyz.decompress(last_item.getDzAsInt(), 2));
      
        last_item = new PointDataRecordWavepacket(result);

        return result;
    }

    @Override
    public boolean chunk_sizes() {
        return false;
    }

}
