package org.apache.karaf.scheduler.core;

import org.apache.karaf.scheduler.SchedulerStorage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class QuartzSchedulerStorage implements SchedulerStorage {

    private final Map<Serializable, Object> store = new HashMap<>();

    @Override
    public <T> T get(final Serializable key) {
        return (T) this.store.get(key);
    }

    @Override
    public void put(final Serializable key, final Object value) {
        this.store.put(key, value);
    }

    @Override
    public boolean contains(final Serializable key) {
        return this.store.containsKey(key);
    }

    @Override
    public void release(final Serializable key) {
        this.store.remove(key);
    }
}
