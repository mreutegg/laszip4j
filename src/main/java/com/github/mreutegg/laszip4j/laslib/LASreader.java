/*
 * Copyright 2005-2013, martin isenburg, rapidlasso - fast tools to catch reality
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
import com.github.mreutegg.laszip4j.laszip.LASindex;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.util.concurrent.Callable;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public abstract class LASreader {

    public LASheader header = new LASheader();
    public LASpoint point = new LASpoint();

    public long npoints;
    public long p_count;

    protected LASindex index;
    protected LASfilter filter;
    protected LAStransform transform;

    protected int inside; // unsigned
    protected float t_ll_x, t_ll_y, t_size, t_ur_x, t_ur_y;
    protected double c_center_x, c_center_y, c_radius, c_radius_squared;
    protected double r_min_x, r_min_y, r_max_x, r_max_y;
    protected double orig_min_x, orig_min_y, orig_max_x, orig_max_y;

    private Callable<Boolean> read_simple;
    private Callable<Boolean> read_complex;

    LASreader()
    {
        npoints = 0;
        p_count = 0;
        read_simple = this::read_point_default;
        read_complex = null;
        index = null;
        filter = null;
        transform = null;
        inside = 0;
        t_ll_x = 0;
        t_ll_y = 0;
        t_size = 0;
        t_ur_x = 0;
        t_ur_y = 0;
        c_center_x = 0;
        c_center_y = 0;
        c_radius = 0;
        c_radius_squared = 0;
        r_min_x = 0;
        r_min_y = 0;
        r_max_x = 0;
        r_max_y = 0;
        orig_min_x = 0;
        orig_min_y = 0;
        orig_max_x = 0;
        orig_max_y = 0;
    }

    public void set_index(LASindex index)
    {
        this.index = index;
    }

    public void set_filter(LASfilter filter)
    {
        this.filter = filter;
        if (filter != null && transform != null)
        {
            read_simple = this::read_point_filtered_and_transformed;
        }
        else if (filter != null)
        {
            read_simple = this::read_point_filtered;
        }
        else if (transform != null)
        {
            read_simple = this::read_point_transformed;
        }
        else
        {
            read_simple = this::read_point_default;
        }
        read_complex = this::read_point_default;
    }

    public void set_transform(LAStransform transform)
    {
        this.transform = transform;
        if (filter != null && transform != null)
        {
            read_simple = this::read_point_filtered_and_transformed;
        }
        else if (filter != null)
        {
            read_simple = this::read_point_filtered;
        }
        else if (transform != null)
        {
            read_simple = this::read_point_transformed;
        }
        else
        {
            read_simple = this::read_point_default;
        }
        read_complex = this::read_point_default;
    }

    public boolean inside_none()
    {
        if (filter != null || transform != null)
        {
            read_complex = this::read_point_default;
        }
        else
        {
            read_simple = this::read_point_default;
        }
        if (inside != 0)
        {
            header.min_x = orig_min_x;
            header.min_y = orig_min_y;
            header.max_x = orig_max_x;
            header.max_y = orig_max_y;
            inside = 0;
        }
        return TRUE;
    }

    public boolean inside_tile(float ll_x, float ll_y, float size)
    {
        inside = 1;
        t_ll_x = ll_x;
        t_ll_y = ll_y;
        t_size = size;
        t_ur_x = ll_x + size;
        t_ur_y = ll_y + size;
        orig_min_x = header.min_x;
        orig_min_y = header.min_y;
        orig_max_x = header.max_x;
        orig_max_y = header.max_y;
        header.min_x = ll_x;
        header.min_y = ll_y;
        header.max_x = ll_x + size;
        header.max_y = ll_y + size;
        header.max_x -= header.x_scale_factor;
        header.max_y -= header.y_scale_factor;
        if (((orig_min_x > header.max_x) || (orig_min_y > header.max_y) || (orig_max_x < header.min_x) || (orig_max_y < header.min_y)))
        {
            if (filter != null || transform != null)
            {
                read_complex = this::read_point_none;
            }
            else
            {
                read_simple = this::read_point_none;
            }
        }
        else if (filter != null || transform != null)
        {
            if (index != null)
            {
                index.intersect_tile(ll_x, ll_y, size);
                read_complex = this::read_point_inside_tile_indexed;
            }
            else
            {
                read_complex = this::read_point_inside_tile;
            }
        }
        else
        {
            if (index != null)
            {
                index.intersect_tile(ll_x, ll_y, size);
                read_simple = this::read_point_inside_tile_indexed;
            }
            else
            {
                read_simple = this::read_point_inside_tile;
            }
        }
        return TRUE;
    }

    public boolean inside_circle(double center_x, double center_y, double radius)
    {
        inside = 2;
        c_center_x = center_x;
        c_center_y = center_y;
        c_radius = radius;
        c_radius_squared = radius*radius;
        orig_min_x = header.min_x;
        orig_min_y = header.min_y;
        orig_max_x = header.max_x;
        orig_max_y = header.max_y;
        header.min_x = center_x - radius;
        header.min_y = center_y - radius;
        header.max_x = center_x + radius;
        header.max_y = center_y + radius;
        if (((orig_min_x > header.max_x) || (orig_min_y > header.max_y) || (orig_max_x < header.min_x) || (orig_max_y < header.min_y)))
        {
            if (filter != null || transform != null)
            {
                read_complex = this::read_point_none;
            }
            else
            {
                read_simple = this::read_point_none;
            }
        }
        else if (filter != null || transform != null)
        {
            if (index != null)
            {
                index.intersect_circle(center_x, center_y, radius);
                read_complex = this::read_point_inside_circle_indexed;
            }
            else
            {
                read_complex = this::read_point_inside_circle;
            }
        }
        else
        {
            if (index != null)
            {
                index.intersect_circle(center_x, center_y, radius);
                read_simple = this::read_point_inside_circle_indexed;
            }
            else
            {
                read_simple = this::read_point_inside_circle;
            }
        }
        return TRUE;
    }

    public boolean inside_rectangle(double min_x, double min_y, double max_x, double max_y)
    {
        inside = 3;
        r_min_x = min_x;
        r_min_y = min_y;
        r_max_x = max_x;
        r_max_y = max_y;
        orig_min_x = header.min_x;
        orig_min_y = header.min_y;
        orig_max_x = header.max_x;
        orig_max_y = header.max_y;
        header.min_x = min_x;
        header.min_y = min_y;
        header.max_x = max_x;
        header.max_y = max_y;
        if (((orig_min_x > max_x) || (orig_min_y > max_y) || (orig_max_x < min_x) || (orig_max_y < min_y)))
        {
            if (filter != null || transform != null)
            {
                read_complex = this::read_point_none;
            }
            else
            {
                read_simple = this::read_point_none;
            }
        }
        else if (filter != null || transform != null)
        {
            if (index != null)
            {
                index.intersect_rectangle(min_x, min_y, max_x, max_y);
                read_complex = this::read_point_inside_rectangle_indexed;
            }
            else
            {
                read_complex = this::read_point_inside_rectangle;
            }
        }
        else
        {
            if (index != null)
            {
                index.intersect_rectangle(min_x, min_y, max_x, max_y);
                read_simple = this::read_point_inside_rectangle_indexed;
            }
            else
            {
                read_simple = this::read_point_inside_rectangle;
            }
        }
        return TRUE;
    }

    public boolean read_point_inside_tile()
    {
        while (read_point_default())
        {
            if (point.inside_tile(t_ll_x, t_ll_y, t_ur_x, t_ur_y)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_inside_tile_indexed()
    {
        while (index.seek_next(this))
        {
            if (read_point_default() && point.inside_tile(t_ll_x, t_ll_y, t_ur_x, t_ur_y)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_inside_circle()
    {
        while (read_point_default())
        {
            if (point.inside_circle(c_center_x, c_center_y, c_radius_squared)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_inside_circle_indexed()
    {
        while (index.seek_next(this))
        {
            if (read_point_default() && point.inside_circle(c_center_x, c_center_y, c_radius_squared)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_inside_rectangle()
    {
        while (read_point_default())
        {
            if (point.inside_rectangle(r_min_x, r_min_y, r_max_x, r_max_y)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_inside_rectangle_indexed()
    {
        while (index.seek_next(this))
        {
            if (read_point_default() && point.inside_rectangle(r_min_x, r_min_y, r_max_x, r_max_y)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_none()
    {
        return FALSE;
    }

    public boolean read_point_filtered()
    {
        while (this.read_complex())
        {
            if (!filter.filter(point)) return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_transformed()
    {
        if (this.read_complex())
        {
            transform.transform(point);
            return TRUE;
        }
        return FALSE;
    }

    public boolean read_point_filtered_and_transformed()
    {
        if (read_point_filtered())
        {
            transform.transform(point);
            return TRUE;
        }
        return FALSE;
    }



    public abstract int get_format();
    public boolean has_layers() { return FALSE; };

    public LASindex get_index() { return index; };
    public LASfilter get_filter() { return filter; };
    public LAStransform get_transform() { return transform; };

    public int get_inside() { return inside; };
    public float get_t_ll_x() { return t_ll_x; };
    public float get_t_ll_y() { return t_ll_y; };
    public float get_t_size() { return t_size; };
    public double get_c_center_x() { return c_center_x; };
    public double get_c_center_y() { return c_center_y; };
    public double get_c_radius() { return c_radius; };
    public double get_r_min_x() { return r_min_x; };
    public double get_r_min_y() { return r_min_y; };
    public double get_r_max_x() { return r_max_x; };
    public double get_r_max_y() { return r_max_y; };

    public abstract boolean seek(long p_index);
    public boolean read_point() {
        try {
            return this.read_simple.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void compute_coordinates() { point.compute_coordinates(); };

    public double get_min_x() { return header.min_x; };
    public double get_min_y() { return header.min_y; };
    public double get_min_z() { return header.min_z; };

    public double get_max_x() { return header.max_x; };
    public double get_max_y() { return header.max_y; };
    public double get_max_z() { return header.max_z; };

    public double get_x() { return get_x(point.get_X()); };
    public double get_y() { return get_y(point.get_Y()); };
    public double get_z() { return get_z(point.get_Z()); };

    public double get_x(int x) { return header.get_x(x); };
    public double get_y(int y) { return header.get_y(y); };
    public double get_z(int z) { return header.get_z(z); };

    public int get_X(double x) { return header.get_X(x); };
    public int get_Y(double y) { return header.get_Y(y); };
    public int get_Z(double z) { return header.get_Z(z); };

    public abstract ByteStreamIn get_stream();
    public void close() { close(true);}
    public abstract void close(boolean close_stream);

    protected abstract boolean read_point_default();

    private boolean read_complex() {
        try {
            return read_complex.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean read_simple() {
        try {
            return read_simple.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public boolean reopen(String file_name) {
        return false;
    }
}
