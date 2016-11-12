/*
 * Copyright 2007-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

public class LASquantizer {
    
    public double x_scale_factor;
    public double y_scale_factor;
    public double z_scale_factor;
    public double x_offset;
    public double y_offset;
    public double z_offset;

    public double get_x(int X) { return x_scale_factor*X+x_offset; };
    public double get_y(int Y) { return y_scale_factor*Y+y_offset; };
    public double get_z(int Z) { return z_scale_factor*Z+z_offset; };

    public int get_X(double x) { if (x >= x_offset) return (int)((x-x_offset)/x_scale_factor+0.5); else return (int)((x-x_offset)/x_scale_factor-0.5); };
    public int get_Y(double y) { if (y >= y_offset) return (int)((y-y_offset)/y_scale_factor+0.5); else return (int)((y-y_offset)/y_scale_factor-0.5); };
    public int get_Z(double z) { if (z >= z_offset) return (int)((z-z_offset)/z_scale_factor+0.5); else return (int)((z-z_offset)/z_scale_factor-0.5); };

    LASquantizer()
    {
        x_scale_factor = 0.01;
        y_scale_factor = 0.01;
        z_scale_factor = 0.01;
        x_offset = 0.0;
        y_offset = 0.0;
        z_offset = 0.0;
    };

    LASquantizer(LASquantizer other) {
        this.x_scale_factor = other.x_scale_factor;
        this.y_scale_factor = other.y_scale_factor;
        this.z_scale_factor = other.z_scale_factor;
        this.x_offset = other.x_offset;
        this.y_offset = other.y_offset;
        this.z_offset = other.z_offset;
    }
}
