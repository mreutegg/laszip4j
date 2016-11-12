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

import com.github.mreutegg.laszip4j.laszip.LASpoint;
import com.github.mreutegg.laszip4j.laszip.MyDefs;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Formatter;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopen;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.ftell;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.clib.Cstring.trim;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwriterTXT extends LASwriter {

    private static final PrintStream stderr = System.err;

    private boolean close_file;
    private PrintStream file;
    private LASheader header;
    private String parse_string;
    private boolean opts;
    private boolean optx;
    private float scale_rgb;
    private char separator_sign;
    private char[] printstring = new char[512];
    private int[] attribute_starts = new int[10];
    private NumberFormat precision2 = DecimalFormat.getInstance();

    boolean refile(PrintStream file)
    {
        this.file = file;
        return TRUE;
    }

    void set_pts(boolean pts)
    {
        this.opts = pts;
    }

    void set_ptx(boolean ptx)
    {
        this.optx = ptx;
    }

    void set_scale_rgb(float scale_rgb)
    {
        this.scale_rgb = scale_rgb;
    }

    boolean open(String file_name, LASheader header, String parse_string, String separator)
    {
        if (file_name == null)
        {
            fprintf(stderr,"ERROR: file name pointer is zero\n");
            return FALSE;
        }


        OutputStream out = fopen(file_name, "w");
        if (out == null)
        {
            fprintf(stderr, "ERROR: cannot open file '%s'\n", file_name);
            return FALSE;
        }
        file = new PrintStream(out);

        close_file = TRUE;

        return open(file, header, parse_string, separator);
    }

    boolean open(PrintStream file, LASheader header, String parse_string, String separator)
    {
        if (file == null)
        {
            fprintf(stderr,"ERROR: file pointer is zero\n");
            return FALSE;
        }

        this.file = file;
        this.header = header;

        this.parse_string = parse_string;

        if (separator != null)
        {
            if (strcmp(separator, "comma") == 0)
            {
                separator_sign = ',';
            }
            else if (strcmp(separator, "tab") == 0)
            {
                separator_sign = '\t';
            }
            else if (strcmp(separator, "dot") == 0 || strcmp(separator, "period") == 0)
            {
                separator_sign = '.';
            }
            else if (strcmp(separator, "colon") == 0)
            {
                separator_sign = ':';
            }
            else if (strcmp(separator, "semicolon") == 0)
            {
                separator_sign = ';';
            }
            else if (strcmp(separator, "hyphen") == 0 || strcmp(separator, "minus") == 0)
            {
                separator_sign = '-';
            }
            else if (strcmp(separator, "space") == 0)
            {
                separator_sign = ' ';
            }
            else
            {
                fprintf(stderr, "ERROR: unknown seperator '%s'\n", separator);
                return FALSE;
            }
        }

        if (opts)
        {
            // look for VRL with PTS or PTX info
            LASvlr ptsVLR = null;
            LASvlr ptxVLR = null;
            if ((ptsVLR = header.get_vlr("LAStools", 2000)) != null || (ptxVLR = header.get_vlr("LAStools", 2001)) != null)
            {
                if ((this.parse_string == null) || (strcmp(this.parse_string, "original") == 0))
                {
                    if (ptsVLR != null && (ptsVLR.record_length_after_header >= 32))
                    {
                        this.parse_string = strdup(ptsVLR.data, 16);
                    }
                    else if (ptxVLR != null && (ptxVLR.record_length_after_header >= 32))
                    {
                        this.parse_string = strdup(ptxVLR.data, 16);
                    }
                    else if (ptsVLR != null)
                    {
                        fprintf(stderr, "WARNING: found VLR for PTS with wrong payload size of %d.\n", ptsVLR.record_length_after_header);
                    }
                    else if (ptxVLR != null)
                    {
                        fprintf(stderr, "WARNING: found VLR for PTX with wrong payload size of %d.\n", ptxVLR.record_length_after_header);
                    }
                }
            }
    else
            {
                fprintf(stderr, "WARNING: found no VLR with PTS or PTX info.\n");
            }
            if (header.version_minor >= 4)
            {
                fprintf(file, "%d       \012", header.extended_number_of_point_records);
            }
            else
            {
                fprintf(file, "%d       \012", header.number_of_point_records);
            }
            if (this.parse_string != null && strcmp(this.parse_string, "xyz") != 0 && strcmp(this.parse_string, "xyzi") != 0 && strcmp(this.parse_string, "xyziRGB") != 0 && strcmp(this.parse_string, "xyzRGB") != 0)
            {
                fprintf(stderr, "WARNING: the parse string for PTS should be 'xyz', 'xyzi', 'xyziRGB', or 'xyzRGB'\n");
            }
            if (separator_sign != ' ')
            {
                fprintf(stderr, "WARNING: the separator for PTS should be 'space' not '%s'\n", separator);
            }
        }
        else if (optx)
        {
            // look for VRL with PTX info
            LASvlr ptxVLR = header.get_vlr("LAStools", 2001);
            if (ptxVLR != null && (ptxVLR.record_length_after_header == 272))
            {
                ByteBuffer payload = ByteBuffer.wrap(ptxVLR.data).order(ByteOrder.LITTLE_ENDIAN);
                if ((this.parse_string == null) || (strcmp(this.parse_string, "original") == 0))
                {
                    this.parse_string = strdup(payload.array(), 16);
                }
                fprintf(file, "%d     \012", payload.getLong(4*8)); // ncols
                fprintf(file, "%d     \012", payload.getLong(5*8)); // nrows
                fprintf(file, "%g %g %g\012", payload.getDouble(6*8), payload.getDouble(7*8), payload.getDouble(8*8)); // translation
                fprintf(file, "%g %g %g\012", payload.getDouble(9*8), payload.getDouble(10*8), payload.getDouble(11*8)); // rotation_row_0
                fprintf(file, "%g %g %g\012", payload.getDouble(12*8), payload.getDouble(13*8), payload.getDouble(14*8)); // rotation_row_1
                fprintf(file, "%g %g %g\012", payload.getDouble(15*8), payload.getDouble(16*8), payload.getDouble(17*8)); // rotation_row_2
                fprintf(file, "%g %g %g %g\012", payload.getDouble(18*8), payload.getDouble(19*8), payload.getDouble(20*8), payload.getDouble(21*8)); // transformation_row_0
                fprintf(file, "%g %g %g %g\012", payload.getDouble(22*8), payload.getDouble(23*8), payload.getDouble(24*8), payload.getDouble(25*8)); // transformation_row_0
                fprintf(file, "%g %g %g %g\012", payload.getDouble(26*8), payload.getDouble(27*8), payload.getDouble(28*8), payload.getDouble(29*8)); // transformation_row_0
                fprintf(file, "%g %g %g %g\012", payload.getDouble(30*8), payload.getDouble(31*8), payload.getDouble(32*8), payload.getDouble(33*8)); // transformation_row_0
            }
            else
            {
                if (ptxVLR != null)
                {
                    fprintf(stderr, "WARNING: found VLR for PTX with wrong payload size of %d.\n", ptxVLR.record_length_after_header);
                }
                else
                {
                    fprintf(stderr, "WARNING: found no VLR with PTX info.\n");
                }
                fprintf(stderr, "         outputting PTS instead ...\n");
                if (header.version_minor >= 4)
                {
                    fprintf(file, "%d       \012", header.extended_number_of_point_records);
                }
                else
                {
                    fprintf(file, "%d       \012", header.number_of_point_records);
                }
            }
            if (this.parse_string != null && strcmp(this.parse_string, "xyz") != 0 && strcmp(this.parse_string, "xyzi") != 0 && strcmp(this.parse_string, "xyziRGB") != 0 && strcmp(this.parse_string, "xyzRGB") != 0)
            {
                fprintf(stderr, "WARNING: the parse string for PTX should be 'xyz', 'xyzi', 'xyziRGB', or 'xyzRGB'\n");
            }
            if (separator_sign != ' ')
            {
                fprintf(stderr, "WARNING: the separator for PTX should be 'space' not '%s'\n", separator);
            }
        }

        if (this.parse_string == null)
        {
            if (header.point_data_format == 1 || header.point_data_format == 4)
            {
                this.parse_string = strdup("xyzt");
            }
            else if (header.point_data_format == 2)
            {
                this.parse_string = strdup("xyzRGB");
            }
            else if (header.point_data_format == 3 || header.point_data_format == 5)
            {
                this.parse_string = strdup("xyztRGB");
            }
            else
            {
                this.parse_string = strdup("xyz");
            }
        }

        return check_parse_string(this.parse_string);
    }

    static void lidardouble2string(char[] string, double value)
    {
        int len;
        len = sprintf(string, "%.15f", value) - 1;
        while (string[len] == '0') len--;
        if (string[len] != '.') len++;
        string[len] = '\0';
    }

    void lidardouble2string(Appendable appendable, double value, double precision) {
        Formatter f = new Formatter(appendable);
        if (precision == 0.1)
            f.format("%.1f", value);
        else if (precision == 0.01)
            try {
                appendable.append(precision2.format(value));
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            //f.format("%.2f", value);
        else if (precision == 0.001)
            f.format("%.3f", value);
        else if (precision == 0.0001)
            f.format("%.4f", value);
        else if (precision == 0.00001)
            f.format("%.5f", value);
        else if (precision == 0.000001)
            f.format("%.6f", value);
        else if (precision == 0.0000001)
            f.format("%.7f", value);
        else if (precision == 0.00000001)
            f.format("%.8f", value);
        else if (precision == 0.000000001)
            f.format("%.9f", value);
        else
            f.format("%.15f", value);
    }

    static void lidardouble2string(char[] string, double value, double precision)
    {
        if (precision == 0.1)
            sprintf(string, "%.1f", value);
        else if (precision == 0.01)
            sprintf(string, "%.2f", value);
        else if (precision == 0.001)
            sprintf(string, "%.3f", value);
        else if (precision == 0.0001)
            sprintf(string, "%.4f", value);
        else if (precision == 0.00001)
            sprintf(string, "%.5f", value);
        else if (precision == 0.000001)
            sprintf(string, "%.6f", value);
        else if (precision == 0.0000001)
            sprintf(string, "%.7f", value);
        else if (precision == 0.00000001)
            sprintf(string, "%.8f", value);
        else if (precision == 0.000000001)
            sprintf(string, "%.9f", value);
        else
            lidardouble2string(string, value);
    }

    private boolean unparse_attribute(LASpoint point, int index)
    {
        if (index >= header.number_attributes)
        {
            return FALSE;
        }
        if (header.attributes.get(index).data_type == 1)
        {
            byte value = point.get_attributeUByte(attribute_starts[index]); // unsigned
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%d", (int)value);
            }
        }
        else if (header.attributes.get(index).data_type == 2)
        {
            byte value = point.get_attributeByte(attribute_starts[index]);
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%d", (int)value);
            }
        }
        else if (header.attributes.get(index).data_type == 3)
        {
            char value = point.get_attributeChar(attribute_starts[index]);
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%d", (int)value);
            }
        }
        else if (header.attributes.get(index).data_type == 4)
        {
            short value = point.get_attributeShort(attribute_starts[index]);
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%d", (int)value);
            }
        }
        else if (header.attributes.get(index).data_type == 5)
        {
            int value = point.get_attributeUInt(attribute_starts[index]); // unsigned
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%d", (int)value);
            }
        }
        else if (header.attributes.get(index).data_type == 6)
        {
            int value = point.get_attributeInt(attribute_starts[index]);
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%d", value);
            }
        }
        else if (header.attributes.get(index).data_type == 9)
        {
            float value = point.get_attributeFloat(attribute_starts[index]);
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%g", value);
            }
        }
        else if (header.attributes.get(index).data_type == 10)
        {
            double value = point.get_attributeDouble(attribute_starts[index]);
            if (header.attributes.get(index).has_scale() || header.attributes.get(index).has_offset())
            {
                double temp_d = header.attributes.get(index).scale[0]*value + header.attributes.get(index).offset[0];
                fprintf(file, "%g", temp_d);
            }
            else
            {
                fprintf(file, "%g", value);
            }
        }
        else
        {
            fprintf(stderr, "WARNING: attribute %d not (yet) implemented.\n", index);
            return FALSE;
        }
        return TRUE;
    }

    public boolean write_point(LASpoint point)
    {
        p_count++;
        int i = 0;
        while (true)
        {
            switch (parse_string.charAt(i))
            {
                case 'x': // the x coordinate
                    lidardouble2string(file, header.get_x(point.get_X()), header.x_scale_factor);
                    break;
                case 'y': // the y coordinate
                    lidardouble2string(file, header.get_y(point.get_Y()), header.y_scale_factor);
                    break;
                case 'z': // the z coordinate
                    lidardouble2string(file, header.get_z(point.get_Z()), header.z_scale_factor);
                    break;
                case 't': // the gps-time
                    fprintf(file, "%.6f", point.get_gps_time());
                    break;
                case 'i': // the intensity
                    if (opts)
                        fprintf(file, "%d", -2048 + point.get_intensity());
                    else if (optx)
                    {
                        int len;
                        len = sprintf(printstring, "%.3f", 1.0f/4095.0f * point.get_intensity()) - 1;
                        while (printstring[len] == '0') len--;
                        if (printstring[len] != '.') len++;
                        printstring[len] = '\0';
                        fprintf(file, "%s", trim(printstring));
                    }
                    else
                        fprintf(file, "%d", (int) point.get_intensity());
                    break;
                case 'a': // the scan angle
                    fprintf(file, "%d", point.get_scan_angle_rank());
                    break;
                case 'r': // the number of the return
                    fprintf(file, "%d", point.get_return_number());
                    break;
                case 'c': // the classification
                    file.print(Byte.toUnsignedInt(point.get_classification()));
                    break;
                case 'u': // the user data
                    fprintf(file, "%d", point.get_user_data());
                    break;
                case 'n': // the number of returns of given pulse
                    fprintf(file, "%d", point.get_number_of_returns());
                    break;
                case 'p': // the point source ID
                    fprintf(file, "%d", (int) point.get_point_source_ID());
                    break;
                case 'e': // the edge of flight line flag
                    fprintf(file, "%d", point.get_edge_of_flight_line());
                    break;
                case 'd': // the direction of scan flag
                    fprintf(file, "%d", point.get_scan_direction_flag());
                    break;
                case 'R': // the red channel of the RGB field
                    if (scale_rgb != 1.0f)
                        fprintf(file, "%.2f", scale_rgb*point.get_rgb()[0]);
                    else
                        fprintf(file, "%d", (int) point.get_rgb()[0]);
                    break;
                case 'G': // the green channel of the RGB field
                    if (scale_rgb != 1.0f)
                        fprintf(file, "%.2f", scale_rgb*point.get_rgb()[1]);
                    else
                        fprintf(file, "%d", (int) point.get_rgb()[1]);
                    break;
                case 'B': // the blue channel of the RGB field
                    if (scale_rgb != 1.0f)
                        fprintf(file, "%.2f", scale_rgb*point.get_rgb()[2]);
                    else
                        fprintf(file, "%d", (int) point.get_rgb()[2]);
                    break;
                case 'm': // the index of the point (count starts at 0)
                    fprintf(file, "%d", p_count-1);
                    break;
                case 'M': // the index of the point (count starts at 1)
                    fprintf(file, "%d", p_count);
                    break;
                case 'w': // the wavepacket descriptor index
                    fprintf(file, "%d", point.wavepacket.getIndex());
                    break;
                case 'W': // all wavepacket attributes
                    fprintf(file, "%d%c%d%c%d%c%g%c%.15g%c%.15g%c%.15g", point.wavepacket.getIndex(), separator_sign, (int)point.wavepacket.getOffset(), separator_sign, point.wavepacket.getSize(), separator_sign, point.wavepacket.getLocation(), separator_sign, point.wavepacket.getXt(), separator_sign, point.wavepacket.getYt(), separator_sign, point.wavepacket.getZt());
                    break;
                case 'X': // the unscaled and unoffset integer X coordinate
                    fprintf(file, "%d", point.get_X());
                    break;
                case 'Y': // the unscaled and unoffset integer Y coordinate
                    fprintf(file, "%d", point.get_Y());
                    break;
                case 'Z': // the unscaled and unoffset integer Z coordinate
                    fprintf(file, "%d", point.get_Z());
                    break;
                default:
                    unparse_attribute(point, (int)(parse_string.charAt(i)-'0'));
            }
            i++;
            if (i < parse_string.length())
            {
                file.print(separator_sign);
            }
            else
            {
                file.print("\012");
                break;
            }
        }
        return TRUE;
    }

    @Override
    public boolean chunk() {
        return false;
    }

    public boolean update_header(LASheader header, boolean use_inventory, boolean update_extra_bytes)
    {
        return TRUE;
    }

    public long close(boolean update_header)
    {
        int bytes = (int) ftell(file);

        if (file != null)
        {
            if (close_file)
            {
                fclose(file);
                close_file = FALSE;
            }
            file = null;
        }
        if (parse_string != null)
        {
            parse_string = null;
        }

        npoints = p_count;
        p_count = 0;

        return bytes;
    }

    LASwriterTXT()
    {
        precision2.setMinimumFractionDigits(2);
        precision2.setMaximumFractionDigits(2);
        precision2.setGroupingUsed(false);
        close_file = FALSE;
        file = null;
        parse_string = null;
        separator_sign = ' ';
        opts = FALSE;
        optx = FALSE;
        scale_rgb = 1.0f;
    }

    private boolean check_parse_string(String parse_string)
    {
        int p = 0;
        while (p < parse_string.length())
        {
            if ((parse_string.charAt(p) != 'x') && // the x coordinate
                    (parse_string.charAt(p) != 'y') && // the y coordinate
                    (parse_string.charAt(p) != 'z') && // the z coordinate
                    (parse_string.charAt(p) != 't') && // the gps time
                    (parse_string.charAt(p) != 'R') && // the red channel of the RGB field
                    (parse_string.charAt(p) != 'G') && // the green channel of the RGB field
                    (parse_string.charAt(p) != 'B') && // the blue channel of the RGB field
                    (parse_string.charAt(p) != 's') && // a string or a number that we don't care about
                    (parse_string.charAt(p) != 'i') && // the intensity
                    (parse_string.charAt(p) != 'a') && // the scan angle
                    (parse_string.charAt(p) != 'n') && // the number of returns of given pulse
                    (parse_string.charAt(p) != 'r') && // the number of the return
                    (parse_string.charAt(p) != 'c') && // the classification
                    (parse_string.charAt(p) != 'u') && // the user data
                    (parse_string.charAt(p) != 'p') && // the point source ID
                    (parse_string.charAt(p) != 'e') && // the edge of flight line flag
                    (parse_string.charAt(p) != 'd') && // the direction of scan flag
                    (parse_string.charAt(p) != 'm') && // the index of the point (count starts at 0)
                    (parse_string.charAt(p) != 'M') && // the index of the point (count starts at 1)
                    (parse_string.charAt(p) != 'w') && // the wavepacket descriptor index
                    (parse_string.charAt(p) != 'W') && // all wavepacket attributes
                    (parse_string.charAt(p) != 'X') && // the unscaled and unoffset integer x coordinate
                    (parse_string.charAt(p) != 'Y') && // the unscaled and unoffset integer y coordinate
                    (parse_string.charAt(p) != 'Z'))   // the unscaled and unoffset integer z coordinate
            {
                if (parse_string.charAt(p) >= '0' && parse_string.charAt(p) <= '9')
                {
                    int index = parse_string.charAt(p) - '0';
                    if (index >= header.number_attributes)
                    {
                        fprintf(stderr, "ERROR: extra bytes attribute '%d' does not exist.\n", index);
                        return FALSE;
                    }
                    attribute_starts[index] = header.get_attribute_start(index);
                }
                else
                {
                    fprintf(stderr, "ERROR: unknown symbol '%c' in parse string. valid are\n", parse_string.charAt(p));
                    fprintf(stderr, "       'x' : the x coordinate\n");
                    fprintf(stderr, "       'y' : the y coordinate\n");
                    fprintf(stderr, "       'z' : the z coordinate\n");
                    fprintf(stderr, "       't' : the gps time\n");
                    fprintf(stderr, "       'R' : the red channel of the RGB field\n");
                    fprintf(stderr, "       'G' : the green channel of the RGB field\n");
                    fprintf(stderr, "       'B' : the blue channel of the RGB field\n");
                    fprintf(stderr, "       's' : a string or a number that we don't care about\n");
                    fprintf(stderr, "       'i' : the intensity\n");
                    fprintf(stderr, "       'a' : the scan angle\n");
                    fprintf(stderr, "       'n' : the number of returns of that given pulse\n");
                    fprintf(stderr, "       'r' : the number of the return\n");
                    fprintf(stderr, "       'c' : the classification\n");
                    fprintf(stderr, "       'u' : the user data\n");
                    fprintf(stderr, "       'p' : the point source ID\n");
                    fprintf(stderr, "       'e' : the edge of flight line flag\n");
                    fprintf(stderr, "       'd' : the direction of scan flag\n");
                    fprintf(stderr, "       'M' : the index of the point\n");
                    fprintf(stderr, "       'w' : the wavepacket descriptor index\n");
                    fprintf(stderr, "       'W' : all wavepacket attributes\n");
                    fprintf(stderr, "       'X' : the unscaled and unoffset integer x coordinate\n");
                    fprintf(stderr, "       'Y' : the unscaled and unoffset integer y coordinate\n");
                    fprintf(stderr, "       'Z' : the unscaled and unoffset integer z coordinate\n");
                    return FALSE;
                }
            }
            p++;
        }
        return TRUE;
    }

    private static String strdup(byte[] data, int offset) {
        byte[] bytes = new byte[data.length - offset];
        System.arraycopy(data, offset, bytes, 0, bytes.length);
        return MyDefs.stringFromByteArray(bytes);
    }

    private static String strdup(String s) {
        return s;
    }
}
