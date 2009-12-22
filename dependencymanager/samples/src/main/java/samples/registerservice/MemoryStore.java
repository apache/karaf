package samples.registerservice;

import java.util.HashMap;
import java.util.Map;

public class MemoryStore implements Store {
    private Map m_map = new HashMap();

    public Object get(String key) {
        return m_map.get(key);
    }

    public void put(String key, Object value) {
        m_map.put(key, value);
    }
}
