package app;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.time.Instant;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import io.github.ericmedvet.jgea.experimenter.Experiment;
import io.github.ericmedvet.jgea.experimenter.Experimenter;
import io.github.ericmedvet.jgea.experimenter.Starter;
import io.github.ericmedvet.jnb.core.BuilderException;
import io.github.ericmedvet.jnb.core.NamedBuilder;
import io.github.ericmedvet.jnb.core.NamedParamMap;
import io.github.ericmedvet.jnb.core.ParamMap;
import io.github.ericmedvet.jnb.core.parsing.StringParser;

import java.util.logging.Logger;

public class MyStarter {

    private static final Logger L = Logger.getLogger(Starter.class.getName());

    public static void main(String[] args) {
        // read configuration
        Starter.Configuration configuration = new Starter.Configuration();
        JCommander jc = JCommander.newBuilder().addObject(configuration).build();
        jc.setProgramName(Starter.class.getName());
        try {
            jc.parse(args);
        } catch (ParameterException e) {
            e.usage();
            L.severe(String.format("Cannot read command line options: %s", e));
            System.exit(-1);
        } catch (RuntimeException e) {
            L.severe(e.getClass().getSimpleName() + ": " + e.getMessage());
            System.exit(-1);
        }
        // check help
        if (configuration.help) {
            jc.usage();
            return;
        }
        // prepare local named builder
        NamedBuilder<Object> nb = NamedBuilder.fromDiscovery();
        // check if it's just a help invocation
        if (configuration.showExpFileHelp) {
            System.out.println(NamedBuilder.prettyToString(nb, true));
            return;
        }
        // read experiment description
        String expDescription = null;
        if (configuration.experimentDescriptionFilePath.isEmpty()
                && !configuration.exampleExperimentDescriptionResourceName.isEmpty()) {
            L.config("Using example experiment description: %s"
                    .formatted(configuration.exampleExperimentDescriptionResourceName));
            InputStream inputStream = Starter.class.getResourceAsStream(
                    "/exp-examples/%s.txt".formatted(configuration.exampleExperimentDescriptionResourceName));
            if (inputStream == null) {
                L.severe("Cannot find default experiment description: %s"
                        .formatted(configuration.exampleExperimentDescriptionResourceName));
            } else {
                try {
                    expDescription = new String(inputStream.readAllBytes());
                } catch (IOException e) {
                    L.severe("Cannot read default experiment description: %s".formatted(e));
                }
            }
        } else if (!configuration.experimentDescriptionFilePath.isEmpty()) {
            L.config(String.format(
                    "Using provided experiment description: %s", configuration.experimentDescriptionFilePath));
            try {
                expDescription = Files.readString(Path.of(configuration.experimentDescriptionFilePath));
            } catch (IOException e) {
                L.severe("Cannot read provided experiment description at %s: %s"
                        .formatted(configuration.experimentDescriptionFilePath, e));
            }
        }
        if (expDescription == null) {
            L.info("No experiment provided");
            System.exit(-1);
        }
        // parse and add name
        Experiment experiment = (Experiment) nb.build(expDescription);
        if (experiment.name().isEmpty()) {
            Path path = Path.of(
                    configuration.experimentDescriptionFilePath.isEmpty()
                            ? configuration.exampleExperimentDescriptionResourceName
                            : configuration.experimentDescriptionFilePath);
            NamedParamMap expNPM = StringParser.parse(expDescription)
                    .with("name", ParamMap.Type.STRING, path.getFileName().toString())
                    .with(
                            "startTime",
                            ParamMap.Type.STRING,
                            "%1$tY-%1$tm-%1$td--%1$tH-%1$tM-%1$tS"
                                    .formatted(Instant.now().toEpochMilli()));
            experiment = (Experiment) nb.build(expNPM);
        }
        // check if just check
        if (configuration.check) {
            try {
                System.out.println("Experiment description is valid");
                System.out.printf("\t%d runs%n", experiment.runs().size());
                System.out.printf("\t%d listeners%n", experiment.listeners().size());
                return;
            } catch (BuilderException e) {
                L.severe("Cannot build experiment: %s%n".formatted(e));
                if (configuration.verbose) {
                    // noinspection CallToPrintStackTrace
                    e.printStackTrace();
                }
                System.exit(-1);
            }
        }
        // prepare and run experimenter
        try {
            L.info("Running experiment '%s' with %d runs and %d listeners"
                    .formatted(
                            experiment.name(),
                            experiment.runs().size(),
                            experiment.listeners().size()));
            Experimenter experimenter = new Experimenter(configuration.nOfConcurrentRuns, configuration.nOfThreads);
            experimenter.run(experiment, configuration.verbose);
        } catch (BuilderException e) {
            L.severe("Cannot run experiment: %s%n".formatted(e));
            if (configuration.verbose) {
                // noinspection CallToPrintStackTrace
                e.printStackTrace();
            }
            System.exit(-1);
        }
    }

}
