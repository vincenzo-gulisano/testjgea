package fitness.utils;

import representation.PatternRepresentation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ScoreCalculator {

    public static double calculateFitnessScore(Set<List<Map<String, Object>>> targetSequences, Set<List<Map<String, Object>>> detectedSequences, PatternRepresentation.KeyByClause keyByClause) {
        // Compare only sequences based on the key, if specified
        Set<List<Map<String, Object>>> keyedTargetSequences = keyByClause != null ? applyKeyByToSequences(targetSequences, keyByClause.key()) : targetSequences;
        Set<List<Map<String, Object>>> keyedDetectedSequences = keyByClause != null ? applyKeyByToSequences(detectedSequences, keyByClause.key()) : detectedSequences;

        long detectedTargetCount = keyedTargetSequences.stream()
                .filter(targetSeq -> keyedDetectedSequences.stream().anyMatch(detectedSeq -> compareSequences(targetSeq, detectedSeq)))
                .count();

        return keyedTargetSequences.isEmpty() ? 0.0 : (double) detectedTargetCount / keyedTargetSequences.size() * 100.0;
    }

    // Filters sequences to include only the specified key
    private static Set<List<Map<String, Object>>> applyKeyByToSequences(Set<List<Map<String, Object>>> sequences, String key) {
        return sequences.stream()
                .map(seq -> seq.stream()
                        .map(eventMap -> Map.of(key, eventMap.get(key)))  // Keep only key-value pairs for the specified key
                        .collect(Collectors.toList()))
                .collect(Collectors.toSet());
    }

    private static boolean compareSequences(List<Map<String, Object>> seq1, List<Map<String, Object>> seq2) {
        if (seq1.size() != seq2.size()) {
            return false;
        }
        for (int i = 0; i < seq1.size(); i++) {
            if (!canonicalizeMap(seq1.get(i)).equals(canonicalizeMap(seq2.get(i)))) {
                return false;
            }
        }
        return true;
    }

    // Creates a string representation of the map, sorted by keys, for comparison
    private static String canonicalizeMap(Map<String, Object> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .reduce((entry1, entry2) -> entry1 + ";" + entry2)
                .orElse("");
    }
}
