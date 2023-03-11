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
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U8_CLAMP;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_RGB;

public class LASreadItemCompressed_RGB14_v3 extends LASreadItemCompressed{
 
  private IByteStreamInProvider instreamProvider;
  private ByteStreamInArray instream_RGB;
  private ArithmeticDecoder dec_RGB;
  private boolean changed_RGB;
  private int num_bytes_RGB;
  private boolean requested_RGB;
  private int current_context;
  private LAScontextRGB14[] contexts = new LAScontextRGB14[4];

  public LASreadItemCompressed_RGB14_v3(IByteStreamInProvider instreamProvider, int decompress_selective)
  {
    assert(instreamProvider != null);
    this.instreamProvider = instreamProvider;

    /* zero instreams and decoders */
    instream_RGB = null;
    dec_RGB = null;

    /* zero num_bytes and init booleans */
    num_bytes_RGB = 0;
    changed_RGB = false;
    requested_RGB = ((decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_RGB) != 0) ? true : false;

    /* mark the four scanner channel contexts as uninitialized */
    for (int c = 0; c < contexts.length; c++)
    {
        contexts[c] = new LAScontextRGB14();
        contexts[c].m_byte_used = null;
    }
    current_context = 0;
  }

  @Override
  public void init(PointDataRecord seedItem, int context) {

    ByteStreamIn instream = instreamProvider.getByteStreamIn();

    /* on the first init create instreams and decoders */

    if (instream_RGB == null)
    {
        /* create instreams */

        instream_RGB = new ByteStreamInArray();

        /* create decoders */

        dec_RGB = new ArithmeticDecoder();
    }
    
    /* load the requested bytes and init the corresponding instreams an decoders */

    if (requested_RGB)
    {
        if (num_bytes_RGB != 0)
        {
            byte[] bytes = new byte[num_bytes_RGB];
            instream.getBytes(bytes, num_bytes_RGB);
            instream_RGB.init(bytes, num_bytes_RGB);
            dec_RGB.init(instream_RGB);
            changed_RGB = true;
        }
        else
        {
            instream_RGB.init(null, 0);
            changed_RGB = false;
        }
    }
    else
    {
        if (num_bytes_RGB != 0)
        {
            instream.skipBytes(num_bytes_RGB);
        }
        changed_RGB = false;
    }

    /* mark the four scanner channel contexts as unused */
    for (int c = 0; c < 4; c++)
    {
        contexts[c].unused = true;
    }

    current_context = context; // all other items use context set by POINT14 reader

    createAndInitModelsAndDecompressors(current_context, (PointDataRecordRGB)seedItem);
  }
  
  @Override
  public boolean chunk_sizes() {
    ByteStreamIn instream = instreamProvider.getByteStreamIn();

    /* read bytes per layer */

    num_bytes_RGB = instream.get32bitsLE();

    return true;
  }
  
  @Override
  public PointDataRecord read(int context) {

    PointDataRecordRGB result = new PointDataRecordRGB();

    // get last
    PointDataRecordRGB last_item = contexts[current_context].last_item;

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

    if (changed_RGB)
    {
      int corr;
      int diff = 0;
      int sym = dec_RGB.decodeSymbol(contexts[current_context].m_byte_used);
      if ((sym & (1 << 0)) != 0)
      {
        corr = dec_RGB.decodeSymbol(contexts[current_context].m_rgb_diff_0);
        byte b = U8_FOLD(corr + (last_item.R & 255));
        result.R = (char)Byte.toUnsignedInt(b);
      }
      else 
      {
        result.R = (char)(last_item.R&0xFF);
      }
      if ((sym & (1 << 1)) != 0)
      {
        corr = dec_RGB.decodeSymbol(contexts[current_context].m_rgb_diff_1);
        byte b = U8_FOLD(corr + (last_item.R >>> 8));
        result.R |= (((char) Byte.toUnsignedInt(b)) << 8);
      }
      else
      {
        result.R |= (last_item.R&0xFF00);
      }
      if ((sym & (1 << 6)) != 0)
      {
        diff = (result.R&0x00FF) - (last_item.R&0x00FF);
        if ((sym & (1 << 2)) != 0)
        {
          corr = dec_RGB.decodeSymbol(contexts[current_context].m_rgb_diff_2);
          byte b = U8_FOLD(corr + U8_CLAMP(diff + (last_item.G & 255)));
          result.G = (char) Byte.toUnsignedInt(b);
        }
        else
        {
          result.G = (char)(last_item.G&0xFF);
        }
        if ((sym & (1 << 4)) != 0)
        {
          corr = dec_RGB.decodeSymbol(contexts[current_context].m_rgb_diff_4);
          diff = (diff + ((result.G&0x00FF) - (last_item.G&0x00FF))) / 2;
          byte b = U8_FOLD(corr + U8_CLAMP(diff + (last_item.B & 255)));
          result.B = (char) Byte.toUnsignedInt(b);
        }
        else
        {
          result.B = (char)(last_item.B&0xFF);
        }
        diff = (result.R>>>8) - (last_item.R>>>8);
        if ((sym & (1 << 3)) != 0)
        {
          corr = dec_RGB.decodeSymbol(contexts[current_context].m_rgb_diff_3);
          byte b = U8_FOLD(corr + U8_CLAMP(diff + (last_item.G >>> 8)));
          result.G |= (((char) Byte.toUnsignedInt(b)) << 8);
        }
        else
        {
          result.G |= (last_item.G&0xFF00);
        }
        if ((sym & (1 << 5)) != 0)
        {
          corr = dec_RGB.decodeSymbol(contexts[current_context].m_rgb_diff_5);
          diff = (diff + ((result.G>>>8) - (last_item.G>>>8))) / 2;
          byte b = U8_FOLD(corr + U8_CLAMP(diff + (last_item.B >>> 8)));
          result.B |= (((char) Byte.toUnsignedInt(b)) << 8);
        }
        else
        {
          result.B |= (last_item.B&0xFF00);
        }
      }
      else
      {
        result.G = result.R;
        result.B = result.R;
      }
      last_item.R = result.R;
      last_item.G = result.G;
      last_item.B = result.B;
    }
    else
    {
        result.R=last_item.R;
        result.G=last_item.G;
        result.B=last_item.B;
    }

    return result;
  }

  private boolean createAndInitModelsAndDecompressors(int context, PointDataRecordRGB seedItem)
  {
    /* should only be called when context is unused */
    assert(contexts[context].unused);

    /* first create all entropy models (if needed) */

    if (contexts[context].m_byte_used == null)
    {
        contexts[context].m_byte_used = dec_RGB.createSymbolModel(128);
        contexts[context].m_rgb_diff_0 = dec_RGB.createSymbolModel(256);
        contexts[context].m_rgb_diff_1 = dec_RGB.createSymbolModel(256);
        contexts[context].m_rgb_diff_2 = dec_RGB.createSymbolModel(256);
        contexts[context].m_rgb_diff_3 = dec_RGB.createSymbolModel(256);
        contexts[context].m_rgb_diff_4 = dec_RGB.createSymbolModel(256);
        contexts[context].m_rgb_diff_5 = dec_RGB.createSymbolModel(256);
    }

    /* then init entropy models */

    dec_RGB.initSymbolModel(contexts[context].m_byte_used);
    dec_RGB.initSymbolModel(contexts[context].m_rgb_diff_0);
    dec_RGB.initSymbolModel(contexts[context].m_rgb_diff_1);
    dec_RGB.initSymbolModel(contexts[context].m_rgb_diff_2);
    dec_RGB.initSymbolModel(contexts[context].m_rgb_diff_3);
    dec_RGB.initSymbolModel(contexts[context].m_rgb_diff_4);
    dec_RGB.initSymbolModel(contexts[context].m_rgb_diff_5);

    /* init current context from item */
    contexts[context].last_item = new PointDataRecordRGB(seedItem);

    contexts[context].unused = false;

    return true;
  }    
}
