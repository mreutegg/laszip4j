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
import java.nio.ByteOrder;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASattribute {

    private static final long UNSIGNED_MASK = 0x7fffffffffffffffL;

    public static final int LAS_ATTRIBUTE_U8  = 0;
    public static final int LAS_ATTRIBUTE_I8  = 1;
    public static final int LAS_ATTRIBUTE_U16 = 2;
    public static final int LAS_ATTRIBUTE_I16 = 3;
    public static final int LAS_ATTRIBUTE_U32 = 4;
    public static final int LAS_ATTRIBUTE_int = 5;
    public static final int LAS_ATTRIBUTE_U64 = 6;
    public static final int LAS_ATTRIBUTE_I64 = 7;
    public static final int LAS_ATTRIBUTE_F32 = 8;
    public static final int LAS_ATTRIBUTE_F64 = 9;
    
    
    public byte[] reserved = new byte[2];           // 2 bytes
    public byte data_type;                          // 1 byte
    public byte options;                            // 1 byte
    public byte[] name = new byte[32];              // 32 bytes
    public byte[] unused = new byte[4];             // 4 bytes
    public U64I64F64[] no_data = newU64I64F64(3);     // 24 = 3*8 bytes
    public U64I64F64[] min = newU64I64F64(3);         // 24 = 3*8 bytes
    public U64I64F64[] max = newU64I64F64(3);         // 24 = 3*8 bytes
    public double[] scale = new double[3];          // 24 = 3*8 bytes
    public double[] offset = new double[3];         // 24 = 3*8 bytes
    public byte[] description = new byte[32];       // 32 bytes

    private LASattribute() {
    }

    LASattribute(byte u_size)
    {
        if (u_size == 0) throw new IllegalArgumentException();
        scale[0] = scale[1] = scale[2] = 1.0;
        this.options = u_size;
    };

    LASattribute(int type, String name) {
        this(type, name, null, 1);
    }

    public LASattribute(int type, String name, String description) {
        this(type, name, description, 1);
    }

    LASattribute(int type, String name, String description, int dim)
    {
        if (type > LAS_ATTRIBUTE_F64) throw new IllegalArgumentException();
        if ((dim < 1) || (dim > 3)) throw new IllegalArgumentException();
        if (name == null) throw new NullPointerException();
        scale[0] = scale[1] = scale[2] = 1.0;
        this.data_type = (byte) ((dim-1)*10+type+1);
        System.arraycopy(MyDefs.asByteArray(name), 0, this.name, 0, name.length());
        this.name[name.length()] = '\0';
        if (description != null) {
            System.arraycopy(MyDefs.asByteArray(description), 0, this.description, 0, description.length());
            this.description[description.length()] = '\0';
        }
    }

    private static U64I64F64[] newU64I64F64(int size) {
        U64I64F64[] data = new U64I64F64[size];
        for (int i = 0; i < size; i++) {
            data[i] = new U64I64F64();
        }
        return data;
    }

    public boolean set_no_dataU(byte no_data, int dim) { if ((0 == get_type()) && (dim < get_dim())) { this.no_data[dim].setU64(Byte.toUnsignedLong(no_data)); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_data(byte no_data, int dim) { if ((1 == get_type()) && (dim < get_dim())) { this.no_data[dim].setI64(no_data); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_dataU(short no_data, int dim) { if ((2 == get_type()) && (dim < get_dim())) { this.no_data[dim].setU64(Short.toUnsignedLong(no_data)); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_data(short no_data, int dim) { if ((3 == get_type()) && (dim < get_dim())) { this.no_data[dim].setI64(no_data); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_dataU(int no_data, int dim) { if ((4 == get_type()) && (dim < get_dim())) { this.no_data[dim].setU64(Integer.toUnsignedLong(no_data)); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_data(int no_data, int dim) { if ((5 == get_type()) && (dim < get_dim())) { this.no_data[dim].setI64(no_data); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_dataU(long no_data, int dim) { if ((6 == get_type()) && (dim < get_dim())) { this.no_data[dim].setU64(no_data); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_data(long no_data, int dim) { if ((7 == get_type()) && (dim < get_dim())) { this.no_data[dim].setU64(no_data); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_data(float no_data, int dim) { if ((8 == get_type()) && (dim < get_dim())) { this.no_data[dim].setF64(no_data); options |= 0x01; return TRUE; } return FALSE; };
    public boolean set_no_data(double no_data, int dim) { if ((9 == get_type()) && (dim < get_dim())) { this.no_data[dim].setF64(no_data); options |= 0x01; return TRUE; } return FALSE; };

    public void set_min(byte[] min, int dim) { this.min[dim] = cast(min); options |= 0x02; };
    public void update_min(byte[] min, int dim) { this.min[dim] = smallest(cast(min), this.min[dim]); };
    public boolean set_minU(byte min, int dim) { if ((0 == get_type()) && (dim < get_dim())) { this.min[dim].setU64(Byte.toUnsignedLong(min)); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_min(byte min, int dim) { if ((1 == get_type()) && (dim < get_dim())) { this.min[dim].setI64(min); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_minU(short min, int dim) { if ((2 == get_type()) && (dim < get_dim())) { this.min[dim].setU64(Short.toUnsignedLong(min)); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_min(short min, int dim) { if ((3 == get_type()) && (dim < get_dim())) { this.min[dim].setI64(min); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_minU(int min, int dim) { if ((4 == get_type()) && (dim < get_dim())) { this.min[dim].setU64(Integer.toUnsignedLong(min));; options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_min(int min, int dim) { if ((5 == get_type()) && (dim < get_dim())) { this.min[dim].setI64(min); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_minU(long min, int dim) { if ((6 == get_type()) && (dim < get_dim())) { this.min[dim].setU64(min); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_min(long min, int dim) { if ((7 == get_type()) && (dim < get_dim())) { this.min[dim].setI64(min); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_min(float min, int dim) { if ((8 == get_type()) && (dim < get_dim())) { this.min[dim].setF64(min); options |= 0x02; return TRUE; } return FALSE; };
    public boolean set_min(double min, int dim) { if ((9 == get_type()) && (dim < get_dim())) { this.min[dim].setF64(min); options |= 0x02; return TRUE; } return FALSE; };

    public void set_max(byte[] max, int dim) { this.max[dim] = cast(max); options |= 0x04; };
    public void update_max(byte[] max, int dim) { this.max[dim] = biggest(cast(max), this.max[dim]); };
    public boolean set_maxU(byte max, int dim) { if ((0 == get_type()) && (dim < get_dim())) { this.max[dim].setU64(Byte.toUnsignedLong(max)); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_max(byte max, int dim) { if ((1 == get_type()) && (dim < get_dim())) { this.max[dim].setI64(max); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_maxU(short max, int dim) { if ((2 == get_type()) && (dim < get_dim())) { this.max[dim].setU64(Short.toUnsignedLong(max)); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_max(short max, int dim) { if ((3 == get_type()) && (dim < get_dim())) { this.max[dim].setI64(max); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_maxU(int max, int dim) { if ((4 == get_type()) && (dim < get_dim())) { this.max[dim].setU64(Integer.toUnsignedLong(max)); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_max(int max, int dim) { if ((5 == get_type()) && (dim < get_dim())) { this.max[dim].setI64(max); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_maxU(long max, int dim) { if ((6 == get_type()) && (dim < get_dim())) { this.max[dim].setU64(max);; options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_max(long max, int dim) { if ((7 == get_type()) && (dim < get_dim())) { this.max[dim].setI64(max); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_max(float max, int dim) { if ((8 == get_type()) && (dim < get_dim())) { this.max[dim].setF64(max); options |= 0x04; return TRUE; } return FALSE; };
    public boolean set_max(double max, int dim) { if ((9 == get_type()) && (dim < get_dim())) { this.max[dim].setF64(max); options |= 0x04; return TRUE; } return FALSE; };

    public boolean set_scale(double scale, int dim) { if (data_type != 0) { this.scale[dim] = scale; options |= 0x08; return TRUE; } return FALSE; };
    public boolean set_offset(double offset, int dim) { if (data_type != 0) { this.offset[dim] = offset; options |= 0x10; return TRUE; } return FALSE; };

    public boolean has_no_data() { return (options & 0x01) != 0; };
    public boolean has_min() { return (options & 0x02) != 0; };
    public boolean has_max() { return (options & 0x04) != 0; };
    public boolean has_scale() { return (options & 0x08) != 0; };
    public boolean has_offset() { return (options & 0x10) != 0; };

    public int get_size() // unsigned
    {
        if (data_type != 0)
        {
            int[] size_table = { 1, 1, 2, 2, 4, 4, 8, 8, 4, 8 };
            int type = get_type();
            int dim = get_dim();
            return size_table[type]*dim;
        }
        else
        {
            return options;
        }
    };

    public double get_value_as_float(byte[] value)
    {
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        double casted_value;
        int type = get_type();
        if (type == 0) {
            casted_value = (double) Byte.toUnsignedInt(buffer.get());
        } else if (type == 1) {
            casted_value = (double) buffer.get();
        } else if (type == 2) {
            casted_value = (double) Short.toUnsignedInt(buffer.getShort());
        } else if (type == 3) {
            casted_value = (double) buffer.getShort();
        } else if (type == 4) {
            casted_value = (double) Integer.toUnsignedLong(buffer.getInt());
        } else if (type == 5) {
            casted_value = (double) buffer.getInt();
        } else if (type == 6) {
            casted_value = unsignedLongAsDouble(buffer.getLong());
        } else if (type == 7) {
            casted_value = (double) buffer.getLong();
        } else if (type == 8) {
            casted_value = (double) buffer.getFloat();
        } else {
            casted_value = buffer.getDouble();
        }
        return offset[0]+scale[0]*casted_value;
    }

    public static int getMemory() {
        return 192;
    }

    public byte[] asByteArray() {
        ByteBuffer bb = ByteBuffer.allocate(getMemory());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (byte b : reserved) {
            bb.put(b);
        }
        bb.put(data_type);
        bb.put(options);
        bb.put(name);
        bb.position(36);
        bb.put(unused);
        for (U64I64F64 data : no_data) {
            bb.putLong(data.getI64());
        }
        for (U64I64F64 data : min) {
            bb.putLong(data.getI64());
        }
        for (U64I64F64 data : max) {
            bb.putLong(data.getI64());
        }
        for (double s : scale) {
            bb.putDouble(s);
        }
        for (double o : offset) {
            bb.putDouble(o);
        }
        bb.put(MyDefs.asByteArray(new String(description)));
        return bb.array();
    }

    private int get_type()
    {
        return ((int)data_type - 1)%10;
    };
    
    private int get_dim()
    {
        return 1 + ((int)data_type - 1)/10;
    };
    
    private U64I64F64 cast(byte[] value)
    {
        ByteBuffer buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN);
        int type = get_type();
        U64I64F64 casted_value = new U64I64F64();
        if (type == 0) {
            casted_value.setU64(Byte.toUnsignedInt(buffer.get()));
        } else if (type == 1) {
            casted_value.setI64(buffer.get());
        } else if (type == 2) {
            casted_value.setU64(Short.toUnsignedLong(buffer.getShort()));
        } else if (type == 3) {
            casted_value.setI64(buffer.getShort());
        } else if (type == 4) {
            casted_value.setU64(Integer.toUnsignedLong(buffer.getInt()));
        } else if (type == 5) {
            casted_value.setI64(buffer.getInt());
        } else if (type == 6) {
            casted_value.setU64(buffer.getLong());
        } else if (type == 7) {
            casted_value.setI64(buffer.getLong());
        } else if (type == 8) {
            casted_value.setF64(buffer.getFloat());
        } else {
            casted_value.setF64(buffer.getDouble());
        }
        return casted_value;
    };
    
    private U64I64F64 smallest(U64I64F64 a, U64I64F64 b)
    {
        int type = get_type();
        if (type >= 8) // float compare
        {
            if (a.getF64() < b.getF64()) return a;
            else               return b;
        }
        if ((type & 1) != 0) // int compare
        {
            if (a.getI64() < b.getI64()) return a;
            else               return b;
        }
        if (Long.compareUnsigned(a.getU64(), b.getU64()) < 0) return a;
        else               return b;
    };
    
    private U64I64F64 biggest(U64I64F64 a, U64I64F64 b)
    {
        int type = get_type();
        if (type >= 8) // float compare
        {
            if (a.getF64() > b.getF64()) return a;
            else               return b;
        }
        if ((type & 1) != 0) // int compare
        {
            if (a.getI64() > b.getI64()) return a;
            else               return b;
        }
        if (Long.compareUnsigned(a.getU64(), b.getU64()) > 0) return a;
        else               return b;
    };

    private static double unsignedLongAsDouble(long value) {
        double dValue = (double) (value & UNSIGNED_MASK);
        if (value < 0) {
            dValue += 0x1.0p63;
        }
        return dValue;
    }

    public static LASattribute[] fromByteArray(byte[] data) {
        ByteBuffer bb = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        LASattribute[] attributes = new LASattribute[data.length / getMemory()];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = LASattribute.fromByteBuffer(bb);
        }
        return attributes;
    }

    private static LASattribute fromByteBuffer(ByteBuffer bb) {
        LASattribute attr = new LASattribute();
        bb.get(attr.reserved);
        attr.data_type = bb.get();
        attr.options = bb.get();
        bb.get(attr.name);
        bb.get(attr.unused);
        for (U64I64F64 s : attr.no_data) {
            s.setI64(bb.getLong());
        }
        for (U64I64F64 s : attr.min) {
            s.setI64(bb.getLong());
        }
        for (U64I64F64 s : attr.max) {
            s.setI64(bb.getLong());
        }
        for (int i = 0; i < attr.scale.length; i++) {
            attr.scale[i] = bb.getDouble();
        }
        for (int i = 0; i < attr.offset.length; i++) {
            attr.offset[i] = bb.getDouble();
        }
        bb.get(attr.description);
        return attr;
    }
}
