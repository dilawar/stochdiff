/**
 * NeuroRD — Stochastic reaction-diffusion simulator
 *
 * This software is licensed under the GNU Publice License, version 2
 * or any later, at your option.
 */

package neurord;

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import neurord.model.SDRun;
import neurord.xml.ModelReader.XMLUnmarshallingFailure;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Level;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;

import neurord.util.CustomFileAppender;
import neurord.util.Settings;
import neurord.util.Logging;

public class StochDiff {
    static final Logger log = LogManager.getLogger();

    // The main method - a bit of basic checking and if all is well, create the
    // SDCalc object and run it;

    public static void help_exit(Options options, boolean error) {
        String header =
            "\nwhere the <model> is an XML specification of the model to run. "
            + "The optional <output> specifies where the results should be stored "
            + "(w/o extension). When not supplied, <output> defaults to <model> "
            + "without the extension.\n\n";

        HelpFormatter formatter = new HelpFormatter();
        PrintWriter pw = new PrintWriter(error ? System.err : System.out);
        int columns =
            Math.max(Math.min(Settings.getEnvironmentVariable("COLUMNS", 80),
                              120),
                     20);
        formatter.printHelp(pw,
                            columns,
                            Settings.javaExecutable(StochDiff.class) + " [option...] <model> [<output>]",
                            header,
                            options,
                            HelpFormatter.DEFAULT_LEFT_PAD,
                            HelpFormatter.DEFAULT_DESC_PAD,
                            "");
        pw.flush();

        if (!error) {
            System.out.println();
            Settings.printAvailableSettings(System.out);
        }

        System.exit(error ? 1 : 0);
    }

    static Options buildOptions() {
        Options options = new Options();

        options.addOption(null, "version", false, "print version string and exit");

        options.addOption("t", "runtime", true, "override simulation time");
        options.addOption("i", "ic", true, "output file to take the initial conditions from");
        options.addOption(null, "ic-trial", true, "trial to take the seed from (default: 0)");
        options.addOption(null, "ic-time", true, "time to take the ICs from (default: none)");

        options.addOption(null, "log", true, "log file name (\"no\" to disable)");
        options.addOption("v", "verbose", false, "increase log level");
        options.addOption("s", "statistics", true, "override statistics gathering " +
                          "({none|injections|by-channel|by-event}[:interval])");

        Option property = OptionBuilder.withArgName("property=value")
            .hasArgs(2)
            .withValueSeparator()
            .withDescription("set java property")
            .create("D");
        options.addOption(property);

        return options;
    }

    public static void main(String[] argv) throws Exception {
        File modelFile, outputFile;

        CommandLineParser parser = new DefaultParser();
        Options options = buildOptions();

        List<String> args = Arrays.asList(argv);
        boolean help_requested = args.contains("-h") || args.contains("--help");
        if (help_requested || argv.length == 0)
            help_exit(options, !help_requested);

        CommandLine cmd = parser.parse(options, argv);
        Settings.augmentProperties(cmd.getOptionProperties("D"));

        if (cmd.hasOption("version")) {
            System.out.println(Settings.getProgramVersion());
            System.exit(0);
        }

        final int verbose = Settings.optionCount(cmd, argv, "verbose", "v");
        if (verbose > 0) {
            Level level = verbose == 1 ? Level.INFO : Level.DEBUG;
            Logging.setLogLevel(null, LogManager.ROOT_LOGGER_NAME, level);
        }

        Logging.configureConsoleLogging();

        argv = cmd.getArgs();
        if (argv.length == 0) {
            log.fatal("at least one argument is required");
            System.exit(1);
        }

        modelFile = new File(argv[0]);
        if (!modelFile.exists()) {
            log.fatal("no such file: {}", modelFile);
            System.exit(2);
        }

        final File unsuffixed;
        if (argv[0].indexOf(".") > 0)
            unsuffixed = new File(argv[0].substring(0, argv[0].lastIndexOf(".")));
        else
            unsuffixed = new File(argv[0]);

        if (argv.length > 1) {
            if (argv[1].endsWith(".h5") ||
                argv[1].endsWith(".txt") ||
                argv[1].endsWith(".log"))
                outputFile = new File(argv[1].substring(0, argv[1].lastIndexOf(".")));
            else
                outputFile = new File(argv[1]);
            if (outputFile.isDirectory())
                outputFile = new File(outputFile, unsuffixed.getName());
        } else
              outputFile = unsuffixed;

        final String logfile = cmd.getOptionValue("log", outputFile + ".log");
        Logging.configureFileLogging(logfile);

        /* Write out the version, after creating the log appenders. */
        log.info("{}", Settings.getProgramVersion());

        if (!logfile.equals("no"))
            log.info("Writing logs to {}", logfile);

        final File ic_file = Settings.getOption(cmd, "ic", null);
        final int ic_trial = Settings.getOption(cmd, "ic-trial", 0);
        final double ic_time = Settings.getOption(cmd, "ic-time", Double.NaN);

        SDRun model = null;
        try {
            model = SDRun.loadFromFile(modelFile, ic_file, ic_trial, ic_time);
        } catch(XMLUnmarshallingFailure e) {
            System.exit(2);
        }

        double runtime = Settings.getOption(cmd, "runtime", Double.NaN);
        if (!Double.isNaN(runtime))
            model.overrideRuntime(runtime);

        String statistics = cmd.getOptionValue("statistics");
        if (statistics != null) {
            Double interval = null;
            String[] parts = statistics.split(":", 2);
            if (parts.length > 1) {
                statistics = parts[0];
                interval = Double.valueOf(parts[1]);
            }

            model.overrideStatistics(statistics, interval);
        }

        SDCalc calc = new SDCalc(model, outputFile);
        int ret = calc.run();

        CustomFileAppender.close();
        System.exit(ret);
    }
}
