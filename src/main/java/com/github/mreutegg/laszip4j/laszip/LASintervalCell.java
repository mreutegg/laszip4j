/*
 * Copyright 2011-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

public class LASintervalCell {
    public int start; // unsigned
    public int end; // unsigned
    public LASintervalCell next;

    public LASintervalCell() {
        start = 0;
        end = 0;
        next = null;
    }

    LASintervalCell(int p_index)
    {
        start = p_index;
        end = p_index;
        next = null;
    }

    LASintervalCell(LASintervalCell cell)
    {
        start = cell.start;
        end = cell.end;
        next = null;
    }
}
