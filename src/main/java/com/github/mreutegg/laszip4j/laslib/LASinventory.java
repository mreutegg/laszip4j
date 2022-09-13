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

import com.github.mreutegg.laszip4j.laszip.LASpoint;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_MAX;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASinventory {
    
    private boolean first;

    public boolean active(){ return (first == FALSE); };
    public long extended_number_of_point_records;
    public long[] extended_number_of_points_by_return = new long[16];
    public int max_X;
    public int min_X;
    public int max_Y;
    public int min_Y;
    public int max_Z;
    public int min_Z;

    LASinventory()
    {
        int i;
        extended_number_of_point_records = 0;
        for (i = 0; i < 16; i++) extended_number_of_points_by_return[i] = 0;
        max_X = min_X = 0;
        max_Y = min_Y = 0;
        max_Z = min_Z = 0;
        first = TRUE;
    }

    boolean init(LASheader header)
    {
        if (header != null)
        {
            int i;
            extended_number_of_point_records = (header.number_of_point_records != 0 ? header.number_of_point_records : header.extended_number_of_point_records);
            for (i = 0; i < 5; i++) extended_number_of_points_by_return[i] = (header.number_of_points_by_return[i] != 0 ? header.number_of_points_by_return[i] : header.extended_number_of_points_by_return[i]);
            for (i = 5; i < 16; i++) extended_number_of_points_by_return[i] = header.extended_number_of_points_by_return[i];
            max_X = header.get_X(header.max_x);
            min_X = header.get_X(header.min_x);
            max_Y = header.get_Y(header.max_y);
            min_Y = header.get_Y(header.min_y);
            max_Z = header.get_Z(header.max_z);
            min_Z = header.get_Z(header.min_z);
            first = FALSE;
            return TRUE;
        }
        return FALSE;
    }

    boolean add(LASpoint point)
    {
        extended_number_of_point_records++;
        extended_number_of_points_by_return[point.getReturn_number()]++;
        
        if (first)
        {
            min_X = max_X = point.get_X();
            min_Y = max_Y = point.get_Y();
            min_Z = max_Z = point.get_Z();
            first = FALSE;
        }
        else
        {
            if (point.get_X() < min_X) min_X = point.get_X();
            else if (point.get_X() > max_X) max_X = point.get_X();
            if (point.get_Y() < min_Y) min_Y = point.get_Y();
            else if (point.get_Y() > max_Y) max_Y = point.get_Y();
            if (point.get_Z() < min_Z) min_Z = point.get_Z();
            else if (point.get_Z() > max_Z) max_Z = point.get_Z();
        }
        return TRUE;
    }

    boolean update_header(LASheader header)
    {
        if (header != null)
        {
            int i;
            if (extended_number_of_point_records > Integer.toUnsignedLong(U32_MAX))
            {
                if (header.version_minor >= 4)
                {
                    header.number_of_point_records = 0;
                }
                else
                {
                    return FALSE;
                }
            }
            else
            {
                header.number_of_point_records = (int)extended_number_of_point_records;
            }
            for (i = 0; i < 5; i++)
            {
                if (extended_number_of_points_by_return[i+1] > Integer.toUnsignedLong(U32_MAX))
                {
                    if (header.version_minor >= 4)
                    {
                        header.number_of_points_by_return[i] = 0;
                    }
                    else
                    {
                        return FALSE;
                    }
                }
                else
                {
                    header.number_of_points_by_return[i] = (int)extended_number_of_points_by_return[i+1];
                }
            }
            header.max_x = header.get_x(max_X);
            header.min_x = header.get_x(min_X);
            header.max_y = header.get_y(max_Y);
            header.min_y = header.get_y(min_Y);
            header.max_z = header.get_z(max_Z);
            header.min_z = header.get_z(min_Z);
            header.extended_number_of_point_records = extended_number_of_point_records;
            for (i = 0; i < 15; i++)
            {
                header.extended_number_of_points_by_return[i] = extended_number_of_points_by_return[i+1];
            }
            return TRUE;
        }
        else
        {
            return FALSE;
        }
    }}
