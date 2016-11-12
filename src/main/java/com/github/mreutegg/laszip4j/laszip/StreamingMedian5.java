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
package com.github.mreutegg.laszip4j.laszip;

public class StreamingMedian5 {

    public static StreamingMedian5[] newStreamingMedian5(int num) {
        StreamingMedian5[] medians = new StreamingMedian5[num];
        for (int i = 0; i < num; i++) {
            medians[i] = new StreamingMedian5();
        }
        return medians;
    }

    public int[] values = new int[5];
    public boolean high;

    public void init()
    {
        values[0] = values[1] = values[2] = values[3] = values[4] = 0;
        high = true;
    }

    public void add(int v)
    {
        if (high)
        {
            if (v < values[2])
            {
                values[4] = values[3];
                values[3] = values[2];
                if (v < values[0])
                {
                    values[2] = values[1];
                    values[1] = values[0];
                    values[0] = v;
                }
                else if (v < values[1])
                {
                    values[2] = values[1];
                    values[1] = v;
                }
                else
                {
                    values[2] = v;
                }
            }
            else
            {
                if (v < values[3])
                {
                    values[4] = values[3];
                    values[3] = v;
                }
                else
                {
                    values[4] = v;
                }
                high = false;
            }
        }
        else
        {
            if (values[2] < v)
            {
                values[0] = values[1];
                values[1] = values[2];
                if (values[4] < v)
                {
                    values[2] = values[3];
                    values[3] = values[4];
                    values[4] = v;
                }
                else if (values[3] < v)
                {
                    values[2] = values[3];
                    values[3] = v;
                }
                else
                {
                    values[2] = v;
                }
            }
            else
            {
                if (values[1] < v)
                {
                    values[0] = values[1];
                    values[1] = v;
                }
                else
                {
                    values[0] = v;
                }
                high = true;
            }
        }
    }

    public int get()
    {
        return values[2];
    }

    StreamingMedian5()
    {
        init();
    }
}
