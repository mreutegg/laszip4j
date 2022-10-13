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

import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_Z;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_FLAGS;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_INTENSITY;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_USER_DATA;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME;
import static com.github.mreutegg.laszip4j.laszip.Common_v3.number_return_level_8ctx;
import static com.github.mreutegg.laszip4j.laszip.Common_v3.number_return_map_6ctx;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_ZERO_BIT_0;

public class LASreadItemCompressed_POINT14_v3 extends LASreadItemCompressed {
  
    private IByteStreamInProvider instreamProvider;
    private ByteStreamInArray instream_channel_returns_XY;
    private ByteStreamInArray instream_Z;
    private ByteStreamInArray instream_classification;
    private ByteStreamInArray instream_flags;
    private ByteStreamInArray instream_intensity;
    private ByteStreamInArray instream_scan_angle;
    private ByteStreamInArray instream_user_data;
    private ByteStreamInArray instream_point_source;
    private ByteStreamInArray instream_gps_time;

    private ArithmeticDecoder dec_channel_returns_XY;
    private ArithmeticDecoder dec_Z;
    private ArithmeticDecoder dec_classification;
    private ArithmeticDecoder dec_flags;
    private ArithmeticDecoder dec_intensity;
    private ArithmeticDecoder dec_scan_angle;
    private ArithmeticDecoder dec_user_data;
    private ArithmeticDecoder dec_point_source;
    private ArithmeticDecoder dec_gps_time;

    private boolean changed_Z;
    private boolean changed_classification;
    private boolean changed_flags;
    private boolean changed_intensity;
    private boolean changed_scan_angle;
    private boolean changed_user_data;
    private boolean changed_point_source;
    private boolean changed_gps_time;

    private boolean requested_Z;
    private boolean requested_classification;
    private boolean requested_flags;
    private boolean requested_intensity;
    private boolean requested_scan_angle;
    private boolean requested_user_data;
    private boolean requested_point_source;
    private boolean requested_gps_time;

    private int num_bytes_channel_returns_XY;
    private int num_bytes_Z;
    private int num_bytes_classification;
    private int num_bytes_flags;
    private int num_bytes_intensity;
    private int num_bytes_scan_angle;
    private int num_bytes_user_data;
    private int num_bytes_point_source;
    private int num_bytes_gps_time;

    private int current_context;

    private LAScontextPOINT14[] contexts = new LAScontextPOINT14[4];

    public static final int LASZIP_GPSTIME_MULTI = 500;
    
    public static final int LASZIP_GPSTIME_MULTI_MINUS = -10;
    public static final int LASZIP_GPSTIME_MULTI_CODE_FULL = (LASZIP_GPSTIME_MULTI - LASZIP_GPSTIME_MULTI_MINUS + 1);

    public static final int LASZIP_GPSTIME_MULTI_TOTAL = (LASZIP_GPSTIME_MULTI - LASZIP_GPSTIME_MULTI_MINUS + 5);

    public LASreadItemCompressed_POINT14_v3(IByteStreamInProvider instreamProvider, int decompress_selective) {
    
        assert(instreamProvider != null);
        this.instreamProvider = instreamProvider;

        /* zero instreams and decoders */

        instream_channel_returns_XY = null;
        instream_Z = null;
        instream_classification = null;
        instream_flags = null;
        instream_intensity = null;
        instream_scan_angle = null;
        instream_user_data = null;
        instream_point_source = null;
        instream_gps_time = null;

        dec_channel_returns_XY = null;
        dec_Z = null;
        dec_classification = null;
        dec_flags = null;
        dec_intensity = null;
        dec_scan_angle = null;
        dec_user_data = null;
        dec_point_source = null;
        dec_gps_time = null;

        /* mark the four scanner channel contexts as uninitialized */

        for (int i = 0; i < contexts.length; i++)
        {
            contexts[i] = new LAScontextPOINT14();
            contexts[i].initialized = false;
        }
        current_context = 0;

        /* zero num_bytes and init booleans */

        num_bytes_channel_returns_XY = 0;
        num_bytes_Z = 0;
        num_bytes_classification = 0;
        num_bytes_flags = 0;
        num_bytes_intensity = 0;
        num_bytes_scan_angle = 0;
        num_bytes_user_data = 0;
        num_bytes_point_source = 0;
        num_bytes_gps_time = 0;

        changed_Z = false;
        changed_classification = false;
        changed_flags = false;
        changed_intensity = false;
        changed_scan_angle = false;
        changed_user_data = false;
        changed_point_source = false;
        changed_gps_time = false;

        requested_Z = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_Z) != 0;
        requested_classification = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION) != 0;
        requested_flags = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_FLAGS) != 0;
        requested_intensity = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_INTENSITY) != 0;
        requested_scan_angle = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE)!= 0;
        requested_user_data = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_USER_DATA) != 0;
        requested_point_source = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE) != 0;
        requested_gps_time = (decompress_selective & LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME) != 0;
    }

    @Override
    public void init(PointDataRecord seedItem, int notUsed) {

        ByteStreamIn instream = instreamProvider.getByteStreamIn();

        /* on the first init create instreams and decoders */

        if (null == instream_channel_returns_XY)
        {
            /* create instreams */

            instream_channel_returns_XY = new ByteStreamInArray();
            instream_Z = new ByteStreamInArray();
            instream_classification = new ByteStreamInArray();
            instream_flags = new ByteStreamInArray();
            instream_intensity = new ByteStreamInArray();
            instream_scan_angle = new ByteStreamInArray();
            instream_user_data = new ByteStreamInArray();
            instream_point_source = new ByteStreamInArray();
            instream_gps_time = new ByteStreamInArray();

            /* create decoders */

            dec_channel_returns_XY = new ArithmeticDecoder();
            dec_Z = new ArithmeticDecoder();
            dec_classification = new ArithmeticDecoder();
            dec_flags = new ArithmeticDecoder();
            dec_intensity = new ArithmeticDecoder();
            dec_scan_angle = new ArithmeticDecoder();
            dec_user_data = new ArithmeticDecoder();
            dec_point_source = new ArithmeticDecoder();
            dec_gps_time = new ArithmeticDecoder();
        }

        /* load the requested bytes and init the corresponding instreams and decoders */

        byte[] bytes = new byte[num_bytes_channel_returns_XY];
        instream.getBytes(bytes, num_bytes_channel_returns_XY);
        instream_channel_returns_XY.init(bytes, num_bytes_channel_returns_XY);
        dec_channel_returns_XY.init(instream_channel_returns_XY);

        if (requested_Z)
        {
            if (num_bytes_Z > 0 )
            {
                bytes = new byte[num_bytes_Z];
                instream.getBytes(bytes, num_bytes_Z);
                instream_Z.init(bytes, num_bytes_Z);
                dec_Z.init(instream_Z);
                changed_Z = true;
            }
            else
            {
                instream_Z.init(null, 0L);
                changed_Z = false;
            }
        }
        else 
        {
            if (num_bytes_Z > 0)
            {
                instream.skipBytes(num_bytes_Z);
            }
            changed_Z = false;
        }

        if (requested_classification)
        {
            if (num_bytes_classification > 0)
            {
                bytes = new byte[num_bytes_classification];
                instream.getBytes(bytes, num_bytes_classification);
                instream_classification.init(bytes, num_bytes_classification);
                dec_classification.init(instream_classification);
                changed_classification = true;
            }
            else
            {
                instream_classification.init(null, 0L);
                changed_classification = false;
            }
        }
        else 
        {
            if (num_bytes_classification > 0)
            {
                instream.skipBytes(num_bytes_classification);
            }
            changed_classification = false;
        }

        if (requested_flags)
        {
            if (num_bytes_flags > 0)
            {
                bytes = new byte[num_bytes_flags];
                instream.getBytes(bytes, num_bytes_flags);
                instream_flags.init(bytes, num_bytes_flags);
                dec_flags.init(instream_flags);
                changed_flags = true;
            }
            else
            {
                instream_flags.init(null, 0L);
                changed_flags = false;
            }
        }
        else 
        {
            if (num_bytes_flags > 0)
            {
                instream.skipBytes(num_bytes_flags);
            }
            changed_flags = false;
        }

        if (requested_intensity)
        {
            if (num_bytes_intensity > 0)
            {
                bytes = new byte[num_bytes_intensity];
                instream.getBytes(bytes, num_bytes_intensity);
                instream_intensity.init(bytes, num_bytes_intensity);
                dec_intensity.init(instream_intensity);
                changed_intensity = true;
            }
            else
            {
                instream_intensity.init(null, 0L);
                changed_intensity = false;
            }
        }
        else 
        {
            if (num_bytes_intensity > 0)
            {
                instream.skipBytes(num_bytes_intensity);
            }
            changed_intensity = false;
        }

        if (requested_scan_angle)
        {
            if (num_bytes_scan_angle > 0)
            {
                bytes = new byte[num_bytes_scan_angle];
                instream.getBytes(bytes, num_bytes_scan_angle);
                instream_scan_angle.init(bytes, num_bytes_scan_angle);
                dec_scan_angle.init(instream_scan_angle);
                changed_scan_angle = true;
            }
            else
            {
                instream_scan_angle.init(null, 0L);
                changed_scan_angle = false;
            }
        }
        else 
        {
            if (num_bytes_scan_angle > 0)
            {
                instream.skipBytes(num_bytes_scan_angle);
            }
            changed_scan_angle = false;
        }

        if (requested_user_data)
        {
            if (num_bytes_user_data > 0)
            {
                bytes = new byte[num_bytes_user_data];
                instream.getBytes(bytes, num_bytes_user_data);
                instream_user_data.init(bytes, num_bytes_user_data);
                dec_user_data.init(instream_user_data);
                changed_user_data = true;
            }
            else
            {
                instream_user_data.init(null, 0L);
                changed_user_data = false;
            }
        }
        else 
        {
            if (num_bytes_user_data > 0)
            {
                instream.skipBytes(num_bytes_user_data);
            }
            changed_user_data = false;
        }

        if (requested_point_source)
        {
            if (num_bytes_point_source > 0)
            {
                bytes = new byte[num_bytes_point_source];
                instream.getBytes(bytes, num_bytes_point_source);
                instream_point_source.init(bytes, num_bytes_point_source);
                dec_point_source.init(instream_point_source);
                changed_point_source = true;
            }
            else
            {
                instream_point_source.init(null, 0L);
                changed_point_source = false;
            }
        }
        else
        {
            if (num_bytes_point_source > 0)
            {
                instream.skipBytes(num_bytes_point_source);
            }
            changed_point_source = false;
        }

        if (requested_gps_time)
        {
            if (num_bytes_gps_time > 0)
            {
                bytes = new byte[num_bytes_gps_time];
                instream.getBytes(bytes, num_bytes_gps_time);
                instream_gps_time.init(bytes, num_bytes_gps_time);
                dec_gps_time.init(instream_gps_time);
                changed_gps_time = true;
            }
            else
            {
                instream_gps_time.init(null, 0L);
                changed_gps_time = false;
            }
        }
        else
        {
            if (num_bytes_gps_time > 0)
            {
                instream.skipBytes(num_bytes_gps_time);
            }
            changed_gps_time = false;
        }

        /* mark the four scanner channel contexts as unused */

        for (int c = 0; c < 4; c++)
        {
            contexts[c].unused = true;
        }

        /* set scanner channel as current context */

        current_context = ((PointDataRecordPoint14)seedItem).getScannerChannel();

        /* create and init models and decompressors */

        createAndInitModelsAndDecompressors(current_context, (PointDataRecordPoint14)seedItem);
    }

    @Override
    public PointDataRecord read(int context) 
    {
        // get last

        PointDataRecordPoint14 last_item = contexts[current_context].last_item;

        ////////////////////////////////////////
        // decompress returns_XY layer 
        ////////////////////////////////////////

        // create single (3) / first (1) / last (2) / intermediate (0) context from last point return

        int lpr = (last_item.getReturnNumber() == 1 ? 1 : 0); // first?
        int nrgp = last_item.getNumberOfReturns();
        lpr += (last_item.getReturnNumber() >= nrgp ? 2 : 0); // last?

        // add info whether the GPS time changed in the last return to the context

        lpr += (last_item.gps_time_change ? 4 : 0);

        // decompress which values have changed with last point return context

        int changed_values = dec_channel_returns_XY.decodeSymbol(contexts[current_context].m_changed_values[lpr]);

        // if scanner channel has changed

        if ((changed_values & (1 << 6)) != 0)
        {
            int diff = dec_channel_returns_XY.decodeSymbol(contexts[current_context].m_scanner_channel); // curr = last + (sym + 1)
            int scanner_channel = (current_context + diff + 1) % 4;
            // maybe create and init entropy models and integer compressors
            if (contexts[scanner_channel].unused)
            {
                // create and init entropy models and integer decompressors
                createAndInitModelsAndDecompressors(scanner_channel, contexts[current_context].last_item);
            }
            // switch context to current scanner channel
            current_context = scanner_channel;

            // get last for new context
            last_item = contexts[current_context].last_item;
            last_item.setScannerChannel( (byte)scanner_channel );
        }

        // determine changed attributes

        boolean point_source_change = (changed_values & (1 << 5)) != 0;
        boolean gps_time_change = (changed_values & (1 << 4)) != 0;
        boolean scan_angle_change = (changed_values & (1 << 3)) != 0;

        // get last return counts

        int last_n = last_item.getNumberOfReturns();
        int last_r = last_item.getReturnNumber();

        // if number of returns is different we decompress it

        int n;
        if ((changed_values & (1 << 2)) != 0)
        {
            if (contexts[current_context].m_number_of_returns[last_n] == null)
            {
                contexts[current_context].m_number_of_returns[last_n] = dec_channel_returns_XY.createSymbolModel(16);
                dec_channel_returns_XY.initSymbolModel(contexts[current_context].m_number_of_returns[last_n]);
            }
            n = dec_channel_returns_XY.decodeSymbol(contexts[current_context].m_number_of_returns[last_n]);
            last_item.setNumberOfReturns((byte)n);
        }
        else
        {
            n = last_n;
        }

        // how is the return number different

        int r;
        if ((changed_values & 3) == 0) // same return number
        {
            r = last_r;
        }
        else if ((changed_values & 3) == 1) // return number plus 1 mod 16
        {
            r = ((last_r + 1) % 16);
            last_item.setReturnNumber((byte)r);
        }
        else if ((changed_values & 3) == 2) // return number minus 1 mod 16
        {
            r = ((last_r + 15) % 16);
            last_item.setReturnNumber((byte)r);
        }
        else
        {
            // the return number difference is bigger than +1 / -1 so we decompress how it is different

            if (gps_time_change) // if the GPS time has changed
            {
                if (contexts[current_context].m_return_number[last_r] == null)
                {
                    contexts[current_context].m_return_number[last_r] = dec_channel_returns_XY.createSymbolModel(16);
                    dec_channel_returns_XY.initSymbolModel(contexts[current_context].m_return_number[last_r]);
                }
                r = dec_channel_returns_XY.decodeSymbol(contexts[current_context].m_return_number[last_r]);
            }
            else // if the GPS time has not changed
            {
                int sym = dec_channel_returns_XY.decodeSymbol(contexts[current_context].m_return_number_gps_same);
                r = (last_r + (sym + 2)) % 16;
            }
            last_item.setReturnNumber((byte)r);
        }

        // get return map m and return level l context for current point

        int m = number_return_map_6ctx[n][r];
        int l = number_return_level_8ctx[n][r];

        // create single (3) / first (1) / last (2) / intermediate (0) return context for current point

        int cpr = (r == 1 ? 2 : 0); // first ?
        cpr += (r >= n ? 1 : 0); // last ?

        int k_bits;
        int median, diff;

        // decompress X coordinate
        median = contexts[current_context].last_X_diff_median5[(m<<1) | (gps_time_change?1:0)].get();
        diff = contexts[current_context].ic_dX.decompress(median, n==1?1:0);
        last_item.X += diff;
        contexts[current_context].last_X_diff_median5[(m<<1) | (gps_time_change?1:0)].add(diff);

        // decompress Y coordinate
        median = contexts[current_context].last_Y_diff_median5[(m<<1) | (gps_time_change?1:0)].get();
        k_bits = contexts[current_context].ic_dX.getK();
        diff = contexts[current_context].ic_dY.decompress(median, (n==1?1:0) + ( k_bits < 20 ? U32_ZERO_BIT_0(k_bits) : 20 ));
        last_item.Y += diff;
        contexts[current_context].last_Y_diff_median5[(m<<1) | (gps_time_change?1:0)].add(diff);

        ////////////////////////////////////////
        // decompress Z layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_Z) // if the Z coordinate should be decompressed and changes within this chunk
        {
            k_bits = (contexts[current_context].ic_dX.getK() + contexts[current_context].ic_dY.getK()) / 2;
            last_item.Z = contexts[current_context].ic_Z.decompress((int)contexts[current_context].last_Z[l], (n==1?1:0) + (k_bits < 18 ? U32_ZERO_BIT_0(k_bits) : 18));
            contexts[current_context].last_Z[l] = last_item.Z;
        }

        ////////////////////////////////////////
        // decompress classifications layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_classification) // if the classification should be decompressed and changes within this chunk
        {
            int last_classification = last_item.Classification;
            int ccc = ((last_classification & 0x1F) << 1) + (cpr == 3 ? 1 : 0);
            if (contexts[current_context].m_classification[ccc] == null)
            {
                contexts[current_context].m_classification[ccc] = dec_classification.createSymbolModel(256);
                dec_classification.initSymbolModel(contexts[current_context].m_classification[ccc]);
            }
            last_item.Classification = (byte)dec_classification.decodeSymbol(contexts[current_context].m_classification[ccc]);
        }

        ////////////////////////////////////////
        // decompress flags layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_flags) // if the flags should be decompressed and change within this chunk
        {
            // These flag bit locations don't agree with the las spec because the scanner channel bits are omitted from 'flags' here.
            // So scan_direction_flag is at bit 4, and edge_of_flight_line is at bit 5
            // (classification flags are per spec, 4 bits covering bits 0-3)
            
            int last_flags = ((last_item.hasScanFlag(ScanFlag.EdgeOfFlightLine)?1:0) << 5) |
                             ((last_item.hasScanFlag(ScanFlag.ScanDirection)?1:0) << 4) |
                             last_item.getClassificationFlags();
            if (contexts[current_context].m_flags[last_flags] == null)
            {
                contexts[current_context].m_flags[last_flags] = dec_flags.createSymbolModel(64);
                dec_flags.initSymbolModel(contexts[current_context].m_flags[last_flags]);
            }
            int flags = dec_flags.decodeSymbol(contexts[current_context].m_flags[last_flags]);
            last_item.setScanDirection( ((flags >>> 4) & 1) == 1 );
            last_item.setEdgeOfFlightLine( ((flags >>> 5) & 1) == 1 );
            last_item.setClassificationFlags( (byte)(flags & 0x0F) );
        }

        ////////////////////////////////////////
        // decompress intensity layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_intensity) // if the intensity should be decompressed and changes within this chunk
        {
            int intensity = contexts[current_context].ic_intensity.decompress(
                contexts[current_context].last_intensity[(cpr<<1) | (gps_time_change?1:0)], cpr);
            contexts[current_context].last_intensity[(cpr<<1) | (gps_time_change?1:0)] = (char)intensity;
            last_item.Intensity = (char)intensity;
        }

        ////////////////////////////////////////
        // decompress scan_angle layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_scan_angle) // if the scan angle should be decompressed and changes within this chunk
        {
            if (scan_angle_change) // if the scan angle has actually changed
            {
                last_item.ScanAngle = (short)contexts[current_context].ic_scan_angle.decompress(last_item.ScanAngle, (gps_time_change?1:0));
            }
        }

        ////////////////////////////////////////
        // decompress user_data layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_user_data) // if the user data should be decompressed and changes within this chunk
        {
            if (contexts[current_context].m_user_data[last_item.UserData/4] == null)
            {
                contexts[current_context].m_user_data[last_item.UserData/4] = dec_user_data.createSymbolModel(256);
                dec_user_data.initSymbolModel(contexts[current_context].m_user_data[last_item.UserData/4]);
            }
            last_item.UserData = (short)dec_user_data.decodeSymbol(contexts[current_context].m_user_data[last_item.UserData/4] );
        }

        

        ////////////////////////////////////////
        // decompress point_source layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_point_source) // if the point source ID should be decompressed and changes within this chunk
        {
            if (point_source_change) // if the point source ID has actually changed
            {
                last_item.PointSourceID = (char)contexts[current_context].ic_point_source_ID.decompress(last_item.PointSourceID);
            }
        }

        ////////////////////////////////////////
        // decompress gps_time layer (if changed and requested)
        ////////////////////////////////////////

        if (changed_gps_time) // if the GPS time should be decompressed and changes within this chunk
        {
            if (gps_time_change) // if the GPS time has actually changed
            {
                read_gps_time();
                long x = contexts[current_context].last_gpstime[contexts[current_context].last];
                if ( x <= 0 )
                last_item.GPSTime = last_item.GPSTime;
                else
                last_item.GPSTime = x;
            }
        }

        PointDataRecordPoint14 result = new PointDataRecordPoint14(last_item);
        result.CompressionContext = current_context;

        // remember if the last point had a gps_time_change
        last_item.gps_time_change = gps_time_change;

        return result; 
    }

    @Override
    public boolean chunk_sizes() {

        ByteStreamIn instream = instreamProvider.getByteStreamIn();

        /* read bytes per layer */

        num_bytes_channel_returns_XY = instream.get32bitsLE();
        num_bytes_Z = instream.get32bitsLE();
        num_bytes_classification = instream.get32bitsLE();
        num_bytes_flags = instream.get32bitsLE();
        num_bytes_intensity = instream.get32bitsLE();
        num_bytes_scan_angle = instream.get32bitsLE();
        num_bytes_user_data = instream.get32bitsLE();
        num_bytes_point_source = instream.get32bitsLE();
        num_bytes_gps_time = instream.get32bitsLE();

        return true;
    }

    private boolean createAndInitModelsAndDecompressors(int context, PointDataRecordPoint14 seedItem)
    {
        /* should only be called when context is unused */
      
        assert(contexts[context].unused);
      
        /* first create all entropy models and integer decompressors (if needed) */
      
        if ( !contexts[context].initialized )
        {
          /* for the channel_returns_XY layer */
      
          contexts[context].m_changed_values[0] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[1] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[2] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[3] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[4] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[5] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[6] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_changed_values[7] = dec_channel_returns_XY.createSymbolModel(128);
          contexts[context].m_scanner_channel = dec_channel_returns_XY.createSymbolModel(3);
          for (int i = 0; i < 16; i++)
          {
            contexts[context].m_number_of_returns[i] = null;
            contexts[context].m_return_number[i] = null;
          }
          contexts[context].m_return_number_gps_same = dec_channel_returns_XY.createSymbolModel(13);
      
          contexts[context].ic_dX = new IntegerCompressor(dec_channel_returns_XY, 32, 2);  // 32 bits, 2 context
          contexts[context].ic_dY = new IntegerCompressor(dec_channel_returns_XY, 32, 22); // 32 bits, 22 contexts
      
          /* for the Z layer */
      
          contexts[context].ic_Z = new IntegerCompressor(dec_Z, 32, 20);  // 32 bits, 20 contexts
      
          /* for the classification layer */
          /* for the flags layer */
          /* for the user_data layer */
      
          for (int i = 0; i < 64; i++)
          {
            contexts[context].m_classification[i] = null;
            contexts[context].m_flags[i] = null;
            contexts[context].m_user_data[i] = null;
          }
      
          /* for the intensity layer */
      
          contexts[context].ic_intensity = new IntegerCompressor(dec_intensity, 16, 4);
      
          /* for the scan_angle layer */
      
          contexts[context].ic_scan_angle = new IntegerCompressor(dec_scan_angle, 16, 2);
      
          /* for the point_source_ID layer */
      
          contexts[context].ic_point_source_ID = new IntegerCompressor(dec_point_source, 16);
      
          /* for the gps_time layer */
      
          contexts[context].m_gpstime_multi = dec_gps_time.createSymbolModel(LASZIP_GPSTIME_MULTI_TOTAL);
          contexts[context].m_gpstime_0diff = dec_gps_time.createSymbolModel(5);
          contexts[context].ic_gpstime = new IntegerCompressor(dec_gps_time, 32, 9); // 32 bits, 9 contexts

          contexts[context].initialized = true;
        }
      
        /* then init entropy models and integer compressors */
      
        /* for the channel_returns_XY layer */
      
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[0]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[1]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[2]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[3]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[4]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[5]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[6]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_changed_values[7]);
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_scanner_channel);
        for (int i = 0; i < 16; i++)
        {
          if (null != contexts[context].m_number_of_returns[i]) dec_channel_returns_XY.initSymbolModel(contexts[context].m_number_of_returns[i]);
          if (null != contexts[context].m_return_number[i]) dec_channel_returns_XY.initSymbolModel(contexts[context].m_return_number[i]);
        }
        dec_channel_returns_XY.initSymbolModel(contexts[context].m_return_number_gps_same);
        contexts[context].ic_dX.initDecompressor();
        contexts[context].ic_dY.initDecompressor();
        for (int i = 0; i < 12; i++)
        {
          contexts[context].last_X_diff_median5[i].init();
          contexts[context].last_Y_diff_median5[i].init();
        }

        /* for the Z layer */
      
        contexts[context].ic_Z.initDecompressor();
        for (int i = 0; i < 8; i++)
        {
          contexts[context].last_Z[i] = seedItem.Z;
        }
      
        /* for the classification layer */
        /* for the flags layer */
        /* for the user_data layer */
      
        for (int i = 0; i < 64; i++)
        {
          if (null != contexts[context].m_classification[i]) dec_classification.initSymbolModel(contexts[context].m_classification[i]);
          if (null != contexts[context].m_flags[i]) dec_flags.initSymbolModel(contexts[context].m_flags[i]);
          if (null != contexts[context].m_user_data[i]) dec_user_data.initSymbolModel(contexts[context].m_user_data[i]);
        }
      
        /* for the intensity layer */
      
        contexts[context].ic_intensity.initDecompressor();
        for (int i = 0; i < 8; i++)
        {
            contexts[context].last_intensity[i] = seedItem.Intensity;
        }
      
        /* for the scan_angle layer */
      
        contexts[context].ic_scan_angle.initDecompressor();
      
        /* for the point_source_ID layer */
      
        contexts[context].ic_point_source_ID.initDecompressor();
      
        /* for the gps_time layer */
      
        dec_gps_time.initSymbolModel(contexts[context].m_gpstime_multi);
        dec_gps_time.initSymbolModel(contexts[context].m_gpstime_0diff);
        contexts[context].ic_gpstime.initDecompressor();
        contexts[context].last = 0;
        contexts[context].next = 0;
        contexts[context].last_gpstime_diff[0] = 0;
        contexts[context].last_gpstime_diff[1] = 0;
        contexts[context].last_gpstime_diff[2] = 0;
        contexts[context].last_gpstime_diff[3] = 0;
        contexts[context].multi_extreme_counter[0] = 0;
        contexts[context].multi_extreme_counter[1] = 0;
        contexts[context].multi_extreme_counter[2] = 0;
        contexts[context].multi_extreme_counter[3] = 0;
        contexts[context].last_gpstime[0] = seedItem.GPSTime;
        contexts[context].last_gpstime[1] = 0;
        contexts[context].last_gpstime[2] = 0;
        contexts[context].last_gpstime[3] = 0;
      
        /* init current context from last item */
      
        contexts[context].last_item = seedItem;
        contexts[context].last_item.gps_time_change = false;
            
        contexts[context].unused = false;
      
        return true;
    }

    void read_gps_time()
    {
      int multi;
      if (contexts[current_context].last_gpstime_diff[contexts[current_context].last] == 0) // if the last integer difference was zero
      {
        multi = dec_gps_time.decodeSymbol(contexts[current_context].m_gpstime_0diff);
        if (multi == 0) // the difference can be represented with 32 bits
        {
          contexts[current_context].last_gpstime_diff[contexts[current_context].last] = contexts[current_context].ic_gpstime.decompress(0, 0);
          contexts[current_context].last_gpstime[contexts[current_context].last] += contexts[current_context].last_gpstime_diff[contexts[current_context].last];
          contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0; 
        }
        else if (multi == 1) // the difference is huge
        {
          contexts[current_context].next = (contexts[current_context].next+1)&3;
          contexts[current_context].last_gpstime[contexts[current_context].next] = contexts[current_context].ic_gpstime.decompress((int)(contexts[current_context].last_gpstime[contexts[current_context].last] >>> 32), 8);
          contexts[current_context].last_gpstime[contexts[current_context].next] = (contexts[current_context].last_gpstime[contexts[current_context].next] << 32);
          contexts[current_context].last_gpstime[contexts[current_context].next] |= dec_gps_time.readInt();
          contexts[current_context].last = contexts[current_context].next;
          contexts[current_context].last_gpstime_diff[contexts[current_context].last] = 0;
          contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0; 
        }
        else // we switch to another sequence
        {
          contexts[current_context].last = (contexts[current_context].last+multi-1)&3;
          read_gps_time();
        }
      }
      else
      {
        multi = dec_gps_time.decodeSymbol(contexts[current_context].m_gpstime_multi);
        if (multi == 1)
        {
          contexts[current_context].last_gpstime[contexts[current_context].last] += contexts[current_context].ic_gpstime.decompress(contexts[current_context].last_gpstime_diff[contexts[current_context].last], 1);
          contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0;
        }
        else if (multi < LASZIP_GPSTIME_MULTI_CODE_FULL)
        {
          int gpstime_diff;
          if (multi == 0)
          {
            gpstime_diff = contexts[current_context].ic_gpstime.decompress(0, 7);
            contexts[current_context].multi_extreme_counter[contexts[current_context].last]++;
            if (contexts[current_context].multi_extreme_counter[contexts[current_context].last] > 3)
            {
              contexts[current_context].last_gpstime_diff[contexts[current_context].last] = gpstime_diff;
              contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0;
            }
          }
          else if (multi < LASZIP_GPSTIME_MULTI)
          {
            if (multi < 10)
              gpstime_diff = contexts[current_context].ic_gpstime.decompress(multi*contexts[current_context].last_gpstime_diff[contexts[current_context].last], 2);
            else
              gpstime_diff = contexts[current_context].ic_gpstime.decompress(multi*contexts[current_context].last_gpstime_diff[contexts[current_context].last], 3);
          }
          else if (multi == LASZIP_GPSTIME_MULTI)
          {
            gpstime_diff = contexts[current_context].ic_gpstime.decompress(LASZIP_GPSTIME_MULTI*contexts[current_context].last_gpstime_diff[contexts[current_context].last], 4);
            contexts[current_context].multi_extreme_counter[contexts[current_context].last]++;
            if (contexts[current_context].multi_extreme_counter[contexts[current_context].last] > 3)
            {
              contexts[current_context].last_gpstime_diff[contexts[current_context].last] = gpstime_diff;
              contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0;
            }
          }
          else
          {
            multi = LASZIP_GPSTIME_MULTI - multi;
            if (multi > LASZIP_GPSTIME_MULTI_MINUS)
            {
              gpstime_diff = contexts[current_context].ic_gpstime.decompress(multi*contexts[current_context].last_gpstime_diff[contexts[current_context].last], 5);
            }
            else
            {
              gpstime_diff = contexts[current_context].ic_gpstime.decompress(LASZIP_GPSTIME_MULTI_MINUS*contexts[current_context].last_gpstime_diff[contexts[current_context].last], 6);
              contexts[current_context].multi_extreme_counter[contexts[current_context].last]++;
              if (contexts[current_context].multi_extreme_counter[contexts[current_context].last] > 3)
              {
                contexts[current_context].last_gpstime_diff[contexts[current_context].last] = gpstime_diff;
                contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0;
              }
            }
          }
          contexts[current_context].last_gpstime[contexts[current_context].last] += gpstime_diff;
        }
        else if (multi ==  LASZIP_GPSTIME_MULTI_CODE_FULL)
        {
          contexts[current_context].next = (contexts[current_context].next+1)&3;
          contexts[current_context].last_gpstime[contexts[current_context].next] = contexts[current_context].ic_gpstime.decompress((int)((contexts[current_context].last_gpstime[contexts[current_context].last]) >>> 32), 8);
          contexts[current_context].last_gpstime[contexts[current_context].next] = contexts[current_context].last_gpstime[contexts[current_context].next] << 32;
          contexts[current_context].last_gpstime[contexts[current_context].next] |= Integer.toUnsignedLong(dec_gps_time.readInt());
          contexts[current_context].last = contexts[current_context].next;
          contexts[current_context].last_gpstime_diff[contexts[current_context].last] = 0;
          contexts[current_context].multi_extreme_counter[contexts[current_context].last] = 0; 
        }
        else if (multi >=  LASZIP_GPSTIME_MULTI_CODE_FULL)
        {
          contexts[current_context].last = (contexts[current_context].last+multi-LASZIP_GPSTIME_MULTI_CODE_FULL)&3;
          read_gps_time();
        }
      }
    }
        
}
