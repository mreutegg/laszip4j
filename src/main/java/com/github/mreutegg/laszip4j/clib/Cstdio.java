/*
 * Copyright 2016 Marcel Reutegger
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
package com.github.mreutegg.laszip4j.clib;

import com.github.mreutegg.laszip4j.laszip.MyDefs;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.RandomAccessFile;

public final class Cstdio {

    private Cstdio() {
    }

    public static void fprintf(PrintStream ps, String msg, Object... args) {
        ps.printf(msg, args);
    }

    public static int fputc(int b, OutputStream out) {
        try {
            out.write(b);
            return b;
        } catch (IOException e) {
            return -1;
        }
    }

    public static InputStream fopenR(char[] filename, String mode) {
        return fopenR(new String(filename), mode);
    }

    public static InputStream fopenR(String filename, String mode) {
        File f = new File(filename);
        if (f.exists() && !f.delete()) {
            return null;
        }
        try {
            if (f.createNewFile()) {
                return new FileInputStream(filename);
            }
        } catch (IOException ignore) {
        }
        return null;
    }

    public static OutputStream fopen(String filename, String mode) {
        File f = new File(filename);
        if (f.exists() && !f.delete()) {
            return null;
        }
        try {
            return new BufferedOutputStream(new FileOutputStream(f));
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static OutputStream fopen(char[] filename, String mode) {
        return fopen(new String(filename), mode);
    }

    public static RandomAccessFile fopenRAF(char[] filename, String mode) {
        File f = new File(new String(filename));
        if (mode.contains("w") && f.exists() && !f.delete()) {
            return null;
        }
        try {
            mode = mode.replace("b", "");
            return new RandomAccessFile(f, mode);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static int fclose(Closeable c) {
        try {
            c.close();
            return 0;
        } catch (IOException e) {
            return -1;
        }
    }

    public static long ftell(PrintStream file) {
        return -1;
    }

    public static long ftell(RandomAccessFile file) {
        try {
            return file.getFilePointer();
        } catch (IOException e) {
            return -1;
        }
    }

    public static int sprintf(StringBuilder str, String format, Object... args) {
        String s = String.format(format, args);
        str.append(s);
        return s.length();
    }

    public static int sprintf(byte[] str, String format, Object... args) {
        StringBuilder sb = new StringBuilder();
        sprintf(sb, format, args);
        byte[] data = MyDefs.asByteArray(sb.toString());
        System.arraycopy(data, 0, str, 0, data.length);
        str[data.length] = '\0';
        return sb.length();
    }

    public static int sprintf(char[] str, String format, Object... args) {
        return sprintf(str, 0, format, args);
    }

    public static int sprintf(char[] str, int offset, String format, Object... args) {
        String s = String.format(format, args);
        System.arraycopy(s.toCharArray(), 0, str, offset, s.length());
        str[offset + s.length()] = '\0';
        return s.length();
    }
}
