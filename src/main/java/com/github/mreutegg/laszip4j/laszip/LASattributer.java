/*
 * Copyright 2005-2015, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.mreutegg.laszip4j.clib.Cstring.strcmp;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

// extends LASquantizer because Java does not have multiple inheritance
public class LASattributer extends LASquantizer {

    public int number_attributes;
    public List<LASattribute> attributes = new ArrayList<LASattribute>();
    public List<Integer> attribute_starts = new ArrayList<Integer>();
    public List<Integer> attribute_sizes = new ArrayList<Integer>();

    public LASattributer() {
    };

    protected void clean_attributes()
    {
        if (number_attributes != 0)
        {
            number_attributes = 0;
            attributes.clear();
            attribute_starts.clear();
            attribute_sizes.clear();
        }
    };

    public boolean init_attributes(int number_attributes, LASattribute[] attributes)
    {
        int i;
        clean_attributes();
        this.number_attributes = number_attributes;
        this.attributes = new ArrayList<LASattribute>(Arrays.asList(attributes));
        attribute_starts = new ArrayList<Integer>();
        attribute_sizes = new ArrayList<Integer>();
        attribute_starts.add(0);
        attribute_sizes.add(attributes[0].get_size());
        for (i = 1; i < number_attributes; i++)
        {
            attribute_starts.add(attribute_starts.get(i-1) + attribute_sizes.get(i-1));
            attribute_sizes.add(attributes[i].get_size());
        }
        return TRUE;
    };

    public int add_attribute(LASattribute attribute)
    {
        if (attribute.get_size() != 0)
        {
            number_attributes++;
            attributes.add(attribute);
            int priorStart = attribute_starts.size() > 1 ? attribute_starts.get(attribute_starts.size()-2) : 0;
            int priorSize = attribute_sizes.size() > 1 ? attribute_sizes.get(attribute_sizes.size()-2) : 0;
            attribute_starts.add(priorStart + priorSize);
            attribute_sizes.add(attributes.get(number_attributes-1).get_size());
            return number_attributes-1;
        }
        return -1;
    };

    short get_attributes_size()
    {
        return (short) (!attributes.isEmpty() ? attribute_starts.get(number_attributes-1) + attribute_sizes.get(number_attributes-1) : 0);
    }

    public int get_attribute_index(String name)
    {
        int i;
        for (i = 0; i < number_attributes; i++)
        {
            if (strcmp(attributes.get(i).name, name) == 0)
            {
                return i;
            }
        }
        return -1;
    }

    int get_attribute_start(String name)
    {
        int i;
        for (i = 0; i < number_attributes; i++)
        {
            if (strcmp(attributes.get(i).name, name) == 0)
            {
                return attribute_starts.get(i);
            }
        }
        return -1;
    }

    public int get_attribute_start(int index)
    {
        if (index < number_attributes)
        {
            return attribute_starts.get(index);
        }
        return -1;
    }

    int get_attribute_size(int index)
    {
        if (index < number_attributes)
        {
            return attribute_sizes.get(index);
        }
        return -1;
    }

    public boolean remove_attribute(int index)
    {
        if (index < 0 || index >= number_attributes)
        {
            return FALSE;
        }
        for (index = index + 1; index < number_attributes; index++)
        {
            attributes.set(index-1, attributes.get(index));
            if (index > 1)
            {
                attribute_starts.set(index-1, attribute_starts.get(index-2) + attribute_sizes.get(index-2));
            }
            else
            {
                attribute_starts.set(index-1, 0);
            }
            attribute_sizes.set(index-1, attribute_sizes.get(index));
        }
        number_attributes--;
        return TRUE;
    }

    boolean remove_attribute(String name)
    {
        int index = get_attribute_index(name);
        if (index != -1)
        {
            return remove_attribute(index);
        }
        return FALSE;
    }
}
