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
package com.github.mreutegg.laszip4j.laslib;

import java.io.File;
import java.io.PrintStream;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atof;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atoi;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_BIN;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_DEFAULT;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAS;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAZ;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_QFIT;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_TXT;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_VRML;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_IO_OBUFFER_SIZE;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_CHUNK_SIZE_DEFAULT;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_CHUNKED;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_LAYERED_CHUNKED;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_COMPRESSOR_NONE;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwriteOpener {

    private static final PrintStream stderr = System.err;
    private static final PrintStream stdout = System.out;
    
    private int io_obuffer_size;
    private String directory;
    private String file_name;
    private String appendix;
    private int cut;                // unsigned
    private boolean opts;
    private boolean optx;
    private String parse_string;
    private String separator;
    private float scale_rgb;
    private int format;             // unsigned
    private boolean specified;
    private boolean force;
    private boolean ntive;          // 'native'
    private int chunk_size;         // unsigned
    private boolean use_stdout;
    private boolean use_nil;
    private boolean buffered;

    public boolean is_piped()
    {
        return ((file_name == null) && use_stdout);
    }

    public LASwriter open(LASheader header)
    {
        if (use_nil)
        {
            LASwriterLAS laswriterlas = new LASwriterLAS();
            if (!laswriterlas.open(header, (format == LAS_TOOLS_FORMAT_LAZ ? LASZIP_COMPRESSOR_CHUNKED : LASZIP_COMPRESSOR_NONE), 2, chunk_size))
            {
                fprintf(stderr,"ERROR: cannot open laswriterlas to NULL\n");
                return null;
            }
            return laswriterlas;
        }
        else if (file_name != null)
        {
            if (format <= LAS_TOOLS_FORMAT_LAZ)
            {
                LASwriterLAS laswriterlas = new LASwriterLAS();
                if (!laswriterlas.open(file_name, header, (format == LAS_TOOLS_FORMAT_LAZ ? (ntive ? LASZIP_COMPRESSOR_LAYERED_CHUNKED : LASZIP_COMPRESSOR_CHUNKED) : LASZIP_COMPRESSOR_NONE), 2, chunk_size, io_obuffer_size))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterlas with file name '%s'\n", file_name);
                    return null;
                }
                return laswriterlas;
            }
            else if (format == LAS_TOOLS_FORMAT_TXT)
            {
                LASwriterTXT laswritertxt = new LASwriterTXT();
                if (opts) laswritertxt.set_pts(TRUE);
                else if (optx) laswritertxt.set_ptx(TRUE);
                if (!laswritertxt.open(file_name, header, parse_string, separator))
                {
                    fprintf(stderr,"ERROR: cannot open laswritertxt with file name '%s'\n", file_name);
                    return null;
                }
                if (scale_rgb != 1.0f) laswritertxt.set_scale_rgb(scale_rgb);
                return laswritertxt;
            }
            else if (format == LAS_TOOLS_FORMAT_BIN)
            {
                LASwriterBIN laswriterbin = new LASwriterBIN();
                if (!laswriterbin.open(file_name, header, "ts8"))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterbin with file name '%s'\n", file_name);
                    return null;
                }
                return laswriterbin;
            }
            else if (format == LAS_TOOLS_FORMAT_QFIT)
            {
                LASwriterQFIT laswriterqfit = new LASwriterQFIT();
                if (!laswriterqfit.open(file_name, header, 40))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterqfit with file name '%s'\n", file_name);
                    return null;
                }
                return laswriterqfit;
            }
            else if (format == LAS_TOOLS_FORMAT_VRML)
            {
                LASwriterWRL laswriterwrl = new LASwriterWRL();
                if (!laswriterwrl.open(file_name, header, parse_string))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterwrl with file name '%s'\n", file_name);
                    return null;
                }
                return laswriterwrl;
            }
            else
            {
                fprintf(stderr,"ERROR: unknown format %d\n", format);
                return null;
            }
        }
        else if (use_stdout)
        {
            if (format <= LAS_TOOLS_FORMAT_LAZ)
            {
                LASwriterLAS laswriterlas = new LASwriterLAS();
                if (!laswriterlas.open(stdout, header, (format == LAS_TOOLS_FORMAT_LAZ ? LASZIP_COMPRESSOR_CHUNKED : LASZIP_COMPRESSOR_NONE), 2, chunk_size))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterlas to stdout\n");
                    return null;
                }
                return laswriterlas;
            }
            else if (format == LAS_TOOLS_FORMAT_TXT)
            {
                LASwriterTXT laswritertxt = new LASwriterTXT();
                if (opts) laswritertxt.set_pts(TRUE);
                else if (optx) laswritertxt.set_ptx(TRUE);
                if (!laswritertxt.open(stdout, header, parse_string, separator))
                {
                    fprintf(stderr,"ERROR: cannot open laswritertxt to stdout\n");
                    return null;
                }
                if (scale_rgb != 1.0f) laswritertxt.set_scale_rgb(scale_rgb);
                return laswritertxt;
            }
            else if (format == LAS_TOOLS_FORMAT_BIN)
            {
                LASwriterBIN laswriterbin = new LASwriterBIN();
                if (!laswriterbin.open(stdout, header, "ts8"))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterbin to stdout\n");
                    return null;
                }
                return laswriterbin;
            }
            else if (format == LAS_TOOLS_FORMAT_QFIT)
            {
                LASwriterQFIT laswriterqfit = new LASwriterQFIT();
                if (!laswriterqfit.open(stdout, header, 40))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterbin to stdout\n");
                    return null;
                }
                return laswriterqfit;
            }
            else if (format == LAS_TOOLS_FORMAT_VRML)
            {
                LASwriterWRL laswriterwrl = new LASwriterWRL();
                if (!laswriterwrl.open(stdout, header, parse_string))
                {
                    fprintf(stderr,"ERROR: cannot open laswriterwrl with file name '%s'\n", file_name);
                    return null;
                }
                return laswriterwrl;
            }
            else
            {
                fprintf(stderr,"ERROR: unknown format %d\n", format);
                return null;
            }
        }
        else
        {
            fprintf(stderr,"ERROR: no laswriter output specified\n");
            return null;
        }
    }

    public LASwaveform13writer open_waveform13(LASheader lasheader)
    {
        if (lasheader.point_data_format < 4) return null;
        if ((lasheader.point_data_format > 5) && (lasheader.point_data_format < 9)) return null;
        if (lasheader.vlr_wave_packet_descr == null) return null;
        if (get_file_name() == null) return null;
        LASwaveform13writer waveform13writer = new LASwaveform13writer();
        if (waveform13writer.open(get_file_name(), lasheader.vlr_wave_packet_descr))
        {
            return waveform13writer;
        }
        return null;
    }

    void usage()
    {
        fprintf(stderr,"Supported LAS Outputs\n");
        fprintf(stderr,"  -o lidar.las\n");
        fprintf(stderr,"  -o lidar.laz\n");
        fprintf(stderr,"  -o xyzta.txt -oparse xyzta (on-the-fly to ASCII)\n");
        fprintf(stderr,"  -o terrasolid.bin\n");
        fprintf(stderr,"  -o nasa.qi\n");
        fprintf(stderr,"  -odir C:%cdata%cground (specify output directory)\n", File.pathSeparatorChar, File.pathSeparatorChar);
        fprintf(stderr,"  -odix _classified (specify file name appendix)\n");
        fprintf(stderr,"  -ocut 2 (cut the last two characters from name)\n");
        fprintf(stderr,"  -olas -olaz -otxt -obin -oqfit (specify format)\n");
        fprintf(stderr,"  -stdout (pipe to stdout)\n");
        fprintf(stderr,"  -nil    (pipe to NULL)\n");
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
                usage();
                return TRUE;
            }
            else if (strcmp(argv[i],"-o") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: file_name\n", argv[i]);
                    return FALSE;
                }
                set_file_name(argv[i+1]);
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-odir") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: directory\n", argv[i]);
                    return FALSE;
                }
                set_directory(argv[i+1]);
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-odix") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: appendix\n", argv[i]);
                    return FALSE;
                }
                set_appendix(argv[i+1]);
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-ocut") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: number of characters to cut\n", argv[i]);
                    return FALSE;
                }
                set_cut(atoi(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-oforce") == 0)
            {
                set_force(TRUE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-native") == 0)
            {
                set_native(TRUE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-compatible") == 0)
            {
                set_native(FALSE);
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-olas") == 0)
            {
                specified = TRUE;
                format = LAS_TOOLS_FORMAT_LAS;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-olaz") == 0)
            {
                specified = TRUE;
                format = LAS_TOOLS_FORMAT_LAZ;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-otxt") == 0)
            {
                specified = TRUE;
                format = LAS_TOOLS_FORMAT_TXT;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-obin") == 0)
            {
                specified = TRUE;
                format = LAS_TOOLS_FORMAT_BIN;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-oqi") == 0)
            {
                specified = TRUE;
                format = LAS_TOOLS_FORMAT_QFIT;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-owrl") == 0)
            {
                specified = TRUE;
                format = LAS_TOOLS_FORMAT_VRML;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-stdout") == 0)
            {
                use_stdout = TRUE;
                use_nil = FALSE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-nil") == 0)
            {
                use_nil = TRUE;
                use_stdout = FALSE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-chunk_size") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: number_points\n", argv[i]);
                    return FALSE;
                }
                set_chunk_size(atoi(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-oparse") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: string\n", argv[i]);
                    return FALSE;
                }
                set_parse_string(argv[i+1]);
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-osep") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: separator\n", argv[i]);
                    return FALSE;
                }
                set_separator(argv[i+1]);
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-oscale_rgb") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: scale\n", argv[i]);
                    return FALSE;
                }
                set_scale_rgb((float)atof(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
            else if (strcmp(argv[i],"-opts") == 0)
            {
                opts = TRUE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-optx") == 0)
            {
                optx = TRUE;
                argv[i]="\0";
            }
            else if (strcmp(argv[i],"-io_obuffer") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: size\n", argv[i]);
                    return FALSE;
                }
                set_io_obuffer_size((int)atoi(argv[i+1]));
                argv[i]="\0"; argv[i+1]="\0"; i+=1;
            }
        }
        return TRUE;
    }

    void set_io_obuffer_size(int io_obuffer_size)
    {
        this.io_obuffer_size = io_obuffer_size;
    }

    void set_directory(String directory)
    {
        if (directory != null)
        {
            if (strstr(directory, ";") || strstr(directory, "\""))
            {
                fprintf(stderr,"WARNING: specified '-odir' seems to contain a substring '\\\"' such\n");
                fprintf(stderr,"         as -odir \"D:\\\" or -odir \"..\\tiles\\\". this command will\n");
                fprintf(stderr,"         probably fail. please use -odir \"D:\" or -odir \"..\\tiles\"\n");
                fprintf(stderr,"         instead.\n");
            }
            this.directory = directory;
            if (this.directory.endsWith("\\") || this.directory.endsWith("/") || this.directory.endsWith(":")) {
                this.directory = this.directory.substring(0, this.directory.length() - 1);
            }
            if (file_name != null) add_directory();
        }
        else
        {
            this.directory = null;
        }
    }

    public void set_file_name(String file_name)
    {
        if (file_name != null)
        {
            if (!specified)
            {
                int len = file_name.length();
                String format = file_name.substring(len - 4);
                if (strstr(format, ".laz") || strstr(format, ".LAZ"))
                {
                    this.format = LAS_TOOLS_FORMAT_LAZ;
                }
                else if (strstr(format, ".las") || strstr(format, ".LAS"))
                {
                    this.format = LAS_TOOLS_FORMAT_LAS;
                }
                else if (strstr(format, ".bin") || strstr(format, ".BIN")) // terrasolid
                {
                    this.format = LAS_TOOLS_FORMAT_BIN;
                }
                else if (strstr(format, ".qi") || strstr(format, ".QI")) // QFIT
                {
                    this.format = LAS_TOOLS_FORMAT_QFIT;
                }
                else if (strstr(format, ".wrl") || strstr(format, ".WRL")) // VRML
                {
                    this.format = LAS_TOOLS_FORMAT_VRML;
                }
                else // assume ascii output
                {
                    this.format = LAS_TOOLS_FORMAT_TXT;
                }
            }
            this.file_name = file_name;

            if (directory != null) add_directory();
            if (cut != 0) cut_characters();
            if (appendix != null) add_appendix();
        }
        else
        {
            this.file_name = null;
        }
    }

    void set_appendix(String appendix)
    {
        if (appendix != null)
        {
            this.appendix = appendix;
            if (file_name != null) add_appendix();
        }
        else
        {
            this.appendix = null;
        }
    }

    void set_cut(int cut)
    {
        this.cut = cut;
        if (cut != 0 && file_name != null) cut_characters();
    }

    void set_native(boolean ntive)
    {
      this.ntive = ntive;
    }

    public boolean set_format(int format)
    {
        if ((format < LAS_TOOLS_FORMAT_DEFAULT) || (format > LAS_TOOLS_FORMAT_TXT))
        {
            return FALSE;
        }

        specified = TRUE;
        this.format = format;

        if (file_name != null)
        {
            int len = file_name.length();
            len--;
            while (len > 0 && this.file_name.charAt(len) != '.')
            {
                len--;
            }
            if (len != 0)
            {
                if (format <= LAS_TOOLS_FORMAT_LAS)
                {
                    file_name = setChar(file_name, len+1, 'l');
                    file_name = setChar(file_name, len+2, 'a');
                    file_name = setChar(file_name, len+3, 's');
                }
                else if (format == LAS_TOOLS_FORMAT_LAZ)
                {
                    file_name = setChar(file_name, len+1, 'l');
                    file_name = setChar(file_name, len+2, 'a');
                    file_name = setChar(file_name, len+3, 'z');
                }
                else if (format == LAS_TOOLS_FORMAT_BIN)
                {
                    // TODO: report bug
                    file_name = setChar(file_name, len+1, 'b');
                    file_name = setChar(file_name, len+2, 'i');
                    file_name = setChar(file_name, len+3, 'n');
                }
                else if (format == LAS_TOOLS_FORMAT_QFIT)
                {
                    file_name = setChar(file_name, len+1, 'q');
                    file_name = setChar(file_name, len+2, 'i');
                }
                else if (format == LAS_TOOLS_FORMAT_VRML)
                {
                    file_name = setChar(file_name, len+1, 'w');
                    file_name = setChar(file_name, len+2, 'r');
                    file_name = setChar(file_name, len+3, 'l');
                }
                else if (format == LAS_TOOLS_FORMAT_TXT)
                {
                    file_name = setChar(file_name, len+1, 't');
                    file_name = setChar(file_name, len+2, 'x');
                    file_name = setChar(file_name, len+3, 't');
                }
                else
                {
                    return FALSE;
                }
            }
        }
        return TRUE;
    }

    boolean set_format(String format)
    {
        if (format != null)
        {
            if (strstr(format, "laz") || strstr(format, "LAZ"))
            {
                return set_format(LAS_TOOLS_FORMAT_LAZ);
            }
            else if (strstr(format, "las") || strstr(format, "LAS"))
            {
                return set_format(LAS_TOOLS_FORMAT_LAS);
            }
            else if (strstr(format, "bin") || strstr(format, "BIN")) // terrasolid
            {
                return set_format(LAS_TOOLS_FORMAT_BIN);
            }
            else if (strstr(format, "qi") || strstr(format, "QI")) // QFIT
            {
                return set_format(LAS_TOOLS_FORMAT_QFIT);
            }
            else if (strstr(format, "wrl") || strstr(format, "WRL")) // VRML
            {
                return set_format(LAS_TOOLS_FORMAT_VRML);
            }
            else // assume ascii output
            {
                return set_format(LAS_TOOLS_FORMAT_TXT);
            }
        }
        else
        {
            specified = FALSE;
            this.format = LAS_TOOLS_FORMAT_DEFAULT;
        }
        return TRUE;
    }

    public void set_force(boolean force)
    {
        this.force = force;
    }

    void set_chunk_size(int chunk_size)
    {
        this.chunk_size = chunk_size;
    }

    void make_numbered_file_name(String file_name, int digits)
    {
        int len;
        if (file_name != null)
        {
            len = file_name.length();
            this.file_name = file_name;
        }
        else
        {
            if (this.file_name == null) this.file_name = "output.xxx";
            len = this.file_name.length();
        }
        len--;
        while (len > 0 && this.file_name.charAt(len) != '.')
        {
            len--;
        }
        if (len > 0)
        {
            this.file_name = setChar(this.file_name, len, '_');
            len++;
        }
        while (digits > 0)
        {
            this.file_name = setChar(this.file_name, len, '0');
            digits--;
            len++;
        }
        this.file_name = setChar(this.file_name, len, '.');
        len++;
        this.file_name = setChar(this.file_name, len, 'x');
        this.file_name = setChar(this.file_name, len+1, 'x');
        this.file_name = setChar(this.file_name, len+2, 'x');
    }

    public void make_file_name(String file_name, int file_number)
    {
        int len;

        if (file_number > -1)
        {
            if (file_name != null)
            {
                len = file_name.length();
                this.file_name = file_name;
                if (cut != 0)
                {
                    cut_characters();
                    len = file_name.length();
                }
            }
            else
            {
                if (this.file_name == null)
                {
                    this.file_name = "output_0000000.xxx";
                }
                len = this.file_name.length();
            }
            len--;
            while (len > 0 && this.file_name.charAt(len) != '.') len--;
            len++;
            int num = len - 2;
            int file_num = file_number;
            while (num > 0 && this.file_name.charAt(num) >= '0' && this.file_name.charAt(num) <= '9')
            {
                this.file_name = new StringBuilder(this.file_name).deleteCharAt(num).insert(num, '0' + (file_num%10)).toString();
                file_num = file_num/10;
                num--;
            }
            if (file_num != 0)
            {
                fprintf(stderr,"WARNING: file name number %d too big to store in '%s'. use more digits.\n", file_number, this.file_name);
            }
        }
        else
        {
            if (file_name != null)
            {
                len = file_name.length() - 1;
                this.file_name = file_name;
                while (len > 0 && this.file_name.charAt(len) != '.') len--;
                if (cut != 0)
                {
                    len -= cut;
                    if (len < 0) len = 0;
                }
                if (appendix != null)
                {
                    this.file_name += appendix;
                    len += appendix.length();
                }
                else if ((directory == null) && (cut == 0) && (file_number == -1))
                {
                    this.file_name = setChar(this.file_name, len, '_');
                    this.file_name = setChar(this.file_name, len+1, '1');
                    len += 2;
                }
                this.file_name = setChar(this.file_name, len, '.');
                len++;
            }
            else
            {
                len = 7;
                this.file_name = "output.xxx";
            }
        }
        if (format <= LAS_TOOLS_FORMAT_LAS)
        {
            this.file_name = setChar(this.file_name, len, 'l');
            this.file_name = setChar(this.file_name, len+1, 'a');
            this.file_name = setChar(this.file_name, len+2, 's');
        }
        else if (format == LAS_TOOLS_FORMAT_LAZ)
        {
            this.file_name = setChar(this.file_name, len, 'l');
            this.file_name = setChar(this.file_name, len+1, 'a');
            this.file_name = setChar(this.file_name, len+2, 'z');
        }
        else if (format == LAS_TOOLS_FORMAT_BIN)
        {
            this.file_name = setChar(this.file_name, len, 'b');
            this.file_name = setChar(this.file_name, len+1, 'i');
            this.file_name = setChar(this.file_name, len+2, 'n');
        }
        else if (format == LAS_TOOLS_FORMAT_QFIT)
        {
            this.file_name = setChar(this.file_name, len, 'q');
            this.file_name = setChar(this.file_name, len+1, 'i');
        }
        else // if (format == LAS_TOOLS_FORMAT_TXT)
        {
            this.file_name = setChar(this.file_name, len, 't');
            this.file_name = setChar(this.file_name, len+1, 'x');
            this.file_name = setChar(this.file_name, len+2, 't');
        }

        if (directory != null) add_directory();

        if (file_name != null && (strcmp(this.file_name, file_name) == 0))
        {
            if (!force)
            {
                if (format <= LAS_TOOLS_FORMAT_LAS)
                {
                    this.file_name =  "temp.las";
                }
                else if (format == LAS_TOOLS_FORMAT_LAZ)
                {
                    this.file_name = "temp.laz";
                }
                else if (format == LAS_TOOLS_FORMAT_BIN)
                {
                    this.file_name = "temp.bin";
                }
                else if (format == LAS_TOOLS_FORMAT_QFIT)
                {
                    this.file_name = "temp.qi";
                }
                else if (format == LAS_TOOLS_FORMAT_VRML)
                {
                    this.file_name = "temp.wrl";
                }
                else // if (format == LAS_TOOLS_FORMAT_TXT)
                {
                    this.file_name = "temp.txt";
                }
                fprintf(stderr,"WARNING: generated output name '%s'\n", file_name);
                fprintf(stderr,"         identical to input name. changed to '%s'.\n", this.file_name);
                fprintf(stderr,"         you can override this safety measure with '-oforce'.\n");
            }
        }
    }

    String get_directory()
    {
        return directory;
    }

    public String get_file_name()
    {
        return file_name;
    }

    String get_file_name_base()
    {
        String file_name_base = null;

        if (file_name != null)
        {
            file_name_base = file_name;
            // remove extension
            int len = file_name_base.length() - 1;
            while ((len > 0) && (file_name_base.charAt(len) != '.') && (file_name_base.charAt(len) != '\\') && (file_name_base.charAt(len) != '/') && (file_name_base.charAt(len) != ':')) len--;
            if (file_name_base.charAt(len) == '.')
            {
                file_name_base = file_name_base.substring(0, len);
            }
        }
        else if (directory != null)
        {
            file_name_base = String.format("%s\\", directory);
        }

        return file_name_base;
    }

    String get_file_name_only()
    {
        String file_name_only = null;

        if (file_name != null)
        {
            file_name_only = new File(file_name).getName();
        }

        return file_name_only;
    }

    String get_appendix()
    {
        return appendix;
    }

    int get_cut()
    {
        return cut;
    }

    public boolean get_native()
    {
      return ntive;
    }

    public boolean format_was_specified()
    {
        return specified;
    }

    static String[] LAS_TOOLS_FORMAT_NAMES = new String[]{ "las", "las", "laz", "bin", "qi", "wrl", "txt", "shp", "asc", "bil", "flt" };

    String get_format_name()
    {
        return LAS_TOOLS_FORMAT_NAMES[get_format()];
    }

    public int get_format()
    {
        if (specified || (file_name == null))
        {
            return format;
        }
        else
        {
            if (strstr(file_name, ".laz") || strstr(file_name, ".LAZ"))
            {
                return LAS_TOOLS_FORMAT_LAZ;
            }
            else if (strstr(file_name, ".las") || strstr(file_name, ".LAS"))
            {
                return LAS_TOOLS_FORMAT_LAS;
            }
            else if (strstr(file_name, ".bin") || strstr(file_name, ".BIN")) // terrasolid
            {
                return LAS_TOOLS_FORMAT_BIN;
            }
            else if (strstr(file_name, ".qi") || strstr(file_name, ".QI")) // QFIT
            {
                return LAS_TOOLS_FORMAT_QFIT;
            }
            else if (strstr(file_name, ".wrl") || strstr(file_name, ".WRL")) // VRML
            {
                return LAS_TOOLS_FORMAT_VRML;
            }
            else // assume ascii output
            {
                return LAS_TOOLS_FORMAT_TXT;
            }
        }
    }

    void set_parse_string(String parse_string)
    {
        this.parse_string = parse_string;
    }

    void set_separator(String separator)
    {
        this.separator = separator;
    }

    void set_scale_rgb(float scale_rgb)
    {
        this.scale_rgb = scale_rgb;
    }

    public boolean active()
    {
        return (file_name != null || use_stdout || use_nil);
    }

    void add_directory() {
        add_directory(null);
    }

    void add_directory(String directory)
    {
        if (directory == null) directory = this.directory;

        if (file_name != null && directory != null)
        {
            file_name = new File(directory, file_name).getPath();
        }
    }

    void add_appendix() {
        add_appendix(null);
    }

    void add_appendix(String appendix)
    {
        if (appendix == null) appendix = this.appendix;

        if (file_name != null && appendix != null)
        {
            int len = file_name.length() - 1;
            String new_file_name;
            while ((len > 0) && (file_name.charAt(len) != '.') && (file_name.charAt(len) != '\\') && (file_name.charAt(len) != '/') && (file_name.charAt(len) != ':')) len--;

            if ((len == 0) || (file_name.charAt(len) == '\\') || (file_name.charAt(len) == '/') || (file_name.charAt(len) == ':'))
            {
                new_file_name = file_name + appendix;
            }
            else
            {
                new_file_name = file_name.substring(0, len) + appendix + file_name.substring(len);
            }
            file_name = new_file_name;
        }
    }

    void cut_characters() {
        cut_characters(0);
    }

    void cut_characters(int cut)
    {
        if (cut == 0) cut = this.cut;

        if (file_name != null && cut != 0)
        {
            int len = file_name.length() - 1;
            String new_file_name;
            while ((len > 0) && (file_name.charAt(len) != '.') && (file_name.charAt(len) != '\\') && (file_name.charAt(len) != '/') && (file_name.charAt(len) != ':')) len--;

            if ((len == 0) || (file_name.charAt(len) == '\\') || (file_name.charAt(len) == '/') || (file_name.charAt(len) == ':'))
            {
                new_file_name = file_name.substring(0, file_name.length()-cut);
            }
            else
            {
                new_file_name = file_name.substring(0, len-cut) + file_name.substring(len);
            }
            file_name = new_file_name;
        }
    }

    public LASwriteOpener()
    {
        io_obuffer_size = LAS_TOOLS_IO_OBUFFER_SIZE;
        directory = null;
        file_name = null;
        appendix = null;
        cut = 0;
        opts = FALSE;
        optx = FALSE;
        parse_string = null;
        separator = null;
        scale_rgb = 1.0f;
        ntive = true;
        format = LAS_TOOLS_FORMAT_DEFAULT;
        specified = FALSE;
        force = FALSE;
        chunk_size = LASZIP_CHUNK_SIZE_DEFAULT;
        use_stdout = FALSE;
        use_nil = FALSE;
    }

    private static boolean strstr(String s1, String s2) {
        return s1.contains(s2);
    }

    private static String setChar(String s, int index, char c) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() <= index) {
            sb.append('\0');
        }
        sb.setCharAt(index, c);
        return sb.toString();
    }
}
