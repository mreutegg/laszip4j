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

public class ArithmeticBitModel {

    private static final int BM__LengthShift = 13;     // length bits discarded before mult.
    private static final int BM__MaxCount    = 1 << BM__LengthShift;  // for adaptive models

    int u_update_cycle, u_bits_until_update;
    int u_bit_0_prob, u_bit_0_count, u_bit_count;

    public ArithmeticBitModel() {
        init();
    }

    void init() {
        // initialization to equiprobable model
        u_bit_0_count = 1;
        u_bit_count = 2;
        u_bit_0_prob = 1 << (BM__LengthShift - 1);
        // start with frequent updates
        u_update_cycle = u_bits_until_update = 4;
    }

    void update() {
        // halve counts when a threshold is reached
        if ((u_bit_count += u_update_cycle) > BM__MaxCount)
        {
            u_bit_count = (u_bit_count + 1) >>> 1;
            u_bit_0_count = (u_bit_0_count + 1) >>> 1;
            if (u_bit_0_count == u_bit_count) ++u_bit_count;
        }

        // compute scaled bit 0 probability
        int scale = Integer.divideUnsigned(0x80000000, u_bit_count);
        u_bit_0_prob = (u_bit_0_count * scale) >>> (31 - BM__LengthShift);

        // set frequency of model updates
        u_update_cycle = (5 * u_update_cycle) >>> 2;
        if (u_update_cycle > 64) u_update_cycle = 64;
        u_bits_until_update = u_update_cycle;
    }
}
