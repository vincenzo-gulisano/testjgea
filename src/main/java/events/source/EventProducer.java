package events.source;

import events.BaseEvent;
import events.GenericEvent;
import grammar.utils.CSVTypesExtractor;
import grammar.types.DataTypesEnum;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
// import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EventProducer {

    public static DataStream<BaseEvent> generateEventDataStreamFromCSV(StreamExecutionEnvironment env, String csvFilePath) {
        List<BaseEvent> events = new ArrayList<>();

        try {
            // Use CSVReader to get column types
            Map<String, DataTypesEnum> columnTypes = CSVTypesExtractor.getColumnTypesFromCSV(csvFilePath);

            try (Reader reader = new FileReader(csvFilePath);
                 CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())) {

                for (CSVRecord record : csvParser) {
                    Long timestamp;
                    try {
                        timestamp = Long.parseLong(record.get("timestamp"));
                    } catch (NumberFormatException e) {
                        System.err.println("Failed to parse 'timestamp' as Long with value: " + record.get("timestamp"));
                        continue;
                    }

                    GenericEvent event = new GenericEvent(timestamp);

                    // Parse other attributes without "timestamp"
                    for (Map.Entry<String, DataTypesEnum> entry : columnTypes.entrySet()) {
                        String column = entry.getKey();

                        // Skip the "timestamp" field since it's already parsed
                        if ("timestamp".equals(column)) {
                            continue;
                        }

                        String type = String.valueOf(entry.getValue());
                        String value = record.get(column);

                        try {
                            switch (type) {
                                case "INT" -> event.setAttribute(column, Integer.parseInt(value));
                                case "FLOAT" -> event.setAttribute(column, Float.parseFloat(value));
                                case "LONG" -> event.setAttribute(column, Long.parseLong(value));
                                case "BOOLEAN" -> event.setAttribute(column, Boolean.parseBoolean(value));
                                case "STRING" -> event.setAttribute(column, value);
                                default -> throw new IllegalArgumentException("Unsupported data type: " + type);
                            }
                        } catch (NumberFormatException nfe) {
                            System.err.println("Failed to parse attribute " + column + " as " + type + " with value " + value);
                        }
                    }
                    events.add(event);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("Generated " + events.size() + " events from CSV.");

        if (events.isEmpty()) {
            throw new IllegalArgumentException("Event list is empty. Ensure the CSV file is not empty and has correct data.");
        }

        return env.fromData(events);
    }
}
