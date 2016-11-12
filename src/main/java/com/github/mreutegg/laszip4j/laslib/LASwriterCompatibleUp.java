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

import com.github.mreutegg.laszip4j.laszip.ByteStreamInArray;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.io.PrintStream;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I16_QUANTIZE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwriterCompatibleUp extends LASwriter {

    private static final PrintStream stderr = System.err;

    private LASpoint pointCompatibleUp;
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

    public boolean open(LASheader header, LASwriteOpener laswriteopener)
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
        if (header.version_minor > 3) // makes only sense for LAS 1.0, 1.1, 1.2, or 1.3 input
        {
            return FALSE;
        }
        else if (header.point_data_format == 0) // and not for the old point type 0
        {
            return FALSE;
        }
        else if (header.point_data_format == 2) // and not for the old point type 2
        {
            return FALSE;
        }
        else if (header.point_data_format > 5) // and certainly not fow newer point types
        {
            return FALSE;
        }
        // the compatibility VLR must be in the header
        LASvlr compatibility_vlr = header.get_vlr("lascompatible", 22204);
        if (compatibility_vlr == null)
        {
            fprintf(stderr, "ERROR: no compatibility VLR in header\n");
            return FALSE;
        }
        // the compatibility VLR must have the right length
        if (compatibility_vlr.record_length_after_header != (2+2+4+148))
        {
            fprintf(stderr, "ERROR: compatibility VLR has %d instead of %d bytes in payload\n", compatibility_vlr.record_length_after_header, 2+2+4+148);
            return FALSE;
        }
        int index_scan_angle = header.get_attribute_index("LAS 1.4 scan angle");
        if (index_scan_angle == -1)
        {
            fprintf(stderr, "ERROR: attribute \"LAS 1.4 scan angle\" is not in EXTRA_BYTES\n");
            return FALSE;
        }
        start_scan_angle = header.get_attribute_start(index_scan_angle);
        int index_extended_returns = header.get_attribute_index("LAS 1.4 extended returns");
        if (index_extended_returns == -1)
        {
            fprintf(stderr, "ERROR: attribute \"LAS 1.4 extended returns\" is not in EXTRA_BYTES\n");
            return FALSE;
        }
        start_extended_returns = header.get_attribute_start(index_extended_returns);
        int index_classification = header.get_attribute_index("LAS 1.4 classification");
        if (index_classification == -1)
        {
            fprintf(stderr, "ERROR: attribute \"LAS 1.4 classification\" is not in EXTRA_BYTES\n");
            return FALSE;
        }
        start_classification = header.get_attribute_start(index_classification);
        int index_flags_and_channel = header.get_attribute_index("LAS 1.4 flags and channel");
        // TODO: report bug. original:
        // if (index_scan_angle == -1)
        if (index_flags_and_channel == -1)
        {
            fprintf(stderr, "ERROR: attribute \"LAS 1.4 flags and channel\" is not in EXTRA_BYTES\n");
            return FALSE;
        }
        start_flags_and_channel = header.get_attribute_start(index_flags_and_channel);

        this.header = header;

        // upgrade it to LAS 1.4

        if (header.version_minor < 3)
        {
            // add the 148 byte difference between LAS 1.4 and LAS 1.2 header sizes
            header.header_size += 148;
            header.offset_to_point_data += 148;
        }
        else if (header.version_minor == 3)
        {
            // add the 140 byte difference between LAS 1.4 and LAS 1.3 header sizes
            header.header_size += 140;
            header.offset_to_point_data += 140;
        }
        header.version_minor = 4;

        // maybe turn on the bit indicating the presence of the OGC WKT
        for (i = 0; i < header.number_of_variable_length_records; i++)
        {
            if ((strncmp(stringFromByteArray(header.vlrs[i].user_id), "LASF_Projection", 16) == 0) && (header.vlrs[i].record_id == 2112))
            {
                header.global_encoding |= (1<<4);
                break;
            }
        }

        // read the 2+2+4+148 bytes payload from the compatibility VLR
        ByteStreamInArray in = new ByteStreamInArray(compatibility_vlr.data, compatibility_vlr.record_length_after_header);
        // read the 2+2+4+148 bytes of the extended LAS 1.4 header
        char lastools_version = in.get16bitsLE();
        char compatible_version = in.get16bitsLE();
        if (compatible_version != 3)
        {
            fprintf(stderr, "ERROR: compatibility mode version %d not implemented\n", compatible_version);
            return FALSE;
        }
        int unused = in.get32bitsLE(); // unsigned
        if (unused != 0)
        {
            fprintf(stderr, "WARNING: unused is %d instead of 0\n", unused);
        }
        header.start_of_waveform_data_packet_record = in.get64bitsLE();
        header.start_of_first_extended_variable_length_record = in.get64bitsLE();
        header.number_of_extended_variable_length_records = in.get32bitsLE();
        header.extended_number_of_point_records = in.get64bitsLE();
        for (i = 0; i < 15; i++)
        {
            header.extended_number_of_points_by_return[i] = in.get64bitsLE();
        }
        // delete the compatibility VLR
        header.remove_vlr("lascompatible", 22204);
        // zero the 32-bit legary counters as we have new point types
        header.number_of_point_records = 0;
        for (i = 0; i < 5; i++)
        {
            header.number_of_points_by_return[i] = 0;
        }
        // new point type is two bytes longer
        header.point_data_record_length += 2;
        // but we subtract 5 bytes of attributes
        header.point_data_record_length -= 5;
        // maybe we also have a NIR band?
        if ((header.point_data_format == 3) || (header.point_data_format == 5))
        {
            int index_NIR_band = header.get_attribute_index("LAS 1.4 NIR band");
            if (index_NIR_band != -1)
            {
                start_NIR_band = header.get_attribute_start(index_NIR_band);
                header.remove_attribute(index_NIR_band);
            }
        }
        // remove attributes from Extra Bytes VLR
        header.remove_attribute(index_flags_and_channel);
        header.remove_attribute(index_classification);
        header.remove_attribute(index_extended_returns);
        header.remove_attribute(index_scan_angle);
        // update VLR
        header.update_extra_bytes_vlr(TRUE);
        // update point type
        if (header.point_data_format == 1)
        {
            header.point_data_format = 6;
        }
        else if (header.point_data_format == 3)
        {
            if (start_NIR_band != -1)
            {
                header.point_data_format = 8;
            }
            else
            {
                header.point_data_format = 7;
            }
        }
        else
        {
            header.point_data_format += 5;
        }
        // remove old LASzip
        header.clean_laszip();

        writer = laswriteopener.open(header);

        if (writer == null)
        {
            return FALSE;
        }

        pointCompatibleUp.init(header, header.point_data_format, header.point_data_record_length, header);

        return TRUE;
    }

    public boolean write_point(LASpoint point)
    {
        short scan_angle;
        byte extended_returns;
        byte classification;
        byte flags_and_channel;
        int return_number_increment;
        int number_of_returns_increment;
        int overlap_bit;
        int scanner_channel;
        // copy point
        pointCompatibleUp = new LASpoint(point);
        // get extra_attributes
        scan_angle = point.get_attributeShort(start_scan_angle);
        extended_returns = point.get_attributeByte(start_extended_returns);
        classification = point.get_attributeByte(start_classification);
        flags_and_channel = point.get_attributeByte(start_flags_and_channel);
        if (start_NIR_band != -1)
        {
            pointCompatibleUp.setRgb(3, point.get_attributeChar(start_NIR_band));
        }
        // decompose into individual attributes
        return_number_increment = (extended_returns >>> 4) & 0x0F;
        number_of_returns_increment = extended_returns & 0x0F;
        scanner_channel = (flags_and_channel >>> 1) & 0x03;
        overlap_bit = flags_and_channel & 0x01;
        // instill into point
        pointCompatibleUp.setExtended_scan_angle((short) (scan_angle + I16_QUANTIZE(((float) pointCompatibleUp.getScan_angle_rank()) / 0.006f)));
        pointCompatibleUp.setExtended_return_number((byte) (return_number_increment + pointCompatibleUp.getReturn_number()));
        pointCompatibleUp.setExtended_number_of_returns((byte) (number_of_returns_increment + pointCompatibleUp.getNumber_of_returns()));
        pointCompatibleUp.setExtended_classification((byte) (classification + pointCompatibleUp.get_classification()));
        pointCompatibleUp.setExtended_scanner_channel((byte) scanner_channel);
        pointCompatibleUp.setExtended_classification_flags((byte) ((overlap_bit << 3) | (pointCompatibleUp.getClassification() >>> 5)));

        writer.write_point(pointCompatibleUp);
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

    public LASwriterCompatibleUp()
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
