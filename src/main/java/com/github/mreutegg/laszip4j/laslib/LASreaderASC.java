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

import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;

// TODO: only dummy implementation
public class LASreaderASC extends LASreader {
    @Override
    public int get_format() {
        return 0;
    }

    @Override
    public boolean seek(long p_index) {
        return false;
    }

    @Override
    public ByteStreamIn get_stream() {
        return null;
    }

    @Override
    public void close(boolean close_stream) {

    }

    @Override
    protected boolean read_point_default() {
        return false;
    }

    public boolean open(String file_name, boolean comma_not_point) {
        return false;
    }
}
