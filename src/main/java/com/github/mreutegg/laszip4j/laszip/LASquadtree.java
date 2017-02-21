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

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_QUANTIZE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.asByteArray;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.realloc;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

public class LASquadtree {

    public static final int LAS_SPATIAL_QUAD_TREE = 0;

    private static final PrintStream stderr = System.err;

    public int levels; // unsigned
    public float cell_size;
    public float min_x;
    public float max_x;
    public float min_y;
    public float max_y;
    public int cells_x; // unsigned
    public int cells_y; // unsigned
    public int current_cell;
    
    private int sub_level; // unsigned
    private int sub_level_index; // unsigned
    private int[] level_offset = new int[24]; // unsigned
    private int[] coarser_indices = new int[4]; // unsigned
    private int adaptive_alloc; // unsigned
    private int[] adaptive;
    private List<Integer> current_cells;
    private int next_cell_index; // unsigned

    // returns the bounding box of the cell that x & y fall into at the specified level
    void get_cell_bounding_box(double x, double y, int level, float[] min, float[] max)
    {
        float cell_mid_x;
        float cell_mid_y;
        float cell_min_x, cell_max_x;
        float cell_min_y, cell_max_y;

        cell_min_x = min_x;
        cell_max_x = max_x;
        cell_min_y = min_y;
        cell_max_y = max_y;

        while (level != 0)
        {
            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;
            if (x < cell_mid_x)
            {
                cell_max_x = cell_mid_x;
            }
            else
            {
                cell_min_x = cell_mid_x;
            }
            if (y < cell_mid_y)
            {
                cell_max_y = cell_mid_y;
            }
            else
            {
                cell_min_y = cell_mid_y;
            }
            level--;
        }
        if (min != null)
        {
            min[0] = cell_min_x;
            min[1] = cell_min_y;
        }
        if (max != null)
        {
            max[0] = cell_max_x;
            max[1] = cell_max_y;
        }
    }

    // returns the bounding box of the cell that x & y fall into
    void get_cell_bounding_box(double x, double y, float[] min, float[] max)
    {
        get_cell_bounding_box(x, y, levels, min, max);
    }

    // returns the bounding box of the cell with the specified level_index at the specified level
    void get_cell_bounding_box(int level_index, int level, float[] min, float[] max)
    {
        float cell_mid_x;
        float cell_mid_y;
        float cell_min_x, cell_max_x;
        float cell_min_y, cell_max_y;

        cell_min_x = min_x;
        cell_max_x = max_x;
        cell_min_y = min_y;
        cell_max_y = max_y;

        int index;
        while (level != 0)
        {
            index = (level_index >>>(2*(level-1)))&3;
            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;
            if ((index & 1) != 0)
            {
                cell_min_x = cell_mid_x;
            }
            else
            {
                cell_max_x = cell_mid_x;
            }
            if ((index & 2) != 0)
            {
                cell_min_y = cell_mid_y;
            }
            else
            {
                cell_max_y = cell_mid_y;
            }
            level--;
        }
        if (min != null)
        {
            min[0] = cell_min_x;
            min[1] = cell_min_y;
        }
        if (max != null)
        {
            max[0] = cell_max_x;
            max[1] = cell_max_y;
        }
    }

    // returns the bounding box of the cell with the specified level_index at the specified level
    void get_cell_bounding_box(int level_index, int level, double[] min, double[] max)
    {
        double cell_mid_x;
        double cell_mid_y;
        double cell_min_x, cell_max_x;
        double cell_min_y, cell_max_y;

        cell_min_x = min_x;
        cell_max_x = max_x;
        cell_min_y = min_y;
        cell_max_y = max_y;

        int index;
        while (level != 0)
        {
            index = (level_index >>>(2*(level-1)))&3;
            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;
            if ((index & 1) != 0)
            {
                cell_min_x = cell_mid_x;
            }
            else
            {
                cell_max_x = cell_mid_x;
            }
            if ((index & 2) != 0)
            {
                cell_min_y = cell_mid_y;
            }
            else
            {
                cell_max_y = cell_mid_y;
            }
            level--;
        }
        if (min != null)
        {
            min[0] = cell_min_x;
            min[1] = cell_min_y;
        }
        if (max != null)
        {
            max[0] = cell_max_x;
            max[1] = cell_max_y;
        }
    }

    // returns the bounding box of the cell with the specified level_index
    void get_cell_bounding_boxU(int level_index, // unsigned
                               float[] min, float[] max)
    {
        get_cell_bounding_box(level_index, levels, min, max);
    }

    // returns the bounding box of the cell with the specified level_index
    void get_cell_bounding_box(int level_index, double[] min, double[] max)
    {
        get_cell_bounding_box(level_index, levels, min, max);
    }

    // returns the bounding box of the cell with the specified cell_index
    void get_cell_bounding_boxI(int cell_index, // signed
                               float[] min, float[] max)
    {
        int level = get_level(cell_index);
        int level_index = get_level_index(cell_index, level);
        get_cell_bounding_box(level_index, level, min, max);
    }

    // returns the (sub-)level index of the cell that x & y fall into at the specified level
    int get_level_index(double x, double y, int level)
    {
        float cell_mid_x;
        float cell_mid_y;
        float cell_min_x, cell_max_x;
        float cell_min_y, cell_max_y;

        cell_min_x = min_x;
        cell_max_x = max_x;
        cell_min_y = min_y;
        cell_max_y = max_y;

        int level_index = 0;

        while (level != 0)
        {
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (x < cell_mid_x)
            {
                cell_max_x = cell_mid_x;
            }
            else
            {
                cell_min_x = cell_mid_x;
                level_index |= 1;
            }
            if (y < cell_mid_y)
            {
                cell_max_y = cell_mid_y;
            }
            else
            {
                cell_min_y = cell_mid_y;
                level_index |= 2;
            }
            level--;
        }

        return level_index;
    }

    // returns the (sub-)level index of the cell that x & y fall into
    int get_level_index(double x, double y)
    {
        return get_level_index(x, y, levels);
    }

    // returns the (sub-)level index and the bounding box of the cell that x & y fall into at the specified level
    int get_level_index(double x, double y, int level, float[] min, float[] max)
    {
        float cell_mid_x;
        float cell_mid_y;
        float cell_min_x, cell_max_x;
        float cell_min_y, cell_max_y;

        cell_min_x = min_x;
        cell_max_x = max_x;
        cell_min_y = min_y;
        cell_max_y = max_y;

        int level_index = 0;

        while (level != 0)
        {
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (x < cell_mid_x)
            {
                cell_max_x = cell_mid_x;
            }
            else
            {
                cell_min_x = cell_mid_x;
                level_index |= 1;
            }
            if (y < cell_mid_y)
            {
                cell_max_y = cell_mid_y;
            }
            else
            {
                cell_min_y = cell_mid_y;
                level_index |= 2;
            }
            level--;
        }
        if (min != null)
        {
            min[0] = cell_min_x;
            min[1] = cell_min_y;
        }
        if (max != null)
        {
            max[0] = cell_max_x;
            max[1] = cell_max_y;
        }
        return level_index;
    }

    // returns the (sub-)level index and the bounding box of the cell that x & y fall into
    int get_level_index(double x, double y, float[] min, float[] max)
    {
        return get_level_index(x, y, levels, min, max);
    }

    // returns the index of the cell that x & y fall into at the specified level
    int get_cell_index(double x, double y, int level)
    {
        if (sub_level != 0)
        {
            return level_offset[sub_level+level] + (sub_level_index << (level*2)) + get_level_index(x, y, level);
        }
        else
        {
            return level_offset[level]+get_level_index(x, y, level);
        }
    }

    // returns the index of the cell that x & y fall into
    int get_cell_index(double x, double y)
    {
        return get_cell_index(x, y, levels);
    }

    // returns the indices of parent and siblings for the specified cell index
    boolean coarsen(int cell_index, int[] coarser_cell_index, int[] num_cell_indices, int[][] cell_indices)
    {
        if (cell_index < 0) return FALSE;
        int level = get_level(cell_index);
        if (level == 0) return FALSE;
        int level_index = get_level_index(cell_index, level);
        level_index = level_index >>> 2;
        if (coarser_cell_index != null) coarser_cell_index[0] = get_cell_index(level_index, level-1);
        if (num_cell_indices != null && cell_indices != null)
        {
            num_cell_indices[0] = 4;
            cell_indices[0] = coarser_indices;
            level_index = level_index << 2;
            cell_indices[0][0] = get_cell_index(level_index + 0, level);
            cell_indices[0][1] = get_cell_index(level_index + 1, level);
            cell_indices[0][2] = get_cell_index(level_index + 2, level);
            cell_indices[0][3] = get_cell_index(level_index + 3, level);
        }
        return TRUE;
    }

    // returns the level index of the cell index at the specified level
    int get_level_index(int cell_index, int level)
    {
        if (sub_level != 0)
        {
            return cell_index - (sub_level_index << (level*2)) - level_offset[sub_level+level];
        }
        else
        {
            return cell_index - level_offset[level];
        }
    }

    // returns the level index of the cell index
    int get_level_index(int cell_index)
    {
        return get_level_index(cell_index, levels);
    }

    // returns the level the cell index
    int get_level(int cell_index)
    {
        int level = 0;
        while (Integer.compareUnsigned(cell_index, level_offset[level+1]) >= 0) level++;
        return level;
    }

    // returns the cell index of the level index at the specified level
    int get_cell_index(int level_index, int level)
    {
        if (sub_level != 0)
        {
            return level_index + (sub_level_index << (level*2)) + level_offset[sub_level+level];
        }
        else
        {
            return level_index + level_offset[level];
        }
    }

    // returns the cell index of the level index
    int get_cell_index(int level_index)
    {
        return get_cell_index(level_index, levels);
    }

    // returns the maximal level index at the specified level
    int get_max_level_index(int level)
    {
        return (1<<level)*(1<<level);
    }

    // returns the maximal level index
    int get_max_level_index()
    {
        return get_max_level_index(levels);
    }

    // returns the maximal cell index at the specified level
    int get_max_cell_index(int level)
    {
        return level_offset[level+1]-1;
    }

    // returns the maximal cell index
    int get_max_cell_index()
    {
        return get_max_cell_index(levels);
    }

    // recursively does the actual rastering of the occupancy
    void raster_occupancy(Function<Integer, Boolean> does_cell_exist, int[] data, int min_x, int min_y, int level_index, int level, int stop_level)
    {
        int cell_index = get_cell_index(level_index, level);
        int adaptive_pos = cell_index/32;
        int adaptive_bit = 1 << (cell_index%32);
        // have we reached a leaf
        if ((adaptive[adaptive_pos] & adaptive_bit) != 0) // interior node
        {
            if (level < stop_level) // do we need to continue
            {
                level_index <<= 2;
                level += 1;
                int size = 1 << (stop_level-level);
                // recurse into the four children
                raster_occupancy(does_cell_exist, data, min_x, min_y, level_index, level, stop_level);
                raster_occupancy(does_cell_exist, data, min_x+size, min_y, level_index + 1, level, stop_level);
                raster_occupancy(does_cell_exist, data, min_x, min_y+size, level_index + 2, level, stop_level);
                raster_occupancy(does_cell_exist, data, min_x+size, min_y+size, level_index + 3, level, stop_level);
                return;
            }
            else // no ... raster remaining subtree
            {
                int full_size = (1 << stop_level);
                int size = 1 << (stop_level-level);
                int max_y = min_y + size;
                int pos, pos_x, pos_y;
                for (pos_y = min_y; pos_y < max_y; pos_y++)
                {
                    pos = pos_y*full_size + min_x;
                    for (pos_x = 0; pos_x < size; pos_x++)
                    {
                        data[pos/32] |= (1<<(pos%32));
                        pos++;
                    }
                }
            }
        }
        else if (does_cell_exist.apply(cell_index))
        {
            // raster actual cell
            int full_size = (1 << stop_level);
            int size = 1 << (stop_level-level);
            int max_y = min_y + size;
            int pos, pos_x, pos_y;
            for (pos_y = min_y; pos_y < max_y; pos_y++)
            {
                pos = pos_y*full_size + min_x;
                for (pos_x = 0; pos_x < size; pos_x++)
                {
                    data[pos/32] |= (1<<(pos%32));
                    pos++;
                }
            }
        }
    }

    // rasters the occupancy to a simple binary raster at depth level
    int[] raster_occupancy(Function<Integer, Boolean> does_cell_exist, int level)
    {
        int size_xy = (1<<level);
        int temp_size = (size_xy*size_xy)/32 + (((size_xy*size_xy) % 32) != 0 ? 1 : 0);
        int[] data = new int[temp_size];
        raster_occupancy(does_cell_exist, data, 0, 0, 0, 0, level);
        return data;
    }

    // rasters the occupancy to a simple binary raster at depth levels
    int[] raster_occupancy(Function<Integer, Boolean> does_cell_exist)
    {
        return raster_occupancy(does_cell_exist, levels);
    }

    // read from file
    boolean read(ByteStreamIn stream)
    {
        // read data in the following order
        //     U32  levels          4 bytes 
        //     U32  level_index     4 bytes (default 0)
        //     U32  implicit_levels 4 bytes (only used when level_index != 0))
        //     float  min_x           4 bytes 
        //     float  max_x           4 bytes 
        //     float  min_y           4 bytes 
        //     float  max_y           4 bytes 
        // which totals 28 bytes

        byte[] signature = new byte[4];
        try { stream.getBytes(signature, 4); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading LASspatial signature\n");
            return FALSE;
        }
        if (strncmp(stringFromByteArray(signature), "LASS", 4) != 0)
        {
            fprintf(stderr,"ERROR (LASquadtree): wrong LASspatial signature %4s instead of 'LASS'\n", stringFromByteArray(signature));
            return FALSE;
        }
        int type;
        try { type = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading LASspatial type\n");
            return FALSE;
        }
        if (type != LAS_SPATIAL_QUAD_TREE)
        {
            fprintf(stderr,"ERROR (LASquadtree): unknown LASspatial type %d\n", type);
            return FALSE;
        }
        try { stream.getBytes(signature, 4); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading signature\n");
            return FALSE;
        }
        if (strncmp(stringFromByteArray(signature), "LASQ", 4) != 0)
        {
            //    fprintf(stderr,"ERROR (LASquadtree): wrong signature %4s instead of 'LASV'\n", signature);
            //    return FALSE;
            levels = ByteBuffer.wrap(signature).order(ByteOrder.LITTLE_ENDIAN).getInt();
        }
        else
        {
            int version;
            try { version = stream.get32bitsLE(); } catch (Exception e)
            {
                fprintf(stderr,"ERROR (LASquadtree): reading version\n");
                return FALSE;
            }
            try { levels = stream.get32bitsLE(); } catch (Exception e)
            {
                fprintf(stderr,"ERROR (LASquadtree): reading levels\n");
                return FALSE;
            }
        }
        int level_index;
        try { level_index = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading level_index\n");
            return FALSE;
        }
        int implicit_levels;
        try { implicit_levels = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading implicit_levels\n");
            return FALSE;
        }
        try { min_x = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading min_x\n");
            return FALSE;
        }
        try { max_x = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading max_x\n");
            return FALSE;
        }
        try { min_y = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading min_y\n");
            return FALSE;
        }
        try { max_y = intBitsToFloat(stream.get32bitsLE()); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASquadtree): reading max_y\n");
            return FALSE;
        }
        return TRUE;
    }

    boolean write(ByteStreamOut stream)
    {
        // which totals 28 bytes
        //     U32  levels          4 bytes 
        //     U32  level_index     4 bytes (default 0)
        //     U32  implicit_levels 4 bytes (only used when level_index != 0))
        //     float  min_x           4 bytes 
        //     float  max_x           4 bytes 
        //     float  min_y           4 bytes 
        //     float  max_y           4 bytes 
        // which totals 28 bytes

        if (!stream.putBytes(asByteArray("LASS"), 4))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing LASspatial signature\n");
            return FALSE;
        }

        int type = LAS_SPATIAL_QUAD_TREE;
        if (!stream.put32bitsLE(type))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing LASspatial type %d\n", type);
            return FALSE;
        }

        if (!stream.putBytes(asByteArray("LASQ"), 4))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing signature\n");
            return FALSE;
        }

        int version = 0;
        if (!stream.put32bitsLE(version))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing version\n");
            return FALSE;
        }

        if (!stream.put32bitsLE(levels))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing levels %d\n", levels);
            return FALSE;
        }
        int level_index = 0;
        if (!stream.put32bitsLE(level_index))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing level_index %d\n", level_index);
            return FALSE;
        }
        int implicit_levels = 0;
        if (!stream.put32bitsLE(implicit_levels))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing implicit_levels %d\n", implicit_levels);
            return FALSE;
        }
        if (!stream.put32bitsLE(floatToIntBits(min_x)))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing min_x %g\n", min_x);
            return FALSE;
        }
        if (!stream.put32bitsLE(floatToIntBits(max_x)))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing max_x %g\n", max_x);
            return FALSE;
        }
        if (!stream.put32bitsLE(floatToIntBits(min_y)))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing min_y %g\n", min_y);
            return FALSE;
        }
        if (!stream.put32bitsLE(floatToIntBits(max_y)))
        {
            fprintf(stderr,"ERROR (LASquadtree): writing max_y %g\n", max_y);
            return FALSE;
        }
        return TRUE;
    }

    boolean manage_cell(int cell_index) {
        return manage_cell(cell_index, false);
    }

    // create or finalize the cell (in the spatial hierarchy) 
    boolean manage_cell(int cell_index, boolean finalize)
    {
        int adaptive_pos = cell_index/32;
        int adaptive_bit = (1) << (cell_index%32);
        if (adaptive_pos >= adaptive_alloc)
        {
            if (adaptive != null)
            {
                adaptive = realloc(adaptive, adaptive_pos*2);
                for (int i = adaptive_alloc; i < adaptive_pos*2; i++) adaptive[i] = 0;
                adaptive_alloc = adaptive_pos*2;
            }
            else
            {
                adaptive = new int[adaptive_pos+1];
                for (int i = adaptive_alloc; i <= adaptive_pos; i++) adaptive[i] = 0;
                adaptive_alloc = adaptive_pos+1;
            }
        }
        adaptive[adaptive_pos] &= ~adaptive_bit;
        int index;
        int level = get_level(cell_index);
        int level_index = get_level_index(cell_index, level);
        while (level != 0)
        {
            level--;
            level_index = level_index >> 2;
            index = get_cell_index(level_index, level);
            adaptive_pos = index/32;
            adaptive_bit = (1) << (index%32);
            if ((adaptive[adaptive_pos] & adaptive_bit) != 0) break;
            adaptive[adaptive_pos] |= adaptive_bit;
        }
        return TRUE;
    }

    // check whether the x & y coordinates fall into the tiling
    boolean inside(double x, double y)
    {
        return ((min_x <= x) && (x < max_x) && (min_y <= y) && (y < max_y));
    }

    int intersect_rectangle(double r_min_x, double r_min_y, double r_max_x, double r_max_y, int level)
    {
        if (current_cells == null)
        {
            current_cells = new ArrayList<>();
        }
        else
        {
            current_cells.clear();
        }

        if (r_max_x <= min_x || !(r_min_x <= max_x) || r_max_y <= min_y || !(r_min_y <= max_y))
        {
            return 0;
        }

        if (adaptive != null)
        {
            intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, min_x, max_x, min_y, max_y, 0, 0);
        }
        else
        {
            intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, min_x, max_x, min_y, max_y, level, 0);
        }

        return current_cells.size();
    }

    int intersect_rectangle(double r_min_x, double r_min_y, double r_max_x, double r_max_y)
    {
        return intersect_rectangle(r_min_x, r_min_y, r_max_x, r_max_y, levels);
    }

    int intersect_tile(float ll_x, float ll_y, float size, int level)
    {
        if (current_cells == null)
        {
            current_cells = new ArrayList<>();
        }
        else
        {
            current_cells.clear();
        }

        float ur_x = ll_x + size;
        float ur_y = ll_y + size;

        if (ur_x <= min_x || !(ll_x <= max_x) || ur_y <= min_y || !(ll_y <= max_y))
        {
            return 0;
        }

        if (adaptive != null)
        {
            intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, min_x, max_x, min_y, max_y, 0, 0);
        }
        else
        {
            intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, min_x, max_x, min_y, max_y, level, 0);
        }

        return current_cells.size();
    }

    int intersect_tile(float ll_x, float ll_y, float size)
    {
        return intersect_tile(ll_x, ll_y, size, levels);
    }

    int intersect_circle(double center_x, double center_y, double radius, int level)
    {
        if (current_cells == null)
        {
            current_cells = new ArrayList<>();
        }
        else
        {
            current_cells.clear();
        }

        double r_min_x = center_x - radius;
        double r_min_y = center_y - radius;
        double r_max_x = center_x + radius;
        double r_max_y = center_y + radius;

        if (r_max_x <= min_x || !(r_min_x <= max_x) || r_max_y <= min_y || !(r_min_y <= max_y))
        {
            return 0;
        }

        if (adaptive != null)
        {
            intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, min_x, max_x, min_y, max_y, 0, 0);
        }
        else
        {
            intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, min_x, max_x, min_y, max_y, level, 0);
        }

        return current_cells.size();
    }

    int intersect_circle(double center_x, double center_y, double radius)
    {
        return intersect_circle(center_x, center_y, radius, levels);
    }

    void intersect_rectangle_with_cells(double r_min_x, double r_min_y, double r_max_x, double r_max_y, float cell_min_x, float cell_max_x, float cell_min_y, float cell_max_y, int level, int level_index)
    {
        float cell_mid_x;
        float cell_mid_y;
        if (level != 0)
        {
            level--;
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (r_max_x <= cell_mid_x)
            {
                // cell_max_x = cell_mid_x;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
                else
                {
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
            }
            else if (!(r_min_x < cell_mid_x))
            {
                // cell_min_x = cell_mid_x;
                // level_index |= 1;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
            else
            {
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_rectangle_with_cells(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
        }
        else
        {
            current_cells.add(level_index);
        }
    }

    void intersect_rectangle_with_cells_adaptive(double r_min_x, double r_min_y, double r_max_x, double r_max_y, float cell_min_x, float cell_max_x, float cell_min_y, float cell_max_y, int level, int level_index)
    {
        float cell_mid_x;
        float cell_mid_y;
        int cell_index = get_cell_index(level_index, level);
        int adaptive_pos = cell_index/32;
        int adaptive_bit = (1) << (cell_index%32);
        if ((level < levels) && ((adaptive[adaptive_pos] & adaptive_bit) != 0))
        {
            level++;
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (r_max_x <= cell_mid_x)
            {
                // cell_max_x = cell_mid_x;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
                else
                {
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
            }
            else if (!(r_min_x < cell_mid_x))
            {
                // cell_min_x = cell_mid_x;
                // level_index |= 1;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
            else
            {
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_rectangle_with_cells_adaptive(r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
        }
        else
        {
            current_cells.add(cell_index);
        }
    }

    void intersect_tile_with_cells(float ll_x, float ll_y, float ur_x, float ur_y, float cell_min_x, float cell_max_x, float cell_min_y, float cell_max_y, int level, int level_index)
    {
        float cell_mid_x;
        float cell_mid_y;
        if (level != 0)
        {
            level--;
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (ur_x <= cell_mid_x)
            {
                // cell_max_x = cell_mid_x;
                if (ur_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                }
                else if (!(ll_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
                else
                {
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
            }
            else if (!(ll_x < cell_mid_x))
            {
                // cell_min_x = cell_mid_x;
                // level_index |= 1;
                if (ur_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(ll_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
            else
            {
                if (ur_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(ll_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_tile_with_cells(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
        }
        else
        {
            current_cells.add(level_index);
        }
    }

    void intersect_tile_with_cells_adaptive(float ll_x, float ll_y, float ur_x, float ur_y, float cell_min_x, float cell_max_x, float cell_min_y, float cell_max_y, int level, int level_index)
    {
        float cell_mid_x;
        float cell_mid_y;
        int cell_index = get_cell_index(level_index, level);
        int adaptive_pos = cell_index/32;
        int adaptive_bit = (1) << (cell_index%32);
        if ((level < levels) && ((adaptive[adaptive_pos] & adaptive_bit) != 0))
        {
            level++;
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (ur_x <= cell_mid_x)
            {
                // cell_max_x = cell_mid_x;
                if (ur_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                }
                else if (!(ll_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
                else
                {
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
            }
            else if (!(ll_x < cell_mid_x))
            {
                // cell_min_x = cell_mid_x;
                // level_index |= 1;
                if (ur_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(ll_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
            else
            {
                if (ur_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(ll_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_tile_with_cells_adaptive(ll_x, ll_y, ur_x, ur_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
        }
        else
        {
            current_cells.add(cell_index);
        }
    }

    void intersect_circle_with_cells(double center_x, double center_y, double radius, double r_min_x, double r_min_y, double r_max_x, double r_max_y, float cell_min_x, float cell_max_x, float cell_min_y, float cell_max_y, int level, int level_index)
    {
        float cell_mid_x;
        float cell_mid_y;
        if (level != 0)
        {
            level--;
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (r_max_x <= cell_mid_x)
            {
                // cell_max_x = cell_mid_x;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
                else
                {
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
            }
            else if (!(r_min_x < cell_mid_x))
            {
                // cell_min_x = cell_mid_x;
                // level_index |= 1;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
            else
            {
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_circle_with_cells(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
        }
        else
        {
            if (intersect_circle_with_rectangle(center_x, center_y, radius, cell_min_x, cell_max_x, cell_min_y, cell_max_y))
            {
                current_cells.add(level_index);
            }
        }
    }

    void intersect_circle_with_cells_adaptive(double center_x, double center_y, double radius, double r_min_x, double r_min_y, double r_max_x, double r_max_y, float cell_min_x, float cell_max_x, float cell_min_y, float cell_max_y, int level, int level_index)
    {
        float cell_mid_x;
        float cell_mid_y;
        int cell_index = get_cell_index(level_index, level);
        int adaptive_pos = cell_index/32;
        int adaptive_bit = (1) << (cell_index%32);
        if ((level < levels) && ((adaptive[adaptive_pos] & adaptive_bit) != 0))
        {
            level++;
            level_index <<= 2;

            cell_mid_x = (cell_min_x + cell_max_x)/2;
            cell_mid_y = (cell_min_y + cell_max_y)/2;

            if (r_max_x <= cell_mid_x)
            {
                // cell_max_x = cell_mid_x;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
                else
                {
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                }
            }
            else if (!(r_min_x < cell_mid_x))
            {
                // cell_min_x = cell_mid_x;
                // level_index |= 1;
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
            else
            {
                if (r_max_y <= cell_mid_y)
                {
                    // cell_max_y = cell_mid_y;
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                }
                else if (!(r_min_y < cell_mid_y))
                {
                    // cell_min_y = cell_mid_y;
                    // level_index |= 1;
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
                else
                {
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_min_y, cell_mid_y, level, level_index);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_min_y, cell_mid_y, level, level_index | 1);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_min_x, cell_mid_x, cell_mid_y, cell_max_y, level, level_index | 2);
                    intersect_circle_with_cells_adaptive(center_x, center_y, radius, r_min_x, r_min_y, r_max_x, r_max_y, cell_mid_x, cell_max_x, cell_mid_y, cell_max_y, level, level_index | 3);
                }
            }
        }
        else
        {
            if (intersect_circle_with_rectangle(center_x, center_y, radius, cell_min_x, cell_max_x, cell_min_y, cell_max_y))
            {
                current_cells.add(cell_index);
            }
        }
    }

    boolean intersect_circle_with_rectangle(double center_x, double center_y, double radius, float r_min_x, float r_max_x, float r_min_y, float r_max_y)
    {
        double r_diff_x, r_diff_y;
        double radius_squared = radius * radius;
        if (r_max_x < center_x) // R to left of circle center
        {
            r_diff_x = center_x - r_max_x;
            if (r_max_y < center_y) // R in lower left corner
            {
                r_diff_y = center_y - r_max_y;
                return ((r_diff_x * r_diff_x + r_diff_y * r_diff_y) < radius_squared);
            }
            else if (r_min_y > center_y) // R in upper left corner
            {
                r_diff_y = -center_y + r_min_y;
                return ((r_diff_x * r_diff_x + r_diff_y * r_diff_y) < radius_squared);
            }
            else // R due West of circle
            {
                return (r_diff_x < radius);
            }
        }
        else if (r_min_x > center_x) // R to right of circle center
        {
            r_diff_x = -center_x + r_min_x;
            if (r_max_y < center_y) // R in lower right corner
            {
                r_diff_y = center_y - r_max_y;
                return ((r_diff_x * r_diff_x + r_diff_y * r_diff_y) < radius_squared);
            }
            else if (r_min_y > center_y) // R in upper right corner
            {
                r_diff_y = -center_y + r_min_y;
                return ((r_diff_x * r_diff_x + r_diff_y * r_diff_y) < radius_squared);
            }
            else // R due East of circle
            {
                return (r_diff_x < radius);
            }
        }
        else // R on circle vertical centerline
        {
            if (r_max_y < center_y) // R due South of circle
            {
                r_diff_y = center_y - r_max_y;
                return (r_diff_y < radius);
            }
            else if (r_min_y > center_y) // R due North of circle
            {
                r_diff_y = -center_y + r_min_y;
                return (r_diff_y < radius);
            }
            else // R contains circle centerpoint
            {
                return TRUE;
            }
        }
    }

    boolean get_all_cells()
    {
        intersect_rectangle(min_x, min_y, max_x, max_y);
        return get_intersected_cells();
    }

    boolean get_intersected_cells()
    {
        next_cell_index = 0;
        if (current_cells == null)
        {
            return FALSE;
        }
        if (current_cells.size() == 0)
        {
            return FALSE;
        }
        return TRUE;
    }

    boolean has_more_cells()
    {
        if (current_cells == null)
        {
            return FALSE;
        }
        if (next_cell_index >= current_cells.size())
        {
            return FALSE;
        }
        if (adaptive != null)
        {
            current_cell = current_cells.get(next_cell_index);
        }
        else
        {
            current_cell = level_offset[levels] + current_cells.get(next_cell_index);
        }
        next_cell_index++;
        return TRUE;
    }

    public boolean setup(double bb_min_x, double bb_max_x, double bb_min_y, double bb_max_y, float cell_size)
    {
        this.cell_size = cell_size;
        this.sub_level = 0;
        this.sub_level_index = 0;

        // enlarge bounding box to units of cells
        if (bb_min_x >= 0) min_x = cell_size*((int)(bb_min_x/cell_size));
        else min_x = cell_size*((int)(bb_min_x/cell_size)-1);
        if (bb_max_x >= 0) max_x = cell_size*((int)(bb_max_x/cell_size)+1);
        else max_x = cell_size*((int)(bb_max_x/cell_size));
        if (bb_min_y >= 0) min_y = cell_size*((int)(bb_min_y/cell_size));
        else min_y = cell_size*((int)(bb_min_y/cell_size)-1);
        if (bb_max_y >= 0) max_y = cell_size*((int)(bb_max_y/cell_size)+1);
        else max_y = cell_size*((int)(bb_max_y/cell_size));

        // how many cells minimally in each direction
        cells_x = U32_QUANTIZE((max_x - min_x)/cell_size);
        cells_y = U32_QUANTIZE((max_y - min_y)/cell_size);

        if (cells_x == 0 || cells_y == 0)
        {
            fprintf(stderr, "ERROR: cells_x %d cells_y %d\n", cells_x, cells_y);
            return FALSE;
        }

        // how many quad tree levels to get to that many cells
        int c = ((cells_x > cells_y) ? cells_x - 1 : cells_y - 1);
        levels = 0;
        while (c != 0)
        {
            c = c >>> 1;
            levels++;
        }

        // enlarge bounding box to quad tree size
        int c1, c2;
        c = (1 << levels) - cells_x;
        c1 = c/2;
        c2 = c - c1;
        min_x -= (c2 * cell_size);
        max_x += (c1 * cell_size);
        c = (1 << levels) - cells_y;
        c1 = c/2;
        c2 = c - c1;
        min_y -= (c2 * cell_size);
        max_y += (c1 * cell_size);

        return TRUE;
    }

    boolean setup(double bb_min_x, double bb_max_x, double bb_min_y, double bb_max_y, float cell_size, float offset_x, float offset_y)
    {
        this.cell_size = cell_size;
        this.sub_level = 0;
        this.sub_level_index = 0;

        // enlarge bounding box to units of cells
        if ((bb_min_x-offset_x) >= 0) min_x = cell_size*((int)((bb_min_x-offset_x)/cell_size)) + offset_x;
        else min_x = cell_size*((int)((bb_min_x-offset_x)/cell_size)-1) + offset_x;
        if ((bb_max_x-offset_x) >= 0) max_x = cell_size*((int)((bb_max_x-offset_x)/cell_size)+1) + offset_x;
        else max_x = cell_size*((int)((bb_max_x-offset_x)/cell_size)) + offset_x;
        if ((bb_min_y-offset_y) >= 0) min_y = cell_size*((int)((bb_min_y-offset_y)/cell_size)) + offset_y;
        else min_y = cell_size*((int)((bb_min_y-offset_y)/cell_size)-1) + offset_y;
        if ((bb_max_y-offset_y) >= 0) max_y = cell_size*((int)((bb_max_y-offset_y)/cell_size)+1) + offset_y;
        else max_y = cell_size*((int)((bb_max_y-offset_y)/cell_size)) + offset_y;

        // how many cells minimally in each direction
        cells_x = U32_QUANTIZE((max_x - min_x)/cell_size);
        cells_y = U32_QUANTIZE((max_y - min_y)/cell_size);

        if (cells_x == 0 || cells_y == 0)
        {
            fprintf(stderr, "ERROR: cells_x %d cells_y %d\n", cells_x, cells_y);
            return FALSE;
        }

        // how many quad tree levels to get to that many cells
        int c = ((cells_x > cells_y) ? cells_x - 1 : cells_y - 1);
        levels = 0;
        while (c != 0)
        {
            c = c >> 1;
            levels++;
        }

        // enlarge bounding box to quad tree size
        int c1, c2;
        c = (1 << levels) - cells_x;
        c1 = c/2;
        c2 = c - c1;
        min_x -= (c2 * cell_size);
        max_x += (c1 * cell_size);
        c = (1 << levels) - cells_y;
        c1 = c/2;
        c2 = c - c1;
        min_y -= (c2 * cell_size);
        max_y += (c1 * cell_size);

        return TRUE;
    }

    boolean tiling_setup(float min_x, float max_x, float min_y, float max_y, int levels)
    {
        this.min_x = min_x;
        this.max_x = max_x;
        this.min_y = min_y;
        this.max_y = max_y;
        this.levels = levels;
        this.sub_level = 0;
        this.sub_level_index = 0;
        return TRUE;
    }

    boolean subtiling_setup(float min_x, float max_x, float min_y, float max_y, int sub_level, int sub_level_index, int levels)
    {
        this.min_x = min_x;
        this.max_x = max_x;
        this.min_y = min_y;
        this.max_y = max_y;
        float[] min = new float[2];
        float[] max = new float[2];
        get_cell_bounding_box(sub_level_index, sub_level, min, max);
        this.min_x = min[0];
        this.max_x = max[0];
        this.min_y = min[1];
        this.max_y = max[1];
        this.sub_level = sub_level;
        this.sub_level_index = sub_level_index;
        this.levels = levels;
        return TRUE;
    }

    public LASquadtree()
    {
        int l;
        levels = 0;
        cell_size = 0;
        min_x = 0;
        max_x = 0;
        min_y = 0;
        max_y = 0;
        cells_x = 0;
        cells_y = 0;
        sub_level = 0;
        sub_level_index = 0;
        level_offset[0] = 0;
        for (l = 0; l < 23; l++)
        {
            level_offset[l+1] = level_offset[l] + ((1<<l)*(1<<l));
        }
        current_cells = null;
        adaptive_alloc = 0;
        adaptive = null;
    }
}
