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

// TODO: only dummy implementation
public class LASwriteItemCompressed_BYTE14_v3 extends LASwriteItemCompressed {
    public LASwriteItemCompressed_BYTE14_v3(ArithmeticEncoder enc, char size) {
    }

    @Override
    public boolean write(PointDataRecord point, int context) {
        return false;
    }

    @Override
    public boolean init(PointDataRecord point, int context) {
        return false;
    }
}
