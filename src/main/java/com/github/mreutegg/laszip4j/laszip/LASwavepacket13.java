/*
 * Copyright 2007-2014, martin isenburg, rapidlasso - fast tools to catch reality
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

public class LASwavepacket13 {

    private U32I32F32 return_point = new U32I32F32();
    private U32I32F32 x = new U32I32F32();
    private U32I32F32 y = new U32I32F32();
    private U32I32F32 z = new U32I32F32();

    /**
     * U64 offset;                      0
     * U32 packet_size;                 8
     * U32I32F32 return_point;         12
     * U32I32F32 x;                    16
     * U32I32F32 y;                    20
     * U32I32F32 z;                    24
     */
    private final ByteBuffer data = ByteBuffer.allocate(28);

    public LASwavepacket13() {
        this.return_point = U32I32F32.wrap(slice(12, 4));
        this.x = U32I32F32.wrap(slice(16, 4));
        this.y = U32I32F32.wrap(slice(20, 4));
        this.z = U32I32F32.wrap(slice(24, 4));
    }

    public ByteBuffer getData() {
        return data;
    }

    private ByteBuffer slice(int offset, int size) {
        return ((ByteBuffer) data.duplicate().position(offset).limit(offset + size)).slice();
    }

    static LASwavepacket13 unpack(byte[] itemArray)
    {
        // unpack a LAS wavepacket out of raw memory
        LASwavepacket13 r = new LASwavepacket13();

        ByteBuffer item = ByteBuffer.wrap(itemArray);
        r.setOffset(makeU64(item));
        r.setPacket_size(makeU32(item));
        r.getReturn_point().setU32(makeU32(item));

        r.getX().setU32(makeU32(item));
        r.getY().setU32(makeU32(item));
        r.getZ().setU32(makeU32(item));

        return r;
    }

    void pack(ByteBuffer item)
    {
        // pack a LAS wavepacket into raw memory
        packU32((int)(getOffset() & 0xFFFFFFFF), item);
        packU32((int)(getOffset() >>> 32), item);

        packU32(getPacket_size(), item);
        packU32(getReturn_point().getU32(), item);
        packU32(getX().getU32(), item);
        packU32(getY().getU32(), item);
        packU32(getZ().getU32(), item);
    }

    private static long makeU64(ByteBuffer item)
    {
        long dw0 = (long)makeU32(item);
        long dw1 = (long)makeU32(item);

        return dw0 | (dw1 << 32);
    }

    private static int makeU32(ByteBuffer item)
    {
        byte b0 = item.get();
        byte b1 = item.get();
        byte b2 = item.get();
        byte b3 = item.get();

        return b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
    }

    private static void packU32(int v, ByteBuffer item)
    {
        item.put((byte) (v & 0xFF));
        item.put((byte) ((v >>> 8) & 0xFF));
        item.put((byte) ((v >>> 16) & 0xFF));
        item.put((byte) ((v >>> 24) & 0xFF));
    }

    public long getOffset() {
        return data.getLong(0);
    }

    public void setOffset(long offset) {
        this.data.putLong(0, offset);
    }

    public int getPacket_size() {
        return data.getInt(8);
    }

    public void setPacket_size(int packet_size) {
        this.data.putInt(8, packet_size);
    }

    public U32I32F32 getReturn_point() {
        return return_point;
    }

    public U32I32F32 getX() {
        return x;
    }

    public U32I32F32 getY() {
        return y;
    }

    public U32I32F32 getZ() {
        return z;
    }
}
