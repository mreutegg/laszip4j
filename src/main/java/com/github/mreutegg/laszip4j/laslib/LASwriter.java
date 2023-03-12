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
import com.github.mreutegg.laszip4j.laszip.LASquantizer;

public abstract class LASwriter {

    public LASquantizer quantizer = new LASquantizer();
    public long npoints;
    public long p_count;
    public LASinventory inventory;

    public abstract boolean write_point(LASpoint point);
    public void update_inventory(LASpoint point) { inventory.add(point); };
    public abstract boolean chunk();

    public boolean update_header(LASheader header) {
        return update_header(header, false, false);
    }
    public boolean update_header(LASheader header, boolean use_inventory) {
        return update_header(header, use_inventory, false);
    }
    public abstract boolean update_header(LASheader header, boolean use_inventory, boolean update_extra_bytes);
    public long close() {
        return close(true);
    }
    public abstract long close(boolean update_npoints);

    public LASwriter() { npoints = 0; p_count = 0; };
}
