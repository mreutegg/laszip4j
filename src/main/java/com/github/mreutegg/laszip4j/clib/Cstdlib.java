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

import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Random;

public final class Cstdlib {

    public static final int RAND_MAX = Character.MAX_VALUE;

    private static final ByteBuffer SEED = ByteBuffer.allocate(32)
            .put(SecureRandom.getSeed(32));

    private static Random random = newSecureRandom(SEED);

    private Cstdlib() {
    }

    public static double atof(String s) {
        return Double.parseDouble(s.trim());
    }

    public static int atoi(String s) {
        return Integer.parseInt(s.trim());
    }

    public static void srand(int seed) {
        ByteBuffer seedBytes = SEED;
        if (seed != 1) {
            seedBytes = ByteBuffer.allocate(4).putInt(seed);
        }
        random = newSecureRandom(seedBytes);
    }

    public static int rand() {
        return random.nextInt(RAND_MAX + 1);
    }

    private static SecureRandom newSecureRandom(ByteBuffer seed) {
        try {
            SecureRandom r = SecureRandom.getInstance("SHA1PRNG");
            r.setSeed(seed.array());
            return r;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException();
        }
    }
}
