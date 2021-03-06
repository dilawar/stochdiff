package neurord.numeric.pool;

import neurord.model.SDRun;
import neurord.numeric.math.Matrix;
import neurord.numeric.math.Column;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/*
 * This evolves concentrations such that the concentration change is computed from the
 * concentrations AT THE END of the step. It uses a multidimensional Newton Raphson
 * method to iterate to the solution to the concentration increments to be applied.
 *
 * The good thing about this is that it handles stiff equations nicely: you can tak a timestep that
 *  is much bigger than the timescale of the fastest reaction. In particular, it handles an approach
 *  to equilibrium nicely where some rates are so fast that the reactions are always in equilibrium.
 *  You pay the price in accuracy if you use large timesteps, but that is not always so bad as it
 *  seems as you may not care about the detailed time evolution of rapidly reacting species.
 */



public class ImplicitEulerPoolCalc extends DeterministicPoolCalc {
    static final Logger log = LogManager.getLogger();

    int maxIterationsSoFar;



    public ImplicitEulerPoolCalc(int trial, SDRun sdm) {
        super(trial, sdm);
    }

    public void dpcInit() {
        maxIterationsSoFar = 0;
    }

    public double advance() {
        double tol = 1.e-7;

        Column c = mconc;
        Column dc = new Column(c.size());


        int iit = 0;
        double erhs = 0.;
        do {
            // fill rhs vector for step in conc dc, time dt;
            Column rhs = rtab.stepResiduals(c, dc, dt);

            erhs =rhs.avgAbs();

            iit++;
            if (erhs > tol) {
                if (iit > maxIterationsSoFar) {
                    maxIterationsSoFar = iit;
                }

                Matrix m = numderivs(c, dc, dt);
                Column w = m.LUSolve(rhs);
                dc.decrementBy(w);
            }
            if (iit > 12) {
                log.error("Failed to converge");
                break;
            }
        } while (erhs > 1.e-7);

        mconc.incrementBy(dc);

        return dt;
    }



    private final Matrix numderivs(Column vc, Column vdc,  double deltat) {
        int n = rtab.getNSpecies();
        Matrix m = new Matrix(n);

        double[] r0 = rtab.stepResiduals(vc, vdc, deltat).getData();
        double delta = 1.e-6;

        for (int j = 0; j < n; j++) {
            Column vdelta = vdc.copy();
            vdelta.increment(j, delta);

            double[] rd = rtab.stepResiduals(vc, vdelta, deltat).getData();

            for (int i = 0; i < n; i++) {
                m.set(i, j, (rd[i] - r0[i]) / delta);
            }
        }
        return m;
    }

    public long getParticleCount() {
        // TODO Auto-generated method stub
        return 0;
    }


}







