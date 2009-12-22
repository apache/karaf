package samples.registerservice;

public interface Store {
    public void put(String key, Object value);
    public Object get(String key);
}
