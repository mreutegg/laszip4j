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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class LASvlr_key_entry {
    public char key_id;
    public char tiff_tag_location;
    public char count;
    public char value_offset;

    public LASvlr_key_entry() {
    }

    public LASvlr_key_entry(LASvlr_key_entry other) {
        this.key_id = other.key_id;
        this.tiff_tag_location = other.tiff_tag_location;
        this.count = other.count;
        this.value_offset = other.value_offset;
    }

    public void writeTo(ByteBuffer buffer) {
        buffer.putChar(key_id);
        buffer.putChar(tiff_tag_location);
        buffer.putChar(count);
        buffer.putChar(value_offset);
    }

    public static LASvlr_key_entry[] fromByteBuffer(ByteBuffer buffer) {
        List<LASvlr_key_entry> entries = new ArrayList<>();
        while (buffer.hasRemaining()) {
            LASvlr_key_entry entry = new LASvlr_key_entry();
            entry.key_id = buffer.getChar();
            entry.tiff_tag_location = buffer.getChar();
            entry.count = buffer.getChar();
            entry.value_offset = buffer.getChar();
            entries.add(entry);
        }
        return entries.toArray(new LASvlr_key_entry[entries.size()]);
    }
}
