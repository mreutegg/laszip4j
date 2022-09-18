/*
  COPYRIGHT:

    (c) 2007-2022, rapidlasso GmbH - fast tools to catch reality

    This is free software; you can redistribute and/or modify it under the
    terms of the Apache Public License 2.0 published by the Apache Software
    Foundation. See the COPYING file for more information.

    This software is distributed WITHOUT ANY WARRANTY and without even the
    implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET;

public class LASreadItemCompressed_WAVEPACKET14_v3 extends LASreadItemCompressed{

    private IByteStreamInProvider instreamProvider;

    private ByteStreamInArray instream_wavepacket;

    private ArithmeticDecoder dec_wavepacket;

    boolean changed_wavepacket;

    int num_bytes_wavepacket;

    boolean requested_wavepacket;

    int current_context;
    private LAScontextWAVEPACKET14[] contexts = new LAScontextWAVEPACKET14[4];

    public LASreadItemCompressed_WAVEPACKET14_v3(IByteStreamInProvider instreamProvider, int decompress_selective) {
        
        assert(instreamProvider!=null);
        this.instreamProvider = instreamProvider;

        /* zero instreams and decoders */

        instream_wavepacket = null;

        dec_wavepacket = null;

        /* zero num_bytes and init booleans */

        num_bytes_wavepacket = 0;

        changed_wavepacket = false;

        requested_wavepacket = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET) != 0;

        /* mark the four scanner channel contexts as uninitialized */

        for (int c = 0; c < contexts.length; c++)
        {
            contexts[c] = new LAScontextWAVEPACKET14();
            contexts[c].m_packet_index = null;
        }
        current_context = 0;
    }

    @Override
    public void init(PointDataRecord seedItem, int context) {

        ByteStreamIn instream = instreamProvider.getByteStreamIn();

        /* on the first init create instreams and decoders */

        if (instream_wavepacket == null)
        {
            /* create instreams */

            instream_wavepacket = new ByteStreamInArray();

            /* create decoders */

            dec_wavepacket = new ArithmeticDecoder();
        }

        /* load the requested bytes and init the corresponding instreams an decoders */

        if (requested_wavepacket)
        {
            if (num_bytes_wavepacket != 0)
            {
                byte[] bytes = new byte[num_bytes_wavepacket];
                instream.getBytes(bytes, num_bytes_wavepacket);
                instream_wavepacket.init(bytes, num_bytes_wavepacket);
                dec_wavepacket.init(instream_wavepacket);
                changed_wavepacket = true;
            }
            else
            {
                instream_wavepacket.init(null, 0);
                changed_wavepacket = false;
            }
        }
        else
        {
            if (num_bytes_wavepacket != 0)
            {
                instream.skipBytes(num_bytes_wavepacket);
            }
            changed_wavepacket = false;
        }

        /* mark the four scanner channel contexts as unused */

        for (int c = 0; c < 4; c++)
        {
            contexts[c].unused = true;
        }

        /* set scanner channel as current context */

        current_context = context; // all other items use context set by POINT14 reader

        /* create and init models and decompressors */

        createAndInitModelsAndDecompressors(current_context, (PointDataRecordWavepacket)seedItem);
    }

    @Override
    public boolean chunk_sizes() {

        ByteStreamIn instream = instreamProvider.getByteStreamIn();

        /* read bytes per layer */

        num_bytes_wavepacket = instream.get32bitsLE();

        return true;
    }

    @Override
    public PointDataRecord read(int context) {
        // get last

        PointDataRecordWavepacket last_item = contexts[current_context].last_item;

        // check for context switch

        if (current_context != context)
        {
            current_context = context; // all other items use context set by POINT14 reader
            if (contexts[current_context].unused)
            {
                createAndInitModelsAndDecompressors(current_context, last_item);
            }
            last_item = contexts[current_context].last_item;
        }

        // decompress
        PointDataRecordWavepacket result = new PointDataRecordWavepacket();

        if (changed_wavepacket)
        {
            result.DescriptorIndex = (byte)(dec_wavepacket.decodeSymbol(contexts[current_context].m_packet_index));

            contexts[current_context].sym_last_offset_diff = 
              dec_wavepacket.decodeSymbol(contexts[current_context].m_offset_diff[contexts[current_context].sym_last_offset_diff]);

            if (contexts[current_context].sym_last_offset_diff == 0)
            {
                result.OffsetToWaveformData = last_item.OffsetToWaveformData;
            }
            else if (contexts[current_context].sym_last_offset_diff == 1)
            {
                result.OffsetToWaveformData = last_item.OffsetToWaveformData + last_item.PacketSize;
            }
            else if (contexts[current_context].sym_last_offset_diff == 2)
            {
                contexts[current_context].last_diff_32 = contexts[current_context].ic_offset_diff.decompress(contexts[current_context].last_diff_32);
                result.OffsetToWaveformData = last_item.OffsetToWaveformData + contexts[current_context].last_diff_32;
            }
            else
            {
                result.OffsetToWaveformData = dec_wavepacket.readInt64();
            }

            result.PacketSize = contexts[current_context].ic_packet_size.decompress((int)last_item.PacketSize);
            result.setReturnPointWaveformLocation( contexts[current_context].ic_return_point.decompress(last_item.getReturnPointWaveformLocationAsInt()) );
            result.setDx( contexts[current_context].ic_xyz.decompress(last_item.getDxAsInt(), 0) );
            result.setDy( contexts[current_context].ic_xyz.decompress(last_item.getDyAsInt(), 1) );
            result.setDz( contexts[current_context].ic_xyz.decompress(last_item.getDzAsInt(), 2) );
        
            last_item = new PointDataRecordWavepacket(result);
        }

        return result;
    }

    private boolean createAndInitModelsAndDecompressors(int context, PointDataRecordWavepacket seedItem) {
        
        /* should only be called when context is unused */

        assert(contexts[context].unused);

        /* first create all entropy models (if needed) */

        if (requested_wavepacket)
        {
            if (contexts[context].m_packet_index == null)
            {
                contexts[context].m_packet_index = dec_wavepacket.createSymbolModel(256);
                contexts[context].m_offset_diff[0] = dec_wavepacket.createSymbolModel(4);
                contexts[context].m_offset_diff[1] = dec_wavepacket.createSymbolModel(4);
                contexts[context].m_offset_diff[2] = dec_wavepacket.createSymbolModel(4);
                contexts[context].m_offset_diff[3] = dec_wavepacket.createSymbolModel(4);
                contexts[context].ic_offset_diff = new IntegerCompressor(dec_wavepacket, 32);
                contexts[context].ic_packet_size = new IntegerCompressor(dec_wavepacket, 32);
                contexts[context].ic_return_point = new IntegerCompressor(dec_wavepacket, 32);
                contexts[context].ic_xyz = new IntegerCompressor(dec_wavepacket, 32, 3);
            }

            /* then init entropy models */

            dec_wavepacket.initSymbolModel(contexts[context].m_packet_index);
            dec_wavepacket.initSymbolModel(contexts[context].m_offset_diff[0]);
            dec_wavepacket.initSymbolModel(contexts[context].m_offset_diff[1]);
            dec_wavepacket.initSymbolModel(contexts[context].m_offset_diff[2]);
            dec_wavepacket.initSymbolModel(contexts[context].m_offset_diff[3]);
            contexts[context].ic_offset_diff.initDecompressor();
            contexts[context].ic_packet_size.initDecompressor();
            contexts[context].ic_return_point.initDecompressor();
            contexts[context].ic_xyz.initDecompressor();
        }

        /* init current context from item */

        contexts[context].last_diff_32 = 0;
        contexts[context].sym_last_offset_diff = 0;
        contexts[context].last_item = seedItem;

        contexts[context].unused = false;

        return true;
    }
    
}
