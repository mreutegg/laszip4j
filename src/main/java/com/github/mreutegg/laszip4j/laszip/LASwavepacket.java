/*
 * Copyright 2007-2015, martin isenburg, rapidlasso - fast tools to catch reality
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
import java.util.Arrays;

public class LASwavepacket {
    
    private byte index;
    private long u_offset;
    private int u_size;
    private float location;
    private float x;
    private float y;
    private float z;

    private final ByteBuffer data = ByteBuffer.allocate(29);
    
    public LASwavepacket() {zero();}
    
    public void zero() {
        Arrays.fill(data.array(), (byte) 0);
    }
    public byte getIndex() {return data.get(0);}
    public long getOffset() {return data.getLong(1);}
    public int getSize() {return data.getInt(9);}
    public float getLocation() {return data.getFloat(13);}
    public float getXt() {return data.getFloat(17);}
    public float getYt() {return data.getFloat(21);}
    public float getZt() {return data.getFloat(25);}
    public void setIndex(byte index) {this.data.put(0,index);}
    public void setOffset(long u_offset) {
        this.data.putLong(1, u_offset);}
    public void setSize(int u_size) {
        this.data.putInt(9, u_size);}
    public void setLocation(float location) { this.data.putFloat(13, location);}
    public void setXt(float xt) {
        this.data.putFloat(17, xt);}
    public void setYt(float yt) {
        this.data.putFloat(21, yt);}
    public void setZt(float zt) {
        this.data.putFloat(25, zt);}
    public void flipDirection() {
        setXt(getXt() * -1); setYt(getYt() * -1); setZt(getZt() * -1);}
    public byte[] asByteArray() {
        return data.array();
    }
}
