/*
 * Copyright 2007-2012, martin isenburg, rapidlasso - fast tools to catch reality
 *
 * This is free software; you can redistribute and/or modify it under the
 * terms of the GNU Lesser General Licence as published by the Free Software
 * Foundation. See the LICENSE.txt file for more information.
 *
 * This software is distributed WITHOUT ANY WARRANTY and without even the
 * implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package com.github.mreutegg.laszip4j.laslib;

import com.github.mreutegg.laszip4j.laszip.ArithmeticEncoder;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOut;
import com.github.mreutegg.laszip4j.laszip.ByteStreamOutFile;
import com.github.mreutegg.laszip4j.laszip.IntegerCompressor;
import com.github.mreutegg.laszip4j.laszip.LASpoint;

import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import static com.github.mreutegg.laszip4j.clib.Cstdio.fclose;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fopenRAF;
import static com.github.mreutegg.laszip4j.clib.Cstdio.fprintf;
import static com.github.mreutegg.laszip4j.clib.Cstdio.sprintf;
import static com.github.mreutegg.laszip4j.clib.Cstring.strlen;
import static com.github.mreutegg.laszip4j.laslib.LasDefinitions.LAS_TOOLS_VERSION;
import static com.github.mreutegg.laszip4j.laszip.MyDefs.asByteArray;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

public class LASwaveform13writer {

    private static final PrintStream stderr = System.err;

    private LASwaveformDescription[] waveforms;
    private RandomAccessFile file;
    private ByteStreamOut stream;

    private ArithmeticEncoder enc;
    private IntegerCompressor ic8;
    private IntegerCompressor ic16;

    LASwaveform13writer()
    {
        waveforms = null;
        file = null;
        stream = null;
        enc = null;
        ic8 = null;
        ic16 = null;
    }

    boolean open(String file_name, LASvlr_wave_packet_descr[] wave_packet_descr)
    {
        if (file_name == null)
        {
            fprintf(stderr,"ERROR: file name pointer is zero\n");
            return FALSE;
        }

        if (wave_packet_descr == null)
        {
            fprintf(stderr,"ERROR: wave packet descriptor pointer is zero\n");
            return FALSE;
        }

        // copy relevant wave packet descriptions and check if compressed or not

        char i, number = 0;
        boolean compressed = FALSE;

        if (waveforms == null)
        {
            waveforms = new LASwaveformDescription[256];
        }

        for (i = 0; i < 256; i++)
        {
            if (wave_packet_descr[i] != null)
            {
                if (waveforms[i] == null)
                {
                    waveforms[i] = new LASwaveformDescription();
                }
                waveforms[i].compression = wave_packet_descr[i].getCompressionType();
                waveforms[i].nbits = wave_packet_descr[i].getBitsPerSample();
                waveforms[i].nsamples = (char) wave_packet_descr[i].getNumberOfSamples();
                compressed = compressed || (waveforms[i].compression > 0);
                number++;
            }
            else
            {
                if (waveforms[i] != null)
                {
                    waveforms[i] = null;
                }
            }
        }

        // create file name and open file

        char[] file_name_temp = file_name.toCharArray();

        int len = strlen(file_name_temp);
        if (file_name_temp[len-3] == 'L' || file_name_temp[len-3] == 'W')
        {
            file_name_temp[len-3] = 'W';
            file_name_temp[len-2] = 'D';
            file_name_temp[len-1] = (compressed ? 'Z' : 'P');
        }
        else
        {
            file_name_temp[len-3] = 'w';
            file_name_temp[len-2] = 'd';
            file_name_temp[len-1] = (compressed ? 'z' : 'p');
        }
        file = fopenRAF(file_name_temp, "wb");

        if (file == null)
        {
            fprintf(stderr, "ERROR: cannot open waveform file '%s'\n", new String(file_name_temp));
            return FALSE;
        }

        // create stream

        stream = new ByteStreamOutFile(file);

        // write extended variable length header variable after variable (to avoid alignment issues)

        char reserved = 0xAABB;
        if (!stream.put16bitsLE(reserved))
        {
            fprintf(stderr,"ERROR: writing EVLR reserved\n");
            return FALSE;
        }
        byte[] user_id = new byte[16];

        System.arraycopy(asByteArray("LASF_Spec"), 0, user_id, 0, "LASF_Spec".length());
        if (!stream.putBytes(user_id, 16))
        {
            fprintf(stderr,"ERROR: writing EVLR user_id\n");
            return FALSE;
        }
        char record_id = 65535;
        if (!stream.put16bitsLE(record_id))
        {
            fprintf(stderr,"ERROR: writing EVLR record_id\n");
            return FALSE;
        }
        long record_length_after_header = 0;
        if (!stream.put64bitsLE(record_length_after_header))
        {
            fprintf(stderr,"ERROR: writing EVLR record_length_after_header\n");
            return FALSE;
        }
        byte[] description = new byte[32];
        String desc = String.format("%s by LAStools (%d)", (compressed ? "compressed" : "created"), LAS_TOOLS_VERSION);
        System.arraycopy(asByteArray(desc), 0, description, 0, desc.length());
        if (!stream.putBytes(description, 32))
        {
            fprintf(stderr,"ERROR: writing EVLR description\n");
            return FALSE;
        }

        // write waveform descriptor cross-check

        char[] magic = new char[25];
        sprintf(magic, "LAStools waveform %d", LAS_TOOLS_VERSION);

        if (!stream.putBytes(asByteArray(new String(magic)), 24))
        {
            fprintf(stderr,"ERROR: writing waveform descriptor cross-check\n");
            return FALSE;
        }

        if (!stream.put16bitsLE(number))
        {
            fprintf(stderr,"ERROR: writing number of waveform descriptors\n");
            return FALSE;
        }

        for (i = 0; i < 256; i++)
        {
            if (waveforms[i] != null)
            {
                if (!stream.put16bitsLE(i))
                {
                    fprintf(stderr,"ERROR: writing index of waveform descriptor %d\n", i);
                    return FALSE;
                }
                if (!stream.putByte(waveforms[i].compression))
                {
                    fprintf(stderr,"ERROR: writing compression of waveform descriptor %d\n", i);
                    return FALSE;
                }
                if (!stream.putByte(waveforms[i].nbits))
                {
                    fprintf(stderr,"ERROR: writing nbits of waveform descriptor %d\n", i);
                    return FALSE;
                }
                if (!stream.put16bitsLE(waveforms[i].nsamples))
                {
                    fprintf(stderr,"ERROR: writing nsamples of waveform descriptor %d\n", i);
                    return FALSE;
                }
            }
        }

        // create compressor

        if (compressed)
        {
            if (enc == null) enc = new ArithmeticEncoder();
            if (ic8 == null) ic8 = new IntegerCompressor(enc, 8);
            if (ic16 == null) ic16 = new IntegerCompressor(enc, 16);
        }

        return TRUE;
    }

    public boolean write_waveform(LASpoint point, byte[] samples)
    {
        int index = point.wavepacket.getIndex();
        if (index == 0)
        {
            return FALSE;
        }

        int nbits = waveforms[index].nbits;
        if ((nbits != 8) && (nbits != 16))
        {
            fprintf(stderr, "ERROR: waveform with %d bits per samples not supported yet\n", nbits);
            return FALSE;
        }

        int nsamples = waveforms[index].nsamples;
        if (nsamples == 0)
        {
            fprintf(stderr, "ERROR: waveform has no samples\n");
            return FALSE;
        }

        // set offset to waveform data

        long offset = stream.tell();
        point.wavepacket.setOffset(offset);

        // write waveform

        if (waveforms[index].compression == 0)
        {
            int size = ((nbits/8) * nsamples);
            if (!stream.putBytes(samples, size))
            {
                fprintf(stderr, "ERROR: cannot write %d bytes for waveform with %d samples of %d bits\n", size, nsamples, nbits);
                return FALSE;
            }
            point.wavepacket.setSize(size);
        }
        else
        {
            int s_count;
            if (nbits == 8)
            {
                stream.putBytes(samples, 1);
                enc.init(stream);
                ic8.initCompressor();
                for (s_count = 1; s_count < nsamples; s_count++)
                {
                    ic8.compress(samples[s_count-1], samples[s_count]);
                }
            }
            else
            {
                stream.putBytes(samples, 2);
                enc.init(stream);
                ic16.initCompressor();
                ByteBuffer bb = ByteBuffer.wrap(samples).order(ByteOrder.LITTLE_ENDIAN);
                for (s_count = 1; s_count < nsamples; s_count++)
                {
                    ic16.compress(bb.getChar((s_count-1)*2), bb.getChar(s_count*2));
                }
            }
            enc.done();
            int size = (int)(stream.tell() - offset);
            point.wavepacket.setSize(size);
        }

        return TRUE;
    }

    void close()
    {
        if (stream.isSeekable())
        {
            long record_length_after_header = stream.tell();
            record_length_after_header -= 60;
            stream.seek(18);
            if (!stream.put64bitsLE(record_length_after_header))
            {
                fprintf(stderr,"ERROR: updating EVLR record_length_after_header\n");
            }
            stream.seekEnd();
        }
        if (stream != null)
        {
            fclose(stream);
            stream = null;
        }
        if (file != null)
        {
            fclose(file);
            file = null;
        }
    }
}
