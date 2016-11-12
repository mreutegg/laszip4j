/*
 * Copyright 2007-2014, martin isenburg, rapidlasso - fast tools to catch reality
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

import java.io.PrintStream;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atof;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.atoi;
import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASfilter {

    private static final PrintStream stderr = System.err;

    private int num_criteria; // unsigned
    private int alloc_criteria; // unsigned
    private LAScriterion[] criteria;
    private int[] counters;

    void clean()
        {
        alloc_criteria = 0;
        num_criteria = 0;
        criteria = null;
        counters = null;
    }

    static void usage()
    {
        fprintf(stderr,"Filter points based on their coordinates.\n");
        fprintf(stderr,"  -keep_tile 631000 4834000 1000 (ll_x ll_y size)\n");
        fprintf(stderr,"  -keep_circle 630250.00 4834750.00 100 (x y radius)\n");
        fprintf(stderr,"  -keep_xy 630000 4834000 631000 4836000 (min_x min_y max_x max_y)\n");
        fprintf(stderr,"  -drop_xy 630000 4834000 631000 4836000 (min_x min_y max_x max_y)\n");
        fprintf(stderr,"  -keep_x 631500.50 631501.00 (min_x max_x)\n");
        fprintf(stderr,"  -drop_x 631500.50 631501.00 (min_x max_x)\n");
        fprintf(stderr,"  -drop_x_below 630000.50 (min_x)\n");
        fprintf(stderr,"  -drop_x_above 630500.50 (max_x)\n");
        fprintf(stderr,"  -keep_y 4834500.25 4834550.25 (min_y max_y)\n");
        fprintf(stderr,"  -drop_y 4834500.25 4834550.25 (min_y max_y)\n");
        fprintf(stderr,"  -drop_y_below 4834500.25 (min_y)\n");
        fprintf(stderr,"  -drop_y_above 4836000.75 (max_y)\n");
        fprintf(stderr,"  -keep_z 11.125 130.725 (min_z max_z)\n");
        fprintf(stderr,"  -drop_z 11.125 130.725 (min_z max_z)\n");
        fprintf(stderr,"  -drop_z_below 11.125 (min_z)\n");
        fprintf(stderr,"  -drop_z_above 130.725 (max_z)\n");
        fprintf(stderr,"  -keep_xyz 620000 4830000 100 621000 4831000 200 (min_x min_y min_z max_x max_y max_z)\n");
        fprintf(stderr,"  -drop_xyz 620000 4830000 100 621000 4831000 200 (min_x min_y min_z max_x max_y max_z)\n");
        fprintf(stderr,"Filter points based on their return number.\n");
        fprintf(stderr,"  -keep_first -first_only -drop_first\n");
        fprintf(stderr,"  -keep_last -last_only -drop_last\n");
        fprintf(stderr,"  -keep_first_of_many -keep_last_of_many\n");
        fprintf(stderr,"  -drop_first_of_many -drop_last_of_many\n");
        fprintf(stderr,"  -keep_middle -drop_middle\n");
        fprintf(stderr,"  -keep_return 1 2 3\n");
        fprintf(stderr,"  -drop_return 3 4\n");
        fprintf(stderr,"  -keep_single -drop_single\n");
        fprintf(stderr,"  -keep_double -drop_double\n");
        fprintf(stderr,"  -keep_triple -drop_triple\n");
        fprintf(stderr,"  -keep_quadruple -drop_quadruple\n");
        fprintf(stderr,"  -keep_quintuple -drop_quintuple\n");
        fprintf(stderr,"Filter points based on the scanline flags.\n");
        fprintf(stderr,"  -drop_scan_direction 0\n");
        fprintf(stderr,"  -keep_scan_direction_change\n");
        fprintf(stderr,"  -keep_edge_of_flight_line\n");
        fprintf(stderr,"Filter points based on their intensity.\n");
        fprintf(stderr,"  -keep_intensity 20 380\n");
        fprintf(stderr,"  -drop_intensity_below 20\n");
        fprintf(stderr,"  -drop_intensity_above 380\n");
        fprintf(stderr,"  -drop_intensity_between 4000 5000\n");
        fprintf(stderr,"Filter points based on classifications or flags.\n");
        fprintf(stderr,"  -keep_class 1 3 7\n");
        fprintf(stderr,"  -drop_class 4 2\n");
        fprintf(stderr,"  -keep_extended_class 43\n");
        fprintf(stderr,"  -drop_extended_class 129 135\n");
        fprintf(stderr,"  -drop_synthetic -keep_synthetic\n");
        fprintf(stderr,"  -drop_keypoint -keep_keypoint\n");
        fprintf(stderr,"  -drop_withheld -keep_withheld\n");
        fprintf(stderr,"  -drop_overlap -keep_overlap\n");
        fprintf(stderr,"Filter points based on their user data.\n");
        fprintf(stderr,"  -keep_user_data 1\n");
        fprintf(stderr,"  -drop_user_data 255\n");
        fprintf(stderr,"  -keep_user_data_below 50\n");
        fprintf(stderr,"  -keep_user_data_above 150\n");
        fprintf(stderr,"  -keep_user_data_between 10 20\n");
        fprintf(stderr,"  -drop_user_data_below 1\n");
        fprintf(stderr,"  -drop_user_data_above 100\n");
        fprintf(stderr,"  -drop_user_data_between 10 40\n");
        fprintf(stderr,"Filter points based on their point source ID.\n");
        fprintf(stderr,"  -keep_point_source 3\n");
        fprintf(stderr,"  -keep_point_source_between 2 6\n");
        fprintf(stderr,"  -drop_point_source 27\n");
        fprintf(stderr,"  -drop_point_source_below 6\n");
        fprintf(stderr,"  -drop_point_source_above 15\n");
        fprintf(stderr,"  -drop_point_source_between 17 21\n");
        fprintf(stderr,"Filter points based on their scan angle.\n");
        fprintf(stderr,"  -keep_scan_angle -15 15\n");
        fprintf(stderr,"  -drop_abs_scan_angle_above 15\n");
        fprintf(stderr,"  -drop_abs_scan_angle_below 1\n");
        fprintf(stderr,"  -drop_scan_angle_below -15\n");
        fprintf(stderr,"  -drop_scan_angle_above 15\n");
        fprintf(stderr,"  -drop_scan_angle_between -25 -23\n");
        fprintf(stderr,"Filter points based on their gps time.\n");
        fprintf(stderr,"  -keep_gps_time 11.125 130.725\n");
        fprintf(stderr,"  -drop_gps_time_below 11.125\n");
        fprintf(stderr,"  -drop_gps_time_above 130.725\n");
        fprintf(stderr,"  -drop_gps_time_between 22.0 48.0\n");
        fprintf(stderr,"Filter points based on their RGB/NIR channel.\n");
        fprintf(stderr,"  -keep_RGB_red 1 1\n");
        fprintf(stderr,"  -keep_RGB_green 30 100\n");
        fprintf(stderr,"  -keep_RGB_blue 0 0\n");
        fprintf(stderr,"  -keep_RGB_nir 64 127\n");
        fprintf(stderr,"Filter points based on their wavepacket.\n");
        fprintf(stderr,"  -keep_wavepacket 0\n");
        fprintf(stderr,"  -drop_wavepacket 3\n");
        fprintf(stderr,"Filter points with simple thinning.\n");
        fprintf(stderr,"  -keep_every_nth 2\n");
        fprintf(stderr,"  -keep_random_fraction 0.1\n");
        fprintf(stderr,"  -thin_with_grid 1.0\n");
        fprintf(stderr,"  -thin_with_time 0.001\n");
        fprintf(stderr,"Boolean combination of filters.\n");
        fprintf(stderr,"  -filter_and\n");
    }

    public boolean parse(int argc, String argv[])
    {
        int i;

        int keep_return_mask = 0; // unsigned
        int drop_return_mask = 0; // unsigned

        int keep_classification_mask = 0; // unsigned
        int drop_classification_mask = 0; // unsigned

        int[] keep_extended_classification_mask = {0, 0, 0, 0, 0, 0, 0, 0}; // unsigned
        int[] drop_extended_classification_mask = {0, 0, 0, 0, 0, 0, 0, 0}; // unsigned

        for (i = 1; i < argc; i++)
        {
            if (argv[i].isEmpty())
            {
                continue;
            }
            else if (strcmp(argv[i],"-h") == 0 || strcmp(argv[i],"-help") == 0)
            {
                usage();
                return TRUE;
            }
            else if (strncmp(argv[i],"-clip_", 6) == 0)
            {
                if (strcmp(argv[i], "-clip_z_below") == 0)
                {
                    fprintf(stderr,"WARNING: '%s' will not be supported in the future. check documentation with '-h'.\n", argv[i]);
                    fprintf(stderr,"  rename '-clip_z_below' to '-drop_z_below'.\n");
                    fprintf(stderr,"  rename '-clip_z_above' to '-drop_z_above'.\n");
                    fprintf(stderr,"  rename '-clip_z_between' to '-drop_z'.\n");
                    fprintf(stderr,"  rename '-clip' to '-keep_xy'.\n");
                    fprintf(stderr,"  rename '-clip_tile' to '-keep_tile'.\n");
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: max_z\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionDropzBelow(atof(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if (strcmp(argv[i], "-clip_z_above") == 0)
                {
                    fprintf(stderr,"WARNING: '%s' will not be supported in the future. check documentation with '-h'.\n", argv[i]);
                    fprintf(stderr,"  rename '-clip_z_below' to '-drop_z_below'.\n");
                    fprintf(stderr,"  rename '-clip_z_above' to '-drop_z_above'.\n");
                    fprintf(stderr,"  rename '-clip_z_between' to '-drop_z'.\n");
                    fprintf(stderr,"  rename '-clip' to '-keep_xy'.\n");
                    fprintf(stderr,"  rename '-clip_tile' to '-keep_tile'.\n");
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: max_z\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionDropzAbove(atof(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if ((strcmp(argv[i], "-clip_to_bounding_box") != 0) && (strcmp(argv[i],"-clip_to_bb") != 0))
                {
                    fprintf(stderr,"ERROR: '%s' is no longer recognized. check documentation with '-h'.\n", argv[i]);
                    fprintf(stderr,"  rename '-clip' to '-keep_xy'.\n");
                    fprintf(stderr,"  rename '-clip_box' to '-keep_xyz'.\n");
                    fprintf(stderr,"  rename '-clip_tile' to '-keep_tile'.\n");
                    fprintf(stderr,"  rename '-clip_z_below' to '-drop_z_below'.\n");
                    fprintf(stderr,"  rename '-clip_z_above' to '-drop_z_above'.\n");
                    fprintf(stderr,"  rename '-clip_z_between' to '-drop_z'.\n");
                    fprintf(stderr,"  etc ...\n");
                    return FALSE;
                }
            }
            else if (strncmp(argv[i],"-keep_", 6) == 0)
            {
                if (strncmp(argv[i],"-keep_first", 11) == 0)
                {
                    if (strcmp(argv[i],"-keep_first") == 0)
                    {
                        add_criterion(new LAScriterionKeepFirstReturn());
                        argv[i]="";
                    }
                    else if (strcmp(argv[i],"-keep_first_of_many") == 0)
                    {
                        add_criterion(new LAScriterionKeepFirstOfManyReturn());
                        argv[i]="";
                    }
                }
                else if (strcmp(argv[i],"-keep_middle") == 0)
                {
                    add_criterion(new LAScriterionKeepMiddleReturn());
                    argv[i]="";
                }
                else if (strncmp(argv[i],"-keep_last", 10) == 0)
                {
                    if (strcmp(argv[i],"-keep_last") == 0)
                    {
                        add_criterion(new LAScriterionKeepLastReturn());
                        argv[i]="";
                    }
                    else if (strcmp(argv[i],"-keep_last_of_many") == 0)
                    {
                        add_criterion(new LAScriterionKeepLastOfManyReturn());
                        argv[i]="";
                    }
                }
                else if (strncmp(argv[i],"-keep_class", 11) == 0)
                {
                    if (strcmp(argv[i],"-keep_classification") == 0 || strcmp(argv[i],"-keep_class") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 at least argument: classification\n", argv[i]);
                            return FALSE;
                        }
                        argv[i]="";
                        i+=1;
                        do
                        {
                            if (atoi(argv[i]) > 31)
                            {
                                fprintf(stderr,"ERROR: cannot keep classification %d because it is larger than 31\n", atoi(argv[i]));
                                return FALSE;
                            }
                            else if (atoi(argv[i]) < 0)
                            {
                                fprintf(stderr,"ERROR: cannot keep classification %d because it is smaller than 0\n", atoi(argv[i]));
                                return FALSE;
                            }
                            keep_classification_mask |= (1 << atoi(argv[i]));
                            argv[i]="";
                            i+=1;
                        } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                        i-=1;
                    }
                }
                else if (strncmp(argv[i],"-keep_extended_", 15) == 0)
                {
                    if (strcmp(argv[i],"-keep_extended_classification") == 0 || strcmp(argv[i],"-keep_extended_class") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 at least argument: classification\n", argv[i]);
                            return FALSE;
                        }
                        argv[i]="";
                        i+=1;
                        do
                        {
                            if (atoi(argv[i]) > 255)
                            {
                                fprintf(stderr,"ERROR: cannot keep extended classification %d because it is larger than 255\n", atoi(argv[i]));
                                return FALSE;
                            }
                            else if (atoi(argv[i]) < 0)
                            {
                                fprintf(stderr,"ERROR: cannot keep extended classification %d because it is smaller than 0\n", atoi(argv[i]));
                                return FALSE;
                            }
                            keep_extended_classification_mask[atoi(argv[i])/32] |= (1 << (atoi(argv[i]) - (32*(atoi(argv[i])/32))));
                            argv[i]="";
                            i+=1;
                        } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                        i-=1;
                    }
                }
                else if (strncmp(argv[i],"-keep_x", 7) == 0)
                {
                    if (strcmp(argv[i],"-keep_xy") == 0)
                    {
                        if ((i+4) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 4 arguments: min_x min_y max_x max_y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepxy(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3]), atof(argv[i+4])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; argv[i+4]=""; i+=4;
                    }
                    else if (strcmp(argv[i],"-keep_xyz") == 0)
                    {
                        if ((i+6) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 6 arguments: min_x min_y min_z max_x max_y max_z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepxyz(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3]), atof(argv[i+4]), atof(argv[i+5]), atof(argv[i+6])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; argv[i+4]=""; argv[i+5]=""; argv[i+6]=""; i+=6;
                    }
                    else if (strcmp(argv[i],"-keep_x") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_x max_x\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepx(atof(argv[i+1]), atof(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strcmp(argv[i],"-keep_y") == 0)
                {
                    if ((i+2) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_y max_y\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepy(atof(argv[i+1]), atof(argv[i+2])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                }
                else if (strcmp(argv[i],"-keep_z") == 0)
                {
                    if ((i+2) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_z max_z\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepz(atof(argv[i+1]), atof(argv[i+2])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                }
                else if (strncmp(argv[i],"-keep_X", 7) == 0)
                {
                    if (strcmp(argv[i],"-keep_XY") == 0)
                    {
                        if ((i+4) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 4 arguments: min_X min_Y max_X max_Y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepXY(atoi(argv[i+1]), atoi(argv[i+2]), atoi(argv[i+3]), atoi(argv[i+4])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; argv[i+4]=""; i+=4;
                    }
                    else if (strcmp(argv[i],"-keep_X") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_X max_X\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepX(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strcmp(argv[i],"-keep_Y") == 0)
                {
                    if ((i+2) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_Y max_Y\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepY(atoi(argv[i+1]), atoi(argv[i+2])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                }
                else if (strcmp(argv[i],"-keep_Z") == 0)
                {
                    if ((i+2) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_Z max_Z\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepZ(atoi(argv[i+1]), atoi(argv[i+2])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                }
                else if (strcmp(argv[i],"-keep_tile") == 0)
                {
                    if ((i+3) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 3 arguments: llx lly size\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepTile((float)atof(argv[i+1]), (float)atof(argv[i+2]), (float)atof(argv[i+3])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; i+=3;
                }
                else if (strcmp(argv[i],"-keep_circle") == 0)
                {
                    if ((i+3) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 3 arguments: center_x center_y radius\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepCircle(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; i+=3;
                }
                else if (strcmp(argv[i],"-keep_return") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs at least 1 argument: return_number\n", argv[i]);
                        return FALSE;
                    }
                    argv[i]="";
                    i+=1;
                    do
                    {
                        keep_return_mask |= (1 << atoi(argv[i]));
                        argv[i]="";
                        i+=1;
                    } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                    i-=1;
                }
                else if (strcmp(argv[i],"-keep_return_mask") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: return_mask\n", argv[i]);
                        return FALSE;
                    }
                    keep_return_mask = atoi(argv[i+1]);
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if (strcmp(argv[i],"-keep_single") == 0)
                {
                    add_criterion(new LAScriterionKeepSpecificNumberOfReturns(1));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_double") == 0)
                {
                    add_criterion(new LAScriterionKeepSpecificNumberOfReturns(2));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_triple") == 0)
                {
                    add_criterion(new LAScriterionKeepSpecificNumberOfReturns(3));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_quadruple") == 0)
                {
                    add_criterion(new LAScriterionKeepSpecificNumberOfReturns(4));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_quintuple") == 0)
                {
                    add_criterion(new LAScriterionKeepSpecificNumberOfReturns(5));
                    argv[i]="";
                }
                else if (strncmp(argv[i],"-keep_intensity", 15) == 0)
                {
                    if (strcmp(argv[i],"-keep_intensity") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepIntensity(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-keep_intensity_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepIntensityAbove(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-keep_intensity_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepIntensityBelow(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-keep_RGB_", 10) == 0)
                {
                    if (strcmp(argv[i]+10,"red") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepRGB(atoi(argv[i+1]), atoi(argv[i+2]), 0));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i]+10,"green") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepRGB(atoi(argv[i+1]), atoi(argv[i+2]), 1));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i]+10,"blue") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepRGB(atoi(argv[i+1]), atoi(argv[i+2]), 2));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i]+10,"nir") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepRGB(atoi(argv[i+1]), atoi(argv[i+2]), 3));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strcmp(argv[i],"-keep_scan_angle") == 0)
                {
                    if ((i+2) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepScanAngle(atoi(argv[i+1]), atoi(argv[i+2])));
                    argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                }
                else if (strcmp(argv[i],"-keep_synthetic") == 0)
                {
                    add_criterion(new LAScriterionKeepSynthetic());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_keypoint") == 0)
                {
                    add_criterion(new LAScriterionKeepKeypoint());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_withheld") == 0)
                {
                    add_criterion(new LAScriterionKeepWithheld());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_overlap") == 0)
                {
                    add_criterion(new LAScriterionKeepOverlap());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_wavepacket") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: index\n", argv[i]);
                        return FALSE;
                    }
                    argv[i]="";
                    i+=1;
                    add_criterion(new LAScriterionKeepWavepacket(atoi(argv[i])));
                    argv[i]="";
                }
                else if (strncmp(argv[i],"-keep_user_data", 15) == 0)
                {
                    if (strcmp(argv[i],"-keep_user_data") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepUserData((byte) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-keep_user_data_below") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepUserDataBelow((byte) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-keep_user_data_above") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepUserDataAbove((byte) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-keep_user_data_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_value max_value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepUserDataBetween((byte) atoi(argv[i+1]), (byte) atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strncmp(argv[i],"-keep_point_source", 18) == 0)
                {
                    if (strcmp(argv[i],"-keep_point_source") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: ID\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepPointSource((char) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-keep_point_source_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_ID max_ID\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepPointSourceBetween((char) atoi(argv[i+1]), (char) atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strncmp(argv[i],"-keep_gps", 9) == 0)
                {
                    if (strcmp(argv[i],"-keep_gps_time") == 0 || strcmp(argv[i],"-keep_gpstime") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionKeepGpsTime(atof(argv[i+1]), atof(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strcmp(argv[i],"-keep_every_nth") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: nth\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepEveryNth(atoi(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if (strcmp(argv[i],"-keep_random_fraction") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: fraction\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionKeepRandomFraction((float)atof(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if (strcmp(argv[i],"-keep_scan_direction_change") == 0)
                {
                    add_criterion(new LAScriterionKeepScanDirectionChange());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-keep_edge_of_flight_line") == 0)
                {
                    add_criterion(new LAScriterionKeepEdgeOfFlightLine());
                    argv[i]="";
                }
            }
            else if (strncmp(argv[i],"-drop_", 6) == 0)
            {
                if (strncmp(argv[i],"-drop_first", 11) == 0)
                {
                    if (strcmp(argv[i],"-drop_first") == 0)
                    {
                        add_criterion(new LAScriterionDropFirstReturn());
                        argv[i]="";
                    }
                    else if (strcmp(argv[i],"-drop_first_of_many") == 0)
                    {
                        add_criterion(new LAScriterionDropFirstOfManyReturn());
                        argv[i]="";
                    }
                }
                else if (strncmp(argv[i],"-drop_last", 10) == 0)
                {
                    if (strcmp(argv[i],"-drop_last") == 0)
                    {
                        add_criterion(new LAScriterionDropLastReturn());
                        argv[i]="";
                    }
                    else if (strcmp(argv[i],"-drop_last_of_many") == 0)
                    {
                        add_criterion(new LAScriterionDropLastOfManyReturn());
                        argv[i]="";
                    }
                }
                else if (strcmp(argv[i],"-drop_middle") == 0)
                {
                    add_criterion(new LAScriterionDropMiddleReturn());
                    argv[i]="";
                }
                else if (strncmp(argv[i],"-drop_class", 11) == 0)
                {
                    if (strcmp(argv[i],"-drop_classification") == 0 || strcmp(argv[i],"-drop_class") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs at least 1 argument: classification\n", argv[i]);
                            return FALSE;
                        }
                        argv[i]="";
                        i+=1;
                        do
                        {
                            if (atoi(argv[i]) > 31)
                            {
                                fprintf(stderr,"ERROR: cannot drop classification %d because it is larger than 31\n", atoi(argv[i]));
                                return FALSE;
                            }
                            else if (atoi(argv[i]) < 0)
                            {
                                fprintf(stderr,"ERROR: cannot drop classification %d because it is smaller than 0\n", atoi(argv[i]));
                                return FALSE;
                            }
                            drop_classification_mask |= (1 << atoi(argv[i]));
                            argv[i]="";
                            i+=1;
                        } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                        i-=1;
                    }
                    else if (strcmp(argv[i],"-drop_classification_mask") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: mask\n", argv[i]);
                            return FALSE;
                        }
                        drop_classification_mask = atoi(argv[i+1]);
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_extended_", 15) == 0)
                {
                    if (strcmp(argv[i],"-drop_extended_classification") == 0 || strcmp(argv[i],"-drop_extended_class") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs at least 1 argument: classification\n", argv[i]);
                            return FALSE;
                        }
                        argv[i]="";
                        i+=1;
                        do
                        {
                            if (atoi(argv[i]) > 255)
                            {
                                fprintf(stderr,"ERROR: cannot drop extended classification %d because it is larger than 255\n", atoi(argv[i]));
                                return FALSE;
                            }
                            else if (atoi(argv[i]) < 0)
                            {
                                fprintf(stderr,"ERROR: cannot drop extended classification %d because it is smaller than 0\n", atoi(argv[i]));
                                return FALSE;
                            }
                            drop_extended_classification_mask[atoi(argv[i])/32] |= (1 << (atoi(argv[i]) - (32*(atoi(argv[i])/32))));
                            argv[i]="";
                            i+=1;
                        } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                        i-=1;
                    }
                    else if (strcmp(argv[i],"-drop_extended_classification_mask") == 0)
                    {
                        if ((i+8) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 8 arguments: mask7 mask6 mask5 mask4 mask3 mask2 mask1 mask0\n", argv[i]);
                            return FALSE;
                        }
                        drop_extended_classification_mask[7] = atoi(argv[i+1]);
                        drop_extended_classification_mask[6] = atoi(argv[i+2]);
                        drop_extended_classification_mask[5] = atoi(argv[i+3]);
                        drop_extended_classification_mask[4] = atoi(argv[i+4]);
                        drop_extended_classification_mask[3] = atoi(argv[i+5]);
                        drop_extended_classification_mask[2] = atoi(argv[i+6]);
                        drop_extended_classification_mask[1] = atoi(argv[i+7]);
                        drop_extended_classification_mask[0] = atoi(argv[i+8]);
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; argv[i+4]=""; argv[i+5]=""; argv[i+6]=""; argv[i+7]=""; argv[i+8]=""; i+=8;
                    }
                }
                else if (strncmp(argv[i],"-drop_x", 7) == 0)
                {
                    if (strcmp(argv[i],"-drop_xy") == 0)
                    {
                        if ((i+4) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 4 arguments: min_x min_y max_x max_y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropxy(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3]), atof(argv[i+4])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; argv[i+4]=""; i+=4;
                    }
                    else if (strcmp(argv[i],"-drop_xyz") == 0)
                    {
                        if ((i+6) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 6 arguments: min_x min_y min_z max_x max_y max_z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropxyz(atof(argv[i+1]), atof(argv[i+2]), atof(argv[i+3]), atof(argv[i+4]), atof(argv[i+5]), atof(argv[i+6])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; argv[i+3]=""; argv[i+4]=""; argv[i+5]=""; argv[i+6]=""; i+=6;
                    }
                    else if (strcmp(argv[i],"-drop_x") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_x max_x\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropx(atof(argv[i+1]), atof(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-drop_x_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_x\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropxBelow(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_x_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_x\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropxAbove(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_y", 7) == 0)
                {
                    if (strcmp(argv[i],"-drop_y") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_y max_y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropy(atof(argv[i+1]), atof(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-drop_y_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropyBelow(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_y_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropyAbove(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_z", 7) == 0)
                {
                    if (strcmp(argv[i],"-drop_z") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_z max_z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropz(atof(argv[i+1]), atof(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-drop_z_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropzBelow(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_z_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropzAbove(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_X", 7) == 0)
                {
                    if (strcmp(argv[i],"-drop_X") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_X max_X\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropX(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-drop_X_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_X\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropXBelow(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_X_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_X\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropXAbove(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_Y", 7) == 0)
                {
                    if (strcmp(argv[i],"-drop_Y") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_Y max_Y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropY(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-drop_Y_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_Y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropYBelow(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_Y_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_Y\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropYAbove(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_Z", 7) == 0)
                {
                    if (strcmp(argv[i],"-drop_Z") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_Z max_Z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropZ(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                    else if (strcmp(argv[i],"-drop_Z_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_Z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropZBelow(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_Z_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_Z\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropZAbove(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strcmp(argv[i],"-drop_return") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs at least 1 argument: return_number\n", argv[i]);
                        return FALSE;
                    }
                    argv[i]="";
                    i+=1;
                    do
                    {
                        drop_return_mask |= (1 << atoi(argv[i]));
                        argv[i]="";
                        i+=1;
                    } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                    i-=1;
                }
                else if (strcmp(argv[i],"-drop_single") == 0)
                {
                    add_criterion(new LAScriterionDropSpecificNumberOfReturns(1));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_double") == 0)
                {
                    add_criterion(new LAScriterionDropSpecificNumberOfReturns(2));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_triple") == 0)
                {
                    add_criterion(new LAScriterionDropSpecificNumberOfReturns(3));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_quadruple") == 0)
                {
                    add_criterion(new LAScriterionDropSpecificNumberOfReturns(4));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_quintuple") == 0)
                {
                    add_criterion(new LAScriterionDropSpecificNumberOfReturns(5));
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_scan_direction") == 0)
                {
                    add_criterion(new LAScriterionDropScanDirection(atoi(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if (strncmp(argv[i],"-drop_intensity_above",15) == 0)
                {
                    if (strcmp(argv[i],"-drop_intensity_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropIntensityAbove(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_intensity_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropIntensityBelow(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_intensity_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropIntensityBetween(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strncmp(argv[i],"-drop_abs_scan_angle_",21) == 0)
                {
                    if (strcmp(argv[i],"-drop_abs_scan_angle_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max\n", argv[i]);
                            return FALSE;
                        }
                        int angle = atoi(argv[i+1]);
                        add_criterion(new LAScriterionKeepScanAngle(-angle, angle));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_abs_scan_angle_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min\n", argv[i]);
                            return FALSE;
                        }
                        int angle = atoi(argv[i+1]);
                        add_criterion(new LAScriterionDropScanAngleBetween(-angle+1, angle-1));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                }
                else if (strncmp(argv[i],"-drop_scan_angle_",17) == 0)
                {
                    if (strcmp(argv[i],"-drop_scan_angle_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropScanAngleAbove(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_scan_angle_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropScanAngleBelow(atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_scan_angle_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropScanAngleBetween(atoi(argv[i+1]), atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strcmp(argv[i],"-drop_synthetic") == 0)
                {
                    add_criterion(new LAScriterionDropSynthetic());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_keypoint") == 0)
                {
                    add_criterion(new LAScriterionDropKeypoint());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_withheld") == 0)
                {
                    add_criterion(new LAScriterionDropWithheld());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_overlap") == 0)
                {
                    add_criterion(new LAScriterionDropOverlap());
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-drop_wavepacket") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: index\n", argv[i]);
                        return FALSE;
                    }
                    argv[i]="";
                    i+=1;
                    add_criterion(new LAScriterionDropWavepacket(atoi(argv[i])));
                    argv[i]="";
                }
                else if (strncmp(argv[i],"-drop_user_data", 15) == 0)
                {
                    if (strcmp(argv[i],"-drop_user_data") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs at least 1 argument: ID\n", argv[i]);
                            return FALSE;
                        }
                        argv[i]="";
                        i+=1;
                        do
                        {
                            add_criterion(new LAScriterionDropUserData((byte) atoi(argv[i])));
                            argv[i]="";
                            i+=1;
                        } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                        i-=1;
                    }
                    else if (strcmp(argv[i],"-drop_user_data_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropUserDataBelow((byte) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_user_data_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropUserDataAbove((byte) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_user_data_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_value max_value\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropUserDataBetween((byte) atoi(argv[i+1]), (byte) atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strncmp(argv[i],"-drop_point_source", 18) == 0)
                {
                    if (strcmp(argv[i],"-drop_point_source") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs at least 1 argument: ID\n", argv[i]);
                            return FALSE;
                        }
                        argv[i]="";
                        i+=1;
                        do
                        {
                            add_criterion(new LAScriterionDropPointSource((char) atoi(argv[i])));
                            argv[i]="";
                            i+=1;
                        } while ((i < argc) && ('0' <= asChar(argv[i])) && (asChar(argv[i]) <= '9'));
                        i-=1;
                    }
                    else if (strcmp(argv[i],"-drop_point_source_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_ID\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropPointSourceBelow((char) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_point_source_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_ID\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropPointSourceAbove((char) atoi(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_point_source_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min_ID max_ID\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropPointSourceBetween((char) atoi(argv[i+1]), (char) atoi(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
                else if (strncmp(argv[i],"-drop_gps", 9) == 0)
                {
                    if (strcmp(argv[i],"-drop_gps_time_above") == 0 || strcmp(argv[i],"-drop_gpstime_above") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: max_gps_time\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropGpsTimeAbove(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_gps_time_below") == 0 || strcmp(argv[i],"-drop_gpstime_below") == 0)
                    {
                        if ((i+1) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 1 argument: min_gps_time\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropGpsTimeBelow(atof(argv[i+1])));
                        argv[i]=""; argv[i+1]=""; i+=1;
                    }
                    else if (strcmp(argv[i],"-drop_gps_time_between") == 0 || strcmp(argv[i],"-drop_gpstime_between") == 0)
                    {
                        if ((i+2) >= argc)
                        {
                            fprintf(stderr,"ERROR: '%s' needs 2 arguments: min max\n", argv[i]);
                            return FALSE;
                        }
                        add_criterion(new LAScriterionDropGpsTimeBetween(atof(argv[i+1]), atof(argv[i+2])));
                        argv[i]=""; argv[i+1]=""; argv[i+2]=""; i+=2;
                    }
                }
            }
            else if (strcmp(argv[i],"-first_only") == 0)
            {
                add_criterion(new LAScriterionKeepFirstReturn());
                argv[i]="";
            }
            else if (strcmp(argv[i],"-last_only") == 0)
            {
                add_criterion(new LAScriterionKeepLastReturn());
                argv[i]="";
            }
            else if (strncmp(argv[i],"-thin_", 6) == 0)
            {
                if (strcmp(argv[i],"-thin_with_grid") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: grid_spacing\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionThinWithGrid((float)atof(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
                else if (strcmp(argv[i],"-thin_with_time") == 0)
                {
                    if ((i+1) >= argc)
                    {
                        fprintf(stderr,"ERROR: '%s' needs 1 argument: time_spacing\n", argv[i]);
                        return FALSE;
                    }
                    add_criterion(new LAScriterionThinWithTime((float)atof(argv[i+1])));
                    argv[i]=""; argv[i+1]=""; i+=1;
                }
            }
            else if (strncmp(argv[i],"-filter_", 8) == 0)
            {
                if (strcmp(argv[i],"-filter_and") == 0)
                {
                    if (num_criteria < 2)
                    {
                        fprintf(stderr,"ERROR: '%s' needs to be preceeded by at least two filters\n", argv[i]);
                        return FALSE;
                    }
                    LAScriterion filter_criterion = new LAScriterionAnd(criteria[num_criteria-2], criteria[num_criteria-1]);
                    num_criteria--;
                    criteria[num_criteria] = null;
                    num_criteria--;
                    criteria[num_criteria] = null;
                    add_criterion(filter_criterion);
                    argv[i]="";
                }
                else if (strcmp(argv[i],"-filter_or") == 0)
                {
                    if (num_criteria < 2)
                    {
                        fprintf(stderr,"ERROR: '%s' needs to be preceeded by at least two filters\n", argv[i]);
                        return FALSE;
                    }
                    LAScriterion filter_criterion = new LAScriterionOr(criteria[num_criteria-2], criteria[num_criteria-1]);
                    num_criteria--;
                    criteria[num_criteria] = null;
                    num_criteria--;
                    criteria[num_criteria] = null;
                    add_criterion(filter_criterion);
                    argv[i]="";
                }
            }
        }

        if (drop_return_mask != 0)
        {
            if (keep_return_mask != 0)
            {
                fprintf(stderr,"ERROR: cannot use '-drop_return' and '-keep_return' simultaneously\n");
                return FALSE;
            }
            else
            {
                keep_return_mask = 255 & ~drop_return_mask;
            }
        }
        if (keep_return_mask != 0)
        {
            add_criterion(new LAScriterionKeepReturns(keep_return_mask));
        }

        if (keep_classification_mask != 0)
        {
            if (drop_classification_mask != 0)
            {
                fprintf(stderr,"ERROR: cannot use '-drop_class' and '-keep_class' simultaneously\n");
                return FALSE;
            }
            else
            {
                drop_classification_mask = ~keep_classification_mask;
            }
        }
        if (drop_classification_mask != 0)
        {
            add_criterion(new LAScriterionDropClassifications(drop_classification_mask));
        }

        if (asBoolean(keep_extended_classification_mask[0]) || asBoolean(keep_extended_classification_mask[1]) || asBoolean(keep_extended_classification_mask[2]) || asBoolean(keep_extended_classification_mask[3]) || asBoolean(keep_extended_classification_mask[4]) || asBoolean(keep_extended_classification_mask[5]) || asBoolean(keep_extended_classification_mask[6]) || asBoolean(keep_extended_classification_mask[7]))
        {
            if (asBoolean(drop_extended_classification_mask[0]) || asBoolean(drop_extended_classification_mask[1]) || asBoolean(drop_extended_classification_mask[2]) || asBoolean(drop_extended_classification_mask[3]) || asBoolean(drop_extended_classification_mask[4]) || asBoolean(drop_extended_classification_mask[5]) || asBoolean(drop_extended_classification_mask[6]) || asBoolean(drop_extended_classification_mask[7]))
            {
                fprintf(stderr,"ERROR: cannot use '-drop_extended_class' and '-keep_extended_class' simultaneously\n");
                return FALSE;
            }
            else
            {
                drop_extended_classification_mask[0] = ~keep_extended_classification_mask[0];
                drop_extended_classification_mask[1] = ~keep_extended_classification_mask[1];
                drop_extended_classification_mask[2] = ~keep_extended_classification_mask[2];
                drop_extended_classification_mask[3] = ~keep_extended_classification_mask[3];
                drop_extended_classification_mask[4] = ~keep_extended_classification_mask[4];
                drop_extended_classification_mask[5] = ~keep_extended_classification_mask[5];
                drop_extended_classification_mask[6] = ~keep_extended_classification_mask[6];
                drop_extended_classification_mask[7] = ~keep_extended_classification_mask[7];
            }
        }
        if (asBoolean(drop_extended_classification_mask[0]) || asBoolean(drop_extended_classification_mask[1]) || asBoolean(drop_extended_classification_mask[2]) || asBoolean(drop_extended_classification_mask[3]) || asBoolean(drop_extended_classification_mask[4]) || asBoolean(drop_extended_classification_mask[5]) || asBoolean(drop_extended_classification_mask[6]) || asBoolean(drop_extended_classification_mask[7]))
        {
            add_criterion(new LAScriterionDropExtendedClassifications(drop_extended_classification_mask));
        }

        return TRUE;
    }

    public boolean parse(String string)
    {
        String[] argv = string.split(" ");
        return parse(argv.length, argv);
    }

    int unparse(StringBuilder string)
    {
        int i;
        int n = 0;
        for (i = 0; i < num_criteria; i++)
        {
            n += criteria[i].get_command(string);
        }
        return n;
    }

    void addClipCircle(double x, double y, double radius)
    {
        add_criterion(new LAScriterionKeepCircle(x, y, radius));
    }

    void addClipBox(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z)
    {
        add_criterion(new LAScriterionKeepxyz(min_x, min_y, min_z, max_x, max_y, max_z));
    }

    void addKeepScanDirectionChange()
    {
        add_criterion(new LAScriterionKeepScanDirectionChange());
    }

    public boolean filter(LASpoint point)
    {
        int i;

        for (i = 0; i < num_criteria; i++)
        {
            if (criteria[i].filter(point))
            {
                counters[i]++;
                return TRUE; // point was filtered
            }
        }
        return FALSE; // point survived
    }

    void reset()
    {
        int i;
        for (i = 0; i < num_criteria; i++)
        {
            criteria[i].reset();
        }
    }

    LASfilter()
    {
        alloc_criteria = 0;
        num_criteria = 0;
        criteria = null;
        counters = null;
    }

    void add_criterion(LAScriterion filter_criterion)
    {
        if (num_criteria == alloc_criteria)
        {
            int i;
            alloc_criteria += 16;
            LAScriterion[] temp_criteria = new LAScriterion[alloc_criteria];
            int[] temp_counters = new int[alloc_criteria];
            if (criteria != null)
            {
                for (i = 0; i < num_criteria; i++)
                {
                    temp_criteria[i] = criteria[i];
                    temp_counters[i] = counters[i];
                }
            }
            criteria = temp_criteria;
            counters = temp_counters;
        }
        criteria[num_criteria] = filter_criterion;
        counters[num_criteria] = 0;
        num_criteria++;
    }

    private static boolean asBoolean(int i) {
        return i != 0;
    }

    private static char asChar(String s) {
        if (s != null && !s.isEmpty()) {
            return s.charAt(0);
        } else {
            return '\0';
        }
    }

    public boolean active() {
        return (num_criteria != 0);
    }
}
