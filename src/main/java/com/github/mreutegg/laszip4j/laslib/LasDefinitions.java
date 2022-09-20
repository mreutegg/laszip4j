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

public class LasDefinitions {

    public static final int LAS_TOOLS_VERSION = 220310;

    public static final int LAS_TOOLS_FORMAT_DEFAULT = 0;
    public static final int LAS_TOOLS_FORMAT_LAS     = 1;
    public static final int LAS_TOOLS_FORMAT_LAZ     = 2;
    public static final int LAS_TOOLS_FORMAT_BIN     = 3;
    public static final int LAS_TOOLS_FORMAT_QFIT    = 4;
    public static final int LAS_TOOLS_FORMAT_VRML    = 5;
    public static final int LAS_TOOLS_FORMAT_TXT     = 6;
    public static final int LAS_TOOLS_FORMAT_SHP     = 7;
    public static final int LAS_TOOLS_FORMAT_ASC     = 8;
    public static final int LAS_TOOLS_FORMAT_BIL     = 9;
    public static final int LAS_TOOLS_FORMAT_FLT     = 10;
    public static final int LAS_TOOLS_FORMAT_DTM     = 11;

    public static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_GPS_TIME_TYPE = 0;
    public static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_WDP_INTERNAL  = 1;
    public static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_WDP_EXTERNAL  = 2;
    public static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_SYNTHETIC     = 3;
    public static final int LAS_TOOLS_GLOBAL_ENCODING_BIT_OGC_WKT_CRS   = 4;

    public static final int LAS_TOOLS_IO_IBUFFER_SIZE   = 262144;
    public static final int LAS_TOOLS_IO_OBUFFER_SIZE   = 262144;
}
