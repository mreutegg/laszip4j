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

import java.io.PrintStream;

// TODO: only dummy implementation
public class LASwriterWRL extends LASwriter {
    @Override
    public boolean write_point(LASpoint point) {
        return false;
    }

    @Override
    public boolean chunk() {
        return false;
    }

    @Override
    public boolean update_header(LASheader header, boolean use_inventory, boolean update_extra_bytes) {
        return false;
    }

    @Override
    public long close(boolean update_npoints) {
        return 0;
    }

    public boolean open(String file_name, LASheader header, String parse_string) {
        return false;
    }

    public boolean open(PrintStream out, LASheader header, String parse_string) {
        return false;
    }
}
