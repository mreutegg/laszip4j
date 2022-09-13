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

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.RAND_MAX;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.rand;
import static com.github.mreutegg.laszip4j.clib.Cstdlib.srand;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I64_FLOOR;

import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_Z; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_FLAGS; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_RGB; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE;
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; 
import static com.github.mreutegg.laszip4j.laszip.LASzip.LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET; 

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public abstract class LAScriterion
{
    public abstract String name();
    public abstract int get_command(StringBuilder string);
    public int get_decompress_selective(){return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY;};
    public abstract boolean filter(LASpoint point);
    public void reset(){};
};

class LAScriterionAnd extends LAScriterion
{
    public String name() { return "filter_and"; };
    public int get_command(StringBuilder string) { int n = 0; n += one.get_command(string); n += two.get_command(string); n += sprintf(string, "-%s ", name()); return n; };
    public int get_decompress_selective() { return (one.get_decompress_selective() | two.get_decompress_selective()); };
    public boolean filter(LASpoint point) { return one.filter(point) && two.filter(point); };
    LAScriterionAnd(LAScriterion one, LAScriterion two) { this.one = one; this.two = two; };
    private LAScriterion one;
    private LAScriterion two;
};

class LAScriterionOr extends LAScriterion
{
    public String name() { return "filter_or"; };
    public int get_command(StringBuilder string) { int n = 0; n += one.get_command(string); n += two.get_command(string); n += sprintf(string, "-%s ", name()); return n; };
    public int get_decompress_selective() { return (one.get_decompress_selective() | two.get_decompress_selective()); };
    public boolean filter(LASpoint point) { return one.filter(point) || two.filter(point); };
    public LAScriterionOr(LAScriterion one, LAScriterion two) { this.one = one; this.two = two; };
    private LAScriterion one;
    private LAScriterion two;
};

class LAScriterionKeepTile extends LAScriterion
{
    public String name() { return "keep_tile"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g %g ", name(), ll_x, ll_y, tile_size); };
    public boolean filter(LASpoint point) { return (!point.inside_tile(ll_x, ll_y, ur_x, ur_y)); };
    public LAScriterionKeepTile(float ll_x, float ll_y, float tile_size) { this.ll_x = ll_x; this.ll_y = ll_y; this.ur_x = ll_x+tile_size; this.ur_y = ll_y+tile_size; this.tile_size = tile_size; };
    private float ll_x, ll_y, ur_x, ur_y, tile_size;
};

class LAScriterionKeepCircle extends LAScriterion
{
    public String name() { return "keep_circle"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g %g ", name(), center_x, center_y, radius); };
    public boolean filter(LASpoint point) { return (!point.inside_circle(center_x, center_y, radius_squared)); };
    public LAScriterionKeepCircle(double x, double y, double radius) { this.center_x = x; this.center_y = y; this.radius = radius; this.radius_squared = radius*radius; };
    private double center_x, center_y, radius, radius_squared;
};

class LAScriterionKeepxyz extends LAScriterion
{
    public String name() { return "keep_xyz"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g %g %g ", name(), min_x, min_y, min_z, max_x, max_y, max_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY | LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (!point.inside_box(min_x, min_y, min_z, max_x, max_y, max_z)); };
    public LAScriterionKeepxyz(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z) { this.min_x = min_x; this.min_y = min_y; this.min_z = min_z; this.max_x = max_x; this.max_y = max_y; this.max_z = max_z; };
    private double min_x, min_y, min_z, max_x, max_y, max_z;
};

class LAScriterionDropxyz extends LAScriterion
{
    public String name() { return "drop_xyz"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g %g %g ", name(), min_x, min_y, min_z, max_x, max_y, max_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CHANNEL_RETURNS_XY | LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.inside_box(min_x, min_y, min_z, max_x, max_y, max_z)); };
    public LAScriterionDropxyz(double min_x, double min_y, double min_z, double max_x, double max_y, double max_z) { this.min_x = min_x; this.min_y = min_y; this.min_z = min_z; this.max_x = max_x; this.max_y = max_y; this.max_z = max_z; };
    private double min_x, min_y, min_z, max_x, max_y, max_z;
};

class LAScriterionKeepxy extends LAScriterion
{
    public String name() { return "keep_xy"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g ", name(), below_x, below_y, above_x, above_y); };
    public boolean filter(LASpoint point) { return (!point.inside_rectangle(below_x, below_y, above_x, above_y)); };
    public LAScriterionKeepxy(double below_x, double below_y, double above_x, double above_y) { this.below_x = below_x; this.below_y = below_y; this.above_x = above_x; this.above_y = above_y; };
    private double below_x, below_y, above_x, above_y;
};

class LAScriterionDropxy extends LAScriterion
{
    public String name() { return "drop_xy"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g %g %g ", name(), below_x, below_y, above_x, above_y); };
    public boolean filter(LASpoint point) { return (point.inside_rectangle(below_x, below_y, above_x, above_y)); };
    public LAScriterionDropxy(double below_x, double below_y, double above_x, double above_y) { this.below_x = below_x; this.below_y = below_y; this.above_x = above_x; this.above_y = above_y; };
    private double below_x, below_y, above_x, above_y;
};

class LAScriterionKeepx extends LAScriterion
{
    public String name() { return "keep_x"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_x, above_x); };
    public boolean filter(LASpoint point) { double x = point.get_x(); return (x < below_x) || (x >= above_x); };
    public LAScriterionKeepx(double below_x, double above_x) { this.below_x = below_x; this.above_x = above_x; };
    private double below_x, above_x;
};

class LAScriterionDropx extends LAScriterion
{
    public String name() { return "drop_x"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_x, above_x); };
    public boolean filter(LASpoint point) { double x = point.get_x(); return ((below_x <= x) && (x < above_x)); };
    public LAScriterionDropx(double below_x, double above_x) { this.below_x = below_x; this.above_x = above_x; };
    private double below_x, above_x;
};

class LAScriterionKeepy extends LAScriterion
{
    public String name() { return "keep_y"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_y, above_y); };
    public boolean filter(LASpoint point) { double y = point.get_y(); return (y < below_y) || (y >= above_y); };
    public LAScriterionKeepy(double below_y, double above_y) { this.below_y = below_y; this.above_y = above_y; };
    private double below_y, above_y;
};

class LAScriterionDropy extends LAScriterion
{
    public String name() { return "drop_y"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_y, above_y); };
    public boolean filter(LASpoint point) { double y = point.get_y(); return ((below_y <= y) && (y < above_y)); };
    public LAScriterionDropy(double below_y, double above_y) { this.below_y = below_y; this.above_y = above_y; };
    private double below_y, above_y;
};

class LAScriterionKeepz extends LAScriterion
{
    public String name() { return "keep_z"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_z, above_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { double z = point.get_z(); return (z < below_z) || (z >= above_z); };
    LAScriterionKeepz(double below_z, double above_z) { this.below_z = below_z; this.above_z = above_z; };
    double below_z, above_z;
};

class LAScriterionDropz extends LAScriterion
{
    public String name() { return "drop_z"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g %g ", name(), below_z, above_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { double z = point.get_z(); return ((below_z <= z) && (z < above_z)); };
    public LAScriterionDropz(double below_z, double above_z) { this.below_z = below_z; this.above_z = above_z; };
    public double below_z, above_z;
};

class LAScriterionDropxBelow extends LAScriterion
{
    public String name() { return "drop_x_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), below_x); };
    public boolean filter(LASpoint point) { return (point.get_x() < below_x); };
    public LAScriterionDropxBelow(double below_x) { this.below_x = below_x; };
    private double below_x;
};

class LAScriterionDropxAbove extends LAScriterion
{
    public String name() { return "drop_x_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), above_x); };
    public boolean filter(LASpoint point) { return (point.get_x() >= above_x); };
    public LAScriterionDropxAbove(double above_x) { this.above_x = above_x; };
    private double above_x;
};

class LAScriterionDropyBelow extends LAScriterion
{
    public String name() { return "drop_y_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), below_y); };
    public boolean filter(LASpoint point) { return (point.get_y() < below_y); };
    public LAScriterionDropyBelow(double below_y) { this.below_y = below_y; };
    private double below_y;
};

class LAScriterionDropyAbove extends LAScriterion
{
    public String name() { return "drop_y_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), above_y); };
    public boolean filter(LASpoint point) { return (point.get_y() >= above_y); };
    public LAScriterionDropyAbove(double above_y) { this.above_y = above_y; };
    private double above_y;
};

class LAScriterionDropzBelow extends LAScriterion
{
    public String name() { return "drop_z_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), below_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_z() < below_z); };
    public LAScriterionDropzBelow(double below_z) { this.below_z = below_z; };
    private double below_z;
};

class LAScriterionDropzAbove extends LAScriterion
{
    public String name() { return "drop_z_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), above_z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_z() >= above_z); };
    LAScriterionDropzAbove(double above_z) { this.above_z = above_z; };
    double above_z;
};

class LAScriterionKeepXYInt extends LAScriterion
{
    public String name() { return "keep_XY"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d %d %d ", name(), below_X, below_Y, above_X, above_Y); };
    public boolean filter(LASpoint point) { return (point.get_X() < below_X) || (point.get_Y() < below_Y) || (point.get_X() >= above_X) || (point.get_Y() >= above_Y); };
    LAScriterionKeepXYInt(int below_X, int below_Y, int above_X, int above_Y) { this.below_X = below_X; this.below_Y = below_Y; this.above_X = above_X; this.above_Y = above_Y; };
    int below_X, below_Y, above_X, above_Y;
};

class LAScriterionKeepXInt extends LAScriterion
{
    public String name() { return "keep_X"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_X, above_X); };
    public boolean filter(LASpoint point) { return (point.get_X() < below_X) || (above_X <= point.get_X()); };
    LAScriterionKeepXInt(int below_X, int above_X) { this.below_X = below_X; this.above_X = above_X; };
    int below_X, above_X;
};

class LAScriterionDropXInt extends LAScriterion
{
    public String name() { return "drop_X"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_X, above_X); };
    public boolean filter(LASpoint point) { return ((below_X <= point.get_X()) && (point.get_X() < above_X)); };
    LAScriterionDropXInt(int below_X, int above_X) { this.below_X = below_X; this.above_X = above_X; };
    int below_X;
    int above_X;
};

class LAScriterionKeepYInt extends LAScriterion
{
    public String name() { return "keep_Y"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Y, above_Y); };
    public boolean filter(LASpoint point) { return (point.get_Y() < below_Y) || (above_Y <= point.get_Y()); };
    LAScriterionKeepYInt(int below_Y, int above_Y) { this.below_Y = below_Y; this.above_Y = above_Y; };
    int below_Y, above_Y;
};

class LAScriterionDropYInt extends LAScriterion
{
    public String name() { return "drop_Y"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Y, above_Y); };
    public boolean filter(LASpoint point) { return ((below_Y <= point.get_Y()) && (point.get_Y() < above_Y)); };
    LAScriterionDropYInt(int below_Y, int above_Y) { this.below_Y = below_Y; this.above_Y = above_Y; };
    int below_Y;
    int above_Y;
};

class LAScriterionKeepZInt extends LAScriterion
{
    public String name() { return "keep_Z"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Z, above_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_Z() < below_Z) || (above_Z <= point.get_Z()); };
    LAScriterionKeepZInt(int below_Z, int above_Z) { this.below_Z = below_Z; this.above_Z = above_Z; };
    int below_Z, above_Z;
};

class LAScriterionDropZInt extends LAScriterion
{
    public String name() { return "drop_Z"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_Z, above_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return ((below_Z <= point.get_Z()) && (point.get_Z() < above_Z)); };
    LAScriterionDropZInt(int below_Z, int above_Z) { this.below_Z = below_Z; this.above_Z = above_Z; };
    int below_Z;
    int above_Z;
};

class LAScriterionDropXIntBelow extends LAScriterion
{
    public String name() { return "drop_X_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_X); };
    public boolean filter(LASpoint point) { return (point.get_X() < below_X); };
    LAScriterionDropXIntBelow(int below_X) { this.below_X = below_X; };
    int below_X;
};

class LAScriterionDropXIntAbove extends LAScriterion
{
    public String name() { return "drop_X_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_X); };
    public boolean filter(LASpoint point) { return (point.get_X() >= above_X); };
    LAScriterionDropXIntAbove(int above_X) { this.above_X = above_X; };
    int above_X;
};

class LAScriterionDropYIntBelow extends LAScriterion
{
    public String name() { return "drop_Y_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_Y); };
    public boolean filter(LASpoint point) { return (point.get_Y() < below_Y); };
    LAScriterionDropYIntBelow(int below_Y) { this.below_Y = below_Y; };
    int below_Y;
};

class LAScriterionDropYIntAbove extends LAScriterion
{
    public String name() { return "drop_Y_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_Y); };
    public boolean filter(LASpoint point) { return (point.get_Y() >= above_Y); };
    LAScriterionDropYIntAbove(int above_Y) { this.above_Y = above_Y; };
    int above_Y;
};

class LAScriterionDropZIntBelow extends LAScriterion
{
    public String name() { return "drop_Z_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_Z() < below_Z); };
    LAScriterionDropZIntBelow(int below_Z) { this.below_Z = below_Z; };
    int below_Z;
};

class LAScriterionDropZIntAbove extends LAScriterion
{
    public String name() { return "drop_Z_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_Z); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_Z; };
    public boolean filter(LASpoint point) { return (point.get_Z() >= above_Z); };
    LAScriterionDropZIntAbove(int above_Z) { this.above_Z = above_Z; };
    int above_Z;
};

class LAScriterionKeepFirstReturn extends LAScriterion
{
    public String name() { return "keep_first"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() > 1); };
};

class LAScriterionKeepFirstOfManyReturn extends LAScriterion
{
    public String name() { return "keep_first_of_many"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getNumber_of_returns() == 1) || (point.getReturn_number() > 1)); };
};

class LAScriterionKeepMiddleReturn extends LAScriterion
{
    public String name() { return "keep_middle"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getReturn_number() == 1) || (point.getReturn_number() >= point.getNumber_of_returns())); };
};

class LAScriterionKeepLastReturn extends LAScriterion
{
    public String name() { return "keep_last"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() < point.getNumber_of_returns()); };
};

class LAScriterionKeepLastOfManyReturn extends LAScriterion
{
    public String name() { return "keep_last_of_many"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getReturn_number() == 1) || (point.getReturn_number() < point.getNumber_of_returns())); };
};

class LAScriterionDropFirstReturn extends LAScriterion
{
    public String name() { return "drop_first"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() == 1); };
};

class LAScriterionDropFirstOfManyReturn extends LAScriterion
{
    public String name() { return "drop_first_of_many"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getNumber_of_returns() > 1) && (point.getReturn_number() == 1)); };
};

class LAScriterionDropMiddleReturn extends LAScriterion
{
    public String name() { return "drop_middle"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getReturn_number() > 1) && (point.getReturn_number() < point.getNumber_of_returns())); };
};

class LAScriterionDropLastReturn extends LAScriterion
{
    public String name() { return "drop_last"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getReturn_number() >= point.getNumber_of_returns()); };
};

class LAScriterionDropLastOfManyReturn extends LAScriterion
{
    public String name() { return "drop_last_of_many"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return ((point.getNumber_of_returns() > 1) && (point.getReturn_number() >= point.getNumber_of_returns())); };
};

class LAScriterionKeepReturns extends LAScriterion
{
    public String name() { return "keep_return_mask"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), ~drop_return_mask); };
    public boolean filter(LASpoint point) { return ((1 << point.getReturn_number()) & drop_return_mask) != 0; };
    LAScriterionKeepReturns(int keep_return_mask) { drop_return_mask = ~keep_return_mask; };
    private int drop_return_mask; // unsigned
};

class LAScriterionKeepSpecificNumberOfReturns extends LAScriterion
{
    public String name() { return (numberOfReturns == 1 ? "keep_single" : (numberOfReturns == 2 ? "keep_double" : (numberOfReturns == 3 ? "keep_triple" : (numberOfReturns == 4 ? "keep_quadruple" : "keep_quintuple")))); };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getNumber_of_returns() != numberOfReturns); };
    LAScriterionKeepSpecificNumberOfReturns(int numberOfReturns) { this.numberOfReturns = numberOfReturns; };
    private int numberOfReturns; // unsigned
};

class LAScriterionDropSpecificNumberOfReturns extends LAScriterion
{
    public String name() { return (numberOfReturns == 1 ? "drop_single" : (numberOfReturns == 2 ? "drop_double" : (numberOfReturns == 3 ? "drop_triple" : (numberOfReturns == 4 ? "drop_quadruple" : "drop_quintuple")))); };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getNumber_of_returns() == numberOfReturns); };
    LAScriterionDropSpecificNumberOfReturns(int numberOfReturns) { this.numberOfReturns = numberOfReturns; };
    private int numberOfReturns; // unsigned
};

class LAScriterionDropScanDirection extends LAScriterion
{
    public String name() { return "drop_scan_direction"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), scan_direction); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (scan_direction == point.getScan_direction_flag()); };
    LAScriterionDropScanDirection(int scan_direction) { this.scan_direction = scan_direction; };
    private int scan_direction;
};

class LAScriterionKeepScanDirectionChange extends LAScriterion
{
    public String name() { return "keep_scan_direction_change"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { if (scan_direction_flag == point.getScan_direction_flag()) return TRUE; int s = scan_direction_flag; scan_direction_flag = point.getScan_direction_flag(); return s == -1; };
    @Override
    public void reset() { scan_direction_flag = -1; };
    LAScriterionKeepScanDirectionChange() { reset(); };
    private int scan_direction_flag;
};

class LAScriterionKeepEdgeOfFlightLine extends LAScriterion
{
    public String name() { return "keep_edge_of_flight_line"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public boolean filter(LASpoint point) { return (point.getEdge_of_flight_line() == 0); };
}

class LAScriterionKeepRGB extends LAScriterion
{
    public String name() { return "keep_RGB"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s_%s %d %d ", name(), (channel == 0 ? "red" : (channel == 1 ? "green" : (channel == 2 ? "blue" : "nir"))),  below_RGB, above_RGB); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_RGB; };
    public boolean filter(LASpoint point) { return ((point.getRgb(channel) < below_RGB) || (above_RGB < point.getRgb(channel))); };
    LAScriterionKeepRGB(int below_RGB, int above_RGB, int channel) { if (above_RGB < below_RGB) { this.below_RGB = above_RGB; this.above_RGB = below_RGB; } else { this.below_RGB = below_RGB; this.above_RGB = above_RGB; }; this.channel = channel; };
    private int below_RGB, above_RGB, channel;
}

class LAScriterionKeepScanAngle extends LAScriterion
{
    public String name() { return "keep_scan_angle"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_scan, above_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (point.getScan_angle_rank() < below_scan) || (above_scan < point.getScan_angle_rank()); };
    LAScriterionKeepScanAngle(int below_scan, int above_scan) { if (above_scan < below_scan) { this.below_scan = above_scan; this.above_scan = below_scan; } else { this.below_scan = below_scan; this.above_scan = above_scan; } };
    private int below_scan, above_scan;
};

class LAScriterionDropScanAngleBelow extends LAScriterion
{
    public String name() { return "drop_scan_angle_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (point.getScan_angle_rank() < below_scan); };
    LAScriterionDropScanAngleBelow(int below_scan) { this.below_scan = below_scan; };
    int below_scan;
};

class LAScriterionDropScanAngleAbove extends LAScriterion
{
    public String name() { return "drop_scan_angle_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (point.getScan_angle_rank() > above_scan); };
    LAScriterionDropScanAngleAbove(int above_scan) { this.above_scan = above_scan; };
    int above_scan;
};

class LAScriterionDropScanAngleBetween extends LAScriterion
{
    public String name() { return "drop_scan_angle_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_scan, above_scan); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_SCAN_ANGLE; };
    public boolean filter(LASpoint point) { return (below_scan <= point.getScan_angle_rank()) && (point.getScan_angle_rank() <= above_scan); };
    LAScriterionDropScanAngleBetween(int below_scan, int above_scan) { if (above_scan < below_scan) { this.below_scan = above_scan; this.above_scan = below_scan; } else { this.below_scan = below_scan; this.above_scan = above_scan; } };
    int below_scan, above_scan;
};

class LAScriterionKeepIntensity extends LAScriterion
{
    public String name() { return "keep_intensity"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_intensity, above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() < below_intensity) || (point.getIntensity() > above_intensity); };
    LAScriterionKeepIntensity(int below_intensity, int above_intensity) { this.below_intensity = below_intensity; this.above_intensity = above_intensity; };
    int below_intensity, above_intensity;
};

class LAScriterionKeepIntensityBelow extends LAScriterion
{
    public String name() { return "keep_intensity_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() >= below_intensity); };
    LAScriterionKeepIntensityBelow(int below_intensity) { this.below_intensity = below_intensity; };
    int below_intensity;
};

class LAScriterionKeepIntensityAbove extends LAScriterion
{
    public String name() { return "keep_intensity_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() <= above_intensity); };
    LAScriterionKeepIntensityAbove(int above_intensity) { this.above_intensity = above_intensity; };
    int above_intensity;
};

class LAScriterionDropIntensityBelow extends LAScriterion
{
    public String name() { return "drop_intensity_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() < below_intensity); };
    LAScriterionDropIntensityBelow(int below_intensity) { this.below_intensity = below_intensity; };
    int below_intensity;
};

class LAScriterionDropIntensityAbove extends LAScriterion
{
    public String name() { return "drop_intensity_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (point.getIntensity() > above_intensity); };
    LAScriterionDropIntensityAbove(int above_intensity) { this.above_intensity = above_intensity; };
    int above_intensity;
};

class LAScriterionDropIntensityBetween extends LAScriterion
{
    public String name() { return "drop_intensity_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_intensity, above_intensity); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_INTENSITY; };
    public boolean filter(LASpoint point) { return (below_intensity <= point.getIntensity()) && (point.getIntensity() <= above_intensity); };
    LAScriterionDropIntensityBetween(int below_intensity, int above_intensity) { this.below_intensity = below_intensity; this.above_intensity = above_intensity; };
    int below_intensity, above_intensity;
};

class LAScriterionDropClassifications extends LAScriterion
{
    public String name() { return "drop_classification_mask"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), drop_classification_mask); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_CLASSIFICATION; };
    public boolean filter(LASpoint point) { return ((1 << point.getClassification()) & drop_classification_mask) != 0; };
    LAScriterionDropClassifications(int drop_classification_mask) { this.drop_classification_mask = drop_classification_mask; };
    private int drop_classification_mask; // unsigned
};

class LAScriterionDropSynthetic extends LAScriterion
{
    public String name() { return "drop_synthetic"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_synthetic_flag() == 1); };
};

class LAScriterionKeepSynthetic extends LAScriterion
{
    public String name() { return "keep_synthetic"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_synthetic_flag() == 0); };
};

class LAScriterionDropKeypoint extends LAScriterion
{
    public String name() { return "drop_keypoint"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_keypoint_flag() == 1); };
};

class LAScriterionKeepKeypoint extends LAScriterion
{
    public String name() { return "keep_keypoint"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_keypoint_flag() == 0); };
};

class LAScriterionDropWithheld extends LAScriterion
{
    public String name() { return "drop_withheld"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_withheld_flag() == 1); };
};

class LAScriterionKeepWithheld extends LAScriterion
{
    public String name() { return "keep_withheld"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_withheld_flag() == 0); };
};

class LAScriterionDropOverlap extends LAScriterion
{
    public String name() { return "drop_overlap"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_extended_overlap_flag() == 1); };
};

class LAScriterionKeepOverlap extends LAScriterion
{
    public String name() { return "keep_overlap"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s ", name()); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_FLAGS; };
    public boolean filter(LASpoint point) { return (point.get_extended_overlap_flag() == 0); };
};

class LAScriterionKeepUserData extends LAScriterion
{
    public String name() { return "keep_user_data"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() != user_data); };
    LAScriterionKeepUserData(byte user_data) { this.user_data = user_data; };
    private byte user_data;
};

class LAScriterionKeepUserDataBelow extends LAScriterion
{
    public String name() { return "keep_user_data_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() >= below_user_data); };
    LAScriterionKeepUserDataBelow(byte below_user_data) { this.below_user_data = below_user_data; };
    private byte below_user_data;
};

class LAScriterionKeepUserDataAbove extends LAScriterion
{
    public String name() { return "keep_user_data_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() <= above_user_data); };
    LAScriterionKeepUserDataAbove(byte above_user_data) { this.above_user_data = above_user_data; };
    private byte above_user_data;
};

class LAScriterionKeepUserDataBetween extends LAScriterion
{
    public String name() { return "keep_user_data_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_user_data, above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() < below_user_data) || (above_user_data < point.getUser_data()); };
    LAScriterionKeepUserDataBetween(byte below_user_data, byte above_user_data) { this.below_user_data = below_user_data; this.above_user_data = above_user_data; };
    private byte below_user_data, above_user_data;
};

class LAScriterionDropUserData extends LAScriterion
{
    public String name() { return "drop_user_data"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() == user_data); };
    LAScriterionDropUserData(byte user_data) { this.user_data = user_data; };
    private byte user_data;
};

class LAScriterionDropUserDataBelow extends LAScriterion
{
    public String name() { return "drop_user_data_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() < below_user_data) ; };
    LAScriterionDropUserDataBelow(byte below_user_data) { this.below_user_data = below_user_data; };
    private byte below_user_data;
};

class LAScriterionDropUserDataAbove extends LAScriterion
{
    public String name() { return "drop_user_data_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (point.getUser_data() > above_user_data); };
    LAScriterionDropUserDataAbove(byte above_user_data) { this.above_user_data = above_user_data; };
    private byte above_user_data;
};

class LAScriterionDropUserDataBetween extends LAScriterion
{
    public String name() { return "drop_user_data_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_user_data, above_user_data); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_USER_DATA; };
    public boolean filter(LASpoint point) { return (below_user_data <= point.getUser_data()) && (point.getUser_data() <= above_user_data); };
    LAScriterionDropUserDataBetween(byte below_user_data, byte above_user_data) { this.below_user_data = below_user_data; this.above_user_data = above_user_data; };
    private byte below_user_data, above_user_data;
};

class LAScriterionKeepPointSource extends LAScriterion
{
    public String name() { return "keep_point_source"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() != point_source_id); };
    LAScriterionKeepPointSource(char point_source_id) { this.point_source_id = point_source_id; };
    private char point_source_id;
};

class LAScriterionKeepPointSourceBetween extends LAScriterion
{
    public String name() { return "keep_point_source_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_point_source_id, above_point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() < below_point_source_id) || (above_point_source_id < point.getPoint_source_ID()); };
    LAScriterionKeepPointSourceBetween(char below_point_source_id, char above_point_source_id) { this.below_point_source_id = below_point_source_id; this.above_point_source_id = above_point_source_id; };
    private char below_point_source_id, above_point_source_id;
};

class LAScriterionDropPointSource extends LAScriterion
{
    public String name() { return "drop_point_source"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() == point_source_id) ; };
    LAScriterionDropPointSource(char point_source_id) { this.point_source_id = point_source_id; };
    private char point_source_id;
};

class LAScriterionDropPointSourceBelow extends LAScriterion
{
    public String name() { return "drop_point_source_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), below_point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() < below_point_source_id) ; };
    LAScriterionDropPointSourceBelow(char below_point_source_id) { this.below_point_source_id = below_point_source_id; };
    private char below_point_source_id;
};

class LAScriterionDropPointSourceAbove extends LAScriterion
{
    public String name() { return "drop_point_source_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), above_point_source_id); };
    public boolean filter(LASpoint point) { return (point.getPoint_source_ID() > above_point_source_id); };
    LAScriterionDropPointSourceAbove(char above_point_source_id) { this.above_point_source_id = above_point_source_id; };
    private char above_point_source_id;
};

class LAScriterionDropPointSourceBetween extends LAScriterion
{
    public String name() { return "drop_point_source_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d %d ", name(), below_point_source_id, above_point_source_id); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_POINT_SOURCE; };
    public boolean filter(LASpoint point) { return (below_point_source_id <= point.getPoint_source_ID()) && (point.getPoint_source_ID() <= above_point_source_id); };
    LAScriterionDropPointSourceBetween(char below_point_source_id, char above_point_source_id) { this.below_point_source_id = below_point_source_id; this.above_point_source_id = above_point_source_id; };
    private char below_point_source_id, above_point_source_id;
};

class LAScriterionKeepGpsTime extends LAScriterion
{
    public String name() { return "keep_gps_time"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %.6f %.6f ", name(), below_gpstime, above_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && ((point.getGps_time() < below_gpstime) || (point.getGps_time() > above_gpstime))); };
    LAScriterionKeepGpsTime(double below_gpstime, double above_gpstime) { this.below_gpstime = below_gpstime; this.above_gpstime = above_gpstime; };
    double below_gpstime, above_gpstime;
};

class LAScriterionDropGpsTimeBelow extends LAScriterion
{
    public String name() { return "drop_gps_time_below"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %.6f ", name(), below_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && (point.getGps_time() < below_gpstime)); };
    LAScriterionDropGpsTimeBelow(double below_gpstime) { this.below_gpstime = below_gpstime; };
    double below_gpstime;
};

class LAScriterionDropGpsTimeAbove extends LAScriterion
{
    public String name() { return "drop_gps_time_above"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %.6f ", name(), above_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && (point.getGps_time() > above_gpstime)); };
    LAScriterionDropGpsTimeAbove(double above_gpstime) { this.above_gpstime = above_gpstime; };
    double above_gpstime;
};

class LAScriterionDropGpsTimeBetween extends LAScriterion
{
    public String name() { return "drop_gps_time_between"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %.6f %.6f ", name(), below_gpstime, above_gpstime); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_GPS_TIME; };
    public boolean filter(LASpoint point) { return (point.haveGpsTime() && ((below_gpstime <= point.getGps_time()) && (point.getGps_time() <= above_gpstime))); };
    LAScriterionDropGpsTimeBetween(double below_gpstime, double above_gpstime) { this.below_gpstime = below_gpstime; this.above_gpstime = above_gpstime; };
    double below_gpstime, above_gpstime;
};

class LAScriterionKeepWavepacket extends LAScriterion
{
    public String name() { return "keep_wavepacket"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), keep_wavepacket); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET; };
    public boolean filter(LASpoint point) { return (point.getWavepacketDescriptorIndex() != keep_wavepacket); };
    LAScriterionKeepWavepacket(int keep_wavepacket) { this.keep_wavepacket = keep_wavepacket; };
    int keep_wavepacket; // unsigned
};

class LAScriterionDropWavepacket extends LAScriterion
{
    public String name() { return "drop_wavepacket"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), drop_wavepacket); };
    public int get_decompress_selective() { return LASZIP_DECOMPRESS_SELECTIVE_WAVEPACKET; };
    public boolean filter(LASpoint point) { return (point.getWavepacketDescriptorIndex() == drop_wavepacket); };
    LAScriterionDropWavepacket(int drop_wavepacket) { this.drop_wavepacket = drop_wavepacket; };
    int drop_wavepacket;
};

class LAScriterionKeepEveryNth extends LAScriterion
{
    public String name() { return "keep_every_nth"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %d ", name(), every); };
    public boolean filter(LASpoint point) { if (counter == every) { counter = 1; return FALSE; } else { counter++; return TRUE; } };
    LAScriterionKeepEveryNth(int every) { this.every = every; counter = 1; };
    int counter;
    int every;
};

class LAScriterionKeepRandomFraction extends LAScriterion
{
    public String name() { return "keep_random_fraction"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), fraction); };
    public boolean filter(LASpoint point)
    {
        srand(seed);
        seed = rand();
        return ((float)seed/(float)RAND_MAX) > fraction;
    };
    @Override
    public void reset() { seed = 0; };
    LAScriterionKeepRandomFraction(float fraction) { seed = 0; this.fraction = fraction; };
    int seed;
    float fraction;
};

class LAScriterionThinWithGrid extends LAScriterion
{
    public String name() { return "thin_with_grid"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), (grid_spacing > 0 ? grid_spacing : -grid_spacing)); };
    public boolean filter(LASpoint point)
    {
        return FALSE;
        // TODO: port to Java
        /*
        if (grid_spacing < 0)
        {
            grid_spacing = -grid_spacing;
            anker = I32_FLOOR(point.get_y() / grid_spacing);
        }
        int pos_x = I32_FLOOR(point.get_x() / grid_spacing);
        int pos_y = I32_FLOOR(point.get_y() / grid_spacing) - anker;
        boolean no_x_anker = FALSE;
        int[] array_size; // unsigned
        int[][] ankers;
        int[][][] array; // unsigned
        char[][] array_sizes;
        if (pos_y < 0)
        {
            pos_y = -pos_y - 1;
            ankers = minus_ankers;
            if (pos_y < minus_plus_size && minus_plus_sizes[pos_y])
            {
                pos_x -= minus_ankers[pos_y];
                if (pos_x < 0)
                {
                    pos_x = -pos_x - 1;
                    array_size = &minus_minus_size;
                    array = &minus_minus;
                    array_sizes = &minus_minus_sizes;
                }
                else
                {
                    array_size = &minus_plus_size;
                    array = &minus_plus;
                    array_sizes = &minus_plus_sizes;
                }
            }
            else
            {
                no_x_anker = TRUE;
                array_size = &minus_plus_size;
                array = &minus_plus;
                array_sizes = &minus_plus_sizes;
            }
        }
        else
        {
            ankers = &plus_ankers;
            if (pos_y < plus_plus_size && plus_plus_sizes[pos_y])
            {
                pos_x -= plus_ankers[pos_y];
                if (pos_x < 0)
                {
                    pos_x = -pos_x - 1;
                    array_size = &plus_minus_size;
                    array = &plus_minus;
                    array_sizes = &plus_minus_sizes;
                }
                else
                {
                    array_size = &plus_plus_size;
                    array = &plus_plus;
                    array_sizes = &plus_plus_sizes;
                }
            }
            else
            {
                no_x_anker = TRUE;
                array_size = &plus_plus_size;
                array = &plus_plus;
                array_sizes = &plus_plus_sizes;
            }
        }
        // maybe grow banded grid in y direction
        if (pos_y >= *array_size)
        {
            int array_size_new = ((pos_y/1024)+1)*1024;
            if (*array_size)
            {
                if (array == &minus_plus || array == &plus_plus) *ankers = (I32*)realloc(*ankers, array_size_new*sizeof(I32));
            *array = (U32**)realloc(*array, array_size_new*sizeof(U32*));
            *array_sizes = (U16*)realloc(*array_sizes, array_size_new*sizeof(U16));
            }
            else
            {
                if (array == &minus_plus || array == &plus_plus) *ankers = (I32*)malloc(array_size_new*sizeof(I32));
            *array = (U32**)malloc(array_size_new*sizeof(U32*));
            *array_sizes = (U16*)malloc(array_size_new*sizeof(U16));
            }
            for (U32 i = *array_size; i < array_size_new; i++)
            {
                (*array)[i] = 0;
                (*array_sizes)[i] = 0;
            }
            *array_size = array_size_new;
        }
        // is this the first x anker for this y pos?
        if (no_x_anker)
        {
            (*ankers)[pos_y] = pos_x;
            pos_x = 0;
        }
        // maybe grow banded grid in x direction
        int pos_x_pos = pos_x/32;
        if (pos_x_pos >= (*array_sizes)[pos_y])
        {
            int array_sizes_new = ((pos_x_pos/256)+1)*256;
            if ((*array_sizes)[pos_y])
            {
                (*array)[pos_y] = (U32*)realloc((*array)[pos_y], array_sizes_new*sizeof(U32));
            }
            else
            {
                (*array)[pos_y] = new int[array_sizes_new];
            }
            for (char i = (*array_sizes)[pos_y]; i < array_sizes_new; i++)
            {
                (*array)[pos_y][i] = 0;
            }
            (*array_sizes)[pos_y] = array_sizes_new;
        }
        int pos_x_bit = 1 << (pos_x%32);
        if ((*array)[pos_y][pos_x_pos] & pos_x_bit) return TRUE;
        (*array)[pos_y][pos_x_pos] |= pos_x_bit;
        return FALSE;
        */
    }
    @Override
    public void reset()
    {
        if (grid_spacing > 0) grid_spacing = -grid_spacing;
        if (minus_minus_size != 0)
        {
            minus_minus = null;
            minus_minus_sizes = null;
            minus_minus_size = 0;
        }
        if (minus_plus_size != 0)
        {
            minus_ankers = null;
            minus_plus = null;
            minus_plus_sizes = null;
            minus_plus_size = 0;
        }
        if (plus_minus_size != 0)
        {
            plus_minus = null;
            plus_minus_sizes = null;
            plus_minus_size = 0;
        }
        if (plus_plus_size != 0)
        {
            plus_ankers = null;
            plus_plus = null;
            plus_plus_sizes = null;
            plus_plus_size = 0;
        }
    };
    LAScriterionThinWithGrid(float grid_spacing)
    {
        this.grid_spacing = -grid_spacing;
        minus_ankers = null;
        minus_minus_size = 0;
        minus_minus = null;
        minus_minus_sizes = null;
        minus_plus_size = 0;
        minus_plus = null;
        minus_plus_sizes = null;
        plus_ankers = null;
        plus_minus_size = 0;
        plus_minus = null;
        plus_minus_sizes = null;
        plus_plus_size = 0;
        plus_plus = null;
        plus_plus_sizes = null;
    };

    private float grid_spacing;
    private int anker;
    private int[] minus_ankers;
    private int minus_minus_size; // unsigned
    private int[][] minus_minus; // unsigned
    private char[] minus_minus_sizes;
    private int minus_plus_size; // unsigned
    private int[][] minus_plus; // unsigned
    private char[] minus_plus_sizes;
    private int[] plus_ankers;
    private int plus_minus_size; // unsigned
    private int[][] plus_minus; // unsigned
    private char[] plus_minus_sizes;
    private int plus_plus_size; // unsigned
    private int[][] plus_plus; // unsigned
    private char[] plus_plus_sizes;
};

class LAScriterionThinWithTime extends LAScriterion
{
    public String name() { return "thin_with_time"; };
    public int get_command(StringBuilder string) { return sprintf(string, "-%s %g ", name(), (time_spacing > 0 ? time_spacing : -time_spacing)); };
    public boolean filter(LASpoint point)
    {
        long pos_t = I64_FLOOR(point.get_gps_time() / time_spacing);
        List<Double> map_element = times.get(pos_t);
        if (map_element == null)
        {
            map_element = new ArrayList<>();
            map_element.add(point.get_gps_time());
            times.put(pos_t, map_element);
            return FALSE;
        }
        else if (map_element.get(0) == point.get_gps_time())
        {
            return FALSE;
        }
            else
        {
            return TRUE;
        }
    }
    @Override
    public void reset()
    {
        times.clear();
    };
    LAScriterionThinWithTime(double time_spacing)
    {
        this.time_spacing = time_spacing;
    };
    double time_spacing;
    SortedMap<Long, List<Double>> times = new TreeMap<>();
};
