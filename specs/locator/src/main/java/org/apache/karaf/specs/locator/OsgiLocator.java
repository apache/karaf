/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.karaf.specs.locator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class OsgiLocator {

    public static final long DEFAULT_TIMEOUT = 0L;
    public static final String TIMEOUT = "org.apache.karaf.specs.timeout";

    private static final Map<String, List<Callable<Class>>> FACTORIES = new HashMap<>();
    
    private static final ReadWriteLock LOCK = new ReentrantReadWriteLock();

    private OsgiLocator() {
    }

    public static void unregister(String id, Callable<Class> factory) {
        LOCK.writeLock().lock();
        try {
            List<Callable<Class>> l = FACTORIES.get(id);
            if (l != null) {
                l.remove(factory);
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    public static void register(String id, Callable<Class> factory) {
        LOCK.writeLock().lock();
        try {
            FACTORIES.computeIfAbsent(id, k -> new ArrayList<>())
                    .add(0, factory);
            synchronized (LOCK) {
                LOCK.notifyAll();
            }
        } finally {
            LOCK.writeLock().unlock();
        }
    }


    public static <T> Class<T> locate(Class<T> factoryId) {
        return locate(factoryId, factoryId.getName());
    }

    private static long getTimeout() {
        long timeout = DEFAULT_TIMEOUT;
        try {
            String prop = System.getProperty(TIMEOUT);
            if (prop != null) {
                timeout = Long.parseLong(prop);
            }
        } catch (Throwable t) { }
        return timeout;
    }

    public static <T> Class<T> locate(Class<T> factoryClass, String factoryId) {
        long timeout = getTimeout();
        if (timeout <= 0) {
            return doLocate(factoryClass, factoryId);
        }
        long t0 = System.currentTimeMillis();
        long t1 = t0;
        while (t1 - t0 < timeout) {
            Class<T> impl = doLocate(factoryClass, factoryId);
            if (impl != null) {
                return impl;
            }
            synchronized (LOCK) {
                try {
                    LOCK.wait(timeout - (t1 - t0));
                } catch (InterruptedException e) {
                    return null;
                }
            }
            t1 = System.currentTimeMillis();
        }
        return null;
    }

    private static <T> Class<T> doLocate(Class<T> factoryClass, String factoryId) {
        LOCK.readLock().lock();
        try {
            List<Callable<Class>> l = FACTORIES.get(factoryId);
            if (l != null && !l.isEmpty()) {
                // look up the System property first
                String factoryClassName = System.getProperty(factoryId);
                try {
                    for (Callable<Class> i : l) {
                        Class c = null;
                        try {
                            c = i.call();
                        } catch (Exception ex) {
                            // do nothing here
                        }
                        if (c != null && factoryClass == c.getClassLoader().loadClass(factoryClass.getName())
                                 && (factoryClassName == null || c.getName().equals(factoryClassName)))
                        {
                            return c;
                        }
                    }
                } catch (Exception ex) {
                    // do nothing here
                }
            }
            return null;
        } finally {
            LOCK.readLock().unlock();
        }
    }

    public static <T> List<Class<? extends T>> locateAll(Class<T> factoryId) {
        return locateAll(factoryId, factoryId.getName());
    }

    public static <T> List<Class<? extends T>> locateAll(Class<T> factoryClass, String factoryId) {
        LOCK.readLock().lock();
        try {
            List<Class<? extends T>> classes = new ArrayList<>();
            List<Callable<Class>> l = FACTORIES.get(factoryId);
            if (l != null) {
                for (Callable<Class> i : l) {
                    try {
                        Class c = i.call();
                        if (c != null && factoryClass.isAssignableFrom(c)) {
                            classes.add(c);
                        }
                    } catch (Exception e) {
                    }
                }
            }
            return classes;
        } finally {
            LOCK.readLock().unlock();
        }
    }

}
