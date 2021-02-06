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

import java.security.SecureRandom;

public final class Cstdlib {

    public static final int RAND_MAX = Character.MAX_VALUE;

    private static final long SEED = System.currentTimeMillis();

    private static final Random RANDOM = new SecureRandom();
    
    static {
        RANDOM.setSeed(SEED);
    }    

    private Cstdlib() {
    }


    public static double atof(String s) {
        return Double.parseDouble(s.trim());
    }

    public static int atoi(String s) {
        return Integer.parseInt(s.trim());
    }

    public static void srand(int seed) {
        if (seed == 1) {
            RANDOM.setSeed(SEED);
        } else {
            RANDOM.setSeed(seed);
        }
    }

    public static int rand() {
        return RANDOM.nextInt(RAND_MAX + 1);
    }
}
