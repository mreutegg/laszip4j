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

import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_WAVEPACKET13_v1 extends LASreadItemCompressed {

    private ArithmeticDecoder dec;
    private byte[] last_item;

    private int last_diff_32;
    private int sym_last_offset_diff; // unsigned
    private ArithmeticModel m_packet_index;
    private ArithmeticModel[] m_offset_diff = new ArithmeticModel[4];
    private IntegerCompressor ic_offset_diff;
    private IntegerCompressor ic_packet_size;
    private IntegerCompressor ic_return_point;
    private IntegerCompressor ic_xyz;

    LASreadItemCompressed_WAVEPACKET13_v1(ArithmeticDecoder dec)
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
        last_item = new byte[28];
    }

    public boolean init(byte[] item)
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
        System.arraycopy(item, 1, last_item, 0, 28);
        return TRUE;
    }

    public void read(byte[] itemArray)
    {
        ByteBuffer item = ByteBuffer.wrap(itemArray);
        item.put((byte)(dec.decodeSymbol(m_packet_index)));

        LASwavepacket13 this_item_m = new LASwavepacket13();
        LASwavepacket13 last_item_m = LASwavepacket13.unpack(last_item);

        sym_last_offset_diff = dec.decodeSymbol(m_offset_diff[sym_last_offset_diff]);

        if (sym_last_offset_diff == 0)
        {
            this_item_m.setOffset(last_item_m.getOffset());
        }
        else if (sym_last_offset_diff == 1)
        {
            this_item_m.setOffset(last_item_m.getOffset() + last_item_m.getPacket_size());
        }
        else if (sym_last_offset_diff == 2)
        {
            last_diff_32 = ic_offset_diff.decompress(last_diff_32);
            this_item_m.setOffset(last_item_m.getOffset() + last_diff_32);
        }
        else
        {
            this_item_m.setOffset(dec.readInt64());
        }

        this_item_m.setPacket_size(ic_packet_size.decompress(last_item_m.getPacket_size()));
        this_item_m.getReturn_point().setI32(ic_return_point.decompress(last_item_m.getReturn_point().getI32()));
        this_item_m.getX().setI32(ic_xyz.decompress(last_item_m.getX().getI32(), 0));
        this_item_m.getY().setI32(ic_xyz.decompress(last_item_m.getY().getI32(), 1));
        this_item_m.getZ().setI32(ic_xyz.decompress(last_item_m.getZ().getI32(), 2));

        this_item_m.pack(item);

        System.arraycopy(item.array(), 1, last_item, 0, 28);
    }

}
