/*
 * Copyright 2005-2013, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.LASindex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenR;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atof;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atoi;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_ASC;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_BIL;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_BIN;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_DTM;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAS;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAZ;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_QFIT;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_SHP;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_TXT;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_IO_IBUFFER_SIZE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASreadOpener {

    private static final PrintStream stderr = System.err;

    private int io_ibuffer_size;
    private final List<String> file_names = new ArrayList<>();
    private String file_name;
    private boolean merged;
    private int file_name_current;
    private float buffer_size;
    private String temp_file_base;
    private final List<String> neighbor_file_names = new ArrayList<>();
    private boolean comma_not_point;
    private double[] scale_factor;
    private double[] offset;
    private boolean auto_reoffset;
    private boolean files_are_flightlines;
    private boolean apply_file_source_ID;
    private boolean itxt;
    private boolean ipts;
    private boolean iptx;
    private float translate_intensity;
    private float scale_intensity;
    private float translate_scan_angle;
    private float scale_scan_angle;
    private int number_attributes;
    private int[] attribute_data_types = new int[10];
    private String[] attribute_names = new String[10];
    private String[] attribute_descriptions = new String[10];
    private double[] attribute_scales = new double[10];
    private double[] attribute_offsets = new double[10];
    private double[] attribute_pre_scales = new double[10];
    private double[] attribute_pre_offsets = new double[10];
    private String parse_string;
    private int skip_lines;
    private boolean populate_header;
    private boolean keep_lastiling;
    private boolean pipe_on;
    private boolean use_stdin;
    private boolean unique;

    // optional extras
    private LASindex index;
    private LASfilter filter;
    private LAStransform transform;

    // optional area-of-interest query (spatially indexed) 
    private float[] inside_tile;
    private double[] inside_circle;
    private double[] inside_rectangle;

    boolean is_piped()
    {
        return (file_names.isEmpty() && use_stdin);
    }

    boolean is_inside()
    {
        return (inside_tile != null || inside_circle != null || inside_rectangle != null);
    }

    int unparse(char[] string)
    {
        int n = 0;
        if (inside_tile != null)
        {
            n = sprintf(string, "-inside_tile %g %g %g ", inside_tile[0], inside_tile[1], inside_tile[2]);
        }
        else if (inside_circle != null)
        {
            n = sprintf(string, "-inside_circle %lf %lf %lf ", inside_circle[0], inside_circle[1], inside_circle[2]);
        }
        else if (inside_rectangle != null)
        {
            n = sprintf(string, "-inside_rectangle %lf %lf %lf %lf ", inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
        }
        if (apply_file_source_ID)
        {
            n += sprintf(string, n, "-apply_file_source_ID ");
        }
        if (scale_factor != null)
        {
            if (scale_factor[2] == 0.0)
            {
                if ((scale_factor[0] != 0.0) && (scale_factor[1] != 0.0))
                {
                    n += sprintf(string, n, "-rescale_xy %g %g ", scale_factor[0], scale_factor[1]);
                }
            }
            else
            {
                if ((scale_factor[0] == 0.0) && (scale_factor[1] == 0.0))
                {
                    n += sprintf(string, n, "-rescale_z %g ", scale_factor[2]);
                }
                else
                {
                    n += sprintf(string, n, "-rescale %g %g %g ", scale_factor[0], scale_factor[1], scale_factor[2]);
                }
            }
        }
        if (offset != null)
        {
            n += sprintf(string, n, "-reoffset %g %g %g ", offset[0], offset[1], offset[2]);
        }
        else if (auto_reoffset)
        {
            n += sprintf(string, n, "-auto_reoffset ");
        }
        if (populate_header)
        {
            n += sprintf(string, n, "-populate ");
        }
        if (io_ibuffer_size != LAS_TOOLS_IO_IBUFFER_SIZE)
        {
            n += sprintf(string, n, "-io_ibuffer %d ", io_ibuffer_size);
        }
        if (temp_file_base != null)
        {
            n += sprintf(string, n, "-temp_files \"%s\" ", temp_file_base);
        }
        return n;
    }

    boolean is_buffered()
    {
        return ((buffer_size > 0) && ((file_names.size() > 1) || (neighbor_file_names.size() > 0)));
    }

    public boolean is_header_populated()
    {
        return (populate_header || (file_name != null && (strstr(file_name, ".las") || strstr(file_name, ".laz") || strstr(file_name, ".LAS") || strstr(file_name, ".LAZ"))));
    }

    void reset()
    {
        file_name_current = 0;
        file_name = null;
    }

    public LASreader open() {
        return open(null, true);
    }

    public LASreader open(String other_file_name) {
        return open(other_file_name, true);
    }

    public LASreader open(String other_file_name, boolean reset_after_other)
    {
        if (filter != null) filter.reset();
        if (transform != null) transform.reset();

        if (!file_names.isEmpty() || other_file_name != null)
        {
            use_stdin = FALSE;
            if ((file_names.size() > 1) && merged)
            {
                LASreaderMerged lasreadermerged = new LASreaderMerged();
                lasreadermerged.set_scale_factor(scale_factor);
                lasreadermerged.set_offset(offset);
                lasreadermerged.set_parse_string(parse_string);
                lasreadermerged.set_skip_lines(skip_lines);
                lasreadermerged.set_populate_header(populate_header);
                lasreadermerged.set_keep_lastiling(keep_lastiling);
                lasreadermerged.set_translate_intensity(translate_intensity);
                lasreadermerged.set_scale_intensity(scale_intensity);
                lasreadermerged.set_translate_scan_angle(translate_scan_angle);
                lasreadermerged.set_scale_scan_angle(scale_scan_angle);
                lasreadermerged.set_io_ibuffer_size(io_ibuffer_size);
                for (file_name_current = 0; file_name_current < file_names.size(); file_name_current++) lasreadermerged.add_file_name(file_names.get(file_name_current));
                if (!lasreadermerged.open())
                {
                    fprintf(stderr, "ERROR: cannot open lasreadermerged with %d file names\n", file_names.size());
                    return null;
                }
                if (files_are_flightlines) lasreadermerged.set_files_are_flightlines(TRUE);
                if (apply_file_source_ID) lasreadermerged.set_apply_file_source_ID(TRUE);
                if (filter != null) lasreadermerged.set_filter(filter);
                if (transform != null) lasreadermerged.set_transform(transform);
                if (inside_tile != null) lasreadermerged.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                if (inside_circle != null) lasreadermerged.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                if (inside_rectangle != null) lasreadermerged.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                if (pipe_on)
                {
                    LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                    if (!lasreaderpipeon.open(lasreadermerged))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreadermerged\n");
                        return null;
                    }
                    return lasreaderpipeon;
                }
                else
                {
                    return lasreadermerged;
                }
            }
            else if ((buffer_size > 0) && ((file_names.size() > 1) || (neighbor_file_names.size() > 0)))
            {
                int i;
                if (other_file_name != null)
                {
                    file_name = other_file_name;
                    if (reset_after_other)
                    {
                        file_name_current = 0;
                    }
                }
                else
                {
                    file_name = file_names.get(file_name_current);
                    file_name_current++;
                }
                LASreaderBuffered lasreaderbuffered = new LASreaderBuffered();
                lasreaderbuffered.set_buffer_size(buffer_size);
                lasreaderbuffered.set_scale_factor(scale_factor);
                lasreaderbuffered.set_offset(offset);
                lasreaderbuffered.set_parse_string(parse_string);
                lasreaderbuffered.set_skip_lines(skip_lines);
                lasreaderbuffered.set_populate_header(populate_header);
                lasreaderbuffered.set_translate_intensity(translate_intensity);
                lasreaderbuffered.set_scale_intensity(scale_intensity);
                lasreaderbuffered.set_translate_scan_angle(translate_scan_angle);
                lasreaderbuffered.set_scale_scan_angle(scale_scan_angle);
                lasreaderbuffered.set_file_name(file_name);
                for (i = 0; i < file_names.size(); i++)
                {
                    if (!file_name.equals(file_names.get(i)))
                    {
                        lasreaderbuffered.add_neighbor_file_name(file_names.get(i));
                    }
                }
                for (i = 0; i < neighbor_file_names.size(); i++)
                {
                    if (strcmp(file_name, neighbor_file_names.get(i)) != 0)
                    {
                        lasreaderbuffered.add_neighbor_file_name(neighbor_file_names.get(i));
                    }
                }
                if (filter != null) lasreaderbuffered.set_filter(filter);
                if (transform != null) lasreaderbuffered.set_transform(transform);
                if (!lasreaderbuffered.open())
                {
                    fprintf(stderr,"ERROR: cannot open lasreaderbuffered with %d file names\n", file_names.size()+neighbor_file_names.size());
                    return null;
                }
                if (inside_tile != null) lasreaderbuffered.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                if (inside_circle != null) lasreaderbuffered.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                if (inside_rectangle != null) lasreaderbuffered.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                if (pipe_on)
                {
                    LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                    if (!lasreaderpipeon.open(lasreaderbuffered))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderbuffered\n");
                        return null;
                    }
                    return lasreaderpipeon;
                }
                else
                {
                    return lasreaderbuffered;
                }
            }
            else
            {
                if (other_file_name != null)
                {
                    file_name = other_file_name;
                    if (reset_after_other)
                    {
                        file_name_current = 0;
                    }
                }
                else
                {
                    file_name = file_names.get(file_name_current);
                    file_name_current++;
                }
                if (files_are_flightlines)
                {
                    transform.setPointSource(file_name_current);
                }
                if (strstr(file_name, ".las") || strstr(file_name, ".laz") || strstr(file_name, ".LAS") || strstr(file_name, ".LAZ"))
                {
                    LASreaderLAS lasreaderlas;
                    if (scale_factor == null && offset == null)
                    {
                        if (auto_reoffset)
                            lasreaderlas = new LASreaderLASreoffset();
                        else
                            lasreaderlas = new LASreaderLAS();
                    }
                    else if (scale_factor != null && offset == null)
                    {
                        if (auto_reoffset)
                            lasreaderlas = new LASreaderLASrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2]);
                        else
                            lasreaderlas = new LASreaderLASrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    }
                    else if (scale_factor == null && offset != null)
                        lasreaderlas = new LASreaderLASreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreaderlas = new LASreaderLASrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreaderlas.open(file_name, io_ibuffer_size))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderlas with file name '%s'\n", file_name);
                        return null;
                    }
                    LASindex index = new LASindex();
                    if (index.read(file_name))
                        lasreaderlas.set_index(index);
                    if (files_are_flightlines)
                    {
                        lasreaderlas.header.file_source_ID = (char) file_name_current;
                    }
                    else if (apply_file_source_ID)
                    {
                        transform.setPointSource(lasreaderlas.header.file_source_ID);
                    }
                    if (filter != null) lasreaderlas.set_filter(filter);
                    if (transform != null) lasreaderlas.set_transform(transform);
                    if (inside_tile != null) lasreaderlas.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreaderlas.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreaderlas.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreaderlas))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderlas\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreaderlas;
                    }
                }
                else if (strstr(file_name, ".bin") || strstr(file_name, ".BIN"))
                {
                    LASreaderBIN lasreaderbin;
                    if (scale_factor == null && offset == null)
                        lasreaderbin = new LASreaderBIN();
                    else if (scale_factor != null && offset == null)
                        lasreaderbin = new LASreaderBINrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    else if (scale_factor == null && offset != null)
                        lasreaderbin = new LASreaderBINreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreaderbin = new LASreaderBINrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreaderbin.open(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderbin with file name '%s'\n", file_name);
                        return null;
                    }
                    LASindex index = new LASindex();
                    if (index.read(file_name))
                        lasreaderbin.set_index(index);
                    if (files_are_flightlines) lasreaderbin.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreaderbin.set_filter(filter);
                    if (transform != null) lasreaderbin.set_transform(transform);
                    if (inside_tile != null) lasreaderbin.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreaderbin.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreaderbin.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreaderbin))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderbin\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreaderbin;
                    }
                }
                else if (strstr(file_name, ".shp") || strstr(file_name, ".SHP"))
                {
                    LASreaderSHP lasreadershp;
                    if (scale_factor == null && offset == null)
                        lasreadershp = new LASreaderSHP();
                    else if (scale_factor != null && offset == null)
                        lasreadershp = new LASreaderSHPrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    else if (scale_factor == null && offset != null)
                        lasreadershp = new LASreaderSHPreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreadershp = new LASreaderSHPrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreadershp.open(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreadershp with file name '%s'\n", file_name);
                        return null;
                    }
                    if (files_are_flightlines) lasreadershp.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreadershp.set_filter(filter);
                    if (transform != null) lasreadershp.set_transform(transform);
                    if (inside_tile != null) lasreadershp.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreadershp.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreadershp.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreadershp))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreadershp\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreadershp;
                    }
                }
                else if (strstr(file_name, ".qi") || strstr(file_name, ".QI"))
                {
                    LASreaderQFIT lasreaderqfit;
                    if (scale_factor == null && offset == null)
                        lasreaderqfit = new LASreaderQFIT();
                    else if (scale_factor != null && offset == null)
                        lasreaderqfit = new LASreaderQFITrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    else if (scale_factor == null && offset != null)
                        lasreaderqfit = new LASreaderQFITreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreaderqfit = new LASreaderQFITrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreaderqfit.open(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderqfit with file name '%s'\n", file_name);
                        return null;
                    }
                    LASindex index = new LASindex();
                    if (index.read(file_name))
                        lasreaderqfit.set_index(index);
                    if (files_are_flightlines) lasreaderqfit.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreaderqfit.set_filter(filter);
                    if (transform != null) lasreaderqfit.set_transform(transform);
                    if (inside_tile != null) lasreaderqfit.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreaderqfit.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreaderqfit.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreaderqfit))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderqfit\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreaderqfit;
                    }
                }
                else if (strstr(file_name, ".asc") || strstr(file_name, ".ASC"))
                {
                    LASreaderASC lasreaderasc;
                    if (scale_factor == null && offset == null)
                        lasreaderasc = new LASreaderASC();
                    else if (scale_factor != null && offset == null)
                        lasreaderasc = new LASreaderASCrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    else if (scale_factor == null && offset != null)
                        lasreaderasc = new LASreaderASCreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreaderasc = new LASreaderASCrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreaderasc.open(file_name, comma_not_point))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderasc with file name '%s'\n", file_name);
                        return null;
                    }
                    if (files_are_flightlines) lasreaderasc.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreaderasc.set_filter(filter);
                    if (transform != null) lasreaderasc.set_transform(transform);
                    if (inside_tile != null) lasreaderasc.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreaderasc.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreaderasc.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreaderasc))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderasc\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreaderasc;
                    }
                }
                else if (strstr(file_name, ".bil") || strstr(file_name, ".BIL"))
                {
                    LASreaderBIL lasreaderbil;
                    if (scale_factor == null && offset == null)
                        lasreaderbil = new LASreaderBIL();
                    else if (scale_factor != null && offset == null)
                        lasreaderbil = new LASreaderBILrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    else if (scale_factor == null && offset != null)
                        lasreaderbil = new LASreaderBILreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreaderbil = new LASreaderBILrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreaderbil.open(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderbil with file name '%s'\n", file_name);
                        return null;
                    }
                    if (files_are_flightlines) lasreaderbil.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreaderbil.set_filter(filter);
                    if (transform != null) lasreaderbil.set_transform(transform);
                    if (inside_tile != null) lasreaderbil.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreaderbil.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreaderbil.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreaderbil))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderbil\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreaderbil;
                    }
                }
                else if (strstr(file_name, ".dtm") || strstr(file_name, ".DTM"))
                {
                    LASreaderDTM lasreaderdtm;
                    if (scale_factor == null && offset == null)
                        lasreaderdtm = new LASreaderDTM();
                    else if (scale_factor != null && offset == null)
                        lasreaderdtm = new LASreaderDTMrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                    else if (scale_factor == null && offset != null)
                        lasreaderdtm = new LASreaderDTMreoffset(offset[0], offset[1], offset[2]);
                    else
                        lasreaderdtm = new LASreaderDTMrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                    if (!lasreaderdtm.open(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderdtm with file name '%s'\n", file_name);
                        return null;
                    }
                    if (files_are_flightlines) lasreaderdtm.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreaderdtm.set_filter(filter);
                    if (transform != null) lasreaderdtm.set_transform(transform);
                    if (inside_tile != null) lasreaderdtm.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreaderdtm.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreaderdtm.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreaderdtm))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderdtm\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreaderdtm;
                    }
                }
                else
                {
                    LASreaderTXT lasreadertxt = new LASreaderTXT();
                    if (ipts) lasreadertxt.set_pts(TRUE);
                    else if (iptx) lasreadertxt.set_ptx(TRUE);
                    if (translate_intensity != 0.0f) lasreadertxt.set_translate_intensity(translate_intensity);
                    if (scale_intensity != 1.0f) lasreadertxt.set_scale_intensity(scale_intensity);
                    if (translate_scan_angle != 0.0f) lasreadertxt.set_translate_scan_angle(translate_scan_angle);
                    if (scale_scan_angle != 1.0f) lasreadertxt.set_scale_scan_angle(scale_scan_angle);
                    lasreadertxt.set_scale_factor(scale_factor);
                    lasreadertxt.set_offset(offset);
                    if (number_attributes != 0)
                    {
                        for (int i = 0; i < number_attributes; i++)
                        {
                            lasreadertxt.add_attribute(attribute_data_types[i], attribute_names[i], attribute_descriptions[i], attribute_scales[i], attribute_offsets[i], attribute_pre_scales[i], attribute_pre_offsets[i]);
                        }
                    }
                    if (!lasreadertxt.open(file_name, parse_string, skip_lines, populate_header))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreadertxt with file name '%s'\n", file_name);
                        return null;
                    }
                    if (files_are_flightlines) lasreadertxt.header.file_source_ID = (char) file_name_current;
                    if (filter != null) lasreadertxt.set_filter(filter);
                    if (transform != null) lasreadertxt.set_transform(transform);
                    if (inside_tile != null) lasreadertxt.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    if (inside_circle != null) lasreadertxt.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    if (inside_rectangle != null) lasreadertxt.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    if (pipe_on)
                    {
                        LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                        if (!lasreaderpipeon.open(lasreadertxt))
                        {
                            fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreadertxt\n");
                            return null;
                        }
                        return lasreaderpipeon;
                    }
                    else
                    {
                        return lasreadertxt;
                    }
                }
            }
        }
        else if (use_stdin)
        {
            use_stdin = FALSE; populate_header = TRUE;
            if (itxt)
            {
                LASreaderTXT lasreadertxt = new LASreaderTXT();
                if (ipts) lasreadertxt.set_pts(TRUE);
                else if (iptx) lasreadertxt.set_ptx(TRUE);
                if (translate_intensity != 0.0f) lasreadertxt.set_translate_intensity(translate_intensity);
                if (scale_intensity != 1.0f) lasreadertxt.set_scale_intensity(scale_intensity);
                if (translate_scan_angle != 0.0f) lasreadertxt.set_translate_scan_angle(translate_scan_angle);
                if (scale_scan_angle != 1.0f) lasreadertxt.set_scale_scan_angle(scale_scan_angle);
                lasreadertxt.set_scale_factor(scale_factor);
                lasreadertxt.set_offset(offset);
                if (number_attributes != 0)
                {
                    for (int i = 0; i < number_attributes; i++)
                    {
                        lasreadertxt.add_attribute(attribute_data_types[i], attribute_names[i], attribute_descriptions[i], attribute_scales[i], attribute_offsets[i], attribute_pre_scales[i], attribute_pre_offsets[i]);
                    }
                }
                if (!lasreadertxt.open(System.in, 0, parse_string, skip_lines, FALSE))
                {
                    fprintf(stderr,"ERROR: cannot open lasreadertxt with file name '%s'\n", file_name);
                    return null;
                }
                if (files_are_flightlines) lasreadertxt.header.file_source_ID = (char) file_name_current;
                if (filter != null) lasreadertxt.set_filter(filter);
                if (transform != null) lasreadertxt.set_transform(transform);
                if (inside_tile != null) lasreadertxt.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                if (inside_circle != null) lasreadertxt.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                if (inside_rectangle != null) lasreadertxt.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                if (pipe_on)
                {
                    LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                    if (!lasreaderpipeon.open(lasreadertxt))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreadertxt\n");
                        return null;
                    }
                    return lasreaderpipeon;
                }
                else
                {
                    return lasreadertxt;
                }
            }
            else
            {
                LASreaderLAS lasreaderlas;
                if (scale_factor == null && offset == null)
                    lasreaderlas = new LASreaderLAS();
                else if (scale_factor != null && offset == null)
                    lasreaderlas = new LASreaderLASrescale(scale_factor[0], scale_factor[1], scale_factor[2]);
                else if (scale_factor == null && offset != null)
                    lasreaderlas = new LASreaderLASreoffset(offset[0], offset[1], offset[2]);
                else
                    lasreaderlas = new LASreaderLASrescalereoffset(scale_factor[0], scale_factor[1], scale_factor[2], offset[0], offset[1], offset[2]);
                if (!lasreaderlas.open(System.in))
                {
                    fprintf(stderr,"ERROR: cannot open lasreaderlas from stdin \n");
                    return null;
                }
                if (filter != null) lasreaderlas.set_filter(filter);
                if (transform != null) lasreaderlas.set_transform(transform);
                if (inside_tile != null) lasreaderlas.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                if (inside_circle != null) lasreaderlas.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                if (inside_rectangle != null) lasreaderlas.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                if (pipe_on)
                {
                    LASreaderPipeOn lasreaderpipeon = new LASreaderPipeOn();
                    if (!lasreaderpipeon.open(lasreaderlas))
                    {
                        fprintf(stderr,"ERROR: cannot open lasreaderpipeon with lasreaderlas from stdin\n");
                        return null;
                    }
                    return lasreaderpipeon;
                }
                else
                {
                    return lasreaderlas;
                }
            }
        }
        else
        {
            return null;
        }
    }

    boolean reopen(LASreader lasreader, boolean remain_buffered)
    {

        if (lasreader == null)
        {
            fprintf(stderr,"ERROR: pointer to LASreader is NULL\n");
        }

        // make sure the LASreader was closed

        lasreader.close();

        if (filter != null) filter.reset();
        if (transform != null) transform.reset();

        if (pipe_on)
        {
            LASreaderPipeOn lasreaderpipeon = (LASreaderPipeOn)lasreader;
            lasreaderpipeon.p_count = 0;
            lasreader = lasreaderpipeon.get_lasreader();
        }

        if (!file_names.isEmpty())
        {
            if ((file_names.size() > 1) && merged)
            {
                LASreaderMerged lasreadermerged = (LASreaderMerged)lasreader;
                if (!lasreadermerged.reopen())
                {
                    fprintf(stderr,"ERROR: cannot reopen lasreadermerged\n");
                    return FALSE;
                }
                if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                {
                    lasreadermerged.inside_none();
                    if (inside_rectangle != null) lasreadermerged.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    else if (inside_tile != null) lasreadermerged.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    else lasreadermerged.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                }
                return TRUE;
            }
            else if ((buffer_size > 0) && ((file_names.size() > 1) || (neighbor_file_names.size() > 0)))
            {
                LASreaderBuffered lasreaderbuffered = (LASreaderBuffered)lasreader;
                if (!lasreaderbuffered.reopen())
                {
                    fprintf(stderr,"ERROR: cannot reopen lasreaderbuffered\n");
                    return FALSE;
                }
                if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                {
                    lasreaderbuffered.inside_none();
                    if (inside_rectangle != null) lasreaderbuffered.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                    else if (inside_tile != null) lasreaderbuffered.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                    else lasreaderbuffered.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                }
                if (!remain_buffered) lasreaderbuffered.remove_buffer();
                return TRUE;
            }
            else
            {
                if (file_name == null) return FALSE;
                if (strstr(file_name, ".las") || strstr(file_name, ".laz") || strstr(file_name, ".LAS") || strstr(file_name, ".LAZ"))
                {
                    LASreaderLAS lasreaderlas = (LASreaderLAS)lasreader;
                    if (!lasreaderlas.open(file_name, io_ibuffer_size))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreaderlas with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (!remain_buffered)
                    {
                        if (lasreaderlas.header.vlr_lasoriginal != null) lasreaderlas.npoints = lasreaderlas.header.vlr_lasoriginal.number_of_point_records;
                        lasreaderlas.header.restore_lasoriginal();
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreaderlas.inside_none();
                        if (inside_rectangle != null) lasreaderlas.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreaderlas.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreaderlas.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else if (strstr(file_name, ".bin") || strstr(file_name, ".BIN"))
                {
                    LASreaderBIN lasreaderbin = (LASreaderBIN)lasreader;
                    if (!lasreaderbin.open(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreaderbin with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreaderbin.inside_none();
                        if (inside_rectangle != null) lasreaderbin.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreaderbin.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreaderbin.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else if (strstr(file_name, ".shp") || strstr(file_name, ".SHP"))
                {
                    LASreaderSHP lasreadershp = (LASreaderSHP)lasreader;
                    if (!lasreadershp.reopen(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreadershp with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreadershp.inside_none();
                        if (inside_rectangle != null) lasreadershp.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreadershp.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreadershp.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else if (strstr(file_name, ".qi") || strstr(file_name, ".QI"))
                {
                    LASreaderQFIT lasreaderqfit = (LASreaderQFIT)lasreader;
                    if (!lasreaderqfit.reopen(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreaderqfit with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreaderqfit.inside_none();
                        if (inside_rectangle != null) lasreaderqfit.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreaderqfit.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreaderqfit.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else if (strstr(file_name, ".asc") || strstr(file_name, ".ASC"))
                {
                    LASreaderASC lasreaderasc = (LASreaderASC)lasreader;
                    if (!lasreaderasc.reopen(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreaderasc with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreaderasc.inside_none();
                        if (inside_rectangle != null) lasreaderasc.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreaderasc.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreaderasc.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else if (strstr(file_name, ".bil") || strstr(file_name, ".BIL"))
                {
                    LASreaderBIL lasreaderbil = (LASreaderBIL)lasreader;
                    if (!lasreaderbil.reopen(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreaderbil with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreaderbil.inside_none();
                        if (inside_rectangle != null) lasreaderbil.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreaderbil.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreaderbil.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else if (strstr(file_name, ".dtm") || strstr(file_name, ".DTM"))
                {
                    LASreaderDTM lasreaderdtm = (LASreaderDTM)lasreader;
                    if (!lasreaderdtm.reopen(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreaderdtm with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreaderdtm.inside_none();
                        if (inside_rectangle != null) lasreaderdtm.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreaderdtm.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreaderdtm.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
                else
                {
                    LASreaderTXT lasreadertxt = (LASreaderTXT)lasreader;
                    if (!lasreadertxt.reopen(file_name))
                    {
                        fprintf(stderr,"ERROR: cannot reopen lasreadertxt with file name '%s'\n", file_name);
                        return FALSE;
                    }
                    if (inside_rectangle != null || inside_tile != null || inside_circle != null)
                    {
                        lasreadertxt.inside_none();
                        if (inside_rectangle != null) lasreadertxt.inside_rectangle(inside_rectangle[0], inside_rectangle[1], inside_rectangle[2], inside_rectangle[3]);
                        else if (inside_tile != null) lasreadertxt.inside_tile(inside_tile[0], inside_tile[1], inside_tile[2]);
                        else lasreadertxt.inside_circle(inside_circle[0], inside_circle[1], inside_circle[2]);
                    }
                    return TRUE;
                }
            }
        }
        else
        {
            fprintf(stderr,"ERROR: no lasreader input specified\n");
            return FALSE;
        }
    }

    public LASwaveform13reader open_waveform13(LASheader lasheader)
    {
        if (lasheader.point_data_format < 4) return null;
        if ((lasheader.point_data_format > 5) && (lasheader.point_data_format < 9)) return null;
        if (lasheader.vlr_wave_packet_descr == null) return null;
        if (get_file_name() == null) return null;
        LASwaveform13reader waveform13reader = new LASwaveform13reader();
        if ((lasheader.global_encoding & 2) != 0 && (lasheader.start_of_waveform_data_packet_record > lasheader.offset_to_point_data))
        {
            if (waveform13reader.open(get_file_name(), lasheader.start_of_waveform_data_packet_record, lasheader.vlr_wave_packet_descr))
            {
                return waveform13reader;
            }
        }
        else
        {
            if (waveform13reader.open(get_file_name(), 0, lasheader.vlr_wave_packet_descr))
            {
                return waveform13reader;
            }
        }
        return null;
    }

    static void usage()
    {
        fprintf(stderr,"Supported LAS Inputs\n");
        fprintf(stderr,"  -i lidar.las\n");
        fprintf(stderr,"  -i lidar.laz\n");
        fprintf(stderr,"  -i lidar1.las lidar2.las lidar3.las -merged\n");
        fprintf(stderr,"  -i *.las - merged\n");
        fprintf(stderr,"  -i flight0??.laz flight1??.laz\n");
        fprintf(stderr,"  -i terrasolid.bin\n");
        fprintf(stderr,"  -i esri.shp\n");
        fprintf(stderr,"  -i nasa.qi\n");
        fprintf(stderr,"  -i lidar.txt -iparse xyzti -iskip 2 (on-the-fly from ASCII)\n");
        fprintf(stderr,"  -i lidar.txt -iparse xyzi -itranslate_intensity 1024\n");
        fprintf(stderr,"  -lof file_list.txt\n");
        fprintf(stderr,"  -stdin (pipe from stdin)\n");
        fprintf(stderr,"  -rescale 0.01 0.01 0.001\n");
        fprintf(stderr,"  -rescale_xy 0.01 0.01\n");
        fprintf(stderr,"  -rescale_z 0.01\n");
        fprintf(stderr,"  -reoffset 600000 4000000 0\n");
        fprintf(stderr,"Fast AOI Queries for LAS/LAZ with spatial indexing LAX files\n");
        fprintf(stderr,"  -inside min_x min_y max_x max_y\n");
        fprintf(stderr,"  -inside_tile ll_x ll_y size\n");
        fprintf(stderr,"  -inside_circle center_x center_y radius\n");
    }

    public boolean parse(int argc, String[] argv)
    {
        int i;
        for (i = 1; i < argc; i++)
        {
            if (argv[i].isEmpty() || argv[i].startsWith("\0"))
            {
                continue;
            }
            else if (strcmp(argv[i],"-h") == 0)
            {
                LASfilter.usage();
                LAStransform.usage();
                usage();
                return TRUE;
            }
            else if (strcmp(argv[i],"-i") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs at least 1 argument: file_name or wild_card\n", argv[i]);
                    return FALSE;
                }
                argv[i]="\0";
                i+=1;
                do
                {
                    add_file_name(argv[i], unique);
                    argv[i]="\0";
                    i+=1;
                } while (i < argc && !argv[i].startsWith("-"));
                i-=1;
            }
            else if (strcmp(argv[i],"-unique") == 0)
            {
                unique = TRUE;
                argv[i]="\0";
            }
            else if (strncmp(argv[i],"-inside", 7) == 0)
            {
                if (strcmp(argv[i],"-inside_tile") == 0)
                {
                    if ((i+3) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 3 arguments: ll_x, ll_y, size\n", argv[i]);
                        return FALSE;
                    }
                    set_inside_tile((float)atof(argv[i+1]), (float)atof(argv[i+2]), (float)atof(argv[i+3]));
                    argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; i+=3;
                }
                else if (strcmp(argv[i],"-inside_circle") == 0)
                {
                    if ((i+3) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 3 arguments: center_x, center_y, radius\n", argv[i]);
                        return FALSE;
                    }
                    set_inside_circle(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3]));
                    argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; i+=3;
                }
                else if (strcmp(argv[i],"-inside") == 0 || strcmp(argv[i],"-inside_rectangle") == 0)
                {
                    if ((i+4) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 4 arguments: min_x, min_y, max_x, max_y\n", argv[i]);
                        return FALSE;
                    }
                    set_inside_rectangle(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3]), atof(argv[i+4]));
                    argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; argv[i+4]="\0"; i+=4;
                }
                else
                {
                    fprintf(stderr,"ERROR: unknown '-inside' option '%s'\n", argv[i]);
                    return FALSE;
                }
            }
            else if (strcmp(argv[i],"-comma_not_point") == 0)
            {
                comma_not_point = TRUE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-stdin") == 0)
            {
                use_stdin = TRUE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-lof") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: list_of_files\n", argv[i]);
                    return FALSE;
                }
                if (!add_list_of_files(argv[i+1], unique))
                {
                    fprintf(stderr, "ERROR: cannot load list of files '%s'\n", argv[i+1]);
                    return FALSE;
                }
      argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-rescale") == 0)
            {
                if ((i+3) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 3 arguments: rescale_x rescale_y rescale_z\n", argv[i]);
                    return FALSE;
                }
                double[] scale_factor = new double[3];
                scale_factor[0] = atof(argv[i+1]);
                scale_factor[1] = atof(argv[i+2]);
                scale_factor[2] = atof(argv[i+3]);
                set_scale_factor(scale_factor);
      argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; i+=3;
            }
            else if (strcmp(argv[i],"-rescale_xy") == 0)
            {
                if ((i+2) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 2 argument: rescale_x rescale_y\n", argv[i]);
                    return FALSE;
                }
                double[] scale_factor = new double[3];
                scale_factor[0] = atof(argv[i+1]);
                scale_factor[1] = atof(argv[i+2]);
                scale_factor[2] = 0;
                set_scale_factor(scale_factor);
      argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; i+=2;
            }
            else if (strcmp(argv[i],"-rescale_z") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: scale\n", argv[i]);
                    return FALSE;
                }
                double[] scale_factor = new double[3];
                scale_factor[0] = 0;
                scale_factor[1] = 0;
                scale_factor[2] = atof(argv[i+1]);
                set_scale_factor(scale_factor);
      argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-reoffset") == 0)
            {
                if ((i+3) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 3 arguments: reoffset_x, reoffset_y, reoffset_z\n", argv[i]);
                    return FALSE;
                }
                double[] offset = new double[3];
                offset[0] = atof(argv[i+1]);
                offset[1] = atof(argv[i+2]);
                offset[2] = atof(argv[i+3]);
                set_offset(offset);
      argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; i+=3;
            }
            else if (strcmp(argv[i],"-auto_reoffset") == 0)
            {
                set_auto_reoffset(TRUE);
      argv[i]="\0";
            }
            else if (strcmp(argv[i],"-files_are_flightlines") == 0 || strcmp(argv[i],"-faf") == 0)
            {
                set_files_are_flightlines(TRUE);
      argv[i]="\0";
            }
            else if (strcmp(argv[i],"-apply_file_source_ID") == 0)
            {
                set_apply_file_source_ID(TRUE);
      argv[i]="\0";
            }
            else if (strcmp(argv[i],"-itranslate_intensity") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: offset\n", argv[i]);
                    return FALSE;
                }
                set_translate_intensity((float)atof(argv[i+1]));
      argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-iscale_intensity") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: scale\n", argv[i]);
                    return FALSE;
                }
                set_scale_intensity((float)atof(argv[i+1]));
      argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-itranslate_scan_angle") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: offset\n", argv[i]);
                    return FALSE;
                }
                set_translate_scan_angle((float)atof(argv[i+1]));
      argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-iscale_scan_angle") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: scale\n", argv[i]);
                    return FALSE;
                }
                set_scale_scan_angle((float)atof(argv[i+1]));
      argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-iadd_extra") == 0 || strcmp(argv[i],"-iadd_attribute") == 0)
            {
                if ((i+3) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 3 arguments: data_type name description\n", argv[i]);
                    return FALSE;
                }
                if (((i+4) < argc) && (atof(argv[i+4]) != 0.0))
                {
                    if (((i+5) < argc) && ((atof(argv[i+5]) != 0.0) || (strcmp(argv[i+5], "0") == 0) || (strcmp(argv[i+5], "0.0") == 0)))
                    {
                        if (((i+6) < argc) && (atof(argv[i+6]) != 0.0))
                        {
                            if (((i+7) < argc) && ((atof(argv[i+7]) != 0.0) || (strcmp(argv[i+7], "0") == 0) || (strcmp(argv[i+7], "0.0") == 0)))
                            {
                                add_attribute(atoi(argv[i+1]), argv[i+2], argv[i+3], atof(argv[i+4]), atof(argv[i+5]), atof(argv[i+6]), atof(argv[i+7]));
                                argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; argv[i+4]="\0"; argv[i+5]="\0"; argv[i+6]="\0"; argv[i+7]="\0"; i+=7;
                            }
                            else
                            {
                                add_attribute(atoi(argv[i+1]), argv[i+2], argv[i+3], atof(argv[i+4]), atof(argv[i+5]), atof(argv[i+6]));
                                argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; argv[i+4]="\0"; argv[i+5]="\0"; argv[i+6]="\0"; i+=6;
                            }
                        }
                        else
                        {
                            add_attribute(atoi(argv[i+1]), argv[i+2], argv[i+3], atof(argv[i+4]), atof(argv[i+5]));
                            argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; argv[i+4]="\0"; argv[i+5]="\0"; i+=5;
                        }
                    }
                    else
                    {
                        add_attribute(atoi(argv[i+1]), argv[i+2], argv[i+3], atof(argv[i+4]));
                         argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; argv[i+4]="\0"; i+=4;
                    }
                }
                else
                {
                    add_attribute(atoi(argv[i+1]), argv[i+2], argv[i+3]);
                    argv[i]="\0"; argv[i+1]="\0"; argv[i+2]="\0"; argv[i+3]="\0"; i+=3;
                }
            }
            else if (strcmp(argv[i],"-iparse") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: string\n", argv[i]);
                    return FALSE;
                }
                set_parse_string(argv[i+1]);
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-iskip") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: number_of_lines\n", argv[i]);
                    return FALSE;
                }
                set_skip_lines(atoi(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-merged") == 0)
            {
                set_merged(TRUE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-buffered") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: size\n", argv[i]);
                    return FALSE;
                }
                set_buffer_size((float)atof(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-temp_files") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: base name\n", argv[i]);
                    return FALSE;
                }
                i++;
                temp_file_base = argv[i];
            }
            else if (strcmp(argv[i],"-neighbors") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs at least 1 argument: file_name or wild_card\n", argv[i]);
                    return FALSE;
                }
                argv[i]="\0";
                i+=1;
                do
                {
                    add_neighbor_file_name(argv[i]);
                    argv[i]="\0";
                    i+=1;
                } while (i < argc && !argv[i].equals("-"));
                i-=1;
            }
            else if (strcmp(argv[i],"-neighbors_lof") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs at least 1 argument: file_name\n", argv[i]);
                    return FALSE;
                }
                InputStream file = fopenR(argv[i+1], "r");
                if (file == null)
                {
                    fprintf(stderr, "ERROR: cannot open '%s'\n", argv[i+1]);
                    return FALSE;
                }
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(file));
                    String line;
                    try {
                        while ((line = reader.readLine()) != null) {
                            // remove extra white spaces and line return at the end
                            add_neighbor_file_name(line.trim());
                        }
                    } catch (IOException ignore) {
                    }
                } finally {
                    fclose(file);
                }
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-pipe_on") == 0)
            {
                set_pipe_on(TRUE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-populate") == 0)
            {
                set_populate_header(TRUE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-io_ibuffer") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: size\n", argv[i]);
                    return FALSE;
                }
                set_io_ibuffer_size((int)atoi(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-do_not_populate") == 0)
            {
                set_populate_header(FALSE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-ipts") == 0)
            {
                itxt = TRUE;
                ipts = TRUE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-iptx") == 0)
            {
                itxt = TRUE;
                iptx = TRUE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-itxt") == 0)
            {
                itxt = TRUE;
                argv[i]="\0";
            }
        }

        // check that there are only buffered neighbors for single files

        if (!neighbor_file_names.isEmpty())
        {
            if (file_names.size() > 1)
            {
                fprintf(stderr, "ERROR: neighbors only supported for one buffered input file, not for %d\n", file_names.size());
                return FALSE;
            }
            if (buffer_size == 0.0f)
            {
                fprintf(stderr, "ERROR: neighbors only make sense when used with '-buffered 50' or similar\n");
                return FALSE;
            }
        }

        if (filter != null) filter.clean();
        else filter = new LASfilter();
        if (!filter.parse(argc, argv))
        {
            return FALSE;
        }
        if (!filter.active())
        {
            filter = null;
        }

        if (transform != null) transform.clean();
        else transform = new LAStransform();
        if (!transform.parse(argc, argv))
        {
            return FALSE;
        }
        if (!transform.active())
        {
            transform = null;
        }
        else if (transform.filtered())
        {
            transform.setFilter(filter);
            filter = null;
        }

        if (files_are_flightlines || apply_file_source_ID)
        {
            if (transform == null) transform = new LAStransform();
            transform.setPointSource(0);
        }

        return TRUE;
    }

    public int get_file_name_number()
    {
        return file_names.size();
    }

    public int get_file_name_current()
    {
        return file_name_current;
    }

    public String get_file_name()
    {
        if (file_name != null)
            return file_name;
        if (!file_names.isEmpty())
            return file_names.get(0);
        return null;
    }

    String get_file_name(int number)
    {
        return file_names.get(number);
    }

    int get_file_format(int number)
    {
        if (strstr(file_names.get(number), ".las") || strstr(file_names.get(number), ".LAS"))
        {
            return LAS_TOOLS_FORMAT_LAS;
        }
        else if (strstr(file_names.get(number), ".laz") || strstr(file_names.get(number), ".LAZ"))
        {
            return LAS_TOOLS_FORMAT_LAZ;
        }
        else if (strstr(file_names.get(number), ".bin") || strstr(file_names.get(number), ".BIN"))
        {
            return LAS_TOOLS_FORMAT_BIN;
        }
        else if (strstr(file_names.get(number), ".shp") || strstr(file_names.get(number), ".SHP"))
        {
            return LAS_TOOLS_FORMAT_SHP;
        }
        else if (strstr(file_names.get(number), ".qi") || strstr(file_names.get(number), ".QI"))
        {
            return LAS_TOOLS_FORMAT_QFIT;
        }
        else if (strstr(file_names.get(number), ".asc") || strstr(file_names.get(number), ".ASC"))
        {
            return LAS_TOOLS_FORMAT_ASC;
        }
        else if (strstr(file_names.get(number), ".bil") || strstr(file_names.get(number), ".BIL"))
        {
            return LAS_TOOLS_FORMAT_BIL;
        }
        else if (strstr(file_names.get(number), ".dtm") || strstr(file_names.get(number), ".DTM"))
        {
            return LAS_TOOLS_FORMAT_DTM;
        }
        else
        {
            return LAS_TOOLS_FORMAT_TXT;
        }
    }

    void set_merged(boolean merged)
    {
        this.merged = merged;
    }

    void set_buffer_size(float buffer_size)
    {
        this.buffer_size = buffer_size;
    }

    float get_buffer_size()
    {
        return buffer_size;
    }

    void set_filter(LASfilter filter)
    {
        this.filter = filter;
    }

    void set_transform(LAStransform transform)
    {
        this.transform = transform;
    }

    void set_auto_reoffset(boolean auto_reoffset)
    {
        this.auto_reoffset = auto_reoffset;
    }

    void set_files_are_flightlines(boolean files_are_flightlines)
    {
        this.files_are_flightlines = files_are_flightlines;
    }

    void set_apply_file_source_ID(boolean apply_file_source_ID)
    {
        this.apply_file_source_ID = apply_file_source_ID;
    }

    void set_io_ibuffer_size(int io_ibuffer_size)
    {
        this.io_ibuffer_size = io_ibuffer_size;
    }

    void set_file_name(String file_name, boolean unique)
    {
        add_file_name(file_name, unique);
    }


    public boolean add_file_name(String file_name) {
        return add_file_name(file_name, false);
    }

    public boolean add_file_name(String file_name, boolean unique)
    {
        if (unique && file_names.contains(file_name))
        {
            return FALSE;
        }
        file_names.add(file_name);
        return TRUE;
    }

    boolean add_list_of_files(String list_of_files, boolean unique)
    {
        InputStream file = fopenR(list_of_files, "r");
        if (file == null)
        {
            fprintf(stderr, "ERROR: cannot open '%s'\n", list_of_files);
            return FALSE;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(file));
            String line;
            while ((line = reader.readLine()) != null)
            {
                // remove extra white spaces and line return at the end
                add_file_name(line.trim(), unique);
            }
        } catch (IOException e) {
            return FALSE;
        } finally {
            fclose(file);
        }
        return TRUE;
    }

    void delete_file_name(int file_name_id)
    {
        file_names.remove(file_name_id);
    }

    boolean set_file_name_current(int file_name_id)
    {
        if (file_name_id < file_names.size())
        {
            file_name_current = file_name_id;
            file_name = file_names.get(file_name_current);
            return TRUE;
        }
        return FALSE;
    }


    boolean add_neighbor_file_name(String neighbor_file_name) {
        return add_neighbor_file_name(neighbor_file_name, false);
    }

    boolean add_neighbor_file_name(String neighbor_file_name, boolean unique)
    {
        if (unique && neighbor_file_names.contains(neighbor_file_name))
        {
            return FALSE;
        }
        neighbor_file_names.add(neighbor_file_name);
        return TRUE;
    }

    void set_parse_string(String parse_string)
    {
        this.parse_string = parse_string;
    }

    String get_parse_string()
    {
        return parse_string;
    }

    void set_scale_factor(double[] scale_factor)
    {
        if (scale_factor != null)
        {
            if (this.scale_factor == null) this.scale_factor = new double[3];
            this.scale_factor[0] = scale_factor[0];
            this.scale_factor[1] = scale_factor[1];
            this.scale_factor[2] = scale_factor[2];
        }
        else if (this.scale_factor != null)
        {
            this.scale_factor = null;
        }
    }

    void set_offset(double[] offset)
    {
        if (offset != null)
        {
            if (this.offset == null) this.offset = new double[3];
            this.offset[0] = offset[0];
            this.offset[1] = offset[1];
            this.offset[2] = offset[2];
        }
        else if (this.offset != null)
        {
            this.offset = null;
        }
    }

    void set_translate_intensity(float translate_intensity)
    {
        this.translate_intensity = translate_intensity;
    }

    void set_scale_intensity(float scale_intensity)
    {
        this.scale_intensity = scale_intensity;
    }

    void set_translate_scan_angle(float translate_scan_angle)
    {
        this.translate_scan_angle = translate_scan_angle;
    }

    void set_scale_scan_angle(float scale_scan_angle)
    {
        this.scale_scan_angle = scale_scan_angle;
    }

    void add_attribute(int data_type, String name) {
        add_attribute(data_type, name, null, 1.0, 0.0, 1.0, 0.0);
    }

    void add_attribute(int data_type, String name, String description) {
        add_attribute(data_type, name, description, 1.0, 0.0, 1.0, 0.0);
    }

    void add_attribute(int data_type, String name, String description, double scale) {
        add_attribute(data_type, name, description, scale, 0.0, 1.0, 0.0);
    }

    void add_attribute(int data_type, String name, String description, double scale, double offset) {
        add_attribute(data_type, name, description, scale, offset, 1.0, 0.0);
    }

    void add_attribute(int data_type, String name, String description, double scale, double offset, double pre_scale) {
        add_attribute(data_type, name, description, scale, offset, pre_scale, 0.0);
    }

    void add_attribute(int data_type, String name, String description, double scale, double offset, double pre_scale, double pre_offset)
    {
        attribute_data_types[number_attributes] = data_type;
        attribute_names[number_attributes] = name;
        attribute_descriptions[number_attributes] = description;
        attribute_scales[number_attributes] = scale;
        attribute_offsets[number_attributes] = offset;
        attribute_pre_scales[number_attributes] = pre_scale;
        attribute_pre_offsets[number_attributes] = pre_offset;
        number_attributes++;
    }

    void set_skip_lines(int skip_lines)
    {
        this.skip_lines = skip_lines;
    }

    void set_populate_header(boolean populate_header)
    {
        this.populate_header = populate_header;
    }

    void set_keep_lastiling(boolean keep_lastiling)
    {
        this.keep_lastiling = keep_lastiling;
    }

    void set_pipe_on(boolean pipe_on)
    {
        this.pipe_on = pipe_on;
    }

    void set_inside_tile(float ll_x, float ll_y, float size)
    {
        if (inside_tile == null) inside_tile = new float[3];
        inside_tile[0] = ll_x;
        inside_tile[1] = ll_y;
        inside_tile[2] = size;
    }

    void set_inside_circle(double center_x, double center_y, double radius)
    {
        if (inside_circle == null) inside_circle = new double[3];
        inside_circle[0] = center_x;
        inside_circle[1] = center_y;
        inside_circle[2] = radius;
    }

    void set_inside_rectangle(double min_x, double min_y, double max_x, double max_y)
    {
        if (inside_rectangle == null) inside_rectangle = new double[4];
        inside_rectangle[0] = min_x;
        inside_rectangle[1] = min_y;
        inside_rectangle[2] = max_x;
        inside_rectangle[3] = max_y;
    }

    public boolean active()
    {
        return ((file_name_current < file_names.size()) || use_stdin);
    }

    public LASreadOpener()
    {
        io_ibuffer_size = LAS_TOOLS_IO_IBUFFER_SIZE;
        file_name = null;
        merged = FALSE;
        use_stdin = FALSE;
        comma_not_point = FALSE;
        scale_factor = null;
        offset = null;
        buffer_size = 0.0f;
        auto_reoffset = FALSE;
        files_are_flightlines = FALSE;
        apply_file_source_ID = FALSE;
        itxt = FALSE;
        ipts = FALSE;
        iptx = FALSE;
        translate_intensity = 0.0f;
        scale_intensity = 1.0f;
        translate_scan_angle = 0.0f;
        scale_scan_angle = 1.0f;
        number_attributes = 0;
        for (int i = 0; i < 10; i++)
        {
            attribute_data_types[i] = 0;
            attribute_names[i] = null;
            attribute_descriptions[i] = null;
            attribute_scales[i] = 1.0;
            attribute_offsets[i] = 0.0;
            attribute_pre_scales[i] = 1.0;
            attribute_pre_offsets[i] = 0.0;
        }
        parse_string = null;
        skip_lines = 0;
        populate_header = FALSE;
        keep_lastiling = FALSE;
        pipe_on = FALSE;
        unique = FALSE;
        file_name_current = 0;
        inside_tile = null;
        inside_circle = null;
        inside_rectangle = null;
        filter = null;
        transform = null;
        temp_file_base = null;
    }

    private static boolean strstr(String s1, String s2) {
        return s1.contains(s2);
    }

}
