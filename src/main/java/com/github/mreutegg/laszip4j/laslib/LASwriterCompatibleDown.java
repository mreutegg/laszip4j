/*
 * Copyright 2007-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.ByteStreamOutArray;
import com.github.mreutegg.laszip4j.laszip.LASattribute;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.io.PrintStream;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_VERSION;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I16_QUANTIZE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U16_MAX;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwriterCompatibleDown extends LASwriter {

    private static final PrintStream stderr = System.err;

    private LASpoint pointCompatibleDown;
    private LASheader header;
    private LASwriter writer;
    private int start_scan_angle;
    private int start_extended_returns;
    private int start_classification;
    private int start_flags_and_channel;
    private int start_NIR_band;

    @Override
    public boolean chunk() {
        return FALSE;
    }

    public boolean open(LASheader header, LASwriteOpener laswriteopener, boolean moveCRSfromEVLRtoVLR, boolean moveEVLRtoVLR)
    {
        int i; // unsigned

        if (header == null)
        {
            return FALSE;
        }
        if (laswriteopener == null)
        {
            return FALSE;
        }
        if (header.version_minor < 4) // makes only sense for LAS 1.4 input
        {
            return FALSE;
        }
        if (header.point_data_format <= 5) // but not for old point types 0, 1, 2, 3, 4, and 5
        {
            return FALSE;
        }
        else if (header.point_data_format > 10) // only the new point types 6, 7, 8, 9, and 10 are supported
        {
            return FALSE;
        }
        this.header = header;

        // downgrade it to LAS 1.2 or LAS 1.3
        if (header.point_data_format <= 8)
        {
            header.version_minor = 2;
            // LAS 1.2 header is 148 bytes less than LAS 1.4+ header
            header.header_size -= 148;
            header.offset_to_point_data -= 148;
        }
        else
        {
            header.version_minor = 3;
            // LAS 1.3 header is 140 bytes less than LAS 1.4+ header
            header.header_size -= 140;
            header.offset_to_point_data -= 140;
        }

        // turn off the bit indicating the presence of the OGC WKT
        header.global_encoding &= ~(1<<4);

        // old point type is two bytes shorter
        header.point_data_record_length -= 2;
        // but we add 5 bytes of attributes
        header.point_data_record_length += 5;

        // create 2+2+4+148 bytes payload for compatibility VLR
        ByteStreamOutArray out = new ByteStreamOutArray();
        // write control info
        char lastools_version = (char)LAS_TOOLS_VERSION;
        out.put16bitsLE(lastools_version);
        char compatible_version = 3;
        out.put16bitsLE(compatible_version);
        int unused = 0;
        out.put32bitsLE(unused);
        // write the 148 bytes of the extended LAS 1.4 header
        long start_of_waveform_data_packet_record = header.start_of_waveform_data_packet_record;
        if (start_of_waveform_data_packet_record != 0)
        {
            fprintf(stderr,"WARNING: header.start_of_waveform_data_packet_record is %lld. writing 0 instead.\n", start_of_waveform_data_packet_record);
            start_of_waveform_data_packet_record = 0;
        }
        out.put64bitsLE(start_of_waveform_data_packet_record);
        long start_of_first_extended_variable_length_record = header.start_of_first_extended_variable_length_record;
        if (start_of_first_extended_variable_length_record != 0)
        {
            fprintf(stderr,"WARNING: EVLRs not supported. header.start_of_first_extended_variable_length_record is %lld. writing 0 instead.\n", start_of_first_extended_variable_length_record);
            start_of_first_extended_variable_length_record = 0;
        }
        out.put64bitsLE(start_of_first_extended_variable_length_record);
        int number_of_extended_variable_length_records = header.number_of_extended_variable_length_records;
        if (number_of_extended_variable_length_records != 0)
        {
            fprintf(stderr,"WARNING: EVLRs not supported. header.number_of_extended_variable_length_records is %d. writing 0 instead.\n", number_of_extended_variable_length_records);
            number_of_extended_variable_length_records = 0;
        }
        out.put32bitsLE(number_of_extended_variable_length_records);
        long extended_number_of_point_records;
        if (header.number_of_point_records != 0)
            extended_number_of_point_records = header.number_of_point_records;
        else
            extended_number_of_point_records = header.extended_number_of_point_records;
        out.put64bitsLE(extended_number_of_point_records);
        long extended_number_of_points_by_return;
        for (i = 0; i < 15; i++)
        {
            if ((i < 5) && header.number_of_points_by_return[i] != 0)
                extended_number_of_points_by_return = header.number_of_points_by_return[i];
            else
                extended_number_of_points_by_return = header.extended_number_of_points_by_return[i];
            out.put64bitsLE(extended_number_of_points_by_return);
        }
        // add the compatibility VLR
        header.add_vlr("lascompatible\0\0", (char) 22204, (char) (2+2+4+148), out.takeData().array());

        // scan_angle (difference or remainder) is stored as a I16
        LASattribute lasattribute_scan_angle = new LASattribute(3, "LAS 1.4 scan angle", "additional attributes");
        lasattribute_scan_angle.set_scale(0.006, 0);
        int index_scan_angle = header.add_attribute(lasattribute_scan_angle);
        start_scan_angle = header.get_attribute_start(index_scan_angle);
        // extended returns stored as a U8
        LASattribute lasattribute_extended_returns = new LASattribute(0, "LAS 1.4 extended returns", "additional attributes");
        int index_extended_returns = header.add_attribute(lasattribute_extended_returns);
        start_extended_returns = header.get_attribute_start(index_extended_returns);
        // classification stored as a U8
        LASattribute lasattribute_classification = new LASattribute(0, "LAS 1.4 classification", "additional attributes");
        int index_classification = header.add_attribute(lasattribute_classification);
        start_classification = header.get_attribute_start(index_classification);
        // flags and channel stored as a U8
        LASattribute lasattribute_flags_and_channel = new LASattribute(0, "LAS 1.4 flags and channel", "additional attributes");
        int index_flags_and_channel = header.add_attribute(lasattribute_flags_and_channel);
        start_flags_and_channel = header.get_attribute_start(index_flags_and_channel);
        // maybe store the NIR band as a U16
        if (header.point_data_format == 8 || header.point_data_format == 10)
        {
            // the NIR band is stored as a U16
            LASattribute lasattribute_NIR_band = new LASattribute(2, "LAS 1.4 NIR band", "additional attributes");
            int index_NIR_band = header.add_attribute(lasattribute_NIR_band);
            start_NIR_band = header.get_attribute_start(index_NIR_band);
        }
        // update VLR
        header.update_extra_bytes_vlr(TRUE);

        // update point type
        if (header.point_data_format == 6)
        {
            header.point_data_format = 1;
        }
        else if (header.point_data_format <= 8)
        {
            header.point_data_format = 3;
        }
        else // 9.4 and 10.5 
        {
            header.point_data_format -= 5;
        }

        // look for CRS in EVLRs and move them to VLRs

        if (moveEVLRtoVLR || moveCRSfromEVLRtoVLR)
        {
            if (header.evlrs != null)
            {
                for (i = 0; i < (int)header.number_of_extended_variable_length_records; i++)
                {
                    if (moveEVLRtoVLR)
                    {
                        if (header.evlrs[i].record_length_after_header <= U16_MAX)
                        {
                            header.add_vlr("LASF_Projection", header.evlrs[i].record_id, (char)header.evlrs[i].record_length_after_header, header.evlrs[i].data);
                            header.evlrs[i].record_length_after_header = 0;
                            header.evlrs[i].data = null;
                        }
                        else
                        {
                            fprintf(stderr,"large EVLR with user ID '%s' and record ID %d with payload size %lld not moved to VLRs.\n", stringFromByteArray(header.evlrs[i].user_id), header.evlrs[i].record_id, header.evlrs[i].record_length_after_header);
                        }
                    }
                    else if (strcmp(new String(header.evlrs[i].user_id), "LASF_Projection") == 0)
                    {
                        if (header.evlrs[i].record_id == 34735) // GeoKeyDirectoryTag
                        {
                            header.add_vlr("LASF_Projection", header.evlrs[i].record_id, (char)header.evlrs[i].record_length_after_header, header.evlrs[i].data);
                            header.evlrs[i].record_length_after_header = 0;
                            header.evlrs[i].data = null;
                        }
                        else if (header.evlrs[i].record_id == 34736) // GeoDoubleParamsTag
                        {
                            header.add_vlr("LASF_Projection", header.evlrs[i].record_id, (char)header.evlrs[i].record_length_after_header, header.evlrs[i].data);
                            header.evlrs[i].record_length_after_header = 0;
                            header.evlrs[i].data = null;
                        }
                        else if (header.evlrs[i].record_id == 34737) // GeoAsciiParamsTag
                        {
                            header.add_vlr("LASF_Projection", header.evlrs[i].record_id, (char)header.evlrs[i].record_length_after_header, header.evlrs[i].data);
                            header.evlrs[i].record_length_after_header = 0;
                            header.evlrs[i].data = null;
                        }
                        else if (header.evlrs[i].record_id == 2111) // OGC MATH TRANSFORM WKT
                        {
                            header.add_vlr("LASF_Projection", header.evlrs[i].record_id, (char)header.evlrs[i].record_length_after_header, header.evlrs[i].data);
                            header.evlrs[i].record_length_after_header = 0;
                            header.evlrs[i].data = null;
                        }
                        else if (header.evlrs[i].record_id == 2112) // OGC COORDINATE SYSTEM WKT
                        {
                            header.add_vlr("LASF_Projection", header.evlrs[i].record_id, (char)header.evlrs[i].record_length_after_header, header.evlrs[i].data);
                            header.evlrs[i].record_length_after_header = 0;
                            header.evlrs[i].data = null;
                        }
                        else
                        {
                            fprintf(stderr,"unknown LASF_Projection EVLR with record ID %d not moved to VLRs.\n", header.evlrs[i].record_id);
                        }
                    }
                }
            }
        }

        writer = laswriteopener.open(header);

        if (writer == null)
        {
            return FALSE;
        }

        pointCompatibleDown.init(header, header.point_data_format, header.point_data_record_length, header);

        return TRUE;
    }

    public boolean write_point(LASpoint point)
    {
        int scan_angle_remainder;
        int number_of_returns_increment;
        int return_number_increment;
        int return_count_difference;
        int overlap_bit;
        int scanner_channel;

        pointCompatibleDown = new LASpoint(point);

        // distill extended attributes

        scan_angle_remainder = pointCompatibleDown.getExtended_scan_angle() - I16_QUANTIZE(((float) pointCompatibleDown.getScan_angle_rank())/0.006f);
        if (pointCompatibleDown.getExtended_number_of_returns() <= 7)
        {
            pointCompatibleDown.setNumber_of_returns(pointCompatibleDown.getExtended_number_of_returns());
            if (pointCompatibleDown.getExtended_return_number() <= 7)
            {
                pointCompatibleDown.setReturn_number(pointCompatibleDown.getExtended_return_number());
            }
            else
            {
                pointCompatibleDown.setReturn_number((byte) 7);
            }
        }
        else
        {
            pointCompatibleDown.setNumber_of_returns((byte) 7);
            if (pointCompatibleDown.getExtended_return_number() <= 4)
            {
                pointCompatibleDown.setReturn_number(pointCompatibleDown.getExtended_return_number());
            }
            else
            {
                return_count_difference = pointCompatibleDown.getExtended_number_of_returns() - pointCompatibleDown.getExtended_return_number();
                if (return_count_difference <= 0)
                {
                    pointCompatibleDown.setReturn_number((byte) 7);
                }
                else if (return_count_difference >= 3)
                {
                    pointCompatibleDown.setReturn_number((byte) 4);
                }
                else
                {
                    pointCompatibleDown.setReturn_number((byte) (7 - return_count_difference));
                }
            }
        }
        return_number_increment = pointCompatibleDown.getExtended_return_number() - pointCompatibleDown.getReturn_number();
        assert(return_number_increment >= 0);
        number_of_returns_increment = pointCompatibleDown.getExtended_number_of_returns() - pointCompatibleDown.getNumber_of_returns();
        assert(number_of_returns_increment >= 0);
        if (pointCompatibleDown.getExtended_classification() > 31)
        {
            pointCompatibleDown.set_classification((byte) 0);
        }
        else
        {
            pointCompatibleDown.setExtended_classification((byte) 0);
        }
        scanner_channel = pointCompatibleDown.getExtended_scanner_channel();
        overlap_bit = (pointCompatibleDown.getExtended_classification_flags() >>> 3);

        // write distilled extended attributes into extra bytes 

        pointCompatibleDown.set_attribute(start_scan_angle, ((short) scan_angle_remainder));
        pointCompatibleDown.set_attribute(start_extended_returns, (byte) ((return_number_increment << 4) | number_of_returns_increment));
        pointCompatibleDown.set_attribute(start_classification, pointCompatibleDown.getExtended_classification());
        pointCompatibleDown.set_attribute(start_flags_and_channel, (byte)((scanner_channel << 1) | overlap_bit));
        if (start_NIR_band != -1)
        {
            pointCompatibleDown.set_attribute(start_NIR_band, pointCompatibleDown.getRgb(3));
        }

        writer.write_point(pointCompatibleDown);
        p_count++;
        return TRUE;
    }

    public boolean update_header(LASheader header, boolean use_inventory, boolean update_extra_bytes)
    {
        return writer.update_header(header, use_inventory, update_extra_bytes);
    }

    public long close(boolean update_header)
    {
        long bytes = writer.close(update_header);

        npoints = p_count;
        p_count = 0;

        return bytes;
    }

    public LASwriterCompatibleDown()
    {
        header = null;
        writer = null;
        start_scan_angle = -1;
        start_extended_returns = -1;
        start_classification = -1;
        start_flags_and_channel = -1;
        start_NIR_band = -1;
    }
}
