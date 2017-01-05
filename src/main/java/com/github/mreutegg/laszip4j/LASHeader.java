/*
 * Copyright 2017 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j;

import com.github.mreutegg.laszip4j.laslib.LASheader;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;

/**
 * A LAS public header block.
 */
public final class LASHeader {

    private final LASheader header;

    LASHeader(LASheader header) {
        this.header = header;
    }

    /**
     * @return "File Signature" ("LASF").
     */
    public String getFileSignature() {
        return stringFromByteArray(header.file_signature);
    }

    /**
     * @return "File Source ID" as an unsigned short (char).
     */
    public char getFileSourceID() {
        return header.file_source_ID;
    }

    /**
     * @return "Global Encoding" as an unsigned short (char).
     */
    public char getGlobalEncoding() {
        return header.global_encoding;
    }

    /**
     * @return "Project ID - GUID data 1" as an unsigned int.
     */
    public int getProjectID_GUIDData1() {
        return header.project_ID_GUID_data_1;
    }

    /**
     * @return "Project ID - GUID data 2" as an unsigned short (char).
     */
    public char getProjectID_GUIDData2() {
        return header.project_ID_GUID_data_2;
    }

    /**
     * @return "Project ID - GUID data 3" as an unsigned short (char).
     */
    public char getProjectID_GUIDData3() {
        return header.project_ID_GUID_data_3;
    }

    /**
     * @return "Project ID - GUID data 4" as a byte array.
     */
    public byte[] getProjectID_GUIDData4() {
        byte[] data = new byte[header.project_ID_GUID_data_4.length];
        System.arraycopy(header.project_ID_GUID_data_4, 0, data, 0, data.length);
        return data;
    }

    /**
     * @return "Version Major" as an unsigned byte.
     */
    public byte getVersionMajor() {
        return header.version_major;
    }

    /**
     * @return "Version Minor" as an unsigned byte.
     */
    public byte getVersionMinor() {
        return header.version_minor;
    }

    /**
     * @return "System Identifier" as a String.
     */
    public String getSystemIdentifier() {
        return stringFromByteArray(header.system_identifier);
    }

    /**
     * @return "Generating Software" as a String.
     */
    public String getGeneratingSoftware() {
        return stringFromByteArray(header.generating_software);
    }

    /**
     * @return "File Creation Day of Year" as an unsigned short (char).
     */
    public char getFileCreationDayOfYear() {
        return header.file_creation_day;
    }

    /**
     * @return "File Creation Year" as an unsigned short (char).
     */
    public char getFileCreationYear() {
        return header.file_creation_year;
    }

    /**
     * @return "Header Size" as an unsigned short (char).
     */
    public char getHeaderSize() {
        return header.header_size;
    }

    /**
     * @return "Offset to point data" as an unsigned int.
     */
    public int getOffsetToPointData() {
        return header.offset_to_point_data;
    }

    /**
     * @return "Number of Variable Length Records" as an unsigned int.
     */
    public int getNumberOfVariableLengthRecords() {
        return header.number_of_variable_length_records;
    }

    /**
     * @return "Point Data Record Format" as an unsigned byte.
     */
    public byte getPointDataRecordFormat() {
        return header.point_data_format;
    }

    /**
     * @return "Point Data Record Length" as an unsigned short (char).
     */
    public char getPointDataRecordLength() {
        return header.point_data_record_length;
    }

    /**
     * @return "Legacy Number of point records" as an unsigned int.
     */
    public int getLegacyNumberOfPointRecords() {
        return header.number_of_point_records;
    }

    /**
     * @return "Legacy Number of points by return" as an unsigned int array.
     */
    public int[] getLegacyNumberOfPointsByReturn() {
        int[] data = new int[header.number_of_points_by_return.length];
        System.arraycopy(header.number_of_points_by_return, 0, data, 0, data.length);
        return data;
    }

    /**
     * @return "X scale factor" as a double.
     */
    public double getXScaleFactor() {
        return header.x_scale_factor;
    }

    /**
     * @return "Y scale factor" as a double.
     */
    public double getYScaleFactor() {
        return header.y_scale_factor;
    }

    /**
     * @return "Z scale factor" as a double.
     */
    public double getZScaleFactor() {
        return header.z_scale_factor;
    }

    /**
     * @return "X offset" as a double.
     */
    public double getXOffset() {
        return header.x_offset;
    }

    /**
     * @return "Y offset" as a double.
     */
    public double getYOffset() {
        return header.y_offset;
    }

    /**
     * @return "Z offset" as a double.
     */
    public double getZOffset() {
        return header.z_offset;
    }

    /**
     * @return "Max X" as a double.
     */
    public double getMaxX() {
        return header.max_x;
    }

    /**
     * @return "Min X" as a double.
     */
    public double getMinX() {
        return header.min_x;
    }

    /**
     * @return "Max Y" as a double.
     */
    public double getMaxY() {
        return header.max_y;
    }

    /**
     * @return "Min Y" as a double.
     */
    public double getMinY() {
        return header.min_y;
    }

    /**
     * @return "Max Z" as a double.
     */
    public double getMaxZ() {
        return header.max_z;
    }

    /**
     * @return "Min Z" as a double.
     */
    public double getMinZ() {
        return header.min_z;
    }

    /**
     * @return "Start of Waveform Data Packet Record" as an unsigned long.
     */
    public long getStartOfWaveformDataPacketRecord() {
        return header.start_of_waveform_data_packet_record;
    }

    /**
     * @return "Start of first Extended Variable Length Record" as an unsigned long.
     */
    public long getStartOfFirstExtendedVariableLengthRecord() {
        return header.start_of_first_extended_variable_length_record;
    }

    /**
     * @return "Number of Extended Variable Length Records" as an unsigned int.
     */
    public int getNumberOfExtendedVariableLengthRecords() {
        return header.number_of_extended_variable_length_records;
    }

    /**
     * @return "Number of point records" as an unsigned long.
     */
    public long getNumberOfPointRecords() {
        return header.extended_number_of_point_records;
    }

    /**
     * @return "Number of points by return" as an unsigned long array.
     */
    public long[] getNumberOfPointsByReturn() {
        long[] data = new long[header.extended_number_of_points_by_return.length];
        System.arraycopy(header.extended_number_of_points_by_return, 0, data, 0, data.length);
        return data;
    }
}
