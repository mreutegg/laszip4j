/*
 * Copyright 2017 Marcel Reutegger
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.mreutegg.laszip4j;

import com.github.mreutegg.laszip4j.laslib.LASvlr;

import java.nio.ByteBuffer;

import static com.github.mreutegg.laszip4j.laszip.MyDefs.stringFromByteArray;

/**
 * A LAS variable length header record.
 */
public class LASVariableLengthRecord {

    private final LASvlr vlr;

    LASVariableLengthRecord(LASvlr vlr) {
        this.vlr = vlr;
    }

    /**
     * @return "Reserved" as an unsigned short (char).
     */
    public char getReserved() {
        return vlr.reserved;
    }

    /**
     * @return "User ID" as a String.
     */
    public String getUserID() {
        return stringFromByteArray(vlr.user_id);
    }

    /**
     * @return "Record ID" as an unsigned short (char).
     */
    public char getRecordID() {
        return vlr.record_id;
    }

    /**
     * @return "Record Length After Header" as an unsigned short (char).
     */
    public char getRecordLength() {
        return vlr.record_length_after_header;
    }

    /**
     * @return "Description" as a String.
     */
    public String getDescription() {
        return stringFromByteArray(vlr.description);
    }

    /**
     * @return the data of the record as a read-only ByteBuffer.
     */
    public ByteBuffer getData() {
        return ByteBuffer.wrap(vlr.data).asReadOnlyBuffer();
    }

    /**
     * @return the data of the record as a String.
     */
    public String getDataAsString() {
        return stringFromByteArray(vlr.data);
    }
}
