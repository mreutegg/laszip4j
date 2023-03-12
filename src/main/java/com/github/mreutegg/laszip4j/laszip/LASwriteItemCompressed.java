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

public abstract class LASwriteItemCompressed extends LASwriteItem<PointDataRecord> {

    public abstract boolean init(PointDataRecord point, int context);

    public boolean chunk_sizes() {
        return FALSE;
    }

    public boolean chunk_bytes() {
        return FALSE;
    }
}
