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

import com.github.mreutegg.laszip4j.laszip.ByteStreamIn;

import java.io.InputStream;

// TODO: only dummy implementation
public class LASreaderTXT extends LASreader {
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

    public void set_pts(boolean pts) {

    }

    public void set_ptx(boolean ptx) {

    }

    public void set_translate_intensity(float translate_intensity) {

    }

    public void set_scale_intensity(float scale_intensity) {

    }

    public void set_translate_scan_angle(float translate_scan_angle) {

    }

    public void set_scale_scan_angle(float scale_scan_angle) {

    }

    public void set_scale_factor(double[] scale_factor) {

    }

    public void set_offset(double[] offset) {

    }

    public void add_attribute(int attribute_data_type, String attribute_name, String attribute_description, double attribute_scale, double attribute_offset, double attribute_pre_scale, double attribute_pre_offset) {

    }

    public boolean open(String file_name, String parse_string, int skip_lines, boolean populate_header) {
        return false;
    }

    public boolean open(InputStream in, int i, String parse_string, int skip_lines, boolean b) {
        return false;
    }
}
