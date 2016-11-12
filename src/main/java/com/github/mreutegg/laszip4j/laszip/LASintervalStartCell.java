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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASintervalStartCell extends LASintervalCell {
    public int full; // unsigned
    public int total; // unsigned
    public LASintervalCell last;

    public LASintervalStartCell() {
        full = 0;
        total = 0;
        last = null;
    }

    LASintervalStartCell(int p_index)
    {
        super(p_index);
        full = 1;
        total = 1;
        last = null;
    }

    boolean add(int p_index) {
        return add(p_index, 1000);
    }

    boolean add(int p_index, int threshold)
    {
        int current_end = (last != null ? last.end : end);
        assert(p_index > current_end);
        int diff = p_index - current_end;
        full++;
        if (diff > threshold)
        {
            if (last != null)
            {
                last.next = new LASintervalCell(p_index);
                last = last.next;
            }
            else
            {
                next = new LASintervalCell(p_index);
                last = next;
            }
            total++;
            return TRUE; // created new interval
        }
        if (last != null)
        {
            last.end = p_index;
        }
        else
        {
            end = p_index;
        }
        total += diff;
        return FALSE; // added to interval
    }

}
