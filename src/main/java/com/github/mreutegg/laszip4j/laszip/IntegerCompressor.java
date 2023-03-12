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

import static java.lang.Integer.compareUnsigned;

public class IntegerCompressor {

    private int u_k;

    private int u_contexts;
    private int u_bits_high;

    private int u_bits;
    private int u_range;

    private int u_corr_bits;
    private int u_corr_range;
    private int corr_min;
    private int corr_max;

    private ArithmeticEncoder enc;
    private ArithmeticDecoder dec;

    private ArithmeticModel[] mBits;

    private ArithmeticBitModel mCorrector0;
    private ArithmeticModel[] mCorrector;

    private int[][] corr_histogram;

    public IntegerCompressor(ArithmeticEncoder enc, int u_bits) {
        this(enc, u_bits, 1, 8, 0);
    }

    public IntegerCompressor(ArithmeticEncoder enc, int u_bits, int u_contexts) {
        this(enc, u_bits, u_contexts, 8, 0);
    }

    IntegerCompressor(ArithmeticEncoder enc, int u_bits, int u_contexts, int u_bits_high, int u_range)
    {
        assert(enc != null);
        this.enc = enc;
        this.dec = null;
        this.u_bits = u_bits;
        this.u_contexts = u_contexts;
        this.u_bits_high = u_bits_high;
        this.u_range = u_range;

        if (u_range != 0) // the corrector's significant bits and range
        {
            u_corr_bits = 0;
            u_corr_range = u_range;
            while (u_range != 0)
            {
                u_range = u_range >>> 1;
                u_corr_bits++;
            }
            if (u_corr_range == (1 << (u_corr_bits -1)))
            {
                u_corr_bits--;
            }
            // the corrector must fall into this interval
            corr_min = -(Integer.divideUnsigned(u_corr_range, 2));
            corr_max = corr_min + u_corr_range - 1;
        }
        else if (u_bits != 0 && compareUnsigned(u_bits, 32) < 0)
        {
            u_corr_bits = u_bits;
            u_corr_range = 1 << u_bits;
            // the corrector must fall into this interval
            corr_min = -(Integer.divideUnsigned(u_corr_range, 2));
            corr_max = corr_min + u_corr_range - 1;
        }
        else
        {
            u_corr_bits = 32;
            u_corr_range = 0;
            // the corrector must fall into this interval
            corr_min = Integer.MIN_VALUE;
            corr_max = Integer.MAX_VALUE;
        }

        u_k = 0;

        mBits = null;
        mCorrector = null;
    }

    public IntegerCompressor(ArithmeticDecoder dec, int u_bits) {
        this(dec, u_bits, 1);
    }

    IntegerCompressor(ArithmeticDecoder dec, int u_bits, int u_contexts, int u_bits_high, int u_range)
    {
        assert(dec != null);
        this.enc = null;
        this.dec = dec;
        this.u_bits = u_bits;
        this.u_contexts = u_contexts;
        this.u_bits_high = u_bits_high;
        this.u_range = u_range;

        if (u_range != 0) // the corrector's significant bits and range
        {
            u_corr_bits = 0;
            u_corr_range = u_range;
            while (u_range != 0)
            {
                u_range = u_range >>> 1;
                u_corr_bits++;
            }
            if (u_corr_range == (1 << (u_corr_bits -1)))
            {
                u_corr_bits--;
            }
            // the corrector must fall into this interval
            corr_min = -(Integer.divideUnsigned(u_corr_range, 2));
            corr_max = corr_min + u_corr_range - 1;
        }
        else if (u_bits != 0 && compareUnsigned(u_bits, 32) < 0)
        {
            u_corr_bits = u_bits;
            u_corr_range = 1 << u_bits;
            // the corrector must fall into this interval
            corr_min = -(Integer.divideUnsigned(u_corr_range, 2));
            corr_max = corr_min + u_corr_range - 1;
        }
        else
        {
            u_corr_bits = 32;
            u_corr_range = 0;
            // the corrector must fall into this interval
            corr_min = Integer.MIN_VALUE;
            corr_max = Integer.MAX_VALUE;
        }

        u_k = 0;

        mBits = null;
        mCorrector = null;
    }

    public IntegerCompressor(ArithmeticDecoder dec, int u_bits, int u_contexts) {
        this(dec, u_bits, u_contexts, 8, 0);
    }

    public int getK() {
        return u_k;
    }

    public void initCompressor()
    {
        int u_i;

        assert(enc != null);

        // maybe create the models
        if (mBits == null)
        {
            mBits = new ArithmeticModel[u_contexts];
            for (u_i = 0; u_i < u_contexts; u_i++)
            {
                mBits[u_i] = enc.createSymbolModel(u_corr_bits +1);
            }
            mCorrector = new ArithmeticModel[u_corr_bits +1];
            mCorrector0 = enc.createBitModel();
            for (u_i = 1; u_i <= u_corr_bits; u_i++)
            {
                if (u_i <= u_bits_high)
                {
                    mCorrector[u_i] = enc.createSymbolModel(1<<u_i);
                }
                else
                {
                    mCorrector[u_i] = enc.createSymbolModel(1<< u_bits_high);
                }
            }
        }

        // certainly init the models
        for (u_i = 0; u_i < u_contexts; u_i++)
        {
            enc.initSymbolModel(mBits[u_i]);
        }

        enc.initBitModel(mCorrector0);
        for (u_i = 1; u_i <= u_corr_bits; u_i++)
        {
            enc.initSymbolModel(mCorrector[u_i]);
        }
    }

    public void compress(int pred, int real) {
        compress(pred, real, 0);
    }

    void compress(int pred, int real, int u_context)
    {
        assert(enc != null);
        // the corrector will be within the interval [ - (corr_range - 1)  ...  + (corr_range - 1) ]
        int corr = real - pred;
        // we fold the corrector into the interval [ corr_min  ...  corr_max ]
        if (corr < corr_min) corr += u_corr_range;
        else if (corr > corr_max) corr -= u_corr_range;
        writeCorrector(corr, mBits[u_context]);
    }

    public void initDecompressor()
    {
        int u_i;

        assert(dec != null);

        // maybe create the models
        if (mBits == null)
        {
            mBits = new ArithmeticModel[u_contexts];
            for (u_i = 0; u_i < u_contexts; u_i++)
            {
                mBits[u_i] = dec.createSymbolModel(u_corr_bits +1);
            }
            mCorrector = new ArithmeticModel[u_corr_bits +1];
            mCorrector0 = dec.createBitModel();
            for (u_i = 1; u_i <= u_corr_bits; u_i++)
            {
                if (u_i <= u_bits_high)
                {
                    mCorrector[u_i] = dec.createSymbolModel(1<<u_i);
                }
                else
                {
                    mCorrector[u_i] = dec.createSymbolModel(1<< u_bits_high);
                }
            }
        }

        // certainly init the models
        for (u_i = 0; u_i < u_contexts; u_i++)
        {
            dec.initSymbolModel(mBits[u_i]);
        }
        dec.initBitModel(mCorrector0);
        for (u_i = 1; u_i <= u_corr_bits; u_i++)
        {
            dec.initSymbolModel(mCorrector[u_i]);
        }
    }

    public int decompress(int pred) {
        return decompress(pred, 0);
    }

    public int decompress(int pred, int u_context) {
        assert(dec != null);
        int real = pred + readCorrector(mBits[u_context]);
        if (real < 0) real += u_corr_range;
        else if (compareUnsigned(real, u_corr_range) >= 0) real -= u_corr_range;
        return real;
    }

    void writeCorrector(int c, ArithmeticModel mBits) {
        int u_c1;

        // find the tighest interval [ - (2^k - 1)  ...  + (2^k) ] that contains c

        u_k = 0;

        // do this by checking the absolute value of c (adjusted for the case that c is 2^k)

        u_c1 = (c <= 0 ? -c : c-1);

        // this loop could be replaced with more efficient code

        while (u_c1 != 0)
        {
            u_c1 = u_c1 >>> 1;
            u_k = u_k + 1;
        }

        // the number k is between 0 and corr_bits and describes the interval the corrector falls into
        // we can compress the exact location of c within this interval using k bits

        enc.encodeSymbol(mBits, u_k);

        if (u_k != 0) // then c is either smaller than 0 or bigger than 1
        {
            assert((c != 0) && (c != 1));
            if (compareUnsigned(u_k, 32) < 0)
            {
                // translate the corrector c into the k-bit interval [ 0 ... 2^k - 1 ]
                if (c < 0) // then c is in the interval [ - (2^k - 1)  ...  - (2^(k-1)) ]
                {
                    // so we translate c into the interval [ 0 ...  + 2^(k-1) - 1 ] by adding (2^k - 1)
                    c += ((1<< u_k) - 1);
                }
                else // then c is in the interval [ 2^(k-1) + 1  ...  2^k ]
                {
                    // so we translate c into the interval [ 2^(k-1) ...  + 2^k - 1 ] by subtracting 1
                    c -= 1;
                }
                if (u_k <= u_bits_high) // for small k we code the interval in one step
                {
                    // compress c with the range coder
                    enc.encodeSymbol(mCorrector[u_k], c);
                }
                else // for larger k we need to code the interval in two steps
                {
                    // figure out how many lower bits there are
                    int k1 = u_k - u_bits_high;
                    // c1 represents the lowest k-bits_high+1 bits
                    u_c1 = c & ((1<<k1) - 1);
                    // c represents the highest bits_high bits
                    c = c >> k1;
                    // compress the higher bits using a context table
                    enc.encodeSymbol(mCorrector[u_k], c);
                    // store the lower k1 bits raw
                    enc.writeBits(k1, u_c1);
                }
            }
        }
        else // then c is 0 or 1
        {
            assert((c == 0) || (c == 1));
            enc.encodeBit(mCorrector0,c);
        }
    }

    int readCorrector(ArithmeticModel mBits)
    {
        int c;

        // decode within which interval the corrector is falling

        u_k = dec.decodeSymbol(mBits);

        // decode the exact location of the corrector within the interval

        // TODO: original code: if (k)
        // TODO: how can k be zero or one?
        if (u_k != 0) // then c is either smaller than 0 or bigger than 1
        {
            if (compareUnsigned(u_k, 32) < 0)
            {
                if (compareUnsigned(u_k, u_bits_high) <= 0) // for small k we can do this in one step
                {
                    // decompress c with the range coder
                    c = dec.decodeSymbol(mCorrector[u_k]);
                }
                else
                {
                    // for larger k we need to do this in two steps
                    int k1 = u_k - u_bits_high;
                    // decompress higher bits with table
                    c = dec.decodeSymbol(mCorrector[u_k]);
                    // read lower bits raw
                    int c1 = dec.readBits(k1);
                    // put the corrector back together
                    c = (c << k1) | c1;
                }
                // translate c back into its correct interval
                if (c >= (1<<(u_k -1))) // if c is in the interval [ 2^(k-1)  ...  + 2^k - 1 ]
                {
                    // so we translate c back into the interval [ 2^(k-1) + 1  ...  2^k ] by adding 1
                    c += 1;
                }
                else // otherwise c is in the interval [ 0 ...  + 2^(k-1) - 1 ]
                {
                    // so we translate c back into the interval [ - (2^k - 1)  ...  - (2^(k-1)) ] by subtracting (2^k - 1)
                    c -= ((1<< u_k) - 1);
                }
            }
            else
            {
                c = corr_min;
            }
        }
        else // then c is either 0 or 1
        {
            c = dec.decodeBit(mCorrector0);
        }

        return c;
    }
}
