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

import java.nio.ByteBuffer;

import static com.github.mreutegg.laszip4j.laszip.U64I64F64.newU64I64F64Array;
import static java.lang.Boolean.TRUE;

public class LASreadItemCompressed_GPSTIME11_v2 extends LASreadItemCompressed {

    private static int LASZIP_GPSTIME_MULTI = 500;
    private static int LASZIP_GPSTIME_MULTI_MINUS = -10;
    private static int LASZIP_GPSTIME_MULTI_UNCHANGED = (LASZIP_GPSTIME_MULTI - LASZIP_GPSTIME_MULTI_MINUS + 1);
    private static int LASZIP_GPSTIME_MULTI_CODE_FULL = (LASZIP_GPSTIME_MULTI - LASZIP_GPSTIME_MULTI_MINUS + 2);

    private static int LASZIP_GPSTIME_MULTI_TOTAL = (LASZIP_GPSTIME_MULTI - LASZIP_GPSTIME_MULTI_MINUS + 6);

    private ArithmeticDecoder dec;
    private int last, next; // unsigned
    private U64I64F64[] last_gpstime = newU64I64F64Array(4);
    private int[] last_gpstime_diff = new int[4];
    private int[] multi_extreme_counter = new int[4];

    private ArithmeticModel m_gpstime_multi;
    private ArithmeticModel m_gpstime_0diff;
    private IntegerCompressor ic_gpstime;

    public LASreadItemCompressed_GPSTIME11_v2(ArithmeticDecoder dec)
    {
        /* set decoder */
        assert(dec != null);
        this.dec = dec;
        /* create entropy models and integer compressors */
        m_gpstime_multi = dec.createSymbolModel(LASZIP_GPSTIME_MULTI_TOTAL);
        m_gpstime_0diff = dec.createSymbolModel(6);
        ic_gpstime = new IntegerCompressor(dec, 32, 9); // 32 bits, 9 contexts
    }

    public boolean init(byte[] item)
    {
        /* init state */
        last = 0; next = 0;
        last_gpstime_diff[0] = 0;
        last_gpstime_diff[1] = 0;
        last_gpstime_diff[2] = 0;
        last_gpstime_diff[3] = 0;
        multi_extreme_counter[0] = 0;
        multi_extreme_counter[1] = 0;
        multi_extreme_counter[2] = 0;
        multi_extreme_counter[3] = 0;

        /* init models and integer compressors */
        dec.initSymbolModel(m_gpstime_multi);
        dec.initSymbolModel(m_gpstime_0diff);
        ic_gpstime.initDecompressor();

        /* init last item */
        last_gpstime[0].setU64(ByteBuffer.wrap(item).getLong());
        last_gpstime[1].setU64(0);
        last_gpstime[2].setU64(0);
        last_gpstime[3].setU64(0);
        return TRUE;
    }

    public void read(byte[] item)
    {
        int multi;
        if (last_gpstime_diff[last] == 0) // if the last integer difference was zero
        {
            multi = dec.decodeSymbol(m_gpstime_0diff);
            if (multi == 1) // the difference can be represented with 32 bits
            {
                last_gpstime_diff[last] = ic_gpstime.decompress(0, 0);
                last_gpstime[last].setI64(last_gpstime[last].getI64() + last_gpstime_diff[last]);
                multi_extreme_counter[last] = 0;
            }
            else if (multi == 2) // the difference is huge
            {
                next = (next+1)&3;
                last_gpstime[next].setU64(ic_gpstime.decompress((int)(last_gpstime[last].getU64() >>> 32), 8));
                last_gpstime[next].setU64(last_gpstime[next].getU64() << 32);
                last_gpstime[next].setU64(last_gpstime[next].getU64() | dec.readInt());
                last = next;
                last_gpstime_diff[last] = 0;
                multi_extreme_counter[last] = 0;
            }
            else if (multi > 2) // we switch to another sequence
            {
                last = (last+multi-2)&3;
                read(item);
            }
        }
        else
        {
            multi = dec.decodeSymbol(m_gpstime_multi);
            if (multi == 1)
            {
                last_gpstime[last].setI64(last_gpstime[last].getI64() + ic_gpstime.decompress(last_gpstime_diff[last], 1));
                multi_extreme_counter[last] = 0;
            }
            else if (multi < LASZIP_GPSTIME_MULTI_UNCHANGED)
            {
                int gpstime_diff;
                if (multi == 0)
                {
                    gpstime_diff = ic_gpstime.decompress(0, 7);
                    multi_extreme_counter[last]++;
                    if (multi_extreme_counter[last] > 3)
                    {
                        last_gpstime_diff[last] = gpstime_diff;
                        multi_extreme_counter[last] = 0;
                    }
                }
                else if (multi < LASZIP_GPSTIME_MULTI)
                {
                    if (multi < 10)
                        gpstime_diff = ic_gpstime.decompress(multi*last_gpstime_diff[last], 2);
                    else
                        gpstime_diff = ic_gpstime.decompress(multi*last_gpstime_diff[last], 3);
                }
                else if (multi == LASZIP_GPSTIME_MULTI)
                {
                    gpstime_diff = ic_gpstime.decompress(LASZIP_GPSTIME_MULTI*last_gpstime_diff[last], 4);
                    multi_extreme_counter[last]++;
                    if (multi_extreme_counter[last] > 3)
                    {
                        last_gpstime_diff[last] = gpstime_diff;
                        multi_extreme_counter[last] = 0;
                    }
                }
                else
                {
                    multi = LASZIP_GPSTIME_MULTI - multi;
                    if (multi > LASZIP_GPSTIME_MULTI_MINUS)
                    {
                        gpstime_diff = ic_gpstime.decompress(multi*last_gpstime_diff[last], 5);
                    }
                    else
                    {
                        gpstime_diff = ic_gpstime.decompress(LASZIP_GPSTIME_MULTI_MINUS*last_gpstime_diff[last], 6);
                        multi_extreme_counter[last]++;
                        if (multi_extreme_counter[last] > 3)
                        {
                            last_gpstime_diff[last] = gpstime_diff;
                            multi_extreme_counter[last] = 0;
                        }
                    }
                }
                last_gpstime[last].setI64(last_gpstime[last].getI64() + gpstime_diff);
            }
            else if (multi ==  LASZIP_GPSTIME_MULTI_CODE_FULL)
            {
                next = (next+1)&3;
                last_gpstime[next].setU64(ic_gpstime.decompress((int)(last_gpstime[last].getU64() >>> 32), 8));
                last_gpstime[next].setU64(last_gpstime[next].getU64() << 32);
                last_gpstime[next].setU64(last_gpstime[next].getU64() + Integer.toUnsignedLong(dec.readInt()));
                last = next;
                last_gpstime_diff[last] = 0;
                multi_extreme_counter[last] = 0;
            }
            else if (multi >=  LASZIP_GPSTIME_MULTI_CODE_FULL)
            {
                last = (last+multi-LASZIP_GPSTIME_MULTI_CODE_FULL)&3;
                read(item);
            }
        }
        ByteBuffer.wrap(item).putLong(last_gpstime[last].getI64());
    }
}
