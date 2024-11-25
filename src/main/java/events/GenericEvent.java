package events;

import java.util.HashMap;
import java.util.Map;

public class GenericEvent extends BaseEvent {
    private final Map<String, Object> attributes;

    public GenericEvent(Long timestamp) {
        super(timestamp);
        this.attributes = new HashMap<>();
    }

    // Adds a new attribute to the event
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    // Returns a map of all attributes including the timestamp
    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>(attributes);
        map.put("timestamp", timestamp);
        return map;
    }

    // Gets a specific attribute by key
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    // Override toString for easier debugging
    @Override
    public String toString() {
        return "FlexibleEvent{" +
                "timestamp=" + timestamp +
                ", attributes=" + attributes +
                '}';
    }
}
