/*
 * Copyright 2007-2013, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public abstract class LASreadItemRaw extends LASreadItem {

    protected ByteStreamIn instream;

    public boolean init(ByteStreamIn instream)
    {
        if (instream == null) return FALSE;
        this.instream = instream;
        return TRUE;
    };
}
