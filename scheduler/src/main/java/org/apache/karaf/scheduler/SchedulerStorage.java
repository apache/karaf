package org.apache.karaf.scheduler;

import java.io.Serializable;

public interface SchedulerStorage {

    <T> T get(final Serializable key);

    void put(final Serializable key, final Object value);

    boolean contains(final Serializable key);

    void release(final Serializable key);

}
