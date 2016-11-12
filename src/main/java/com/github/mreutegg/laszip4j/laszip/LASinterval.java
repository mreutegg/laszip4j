/*
 * Copyright 2011-2015, martin isenburg, rapidlasso - fast tools to catch reality
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.IntStream;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I32_MIN;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.asByteArray;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASinterval {

    private static final PrintStream stderr = System.err;
    
    public int index;
    public int start; // unsigned
    public int end; // unsigned
    public int full; // unsigned
    public int total; // unsigned

    private SortedMap<Integer, LASintervalStartCell> cells;
    private Set<LASintervalCell> cells_to_merge;
    private int threshold;  // unsigned
    private int number_intervals; // unsigned
    private int last_index;
    private LASintervalStartCell last_cell;
    private LASintervalCell current_cell;
    private LASintervalStartCell merged_cells;
    private boolean merged_cells_temporary;

    public LASinterval() {
        this(1000);
    }

    boolean add(int p_index, int c_index)
    {
        if (last_cell == null || last_index != c_index)
        {
            last_index = c_index;
            LASintervalStartCell value = cells.get(c_index);
            if (value == null)
            {
                last_cell = new LASintervalStartCell(p_index);
                cells.put(c_index, last_cell);
                number_intervals++;
                return TRUE;
            }
            last_cell = value;
        }
        if (last_cell.add(p_index, threshold))
        {
            number_intervals++;
            return TRUE;
        }
        return FALSE;
    }

    // get total number of cells
    int get_number_cells()
    {
        return cells.size();
    }

    // get total number of intervals
    int get_number_intervals()
    {
        return number_intervals;
    }

    // merge cells (and their intervals) into one cell
    boolean merge_cells(int num_indices, int[] indices, int new_index)
    {
        int i;
        if (num_indices == 1)
        {
            LASintervalStartCell value = cells.get(indices[0]);
            if (value == null)
            {
                return FALSE;
            }
            cells.put(new_index, value);
            cells.remove(indices[0]);
        }
        else
        {
            if (cells_to_merge != null) cells_to_merge.clear();
            for (i = 0; i < num_indices; i++)
            {
                add_cell_to_merge_cell_set(indices[i], TRUE);
            }
            if (!merge(TRUE)) return FALSE;
            cells.put(new_index, merged_cells);
            merged_cells = null;
        }
        return TRUE;
    }

    // merge adjacent intervals with small gaps in cells to reduce total interval number to maximum
    void merge_intervals(int maximum_intervals, boolean verbose)
    {
        int diff;
        LASintervalCell cell;
        LASintervalCell delete_cell;

        // each cell has minimum one interval

        if (maximum_intervals < get_number_cells())
        {
            maximum_intervals = 0;
        }
        else
        {
            maximum_intervals -= get_number_cells();
        }

        // order intervals by smallest gap

        SortedMap<Integer, List<LASintervalCell>> map = new TreeMap<>();
        for (LASintervalCell c : cells.values()) {
            cell = c;
            while (cell.next != null)
            {
                diff = cell.next.start - cell.end - 1;
                insert(map, diff, cell);
                cell = cell.next;
            }
        }

        diff = map.firstKey();

        int size = size(map);
        // maybe nothing to do
        if (size <= maximum_intervals)
        {
            if (verbose) fprintf(stderr,"next largest interval gap is %d\n", diff);
            return;
        }

        while (size > maximum_intervals)
        {
            Map.Entry<Integer, List<LASintervalCell>> map_element = map.entrySet().iterator().next();
            diff = map_element.getKey();
            cell = map_element.getValue().remove(0);
            if (map_element.getValue().isEmpty()) {
                map.remove(diff);
            }
            if ((cell.start == 1) && (cell.end == 0)) // the (start == 1 && end == 0) signals that the cell is to be deleted
            {
                number_intervals--;
            }
            else
            {
                delete_cell = cell.next;
                cell.end = delete_cell.end;
                cell.next = delete_cell.next;
                if (cell.next != null)
                {
                    insert(map, cell.next.start - cell.end - 1, cell);
                    delete_cell.start = 1; delete_cell.end = 0; // the (start == 1 && end == 0) signals that the cell is to be deleted
                }
                else
                {
                    number_intervals--;
                }
                size--;
            }
        }

        map.values().stream().flatMap(Collection::stream).forEach(c -> {
            if ((c.start == 1) && (c.end == 0)) // the (start == 1 && end == 0) signals that the cell is to be deleted
            {
                number_intervals--;
                // delete cell;
            }
        });
        fprintf(stderr,"largest interval gap increased to %d\n", diff);

        // update totals

        LASintervalStartCell start_cell;
        for (LASintervalStartCell c : cells.values()) {
            start_cell = c;
            start_cell.total = 0;
            cell = start_cell;
            while (cell != null)
            {
                start_cell.total += (cell.end - cell.start + 1);
                cell = cell.next;
            }
        }
    }

    void get_cells()
    {
        last_index = I32_MIN;
        current_cell = null;
    }

    boolean has_cells()
    {
        Iterator<Map.Entry<Integer, LASintervalStartCell>> hash_element;
        if (last_index == I32_MIN)
        {
            hash_element = cells.entrySet().iterator();
        }
        else
        {
            hash_element = cells.tailMap(last_index).entrySet().iterator();
        }
        if (!hash_element.hasNext())
        {
            last_index = I32_MIN;
            current_cell = null;
            return FALSE;
        }
        Map.Entry<Integer, LASintervalStartCell> entry = hash_element.next();
        last_index = entry.getKey();
        index = entry.getKey();
        full = entry.getValue().full;
        total = entry.getValue().total;
        current_cell = entry.getValue();
        return TRUE;
    }

    boolean get_cell(int c_index)
    {
        LASintervalStartCell value = cells.get(c_index);
        if (value == null)
        {
            current_cell = null;
            return FALSE;
        }
        index = c_index;
        full = value.full;
        total = value.total;
        current_cell = value;
        return TRUE;
    }

    boolean add_current_cell_to_merge_cell_set()
    {
        if (current_cell == null)
        {
            return FALSE;
        }
        if (cells_to_merge == null)
        {
            cells_to_merge = new HashSet<>();
        }
        cells_to_merge.add(current_cell);
        return TRUE;
    }

    boolean add_cell_to_merge_cell_set(int c_index, boolean erase)
    {
        LASintervalStartCell value = cells.get(c_index);
        if (value == null)
        {
            return FALSE;
        }
        if (cells_to_merge == null)
        {
            cells_to_merge = new HashSet<>();
        }
        cells_to_merge.add(value);
        if (erase) cells.remove(c_index);
        return TRUE;
    }

    boolean merge() {
        return merge(false);
    }

    boolean merge(boolean erase)
    {
        // maybe delete temporary merge cells from the previous merge
        if (merged_cells != null)
        {
            if (merged_cells_temporary)
            {
                LASintervalCell next_next;
                LASintervalCell next = merged_cells.next;
                while (next != null)
                {
                    next_next = next.next;
                    next = next_next;
                }
            }
            merged_cells = null;
        }
        // are there cells to merge
        if (cells_to_merge == null) return FALSE;
        if (cells_to_merge.size() == 0) return FALSE;
        // is there just one cell
        if (cells_to_merge.size() == 1)
        {
            merged_cells_temporary = FALSE;
            // simply use this cell as the merge cell
            merged_cells = (LASintervalStartCell) cells_to_merge.iterator().next();
        }
        else
        {
            merged_cells_temporary = TRUE;
            merged_cells = new LASintervalStartCell();
            // iterate over all cells and add their intervals to map
            LASintervalCell cell;
            SortedMap<Integer, List<LASintervalCell>> map = new TreeMap<>();
            for (LASintervalCell c : cells_to_merge) {
                cell = c;
                merged_cells.full += ((LASintervalStartCell)cell).full;
                while (cell != null)
                {
                    insert(map, cell.start, cell);
                    cell = cell.next;
                }
            }
            // initialize merged_cells with first interval
            List<LASintervalCell> list = map.values().iterator().next();
            cell = list.remove(0);
            if (list.isEmpty()) {
                map.remove(map.firstKey());
            }
            merged_cells.start = cell.start;
            merged_cells.end = cell.end;
            merged_cells.total = cell.end - cell.start + 1;

            // merge intervals
            LASintervalCell last_cell = merged_cells;
            int diff;
            while (!map.isEmpty())
            {
                list = map.values().iterator().next();
                cell = list.remove(0);
                if (list.isEmpty()) {
                    map.remove(map.firstKey());
                }
                diff = cell.start - last_cell.end;
                if (diff > threshold)
                {
                    last_cell.next = new LASintervalCell(cell);
                    last_cell = last_cell.next;
                    merged_cells.total += (cell.end - cell.start + 1);
                }
                else
                {
                    diff = cell.end - last_cell.end;
                    if (diff > 0)
                    {
                        last_cell.end = cell.end;
                        merged_cells.total += diff;
                    }
                    number_intervals--;
                }
            }
        }
        current_cell = merged_cells;
        full = merged_cells.full;
        total = merged_cells.total;
        return TRUE;
    }

    void clear_merge_cell_set()
    {
        if (cells_to_merge != null)
        {
            cells_to_merge.clear();
        }
    }

    boolean get_merged_cell()
    {
        if (merged_cells != null)
        {
            full = merged_cells.full;
            total = merged_cells.total;
            current_cell = merged_cells;
            return TRUE;
        }
        return FALSE;
    }

    boolean has_intervals()
    {
        if (current_cell != null)
        {
            start = current_cell.start;
            end = current_cell.end;
            current_cell = current_cell.next;
            return TRUE;
        }
        return FALSE;
    }

    LASinterval(int threshold)
    {
        cells = new TreeMap<>();
        cells_to_merge = null;
        this.threshold = threshold;
        number_intervals = 0;
        last_index = I32_MIN;
        last_cell = null;
        current_cell = null;
        merged_cells = null;
        merged_cells_temporary = FALSE;
    }

    boolean read(ByteStreamIn stream)
    {
        byte[] signature = new byte[4];
        try { stream.getBytes(signature, 4); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASinterval): reading signature\n");
            return FALSE;
        }
        if (strncmp(stringFromByteArray(signature), "LASV", 4) != 0)
        {
            fprintf(stderr,"ERROR (LASinterval): wrong signature %4s instead of 'LASV'\n", stringFromByteArray(signature));
            return FALSE;
        }
        int version;
        try { version = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASinterval): reading version\n");
            return FALSE;
        }
        // read number of cells
        int number_cells;
        try { number_cells = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASinterval): reading number of cells\n");
            return FALSE;
        }
        // loop over all cells
        while (number_cells != 0)
        {
            // read index of cell
            int cell_index;
            try { cell_index = stream.get32bitsLE(); } catch (Exception e)
            {
                fprintf(stderr,"ERROR (LASinterval): reading cell index\n");
                return FALSE;
            }
            // create cell and insert into hash
            LASintervalStartCell start_cell = new LASintervalStartCell();
            cells.put(cell_index, start_cell);
            LASintervalCell cell = start_cell;
            // read number of intervals in cell
            int number_intervals;
            try { number_intervals = stream.get32bitsLE(); } catch (Exception e)
            {
                fprintf(stderr,"ERROR (LASinterval): reading number of intervals in cell\n");
                return FALSE;
            }
            // read number of points in cell
            int number_points;
            try { number_points = stream.get32bitsLE(); } catch (Exception e)
            {
                fprintf(stderr,"ERROR (LASinterval): reading number of points in cell\n");
                return FALSE;
            }
            start_cell.full = number_points;
            start_cell.total = 0;
            while (number_intervals != 0)
            {
                // read start of interval
                try { cell.start = stream.get32bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR (LASinterval): reading start %d of interval\n", cell.start);
                    return FALSE;
                }
                // read end of interval
                try { cell.end = stream.get32bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR (LASinterval): reading end %d of interval\n", cell.end);
                    return FALSE;
                }
                start_cell.total += (cell.end - cell.start + 1);
                number_intervals--;
                if (number_intervals != 0)
                {
                    cell.next = new LASintervalCell();
                    cell = cell.next;
                }
            }
            number_cells--;
        }

        return TRUE;
    }

    boolean write(ByteStreamOut stream)
    {
        if (!stream.putBytes(asByteArray("LASV"), 4))
        {
            fprintf(stderr,"ERROR (LASinterval): writing signature\n");
            return FALSE;
        }
        int version = 0;
        if (!stream.put32bitsLE(version))
        {
            fprintf(stderr,"ERROR (LASinterval): writing version\n");
            return FALSE;
        }
        // write number of cells
        int number_cells = cells.size();
        if (!stream.put32bitsLE(number_cells))
        {
            fprintf(stderr,"ERROR (LASinterval): writing number of cells %d\n", number_cells);
            return FALSE;
        }
        // loop over all cells
        for (Map.Entry<Integer, LASintervalStartCell> entry : cells.entrySet())
        {
            LASintervalCell cell = entry.getValue();
            // count number of intervals and points in cell
            int number_intervals = 0;
            int number_points = ((LASintervalStartCell)cell).full;
            while (cell != null)
            {
                number_intervals++;
                cell = cell.next;
            }
            // write index of cell
            int cell_index = entry.getKey();
            if (!stream.put32bitsLE(cell_index))
            {
                fprintf(stderr,"ERROR (LASinterval): writing cell index %d\n", cell_index);
                return FALSE;
            }
            // write number of intervals in cell
            if (!stream.put32bitsLE(number_intervals))
            {
                fprintf(stderr,"ERROR (LASinterval): writing number of intervals %d in cell\n", number_intervals);
                return FALSE;
            }
            // write number of points in cell
            if (!stream.put32bitsLE(number_points))
            {
                fprintf(stderr,"ERROR (LASinterval): writing number of points %d in cell\n", number_points);
                return FALSE;
            }
            // write intervals
            cell = entry.getValue();
            while (cell != null)
            {
                // write start of interval
                if (!stream.put32bitsLE(cell.start))
                {
                    fprintf(stderr,"ERROR (LASinterval): writing start %d of interval\n", cell.start);
                    return FALSE;
                }
                // write end of interval
                if (!stream.put32bitsLE(cell.end))
                {
                    fprintf(stderr,"ERROR (LASinterval): writing end %d of interval\n", cell.end);
                    return FALSE;
                }
                cell = cell.next;
            }
        }
        return TRUE;
    }

    private static int size(SortedMap<Integer, List<LASintervalCell>> map) {
        return map.values().stream().flatMapToInt(objects -> IntStream.of(objects.size())).sum();
    }

    private static void insert(SortedMap<Integer, List<LASintervalCell>> map,
                               Integer key, LASintervalCell value) {
        List<LASintervalCell> cells = map.get(key);
        if (cells == null) {
            cells = new ArrayList<>();
            map.put(key, cells);
        }
        cells.add(value);
    }
}
