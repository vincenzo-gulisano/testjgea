package fitness.utils;

import events.BaseEvent;
import org.apache.flink.cep.CEP;
import org.apache.flink.cep.PatternSelectFunction;
import org.apache.flink.cep.PatternStream;
import org.apache.flink.cep.pattern.Pattern;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamUtils;
import representation.PatternRepresentation;

import java.util.*;

public class EventSequenceMatcher {

    public static Set<List<Map<String, Object>>> collectSequenceMatches(DataStream<BaseEvent> inputDataStream, List<Pattern<BaseEvent, ?>> patterns, String type, PatternRepresentation.KeyByClause keyByClause) throws Exception {
        Set<List<Map<String, Object>>> sequencesSet = new HashSet<>();

        // Apply keyBy if keyByClause is specified
        DataStream<BaseEvent> keyedStream = (keyByClause != null && keyByClause.key() != null)
                ? inputDataStream.keyBy(event -> event.toMap().get(keyByClause.key()))
                : inputDataStream;
        
        for (Pattern<BaseEvent, ?> pattern : patterns) {
            DataStream<List<BaseEvent>> matchedStream = getMatchedDataStream(keyedStream, pattern);

            Iterator<List<BaseEvent>> iterator = DataStreamUtils.collect(matchedStream);
            while (iterator.hasNext()) {
                List<Map<String, Object>> sequence = new ArrayList<>();
                for (BaseEvent event : iterator.next()) {
                    sequence.add(new HashMap<>(event.toMap()));
                }
                sequencesSet.add(sequence);
                System.out.println("[" + type + "] match sequence: " + sequence);
            }
        }
        return sequencesSet;
    }

    // Creates a PatternStream from the keyed or non-keyed DataStream
    private static DataStream<List<BaseEvent>> getMatchedDataStream(DataStream<BaseEvent> inputDataStream, Pattern<BaseEvent, ?> pattern) {
        PatternStream<BaseEvent> patternStream = CEP.pattern(inputDataStream, pattern);
        return patternStream.select(new PatternToListSelectFunction());
    }

    private static class PatternToListSelectFunction implements PatternSelectFunction<BaseEvent, List<BaseEvent>> {
        @Override
        public List<BaseEvent> select(Map<String, List<BaseEvent>> match) {
            List<BaseEvent> collectedEvents = new ArrayList<>();
            match.values().forEach(collectedEvents::addAll);
            return collectedEvents;
        }
    }
}
