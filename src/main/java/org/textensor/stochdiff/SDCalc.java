package org.textensor.stochdiff;

import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.textensor.stochdiff.model.SDRun;
import org.textensor.stochdiff.numeric.BaseCalc;
import org.textensor.stochdiff.numeric.morph.VolumeGrid;
import org.textensor.stochdiff.numeric.grid.ResultWriter;
import org.textensor.stochdiff.numeric.grid.ResultWriterText;
import org.textensor.stochdiff.numeric.grid.ResultWriterHDF5;

import org.textensor.util.Settings;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class SDCalc {
    static final Logger log = LogManager.getLogger();

    final SDRun sdRun;

    final String[] writers = Settings.getPropertyList("stochdiff.writers", "text");
    final int trials = Settings.getProperty("stochdiff.trials", 1);
    final int threads = Settings.getProperty("stochdiff.threads", 0);

    protected final List<ResultWriter> resultWriters = new ArrayList<>();

    public SDCalc(SDRun sdr, File output) {
        this.sdRun = sdr;

        if (trials > 1 && sdr.simulationSeed > 0) {
            log.warn("Ignoring fixed simulation seed");
            sdr.simulationSeed = 0;
        }

        for (String type: writers) {
            final ResultWriter writer;
            final VolumeGrid grid = sdr.getVolumeGrid();
            final String[] species = sdr.getSpecies();
            if (type.equals("text")) {
                writer = new ResultWriterText(output, sdr, sdr.getOutputSets(), species, grid, false);
                log.info("Using text writer for {}", writer.outputFile());
            } else if (type.equals("h5")) {
                writer = new ResultWriterHDF5(output, sdr, sdr.getOutputSets(), species, grid);
                log.info("Using HDF5 writer for {}", writer.outputFile());
            } else {
                log.error("Unknown writer '{}'", type);
                throw new RuntimeException("uknown writer: " + type);
            }
            this.resultWriters.add(writer);
        }

        //        if (sdRun.continueOutput() && outputFile.exists() && sdRun.getStartTime() > 0)
        //            resultWriter.pruneFrom("gridConcentrations", 3, sdRun.getStartTime());
    }

    protected BaseCalc prepareCalc(int trial) {
        SDCalcType calculationType = SDCalcType.valueOf(this.sdRun.calculation);
        BaseCalc calc = calculationType.getCalc(trial, this.sdRun);
        for (ResultWriter resultWriter: this.resultWriters)
                calc.addResultWriter(resultWriter);
        return calc;
    }

    public void run() {
        log.info("Beginning calculations ({} trials)", this.trials);

        if (trials == 1)
            this.prepareCalc(0).run();
        else {
            int poolSize = threads > 0 ? threads : Runtime.getRuntime().availableProcessors();
            ExecutorService pool = Executors.newFixedThreadPool(poolSize);
            log.info("Running with pool {}", pool);

            for (int i = 0; i < trials; i++) {
                log.info("Starting trial {}", i);
                pool.execute(this.prepareCalc(i));
            }

            log.info("Executing shutdown of pool {}", pool);
            pool.shutdown();
            while (true)
                try {
                    pool.awaitTermination(1, TimeUnit.MINUTES);
                    return;
                } catch(InterruptedException e) {
                    log.info("Waiting: {}", pool);
                }
        }
    }
}
