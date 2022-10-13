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
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;
import com.github.mreutegg.laszip4j.laszip.ByteStreamInFile;
import com.github.mreutegg.laszip4j.laszip.ByteStreamInStream;
import com.github.mreutegg.laszip4j.laszip.LASattribute;
import com.github.mreutegg.laszip4j.laszip.LASindex;
import com.github.mreutegg.laszip4j.laszip.LASitem;
import com.github.mreutegg.laszip4j.laszip.LASreadPoint;
import com.github.mreutegg.laszip4j.laszip.LASzip;
import com.github.mreutegg.laszip4j.laszip.MyDefs;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAS;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAZ;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_NONE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.sizeof;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.longBitsToDouble;
import static java.lang.Float.intBitsToFloat;

public class LASreaderLAS extends LASreader {

    private static final PrintStream stderr = System.err;

    private RandomAccessFile file;
    private ByteStreamIn stream;
    private LASreadPoint reader;
    private boolean checked_end;

    boolean open(String file_name, int io_buffer_size, boolean peek_only, int decompress_selective)
    {
        if (file_name == null)
        {
            fprintf(stderr,"ERROR: fine name pointer is zero\n");
            return FALSE;
        }

        file = fopenRAF(file_name.toCharArray(), "rb");
        if (file == null)
        {
            fprintf(stderr, "ERROR: cannot open file '%s'\n", file_name);
            return FALSE;
        }

        // create input
        ByteStreamIn in = new ByteStreamInFile(file);

        return open(in, peek_only, decompress_selective);
    }

    boolean open(RandomAccessFile file, boolean peek_only, int decompress_selective)
    {
        if (file == null)
        {
            fprintf(stderr,"ERROR: file pointer is zero\n");
            return FALSE;
        }

        // create input
        ByteStreamIn in = new ByteStreamInFile(file);

        return open(in, decompress_selective);
    }


    public boolean open(InputStream in, int decompress_selective) {
        return open(in, false, decompress_selective);
    }

    boolean open(InputStream stream, boolean peek_only, int decompress_selective)
    {
        // create input
        ByteStreamIn in = new ByteStreamInStream(stream);

        return open(in, peek_only, decompress_selective);
    }

    boolean open(ByteStreamIn stream, int decompress_selective) {
        return open(stream, false, decompress_selective);
    }

    boolean open(ByteStreamIn stream, boolean peek_only, int decompress_selective)
    {
        int i,j;

        if (stream == null)
        {
            fprintf(stderr,"ERROR: ByteStreamIn is null\n");
            return FALSE;
        }

        this.stream = stream;

        // clean the header

        header.clean();

        // read the header variable after variable (to avoid alignment issues)

        try { stream.getBytes(header.file_signature, 4); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.file_signature\n");
            return FALSE;
        }
        try { header.file_source_ID = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.file_source_ID\n");
            return FALSE;
        }
        try { header.global_encoding = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.global_encoding\n");
            return FALSE;
        }
        try { header.project_ID_GUID_data_1 = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.project_ID_GUID_data_1\n");
            return FALSE;
        }
        try { header.project_ID_GUID_data_2 = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.project_ID_GUID_data_2\n");
            return FALSE;
        }
        try { header.project_ID_GUID_data_3 = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.project_ID_GUID_data_3\n");
            return FALSE;
        }
        try { stream.getBytes(header.project_ID_GUID_data_4, 8); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.project_ID_GUID_data_4\n");
            return FALSE;
        }
        try { header.version_major = stream.getByte(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.version_major\n");
            return FALSE;
        }
        try { header.version_minor = stream.getByte(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.version_minor\n");
            return FALSE;
        }
        try { stream.getBytes(header.system_identifier, 32); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.system_identifier\n");
            return FALSE;
        }
        try { stream.getBytes(header.generating_software, 32); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.generating_software\n");
            return FALSE;
        }
        try { header.file_creation_day = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.file_creation_day\n");
            return FALSE;
        }
        try { header.file_creation_year = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.file_creation_year\n");
            return FALSE;
        }
        try { header.header_size = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.header_size\n");
            return FALSE;
        }
        try { header.offset_to_point_data = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.offset_to_point_data\n");
            return FALSE;
        }
        try { header.number_of_variable_length_records = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.number_of_variable_length_records\n");
            return FALSE;
        }
        try { header.point_data_format = stream.getByte(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.point_data_format\n");
            return FALSE;
        }
        try { header.point_data_record_length = stream.get16bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.point_data_record_length\n");
            return FALSE;
        }
        try { header.number_of_point_records = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.number_of_point_records\n");
            return FALSE;
        }
        for (i = 0; i < 5; i++)
        {
            try { header.number_of_points_by_return[i] = stream.get32bitsLE(); } catch (Exception e)
            {
                fprintf(stderr,"ERROR: reading header.number_of_points_by_return %d\n", i);
                return FALSE;
            }
        }
        try { header.x_scale_factor = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.x_scale_factor\n");
            return FALSE;
        }
        try { header.y_scale_factor = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.y_scale_factor\n");
            return FALSE;
        }
        try { header.z_scale_factor = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.z_scale_factor\n");
            return FALSE;
        }
        try { header.x_offset = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.x_offset\n");
            return FALSE;
        }
        try { header.y_offset = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.y_offset\n");
            return FALSE;
        }
        try { header.z_offset = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.z_offset\n");
            return FALSE;
        }
        try { header.max_x = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.max_x\n");
            return FALSE;
        }
        try { header.min_x = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.min_x\n");
            return FALSE;
        }
        try { header.max_y = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.max_y\n");
            return FALSE;
        }
        try { header.min_y = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.min_y\n");
            return FALSE;
        }
        try { header.max_z = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.max_z\n");
            return FALSE;
        }
        try { header.min_z = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR: reading header.min_z\n");
            return FALSE;
        }

        // check core header contents
        if (!header.check())
        {
            return FALSE;
        }

        // special handling for LAS 1.3
        if ((header.version_major == 1) && (header.version_minor >= 3))
        {
            if (header.header_size < 235)
            {
                fprintf(stderr,"WARNING: for LAS 1.%d header_size should at least be 235 but it is only %d\n", header.version_minor, header.header_size);
                header.user_data_in_header_size = header.header_size - 227;
            }
            else
            {
                try { header.start_of_waveform_data_packet_record = stream.get64bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.start_of_waveform_data_packet_record\n");
                    return FALSE;
                }
                header.user_data_in_header_size = header.header_size - 235;
            }
        }
        else
        {
            header.user_data_in_header_size = header.header_size - 227;
        }

        // special handling for LAS 1.4
        if ((header.version_major == 1) && (header.version_minor >= 4))
        {
            if (header.header_size < 375)
            {
                fprintf(stderr,"ERROR: for LAS 1.%d header_size should at least be 375 but it is only %d\n", header.version_minor, header.header_size);
                return FALSE;
            }
            else
            {
                try { header.start_of_first_extended_variable_length_record = stream.get64bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.start_of_first_extended_variable_length_record\n");
                    return FALSE;
                }
                try { header.number_of_extended_variable_length_records = stream.get32bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.number_of_extended_variable_length_records\n");
                    return FALSE;
                }
                try { header.extended_number_of_point_records = stream.get64bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.extended_number_of_point_records\n");
                    return FALSE;
                }
                for (i = 0; i < 15; i++)
                {
                    try { header.extended_number_of_points_by_return[i] = stream.get64bitsLE(); } catch (Exception e)
                    {
                        fprintf(stderr,"ERROR: reading header.extended_number_of_points_by_return[%d]\n", i);
                        return FALSE;
                    }
                }
                header.user_data_in_header_size = header.header_size - 375;
            }
        }

        // load any number of user-defined bytes that might have been added to the header
        if (header.user_data_in_header_size != 0)
        {
            header.user_data_in_header = new byte[header.user_data_in_header_size];

            try { stream.getBytes(header.user_data_in_header, header.user_data_in_header_size); } catch (Exception e)
            {
                fprintf(stderr,"ERROR: reading %d bytes of data into header.user_data_in_header\n", header.user_data_in_header_size);
                return FALSE;
            }
        }

        npoints = (header.number_of_point_records != 0 ? header.number_of_point_records : header.extended_number_of_point_records);
        p_count = 0;

        if (peek_only)
        {
            // at least repair point type in incomplete header (no VLRs, no LASzip, no LAStiling) 
            header.point_data_format &= 127;
            return TRUE;
        }

        // read the variable length records into the header

        int vlrs_size = 0; // unsigned

        if (header.number_of_variable_length_records != 0)
        {
            header.vlrs = new LASvlr[header.number_of_variable_length_records];

            for (i = 0; i < header.number_of_variable_length_records; i++)
            {
                header.vlrs[i] = new LASvlr();
                // make sure there are enough bytes left to read a variable length record before the point block starts

                if (((int)header.offset_to_point_data - vlrs_size - header.header_size) < 54)
                {
                    fprintf(stderr,"WARNING: only %d bytes until point block after reading %d of %d vlrs. skipping remaining vlrs ...\n", (int)header.offset_to_point_data - vlrs_size - header.header_size, i, header.number_of_variable_length_records);
                    header.number_of_variable_length_records = i;
                    break;
                }

                // read variable length records variable after variable (to avoid alignment issues)

                try { header.vlrs[i].reserved = stream.get16bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].reserved\n", i);
                    return FALSE;
                }

                try { stream.getBytes(header.vlrs[i].user_id, 16); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].user_id\n", i);
                    return FALSE;
                }
                try { header.vlrs[i].record_id = stream.get16bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].record_id\n", i);
                    return FALSE;
                }
                try { header.vlrs[i].record_length_after_header = stream.get16bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].record_length_after_header\n", i);
                    return FALSE;
                }
                try { stream.getBytes(header.vlrs[i].description, 32); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].description\n", i);
                    return FALSE;
                }

                // keep track on the number of bytes we have read so far

                vlrs_size += 54;

                // check variable length record contents

/*
      if (header.vlrs[i].reserved != 0xAABB)
      {
        fprintf(stderr,"WARNING: wrong header.vlrs[%d].reserved: %d != 0xAABB\n", i, header.vlrs[i].reserved);
      }
*/

                // make sure there are enough bytes left to read the data of the variable length record before the point block starts

                if (((int)header.offset_to_point_data - vlrs_size - header.header_size) < header.vlrs[i].record_length_after_header)
                {
                    fprintf(stderr,"WARNING: only %d bytes until point block when trying to read %d bytes into header.vlrs[%d].data\n", (int)header.offset_to_point_data - vlrs_size - header.header_size, header.vlrs[i].record_length_after_header, i);
                    header.vlrs[i].record_length_after_header = (char) (header.offset_to_point_data - vlrs_size - header.header_size);
                }

                // load data following the header of the variable length record

                if (header.vlrs[i].record_length_after_header != 0)
                {
                    if (strcmp(header.vlrs[i].user_id, "laszip encoded") == 0)
                    {
                        header.laszip = new LASzip();

                        // read this data following the header of the variable length record
                        //     U16  compressor                2 bytes 
                        //     U32  coder                     2 bytes 
                        //     U8   version_major             1 byte 
                        //     U8   version_minor             1 byte
                        //     U16  version_revision          2 bytes
                        //     U32  options                   4 bytes 
                        //     int  chunk_size                4 bytes
                        //     long  number_of_special_evlrs   8 bytes
                        //     long  offset_to_special_evlrs   8 bytes
                        //     U16  num_items                 2 bytes
                        //        U16 type                2 bytes * num_items
                        //        U16 size                2 bytes * num_items
                        //        U16 version             2 bytes * num_items
                        // which totals 34+6*num_items

                        try { header.laszip.compressor = stream.get16bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading compressor %d\n", header.laszip.compressor);
                            return FALSE;
                        }
                        try { header.laszip.coder = stream.get16bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading coder %d\n", header.laszip.coder);
                            return FALSE;
                        }
                        try { header.laszip.version_major = stream.getByte(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading version_major %d\n", header.laszip.version_major);
                            return FALSE;
                        }
                        try { header.laszip.version_minor = stream.getByte(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading version_minor %d\n", header.laszip.version_minor);
                            return FALSE;
                        }
                        try { header.laszip.version_revision = stream.get16bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading version_revision %d\n", header.laszip.version_revision);
                            return FALSE;
                        }
                        try { header.laszip.options = stream.get32bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading options %d\n", (int)header.laszip.options);
                            return FALSE;
                        }
                        try { header.laszip.chunk_size = stream.get32bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading chunk_size %d\n", header.laszip.chunk_size);
                            return FALSE;
                        }
                        try { header.laszip.number_of_special_evlrs = stream.get64bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading number_of_special_evlrs %d\n", header.laszip.number_of_special_evlrs);
                            return FALSE;
                        }
                        try { header.laszip.offset_to_special_evlrs = stream.get64bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading offset_to_special_evlrs %d\n", header.laszip.offset_to_special_evlrs);
                            return FALSE;
                        }
                        try { header.laszip.num_items = stream.get16bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading num_items %d\n", header.laszip.num_items);
                            return FALSE;
                        }
                        header.laszip.items = new LASitem[header.laszip.num_items];
                        for (j = 0; j < header.laszip.num_items; j++)
                        {
                            header.laszip.items[j] = new LASitem();
                            char type = 0, size = 0, version = 0;
                            try { type = stream.get16bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading type %d of item %d\n", type, j);
                                return FALSE;
                            }
                            try { size = stream.get16bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading size %d of item %d\n", size, j);
                                return FALSE;
                            }
                            try { version = stream.get16bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading version %d of item %d\n", version, j);
                                return FALSE;
                            }
                            header.laszip.items[j].type = LASitem.Type.fromOrdinal(type);
                            header.laszip.items[j].size = size;
                            header.laszip.items[j].version = version;
                        }
                    }
                    else if (((strcmp(header.vlrs[i].user_id, "LAStools") == 0) && (header.vlrs[i].record_id == 10)) || (strcmp(header.vlrs[i].user_id, "lastools tile") == 0))
                    {
                        header.clean_lastiling();
                        header.vlr_lastiling = new LASvlr_lastiling();

                        // read the payload of this VLR which contains 28 bytes
                        //   U32  level                                          4 bytes 
                        //   U32  level_index                                    4 bytes 
                        //   U32  implicit_levels + buffer bit + reversible bit  4 bytes 
                        //   F32  min_x                                          4 bytes 
                        //   F32  max_x                                          4 bytes 
                        //   F32  min_y                                          4 bytes 
                        //   F32  max_y                                          4 bytes 

                        if (header.vlrs[i].record_length_after_header == 28)
                        {
                            try { header.vlr_lastiling.level = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.level %d\n", header.vlr_lastiling.level);
                                return FALSE;
                            }
                            try { header.vlr_lastiling.level_index = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.level_index %d\n", header.vlr_lastiling.level_index);
                                return FALSE;
                            }
                            try { header.vlr_lastiling.implicit_levels = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.implicit_levels %d\n", header.vlr_lastiling.implicit_levels);
                                return FALSE;
                            }
                            try { header.vlr_lastiling.min_x = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.min_x %g\n", header.vlr_lastiling.min_x);
                                return FALSE;
                            }
                            try { header.vlr_lastiling.max_x = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.max_x %g\n", header.vlr_lastiling.max_x);
                                return FALSE;
                            }
                            try { header.vlr_lastiling.min_y = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.min_y %g\n", header.vlr_lastiling.min_y);
                                return FALSE;
                            }
                            try { header.vlr_lastiling.max_y = stream.get32bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lastiling.max_y %g\n", header.vlr_lastiling.max_y);
                                return FALSE;
                            }
                        }
                        else
                        {
                            fprintf(stderr,"ERROR: record_length_after_header of VLR %s (%d) is %d instead of 28\n", stringFromByteArray(header.vlrs[i].user_id), header.vlrs[i].record_id, header.vlrs[i].record_length_after_header);
                            return FALSE;
                        }
                    }
                    else if (((strcmp(header.vlrs[i].user_id, "LAStools") == 0) && (header.vlrs[i].record_id == 20)))
                    {
                        header.clean_lasoriginal();
                        header.vlr_lasoriginal = new LASvlr_lasoriginal();

                        // read the payload of this VLR which contains 176 bytes

                        if (header.vlrs[i].record_length_after_header == 176)
                        {
                            try { header.vlr_lasoriginal.number_of_point_records = stream.get64bitsLE(); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.number_of_point_records %d\n", header.vlr_lasoriginal.number_of_point_records);
                                return FALSE;
                            }
                            for (j = 0; j < 15; j++)
                            {
                                try { header.vlr_lasoriginal.number_of_points_by_return[j] = stream.get64bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading vlr_lasoriginal.number_of_points_by_return[%d] %d\n", j, header.vlr_lasoriginal.number_of_points_by_return[j]);
                                    return FALSE;
                                }
                            }
                            try { header.vlr_lasoriginal.min_x = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.min_x %g\n", header.vlr_lasoriginal.min_x);
                                return FALSE;
                            }
                            try { header.vlr_lasoriginal.max_x = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.max_x %g\n", header.vlr_lasoriginal.max_x);
                                return FALSE;
                            }
                            try { header.vlr_lasoriginal.min_y = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.min_y %g\n", header.vlr_lasoriginal.min_y);
                                return FALSE;
                            }
                            try { header.vlr_lasoriginal.max_y = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.max_y %g\n", header.vlr_lasoriginal.max_y);
                                return FALSE;
                            }
                            try { header.vlr_lasoriginal.min_z = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.min_z %g\n", header.vlr_lasoriginal.min_z);
                                return FALSE;
                            }
                            try { header.vlr_lasoriginal.max_z = longBitsToDouble(stream.get64bitsLE()); } catch (Exception e)
                            {
                                fprintf(stderr,"ERROR: reading vlr_lasoriginal.max_z %g\n", header.vlr_lasoriginal.max_z);
                                return FALSE;
                            }
                        }
                        else
                        {
                            fprintf(stderr,"ERROR: record_length_after_header of VLR %s (%d) is %d instead of 176\n", stringFromByteArray(header.vlrs[i].user_id), header.vlrs[i].record_id, header.vlrs[i].record_length_after_header);
                            return FALSE;
                        }
                    }
                    else
                    {
                        header.vlrs[i].data = new byte[header.vlrs[i].record_length_after_header];

                        try { stream.getBytes(header.vlrs[i].data, header.vlrs[i].record_length_after_header); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading %d bytes of data into header.vlrs[%d].data\n", header.vlrs[i].record_length_after_header, i);
                            return FALSE;
                        }
                    }
                }
                else
                {
                    header.vlrs[i].data = null;
                }

                // keep track on the number of bytes we have read so far

                vlrs_size += header.vlrs[i].record_length_after_header;

                // special handling for known variable header tags

                if (strcmp(header.vlrs[i].user_id, "LASF_Projection") == 0)
                {
                    if (header.vlrs[i].data != null)
                    {
                        if (header.vlrs[i].record_id == 34735) // GeoKeyDirectoryTag
                        {
                            if (header.vlr_geo_keys != null)
                            {
                                fprintf(stderr,"WARNING: variable length records contain more than one GeoKeyDirectoryTag\n");
                            }
                            header.vlr_geo_keys = LASvlr_geo_keys.fromByteArray(header.vlrs[i].data);

                            // check variable header geo keys contents

                            if (header.vlr_geo_keys.key_directory_version != 1)
                            {
                                fprintf(stderr,"WARNING: wrong vlr_geo_keys.key_directory_version: %d != 1\n",header.vlr_geo_keys.key_directory_version);
                            }
                            if (header.vlr_geo_keys.key_revision != 1)
                            {
                                fprintf(stderr,"WARNING: wrong vlr_geo_keys.key_revision: %d != 1\n",header.vlr_geo_keys.key_revision);
                            }
                            if (header.vlr_geo_keys.minor_revision != 0)
                            {
                                fprintf(stderr,"WARNING: wrong vlr_geo_keys.minor_revision: %d != 0\n",header.vlr_geo_keys.minor_revision);
                            }
                            ByteBuffer buffer = ByteBuffer.wrap(header.vlrs[i].data).order(ByteOrder.LITTLE_ENDIAN);
                            buffer.position(8);
                            header.vlr_geo_key_entries = LASvlr_key_entry.fromByteBuffer(buffer);
                        }
                        else if (header.vlrs[i].record_id == 34736) // GeoDoubleParamsTag
                        {
                            if (header.vlr_geo_double_params != null)
                            {
                                fprintf(stderr,"WARNING: variable length records contain more than one GeoF64ParamsTag\n");
                            }
                            header.vlr_geo_double_params = LASheader.doublesFromByteArray(header.vlrs[i].data);
                        }
                        else if (header.vlrs[i].record_id == 34737) // GeoAsciiParamsTag
                        {
                            if (header.vlr_geo_ascii_params != null)
                            {
                                fprintf(stderr,"WARNING: variable length records contain more than one GeoAsciiParamsTag\n");
                            }
                            header.vlr_geo_ascii_params = MyDefs.stringFromByteArray(header.vlrs[i].data);
                        }
                        else if ((header.vlrs[i].record_id != 2111) && (header.vlrs[i].record_id != 2112)) // WKT OGC MATH TRANSFORM or WKT OGC COORDINATE SYSTEM
                        {
                            fprintf(stderr,"WARNING: unknown LASF_Projection VLR with record_id %d.\n", header.vlrs[i].record_id);
                        }
                    }
                    else if (header.vlrs[i].record_id != 2112) // GeoAsciiParamsTag
                    {
                        fprintf(stderr,"WARNING: no payload for LASF_Projection VLR with record_id %d.\n", header.vlrs[i].record_id);
                    }
                }
                else if (strcmp(header.vlrs[i].user_id, "LASF_Spec") == 0)
                {
                    if (header.vlrs[i].data != null)
                    {
                        if (header.vlrs[i].record_id == 0) // ClassificationLookup
                        {
                            if (header.vlr_classification != null)
                            {
                                fprintf(stderr,"WARNING: variable length records contain more than one ClassificationLookup\n");
                            }
                            header.vlr_classification = LASvlr_classification.fromByteArray(header.vlrs[i].data);
                        }
                        else if (header.vlrs[i].record_id == 2) // Histogram
                        {
                        }
                        else if (header.vlrs[i].record_id == 3) // TextAreaDescription
                        {
                        }
                        else if (header.vlrs[i].record_id == 4) // ExtraBytes
                        {
                            header.init_attributes(header.vlrs[i].record_length_after_header/sizeof(LASattribute.class), LASattribute.fromByteArray(header.vlrs[i].data));
                        }
                        else if ((header.vlrs[i].record_id >= 100) && (header.vlrs[i].record_id < 355)) // WavePacketDescriptor
                        {
                            int idx = header.vlrs[i].record_id - 99;

                            if (header.vlr_wave_packet_descr == null)
                            {
                                header.vlr_wave_packet_descr = new LASvlr_wave_packet_descr[256];
                                for (j = 0; j < 256; j++) header.vlr_wave_packet_descr[j] = null;
                            }
                            if (header.vlr_wave_packet_descr[idx] != null)
                            {
                                fprintf(stderr,"WARNING: variable length records defines wave packet descr %d more than once\n", idx);
                            }
                            if (header.vlrs[i].record_length_after_header != 26)
                            {
                                fprintf(stderr,"WARNING: variable length record payload for wave packet descr %d is %d instead of 26 bytes\n", idx, (int)header.vlrs[i].record_length_after_header);
                            }
                            header.vlr_wave_packet_descr[idx] = LASvlr_wave_packet_descr.fromByteArray(header.vlrs[i].data);
                            if ((header.vlr_wave_packet_descr[idx].getBitsPerSample() != 8) && (header.vlr_wave_packet_descr[idx].getBitsPerSample() != 16))
                            {
                                fprintf(stderr,"WARNING: bits per sample for wave packet descr %d is %d instead of 8 or 16\n", idx, (int)header.vlr_wave_packet_descr[idx].getBitsPerSample());
                            }
                            if (header.vlr_wave_packet_descr[idx].getNumberOfSamples() == 0)
                            {
                                fprintf(stderr,"WARNING: number of samples for wave packet descr %d is zero\n", idx);
                            }
                            if (header.vlr_wave_packet_descr[idx].getNumberOfSamples() > 8096)
                            {
                                fprintf(stderr,"WARNING: number of samples of %d for wave packet descr %d is with unusually large\n", header.vlr_wave_packet_descr[idx].getNumberOfSamples(), idx);
                            }
                            if (header.vlr_wave_packet_descr[idx].getTemporalSpacing() == 0)
                            {
                                fprintf(stderr,"WARNING: temporal spacing for wave packet descr %d is zero\n", idx);
                            }
/*
            // fix for RiPROCESS export error
            if (idx == 1)
              header.vlr_wave_packet_descr[idx].setNumberOfSamples(80);
            else
              header.vlr_wave_packet_descr[idx].setNumberOfSamples(160);

            // fix for Optech LMS export error
            header.vlr_wave_packet_descr[idx].setNumberOfSamples(header.vlr_wave_packet_descr[idx].getNumberOfSamples()/2);
*/
                        }
                    }
                    else
                    {
                        fprintf(stderr,"WARNING: no payload for LASF_Spec (not specification-conform).\n");
                    }
                }
                else if ((strcmp(header.vlrs[i].user_id, "laszip encoded") == 0) || ((strcmp(header.vlrs[i].user_id, "LAStools") == 0) && (header.vlrs[i].record_id < 2000)) || (strcmp(header.vlrs[i].user_id, "lastools tile") == 0))
                {
                    // we take our own VLRs with record IDs below 2000 away from everywhere
                    header.offset_to_point_data -= (54+header.vlrs[i].record_length_after_header);
                    vlrs_size -= (54+header.vlrs[i].record_length_after_header);
                    i--;
                    header.number_of_variable_length_records--;
                }
            }
        }

        // load any number of user-defined bytes that might have been added after the header

        header.user_data_after_header_size = (int)header.offset_to_point_data - vlrs_size - header.header_size;
        if (header.user_data_after_header_size != 0)
        {
            header.user_data_after_header = new byte[header.user_data_after_header_size];

            try { stream.getBytes(header.user_data_after_header, header.user_data_after_header_size); } catch (Exception e)
            {
                fprintf(stderr,"ERROR: reading %d bytes of data into header.user_data_after_header\n", header.user_data_after_header_size);
                return FALSE;
            }
        }

        // special handling for LAS 1.4
        if ((header.version_major == 1) && (header.version_minor >= 4))
        {
            if (header.number_of_extended_variable_length_records != 0)
            {
                if (!stream.isSeekable())
                {
                    fprintf(stderr,"WARNING: LAS %d.%d file has %d EVLRs but stream is not seekable ...\n", header.version_major, header.version_minor, header.number_of_extended_variable_length_records);
                }
                else
                {
                    long here = stream.tell();
                    stream.seek(header.start_of_first_extended_variable_length_record);

                    header.evlrs = new LASevlr[header.number_of_extended_variable_length_records];

                    // read the extended variable length records into the header

                    long evlrs_size = 0;

                    for (i = 0; i < header.number_of_extended_variable_length_records; i++)
                    {
                        header.evlrs[i] = new LASevlr();
                        // read variable length records variable after variable (to avoid alignment issues)

                        try { header.evlrs[i].reserved = stream.get16bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading header.evlrs[%d].reserved\n", i);
                            return FALSE;
                        }
                        try { stream.getBytes(header.evlrs[i].user_id, 16); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading header.evlrs[%d].user_id\n", i);
                            return FALSE;
                        }
                        try { header.evlrs[i].record_id = stream.get16bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading header.evlrs[%d].record_id\n", i);
                            return FALSE;
                        }
                        try { header.evlrs[i].record_length_after_header = stream.get64bitsLE(); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading header.evlrs[%d].record_length_after_header\n", i);
                            return FALSE;
                        }
                        try { stream.getBytes(header.evlrs[i].description, 32); } catch (Exception e)
                        {
                            fprintf(stderr,"ERROR: reading header.evlrs[%d].description\n", i);
                            return FALSE;
                        }

                        // keep track on the number of bytes we have read so far

                        evlrs_size += 60;

                        // check variable length record contents

/*
          if (header.evlrs[i].reserved != 0)
          {
            fprintf(stderr,"WARNING: wrong header.evlrs[%d].reserved: %d != 0\n", i, header.evlrs[i].reserved);
          }
*/

                        // load data following the header of the variable length record

                        if (header.evlrs[i].record_length_after_header != 0)
                        {
                            if (strcmp(header.evlrs[i].user_id, "laszip encoded") == 0)
                            {
                                header.laszip = new LASzip();

                                // read this data following the header of the variable length record
                                //     U16  compressor                2 bytes 
                                //     U32  coder                     2 bytes 
                                //     U8   version_major             1 byte 
                                //     U8   version_minor             1 byte
                                //     U16  version_revision          2 bytes
                                //     U32  options                   4 bytes 
                                //     int  chunk_size                4 bytes
                                //     long  number_of_special_evlrs   8 bytes
                                //     long  offset_to_special_evlrs   8 bytes
                                //     U16  num_items                 2 bytes
                                //        U16 type                2 bytes * num_items
                                //        U16 size                2 bytes * num_items
                                //        U16 version             2 bytes * num_items
                                // which totals 34+6*num_items

                                try { header.laszip.compressor = stream.get16bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading compressor %d\n", (int)header.laszip.compressor);
                                    return FALSE;
                                }
                                try { header.laszip.coder = stream.get16bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading coder %d\n", (int)header.laszip.coder);
                                    return FALSE;
                                }
                                try { header.laszip.version_major = stream.getByte(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading version_major %d\n", header.laszip.version_major);
                                    return FALSE;
                                }
                                try { header.laszip.version_minor = stream.getByte(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading version_minor %d\n", header.laszip.version_minor);
                                    return FALSE;
                                }
                                try { header.laszip.version_revision = stream.get16bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading version_revision %d\n", header.laszip.version_revision);
                                    return FALSE;
                                }
                                try { header.laszip.options = stream.get32bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading options %d\n", (int)header.laszip.options);
                                    return FALSE;
                                }
                                try { header.laszip.chunk_size = stream.get32bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading chunk_size %d\n", header.laszip.chunk_size);
                                    return FALSE;
                                }
                                try { header.laszip.number_of_special_evlrs = stream.get64bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading number_of_special_evlrs %d\n", (int)header.laszip.number_of_special_evlrs);
                                    return FALSE;
                                }
                                try { header.laszip.offset_to_special_evlrs = stream.get64bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading offset_to_special_evlrs %d\n", (int)header.laszip.offset_to_special_evlrs);
                                    return FALSE;
                                }
                                try { header.laszip.num_items = stream.get16bitsLE(); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading num_items %d\n", header.laszip.num_items);
                                    return FALSE;
                                }
                                header.laszip.items = new LASitem[header.laszip.num_items];
                                for (j = 0; j < header.laszip.num_items; j++)
                                {
                                    char type = 0, size = 0, version = 0;
                                    try { type = stream.get16bitsLE(); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading type %d of item %d\n", type, j);
                                        return FALSE;
                                    }
                                    try { size = stream.get16bitsLE(); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading size %d of item %d\n", size, j);
                                        return FALSE;
                                    }
                                    try { version = stream.get16bitsLE(); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading version %d of item %d\n", version, j);
                                        return FALSE;
                                    }
                                    header.laszip.items[j].type = LASitem.Type.fromOrdinal(type);
                                    header.laszip.items[j].size = size;
                                    header.laszip.items[j].version = version;
                                }
                            }
                            else if ((strcmp(header.evlrs[i].user_id, "LAStools") == 0) && (header.evlrs[i].record_id == 10))
                            {
                                header.clean_lastiling();
                                header.vlr_lastiling = new LASvlr_lastiling();

                                // read the payload of this VLR which contains 28 bytes
                                //   U32  level                                          4 bytes 
                                //   U32  level_index                                    4 bytes 
                                //   U32  implicit_levels + buffer bit + reversible bit  4 bytes 
                                //   F32  min_x                                          4 bytes 
                                //   F32  max_x                                          4 bytes 
                                //   F32  min_y                                          4 bytes 
                                //   F32  max_y                                          4 bytes 

                                if (header.evlrs[i].record_length_after_header == 28)
                                {
                                    try { header.vlr_lastiling.level = stream.get32bitsLE(); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.level %d\n", header.vlr_lastiling.level);
                                        return FALSE;
                                    }
                                    try { header.vlr_lastiling.level_index = stream.get32bitsLE(); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.level_index %d\n", header.vlr_lastiling.level_index);
                                        return FALSE;
                                    }
                                    try { header.vlr_lastiling.implicit_levels = stream.get32bitsLE(); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.implicit_levels %d\n", header.vlr_lastiling.implicit_levels);
                                        return FALSE;
                                    }
                                    try { header.vlr_lastiling.min_x = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.min_x %g\n", header.vlr_lastiling.min_x);
                                        return FALSE;
                                    }
                                    try { header.vlr_lastiling.max_x = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.max_x %g\n", header.vlr_lastiling.max_x);
                                        return FALSE;
                                    }
                                    try { header.vlr_lastiling.min_y = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.min_y %g\n", header.vlr_lastiling.min_y);
                                        return FALSE;
                                    }
                                    try { header.vlr_lastiling.max_y = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
                                    {
                                        fprintf(stderr,"ERROR: reading vlr_lastiling.max_y %g\n", header.vlr_lastiling.max_y);
                                        return FALSE;
                                    }
                                }
                                else
                                {
                                    fprintf(stderr,"ERROR: record_length_after_header of EVLR %s (%d) is %d instead of 28\n", stringFromByteArray(header.evlrs[i].user_id), header.evlrs[i].record_id, (int)header.evlrs[i].record_length_after_header);
                                    return FALSE;
                                }
                            }
                            else
                            {
                                header.evlrs[i].data = new byte[(int)header.evlrs[i].record_length_after_header];

                                try { stream.getBytes(header.evlrs[i].data, (int)header.evlrs[i].record_length_after_header); } catch (Exception e)
                                {
                                    fprintf(stderr,"ERROR: reading %d bytes of data into header.evlrs[%d].data\n", (int)header.evlrs[i].record_length_after_header, i);
                                    return FALSE;
                                }
                            }
                        }
                        else
                        {
                            header.evlrs[i].data = null;
                        }

                        // keep track on the number of bytes we have read so far

                        evlrs_size += header.evlrs[i].record_length_after_header;

                        // special handling for known variable header tags

                        if (strcmp(header.evlrs[i].user_id, "LASF_Projection") == 0)
                        {
                            if (header.evlrs[i].record_id == 34735) // GeoKeyDirectoryTag
                            {
                                if (header.vlr_geo_keys != null)
                                {
                                    fprintf(stderr,"WARNING: variable length records contain more than one GeoKeyDirectoryTag\n");
                                }
                                header.vlr_geo_keys = LASvlr_geo_keys.fromByteArray(header.evlrs[i].data);

                                // check variable header geo keys contents

                                if (header.vlr_geo_keys.key_directory_version != 1)
                                {
                                    fprintf(stderr,"WARNING: wrong vlr_geo_keys.key_directory_version: %d != 1\n",header.vlr_geo_keys.key_directory_version);
                                }
                                if (header.vlr_geo_keys.key_revision != 1)
                                {
                                    fprintf(stderr,"WARNING: wrong vlr_geo_keys.key_revision: %d != 1\n",header.vlr_geo_keys.key_revision);
                                }
                                if (header.vlr_geo_keys.minor_revision != 0)
                                {
                                    fprintf(stderr,"WARNING: wrong vlr_geo_keys.minor_revision: %d != 0\n",header.vlr_geo_keys.minor_revision);
                                }
                                ByteBuffer bb = ByteBuffer.wrap(header.evlrs[i].data).order(ByteOrder.LITTLE_ENDIAN);
                                bb.position(8);
                                header.vlr_geo_key_entries = LASvlr_key_entry.fromByteBuffer(bb);
                            }
                            else if (header.evlrs[i].record_id == 34736) // GeoDoubleParamsTag
                            {
                                if (header.vlr_geo_double_params != null)
                                {
                                    fprintf(stderr,"WARNING: variable length records contain more than one GeoF64ParamsTag\n");
                                }
                                header.vlr_geo_double_params = LASheader.doublesFromByteArray(header.evlrs[i].data);
                            }
                            else if (header.evlrs[i].record_id == 34737) // GeoAsciiParamsTag
                            {
                                if (header.vlr_geo_ascii_params != null)
                                {
                                    fprintf(stderr,"WARNING: variable length records contain more than one GeoAsciiParamsTag\n");
                                }
                                header.vlr_geo_ascii_params = MyDefs.stringFromByteArray(header.evlrs[i].data);
                            }
                        }
                        else if (strcmp(header.evlrs[i].user_id, "LASF_Spec") == 0)
                        {
                            if (header.evlrs[i].record_id == 0) // ClassificationLookup
                            {
                                if (header.vlr_classification != null)
                                {
                                    fprintf(stderr,"WARNING: variable length records contain more than one ClassificationLookup\n");
                                }
                                header.vlr_classification = LASvlr_classification.fromByteArray(header.evlrs[i].data);
                            }
                            else if (header.evlrs[i].record_id == 2) // Histogram
                            {
                            }
                            else if (header.evlrs[i].record_id == 3) // TextAreaDescription
                            {
                            }
                            else if (header.evlrs[i].record_id == 4) // ExtraBytes
                            {
                                header.init_attributes((int)header.evlrs[i].record_length_after_header/sizeof(LASattribute.class), LASattribute.fromByteArray(header.evlrs[i].data));
                            }
                            else if ((header.evlrs[i].record_id >= 100) && (header.evlrs[i].record_id < 355)) // WavePacketDescriptor
                            {
                                int idx = header.evlrs[i].record_id - 99;

                                if (header.vlr_wave_packet_descr == null)
                                {
                                    header.vlr_wave_packet_descr = new LASvlr_wave_packet_descr[256];
                                    for (j = 0; j < 256; j++) header.vlr_wave_packet_descr[j] = null;
                                }
                                if (header.vlr_wave_packet_descr[idx] != null)
                                {
                                    fprintf(stderr,"WARNING: extended variable length records defines wave packet descr %d more than once\n", idx);
                                }
                                header.vlr_wave_packet_descr[idx] = LASvlr_wave_packet_descr.fromByteArray(header.evlrs[i].data);
                            }
                        }
                        else if (strcmp(header.evlrs[i].user_id, "laszip encoded") == 0 || strcmp(header.evlrs[i].user_id, "LAStools") == 0)
                        {
                            // we take our own EVLRs away from everywhere
                            evlrs_size -= (60+header.evlrs[i].record_length_after_header);
                            i--;
                            header.number_of_extended_variable_length_records--;
                        }
                    }
                    stream.seek(here);
                }
            }
        }

        // check the compressor state

        if (header.laszip != null)
        {
            if (!header.laszip.check())
            {
                fprintf(stderr,"ERROR: %s\n", header.laszip.get_error());
                fprintf(stderr,"       please upgrade to the latest release of LAStools (with LASzip)\n");
                fprintf(stderr,"       or contact 'martin.isenburg@rapidlasso.com' for assistance.\n");
                return FALSE;
            }
        }

        // remove extra bits in point data type

        if ((header.point_data_format & 128) != 0 || (header.point_data_format & 64) != 0)
        {
            if (header.laszip == null)
            {
                fprintf(stderr,"ERROR: this file was compressed with an experimental version of laszip\n");
                fprintf(stderr,"ERROR: please contact 'martin.isenburg@rapidlasso.com' for assistance.\n");
                return FALSE;
            }
            header.point_data_format &= 127;
        }

        // create the point reader

        reader = new LASreadPoint(decompress_selective);

        // initialize point and the reader

        if (header.laszip != null)
        {
            if (!point.init(header, header.laszip.num_items, header.laszip.items, header)) return FALSE;
            if (!reader.setup(header.laszip.num_items, header.laszip.items, header.laszip)) return FALSE;
        }
        else
        {
            if (!point.init(header, header.point_data_format, header.point_data_record_length, header)) return FALSE;
            if (!reader.setup(point.num_items, point.items)) return FALSE;
        }

        // maybe has internal EVLRs

        if (header.laszip != null && (header.laszip.number_of_special_evlrs > 0) && (header.laszip.offset_to_special_evlrs >= header.offset_to_point_data) && stream.isSeekable())
        {
            long here = stream.tell();
            try
            {
                long number = header.laszip.number_of_special_evlrs;
                long offset = header.laszip.offset_to_special_evlrs;
                long count;
                for (count = 0; count < number; count++)
                {
                    stream.seek(offset + 2);
                    byte[] user_id = new byte[16];
                    stream.getBytes(user_id, 16);
                    char record_id = stream.get16bitsLE();
                    if ((strcmp(user_id, "LAStools") == 0) && (record_id == 30))
                    {
                        stream.seek(offset + 60);
                        index = new LASindex();
                        if (index != null)
                        {
                            if (!index.read(stream))
                            {
                                index = null;
                            }
                        }
                        break;
                    }
                    else
                    {
                        long record_length_after_header = stream.get64bitsLE();
                        offset += (record_length_after_header + 60);
                    }
                }
            }
            catch (Exception e)
            {
                fprintf(stderr,"ERROR: trying to read %d internal EVLRs. ignoring ...\n", (int)header.laszip.number_of_special_evlrs);
            }
            stream.seek(here);
        }

        if (!reader.init(stream)) return FALSE;

        checked_end = FALSE;

        return TRUE;
    }

    public int get_format()
    {
        if (header.laszip != null)
        {
            return (header.laszip.compressor == LASZIP_COMPRESSOR_NONE ? LAS_TOOLS_FORMAT_LAS : LAS_TOOLS_FORMAT_LAZ);
        }
        return LAS_TOOLS_FORMAT_LAS;
    }

    public boolean seek(long p_index)
    {
        if (reader != null)
        {
            if (p_index < npoints)
            {
                if (reader.seek((int)p_count, (int)p_index))
                {
                    p_count = p_index;
                    return TRUE;
                }
            }
        }
        return FALSE;
    }

    protected boolean read_point_default()
    {
        if (p_count < npoints)
        {
            if (reader.read(point.PointRecords) == FALSE)
            {
                if (reader.error() != null)
                {
                    fprintf(stderr,"ERROR: '%s' after %d of %d points\n", reader.error(), (int)p_count, (int)npoints);
                }
                else
                {
                    fprintf(stderr,"WARNING: end-of-file after %d of %d points\n", (int)p_count, (int)npoints);
                }
                return FALSE;
            }

/*
    // fix for OPTECH LMS export error
    if (point.have_wavepacket)
    {
      // distance in meters light travels in one nanoseconds divided by two divided by 1000
      F64 round_trip_distance_in_picoseconds = 0.299792458 / 2 / 1000; 
      F64 x = -point.wavepacket.getXt();
      F64 y = -point.wavepacket.getYt();
      F64 z = -point.wavepacket.getZt();
      F64 len = sqrt(x*x+y*y+z*z);
      x = x / len * round_trip_distance_in_picoseconds;
      y = y / len * round_trip_distance_in_picoseconds;
      z = z / len * round_trip_distance_in_picoseconds;
      point.wavepacket.setXt((F32)x);
      point.wavepacket.setYt((F32)y);
      point.wavepacket.setZt((F32)z);
//      alternative to converge on optical origin 
//      point.wavepacket.setXt(-point.wavepacket.getXt()/point.wavepacket.getLocation());
//      point.wavepacket.setYt(-point.wavepacket.getYt()/point.wavepacket.getLocation());
//      point.wavepacket.setZt(-point.wavepacket.getZt()/point.wavepacket.getLocation());
    }
*/
            p_count++;
            return TRUE;
        }
        else
        {
            if (!checked_end)
            {
                if (reader.check_end() == FALSE)
                {
                    fprintf(stderr,"ERROR: '%s' when reaching end of encoding\n", reader.error());
                    p_count--;
                }
                if (reader.warning() != null)
                {
                    fprintf(stderr,"WARNING: '%s'\n", reader.warning());
                }
                checked_end = TRUE;
            }
        }
        return FALSE;
    }

    public ByteStreamIn get_stream()
    {
        return stream;
    }

    public void close(boolean close_stream)
    {
        if (reader != null)
        {
            reader.done();
            reader = null;
        }
        if (close_stream)
        {
            if (stream != null)
            {
                fclose(stream);
                stream = null;
            }
            if (file != null)
            {
                fclose(file);
                file = null;
            }
        }
    }

    public LASreaderLAS()
    {
        file = null;
        stream = null;
        reader = null;
    }
}
