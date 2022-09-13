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
package com.github.mreutegg.laszip4j.laszip;

// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
// Fast arithmetic coding implementation                                     -
// -> 32-bit variables, 32-bit product, periodic updates, table decoding     -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
// Version 1.00  -  April 25, 2004                                           -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
//                                  WARNING                                  -
//                                 =========                                 -
//                                                                           -
// The only purpose of this program is to demonstrate the basic principles   -
// of arithmetic coding. It is provided as is, without any express or        -
// implied warranty, without even the warranty of fitness for any particular -
// purpose, or that the implementations are correct.                         -
//                                                                           -
// Permission to copy and redistribute this code is hereby granted, provided -
// that this warning and copyright notices are not removed or altered.       -
//                                                                           -
// Copyright (c) 2004 by Amir Said (said@ieee.org) &                         -
//                       William A. Pearlman (pearlw@ecse.rpi.edu)           -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
//                                                                           -
// A description of the arithmetic coding method used here is available in   -
//                                                                           -
// Lossless Compression Handbook, ed. K. Sayood                              -
// Chapter 5: Arithmetic Coding (A. Said), pp. 101-152, Academic Press, 2003 -
//                                                                           -
// A. Said, Introduction to Arithetic Coding Theory and Practice             -
// HP Labs report HPL-2004-76  -  http://www.hpl.hp.com/techreports/         -
//                                                                           -
// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

public class ArithmeticModel {

    static final int AC_BUFFER_SIZE = 4096;

    static final int AC__MinLength = 0x01000000;   // threshold for renormalization
    static final int AC__MaxLength = 0xFFFFFFFF;      // maximum AC interval length

    static final int BM__LengthShift = 13;     // length bits discarded before mult.
    static final int BM__MaxCount    = 1 << BM__LengthShift;  // for adaptive models

    static final int DM__LengthShift = 15;     // length bits discarded before mult.
    static final int DM__MaxCount    = 1 << DM__LengthShift;  // for adaptive models

    private final boolean compress;
    int[] u_distribution, u_symbol_count, u_decoder_table;
    int u_total_count, u_update_cycle, u_symbols_until_update;
    int u_symbols, u_last_symbol, u_table_size, u_table_shift;

    public ArithmeticModel(int u_symbols, boolean compress) {
        this.u_symbols = u_symbols;
        this.compress = compress;
    }

    public int init(int[] u_table) {
        if (u_distribution == null)
        {
            if ( (u_symbols < 2) || (u_symbols > (1 << 11)) )
            {
                return -1; // invalid number of symbols
            }
            u_last_symbol = u_symbols - 1;
            if ((!compress) && (u_symbols > 16))
            {
                int table_bits = 3;
                while (u_symbols > (1 << (table_bits + 2))) ++table_bits;
                u_table_size = 1 << table_bits;
                u_table_shift = DM__LengthShift - table_bits;
                u_decoder_table = new int[u_table_size+2];
            }
            else // small alphabet: no table needed
            {
                u_decoder_table = null;
                u_table_size = u_table_shift = 0;
            }
            u_distribution = new int[2*u_symbols];
            u_symbol_count = new int[u_symbols];
        }

        u_total_count = 0;
        u_update_cycle = u_symbols;
        if (u_table != null)
            for (int k = 0; k < u_symbols; k++) u_symbol_count[k] = u_table[k];
        else
            for (int k = 0; k < u_symbols; k++) u_symbol_count[k] = 1;

        update();
        u_symbols_until_update = u_update_cycle = (u_symbols + 6) >>> 1;

        return 0;
    }

    void update() {
        // halve counts when a threshold is reached
        if ((u_total_count += u_update_cycle) > DM__MaxCount)
        {
            u_total_count = 0;
            for (int n = 0; n < u_symbols; n++)
            {
                u_total_count += (u_symbol_count[n] = (u_symbol_count[n] + 1) >>> 1);
            }
        }

        // compute cumulative distribution, decoder table
        int k, sum = 0, s = 0;
        int scale = Integer.divideUnsigned(0x80000000, u_total_count);

        if (compress || (u_table_size == 0))
        {
            for (k = 0; k < u_symbols; k++)
            {
                u_distribution[k] = (scale * sum) >>> (31 - DM__LengthShift);
                sum += u_symbol_count[k];
            }
        }
        else
        {
            for (k = 0; k < u_symbols; k++)
            {
                u_distribution[k] = (scale * sum) >>> (31 - DM__LengthShift);
                sum += u_symbol_count[k];
                int w = u_distribution[k] >>> u_table_shift;
                while (s < w) u_decoder_table[++s] = k - 1;
            }
            u_decoder_table[0] = 0;
            while (s <= u_table_size) u_decoder_table[++s] = u_symbols - 1;
        }

        // set frequency of model updates
        u_update_cycle = (5 * u_update_cycle) >>> 2;
        int max_cycle = (u_symbols + 6) << 3;
        if (Integer.compareUnsigned(u_update_cycle, max_cycle) > 0) u_update_cycle = max_cycle;
        u_symbols_until_update = u_update_cycle;
    }
}
