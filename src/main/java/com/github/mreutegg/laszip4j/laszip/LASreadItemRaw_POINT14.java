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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.I16_QUANTIZE;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.I8_CLAMP;

public class LASreadItemRaw_POINT14 extends LASreadItemRaw {

    private ByteBuffer bb = ByteBuffer.allocate(30).order(ByteOrder.LITTLE_ENDIAN);

    @Override
    public void read(byte[] item) {
        bb.clear();
        instream.getBytes(bb.array(), 30);
        // write native big endian into item
        ByteBuffer itemBB = ByteBuffer.wrap(item);
        itemBB.putInt(bb.getInt());     // x
        itemBB.putInt(bb.getInt());     // y
        itemBB.putInt(bb.getInt());     // z
        itemBB.putChar(bb.getChar());   // intensity
        byte b = bb.get();              // return_number, number_of_returns
        byte return_number = (byte) (b & 0b1111);
        byte number_of_returns = (byte) ((b >>> 4) & 0b1111);
        byte point10_return_number;
        byte point10_number_of_returns;
        if (number_of_returns > 7) {
            if (return_number > 6) {
                if (return_number >= number_of_returns) {
                    point10_return_number = 7;
                } else {
                    point10_return_number = 6;
                }
            } else {
                point10_return_number = return_number;
            }
            point10_number_of_returns = 7;
        } else {
            point10_return_number = return_number;
            point10_number_of_returns = number_of_returns;
        }
        b = bb.get();                   // classification_flags, scanner_channel, scan_direction_flag, edge_of_flight_line
        byte classification_flags = (byte) (b & 0b1111);
        byte scanner_channel = (byte) ((b >>> 4) & 0b0011);
        byte scan_direction_flag = (byte) ((b >>> 6) & 0b0001);
        byte edge_of_flight_line = (byte) ((b >>> 7) & 0b0001);

        b = 0;
        b |= (point10_return_number & 0b0111);
        b |= ((point10_number_of_returns << 3) & 0b00111000);
        b |= ((scan_direction_flag << 6) & 0b01000000);
        b |= ((edge_of_flight_line << 7) & 0b10000000);
        itemBB.put(b);

        byte classification = bb.get(); // classification (unsigned byte)
        b = 0;
        b |= ((classification_flags << 5) & 0b11100000);
        if (Byte.toUnsignedInt(classification) < 32) {
            b |= classification;
        }
        itemBB.put(b);

        byte user_data = bb.get();
        short scan_angle = bb.getShort();
        char point_source_ID = bb.getChar();

        // scan_angle_rank
        itemBB.put((byte) I8_CLAMP(I16_QUANTIZE(0.006f*scan_angle)));

        // user_data
        itemBB.put(user_data);

        // point_source_ID
        itemBB.putChar(point_source_ID);

        // extended_scan_angle
        itemBB.putShort(scan_angle);

        b = 0;
        b |= ((scanner_channel << 2) & 0b1100);
        b |= ((classification_flags << 4) & 0b11110000);
        // extended_point_type, extended_scanner_channel, extended_classification_flags
        itemBB.put(b);

        // extended_classification
        itemBB.put(classification);

        b = 0;
        b |= (return_number & 0b1111);
        b |= ((number_of_returns << 4) & 0b1110000);
        // extended_return_number, extended_number_of_returns
        itemBB.put(b);

        // gps_time
        itemBB.putLong(32, bb.getLong());
    }
}
