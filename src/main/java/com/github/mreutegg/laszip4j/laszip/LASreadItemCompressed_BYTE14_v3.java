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

import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_FOLD;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_BYTE0;

public class LASreadItemCompressed_BYTE14_v3 extends LASreadItemCompressed{

    private IByteStreamInProvider instreamProvider;

    private ByteStreamInArray[] instream_Bytes;
  
    private ArithmeticDecoder[] dec_Bytes;
  
    private int[] num_bytes_Bytes;
  
    private boolean[] changed_Bytes;
  
    private boolean[] requested_Bytes;
  
    private byte[] bytes;
    private int num_bytes_allocated;
  
    private int current_context;
    private LAScontextBYTE14[] contexts = new LAScontextBYTE14[4];
  
    private int number;  

    public LASreadItemCompressed_BYTE14_v3(IByteStreamInProvider instreamProvider, int number, int decompress_selective) {

        assert(instreamProvider != null);
        this.instreamProvider = instreamProvider;

        /* must be more than one byte */

        assert(number != 0);
        this.number = number;

        /* zero instream and decoder pointer arrays */

        instream_Bytes = null;

        dec_Bytes = null;

        /* create and init num_bytes and booleans arrays */

        num_bytes_Bytes = new int[number];

        changed_Bytes = new boolean[number];

        requested_Bytes = new boolean[number];

        for (int i = 0; i < number; i++)
        {
            num_bytes_Bytes[i] = 0;

            changed_Bytes[i] = false;

            if (i > 15) // currently only the first 16 extra bytes can be selectively decompressed
            {
                requested_Bytes[i] = true;
            }
            else
            {
                requested_Bytes[i] = (decompress_selective & (LASZIP_DECOMPRESS_SELECTIVE_BYTE0 << i)) != 0;
            }
        }

        /* init the bytes buffer to zero */

        bytes = null;
        num_bytes_allocated = 0;

        /* mark the four scanner channel contexts as uninitialized */

        for (int c = 0; c < contexts.length; c++)
        {
            contexts[c] = new LAScontextBYTE14();
            contexts[c].m_bytes = null;
        }
        current_context = 0;
    }

    @Override
    public void init(PointDataRecord seedItem, MutableInteger context) {

        int i;

        ByteStreamIn instream = instreamProvider.getByteStreamIn();
      
        /* on the first init create instreams and decoders */
      
        if (instream_Bytes == null)
        {
          /* create instream pointer array */
      
          instream_Bytes = new ByteStreamInArray[number];
      
          /* create instreams */
      
          for (i = 0; i < number; i++)
          {
              instream_Bytes[i] = new ByteStreamInArray();
          }
      
          /* create decoder pointer array */
      
          dec_Bytes = new ArithmeticDecoder[number];
      
          /* create layer decoders */
      
          for (i = 0; i < number; i++)
          {
            dec_Bytes[i] = new ArithmeticDecoder();
          }
        }
      
        /* how many bytes do we need to read */
      
        int num_bytes = 0;
      
        for (i = 0; i < number; i++)
        {
          if (requested_Bytes[i]) num_bytes += num_bytes_Bytes[i];
        }
      
        /* make sure the buffer is sufficiently large */
      
        if (num_bytes > num_bytes_allocated)
        {
          bytes = new byte[num_bytes];
          num_bytes_allocated = num_bytes;
        }
      
        /* load the requested bytes and init the corresponding instreams an decoders */
      
        num_bytes = 0;
        for (i = 0; i < number; i++)
        {
          if (requested_Bytes[i])
          {
            if (num_bytes_Bytes[i] != 0)
            {
              instream.getBytes(bytes, num_bytes_Bytes[i]);
              instream_Bytes[i].init(bytes, num_bytes_Bytes[i]);
              dec_Bytes[i].init(instream_Bytes[i]);
              num_bytes += num_bytes_Bytes[i];
              changed_Bytes[i] = true;
            }
            else
            {
              dec_Bytes[i].init(null);
              changed_Bytes[i] = false;
            }
          }
          else
          {
            if (num_bytes_Bytes[i] != 0)
            {
              instream.skipBytes(num_bytes_Bytes[i]);
            }
            changed_Bytes[i] = false;
          }
        }
      
        /* mark the four scanner channel contexts as unused */
      
        for (int c = 0; c < 4; c++)
        {
          contexts[c].unused = true;
        }
      
        /* set scanner channel as current context */
      
        current_context = context.get(); // all other items use context set by POINT14 reader
      
        /* create and init models and decompressors */
      
        createAndInitModelsAndDecompressors(current_context, (PointDataRecordBytes)seedItem);
    }

    @Override
    public boolean chunk_sizes() {

        ByteStreamIn instream = instreamProvider.getByteStreamIn();
      
        for (int i = 0; i < number; i++)
        {
          /* read bytes per layer */
      
          num_bytes_Bytes[i] = instream.get32bitsLE();
        }
      
        return true;
    }

    @Override
    public PointDataRecord read(MutableInteger context) {
        // get last

        PointDataRecordBytes last_item = contexts[current_context].last_item;

        // check for context switch

        if (current_context != context.get())
        {
            current_context = context.get(); // all other items use context set by POINT14 reader
            if (contexts[current_context].unused)
            {
                createAndInitModelsAndDecompressors(current_context, last_item);
                last_item = contexts[current_context].last_item;
            }
        }

        // decompress
        PointDataRecordBytes result = new PointDataRecordBytes(number);

        for (int i = 0; i < number; i++)
        {
            if (changed_Bytes[i])
            {
                int value = last_item.Bytes[i] + dec_Bytes[i].decodeSymbol(contexts[current_context].m_bytes[i]);
                result.Bytes[i] = last_item.Bytes[i] = U8_FOLD(value);
            }
            else
            {
                result.Bytes[i] = last_item.Bytes[i];
            }
        }

        return result;
    }

    private boolean createAndInitModelsAndDecompressors(int context, PointDataRecordBytes seedItem) {

        int i;

        /* should only be called when context is unused */
      
        assert(contexts[context].unused);
      
        /* first create all entropy models and last items (if needed) */
      
        if (contexts[context].m_bytes == null)
        {
          contexts[context].m_bytes = new ArithmeticModel[number];
          for (i = 0; i < number; i++)
          {
            contexts[context].m_bytes[i] = dec_Bytes[i].createSymbolModel(256);
            dec_Bytes[i].initSymbolModel(contexts[context].m_bytes[i]);
          }
        }
      
        contexts[context].last_item = new PointDataRecordBytes(seedItem);

        /* then init entropy models */      
        for (i = 0; i < number; i++)
        {
          dec_Bytes[i].initSymbolModel(contexts[context].m_bytes[i]);
        }
            
        contexts[context].unused = false;
      
        return true;
    }
    
}
