package representation;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

public record PatternRepresentation(
        List<Event> events,
        WithinClause withinClause,
        KeyByClause keyByClause
) implements Serializable {

    @Override
    public String toString() {
        String eventsStr = events.stream()
                .map(Event::toString)
                .collect(Collectors.joining(",\n  "));

        return "PatternRepresentation {\n" +
                "  events=[\n  " + eventsStr + "\n  ],\n" +
                "  withinClause=" + withinClause + "\n" +
                "  keyByClause=" + keyByClause + "\n" +
                "}";
    }

    public record Event(
            String identifier,
            List<Condition> conditions,
            Quantifier quantifier,
            Concatenator concatenator // Concatenator with respect to the next event
    ) implements Serializable {
        @Override
        public String toString() {
            String conditionsStr = conditions.stream()
                    .map(Condition::toString)
                    .collect(Collectors.joining(", "));

            return "Event {\n" +
                    "    identifier='" + identifier + "',\n" +
                    "    conditions=[" + conditionsStr + "],\n" +
                    "    quantifier=" + quantifier + ",\n" +
                    "    concatenator=" + concatenator + "\n" +
                    "  }";
        }

        public enum Concatenator implements Serializable {
            NEXT, FOLLOWED_BY, FOLLOWED_BY_ANY, NOT_NEXT, NOT_FOLLOWED_BY
        }
    }

    public record KeyByClause(String key) implements Serializable{
        @Override
        public String toString() {
            return "KeyByClause { key=" + key + " }";
        }
    }

    public record WithinClause(float duration) implements Serializable {
        @Override
        public String toString() {
            return "WithinClause { duration=" + duration + " }";
        }
    }

    public interface Quantifier extends Serializable {
        enum ParamFree implements Quantifier {
            ONE_OR_MORE, OPTIONAL
        }

        record NTimes(int n) implements Quantifier {
            @Override
            public String toString() {
                return "NTimes { n=" + n + " }";
            }
        }

        record FromToTimes(int from, int to) implements Quantifier {
            @Override
            public String toString() {
                return "FromToTimes { from=" + from + " to=" + to + " }";
            }
        }
    }

    public record Condition(
            String variable,
            Operator operator,
            Object value,
            Concatenator concatenator
    ) implements Serializable {
        @Override
        public String toString() {
            return "Condition { variable='" + variable + "', operator=" + operator + ", value=" + value +
                    (concatenator != null ? ", concatenator=" + concatenator : "") + " }";
        }

        public enum Operator implements Serializable {
            EQUAL, NOT_EQUAL, LESS_THAN, GREATER_THAN
        }

        public enum Concatenator implements Serializable {
            AND, OR
        }
    }
}