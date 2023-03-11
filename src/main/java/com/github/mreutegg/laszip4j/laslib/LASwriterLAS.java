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

import com.github.mreutegg.laszip4j.laszip.ByteStreamOut;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOutArray;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOutFile;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOutOstream;
import com.github.mreutegg.laszip4j.laszip.LASpoint;
import com.github.mreutegg.laszip4j.laszip.LASwritePoint;
import com.github.mreutegg.laszip4j.laszip.LASzip;

import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_VERSION;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_LAYERED_CHUNKED;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_NONE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_MAX;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Double.doubleToLongBits;
import static java.lang.Float.floatToIntBits;
import static java.lang.Integer.toUnsignedLong;

public class LASwriterLAS extends LASwriter {

    private static final PrintStream stderr = System.err;

    private RandomAccessFile file;
    private ByteStreamOut stream;
    private boolean delete_stream;
    private LASwritePoint writer;
    private long header_start_position;
    private boolean writing_las_1_4;
    private boolean writing_new_point_type;
    // for delayed write of EVLRs
    private long start_of_first_extended_variable_length_record;
    private int number_of_extended_variable_length_records;     // unsigned
    private LASevlr[] evlrs;

    public LASwriterLAS() {
        this.file = null;
        this.stream = null;
        delete_stream = TRUE;
        this.writer = null;
        this.writing_las_1_4 = FALSE;
        this.writing_new_point_type = FALSE;
        // for delayed write of EVLRs
        this.start_of_first_extended_variable_length_record = 0;
        this.number_of_extended_variable_length_records = 0;
        this.evlrs = null;
    }

    @Override
    public boolean write_point(LASpoint point) {
        p_count++;
        return writer.write(point.PointRecords);
    }

    @Override
    public boolean chunk() {
        return writer.chunk();
    }

    @Override
    public boolean update_header(LASheader header, boolean use_inventory, boolean update_extra_bytes) {
        int i;
        if (header == null)
        {
            fprintf(stderr,"ERROR: header pointer is zero\n");
            return FALSE;
        }
        if (stream == null)
        {
            fprintf(stderr,"ERROR: stream pointer is zero\n");
            return FALSE;
        }
        if (!stream.isSeekable())
        {
            fprintf(stderr,"WARNING: stream not seekable. cannot update header.\n");
            return FALSE;
        }
        if (use_inventory)
        {
            int number; // unsigned
            stream.seek(header_start_position+107);
            if (header.point_data_format >= 6)
            {
                number = 0; // legacy counters are zero for new point types
            }
            else if (inventory.extended_number_of_point_records > toUnsignedLong(U32_MAX))
            {
                if (header.version_minor >= 4)
                {
                    number = 0;
                }
                else
                {
                    fprintf(stderr,"WARNING: too many points in LAS %d.%d file. limit is %d.\n", header.version_major, header.version_minor, toUnsignedLong(U32_MAX));
                    number = U32_MAX;
                }
            }
            else
            {
                number = (int)inventory.extended_number_of_point_records;
            }
            if (!stream.put32bitsLE(number))
            {
                fprintf(stderr,"ERROR: updating inventory.number_of_point_records\n");
                return FALSE;
            }
            npoints = inventory.extended_number_of_point_records;
            for (i = 0; i < 5; i++)
            {
                if (header.point_data_format >= 6)
                {
                    number = 0; // legacy counters are zero for new point types
                }
                else if (inventory.extended_number_of_points_by_return[i+1] > toUnsignedLong(U32_MAX))
                {
                    if (header.version_minor >= 4)
                    {
                        number = 0;
                    }
                    else
                    {
                        number = U32_MAX;
                    }
                }
                else
                {
                    number = (int)inventory.extended_number_of_points_by_return[i+1];
                }
                if (!stream.put32bitsLE(number))
                {
                    fprintf(stderr,"ERROR: updating inventory.number_of_points_by_return[%d]\n", i);
                    return FALSE;
                }
            }
            stream.seek(header_start_position+179);
            double value;
            value = quantizer.get_x(inventory.max_X);
            if (!stream.put64bitsLE(doubleToLongBits(value)))
            {
                fprintf(stderr,"ERROR: updating inventory.max_X\n");
                return FALSE;
            }
            value = quantizer.get_x(inventory.min_X);
            if (!stream.put64bitsLE(doubleToLongBits(value)))
            {
                fprintf(stderr,"ERROR: updating inventory.min_X\n");
                return FALSE;
            }
            value = quantizer.get_y(inventory.max_Y);
            if (!stream.put64bitsLE(doubleToLongBits(value)))
            {
                fprintf(stderr,"ERROR: updating inventory.max_Y\n");
                return FALSE;
            }
            value = quantizer.get_y(inventory.min_Y);
            if (!stream.put64bitsLE(doubleToLongBits(value)))
            {
                fprintf(stderr,"ERROR: updating inventory.min_Y\n");
                return FALSE;
            }
            value = quantizer.get_z(inventory.max_Z);
            if (!stream.put64bitsLE(doubleToLongBits(value)))
            {
                fprintf(stderr,"ERROR: updating inventory.max_Z\n");
                return FALSE;
            }
            value = quantizer.get_z(inventory.min_Z);
            if (!stream.put64bitsLE(doubleToLongBits(value)))
            {
                fprintf(stderr,"ERROR: updating inventory.min_Z\n");
                return FALSE;
            }
            // special handling for LAS 1.4 or higher.
            if (header.version_minor >= 4)
            {
                stream.seek(header_start_position+247);
                if (!stream.put64bitsLE(inventory.extended_number_of_point_records))
                {
                    fprintf(stderr,"ERROR: updating header->extended_number_of_point_records\n");
                    return FALSE;
                }
                for (i = 0; i < 15; i++)
                {
                    if (!stream.put64bitsLE(inventory.extended_number_of_points_by_return[i+1]))
                    {
                        fprintf(stderr,"ERROR: updating header->extended_number_of_points_by_return[%d]\n", i);
                        return FALSE;
                    }
                }
            }
        }
        else
        {
            int number; // unsigned
            stream.seek(header_start_position+107);
            if (header.point_data_format >= 6)
            {
                number = 0; // legacy counters are zero for new point types
            }
            else
            {
                number = header.number_of_point_records;
            }
            if (!stream.put32bitsLE(number))
            {
                fprintf(stderr,"ERROR: updating header->number_of_point_records\n");
                return FALSE;
            }
            npoints = header.number_of_point_records;
            for (i = 0; i < 5; i++)
            {
                if (header.point_data_format >= 6)
                {
                    number = 0; // legacy counters are zero for new point types
                }
                else
                {
                    number = header.number_of_points_by_return[i];
                }
                if (!stream.put32bitsLE(number))
                {
                    fprintf(stderr,"ERROR: updating header->number_of_points_by_return[%d]\n", i);
                    return FALSE;
                }
            }
            stream.seek(header_start_position+179);
            if (!stream.put64bitsLE(doubleToLongBits(header.max_x)))
            {
                fprintf(stderr,"ERROR: updating header->max_x\n");
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.min_x)))
            {
                fprintf(stderr,"ERROR: updating header->min_x\n");
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.max_y)))
            {
                fprintf(stderr,"ERROR: updating header->max_y\n");
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.min_y)))
            {
                fprintf(stderr,"ERROR: updating header->min_y\n");
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.max_z)))
            {
                fprintf(stderr,"ERROR: updating header->max_z\n");
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.min_z)))
            {
                fprintf(stderr,"ERROR: updating header->min_z\n");
                return FALSE;
            }
            // special handling for LAS 1.3 or higher.
            if (header.version_minor >= 3)
            {
                // nobody currently includes waveform. we set the field always to zero
                if (header.start_of_waveform_data_packet_record != 0)
                {
                    fprintf(stderr,"WARNING: header->start_of_waveform_data_packet_record is %d. writing 0 instead.\n", header.start_of_waveform_data_packet_record);
                    long start_of_waveform_data_packet_record = 0;
                    if (!stream.put64bitsLE(start_of_waveform_data_packet_record))
                    {
                        fprintf(stderr,"ERROR: updating start_of_waveform_data_packet_record\n");
                        return FALSE;
                    }
                }
                else
                {
                    if (!stream.put64bitsLE(header.start_of_waveform_data_packet_record))
                    {
                        fprintf(stderr,"ERROR: updating header->start_of_waveform_data_packet_record\n");
                        return FALSE;
                    }
                }
            }
            // special handling for LAS 1.4 or higher.
            if (header.version_minor >= 4)
            {
                stream.seek(header_start_position+235);
                if (!stream.put64bitsLE(header.start_of_first_extended_variable_length_record))
                {
                    fprintf(stderr,"ERROR: updating header->start_of_first_extended_variable_length_record\n");
                    return FALSE;
                }
                if (!stream.put32bitsLE(header.number_of_extended_variable_length_records))
                {
                    fprintf(stderr,"ERROR: updating header->number_of_extended_variable_length_records\n");
                    return FALSE;
                }
                long value;
                if (header.number_of_point_records != 0)
                    value = header.number_of_point_records;
                else
                    value = header.extended_number_of_point_records;
                if (!stream.put64bitsLE(value))
                {
                    fprintf(stderr,"ERROR: updating header->extended_number_of_point_records\n");
                    return FALSE;
                }
                for (i = 0; i < 15; i++)
                {
                    if ((i < 5) && header.number_of_points_by_return[i] != 0)
                        value = header.number_of_points_by_return[i];
                    else
                        value = header.extended_number_of_points_by_return[i];
                    if (!stream.put64bitsLE(value))
                    {
                        fprintf(stderr,"ERROR: updating header->extended_number_of_points_by_return[%d]\n", i);
                        return FALSE;
                    }
                }
            }
        }
        stream.seekEnd();
        if (update_extra_bytes)
        {
            if (header == null)
            {
                fprintf(stderr,"ERROR: header pointer is zero\n");
                return FALSE;
            }
            if (header.number_attributes != 0)
            {
                long start = header_start_position + header.header_size;
                for (i = 0; i < header.number_of_variable_length_records; i++)
                {
                    start += 54;
                    if ((header.vlrs[i].record_id == 4) && (strcmp(header.vlrs[i].user_id, "LASF_Spec") == 0))
                    {
                        break;
                    }
                    else
                    {
                        start += header.vlrs[i].record_length_after_header;
                    }
                }
                if (i == header.number_of_variable_length_records)
                {
                    fprintf(stderr,"WARNING: could not find extra bytes VLR for update\n");
                }
                else
                {
                    stream.seek(start);
                    if (!stream.putBytes(header.vlrs[i].data, header.vlrs[i].record_length_after_header))
                    {
                        fprintf(stderr,"ERROR: writing %d bytes of data from header->vlrs[%d].data\n", header.vlrs[i].record_length_after_header, i);
                        return FALSE;
                    }
                }
            }
            stream.seekEnd();
        }
        return TRUE;
    }

    @Override
    public long close(boolean update_npoints) {
        long bytes = 0;

        if (p_count != npoints)
        {
            if (npoints != 0 || !update_npoints)
            {
                fprintf(stderr,"WARNING: written %lld points but expected %d points\n", p_count, npoints);
            }
        }

        if (writer != null)
        {
            writer.done();
            writer = null;
        }

        if (writing_las_1_4 && number_of_extended_variable_length_records != 0)
        {
            long real_start_of_first_extended_variable_length_record = stream.tell();

            // write extended variable length records variable after variable (to avoid alignment issues)

            for (int i = 0; i < number_of_extended_variable_length_records; i++)
            {
                // check variable length records contents

                if (evlrs[i].reserved != 0xAABB)
                {
                    fprintf(stderr,"WARNING: wrong evlrs[%d].reserved: %d != 0xAABB\n", i, evlrs[i].reserved);
                }

                // write variable length records variable after variable (to avoid alignment issues)

                if (!stream.put16bitsLE(evlrs[i].reserved))
                {
                    fprintf(stderr,"ERROR: writing evlrs[%d].reserved\n", i);
                    return 0;
                }
                if (!stream.putBytes(evlrs[i].user_id, 16))
                {
                    fprintf(stderr,"ERROR: writing evlrs[%d].user_id\n", i);
                    return 0;
                }
                if (!stream.put16bitsLE(evlrs[i].record_id))
                {
                    fprintf(stderr,"ERROR: writing evlrs[%d].record_id\n", i);
                    return 0;
                }
                if (!stream.put64bitsLE(evlrs[i].record_length_after_header))
                {
                    fprintf(stderr,"ERROR: writing evlrs[%d].record_length_after_header\n", i);
                    return 0;
                }
                if (!stream.putBytes(evlrs[i].description, 32))
                {
                    fprintf(stderr,"ERROR: writing evlrs[%d].description\n", i);
                    return 0;
                }

                // write the data following the header of the variable length record

                if (evlrs[i].record_length_after_header != 0)
                {
                    if (evlrs[i].data != null)
                    {
                        if (!stream.putBytes(evlrs[i].data, (int) evlrs[i].record_length_after_header))
                        {
                            fprintf(stderr,"ERROR: writing %d bytes of data from evlrs[%d].data\n", evlrs[i].record_length_after_header, i);
                            return 0;
                        }
                    }
                    else
                    {
                        fprintf(stderr,"ERROR: there should be %d bytes of data in evlrs[%d].data\n", evlrs[i].record_length_after_header, i);
                        return 0;
                    }
                }
            }

            if (real_start_of_first_extended_variable_length_record != start_of_first_extended_variable_length_record)
            {
                stream.seek(header_start_position+235);
                stream.put64bitsLE(real_start_of_first_extended_variable_length_record);
                stream.seekEnd();
            }
        }

        if (stream != null)
        {
            if (update_npoints && p_count != npoints)
            {
                if (!stream.isSeekable())
                {
                    fprintf(stderr, "WARNING: stream not seekable. cannot update header from %d to %d points.\n", npoints, p_count);
                }
                else
                {
                    int number; // unsigned
                    if (writing_new_point_type)
                    {
                        number = 0;
                    }
                    else if (p_count > toUnsignedLong(U32_MAX))
                    {
                        if (writing_las_1_4)
                        {
                            number = 0;
                        }
                        else
                        {
                            number = U32_MAX;
                        }
                    }
                    else
                    {
                        number = (int)p_count;
                    }
                    stream.seek(header_start_position+107);
                    stream.put32bitsLE(number);
                    if (writing_las_1_4)
                    {
                        stream.seek(header_start_position+235+12);
                        stream.put64bitsLE(p_count);
                    }
                    stream.seekEnd();
                }
            }
            bytes = stream.tell() - header_start_position;
            if (delete_stream)
            {
                try {
                    stream.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            stream = null;
        }

        if (file != null)
        {
            fclose(file);
            file = null;
        }

        npoints = p_count;
        p_count = 0;

        return bytes;
    }

    public boolean open(LASheader header, char compressor, int requested_version, int chunk_size) {
        ByteStreamOut out = new ByteStreamOutArray();
        return open(out, header, compressor, requested_version, chunk_size);
    }

    public boolean open(String file_name, LASheader header, char compressor, int requested_version, int chunk_size, int io_buffer_size) {
        if (file_name == null)
        {
            fprintf(stderr,"ERROR: file name is null\n");
            return FALSE;
        }

        file = fopenRAF(file_name.toCharArray(), "rwb");

        if (file == null)
        {
            fprintf(stderr, "ERROR: cannot open file '%s' for write\n", file_name);
            return FALSE;
        }

        return open(file, header, compressor, requested_version, chunk_size);
    }

    public boolean open(RandomAccessFile file, LASheader header, char compressor, int requested_version, int chunk_size) {
        ByteStreamOut out = new ByteStreamOutFile(file);
        return open(out, header, compressor, requested_version, chunk_size);
    }

    public boolean open(PrintStream stdout, LASheader header, char compressor, int requested_version, int chunk_size) {
        ByteStreamOut out = new ByteStreamOutOstream(stdout);
        return open(out, header, compressor, requested_version, chunk_size);
    }

    public boolean open(ByteStreamOut stream, LASheader header, char compressor, int requested_version, int chunk_size)
    {
        int i, j; // unsigned

        if (stream == null)
        {
            fprintf(stderr,"ERROR: ByteStreamOut pointer is zero\n");
            return FALSE;
        }
        this.stream = stream;

        if (header == null)
        {
            fprintf(stderr,"ERROR: LASheader pointer is zero\n");
            return FALSE;
        }

        // check header contents

        if (!header.check()) return FALSE;

        // copy scale_and_offset
        quantizer.x_scale_factor = header.x_scale_factor;
        quantizer.y_scale_factor = header.y_scale_factor;
        quantizer.z_scale_factor = header.z_scale_factor;
        quantizer.x_offset = header.x_offset;
        quantizer.y_offset = header.y_offset;
        quantizer.z_offset = header.z_offset;

        // check if the requested point type is supported

        LASpoint point = new LASpoint();
        byte[] point_data_format = new byte[1]; // unsigned
        char[] point_data_record_length = new char[1]; // unsigned
        boolean point_is_standard = TRUE;

        if (header.laszip != null)
        {
            if (!point.init(quantizer, header.laszip.num_items, header.laszip.items, header)) return FALSE;
            point_is_standard = header.laszip.is_standard(point_data_format, point_data_record_length);
        }
        else
        {
            if (!point.init(quantizer, header.point_data_format, header.point_data_record_length, header)) return FALSE;
            point_data_format[0] = header.point_data_format;
            point_data_record_length[0] = header.point_data_record_length;
        }

        // fail if we don't use the layered compressor for the new LAS 1.4 point types

        if (compressor != 0 && (point_data_format[0] > 5) && (compressor != LASZIP_COMPRESSOR_LAYERED_CHUNKED))
        {
            fprintf(stderr,"ERROR: point type %d requires using \"native LAS 1.4 extension\" of LASzip\n", point_data_format[0]);
            return FALSE;
        }

        // do we need a LASzip VLR (because we compress or use non-standard points?)

        LASzip laszip = null;
        int laszip_vlr_data_size = 0; // unsigned
        if (compressor != 0 || point_is_standard == FALSE)
        {
            laszip = new LASzip();
            laszip.setup(point.num_items, point.items, compressor);
            if (chunk_size > -1) laszip.set_chunk_size(chunk_size);
            if (compressor == LASZIP_COMPRESSOR_NONE) laszip.request_version((char) 0);
            else if (chunk_size == 0 && (point_data_format[0] <= 5)) { fprintf(stderr,"ERROR: adaptive chunking is depricated for point type %d.\n       only available for new LAS 1.4 point types 6 or higher.\n", point_data_format[0]); return FALSE; }
            else if (requested_version != 0) laszip.request_version((char) requested_version);
            else laszip.request_version((char) 2);
            laszip_vlr_data_size = 34 + 6*laszip.num_items;
        }

        // create and setup the point writer

        writer = new LASwritePoint();
        if (laszip != null)
        {
            if (!writer.setup(laszip.num_items, laszip.items, laszip))
            {
                fprintf(stderr,"ERROR: point type %d of size %d not supported (with LASzip)\n", header.point_data_format, header.point_data_record_length);
                return FALSE;
            }
        }
        else
        {
            if (!writer.setup(point.num_items, point.items))
            {
                fprintf(stderr,"ERROR: point type %d of size %d not supported\n", header.point_data_format, header.point_data_record_length);
                return FALSE;
            }
        }

        // save the position where we start writing the header

        header_start_position = stream.tell();

        // write header variable after variable (to avoid alignment issues)

        if (!stream.putBytes(header.file_signature, 4))
        {
            fprintf(stderr,"ERROR: writing header->file_signature\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.file_source_ID))
        {
            fprintf(stderr,"ERROR: writing header->file_source_ID\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.global_encoding))
        {
            fprintf(stderr,"ERROR: writing header->global_encoding\n");
            return FALSE;
        }
        if (!stream.put32bitsLE(header.project_ID_GUID_data_1))
        {
            fprintf(stderr,"ERROR: writing header->project_ID_GUID_data_1\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.project_ID_GUID_data_2))
        {
            fprintf(stderr,"ERROR: writing header->project_ID_GUID_data_2\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.project_ID_GUID_data_3))
        {
            fprintf(stderr,"ERROR: writing header->project_ID_GUID_data_3\n");
            return FALSE;
        }
        if (!stream.putBytes(header.project_ID_GUID_data_4, 8))
        {
            fprintf(stderr,"ERROR: writing header->project_ID_GUID_data_4\n");
            return FALSE;
        }
        // check version major
        byte version_major = header.version_major; // unsigned
        if (header.version_major != 1)
        {
            fprintf(stderr,"WARNING: header->version_major is %d. writing 1 instead.\n", header.version_major);
            version_major = 1;
        }
        if (!stream.putByte(version_major))
        {
            fprintf(stderr,"ERROR: writing header->version_major\n");
            return FALSE;
        }
        // check version minor
        byte version_minor = header.version_minor; // unsigned
        if (version_minor > 4)
        {
            fprintf(stderr,"WARNING: header->version_minor is %d. writing 4 instead.\n", version_minor);
            version_minor = 4;
        }
        if (!stream.putByte(version_minor))
        {
            fprintf(stderr,"ERROR: writing header->version_minor\n");
            return FALSE;
        }
        if (!stream.putBytes(header.system_identifier, 32))
        {
            fprintf(stderr,"ERROR: writing header->system_identifier\n");
            return FALSE;
        }
        if (!stream.putBytes(header.generating_software, 32))
        {
            fprintf(stderr,"ERROR: writing header->generating_software\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.file_creation_day))
        {
            fprintf(stderr,"ERROR: writing header->file_creation_day\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.file_creation_year))
        {
            fprintf(stderr,"ERROR: writing header->file_creation_year\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.header_size))
        {
            fprintf(stderr,"ERROR: writing header->header_size\n");
            return FALSE;
        }
        int offset_to_point_data = header.offset_to_point_data; // unsigned
        if (laszip != null) offset_to_point_data += (54 + laszip_vlr_data_size);
        if (header.vlr_lastiling != null) offset_to_point_data += (54 + 28);
        if (header.vlr_lasoriginal != null) offset_to_point_data += (54 + 176);
        if (!stream.put32bitsLE(offset_to_point_data))
        {
            fprintf(stderr,"ERROR: writing header->offset_to_point_data\n");
            return FALSE;
        }
        int number_of_variable_length_records = header.number_of_variable_length_records; // unsigned
        if (laszip != null) number_of_variable_length_records++;
        if (header.vlr_lastiling != null) number_of_variable_length_records++;
        if (header.vlr_lasoriginal != null) number_of_variable_length_records++;
        if (!stream.put32bitsLE(number_of_variable_length_records))
        {
            fprintf(stderr,"ERROR: writing header->number_of_variable_length_records\n");
            return FALSE;
        }
        if (compressor != 0) point_data_format[0] |= 128;
        if (!stream.putByte(point_data_format[0]))
        {
            fprintf(stderr,"ERROR: writing header->point_data_format\n");
            return FALSE;
        }
        if (!stream.put16bitsLE(header.point_data_record_length))
        {
            fprintf(stderr,"ERROR: writing header->point_data_record_length\n");
            return FALSE;
        }
        if (!stream.put32bitsLE(header.number_of_point_records))
        {
            fprintf(stderr,"ERROR: writing header->number_of_point_records\n");
            return FALSE;
        }
        for (i = 0; i < 5; i++)
        {
            if (!stream.put32bitsLE(header.number_of_points_by_return[i]))
            {
                fprintf(stderr,"ERROR: writing header->number_of_points_by_return[%d]\n", i);
                return FALSE;
            }
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.x_scale_factor)))
        {
            fprintf(stderr,"ERROR: writing header->x_scale_factor\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.y_scale_factor)))
        {
            fprintf(stderr,"ERROR: writing header->y_scale_factor\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.z_scale_factor)))
        {
            fprintf(stderr,"ERROR: writing header->z_scale_factor\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.x_offset)))
        {
            fprintf(stderr,"ERROR: writing header->x_offset\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.y_offset)))
        {
            fprintf(stderr,"ERROR: writing header->y_offset\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.z_offset)))
        {
            fprintf(stderr,"ERROR: writing header->z_offset\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.max_x)))
        {
            fprintf(stderr,"ERROR: writing header->max_x\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.min_x)))
        {
            fprintf(stderr,"ERROR: writing header->min_x\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.max_y)))
        {
            fprintf(stderr,"ERROR: writing header->max_y\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.min_y)))
        {
            fprintf(stderr,"ERROR: writing header->min_y\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.max_z)))
        {
            fprintf(stderr,"ERROR: writing header->max_z\n");
            return FALSE;
        }
        if (!stream.put64bitsLE(doubleToLongBits(header.min_z)))
        {
            fprintf(stderr,"ERROR: writing header->min_z\n");
            return FALSE;
        }

        // special handling for LAS 1.3 or higher.
        if (version_minor >= 3)
        {
            long start_of_waveform_data_packet_record = header.start_of_waveform_data_packet_record; // unsigned
            if (start_of_waveform_data_packet_record != 0)
            {
                fprintf(stderr,"WARNING: header->start_of_waveform_data_packet_record is %d. writing 0 instead.\n", start_of_waveform_data_packet_record);
                start_of_waveform_data_packet_record = 0;
            }
            if (!stream.put64bitsLE(start_of_waveform_data_packet_record))
            {
                fprintf(stderr,"ERROR: writing start_of_waveform_data_packet_record\n");
                return FALSE;
            }
        }

        // special handling for LAS 1.4 or higher.
        if (version_minor >= 4)
        {
            writing_las_1_4 = TRUE;
            if (header.point_data_format >= 6)
            {
                writing_new_point_type = TRUE;
            }
            else
            {
                writing_new_point_type = FALSE;
            }
            start_of_first_extended_variable_length_record = header.start_of_first_extended_variable_length_record;
            if (!stream.put64bitsLE(start_of_first_extended_variable_length_record))
            {
                fprintf(stderr,"ERROR: writing header->start_of_first_extended_variable_length_record\n");
                return FALSE;
            }
            number_of_extended_variable_length_records = header.number_of_extended_variable_length_records;
            if (!stream.put32bitsLE(number_of_extended_variable_length_records))
            {
                fprintf(stderr,"ERROR: writing header->number_of_extended_variable_length_records\n");
                return FALSE;
            }
            evlrs = header.evlrs;
            long extended_number_of_point_records; // unsigned
            if (header.number_of_point_records != 0)
                extended_number_of_point_records = header.number_of_point_records;
            else
                extended_number_of_point_records = header.extended_number_of_point_records;
            if (!stream.put64bitsLE(extended_number_of_point_records))
            {
                fprintf(stderr,"ERROR: writing header->extended_number_of_point_records\n");
                return FALSE;
            }
            long extended_number_of_points_by_return; // unsigned
            for (i = 0; i < 15; i++)
            {
                if ((i < 5) && header.number_of_points_by_return[i] != 0)
                    extended_number_of_points_by_return = header.number_of_points_by_return[i];
                else
                    extended_number_of_points_by_return = header.extended_number_of_points_by_return[i];
                if (!stream.put64bitsLE(extended_number_of_points_by_return))
                {
                    fprintf(stderr,"ERROR: writing header->extended_number_of_points_by_return[%d]\n", i);
                    return FALSE;
                }
            }
        }
        else
        {
            writing_las_1_4 = FALSE;
            writing_new_point_type = FALSE;
        }

        // write any number of user-defined bytes that might have been added into the header

        if (header.user_data_in_header_size != 0)
        {
            if (header.user_data_in_header != null)
            {
                if (!stream.putBytes(header.user_data_in_header, header.user_data_in_header_size))
                {
                    fprintf(stderr,"ERROR: writing %d bytes of data from header->user_data_in_header\n", header.user_data_in_header_size);
                    return FALSE;
                }
            }
            else
            {
                fprintf(stderr,"ERROR: there should be %d bytes of data in header->user_data_in_header\n", header.user_data_in_header_size);
                return FALSE;
            }
        }

        // write variable length records variable after variable (to avoid alignment issues)

        for (i = 0; i < header.number_of_variable_length_records; i++)
        {
            // check variable length records contents

            if (header.vlrs[i].reserved != 0xAABB)
            {
                // commented out because las specification 1.4 says this mu st now be zero
                // fprintf(stderr,"WARNING: wrong header->vlrs[%d].reserved: 0x%04x != 0xAABB\n", i, (int) header.vlrs[i].reserved);
            }

            // write variable length records variable after variable (to avoid alignment issues)

            if (!stream.put16bitsLE(header.vlrs[i].reserved))
            {
                fprintf(stderr,"ERROR: writing header->vlrs[%d].reserved\n", i);
                return FALSE;
            }
            if (!stream.putBytes(header.vlrs[i].user_id, 16))
            {
                fprintf(stderr,"ERROR: writing header->vlrs[%d].user_id\n", i);
                return FALSE;
            }
            if (!stream.put16bitsLE(header.vlrs[i].record_id))
            {
                fprintf(stderr,"ERROR: writing header->vlrs[%d].record_id\n", i);
                return FALSE;
            }
            if (!stream.put16bitsLE(header.vlrs[i].record_length_after_header))
            {
                fprintf(stderr,"ERROR: writing header->vlrs[%d].record_length_after_header\n", i);
                return FALSE;
            }
            if (!stream.putBytes(header.vlrs[i].description, 32))
            {
                fprintf(stderr,"ERROR: writing header->vlrs[%d].description\n", i);
                return FALSE;
            }

            // write the data following the header of the variable length record

            if (header.vlrs[i].record_length_after_header != 0)
            {
                if (header.vlrs[i].data != null)
                {
                    if (!stream.putBytes(header.vlrs[i].data, header.vlrs[i].record_length_after_header))
                    {
                        fprintf(stderr,"ERROR: writing %d bytes of data from header->vlrs[%d].data\n", header.vlrs[i].record_length_after_header, i);
                        return FALSE;
                    }
                }
                else
                {
                    fprintf(stderr,"ERROR: there should be %d bytes of data in header->vlrs[%d].data\n", header.vlrs[i].record_length_after_header, i);
                    return FALSE;
                }
            }
        }

        // write laszip VLR with compression parameters

        if (laszip != null)
        {
            // write variable length records variable after variable (to avoid alignment issues)

            char reserved = 0xAABB; // unsigned
            if (!stream.put16bitsLE(reserved))
            {
                fprintf(stderr,"ERROR: writing reserved %d\n", (int)reserved);
                return FALSE;
            }
            byte[] user_id = new byte[16];
            sprintf(user_id, "laszip encoded");
            if (!stream.putBytes(user_id, 16))
            {
                fprintf(stderr,"ERROR: writing user_id %s\n", stringFromByteArray(user_id));
                return FALSE;
            }
            char record_id = 22204; // unsigned
            if (!stream.put16bitsLE(record_id))
            {
                fprintf(stderr,"ERROR: writing record_id %d\n", (int)record_id);
                return FALSE;
            }
            char record_length_after_header = (char) laszip_vlr_data_size; // unsigned
            if (!stream.put16bitsLE(record_length_after_header))
            {
                fprintf(stderr,"ERROR: writing record_length_after_header %d\n", (int)record_length_after_header);
                return FALSE;
            }
            byte[] description = new byte[32];
            sprintf(description, "by laszip of LAStools (%d)", LAS_TOOLS_VERSION);
            if (!stream.putBytes(description, 32))
            {
                fprintf(stderr,"ERROR: writing description %s\n", stringFromByteArray(description));
                return FALSE;
            }
            // write the data following the header of the variable length record
            //     U16  compressor                2 bytes
            //     U32  coder                     2 bytes
            //     U8   version_major             1 byte
            //     U8   version_minor             1 byte
            //     U16  version_revision          2 bytes
            //     U32  options                   4 bytes
            //     I32  chunk_size                4 bytes
            //     I64  number_of_special_evlrs   8 bytes
            //     I64  offset_to_special_evlrs   8 bytes
            //     U16  num_items                 2 bytes
            //        U16 type                2 bytes * num_items
            //        U16 size                2 bytes * num_items
            //        U16 version             2 bytes * num_items
            // which totals 34+6*num_items

            if (!stream.put16bitsLE(laszip.compressor))
            {
                fprintf(stderr,"ERROR: writing compressor %d\n", (int)compressor);
                return FALSE;
            }
            if (!stream.put16bitsLE(laszip.coder))
            {
                fprintf(stderr,"ERROR: writing coder %d\n", (int)laszip.coder);
                return FALSE;
            }
            if (!stream.putByte(laszip.version_major))
            {
                fprintf(stderr,"ERROR: writing version_major %d\n", laszip.version_major);
                return FALSE;
            }
            if (!stream.putByte(laszip.version_minor))
            {
                fprintf(stderr,"ERROR: writing version_minor %d\n", laszip.version_minor);
                return FALSE;
            }
            if (!stream.put16bitsLE(laszip.version_revision))
            {
                fprintf(stderr,"ERROR: writing version_revision %d\n", laszip.version_revision);
                return FALSE;
            }
            if (!stream.put32bitsLE(laszip.options))
            {
                fprintf(stderr,"ERROR: writing options %d\n", laszip.options);
                return FALSE;
            }
            if (!stream.put32bitsLE(laszip.chunk_size))
            {
                fprintf(stderr,"ERROR: writing chunk_size %d\n", laszip.chunk_size);
                return FALSE;
            }
            if (!stream.put64bitsLE(laszip.number_of_special_evlrs))
            {
                fprintf(stderr,"ERROR: writing number_of_special_evlrs %d\n", (int)laszip.number_of_special_evlrs);
                return FALSE;
            }
            if (!stream.put64bitsLE(laszip.offset_to_special_evlrs))
            {
                fprintf(stderr,"ERROR: writing offset_to_special_evlrs %d\n", laszip.offset_to_special_evlrs);
                return FALSE;
            }
            if (!stream.put16bitsLE(laszip.num_items))
            {
                fprintf(stderr,"ERROR: writing num_items %d\n", laszip.num_items);
                return FALSE;
            }
            for (i = 0; i < laszip.num_items; i++)
            {
                if (!stream.put16bitsLE((char) laszip.items[i].type.ordinal()))
                {
                    fprintf(stderr,"ERROR: writing type %d of item %d\n", laszip.items[i].type.ordinal(), i);
                    return FALSE;
                }
                if (!stream.put16bitsLE(laszip.items[i].size))
                {
                    fprintf(stderr,"ERROR: writing size %d of item %d\n", laszip.items[i].size, i);
                    return FALSE;
                }
                if (!stream.put16bitsLE(laszip.items[i].version))
                {
                    fprintf(stderr,"ERROR: writing version %d of item %d\n", laszip.items[i].version, i);
                    return FALSE;
                }
            }

            laszip = null;
        }

        // write lastiling VLR with the tile parameters

        if (header.vlr_lastiling != null)
        {
            // write variable length records variable after variable (to avoid alignment issues)

            char reserved = 0xAABB; // unsigned
            if (!stream.put16bitsLE(reserved))
            {
                fprintf(stderr,"ERROR: writing reserved %d\n", (int)reserved);
                return FALSE;
            }
            byte[] user_id = new byte[16];
            sprintf(user_id, "LAStools");
            if (!stream.putBytes(user_id, 16))
            {
                fprintf(stderr,"ERROR: writing user_id %s\n", stringFromByteArray(user_id));
                return FALSE;
            }
            char record_id = 10; // unsigned
            if (!stream.put16bitsLE(record_id))
            {
                fprintf(stderr,"ERROR: writing record_id %d\n", (int)record_id);
                return FALSE;
            }
            char record_length_after_header = 28; // unsigned
            if (!stream.put16bitsLE(record_length_after_header))
            {
                fprintf(stderr,"ERROR: writing record_length_after_header %d\n", (int)record_length_after_header);
                return FALSE;
            }
            byte[] description = new byte[32];
            sprintf(description, "tile %s buffer %s", (header.vlr_lastiling.buffer != 0 ? "with" : "without"), (header.vlr_lastiling.reversible != 0 ? ", reversible" : ""));
            if (!stream.putBytes(description, 32))
            {
                fprintf(stderr,"ERROR: writing description %s\n", stringFromByteArray(description));
                return FALSE;
            }

            // write the payload of this VLR which contains 28 bytes
            //   U32  level                                          4 bytes
            //   U32  level_index                                    4 bytes
            //   U32  implicit_levels + buffer bit + reversible bit  4 bytes
            //   F32  min_x                                          4 bytes
            //   F32  max_x                                          4 bytes
            //   F32  min_y                                          4 bytes
            //   F32  max_y                                          4 bytes

            if (!stream.put32bitsLE(header.vlr_lastiling.level))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->level %u\n", header.vlr_lastiling.level);
                return FALSE;
            }
            if (!stream.put32bitsLE(header.vlr_lastiling.level_index))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->level_index %u\n", header.vlr_lastiling.level_index);
                return FALSE;
            }
            if (!stream.put32bitsLE(header.vlr_lastiling.implicit_levels))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->implicit_levels %u\n", header.vlr_lastiling.implicit_levels);
                return FALSE;
            }
            if (!stream.put32bitsLE(floatToIntBits(header.vlr_lastiling.min_x)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->min_x %g\n", header.vlr_lastiling.min_x);
                return FALSE;
            }
            if (!stream.put32bitsLE(floatToIntBits(header.vlr_lastiling.max_x)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->max_x %g\n", header.vlr_lastiling.max_x);
                return FALSE;
            }
            if (!stream.put32bitsLE(floatToIntBits(header.vlr_lastiling.min_y)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->min_y %g\n", header.vlr_lastiling.min_y);
                return FALSE;
            }
            if (!stream.put32bitsLE(floatToIntBits(header.vlr_lastiling.max_y)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lastiling->max_y %g\n", header.vlr_lastiling.max_y);
                return FALSE;
            }
        }

        // write lasoriginal VLR with the original (unbuffered) counts and bounding box extent

        if (header.vlr_lasoriginal != null)
        {
            // write variable length records variable after variable (to avoid alignment issues)

            char reserved = 0xAABB; // unsigned
            if (!stream.put16bitsLE(reserved))
            {
                fprintf(stderr,"ERROR: writing reserved %d\n", (int)reserved);
                return FALSE;
            }
            byte[] user_id = new byte[16];
            sprintf(user_id, "LAStools");
            if (!stream.putBytes(user_id, 16))
            {
                fprintf(stderr,"ERROR: writing user_id %s\n", stringFromByteArray(user_id));
                return FALSE;
            }
            char record_id = 20; // unsigned
            if (!stream.put16bitsLE(record_id))
            {
                fprintf(stderr,"ERROR: writing record_id %d\n", (int)record_id);
                return FALSE;
            }
            char record_length_after_header = 176; // unsigned
            if (!stream.put16bitsLE(record_length_after_header))
            {
                fprintf(stderr,"ERROR: writing record_length_after_header %d\n", (int)record_length_after_header);
                return FALSE;
            }
            byte[] description = new byte[32];
            sprintf(description, "counters and bbox of original");
            if (!stream.putBytes(description, 32))
            {
                fprintf(stderr,"ERROR: writing description %s\n", stringFromByteArray(description));
                return FALSE;
            }

            // save the position in the stream at which the payload of this VLR was written

            header.vlr_lasoriginal.position = stream.tell();

            // write the payload of this VLR which contains 176 bytes

            if (!stream.put64bitsLE(header.vlr_lasoriginal.number_of_point_records))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->number_of_point_records %d\n", header.vlr_lasoriginal.number_of_point_records);
                return FALSE;
            }
            for (j = 0; j < 15; j++)
            {
                if (!stream.put64bitsLE(header.vlr_lasoriginal.number_of_points_by_return[j]))
                {
                    fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->number_of_points_by_return[%u] %d\n", j, header.vlr_lasoriginal.number_of_points_by_return[j]);
                    return FALSE;
                }
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.vlr_lasoriginal.min_x)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->min_x %g\n", header.vlr_lasoriginal.min_x);
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.vlr_lasoriginal.max_x)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->max_x %g\n", header.vlr_lasoriginal.max_x);
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.vlr_lasoriginal.min_y)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->min_y %g\n", header.vlr_lasoriginal.min_y);
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.vlr_lasoriginal.max_y)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->max_y %g\n", header.vlr_lasoriginal.max_y);
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.vlr_lasoriginal.min_z)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->min_z %g\n", header.vlr_lasoriginal.min_z);
                return FALSE;
            }
            if (!stream.put64bitsLE(doubleToLongBits(header.vlr_lasoriginal.max_z)))
            {
                fprintf(stderr,"ERROR: writing header->vlr_lasoriginal->max_z %g\n", header.vlr_lasoriginal.max_z);
                return FALSE;
            }
        }

        // write any number of user-defined bytes that might have been added after the header

        if (header.user_data_after_header_size != 0)
        {
            if (header.user_data_after_header != null)
            {
                if (!stream.putBytes(header.user_data_after_header, header.user_data_after_header_size))
                {
                    fprintf(stderr,"ERROR: writing %d bytes of data from header->user_data_after_header\n", header.user_data_after_header_size);
                    return FALSE;
                }
            }
            else
            {
                fprintf(stderr,"ERROR: there should be %d bytes of data in header->user_data_after_header\n", header.user_data_after_header_size);
                return FALSE;
            }
        }

        // initialize the point writer

        if (!writer.init(stream)) return FALSE;

        npoints = (header.number_of_point_records != 0 ? header.number_of_point_records : header.extended_number_of_point_records);
        p_count = 0;

        return TRUE;
    }
}
