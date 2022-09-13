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

import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;

// TODO: dummy implementation only
public class LAStransform {

    public void transform(LASpoint point) {

    }

    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY; }

    public void reset() {

    }

    public void setPointSource(int file_name_current) {

    }

    public static void usage() {

    }

    public void clean() {

    }

    public boolean parse(int argc, String[] argv) {
        return true;
    }

    public boolean active() {
        return false;
    }

    public boolean filtered() {
        return false;
    }

    public void setFilter(LASfilter filter) {

    }
}
