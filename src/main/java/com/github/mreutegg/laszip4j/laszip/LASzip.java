/*
 * Copyright 2007-2013, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.BYTE;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.GPSTIME11;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.POINT10;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.POINT14;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.RGB12;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.RGBNIR14;
import static com.github.mreutegg.laszip4j.laszip.LASitem.Type.WAVEPACKET13;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASzip {

    private static final PrintStream stderr = System.err;

    public static final int LASZIP_VERSION_MAJOR                = 2;
    public static final int LASZIP_VERSION_MINOR                = 4;
    public static final int LASZIP_VERSION_REVISION             = 1;
    public static final int LASZIP_VERSION_BUILD_DATE      = 150923;

    public static final char LASZIP_COMPRESSOR_NONE              = 0;
    public static final char LASZIP_COMPRESSOR_POINTWISE         = 1;
    public static final char LASZIP_COMPRESSOR_POINTWISE_CHUNKED = 2;
    public static final char LASZIP_COMPRESSOR_TOTAL_NUMBER_OF   = 3;

    public static final char LASZIP_COMPRESSOR_CHUNKED = LASZIP_COMPRESSOR_POINTWISE_CHUNKED;
    public static final char LASZIP_COMPRESSOR_NOT_CHUNKED = LASZIP_COMPRESSOR_POINTWISE;

    public static final char LASZIP_COMPRESSOR_DEFAULT = LASZIP_COMPRESSOR_CHUNKED;

    public static final int LASZIP_CODER_ARITHMETIC             = 0;
    public static final int LASZIP_CODER_TOTAL_NUMBER_OF        = 1;

    public static final int LASZIP_CHUNK_SIZE_DEFAULT           = 50000;

    // pack to and unpack from VLR
    byte[] bytes; // unsigned

    // stored in LASzip VLR data section
    public char compressor;
    public char coder;
    public byte version_major; // unsigned
    public byte version_minor; // unsigned
    public char version_revision;
    public int options; // unsigned
    public int chunk_size; // unsigned
    public long number_of_special_evlrs; /* must be -1 if unused */
    public long offset_to_special_evlrs; /* must be -1 if unused */
    public char num_items;
    public LASitem[] items;

    private String error_string;

    public LASzip()
    {
        compressor = LASZIP_COMPRESSOR_DEFAULT;
        coder = LASZIP_CODER_ARITHMETIC;
        version_major = LASZIP_VERSION_MAJOR;
        version_minor = LASZIP_VERSION_MINOR;
        version_revision = LASZIP_VERSION_REVISION;
        options = 0;
        num_items = 0;
        chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        number_of_special_evlrs = -1;
        offset_to_special_evlrs = -1;
        error_string = null;
        items = null;
        bytes = null;
    }

    // unpack from VLR data
    boolean unpack(byte[] bytes, int num)
    {
        // check input
        if (num < 34) return return_error("too few bytes to unpack");
        if (((num - 34) % 6) != 0) return return_error("wrong number bytes to unpack");
        if (((num - 34) / 6) == 0) return return_error("zero items to unpack");
        num_items = (char) ((num - 34) / 6);

        // create item list
        items = new LASitem[num_items];

        // do the unpacking
        int i;
        int b = 0;
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        compressor = buffer.getChar(b);
        b += 2;
        coder = buffer.getChar(b);
        b += 2;
        version_major = buffer.get(b);
        b += 1;
        version_minor = buffer.get(b);
        b += 1;
        version_revision = buffer.getChar(b);
        b += 2;
        options = buffer.getInt(b);
        b += 4;
        chunk_size = buffer.getInt(b);
        b += 4;
        number_of_special_evlrs = buffer.getLong(b);
        b += 8;
        offset_to_special_evlrs = buffer.getLong(b);
        b += 8;
        num_items = buffer.getChar(b);
        b += 2;
        for (i = 0; i < num_items; i++)
        {
            items[i].type = LASitem.Type.fromOrdinal(buffer.getChar(b));
            b += 2;
            items[i].size = buffer.getChar(b);
            b += 2;
            items[i].version = buffer.getChar(b);
            b += 2;
        }
        assert(num == b);

        // check if we support the contents

        for (i = 0; i < num_items; i++)
        {
            if (!check_item(items[i])) return false;
        }
        return true;
    }

    // pack to VLR data
    boolean pack(byte[][] bytes, int[] num)
    {
        // check if we support the contents
        if (!check()) return false;

        // prepare output
        num[0] = 34 + 6*num_items;
        this.bytes = bytes[0] = new byte[num[0]];

        // pack
        int i;
        int b = 0;
        ByteBuffer buffer = ByteBuffer.wrap(bytes[0]).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putChar(b, compressor);
        b += 2;
        buffer.putChar(b, coder);
        b += 2;
        buffer.put(b, version_major);
        b += 1;
        buffer.put(b, version_minor);
        b += 1;
        buffer.putChar(b, version_revision);
        b += 2;
        buffer.putInt(b, options);
        b += 4;
        buffer.putInt(b, chunk_size);
        b += 4;
        buffer.putLong(b, number_of_special_evlrs);
        b += 8;
        buffer.putLong(b, offset_to_special_evlrs);
        b += 8;
        buffer.putChar(b, num_items);
        b += 2;
        for (i = 0; i < num_items; i++)
        {
            buffer.putChar(b, (char) items[i].type.ordinal());
            b += 2;
            buffer.putChar(b, items[i].size);
            b += 2;
            buffer.putChar(b, items[i].version);
            b += 2;
        }
        assert(num[0] == b);
        return true;
    }

    public String get_error()
    {
        return error_string;
    }

    boolean return_error(String error)
    {
        error_string = String.format("%s (LASzip v%d.%dr%d)", error, LASZIP_VERSION_MAJOR, LASZIP_VERSION_MINOR, LASZIP_VERSION_REVISION);
        return false;
    }

    boolean check_compressor(char compressor)
    {
        if (compressor < LASZIP_COMPRESSOR_TOTAL_NUMBER_OF) return true;
        String error = String.format("compressor %d not supported", (int) compressor);
        return return_error(error);
    }

    boolean check_coder(char coder)
    {
        if (coder < LASZIP_CODER_TOTAL_NUMBER_OF) return true;
        String error = String.format("coder %d not supported", (int) coder);
        return return_error(error);
    }

    boolean check_item(LASitem item)
    {
        switch (item.type)
        {
            case POINT10:
                if (item.size != 20) return return_error("POINT10 has size != 20");
                if (item.version > 2) return return_error("POINT10 has version > 2");
                break;
            case GPSTIME11:
                if (item.size != 8) return return_error("GPSTIME11 has size != 8");
                if (item.version > 2) return return_error("GPSTIME11 has version > 2");
                break;
            case RGB12:
                if (item.size != 6) return return_error("RGB12 has size != 6");
                if (item.version > 2) return return_error("RGB12 has version > 2");
                break;
            case WAVEPACKET13:
                if (item.size != 29) return return_error("WAVEPACKET13 has size != 29");
                if (item.version > 1) return return_error("WAVEPACKET13 has version > 1");
                break;
            case BYTE:
                if (item.size < 1) return return_error("BYTE has size < 1");
                if (item.version > 2) return return_error("BYTE has version > 2");
                break;
            case POINT14:
                if (item.size != 30) return return_error("POINT14 has size != 30");
                if (item.version > 0) return return_error("POINT14 has version > 0");
                break;
            case RGBNIR14:
                if (item.size != 8) return return_error("RGBNIR14 has size != 8");
                if (item.version > 0) return return_error("RGBNIR14 has version > 0");
                break;
            default:
                {
                    String error = String.format("item unknown (%d,%d,%d)", item.type.ordinal(), (int) item.size, (int) item.version);
                    return return_error(error);
                }
        }
        return true;
    }

    boolean check_items(char num_items, LASitem[] items)
    {
        if (num_items == 0) return return_error("number of items cannot be zero");
        if (items == null) return return_error("items pointer cannot be NULL");
        int i;
        for (i = 0; i < num_items; i++)
        {
            if (!check_item(items[i])) return false;
        }
        return true;
    }

    public boolean check()
    {
        if (!check_compressor(compressor)) return false;
        if (!check_coder(coder)) return false;
        if (!check_items(num_items, items)) return false;
        return true;
    }

    boolean request_compatibility_mode(char requested_compatibility_mode)
    {
        if (num_items != 0) return return_error("request compatibility mode before calling setup()");
        if (requested_compatibility_mode > 1)
        {
            return return_error("compatibility mode larger than 1 not supported");
        }
        if (requested_compatibility_mode != 0)
        {
            options = options | 0x00000001;
        }
        else
        {
            options = options & 0xFFFFFFFE;
        }
        return true;
    }

    boolean setup(byte u_point_type, char point_size, char compressor)
    {
        if (!check_compressor(compressor)) return false;

        this.num_items = 0;
        this.items = null;
        char[] _num_items = new char[1];
        LASitem[][] _items = new LASitem[1][];
        if (!setup(_num_items, _items, u_point_type, point_size, compressor)) return false;
        this.num_items = _num_items[0];
        this.items = _items[0];

        this.compressor = compressor;
        if (this.compressor == LASZIP_COMPRESSOR_POINTWISE_CHUNKED)
        {
            if (chunk_size == 0) chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        }
        return true;
    }

    boolean setup(char num_items, LASitem[] items, char compressor)
    {
        // check input
        if (!check_compressor(compressor)) return false;
        if (!check_items(num_items, items)) return false;

        // setup compressor
        this.compressor = compressor;
        if (this.compressor == LASZIP_COMPRESSOR_POINTWISE_CHUNKED)
        {
            if (chunk_size == 0) chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        }

        // prepare items
        this.num_items = 0;
        this.num_items = num_items;
        this.items = new LASitem[num_items];

        // setup items
        int i;
        for (i = 0; i < this.items.length; i++)
        {
            this.items[i] = items[i];
        }

        return true;
    }

    boolean setup(char[] num_items, LASitem[][] items, byte u_point_type, char point_size, char compressor)
    {
        boolean compatible = FALSE;
        boolean have_point14 = FALSE;
        boolean have_gps_time = FALSE;
        boolean have_rgb = FALSE;
        boolean have_nir = FALSE;
        boolean have_wavepacket = FALSE;
        int extra_bytes_number = 0;

        // turns on LAS 1.4 compatibility mode 

        if ((options & 1) != 0) compatible = TRUE;

        // switch over the point types we know
        switch (u_point_type)
        {
            case 0:
                extra_bytes_number = (int)point_size - 20;
                break;
            case 1:
                have_gps_time = TRUE;
                extra_bytes_number = (int)point_size - 28;
                break;
            case 2:
                have_rgb = TRUE;
                extra_bytes_number = (int)point_size - 26;
                break;
            case 3:
                have_gps_time = TRUE;
                have_rgb = TRUE;
                extra_bytes_number = (int)point_size - 34;
                break;
            case 4:
                have_gps_time = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 57;
                break;
            case 5:
                have_gps_time = TRUE;
                have_rgb = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 63;
                break;
            case 6:
                have_point14 = TRUE;
                extra_bytes_number = (int)point_size - 30;
                break;
            case 7:
                have_point14 = TRUE;
                have_rgb = TRUE;
                extra_bytes_number = (int)point_size - 36;
                break;
            case 8:
                have_point14 = TRUE;
                have_rgb = TRUE;
                have_nir = TRUE;
                extra_bytes_number = (int)point_size - 38;
                break;
            case 9:
                have_point14 = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 59;
                break;
            case 10:
                have_point14 = TRUE;
                have_rgb = TRUE;
                have_nir = TRUE;
                have_wavepacket = TRUE;
                extra_bytes_number = (int)point_size - 67;
                break;
            default:
                {
                    String error = String.format("point type %d unknown", u_point_type);
                    return return_error(error);
                }
        }

        if (extra_bytes_number < 0)
        {
            //    char error[64];
            //    sprintf(error, "point size %d too small for point type %d by %d bytes", point_size, point_type, -extra_bytes_number);
            //    return return_error(error);
            fprintf(stderr, "WARNING: point size %d too small by %d bytes for point type %d. assuming point_size of %d\n", point_size, -extra_bytes_number, u_point_type, point_size-extra_bytes_number);
            extra_bytes_number = 0;
        }

        // maybe represent new LAS 1.4 as corresponding LAS 1.3 points plus extra bytes for compatibility
        if (have_point14 && compatible)
        {
            // we need 4 extra bytes for the new point attributes
            extra_bytes_number += 5;
            // we store the GPS time separately
            have_gps_time = TRUE;
            // we do not use the point14 item
            have_point14 = FALSE;
            // if we have NIR ...
            if (have_nir)
            {
                // we need another 2 extra bytes 
                extra_bytes_number += 2;
                // we do not use the NIR item
                have_nir = FALSE;
            }
        }

        // create item description

        num_items[0] = (char) (1 + asInt(have_gps_time) + asInt(have_rgb) + asInt(have_wavepacket) + asInt(extra_bytes_number != 0));
        items[0] = new LASitem[num_items[0]];
        for (int i = 0; i < items[0].length; i++) {
            items[0][i] = new LASitem();
        }

        int i = 1;
        if (have_point14)
        {
            items[0][0].type = POINT14;
            items[0][0].size = 30;
            items[0][0].version = 0;
        }
        else
        {
            items[0][0].type = POINT10;
            items[0][0].size = 20;
            items[0][0].version = 0;
        }
        if (have_gps_time)
        {
            items[0][i].type = GPSTIME11;
            items[0][i].size = 8;
            items[0][i].version = 0;
            i++;
        }
        if (have_rgb)
        {
            if (have_nir)
            {
                items[0][i].type = RGBNIR14;
                items[0][i].size = 8;
                items[0][i].version = 0;
            }
            else
            {
                items[0][i].type = RGB12;
                items[0][i].size = 6;
                items[0][i].version = 0;
            }
            i++;
        }
        if (have_wavepacket)
        {
            items[0][i].type = WAVEPACKET13;
            items[0][i].size = 29;
            items[0][i].version = 0;
            i++;
        }
        if (extra_bytes_number != 0)
        {
            items[0][i].type = BYTE;
            items[0][i].size = (char) extra_bytes_number;
            items[0][i].version = 0;
            i++;
        }
        if (compressor != 0) request_version((char) 2);
        assert(i == num_items[0]);
        return true;
    }

    boolean set_chunk_size(int u_chunk_size)
    {
        if (num_items == 0) return return_error("call setup() before setting chunk size");
        if (this.compressor == LASZIP_COMPRESSOR_POINTWISE_CHUNKED)
        {
            this.chunk_size = u_chunk_size;
            return true;
        }
        return false;
    }

    boolean request_version(char requested_version)
    {
        if (num_items == 0) return return_error("call setup() before requesting version");
        if (compressor == LASZIP_COMPRESSOR_NONE)
        {
            if (requested_version > 0) return return_error("without compression version is always 0");
        }
        else
        {
            if (requested_version < 1) return return_error("with compression version is at least 1");
            if (requested_version > 2) return return_error("version larger than 2 not supported");
        }
        char i;
        for (i = 0; i < num_items; i++)
        {
            switch (items[i].type)
            {
                case POINT10:
                case GPSTIME11:
                case RGB12:
                case BYTE:
                    items[i].version = requested_version;
                    break;
                case WAVEPACKET13:
                    items[i].version = 1; // no version 2
                    break;
                default:
                    return return_error("item type not supported");
            }
        }
        return true;
    }

    boolean is_standard(byte[] point_type, char[] record_length)
    {
        return is_standard(num_items, items, point_type, record_length);
    }

    boolean is_standard(char num_items, LASitem[] items, byte[] point_type, char[] record_length)
    {
        if (items == null) return return_error("LASitem array is zero");

        // this is always true
        if (point_type != null) point_type[0] = 127;
        if (record_length != null)
        {
            char i;
            record_length[0] = 0;
            for (i = 0; i < num_items; i++)
            {
                record_length[0] += items[i].size;
            }
        }

        // the minimal number of items is 1
        if (num_items < 1) return return_error("less than one LASitem entries");
        // the maximal number of items is 5
        if (num_items > 5) return return_error("more than five LASitem entries");

        if (items[0].is_type(POINT10))
        {
            // consider all the POINT10 combinations
            if (num_items == 1)
            {
                if (point_type != null) point_type[0] = 0;
                if (record_length != null) assert(record_length[0] == 20);
                return true;
            }
            else
            {
                if (items[1].is_type(GPSTIME11))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 1;
                        if (record_length != null) assert(record_length[0] == 28);
                        return true;
                    }
                    else
                    {
                        if (items[2].is_type(RGB12))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 3;
                                if (record_length != null) assert(record_length[0] == 34);
                                return true;
                            }
                            else
                            {
                                if (items[3].is_type(WAVEPACKET13))
                                {
                                    if (num_items == 4)
                                    {
                                        if (point_type != null) point_type[0] = 5;
                                        if (record_length != null) assert(record_length[0] == 63);
                                        return true;
                                    }
                                    else
                                    {
                                        if (items[4].is_type(BYTE))
                                        {
                                            if (num_items == 5)
                                            {
                                                if (point_type != null) point_type[0] = 5;
                                                if (record_length != null) assert(record_length[0] == (63 + items[4].size));
                                                return true;
                                            }
                                        }
                                    }
                                }
                                else if (items[3].is_type(BYTE))
                                {
                                    if (num_items == 4)
                                    {
                                        if (point_type != null) point_type[0] = 3;
                                        if (record_length != null) assert(record_length[0] == (34 + items[3].size));
                                        return true;
                                    }
                                }
                            }
                        }
                        else if (items[2].is_type(WAVEPACKET13))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 4;
                                if (record_length != null) assert(record_length[0] == 57);
                                return true;
                            }
                            else
                            {
                                if (items[3].is_type(BYTE))
                                {
                                    if (num_items == 4)
                                    {
                                        if (point_type != null) point_type[0] = 4;
                                        if (record_length != null) assert(record_length[0] == (57 + items[3].size));
                                        return true;
                                    }
                                }
                            }
                        }
                        else if (items[2].is_type(BYTE))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 1;
                                if (record_length != null) assert(record_length[0] == (28 + items[2].size));
                                return true;
                            }
                        }
                    }
                }
                else if (items[1].is_type(RGB12))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 2;
                        if (record_length != null) assert(record_length[0] == 26);
                        return true;
                    }
                    else
                    {
                        if (items[2].is_type(BYTE))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 2;
                                if (record_length != null) assert(record_length[0] == (26 + items[2].size));
                                return true;
                            }
                        }
                    }
                }
                else if (items[1].is_type(BYTE))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 0;
                        if (record_length != null) assert(record_length[0] == (20 + items[1].size));
                        return true;
                    }
                }
            }
        }
        else if (items[0].is_type(POINT14))
        {
            // consider all the POINT14 combinations
            if (num_items == 1)
            {
                if (point_type != null) point_type[0] = 6;
                if (record_length != null) assert(record_length[0] == 30);
                return true;
            }
            else
            {
                if (items[1].is_type(RGB12))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 7;
                        if (record_length != null) assert(record_length[0] == 36);
                        return true;
                    }
                    else
                    {
                        if (items[2].is_type(BYTE))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 7;
                                if (record_length != null) assert(record_length[0] == (36 + items[2].size));
                                return true;
                            }
                        }
                    }
                }
                else if (items[1].is_type(RGBNIR14))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 8;
                        if (record_length != null) assert(record_length[0] == 38);
                        return true;
                    }
                    else
                    {
                        if (items[2].is_type(WAVEPACKET13))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 10;
                                if (record_length != null) assert(record_length[0] == 67);
                                return true;
                            }
                            else
                            {
                                if (items[3].is_type(BYTE))
                                {
                                    if (num_items == 4)
                                    {
                                        if (point_type != null) point_type[0] = 10;
                                        if (record_length != null) assert(record_length[0] == (67 + items[3].size));
                                        return true;
                                    }
                                }
                            }
                        }
                        else if (items[2].is_type(BYTE))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 8;
                                if (record_length != null) assert(record_length[0] == (38 + items[2].size));
                                return true;
                            }
                        }
                    }
                }
                else if (items[1].is_type(WAVEPACKET13))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 9;
                        if (record_length != null) assert(record_length[0] == 59);
                        return true;
                    }
                    else
                    {
                        if (items[2].is_type(BYTE))
                        {
                            if (num_items == 3)
                            {
                                if (point_type != null) point_type[0] = 9;
                                if (record_length != null) assert(record_length[0] == (59 + items[2].size));
                                return true;
                            }
                        }
                    }
                }
                else if (items[1].is_type(BYTE))
                {
                    if (num_items == 2)
                    {
                        if (point_type != null) point_type[0] = 6;
                        if (record_length != null) assert(record_length[0] == (30 + items[1].size));
                        return true;
                    }
                }
            }
        }
        else
        {
            return_error("first LASitem is neither POINT10 nor POINT14");
        }
        return return_error("LASitem array does not match LAS specification 1.4");
    }

    private static int asInt(boolean b) {
        return b ? 1 : 0;
    }

}
