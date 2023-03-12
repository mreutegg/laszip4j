/*
 * (c) 2007-2022, rapidlasso GmbH - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the Apache Public License 2.0 published by the Apache Software
 * Foundation. See the COPYING file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laszip;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public abstract class LASwriteItemRaw<T extends PointDataRecord> extends LASwriteItem<T> {

    protected ByteStreamOut outstream;

    public boolean init(ByteStreamOut outstream) {
        if (outstream == null) {
            return FALSE;
        }
        this.outstream = outstream;
        return TRUE;
    }
}
