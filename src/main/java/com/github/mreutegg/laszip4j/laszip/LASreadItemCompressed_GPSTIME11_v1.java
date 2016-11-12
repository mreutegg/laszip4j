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

public class LASreadItemCompressed_GPSTIME11_v1 extends LASreadItemCompressed {

    private static final int LASZIP_GPSTIME_MULTIMAX = 512;

    private ArithmeticDecoder dec;
    private U64I64F64 last_gpstime = new U64I64F64();

    private ArithmeticModel m_gpstime_multi;
    private ArithmeticModel m_gpstime_0diff;
    private IntegerCompressor ic_gpstime;
    private int multi_extreme_counter;
    private int last_gpstime_diff;


    public LASreadItemCompressed_GPSTIME11_v1(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;
        /* create entropy models and integer compressors */
        m_gpstime_multi = dec.createSymbolModel(LASZIP_GPSTIME_MULTIMAX);
        m_gpstime_0diff = dec.createSymbolModel(3);
        ic_gpstime = new IntegerCompressor(dec, 32, 6); // 32 bits, 6 contexts
    }

    public boolean init(byte[] item)
    {
        /* init state */
        last_gpstime_diff = 0;
        multi_extreme_counter = 0;

        /* init models and integer compressors */
        dec.initSymbolModel(m_gpstime_multi);
        dec.initSymbolModel(m_gpstime_0diff);
        ic_gpstime.initDecompressor();

        /* init last item */
        last_gpstime.setU64(ByteBuffer.wrap(item).getLong());
        return TRUE;
    }

    public void read(byte[] item)
    {
        int multi;
        if (last_gpstime_diff == 0) // if the last integer difference was zero
        {
            multi = dec.decodeSymbol(m_gpstime_0diff);
            if (multi == 1) // the difference can be represented with 32 bits
            {
                last_gpstime_diff = ic_gpstime.decompress(0, 0);
                last_gpstime.setI64(last_gpstime.getI64() + last_gpstime_diff);
            }
            else if (multi == 2) // the difference is huge
            {
                last_gpstime.setU64(dec.readInt64());
            }
        }
        else
        {
            multi = dec.decodeSymbol(m_gpstime_multi);

            if (multi <  LASZIP_GPSTIME_MULTIMAX-2)
            {
                int gpstime_diff;
                if (multi == 1)
                {
                    gpstime_diff = ic_gpstime.decompress(last_gpstime_diff, 1);
                    last_gpstime_diff = gpstime_diff;
                    multi_extreme_counter = 0;
                }
                else if (multi == 0)
                {
                    gpstime_diff = ic_gpstime.decompress(last_gpstime_diff/4, 2);
                    multi_extreme_counter++;
                    if (multi_extreme_counter > 3)
                    {
                        last_gpstime_diff = gpstime_diff;
                        multi_extreme_counter = 0;
                    }
                }
                else if (multi < 10)
                {
                    gpstime_diff = ic_gpstime.decompress(multi*last_gpstime_diff, 3);
                }
                else if (multi < 50)
                {
                    gpstime_diff = ic_gpstime.decompress(multi*last_gpstime_diff, 4);
                }
                else
                {
                    gpstime_diff = ic_gpstime.decompress(multi*last_gpstime_diff, 5);
                    if (multi == LASZIP_GPSTIME_MULTIMAX-3)
                    {
                        multi_extreme_counter++;
                        if (multi_extreme_counter > 3)
                        {
                            last_gpstime_diff = gpstime_diff;
                            multi_extreme_counter = 0;
                        }
                    }
                }
                last_gpstime.setI64(last_gpstime.getI64() + gpstime_diff);
            }
            else if (multi <  LASZIP_GPSTIME_MULTIMAX-1)
            {
                last_gpstime.setU64(dec.readInt64());
            }
        }
        ByteBuffer.wrap(item).putLong(last_gpstime.getI64());
    }
}
