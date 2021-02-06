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

import com.github.mreutegg.laszip4j.laslib.LASevlr;
import com.github.mreutegg.laszip4j.laslib.LASreadOpener;
import com.github.mreutegg.laszip4j.laslib.LASreader;

import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strlen;
import static com.github.mreutegg.laszip4j.clib.Cstring.strncmp;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.asByteArray;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASindex {

    private static final PrintStream stderr = System.err;

    public int start; // unsigned
    public int end; // unsigned
    public int full; // unsigned
    public int total; // unsigned
    public int cells; // unsigned

    private LASquadtree spatial;
    private LASinterval interval;
    private boolean have_interval;

    private static class my_cell_hash extends HashMap<Integer, Integer> {}

    public LASindex()
    {
        spatial = null;
        interval = null;
        have_interval = FALSE;
        start = 0;
        end = 0;
        full = 0;
        total = 0;
        cells = 0;
    }

    public void prepare(LASquadtree spatial, int threshold)
    {
        this.spatial = spatial;
        this.interval = new LASinterval(threshold);
    }

    public boolean add(double x, double y, int p_index)
    {
        int cell = spatial.get_cell_index(x, y);
        return interval.add(p_index, cell);
    }

    public void complete(int u_minimum_points, int maximum_intervals) {
        complete(u_minimum_points, maximum_intervals, true);
    }

    void complete(int u_minimum_points, int maximum_intervals, boolean verbose)
    {
        if (verbose)
        {
            fprintf(stderr,"before complete %d %d\n", u_minimum_points, maximum_intervals);
            print(FALSE);
        }
        if (u_minimum_points != 0)
        {
            int hash1 = 0;
            my_cell_hash[] cell_hash = new my_cell_hash[2];
            for (int i = 0; i < cell_hash.length; i++) {
                cell_hash[i] = new my_cell_hash();
            }
            // insert all cells into hash1
            interval.get_cells();
            while (interval.has_cells())
            {
                cell_hash[hash1].put(interval.index, interval.full);
            }
            while (cell_hash[hash1].size() != 0)
            {
                int hash2 = (hash1+1)%2;
                cell_hash[hash2].clear();
                // coarsen if a coarser cell will still have fewer than minimum_points (and points in all subcells)
                boolean coarsened = FALSE;
                int i, full; // unsigned
                int[] coarser_index = new int[1];
                int[] num_indices = new int[1]; // unsigned
                int num_filled; // unsigned
                int[][] indices = new int[1][];
                Iterator<Map.Entry<Integer, Integer>> it_inner;
                Iterator<Map.Entry<Integer, Integer>> it_outer = cell_hash[hash1].entrySet().iterator();
                while (it_outer.hasNext())
                {
                    Map.Entry<Integer, Integer> hash_element_outer = it_outer.next();
                    if (hash_element_outer.getValue() != 0)
                    {
                        // TODO: double check passed arrays are correctly used
                        if (spatial.coarsen(hash_element_outer.getKey(), coarser_index, num_indices, indices))
                        {
                            full = 0;
                            num_filled = 0;
                            for (i = 0; i < num_indices[0]; i++)
                            {
                                Integer key;
                                Integer value;
                                if (hash_element_outer.getKey() == indices[0][i])
                                {
                                    key = hash_element_outer.getKey();
                                    value = hash_element_outer.getValue();
                                }
                                else
                                {
                                    key = indices[0][i];
                                    value = cell_hash[hash1].get(indices[0][i]);
                                }
                                if (value != null)
                                {
                                    full += value;
                                    cell_hash[hash1].put(key, 0);
                                    num_filled++;
                                }
                            }
                            if ((Integer.compareUnsigned(full, u_minimum_points) < 0) && (num_filled == num_indices[0]))
                            {
                                interval.merge_cells(num_indices[0], indices[0], coarser_index[0]);
                                coarsened = TRUE;
                                cell_hash[hash2].put(coarser_index[0], full);
                            }
                        }
                    }
                }
                if (!coarsened) break;
                hash1 = (hash1+1)%2;
            }
            // tell spatial about the existing cells
            interval.get_cells();
            while (interval.has_cells())
            {
                spatial.manage_cell(interval.index);
            }
            if (verbose)
            {
                fprintf(stderr,"after minimum_points %d\n", u_minimum_points);
                print(FALSE);
            }
        }
        if (maximum_intervals < 0)
        {
            maximum_intervals = -maximum_intervals*interval.get_number_cells();
        }
        if (maximum_intervals != 0)
        {
            interval.merge_intervals(maximum_intervals, verbose);
            if (verbose)
            {
                fprintf(stderr,"after maximum_intervals %d\n", maximum_intervals);
                print(FALSE);
            }
        }
    }

    void print(boolean verbose)
    {
        int total_cells = 0; // unsigned
        int total_full = 0; // unsigned
        int total_total = 0; // unsigned
        int total_intervals = 0; // unsigned
        int total_check; // unsigned
        int intervals; // unsigned
        interval.get_cells();
        while (interval.has_cells())
        {
            total_check = 0;
            intervals = 0;
            while (interval.has_intervals())
            {
                total_check += interval.end-interval.start+1;
                intervals++;
            }
            if (total_check != interval.total)
            {
                fprintf(stderr,"ERROR: total_check %d != interval.total %d\n", total_check, interval.total);
            }
            if (verbose) fprintf(stderr,"cell %d intervals %d full %d total %d (%.2f)\n", interval.index, intervals, interval.full, interval.total, 100.0f*interval.full/interval.total);
            total_cells++;
            total_full += interval.full;
            total_total += interval.total;
            total_intervals += intervals;
        }
        if (verbose) fprintf(stderr,"total cells/intervals %d/%d full %d (%.2f)\n", total_cells, total_intervals, total_full, 100.0f*total_full/total_total);
    }

    LASquadtree get_spatial() 
    {
        return spatial;
    }

    LASinterval get_interval() 
    {
        return interval;
    }

    public boolean intersect_rectangle(double r_min_x, double r_min_y, double r_max_x, double r_max_y)
    {
        have_interval = FALSE;
        cells = spatial.intersect_rectangle(r_min_x, r_min_y, r_max_x, r_max_y);
        //  fprintf(stderr,"%d cells of %g/%g %g/%g intersect rect %g/%g %g/%g\n", num_cells, spatial.get_min_x(), spatial.get_min_y(), spatial.get_max_x(), spatial.get_max_y(), r_min_x, r_min_y, r_max_x, r_max_y);
        if (cells != 0)
            return merge_intervals();
        return FALSE;
    }

    public boolean intersect_tile(float ll_x, float ll_y, float size)
    {
        have_interval = FALSE;
        cells = spatial.intersect_tile(ll_x, ll_y, size);
        //  fprintf(stderr,"%d cells of %g/%g %g/%g intersect tile %g/%g/%g\n", num_cells, spatial.get_min_x(), spatial.get_min_y(), spatial.get_max_x(), spatial.get_max_y(), ll_x, ll_y, size);
        if (cells != 0)
            return merge_intervals();
        return FALSE;
    }

    public boolean intersect_circle(double center_x, double center_y, double radius)
    {
        have_interval = FALSE;
        cells = spatial.intersect_circle(center_x, center_y, radius);
        //  fprintf(stderr,"%d cells of %g/%g %g/%g intersect circle %g/%g/%g\n", num_cells, spatial.get_min_x(), spatial.get_min_y(), spatial.get_max_x(), spatial.get_max_y(), center_x, center_y, radius);
        if (cells != 0)
            return merge_intervals();
        return FALSE;
    }

    boolean get_intervals()
    {
        have_interval = FALSE;
        return interval.get_merged_cell();
    }

    boolean has_intervals()
    {
        if (interval.has_intervals())
        {
            start = interval.start;
            end = interval.end;
            full = interval.full;
            have_interval = TRUE;
            return TRUE;
        }
        have_interval = FALSE;
        return FALSE;
    }

    public boolean read(String file_name)
    {
        if (file_name == null) return FALSE;
        char[] name = file_name.toCharArray();
        if (strstr(file_name, ".las") || strstr(file_name, ".laz"))
        {
            name[strlen(name)-1] = 'x';
        }
        else if (strstr(file_name, ".LAS") || strstr(file_name, ".LAZ"))
        {
            name[strlen(name)-1] = 'X';
        }
        else
        {
            name[strlen(name)-3] = 'l';
            name[strlen(name)-2] = 'a';
            name[strlen(name)-1] = 'x';
        }
        RandomAccessFile file = fopenRAF(name, "rb");
        if (file == null)
        {
            return FALSE;
        }
        ByteStreamIn stream = new ByteStreamInFile(file);
        if (!read(stream))
        {
            fprintf(stderr,"ERROR (LASindex): cannot read '%s'\n", new String(name));
            fclose(file);
            return FALSE;
        }
        fclose(file);
        return TRUE;
    }

    public boolean append(String file_name)
    {
        LASreadOpener lasreadopener = new LASreadOpener();

        if (file_name == null) return FALSE;

        // open reader

        LASreader lasreader = lasreadopener.open(file_name);
        if (lasreader == null) return FALSE;
        if (lasreader.header.laszip == null) return FALSE;

        // close reader

        lasreader.close();

        RandomAccessFile file = fopenRAF(file_name.toCharArray(), "rb");
        ByteStreamIn bytestreamin = new ByteStreamInFile(file);

        // maybe write LASindex EVLR start position into LASzip VLR

        long offset_laz_vlr = -1;

        // where to write LASindex EVLR that will contain the LAX file

        long number_of_special_evlrs = lasreader.header.laszip.number_of_special_evlrs;
        long offset_to_special_evlrs = lasreader.header.laszip.offset_to_special_evlrs;

        if ((number_of_special_evlrs == -1) && (offset_to_special_evlrs == -1))
        {
            bytestreamin.seekEnd();
            number_of_special_evlrs = 1;
            offset_to_special_evlrs = bytestreamin.tell();

            // find LASzip VLR

            long total = lasreader.header.header_size + 2L;
            int number_of_variable_length_records = lasreader.header.number_of_variable_length_records + 1 + asInt(lasreader.header.vlr_lastiling != null) + asInt(lasreader.header.vlr_lasoriginal != null);

            for (int u = 0; u < number_of_variable_length_records; u++)
            {
                bytestreamin.seek(total);

                byte[] user_id = new byte[16];
                try { bytestreamin.getBytes(user_id, 16); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].user_id\n", u);
                    return FALSE;
                }
                if (stringFromByteArray(user_id).startsWith("laszip encoded"))
                {
                    offset_laz_vlr = bytestreamin.tell() - 18;
                    break;
                }
                char record_id;
                try { record_id = bytestreamin.get16bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].record_id\n", u);
                    return FALSE;
                }
                char record_length_after_header;
                try { record_length_after_header = bytestreamin.get16bitsLE(); } catch (Exception e)
                {
                    fprintf(stderr,"ERROR: reading header.vlrs[%d].record_length_after_header\n", u);
                    return FALSE;
                }
                total += (54 + record_length_after_header);
            }

            if (number_of_special_evlrs == -1) return FALSE;
        }

        fclose(file);

        ByteStreamOut bytestreamout;
        file = fopenRAF(file_name.toCharArray(), "rb+");
        bytestreamout = new ByteStreamOutFile(file);
        bytestreamout.seek(offset_to_special_evlrs);

        LASevlr lax_evlr = new LASevlr();
        sprintf(lax_evlr.user_id, "LAStools");
        lax_evlr.record_id = 30;
        sprintf(lax_evlr.description, "LAX spatial indexing (LASindex)");

        bytestreamout.put16bitsLE(lax_evlr.reserved);
        bytestreamout.putBytes(lax_evlr.user_id, 16);
        bytestreamout.put16bitsLE(lax_evlr.record_id);
        bytestreamout.put64bitsLE(lax_evlr.record_length_after_header);
        bytestreamout.putBytes(lax_evlr.description, 32);

        if (!write(bytestreamout))
        {
            fprintf(stderr,"ERROR (LASindex): cannot append LAX to '%s'\n", file_name);
            fclose(file);
            return FALSE;
        }

        // update LASindex EVLR

        lax_evlr.record_length_after_header = bytestreamout.tell() - offset_to_special_evlrs - 60;
        bytestreamout.seek(offset_to_special_evlrs + 20);
        bytestreamout.put64bitsLE(lax_evlr.record_length_after_header);

        // maybe update LASzip VLR

        if (number_of_special_evlrs != -1)
        {
            bytestreamout.seek(offset_laz_vlr + 54 + 16);
            bytestreamout.put64bitsLE(number_of_special_evlrs);
            bytestreamout.put64bitsLE(offset_to_special_evlrs);
        }

        // close writer

        bytestreamout.seekEnd();
        fclose(file);

        return TRUE;
    }

    public boolean write(String file_name)
    {
        if (file_name == null) return FALSE;
        char[] name = file_name.toCharArray();
        if (strstr(file_name, ".las") || strstr(file_name, ".laz"))
        {
            name[strlen(name)-1] = 'x';
        }
        else if (strstr(file_name, ".LAS") || strstr(file_name, ".LAZ"))
        {
            name[strlen(name)-1] = 'X';
        }
        else
        {
            name[strlen(name)-3] = 'l';
            name[strlen(name)-2] = 'a';
            name[strlen(name)-1] = 'x';
        }
        RandomAccessFile file = fopenRAF(name, "wb");
        if (file == null)
        {
            fprintf(stderr,"ERROR (LASindex): cannot open '%s' for write\n", new String(name));
            return FALSE;
        }
        ByteStreamOut stream = new ByteStreamOutFile(file);
        if (!write(stream))
        {
            fprintf(stderr,"ERROR (LASindex): cannot write '%s'\n", new String(name));
            fclose(file);
            return FALSE;
        }
        fclose(file);
        return TRUE;
    }

    public boolean read(ByteStreamIn stream)
    {
        if (spatial != null)
        {
            spatial = null;
        }
        if (interval != null)
        {
            interval = null;
        }
        byte[] signature = new byte[4];
        try { stream.getBytes(signature, 4); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASindex): reading signature\n");
            return FALSE;
        }
        if (strncmp(stringFromByteArray(signature), "LASX", 4) != 0)
        {
            fprintf(stderr,"ERROR (LASindex): wrong signature %4s instead of 'LASX'\n", stringFromByteArray(signature));
            return FALSE;
        }
        int version; // unsigned
        try { version = stream.get32bitsLE(); } catch (Exception e)
        {
            fprintf(stderr,"ERROR (LASindex): reading version\n");
            return FALSE;
        }
        // read spatial quadtree
        spatial = new LASquadtree();
        if (!spatial.read(stream))
        {
            fprintf(stderr,"ERROR (LASindex): cannot read LASspatial (LASquadtree)\n");
            return FALSE;
        }
        // read interval
        interval = new LASinterval();
        if (!interval.read(stream))
        {
            fprintf(stderr,"ERROR (LASindex): reading LASinterval\n");
            return FALSE;
        }
        // tell spatial about the existing cells
        interval.get_cells();
        while (interval.has_cells())
        {
            spatial.manage_cell(interval.index);
        }
        return TRUE;
    }

    boolean write(ByteStreamOut stream)
    {
        if (!stream.putBytes(asByteArray("LASX"), 4))
        {
            fprintf(stderr,"ERROR (LASindex): writing signature\n");
            return FALSE;
        }
        int version = 0; // unsigned
        if (!stream.put32bitsLE(version))
        {
            fprintf(stderr,"ERROR (LASindex): writing version\n");
            return FALSE;
        }
        // write spatial quadtree
        if (!spatial.write(stream))
        {
            fprintf(stderr,"ERROR (LASindex): cannot write LASspatial (LASquadtree)\n");
            return FALSE;
        }
        // write interval
        if (!interval.write(stream))
        {
            fprintf(stderr,"ERROR (LASindex): writing LASinterval\n");
            return FALSE;
        }
        return TRUE;
    }

    // seek to next interval point

    public boolean seek_next(LASreader lasreader)
    {
        if (!have_interval)
        {
            if (!has_intervals()) return FALSE;
            lasreader.seek(start);
        }
        if (lasreader.p_count == end)
        {
            have_interval = FALSE;
        }
        return TRUE;
    }

    // merge the intervals of non-empty cells
    boolean merge_intervals()
    {
        if (spatial.get_intersected_cells())
        {
            int used_cells = 0;
            while (spatial.has_more_cells())
            {
                if (interval.get_cell(spatial.current_cell))
                {
                    interval.add_current_cell_to_merge_cell_set();
                    used_cells++;
                }
            }
            //    fprintf(stderr,"LASindex: used %d cells of total %d\n", used_cells, interval.get_number_cells());
            if (used_cells != 0)
            {
                boolean r = interval.merge();
                full = interval.full;
                total = interval.total;
                interval.clear_merge_cell_set();
                return r;
            }
        }
        return FALSE;
    }

    private static boolean strstr(String s1, String s2) {
        return s1.contains(s2);
    }

    private static int asInt(boolean b) {
        return b ? 1 : 0;
    }
}
