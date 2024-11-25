package problem;

import events.BaseEvent;
import events.source.EventProducer;
import fitness.utils.EventSequenceMatcher;
import fitness.utils.ScoreCalculator;
import fitness.utils.TargetSequenceReader;
import io.github.ericmedvet.jgea.core.problem.TotalOrderQualityBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.GrammarBasedProblem;
import io.github.ericmedvet.jgea.core.representation.grammar.string.StringGrammar;
import io.github.ericmedvet.jgea.core.representation.tree.Tree;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import representation.PatternRepresentation;
import representation.mappers.RepresentationToPatternMapper;
import representation.mappers.TreeToRepresentationMapper;
import grammar.GrammarGenerator;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;

public class PatternInferenceProblem implements GrammarBasedProblem<String, PatternRepresentation>, TotalOrderQualityBasedProblem<PatternRepresentation, Double> {
    private final Set<List<Map<String, Object>>> targetExtractions;
    private final DataStream<BaseEvent> eventStream;
    private final StringGrammar<String> grammar;

    public PatternInferenceProblem(String configPath) throws Exception {
        // Load configuration properties with path validation
        Properties config = loadConfig(configPath);
        String datasetDirPath = getRequiredProperty(config, "datasetDirPath");
        String csvFilePath = datasetDirPath + getRequiredProperty(config, "csvFileName");

        // Initialize Flink environment and load events from CSV
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        this.eventStream = EventProducer.generateEventDataStreamFromCSV(env, csvFilePath);

        // Load target sequences for fitness evaluation
        TargetSequenceReader targetSequenceReader = new TargetSequenceReader();
        String targetDatasetPath = getRequiredProperty(config, "targetDatasetPath");
        this.targetExtractions = targetSequenceReader.readTargetSequencesFromFile(targetDatasetPath);

        // Generate and load grammar from CSV
        String grammarFilePath = getRequiredProperty(config, "grammarDirPath") + getRequiredProperty(config, "grammarFileName");
        GrammarGenerator.generateGrammar(csvFilePath, grammarFilePath);
        this.grammar = loadGrammar(grammarFilePath);
    }

    @Override
    public Comparator<Double> totalOrderComparator() {
        return (v1, v2) -> Double.compare(v2, v1);
    }

    @Override
    public Function<PatternRepresentation, Double> qualityFunction() {
        return patternRepresentation -> {
            try {
                // Transform PatternRepresentation to Flink CEP Pattern
                Pattern<BaseEvent, ?> generatedPattern = new RepresentationToPatternMapper<BaseEvent>().convert(patternRepresentation);
                PatternRepresentation.KeyByClause keyByClause = patternRepresentation.keyByClause();

                // Use EventSequenceMatcher to get detected sequences, now passing keyByClause
                Set<List<Map<String, Object>>> detectedSequences = EventSequenceMatcher.collectSequenceMatches(eventStream, List.of(generatedPattern), "Generated", keyByClause);

                // Compute fitness score
                return ScoreCalculator.calculateFitnessScore(targetExtractions, detectedSequences, keyByClause);
            } catch (Exception e) {
                e.printStackTrace();
                return 0.0;
            }
        };
    }


    @Override
    public StringGrammar<String> getGrammar() {
        return grammar;
    }

    @Override
    public Function<Tree<String>, PatternRepresentation> getSolutionMapper() {
        return new TreeToRepresentationMapper();
    }

    private static Properties loadConfig(String filePath) throws Exception {
        Properties config = new Properties();
        try (InputStream input = new FileInputStream(filePath)) {
            config.load(input);
        }
        return config;
    }

    // Helper method to retrieve required properties
    private static String getRequiredProperty(Properties config, String propertyName) throws IllegalArgumentException {
        String value = config.getProperty(propertyName);
        if (value == null) {
            throw new IllegalArgumentException("Missing required configuration property: " + propertyName);
        }
        return value;
    }

    private static StringGrammar<String> loadGrammar(String filePath) throws Exception {
        try (InputStream grammarStream = new FileInputStream(filePath)) {
            return StringGrammar.load(grammarStream);
        }
    }
}
