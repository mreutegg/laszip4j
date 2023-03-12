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

import static com.github.mreutegg.laszip4j.laszip.LASzip.*;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.U32_MAX;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwritePoint {

    private ByteStreamOut outstream;
    private int num_writers;                // unsigned
    private LASwriteItem[] writers;
    private LASwriteItemRaw[] writers_raw;
    private LASwriteItemCompressed[] writers_compressed;
    private ArithmeticEncoder enc;
    private boolean layered_las14_compression;
    // used for chunking
    private int chunk_size;                 // unsigned
    private int chunk_count;                // unsigned
    private int number_chunks;              // unsigned
    private int alloced_chunks;             // unsigned
    private int[] chunk_sizes;              // unsigned
    private int[] chunk_bytes;              // unsigned
    private long chunk_start_position;
    private long chunk_table_start_position;
    private boolean add_chunk_to_table() {
        if (number_chunks == alloced_chunks)
        {
            if (chunk_bytes == null)
            {
                alloced_chunks = 1024;
                if (chunk_size == U32_MAX) chunk_sizes = new int[alloced_chunks];
                chunk_bytes = new int[alloced_chunks];
            }
            else
            {
                alloced_chunks *= 2;
                if (chunk_size == U32_MAX) chunk_sizes = new int[alloced_chunks];
                chunk_bytes = new int[alloced_chunks];
            }
        }
        long position = outstream.tell();
        if (chunk_size == U32_MAX) chunk_sizes[number_chunks] = chunk_count;
        chunk_bytes[number_chunks] = (int) (position - chunk_start_position);
        chunk_start_position = position;
        number_chunks++;
        return TRUE;
    }
    private boolean write_chunk_table() {
        int i; // unsigned
        long position = outstream.tell();
        if (chunk_table_start_position != -1) // stream is seekable
        {
            if (!outstream.seek(chunk_table_start_position))
            {
                return FALSE;
            }
            if (!outstream.put64bitsLE(position))
            {
                return FALSE;
            }
            if (!outstream.seek(position))
            {
                return FALSE;
            }
        }
        int version = 0; // unsigned
        if (!outstream.put32bitsLE(version))
        {
            return FALSE;
        }
        if (!outstream.put32bitsLE(number_chunks))
        {
            return FALSE;
        }
        if (number_chunks > 0)
        {
            enc.init(outstream);
            IntegerCompressor ic = new IntegerCompressor(enc, 32, 2);
            ic.initCompressor();
            for (i = 0; Integer.compareUnsigned(i, number_chunks) < 0; i++)
            {
                if (chunk_size == U32_MAX) ic.compress((i != 0 ? chunk_sizes[i-1] : 0), chunk_sizes[i], 0);
                ic.compress((i != 0 ? chunk_bytes[i-1] : 0), chunk_bytes[i], 1);
            }
            enc.done();
        }
        if (chunk_table_start_position == -1) // stream is not-seekable
        {
            if (!outstream.put64bitsLE(position))
            {
                return FALSE;
            }
        }
        return TRUE;
    }

    public boolean setup(int /* unsigned */ num_items, LASitem[] items) {
        return setup(num_items, items, null);
    }

    // should only be called *once*
    public boolean setup(int /* unsigned */ num_items, LASitem[] items, LASzip laszip) {
        int i;

        // is laszip exists then we must use its items
        if (laszip != null)
        {
            if (num_items == 0) return FALSE;
            if (items == null) return FALSE;
            if (num_items != laszip.num_items) return FALSE;
            if (items != laszip.items) return FALSE;
        }

        // create entropy encoder (if requested)
        enc = null;
        if (laszip != null && laszip.compressor != 0)
        {
            switch (laszip.coder)
            {
                case LASZIP_CODER_ARITHMETIC:
                    enc = new ArithmeticEncoder();
                    break;
                default:
                    // entropy decoder not supported
                    return FALSE;
            }
            // maybe layered compression for LAS 1.4
            layered_las14_compression = (laszip.compressor == LASZIP_COMPRESSOR_LAYERED_CHUNKED);
        }

        // initizalize the writers
        writers = null;
        num_writers = num_items;

        // disable chunking
        chunk_size = U32_MAX;

        // always create the raw writers
        writers_raw = new LASwriteItemRaw[num_writers];
        for (i = 0; Integer.compareUnsigned(i, num_writers) < 0; i++)
        {
            switch (items[i].type)
            {
                case POINT10:
                    writers_raw[i] = new LASwriteItemRaw_POINT10();
                    break;
                case GPSTIME11:
                    writers_raw[i] = new LASwriteItemRaw_GPSTIME11();
                    break;
                case RGB12:
                case RGB14:
                    writers_raw[i] = new LASwriteItemRaw_RGB12();
                    break;
                case BYTE:
                case BYTE14:
                    writers_raw[i] = new LASwriteItemRaw_BYTE(items[i].size);
                    break;
                case POINT14:
                    writers_raw[i] = new LASwriteItemRaw_POINT14();
                    break;
                case RGBNIR14:
                    writers_raw[i] = new LASwriteItemRaw_RGBNIR14();
                    break;
                case WAVEPACKET13:
                case WAVEPACKET14:
                    writers_raw[i] = new LASwriteItemRaw_WAVEPACKET13();
                    break;
                default:
                    return FALSE;
            }
        }

        // if needed create the compressed writers and set versions
        if (enc != null)
        {
            writers_compressed = new LASwriteItemCompressed[num_writers];
            for (i = 0; i < num_writers; i++)
            {
                switch (items[i].type)
                {
                    case POINT10:
                        if (items[i].version == 1)
                            writers_compressed[i] = new LASwriteItemCompressed_POINT10_v1(enc);
                        else if (items[i].version == 2)
                            writers_compressed[i] = new LASwriteItemCompressed_POINT10_v2(enc);
                        else
                            return FALSE;
                        break;
                    case GPSTIME11:
                        if (items[i].version == 1)
                            writers_compressed[i] = new LASwriteItemCompressed_GPSTIME11_v1(enc);
                        else if (items[i].version == 2)
                            writers_compressed[i] = new LASwriteItemCompressed_GPSTIME11_v2(enc);
                        else
                            return FALSE;
                        break;
                    case RGB12:
                        if (items[i].version == 1)
                            writers_compressed[i] = new LASwriteItemCompressed_RGB12_v1(enc);
                        else if (items[i].version == 2)
                            writers_compressed[i] = new LASwriteItemCompressed_RGB12_v2(enc);
                        else
                            return FALSE;
                        break;
                    case BYTE:
                        if (items[i].version == 1)
                            writers_compressed[i] = new LASwriteItemCompressed_BYTE_v1(enc, items[i].size);
                        else if (items[i].version == 2)
                            writers_compressed[i] = new LASwriteItemCompressed_BYTE_v2(enc, items[i].size);
                        else
                            return FALSE;
                        break;
                    case POINT14:
                        if (items[i].version == 3)
                            writers_compressed[i] = new LASwriteItemCompressed_POINT14_v3(enc);
                        else if (items[i].version == 4)
                            writers_compressed[i] = new LASwriteItemCompressed_POINT14_v4(enc);
                        else
                            return FALSE;
                        break;
                    case RGB14:
                        if (items[i].version == 3)
                            writers_compressed[i] = new LASwriteItemCompressed_RGB14_v3(enc);
                        else if (items[i].version == 4)
                            writers_compressed[i] = new LASwriteItemCompressed_RGB14_v4(enc);
                        else
                            return FALSE;
                        break;
                    case RGBNIR14:
                        if (items[i].version == 3)
                            writers_compressed[i] = new LASwriteItemCompressed_RGBNIR14_v3(enc);
                        else if (items[i].version == 4)
                            writers_compressed[i] = new LASwriteItemCompressed_RGBNIR14_v4(enc);
                        else
                            return FALSE;
                        break;
                    case BYTE14:
                        if (items[i].version == 3)
                            writers_compressed[i] = new LASwriteItemCompressed_BYTE14_v3(enc, items[i].size);
                        else if (items[i].version == 4)
                            writers_compressed[i] = new LASwriteItemCompressed_BYTE14_v4(enc, items[i].size);
                        else
                            return FALSE;
                        break;
                    case WAVEPACKET13:
                        if (items[i].version == 1)
                            writers_compressed[i] = new LASwriteItemCompressed_WAVEPACKET13_v1(enc);
                        else
                            return FALSE;
                        break;
                    case WAVEPACKET14:
                        if (items[i].version == 3)
                            writers_compressed[i] = new LASwriteItemCompressed_WAVEPACKET14_v3(enc);
                        else if (items[i].version == 4)
                            writers_compressed[i] = new LASwriteItemCompressed_WAVEPACKET14_v4(enc);
                        else
                            return FALSE;
                        break;
                    default:
                        return FALSE;
                }
            }
            if (laszip.compressor != LASZIP_COMPRESSOR_POINTWISE)
            {
                if (laszip.chunk_size != 0) chunk_size = laszip.chunk_size;
                chunk_count = 0;
                number_chunks = U32_MAX;
            }
        }
        return TRUE;
    }

    public boolean init(ByteStreamOut outstream) {
        if (outstream == null) return FALSE;
        this.outstream = outstream;

        // if chunking is enabled
        if (number_chunks == U32_MAX)
        {
            number_chunks = 0;
            if (outstream.isSeekable())
            {
                chunk_table_start_position = outstream.tell();
            }
            else
            {
                chunk_table_start_position = -1;
            }
            outstream.put64bitsLE(chunk_table_start_position);
            chunk_start_position = outstream.tell();
        }

        int i; // unsigned
        for (i = 0; Integer.compareUnsigned(i, num_writers) < 0; i++)
        {
            writers_raw[i].init(outstream);
        }

        if (enc != null)
        {
            writers = null;
        }
        else
        {
            writers = writers_raw;
        }

        return TRUE;
    }
    public boolean write(PointDataRecord[] pointRecords) {
        int i; // unsigned
        int context = 0; // unsigned

        if (chunk_count == chunk_size)
        {
            if (enc != null)
            {
                if (layered_las14_compression)
                {
                    // write how many points are in the chunk
                    outstream.put32bitsLE(chunk_count);
                    // write all layers
                    for (i = 0; i < num_writers; i++)
                    {
                        ((LASwriteItemCompressed)writers[i]).chunk_sizes();
                    }
                    for (i = 0; i < num_writers; i++)
                    {
                        ((LASwriteItemCompressed)writers[i]).chunk_bytes();
                    }
                }
                else
                {
                    enc.done();
                }
                add_chunk_to_table();
                init(outstream);
            }
            else
            {
                // happens *only* for uncompressed LAS with over U32_MAX points
                assert(chunk_size == U32_MAX);
            }
            chunk_count = 0;
        }
        chunk_count++;

        if (writers != null)
        {
            for (i = 0; i < num_writers; i++)
            {
                if (!writers[i].write(pointRecords[i], context))
                {
                    return FALSE;
                }
            }
        }
        else
        {
            for (i = 0; i < num_writers; i++)
            {
                if (!writers_raw[i].write(pointRecords[i], context))
                {
                    return FALSE;
                }
                writers_compressed[i].init(pointRecords[i], context);
            }
            writers = writers_compressed;
            assert(enc != null);
            enc.init(outstream);
        }
        return TRUE;
    }
    public boolean chunk() {
        if (chunk_start_position == 0 || chunk_size != U32_MAX)
        {
            return FALSE;
        }
        if (layered_las14_compression)
        {
            int i; // unsigned
            // write how many points are in the chunk
            outstream.put32bitsLE(chunk_count);
            // write all layers
            for (i = 0; Integer.compareUnsigned(i, num_writers) < 0; i++)
            {
                ((LASwriteItemCompressed)writers[i]).chunk_sizes();
            }
            for (i = 0; i < num_writers; i++)
            {
                ((LASwriteItemCompressed)writers[i]).chunk_bytes();
            }
        }
        else
        {
            enc.done();
        }
        add_chunk_to_table();
        init(outstream);
        chunk_count = 0;
        return TRUE;
    }
    public boolean done() {
        if (writers == writers_compressed)
        {
            if (layered_las14_compression)
            {
                int i; // unsigned
                // write how many points are in the chunk
                outstream.put32bitsLE(chunk_count);
                // write all layers
                for (i = 0; i < num_writers; i++)
                {
                    ((LASwriteItemCompressed)writers[i]).chunk_sizes();
                }
                for (i = 0; i < num_writers; i++)
                {
                    ((LASwriteItemCompressed)writers[i]).chunk_bytes();
                }
            }
            else
            {
                enc.done();
            }
            if (chunk_start_position != 0)
            {
                if (chunk_count != 0) add_chunk_to_table();
                return write_chunk_table();
            }
        }
        else if (writers == null)
        {
            if (chunk_start_position != 0)
            {
                return write_chunk_table();
            }
        }

        return TRUE;
    }
}
