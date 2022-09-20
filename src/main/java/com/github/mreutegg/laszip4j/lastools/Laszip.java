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
package com.github.mreutegg.laszip4j.lastools;

import com.github.mreutegg.laszip4j.clib.Cstdio;
import com.github.mreutegg.laszip4j.laslib.LASreader;
import com.github.mreutegg.laszip4j.laslib.LASwaveform13reader;
import com.github.mreutegg.laszip4j.laslib.LASwaveform13writer;
import com.github.mreutegg.laszip4j.laslib.LASwriter;
import com.github.mreutegg.laszip4j.laslib.LasDefinitions;
import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;
import com.github.mreutegg.laszip4j.laslib.LASreadOpener;
import com.github.mreutegg.laszip4j.laslib.LASwriteOpener;
import com.github.mreutegg.laszip4j.laszip.LASindex;
import com.github.mreutegg.laszip4j.laszip.LASquadtree;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atof;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atoi;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAS;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_FORMAT_LAZ;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class Laszip {

    public static void main(String[] args) {
        run(args);
        byebye(false);
    }

    public static void run(String[] args) {
        String[] argv = new String[args.length + 1];
        argv[0] = "laszip";
        System.arraycopy(args, 0, argv, 1, args.length);
        int argc = argv.length;
        PrintStream stderr = System.err;

        int i;
        boolean dry = false;
        boolean verbose = false;
        boolean waveform = false;
        boolean waveform_with_map = false;
        boolean report_file_size = false;
        boolean check_integrity = false;
        int end_of_points = -1;
        boolean projection_was_set = false;
        boolean format_not_specified = false;
        boolean lax = false;
        boolean append = false;
        float tile_size = 100.0f;
        int threshold = 1000;
        int minimum_points = 100000;
        int maximum_intervals = -20;
        double start_time = 0.0;
        double total_start_time = 0;

        LASreadOpener lasreadopener = new LASreadOpener();
        GeoProjectionConverter geoprojectionconverter = new GeoProjectionConverter();
        LASwriteOpener laswriteopener = new LASwriteOpener();

        if (argc == 1) {
            System.err.println("missing argument");
            System.exit(1);
        }
        else
        {
            if (!geoprojectionconverter.parse(argc, argv)) byebye(true);
            if (!lasreadopener.parse(argc, argv)) byebye(true);
            if (!laswriteopener.parse(argc, argv)) byebye(true);
        }

        for (i = 1; i < argc; i++)
        {
            if (argv[i].isEmpty() || argv[i].charAt(0) == '\0')
            {
                continue;
            }
            else if (strcmp(argv[i],"-h") == 0 || strcmp(argv[i],"-help") == 0)
            {
                fprintf(stderr, "LAStools (by martin@rapidlasso.com) version %d\n", LasDefinitions.LAS_TOOLS_VERSION);
                usage();
            }
            else if (strcmp(argv[i],"-v") == 0 || strcmp(argv[i],"-verbose") == 0)
            {
                verbose = TRUE;
            }
            else if (strcmp(argv[i],"-version") == 0)
            {
                fprintf(stderr, "LAStools (by martin@rapidlasso.com) version %d\n", LasDefinitions.LAS_TOOLS_VERSION);
                byebye();
            }
            else if (strcmp(argv[i],"-gui") == 0)
            {
                fprintf(stderr, "WARNING: not compiled with GUI support. ignoring '-gui' ...\n");
            }
            else if (strcmp(argv[i],"-cores") == 0)
            {
                fprintf(stderr, "WARNING: not compiled with multi-core batching. ignoring '-cores' ...\n");
                i++;
            }
            else if (strcmp(argv[i],"-dry") == 0)
            {
                dry = TRUE;
            }
            else if (strcmp(argv[i],"-lax") == 0)
            {
                lax = TRUE;
            }
            else if (strcmp(argv[i],"-append") == 0)
            {
                append = TRUE;
            }
            else if (strcmp(argv[i],"-eop") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: char\n", argv[i]);
                    usage(true);
                }
                i++;
                end_of_points = atoi(argv[i]);
                if ((end_of_points < 0) || (end_of_points > 255))
                {
                    fprintf(stderr,"ERROR: end of points value needs to be between 0 and 255\n");
                    usage(true);
                }
            }
            else if (strcmp(argv[i],"-tile_size") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: size\n", argv[i]);
                    usage(true);
                }
                i++;
                tile_size = (float) atof(argv[i]);
            }
            else if (strcmp(argv[i],"-maximum") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: number\n", argv[i]);
                    usage(true);
                }
                i++;
                maximum_intervals = atoi(argv[i]);
            }
            else if (strcmp(argv[i],"-minimum") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: number\n", argv[i]);
                    usage(true);
                }
                i++;
                minimum_points = atoi(argv[i]);
            }
            else if (strcmp(argv[i],"-threshold") == 0)
            {
                if ((i+1) >= argc)
                {
                    fprintf(stderr,"ERROR: '%s' needs 1 argument: value\n", argv[i]);
                    usage(true);
                }
                i++;
                threshold = atoi(argv[i]);
            }
            else if (strcmp(argv[i],"-size") == 0)
            {
                report_file_size = true;
            }
            else if (strcmp(argv[i],"-check") == 0)
            {
                check_integrity = true;
            }
            else if (strcmp(argv[i],"-waveform") == 0 || strcmp(argv[i],"-waveforms") == 0)
            {
                waveform = true;
            }
            else if (strcmp(argv[i],"-waveform_with_map") == 0 || strcmp(argv[i],"-waveforms_with_map") == 0)
            {
                waveform = true;
                waveform_with_map = true;
            }
            else if ((argv[i].charAt(0) != '-') && (lasreadopener.get_file_name_number() == 0))
            {
                lasreadopener.add_file_name(argv[i]);
                argv[i] = "\0";
            }
            else
            {
                fprintf(stderr, "ERROR: cannot understand argument '%s'\n", argv[i]);
                usage(true);
            }
        }

        // check input

        if (!lasreadopener.active())
        {
            fprintf(stderr,"ERROR: no input specified\n");
            usage(true);
        }

        // check output

        if (laswriteopener.is_piped())
        {
            if (lax)
            {
                fprintf(stderr,"WARNING: disabling LAX generation for piped output\n");
                lax = FALSE;
                append = FALSE;
            }
        }

        // make sure we do not corrupt the input file

        if (lasreadopener.get_file_name() != null && laswriteopener.get_file_name() != null && (strcmp(lasreadopener.get_file_name(), laswriteopener.get_file_name()) == 0))
        {
            fprintf(stderr, "ERROR: input and output file name are identical\n");
            usage(true);
        }

        // check if projection info was set in the command line

        int number_of_keys = 0;
        GeoProjectionGeoKeys[] geo_keys = null;
        int num_geo_double_params = 0;
        double[] geo_double_params = null;

        if (geoprojectionconverter.has_projection())
        {
            projection_was_set = geoprojectionconverter.get_geo_keys_from_projection(number_of_keys, geo_keys, num_geo_double_params, geo_double_params);
        }

        // check if the output format was *not* specified in the command line

        format_not_specified = !laswriteopener.format_was_specified();

        if (verbose) total_start_time = taketime();

        // loop over multiple input files

        while (lasreadopener.active())
        {
            if (verbose) start_time = taketime();

            // open lasreader

            LASreader lasreader = lasreadopener.open();

            if (lasreader == null)
            {
                fprintf(stderr, "ERROR: could not open lasreader\n");
                usage(true);
            }

            // switch

            if (report_file_size)
            {
                // maybe only report uncompressed file size
                long uncompressed_file_size = lasreader.npoints * lasreader.header.point_data_record_length + lasreader.header.offset_to_point_data;
                if (uncompressed_file_size < Integer.MAX_VALUE)
                    fprintf(stderr,"uncompressed file size is %d bytes or %.2f MB for '%s'\n", uncompressed_file_size, (double)uncompressed_file_size/1024.0/1024.0, lasreadopener.get_file_name());
                else
                    fprintf(stderr,"uncompressed file size is %.2f MB or %.2f GB for '%s'\n", (double)uncompressed_file_size/1024.0/1024.0, (double)uncompressed_file_size/1024.0/1024.0/1024.0, lasreadopener.get_file_name());
            }
            else if (dry || check_integrity)
            {
                // maybe only a dry read pass
                start_time = taketime();
                while (lasreader.read_point());
                    //System.out.println( lasreader.point.toString() );

                if (check_integrity)
                {
                    if (lasreader.p_count != lasreader.npoints)
                    {
                        fprintf(stderr,"FAILED integrity check for '%s' after %lld of %lld points\n", lasreadopener.get_file_name(), lasreader.p_count, lasreader.npoints);
                    }
                    else
                    {
                        fprintf(stderr,"SUCCESS for '%s'\n", lasreadopener.get_file_name());
                    }
                }
                else
                {
                    fprintf(stderr,"needed %g secs to read '%s'\n", taketime()-start_time, lasreadopener.get_file_name());
                }
            }
            else
            {
                long start_of_waveform_data_packet_record = 0;

                // create output file name if no output was specified
                if (!laswriteopener.active())
                {
                    if (lasreadopener.get_file_name() == null)
                    {
                        fprintf(stderr,"ERROR: no output specified\n");
                        usage(true);
                    }
                    laswriteopener.set_force(TRUE);
                    if (format_not_specified)
                    {
                        if (lasreader.get_format() == LAS_TOOLS_FORMAT_LAZ)
                        {
                            laswriteopener.set_format(LAS_TOOLS_FORMAT_LAS);
                        }
                        else
                        {
                            laswriteopener.set_format(LAS_TOOLS_FORMAT_LAZ);
                        }
                    }
                    laswriteopener.make_file_name(lasreadopener.get_file_name(), -2);
                }

                // maybe set projection

                if (projection_was_set)
                {
                    lasreader.header.set_geo_keys(number_of_keys, geo_keys);
                    if (geo_double_params != null)
                    {
                        lasreader.header.set_geo_double_params(num_geo_double_params, geo_double_params);
                    }
                    else
                    {
                        lasreader.header.del_geo_double_params();
                    }
                    lasreader.header.del_geo_ascii_params();
                }

                // almost never open laswaveform13reader and laswaveform13writer (-:

                LASwaveform13reader laswaveform13reader = null;
                LASwaveform13writer laswaveform13writer = null;

                if (waveform)
                {
                    laswaveform13reader = lasreadopener.open_waveform13(lasreader.header);
                    if (laswaveform13reader != null)
                    {
                        // switch compression on/off
                        byte compression_type = (byte) (laswriteopener.get_format() == LAS_TOOLS_FORMAT_LAZ ? 1 : 0);
                        for (i = 0; i < 255; i++) if (lasreader.header.vlr_wave_packet_descr[i] != null) lasreader.header.vlr_wave_packet_descr[i].setCompressionType(compression_type);
                        // create laswaveform13writer
                        laswaveform13writer = laswriteopener.open_waveform13(lasreader.header);
                        if (laswaveform13writer == null)
                        {
                            laswaveform13reader = null;
                            waveform = false;
                            // switch compression on/off back
                            compression_type = (byte) (laswriteopener.get_format() == LAS_TOOLS_FORMAT_LAZ ? 0 : 1);
                            for (i = 0; i < 255; i++) if (lasreader.header.vlr_wave_packet_descr[i] != null) lasreader.header.vlr_wave_packet_descr[i].setCompressionType(compression_type);
                        }
                    }
                    else
                    {
                        waveform = false;
                    }
                }

                // special check for LAS 1.3+ files that contain waveform data

                if ((lasreader.header.version_major == 1) && (lasreader.header.version_minor >= 3))
                {
                    if ((lasreader.header.global_encoding & 2) != 0) // if bit # 1 is set we have internal waveform data
                    {
                        lasreader.header.global_encoding &= ~2; // remove internal bit
                        if (lasreader.header.start_of_waveform_data_packet_record != 0) // offset to
                        {
                            start_of_waveform_data_packet_record = lasreader.header.start_of_waveform_data_packet_record;
                            lasreader.header.start_of_waveform_data_packet_record = 0;
                            lasreader.header.global_encoding |= 4; // set external bit
                        }
                    }
                }

                long bytes_written = 0;

                // open laswriter

                LASwriter laswriter = laswriteopener.open(lasreader.header);

                if (laswriter == null)
                {
                    fprintf(stderr, "ERROR: could not open laswriter\n");
                    usage(true);
                }

                // should we also deal with waveform data

                if (waveform)
                {
                    byte compression_type = (byte) (laswaveform13reader.is_compressed() ? 1 : 0);
                    for (i = 0; i < 255; i++) if (lasreader.header.vlr_wave_packet_descr[i] != null) lasreader.header.vlr_wave_packet_descr[i].setCompressionType(compression_type);

                    long u_last_offset = 0;
                    long u_last_size = 60;
                    long u_new_offset = 0;
                    long u_new_size = 0;
                    int u_waves_written = 0;
                    int u_waves_referenced = 0;

                    Map<Long, OffsetSize> offset_size_map = new HashMap<>();

                    LASindex lasindex = new LASindex();
                    if (lax) // should we also create a spatial indexing file
                    {
                        // setup the quadtree
                        LASquadtree lasquadtree = new LASquadtree();
                        lasquadtree.setup(lasreader.header.min_x, lasreader.header.max_x, lasreader.header.min_y, lasreader.header.max_y, tile_size);

                        // create lax index
                        lasindex.prepare(lasquadtree, threshold);
                    }

                    // loop over points

                    while (lasreader.read_point())
                    {
                        if (lasreader.point.getWavepacketDescriptorIndex() != 0) // if point is attached to a waveform
                        {
                            u_waves_referenced++;
                            if (lasreader.point.getWavepacketOffsetToWaveformData()  == u_last_offset)
                            {
                                lasreader.point.setWavepacketOffsetToWaveformData(u_new_offset);
                                lasreader.point.setWavepacketPacketSize(u_new_size);
                            }
                            else if (lasreader.point.getWavepacketOffsetToWaveformData() > u_last_offset)
                            {
                                if (lasreader.point.getWavepacketOffsetToWaveformData() > (u_last_offset + u_last_size))
                                {
                                    if (!waveform_with_map)
                                    {
                                        fprintf(stderr,"WARNING: gap in waveform offsets.\n");
                                        fprintf(stderr,"WARNING: last offset plus size was %lld but new offset is %lld (for point %lld)\n", 
                                            (u_last_offset + u_last_size), 
                                            lasreader.point.getWavepacketOffsetToWaveformData() , lasreader.p_count);
                                    }
                                }
                                u_waves_written++;
                                u_last_offset = lasreader.point.getWavepacketOffsetToWaveformData();
                                u_last_size = lasreader.point.getWavepacketPacketSize();
                                laswaveform13reader.read_waveform(lasreader.point);
                                laswaveform13writer.write_waveform(lasreader.point, laswaveform13reader.samples);
                                u_new_offset = lasreader.point.getWavepacketOffsetToWaveformData();
                                u_new_size = lasreader.point.getWavepacketPacketSize();
                                if (waveform_with_map)
                                {
                                    offset_size_map.put(u_last_offset, new OffsetSize((int)u_new_offset,(int)u_new_size));
                                }
                            }
                            else
                            {
                                if (waveform_with_map)
                                {
                                    OffsetSize map_element = offset_size_map.get(lasreader.point.getWavepacketOffsetToWaveformData());
                                    if (map_element == null)
                                    {
                                        u_waves_written++;
                                        u_last_offset = lasreader.point.getWavepacketOffsetToWaveformData();
                                        u_last_size = lasreader.point.getWavepacketPacketSize();
                                        laswaveform13reader.read_waveform(lasreader.point);
                                        laswaveform13writer.write_waveform(lasreader.point, laswaveform13reader.samples);
                                        u_new_offset = lasreader.point.getWavepacketOffsetToWaveformData();
                                        u_new_size = lasreader.point.getWavepacketPacketSize();
                                        offset_size_map.put(u_last_offset, new OffsetSize((int)u_new_offset,(int)u_new_size));
                                    }
                                    else
                                    {
                                        lasreader.point.setWavepacketOffsetToWaveformData(map_element.offset);
                                        lasreader.point.setWavepacketPacketSize(map_element.size);
                                    }
                                }
                                else
                                {
                                    fprintf(stderr,"ERROR: waveform offsets not in monotonically increasing order.\n");
                                    fprintf(stderr,"ERROR: last offset was %lld but new offset is %lld (for point %lld)\n", 
                                        u_last_offset, lasreader.point.getWavepacketOffsetToWaveformData(), lasreader.p_count);
                                    fprintf(stderr,"ERROR: use option '-waveforms_with_map' to compress.\n");
                                    byebye(true);
                                }
                            }
                        }

                        if ( null != laswriter)
                            laswriter.write_point(lasreader.point);

                        if (lax)
                        {
                            lasindex.add(lasreader.point.get_x(), lasreader.point.get_y(), (int)(laswriter.p_count));
                        }
                        if (!lasreadopener.is_header_populated())
                        {
                            laswriter.update_inventory(lasreader.point);
                        }
                    }

                    if ( null != laswriter ) 
                    {
                        if (verbose && ((laswriter.p_count % 1000000) == 0)) fprintf(stderr,"written %d referenced %d of %d points\n", u_waves_written, u_waves_referenced, laswriter.p_count);

                        if (!lasreadopener.is_header_populated())
                        {
                            laswriter.update_header(lasreader.header, TRUE);
                        }

                        // flush the writer
                        bytes_written = laswriter.close();
                    }

                    if (lax)
                    {
                        // adaptive coarsening
                        lasindex.complete(minimum_points, maximum_intervals);

                        if (append)
                        {
                            // append lax to file
                            lasindex.append(laswriteopener.get_file_name());
                        }
                        else
                        {
                            // write lax to file
                            lasindex.write(laswriteopener.get_file_name());
                        }
                    }
                }
                else
                {
                    // loop over points

                    if (lasreadopener.is_header_populated())
                    {
                        if (lax) // should we also create a spatial indexing file
                        {
                            // setup the quadtree
                            LASquadtree lasquadtree = new LASquadtree();
                            lasquadtree.setup(lasreader.header.min_x, lasreader.header.max_x, lasreader.header.min_y, lasreader.header.max_y, tile_size);

                            // create lax index
                            LASindex lasindex = new LASindex();
                            lasindex.prepare(lasquadtree, threshold);

                            // compress points and add to index
                            while (lasreader.read_point())
                            {
                                lasindex.add(lasreader.point.get_x(), lasreader.point.get_y(), (int)(laswriter.p_count));
                                laswriter.write_point(lasreader.point);
                            }

                            // flush the writer
                            bytes_written = laswriter.close();

                            // adaptive coarsening
                            lasindex.complete(minimum_points, maximum_intervals);

                            if (append)
                            {
                                // append lax to file
                                lasindex.append(laswriteopener.get_file_name());
                            }
                            else
                            {
                                // write lax to file
                                lasindex.write(laswriteopener.get_file_name());
                            }
                        }
                        else
                        {
                            if (end_of_points > -1)
                            {
                                if (verbose) fprintf(stderr, "writing with end_of_points value %d\n", end_of_points);

                                while (lasreader.read_point())
                                {
                                    if ( null != laswriter)
                                    {
                                        laswriter.write_point(lasreader.point);
                                        laswriter.update_inventory(lasreader.point);
                                    }
                                }
                                if (null != laswriter)
                                    laswriter.update_header(lasreader.header, TRUE);
                            }
                            else
                            {
                                while (lasreader.read_point())
                                {
                                    if ( null != laswriter)
                                        laswriter.write_point(lasreader.point);
                                    
                                    System.out.println(lasreader.point.get_x()+" "+lasreader.point.get_y()+" ="+lasreader.point.get_z()+" "+lasreader.point.getIntensity()+" "+lasreader.point.getClassification());
                                }
                            }
                            if ( null != laswriter)
                            {
                                // flush the writer
                                bytes_written = laswriter.close();
                            }
                        }
                    }
                    else
                    {
                        if (lax && (lasreader.header.min_x < lasreader.header.max_x) && (lasreader.header.min_y < lasreader.header.max_y))
                        {
                            // setup the quadtree
                            LASquadtree lasquadtree = new LASquadtree();
                            lasquadtree.setup(lasreader.header.min_x, lasreader.header.max_x, lasreader.header.min_y, lasreader.header.max_y, tile_size);

                            // create lax index
                            LASindex lasindex = new LASindex();
                            lasindex.prepare(lasquadtree, threshold);

                            // compress points and add to index
                            while (lasreader.read_point())
                            {
                                lasindex.add(lasreader.point.get_x(), lasreader.point.get_y(), (int)(laswriter.p_count));
                                laswriter.write_point(lasreader.point);
                                laswriter.update_inventory(lasreader.point);
                            }

                            // flush the writer
                            bytes_written = laswriter.close();

                            // adaptive coarsening
                            lasindex.complete(minimum_points, maximum_intervals);

                            if (append)
                            {
                                // append lax to file
                                lasindex.append(laswriteopener.get_file_name());
                            }
                            else
                            {
                                // write lax to file
                                lasindex.write(laswriteopener.get_file_name());
                            }
                        }
                        else
                        {
                            if (end_of_points > -1)
                            {
                                if (verbose) fprintf(stderr, "writing with end_of_points value %d\n", end_of_points);

                                while (lasreader.read_point())
                                {
                                    if ( null != laswriter )
                                    {
                                        laswriter.write_point(lasreader.point);
                                        laswriter.update_inventory(lasreader.point);
                                    }
                                }
                            }
                            else
                            {
                                while (lasreader.read_point())
                                {
                                    if ( null != laswriter )
                                    {
                                        laswriter.write_point(lasreader.point);
                                        laswriter.update_inventory(lasreader.point);
                                    }
                                }
                            }
                        }

                        if ( null != laswriter )
                        {
                            // update the header
                            laswriter.update_header(lasreader.header, TRUE);

                            // flush the writer
                            bytes_written = laswriter.close();
                        }
                    }
                }

                if (verbose) fprintf(stderr,"%g secs to write %lld bytes for '%s' with %lld points of type %d\n", taketime()-start_time, bytes_written, laswriteopener.get_file_name(), lasreader.p_count, lasreader.header.point_data_format);

                if (start_of_waveform_data_packet_record != 0 && !waveform)
                {
                    lasreader.close(FALSE);
                    ByteStreamIn stream = lasreader.get_stream();
                    stream.seek(start_of_waveform_data_packet_record);
                    char[] wave_form_file_name;
                    if (laswriteopener.get_file_name() != null)
                    {
                        wave_form_file_name = laswriteopener.get_file_name().toCharArray();
                        int len = wave_form_file_name.length;
                        if (wave_form_file_name[len-3] == 'L')
                        {
                            wave_form_file_name[len-3] = 'W';
                            wave_form_file_name[len-2] = 'D';
                            wave_form_file_name[len-1] = 'P';
                        }
                        else
                        {
                            wave_form_file_name[len-3] = 'w';
                            wave_form_file_name[len-2] = 'd';
                            wave_form_file_name[len-1] = 'p';
                        }
                    }
                    else
                    {
                        wave_form_file_name = "wave_form.wdp".toCharArray();
                    }
                    OutputStream file = Cstdio.fopen(wave_form_file_name, "wb");
                    if (file != null)
                    {
                        if (verbose) fprintf(stderr,"writing waveforms to '%s'\n", new String(wave_form_file_name));
                        try
                        {
                            int b;
                            while (true)
                            {
                                b = stream.getByte();
                                Cstdio.fputc(b, file);
                            }
                        }
                        catch (Exception e)
                        {
                            Cstdio.fclose(file);
                        }
                    }
                }

                laswriteopener.set_file_name(null);
                if (format_not_specified)
                {
                    laswriteopener.set_format(0);
                }
            }

            lasreader.close();
        }

        if (verbose && lasreadopener.get_file_name_number() > 1) fprintf(stderr,"needed %g sec for %d files\n", taketime()-total_start_time, lasreadopener.get_file_name_number());
    }

    private static double taketime() {
        return System.currentTimeMillis() / 1000;
    }

    private static void usage() {
        usage(false);
    }

    private static void usage(boolean error)
    {
        PrintStream stderr = System.err;
        fprintf(stderr,"usage:\n");
        fprintf(stderr,"laszip *.las\n");
        fprintf(stderr,"laszip *.laz\n");
        fprintf(stderr,"laszip *.txt -iparse xyztiarn\n");
        fprintf(stderr,"laszip lidar.las\n");
        fprintf(stderr,"laszip lidar.laz -v\n");
        fprintf(stderr,"laszip -i lidar.las -o lidar_zipped.laz\n");
        fprintf(stderr,"laszip -i lidar.laz -o lidar_unzipped.las\n");
        fprintf(stderr,"laszip -i lidar.las -stdout -olaz > lidar.laz\n");
        fprintf(stderr,"laszip -stdin -o lidar.laz < lidar.las\n");
        fprintf(stderr,"laszip -h\n");
        byebye(error);
    }

    private static void byebye() {
        byebye(false);
    }

    private static void byebye(boolean error)
    {
        System.exit(error ? 1 : 0);
    }

}
