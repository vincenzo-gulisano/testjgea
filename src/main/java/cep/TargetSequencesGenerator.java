package cep;

import events.BaseEvent;
import events.source.EventProducer;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.cep.pattern.conditions.SimpleCondition;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamUtils;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.*;
import java.util.*;

public class TargetSequencesGenerator {

    private static String targetDatasetPath;
    private static String keyByField;  // Field to store the key for keyBy operation

    public static List<Pattern<BaseEvent, ?>> createTargetPatterns() {
        List<Pattern<BaseEvent, ?>> targetPatterns = new ArrayList<>();

        Pattern<BaseEvent, BaseEvent> pattern1 = Pattern
                .<BaseEvent>begin("successful_login_true")
                .where(new SimpleCondition<BaseEvent>() {
                    @Override
                    public boolean filter(BaseEvent event) {
                        Object loginStatus = event.toMap().get("successful_login");
                        return Boolean.TRUE.equals(loginStatus);
                    }
                })
                .next("ip_address_129_16_0_30")
                .where(new SimpleCondition<BaseEvent>() {
                    @Override
                    public boolean filter(BaseEvent event) {
                        Object ipAddress = event.toMap().get("ip_address");
                        return "129.16.0.30".equals(ipAddress);
                    }
                });

        Pattern<BaseEvent, BaseEvent> pattern2 = Pattern
                .<BaseEvent>begin("ip_address_129_16_0_5_false")
                .where(new SimpleCondition<BaseEvent>() {
                    @Override
                    public boolean filter(BaseEvent event) {
                        Object ipAddress = event.toMap().get("ip_address");
                        Object loginStatus = event.toMap().get("successful_login");
                        return "129.16.0.5".equals(ipAddress) && Boolean.FALSE.equals(loginStatus);
                    }
                })
                .followedBy("timestamp_check")
                .where(new SimpleCondition<BaseEvent>() {
                    @Override
                    public boolean filter(BaseEvent event) {
                        Object timestamp = event.toMap().get("timestamp");
                        return timestamp instanceof Number && ((Number) timestamp).longValue() > 1724057899441646L;
                    }
                });


        // Add all patterns to the list
        targetPatterns.add(pattern1);
        targetPatterns.add(pattern2);

        return targetPatterns;
    }

    // Apply patterns to the DataStream and save matches to a file
    public static void saveMatchesToFile(List<Pattern<BaseEvent, ?>> patterns, DataStream<BaseEvent> inputDataStream) throws Exception {
        Set<List<Map<String, Object>>> sequencesSet = new HashSet<>();

        // Apply keyBy if keyByField is specified
        DataStream<BaseEvent> streamToUse;
        if (keyByField != null && !keyByField.isEmpty()) {
            streamToUse = inputDataStream.keyBy(event -> event.toMap().get(keyByField));
        } else {
            streamToUse = inputDataStream;  // No keyBy is applied
        }

        try (FileWriter writer = new FileWriter(targetDatasetPath)) {
            for (Pattern<BaseEvent, ?> pattern : patterns) {
                PatternStream<BaseEvent> patternStream = CEP.pattern(streamToUse, pattern);
                DataStream<List<BaseEvent>> matchedStream = patternStream.select(new PatternToListSelectFunction());

                Iterator<List<BaseEvent>> iterator = DataStreamUtils.collect(matchedStream);
                while (iterator.hasNext()) {
                    List<BaseEvent> eventsList = iterator.next();

                    // Convert the list of events into a list of maps for easier comparison
                    List<Map<String, Object>> sequence = new ArrayList<>();
                    for (BaseEvent event : eventsList) {
                        sequence.add(new HashMap<>(event.toMap()));
                    }

                    sequencesSet.add(sequence);
                    System.out.println("[Target] match sequence: " + sequence);

                    // Write the sequence to the CSV file
                    writer.write(sequenceToCsvLine(sequence) + "\n");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String sequenceToCsvLine(List<Map<String, Object>> sequence) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> map : sequence) {
            builder.append(map.toString().replace(",", ";")).append("|");
        }
        return builder.toString();
    }

    private static class PatternToListSelectFunction implements PatternSelectFunction<BaseEvent, List<BaseEvent>> {
        @Override
        public List<BaseEvent> select(Map<String, List<BaseEvent>> match) {
            List<BaseEvent> collectedEvents = new ArrayList<>();
            match.values().forEach(collectedEvents::addAll);
            return collectedEvents;
        }
    }

    public static void main(String[] args) throws Exception {
        // Load configuration properties from config.properties file
        Properties config = loadConfig("config.properties");

        // Read paths from the configuration
        String datasetDirPath = config.getProperty("datasetDirPath");
        String csvFileName = config.getProperty("csvFileName");
        targetDatasetPath = config.getProperty("targetDatasetPath", "Flink-cep-examples-main/src/main/resources/datasets/target/targetDataset.csv");

        // Load keyBy field from configuration
        keyByField = config.getProperty("targetKeyByField", null);

        String csvFilePath = datasetDirPath + csvFileName;

        // Set up Flink environment and load events from CSV
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        DataStream<BaseEvent> eventStream = EventProducer.generateEventDataStreamFromCSV(env, csvFilePath);

        // Create target patterns and save matches to a file
        List<Pattern<BaseEvent, ?>> targetPatterns = createTargetPatterns();
        saveMatchesToFile(targetPatterns, eventStream);
    }

    private static Properties loadConfig(String filePath) throws Exception {
        Properties config = new Properties();
        try (InputStream input = TargetSequencesGenerator.class.getClassLoader().getResourceAsStream(filePath)) {
            if (input == null) {
                throw new FileNotFoundException("Configuration file not found: " + filePath);
            }
            config.load(input);
        }
        return config;
    }
}
