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
import java.nio.ByteOrder;

public class LASvlr_geo_keys {

    public char key_directory_version;
    public char key_revision;
    public char minor_revision;
    public char number_of_keys;

    public void writeTo(ByteBuffer buffer) {
        buffer.putChar(key_directory_version);
        buffer.putChar(key_revision);
        buffer.putChar(minor_revision);
        buffer.putChar(number_of_keys);
    }

    public static LASvlr_geo_keys fromByteArray(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        LASvlr_geo_keys gk = new LASvlr_geo_keys();
        gk.key_directory_version = bb.getChar();
        gk.key_revision = bb.getChar();
        gk.minor_revision = bb.getChar();
        gk.number_of_keys = bb.getChar();
        return gk;
    }
}
