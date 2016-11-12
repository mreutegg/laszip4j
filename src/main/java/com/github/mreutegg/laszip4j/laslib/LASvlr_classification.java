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

public class LASvlr_classification {

    public byte class_number;
    byte[] description = new byte[15];

    public static LASvlr_classification fromByteArray(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        LASvlr_classification c = new LASvlr_classification();
        c.class_number = bb.get();
        bb.get(c.description);
        return c;
    }

}
