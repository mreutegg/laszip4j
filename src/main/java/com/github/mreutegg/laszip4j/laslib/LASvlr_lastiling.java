/*
 * Copyright 2005-2014, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

public class LASvlr_lastiling {
    public int level; // unsigned
    public int level_index;  // unsigned
    public int implicit_levels = 30;  // unsigned
    public int buffer = 1;  // unsigned
    public int reversible = 1;  // unsigned
    public float min_x;
    public float max_x;
    public float min_y;
    public float max_y;
}
