/*
 * Copyright 2005-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.LASattribute;
import com.github.mreutegg.laszip4j.laszip.LASattributer;
import com.github.mreutegg.laszip4j.laszip.LASquantizer;
import com.github.mreutegg.laszip4j.laszip.LASzip;
import com.github.mreutegg.laszip4j.laszip.MyDefs;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I32_QUANTIZE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.sizeof;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASheader extends LASattributer {

    private static final PrintStream stderr = System.err;

    public byte[] file_signature = new byte[4];                  // starts at byte   0
    public char file_source_ID;                      // starts at byte   4
    public char global_encoding;                     // starts at byte   6
    public int project_ID_GUID_data_1;              // starts at byte   8 (unsigned)
    public char project_ID_GUID_data_2;              // starts at byte  12
    public char project_ID_GUID_data_3;              // starts at byte  14
    public byte[] project_ID_GUID_data_4 = new byte[8];            // starts at byte  16
    public byte version_major;                        // starts at byte  24
    public byte version_minor;                        // starts at byte  25
    public byte[] system_identifier = new byte[32];              // starts at byte  26
    public byte[] generating_software = new byte[32];            // starts at byte  58
    public char file_creation_day;                   // starts at byte  90
    public char file_creation_year;                  // starts at byte  92
    public char header_size;                         // starts at byte  94
    public int offset_to_point_data;                // starts at byte  96 (unsigned)
    public int number_of_variable_length_records;   // starts at byte 100 (unsigned)
    public byte point_data_format;                    // starts at byte 104
    public char point_data_record_length;            // starts at byte 105
    public int number_of_point_records;             // starts at byte 107 (unsigned)
    public int[] number_of_points_by_return = new int[5];       // starts at byte 111 (unsigned)
    public double max_x;
    public double min_x;
    public double max_y;
    public double min_y;
    public double max_z;
    public double min_z;

    // LAS 1.3 only
    public long start_of_waveform_data_packet_record; // unsigned

    // LAS 1.4 only
    public long start_of_first_extended_variable_length_record; // unsigned
    public int number_of_extended_variable_length_records; // unsigned
    public long extended_number_of_point_records; // unsigned
    public long[] extended_number_of_points_by_return = new long[15]; // unsigned

    public int user_data_in_header_size; // unsigned
    public byte[] user_data_in_header;

    public LASvlr[] vlrs;
    public LASevlr[] evlrs;
    public LASvlr_geo_keys vlr_geo_keys;
    public LASvlr_key_entry[] vlr_geo_key_entries;
    public double[] vlr_geo_double_params;
    public String vlr_geo_ascii_params;
    public String vlr_geo_wkt_ogc_math;
    public String vlr_geo_wkt_ogc_cs;
    public LASvlr_classification vlr_classification;
    public LASvlr_wave_packet_descr[] vlr_wave_packet_descr;

    public LASzip laszip;
    public LASvlr_lastiling vlr_lastiling;
    public LASvlr_lasoriginal vlr_lasoriginal;

    public int user_data_after_header_size; // unsigned
    public byte[] user_data_after_header;

    LASheader()
    {
        clean_las_header();
    }

    // set functions

    void set_bounding_box(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z) {
        set_bounding_box(min_x, min_y, min_z, max_x, max_y, max_z, true, true);
    }

    void set_bounding_box(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z, boolean auto_scale) {
        set_bounding_box(min_x, min_y, min_z, max_x, max_y, max_z, auto_scale, true);
    }

    void set_bounding_box(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z, boolean auto_scale, boolean auto_offset)
    {
        if (auto_scale)
        {
            if (-360 < min_x  && -360 < min_y && max_x < 360 && max_y < 360)
            {
                x_scale_factor = 0.0000001;
                y_scale_factor = 0.0000001;
            }
            else
            {
                x_scale_factor = 0.01;
                y_scale_factor = 0.01;
            }
            z_scale_factor = 0.01;
        }
        if (auto_offset)
        {
            if (-360 < min_x  && -360 < min_y && max_x < 360 && max_y < 360)
            {
                x_offset = 0;
                y_offset = 0;
                z_offset = 0;
            }
            else
            {
                x_offset = ((int)((min_x + max_x)/200000))*100000;
                y_offset = ((int)((min_y + max_y)/200000))*100000;
                z_offset = ((int)((min_z + max_z)/200000))*100000;
            }
        }
        this.min_x = x_offset + x_scale_factor*I32_QUANTIZE((min_x-x_offset)/x_scale_factor);
        this.min_y = y_offset + y_scale_factor*I32_QUANTIZE((min_y-y_offset)/y_scale_factor);
        this.min_z = z_offset + z_scale_factor*I32_QUANTIZE((min_z-z_offset)/z_scale_factor);
        this.max_x = x_offset + x_scale_factor*I32_QUANTIZE((max_x-x_offset)/x_scale_factor);
        this.max_y = y_offset + y_scale_factor*I32_QUANTIZE((max_y-y_offset)/y_scale_factor);
        this.max_z = z_offset + z_scale_factor*I32_QUANTIZE((max_z-z_offset)/z_scale_factor);
    };

    void set_global_encoding_bit(int bit)
    {
        global_encoding |= (1 << bit);
    }

    boolean get_global_encoding_bit(int bit)
    {
        return (global_encoding & (1 << bit)) != 0;
    }

    // clean functions

    void clean_las_header()
    {
        file_signature[0] = 'L'; file_signature[1] = 'A'; file_signature[2] = 'S'; file_signature[3] = 'F';
        version_major = 1;
        version_minor = 2;
        header_size = 227;
        offset_to_point_data = 227;
        point_data_record_length = 20;
        x_scale_factor = 0.01;
        y_scale_factor = 0.01;
        z_scale_factor = 0.01;
    };

    void clean_user_data_in_header()
    {
        if (user_data_in_header != null)
        {
            header_size -= user_data_in_header_size;
            offset_to_point_data -= user_data_in_header_size;
            user_data_in_header = null;
            user_data_in_header_size = 0;
        }
    };

    void clean_vlrs()
    {
        if (vlrs != null)
        {
            int i;
            for (i = 0; i < number_of_variable_length_records; i++)
            {
                offset_to_point_data -= (54 + vlrs[i].record_length_after_header);
            }
            vlrs = null;
            vlr_geo_keys = null;
            vlr_geo_key_entries = null;
            vlr_geo_double_params = null;
            vlr_geo_ascii_params = null;
            vlr_geo_wkt_ogc_math = null;
            vlr_geo_wkt_ogc_cs = null;
            vlr_classification = null;
            vlr_wave_packet_descr = null;
            number_of_variable_length_records = 0;
        }
    };

    void clean_evlrs()
    {
        if (evlrs != null)
        {
            evlrs = null;
            start_of_first_extended_variable_length_record = 0;
            number_of_extended_variable_length_records = 0;
        }
    };

    void clean_laszip()
    {
        laszip = null;
    };

    void clean_lastiling()
    {
        vlr_lastiling = null;
    };

    void clean_lasoriginal()
    {
        vlr_lasoriginal = null;
    };

    void clean_user_data_after_header()
    {
        if (user_data_after_header != null)
        {
            offset_to_point_data -= user_data_after_header_size;
            user_data_after_header = null;
            user_data_after_header_size = 0;
        }
    };

    void clean()
    {
        clean_user_data_in_header();
        clean_vlrs();
        clean_evlrs();
        clean_laszip();
        clean_lastiling();
        clean_lasoriginal();
        clean_user_data_after_header();
        clean_attributes();
        clean_las_header();
    };

    void unlink()
    {
        user_data_in_header_size = 0;
        user_data_in_header = null;
        vlrs = null;
        number_of_variable_length_records = 0;
        evlrs = null;
        start_of_first_extended_variable_length_record = 0;
        number_of_extended_variable_length_records = 0;
        laszip = null;
        vlr_lastiling = null;
        vlr_lasoriginal = null;
        user_data_after_header_size = 0;
        user_data_after_header = null;
        number_attributes = 0;
        offset_to_point_data = header_size;
    }

    LASheader operatorAssign(LASquantizer quantizer)
    {
        this.x_scale_factor = quantizer.x_scale_factor;
        this.y_scale_factor = quantizer.y_scale_factor;
        this.z_scale_factor = quantizer.z_scale_factor;
        this.x_offset = quantizer.x_offset;
        this.y_offset = quantizer.y_offset;
        this.z_offset = quantizer.z_offset;
        return this;
    };

    boolean check()
    {
        if (strncmp(new String(file_signature), "LASF", 4) != 0)
        {
            fprintf(stderr,"ERROR: wrong file signature '%4s'\n", new String(file_signature));
            return FALSE;
        }
        if ((version_major != 1) || (version_minor > 4))
        {
            fprintf(stderr,"WARNING: unknown version %d.%d (should be 1.0 or 1.1 or 1.2 or 1.3 or 1.4)\n", version_major, version_minor);
        }
        if (header_size < 227)
        {
            fprintf(stderr,"ERROR: header size is %d but should be at least 227\n", header_size);
            return FALSE;
        }
        if (offset_to_point_data < header_size)
        {
            fprintf(stderr,"ERROR: offset to point data %d is smaller than header size %d\n", offset_to_point_data, header_size);
            return FALSE;
        }
        if (x_scale_factor == 0)
        {
            fprintf(stderr,"WARNING: x scale factor is zero.\n");
        }
        if (y_scale_factor == 0)
        {
            fprintf(stderr,"WARNING: y scale factor is zero.\n");
        }
        if (z_scale_factor == 0)
        {
            fprintf(stderr,"WARNING: z scale factor is zero.\n");
        }
        if (max_x < min_x || max_y < min_y || max_z < min_z)
        {
            fprintf(stderr,"WARNING: invalid bounding box [ %g %g %g / %g %g %g ]\n", min_x, min_y, min_z, max_x, max_y, max_z);
        }
        return TRUE;
    };

    boolean is_compressed()
    {
        if (laszip != null)
        {
            if (laszip.compressor != 0)
            {
                return TRUE;
            }
        }
        return FALSE;
    };

    boolean is_lonlat()
    {
        if ((-360.0 <= min_x) && (-90.0 <= min_y) && (max_x <= 360.0) && (max_y <= 90.0))
        {
            return TRUE;
        }
        return FALSE;
    };

    void add_vlr(String user_id, char record_id, char record_length_after_header, byte[] data) {
        add_vlr(user_id, record_id, record_length_after_header, data, false, null, false);
    }

    void add_vlr(String user_id, char record_id, char record_length_after_header, byte[] data, boolean keep_description) {
        add_vlr(user_id, record_id, record_length_after_header, data, keep_description, null, false);
    }

    void add_vlr(String user_id, char record_id, char record_length_after_header, byte[] data, boolean keep_description, String description) {
        add_vlr(user_id, record_id, record_length_after_header, data, keep_description, description, false);
    }

    // note that data needs to be allocated with new [] and not malloc and that LASheader
    // will become the owner over this and manage its deallocation 
    void add_vlr(String user_id, char record_id, char record_length_after_header, byte[] data, boolean keep_description, String description, boolean keep_existing)
    {
        int i = 0;
        boolean found_description = FALSE;
        if (vlrs != null)
        {
            if (keep_existing)
            {
                i = number_of_variable_length_records;
            }
            else
            {
                for (i = 0; i < number_of_variable_length_records; i++)
                {
                    if ((strcmp(vlrs[i].user_id, user_id) == 0) && (vlrs[i].record_id == record_id))
                    {
                        if (vlrs[i].record_length_after_header != 0)
                        {
                            offset_to_point_data -= vlrs[i].record_length_after_header;
                            vlrs[i].data = null;
                        }
                        found_description = TRUE;
                        break;
                    }
                }
            }
            if (i == number_of_variable_length_records)
            {
                number_of_variable_length_records++;
                offset_to_point_data += 54;
                vlrs = realloc(vlrs, number_of_variable_length_records);
            }
        }
        else
        {
            number_of_variable_length_records = 1;
            offset_to_point_data += 54;
            vlrs = new LASvlr[number_of_variable_length_records];
        }
        if (null == vlrs[i])
            vlrs[i] = new LASvlr();
            
        vlrs[i].reserved = 0; // used to be 0xAABB
        vlrs[i].user_id = MyDefs.asByteArray(user_id);
        vlrs[i].record_id = record_id;
        vlrs[i].record_length_after_header = record_length_after_header;
        if (keep_description && found_description)
        {
            // do nothing
        }
        else if (description != null)
        {
            sprintf(vlrs[i].description, "%.31s", description);
        }
        else
        {
            sprintf(vlrs[i].description, "by LAStools of rapidlasso GmbH");
        }
        if (record_length_after_header != 0)
        {
            offset_to_point_data += record_length_after_header;
            vlrs[i].data = data;
        }
        else
        {
            vlrs[i].data = null;
        }
    }

    public LASvlr get_vlr(String user_id, int record_id) {
        return get_vlr(user_id, (char) record_id);
    }

    LASvlr get_vlr(String user_id, char record_id)
    {
        for (int i = 0; i < number_of_variable_length_records; i++)
        {
            if ((strcmp(vlrs[i].user_id, user_id) == 0) && (vlrs[i].record_id == record_id))
            {
                return vlrs[i];
            }
        }
        return null;
    };

    boolean remove_vlr(int i)
    {
        if (vlrs != null)
        {
            if (i < number_of_variable_length_records)
            {
                offset_to_point_data -= (54 + vlrs[i].record_length_after_header);
                number_of_variable_length_records--;
                if (number_of_variable_length_records != 0)
                {
                    vlrs[i] = vlrs[number_of_variable_length_records];
                    vlrs = realloc(vlrs, number_of_variable_length_records);
                }
                else
                {
                    vlrs = null;
                }
            }
            return TRUE;
        }
        return FALSE;
    };

    boolean remove_vlr(String user_id, int record_id) {
        return remove_vlr(user_id, (char) record_id);
    }

    boolean remove_vlr(String user_id, char record_id)
    {
        int i;
        for (i = 0; i < number_of_variable_length_records; i++)
        {
            if ((strcmp(vlrs[i].user_id, user_id) == 0) && (vlrs[i].record_id == record_id))
            {
                return remove_vlr(i);
            }
        }
        return FALSE;
    };

    void set_lastiling(int level, int level_index, int implicit_levels, boolean buffer, boolean reversible, float min_x, float max_x, float min_y, float max_y)
    {
        clean_lastiling();
        vlr_lastiling = new LASvlr_lastiling();
        vlr_lastiling.level = level;
        vlr_lastiling.level_index = level_index;
        vlr_lastiling.implicit_levels = implicit_levels;
        vlr_lastiling.buffer = buffer ? 1 : 0;
        vlr_lastiling.reversible = reversible ? 1 : 0;
        vlr_lastiling.min_x = min_x;
        vlr_lastiling.max_x = max_x;
        vlr_lastiling.min_y = min_y;
        vlr_lastiling.max_y = max_y;
    };

    void set_lasoriginal()
    {
        clean_lasoriginal();
        vlr_lasoriginal = new LASvlr_lasoriginal();
        if (version_minor >= 4)
        {
            vlr_lasoriginal.number_of_point_records = extended_number_of_point_records;
            vlr_lasoriginal.number_of_points_by_return[0] = extended_number_of_points_by_return[0];
            vlr_lasoriginal.number_of_points_by_return[1] = extended_number_of_points_by_return[1];
            vlr_lasoriginal.number_of_points_by_return[2] = extended_number_of_points_by_return[2];
            vlr_lasoriginal.number_of_points_by_return[3] = extended_number_of_points_by_return[3];
            vlr_lasoriginal.number_of_points_by_return[4] = extended_number_of_points_by_return[4];
            vlr_lasoriginal.number_of_points_by_return[5] = extended_number_of_points_by_return[5];
            vlr_lasoriginal.number_of_points_by_return[6] = extended_number_of_points_by_return[6];
            vlr_lasoriginal.number_of_points_by_return[7] = extended_number_of_points_by_return[7];
            vlr_lasoriginal.number_of_points_by_return[8] = extended_number_of_points_by_return[8];
            vlr_lasoriginal.number_of_points_by_return[9] = extended_number_of_points_by_return[9];
            vlr_lasoriginal.number_of_points_by_return[10] = extended_number_of_points_by_return[10];
            vlr_lasoriginal.number_of_points_by_return[11] = extended_number_of_points_by_return[11];
            vlr_lasoriginal.number_of_points_by_return[12] = extended_number_of_points_by_return[12];
            vlr_lasoriginal.number_of_points_by_return[13] = extended_number_of_points_by_return[13];
            vlr_lasoriginal.number_of_points_by_return[14] = extended_number_of_points_by_return[14];
        }
        else
        {
            vlr_lasoriginal.number_of_point_records = number_of_point_records;
            vlr_lasoriginal.number_of_points_by_return[0] = number_of_points_by_return[0];
            vlr_lasoriginal.number_of_points_by_return[1] = number_of_points_by_return[1];
            vlr_lasoriginal.number_of_points_by_return[2] = number_of_points_by_return[2];
            vlr_lasoriginal.number_of_points_by_return[3] = number_of_points_by_return[3];
            vlr_lasoriginal.number_of_points_by_return[4] = number_of_points_by_return[4];
        }
        vlr_lasoriginal.max_x = max_x;
        vlr_lasoriginal.min_x = min_x;
        vlr_lasoriginal.max_y = max_y;
        vlr_lasoriginal.min_y = min_y;
        vlr_lasoriginal.max_z = max_z;
        vlr_lasoriginal.min_z = min_z;
    }

    boolean restore_lasoriginal()
    {
        if (vlr_lasoriginal != null)
        {
            if (version_minor >= 4)
            {
                extended_number_of_point_records = vlr_lasoriginal.number_of_point_records;
                extended_number_of_points_by_return[0] = vlr_lasoriginal.number_of_points_by_return[0];
                extended_number_of_points_by_return[1] = vlr_lasoriginal.number_of_points_by_return[1];
                extended_number_of_points_by_return[2] = vlr_lasoriginal.number_of_points_by_return[2];
                extended_number_of_points_by_return[3] = vlr_lasoriginal.number_of_points_by_return[3];
                extended_number_of_points_by_return[4] = vlr_lasoriginal.number_of_points_by_return[4];
                extended_number_of_points_by_return[5] = vlr_lasoriginal.number_of_points_by_return[5];
                extended_number_of_points_by_return[6] = vlr_lasoriginal.number_of_points_by_return[6];
                extended_number_of_points_by_return[7] = vlr_lasoriginal.number_of_points_by_return[7];
                extended_number_of_points_by_return[8] = vlr_lasoriginal.number_of_points_by_return[8];
                extended_number_of_points_by_return[9] = vlr_lasoriginal.number_of_points_by_return[9];
                extended_number_of_points_by_return[10] = vlr_lasoriginal.number_of_points_by_return[10];
                extended_number_of_points_by_return[11] = vlr_lasoriginal.number_of_points_by_return[11];
                extended_number_of_points_by_return[12] = vlr_lasoriginal.number_of_points_by_return[12];
                extended_number_of_points_by_return[13] = vlr_lasoriginal.number_of_points_by_return[13];
                extended_number_of_points_by_return[14] = vlr_lasoriginal.number_of_points_by_return[14];
            }
            else
            {
                number_of_point_records = (int) vlr_lasoriginal.number_of_point_records;
                number_of_points_by_return[0] = (int) vlr_lasoriginal.number_of_points_by_return[0];
                number_of_points_by_return[1] = (int) vlr_lasoriginal.number_of_points_by_return[1];
                number_of_points_by_return[2] = (int) vlr_lasoriginal.number_of_points_by_return[2];
                number_of_points_by_return[3] = (int) vlr_lasoriginal.number_of_points_by_return[3];
                number_of_points_by_return[4] = (int) vlr_lasoriginal.number_of_points_by_return[4];
            }
            max_x = vlr_lasoriginal.max_x;
            min_x = vlr_lasoriginal.min_x;
            max_y = vlr_lasoriginal.max_y;
            min_y = vlr_lasoriginal.min_y;
            max_z = vlr_lasoriginal.max_z;
            min_z = vlr_lasoriginal.min_z;
            vlr_lasoriginal = null;
            return TRUE;
        }
        return FALSE;
    }

    public void set_geo_keys(int number_of_keys, LASvlr_key_entry[] geo_keys)
    {
        vlr_geo_keys = new LASvlr_geo_keys();
        vlr_geo_keys.key_directory_version = 1;
        vlr_geo_keys.key_revision = 1;
        vlr_geo_keys.minor_revision = 0;
        vlr_geo_keys.number_of_keys = (char) number_of_keys;
        vlr_geo_key_entries = new LASvlr_key_entry[number_of_keys];
        ByteBuffer buffer = ByteBuffer.allocate((number_of_keys + 1) * 8);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        vlr_geo_keys.writeTo(buffer);
        for (int i = 0; i < number_of_keys; i++) {
            vlr_geo_key_entries[i] = new LASvlr_key_entry(geo_keys[i]);
            vlr_geo_key_entries[i].writeTo(buffer);
        }
        add_vlr("LASF_Projection", (char) 34735, (char) buffer.capacity(), buffer.array());
    }

    public void set_geo_double_params(int num_geo_double_params, double[] geo_double_params)
    {
        vlr_geo_double_params = new double[num_geo_double_params];
        System.arraycopy(geo_double_params, 0, vlr_geo_double_params, 0, geo_double_params.length);
        add_vlr("LASF_Projection", (char) 34736, (char) num_geo_double_params, asByteArray(vlr_geo_double_params));
    }

    public void del_geo_double_params()
    {
        if (vlr_geo_double_params != null)
        {
            remove_vlr("LASF_Projection", 34736);
            vlr_geo_double_params = null;
        }
    }

    void set_geo_ascii_params(int num_geo_ascii_params, String geo_ascii_params)
    {
        vlr_geo_ascii_params = geo_ascii_params;
        add_vlr("LASF_Projection", (char) 34737, (char) num_geo_ascii_params, MyDefs.asByteArray(vlr_geo_ascii_params));
    }

    public void del_geo_ascii_params()
    {
        if (vlr_geo_ascii_params != null)
        {
            remove_vlr("LASF_Projection", 34737);
            vlr_geo_ascii_params = null;
        }
    }

    void set_geo_wkt_ogc_math(int num_geo_wkt_ogc_math, String geo_wkt_ogc_math)
    {
        vlr_geo_wkt_ogc_math = geo_wkt_ogc_math;
        add_vlr("LASF_Projection", (char) 2111, (char) num_geo_wkt_ogc_math, MyDefs.asByteArray(vlr_geo_wkt_ogc_math));
    }

    void del_geo_wkt_ogc_math()
    {
        if (vlr_geo_wkt_ogc_math != null)
        {
            remove_vlr("LASF_Projection", 2111);
            vlr_geo_wkt_ogc_math = null;
        }
    }

    void set_geo_wkt_ogc_cs(int num_geo_wkt_ogc_cs, String geo_wkt_ogc_cs)
    {
        vlr_geo_wkt_ogc_cs = geo_wkt_ogc_cs;
        add_vlr("LASF_Projection", (char) 2112, (char) num_geo_wkt_ogc_cs, MyDefs.asByteArray(vlr_geo_wkt_ogc_cs));
    }

    void del_geo_wkt_ogc_cs()
    {
        if (vlr_geo_wkt_ogc_cs != null)
        {
            remove_vlr("LASF_Projection", 2112);
            vlr_geo_wkt_ogc_cs = null;
        }
    }

    void update_extra_bytes_vlr() {
        update_extra_bytes_vlr(false);
    }

    void update_extra_bytes_vlr(boolean keep_description)
    {
        if (number_attributes != 0)
        {
            char record_length_after_header = (char) (sizeof(LASattribute.class)*number_attributes);
            byte[] data = new byte[record_length_after_header];
            ByteBuffer bb = ByteBuffer.wrap(data);
            bb.order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < number_attributes; i++) {
                bb.position(i * sizeof(LASattribute.class));
                bb.put(attributes.get(i).asByteArray());
            }
            add_vlr("LASF_Spec", (char) 4, record_length_after_header, data, keep_description);
        }
        else
        {
            remove_vlr("LASF_Spec", 4);
        }
    }

    private static LASvlr[] realloc(LASvlr[] vlrs, int size) {
        int numElements = Math.min(vlrs.length, size);
        LASvlr[] newVlrs = new LASvlr[size];
        System.arraycopy(vlrs, 0, newVlrs, 0, numElements);
        return newVlrs;
    }

    private static byte[] asByteArray(double[] values) {
        ByteBuffer bb = ByteBuffer.allocate(values.length * 8);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (double d : values) {
            bb.putDouble(d);
        }
        return bb.array();
    }

    public static double[] doublesFromByteArray(byte[] data) {
        double[] values = new double[data.length / 8];
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int i = 0;
        while (bb.hasRemaining()) {
            values[i++] = bb.getDouble();
        }
        return values;
    }
}
