package org.textensor.stochdiff.numeric.math;

import java.util.Arrays;

import org.textensor.report.E;

/*
 * MersenneTwister based on http://cs.gmu.edu/~eclab/projects/ecj/
 *
 * Makato Matsumoto and Takuji Nishimura,
 * "Mersenne Twister: A 623-Dimensionally Equidistributed Uniform
 * Pseudo-Random Number Generator",
 * ACM Transactions on Modeling and Computer Simulation
 * Vol. 8, No. 1, January 1998, pp 3--30.
 */



public class MersenneTwister extends Derived implements RandomGenerator {

    // Period parameters
    private static final int N = 624;
    private static final int M = 397;
    private static final int MATRIX_A = 0x9908b0df;
    private static final int UPPER_MASK = 0x80000000; // most significant w-r
    // bits
    private static final int LOWER_MASK = 0x7fffffff; // least significant r
    // bits


    // Tempering parameters
    private static final int TEMPERING_MASK_B = 0x9d2c5680;
    private static final int TEMPERING_MASK_C = 0xefc60000;

    private int mt[]; // the array for the state vector
    private int mti; // mti==N+1 means mt[N] is not initialized
    private int mag01[];

    // a good initial seed (of int size, though stored in a long)
    // private static final long GOOD_SEED = 4357;

    public MersenneTwister() {
        this(System.currentTimeMillis());
    }


    public MersenneTwister(final long seed) {
        setSeed(seed);
    }



    public void setSeed(final long seed) {
        mt = new int[N];

        mag01 = new int[2];
        mag01[0] = 0x0;
        mag01[1] = MATRIX_A;

        mt[0] = (int)(seed & 0xffffffff);
        for (mti = 1; mti < N; mti++) {
            mt[mti] = (1812433253 * (mt[mti - 1] ^ (mt[mti - 1] >>> 30)) + mti);
            /* See Knuth TAOCP Vol2. 3rd Ed. P.106 for multiplier. */
            /* In the previous versions, MSBs of the seed affect */
            /* only MSBs of the array mt[]. */
            /* 2002/01/09 modified by Makoto Matsumoto */
            mt[mti] &= 0xffffffff;
            /* for >32 bit machines */
        }
    }

    @Override
    public final float random() {
        int y;

        // generate a block of words for use later;
        if (mti >= N) {
            int kk;

            // local copies are faster
            final int[] mtl = mt;
            final int[] mag01l = mag01;

            for (kk = 0; kk < N - M; kk++) {
                y = (mtl[kk] & UPPER_MASK) | (mtl[kk + 1] & LOWER_MASK);
                mtl[kk] = mtl[kk + M] ^ (y >>> 1) ^ mag01l[y & 0x1];
            }
            for (; kk < N - 1; kk++) {
                y = (mtl[kk] & UPPER_MASK) | (mtl[kk + 1] & LOWER_MASK);
                mtl[kk] = mtl[kk + (M - N)] ^ (y >>> 1) ^ mag01l[y & 0x1];
            }
            y = (mtl[N - 1] & UPPER_MASK) | (mtl[0] & LOWER_MASK);
            mtl[N - 1] = mtl[M - 1] ^ (y >>> 1) ^ mag01l[y & 0x1];

            mti = 0;
        }

        y = mt[mti++];
        y ^= y >>> 11; // TEMPERING_SHIFT_U(y)
        y ^= (y << 7) & TEMPERING_MASK_B; // TEMPERING_SHIFT_S(y)
        y ^= (y << 15) & TEMPERING_MASK_C; // TEMPERING_SHIFT_T(y)
        y ^= (y >>> 18); // TEMPERING_SHIFT_L(y)

        return (y >>> 8) / ((float)(1 << 24));
    }


    public static void main(String[] argv) {

        MersenneTwister mt = new MersenneTwister();

        long t1 = System.currentTimeMillis();


        int nran = 100000;
        double pavg = 1300.;


        double dg = 0.;
        for (int i = 0; i < nran; i++) {
            dg += mt.gaussian();
        }


        long t2 = System.currentTimeMillis();

        double dp = 0.;
        for (int i = 0; i < nran; i++) {
            dp += mt.poisson(pavg);
        }



        long t3 = System.currentTimeMillis();

        E.info("times: " + (t2 - t1) + " " + (t3 - t2) + " " + dg / nran + " " + dp / nran);
    }

}
