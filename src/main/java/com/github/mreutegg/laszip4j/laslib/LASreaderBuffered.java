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

// TODO: only dummy implementation
public class LASreaderBuffered extends LASreader {
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


    public void set_buffer_size(float buffer_size) {

    }

    public void set_scale_factor(double[] scale_factor) {

    }

    public void set_offset(double[] offset) {

    }

    public void set_parse_string(String parse_string) {

    }

    public void set_skip_lines(int skip_lines) {

    }

    public void set_populate_header(boolean populate_header) {

    }

    public void set_translate_intensity(float translate_intensity) {

    }

    public void set_scale_intensity(float scale_intensity) {

    }

    public void set_translate_scan_angle(float translate_scan_angle) {

    }

    public void set_scale_scan_angle(float scale_scan_angle) {

    }

    public void set_file_name(String file_name) {

    }

    public void add_neighbor_file_name(String s) {

    }

    public boolean open() {
        return false;
    }

    public boolean reopen() {
        return false;
    }

    public void remove_buffer() {

    }
}
