package org.apache.karaf.scheduler;

import java.io.Serializable;

public interface SchedulerStorage {

    public <T> T get(final Serializable key);

    public void put(final Serializable key, final Object value);

    public boolean contains(final Serializable key);

    public void release(final Serializable key);

}
