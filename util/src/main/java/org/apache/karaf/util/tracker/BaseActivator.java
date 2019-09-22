/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.util.tracker;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseActivator implements BundleActivator, Runnable, ThreadFactory {

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected BundleContext bundleContext;

    protected ExecutorService executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), this);
    private AtomicBoolean scheduled = new AtomicBoolean();

    private long schedulerStopTimeout = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    private final Queue<ServiceRegistration> registrations = new ConcurrentLinkedQueue<>();
    private Map<String, SingleServiceTracker> trackers = new HashMap<>();
    private ServiceRegistration managedServiceRegistration;
    private Dictionary<String, ?> configuration;

    private static final AtomicInteger poolNumber = new AtomicInteger(1);
    private final ThreadGroup group;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final String namePrefix;

    public BaseActivator() {
        SecurityManager s = System.getSecurityManager();
        group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        namePrefix = "activator-" + poolNumber.getAndIncrement() + "-thread-";
    }

    public long getSchedulerStopTimeout() {
        return schedulerStopTimeout;
    }

    public void setSchedulerStopTimeout(long schedulerStopTimeout) {
        this.schedulerStopTimeout = schedulerStopTimeout;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        scheduled.set(true);
        doOpen();
        scheduled.set(false);
        if (managedServiceRegistration == null
                && trackers.values().stream()
                    .allMatch(t -> t.getService() != null)) {
            try {
                doStart();
            } catch (Throwable e) {
                logger.warn("Error starting activator", e);
                doStop();
            }
        } else {
            reconfigure();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        scheduled.set(true);
        doClose();
        executor.shutdown();
        executor.awaitTermination(schedulerStopTimeout, TimeUnit.MILLISECONDS);
        doStop();
    }

    protected void doOpen() throws Exception {
        URL data = bundleContext.getBundle().getResource("OSGI-INF/karaf-tracker/" + getClass().getName());
        if (data != null) {
            Properties props = new Properties();
            try (InputStream is = data.openStream()) {
                props.load(is);
            }
            for (String key : props.stringPropertyNames()) {
                if ("pid".equals(key)) {
                    manage(props.getProperty(key));
                } else {
                    trackService(key, props.getProperty(key));
                }
            }
        }
    }

    protected void doClose() {
        if (managedServiceRegistration != null) {
            managedServiceRegistration.unregister();
        }
        for (SingleServiceTracker tracker : trackers.values()) {
            tracker.close();
        }
    }

    protected void doStart() throws Exception {
    }

    protected void doStop() {
        while (true) {
            ServiceRegistration reg = registrations.poll();
            if (reg == null) {
                break;
            }
            reg.unregister();
        }
    }

    protected boolean ensureStartupConfiguration(String configId) throws IOException {
        if (this.configuration != null) {
            return true;
        }
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin != null) {
            Configuration configuration = configurationAdmin.getConfiguration(configId);
            Dictionary<String, Object> properties = (configuration == null) ? null : configuration.getProperties();

            if (properties != null) {
                this.configuration = properties;
                return true;
            }
        }
        return false;
    }

    /**
     * Called in {@link #doOpen()}.
     *
     * @param pid The configuration PID to manage (ManagedService).
     */
    protected void manage(String pid) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, pid);
        managedServiceRegistration = bundleContext.registerService(
                "org.osgi.service.cm.ManagedService", this, props);
    }

    public void updated(Dictionary<String, ?> properties) {
        this.configuration = properties;
        reconfigure();
    }

    protected Dictionary<String, ?> getConfiguration() {
        return configuration;
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param key The configuration key
     * @param def The default value.
     * @return The value of the configuration key if found, the default value else.
     */
    protected int getInt(String key, int def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                return Integer.parseInt(val.toString());
            }
        }
        return def;
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param key The configuration key.
     * @param def The default value.
     * @return The value of the configuration key if found, the default value else.
     */
    protected boolean getBoolean(String key, boolean def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val instanceof Boolean) {
                return (Boolean) val;
            } else if (val != null) {
                return Boolean.parseBoolean(val.toString());
            }
        }
        return def;
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param key The configuration key.
     * @param def The default value.
     * @return The value of the configuration key if found, the default value else.
     */
    protected long getLong(String key, long def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val instanceof Number) {
                return ((Number) val).longValue();
            } else if (val != null) {
                return Long.parseLong(val.toString());
            }
        }
        return def;
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param key The configuration key.
     * @param def The default value.
     * @return The value of the configuration key if found, the default value else.
     */
    protected String getString(String key, String def) {
        if (configuration != null) {
            Object val = configuration.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
    }

    protected Class<?>[] getClassesArray(String key, String def) {
        final String[] stringArray = getStringArray(key, def);
        if (stringArray == null) {
            return null;
        }
        final ClassLoader loader = Thread.currentThread().getContextClassLoader();
        return Stream.of(stringArray)
                .map(it -> {
                    try {
                        return loader.loadClass(it.trim());
                    } catch (final ClassNotFoundException e) {
                        throw new IllegalArgumentException(e);
                    }
                })
                .toArray(Class[]::new);
    }

    protected String[] getStringArray(String key, String def) {
        Object val = null;
        if (configuration != null) {
            val = configuration.get(key);
        }
        if (val == null) {
            val = def;
        }
        if (val == null) {
            return null;
        }
        Stream<String> s;
        if (val instanceof String[]) {
            return (String[]) val;
        } else if (val instanceof Iterable) {
            return StreamSupport.stream(((Iterable<?>) val).spliterator(), false)
                    .map(Object::toString).toArray(String[]::new);
        } else {
            return val.toString().split("\\s*,\\s*");
        }
    }

    protected void reconfigure() {
        if (scheduled.compareAndSet(false, true)) {
            executor.submit(this);
        }
    }

    @Override
    public void run() {
        scheduled.set(false);
        doStop();
        try {
            doStart();
        } catch (Exception e) {
            logger.warn("Error starting activator", e);
            doStop();
        }
    }

    /**
     * Called in {@link #doOpen()}.
     *
     * @param clazz The service interface to track.
     * @throws InvalidSyntaxException If the tracker syntax is not correct.
     */
    protected void trackService(Class<?> clazz) throws InvalidSyntaxException {
        if (!trackers.containsKey(clazz.getName())) {
            SingleServiceTracker tracker = new SingleServiceTracker<>(bundleContext, clazz, (u, v) -> reconfigure());
            tracker.open();
            trackers.put(clazz.getName(), tracker);
        }
    }

    /**
     * Called in {@link #doOpen()}.
     *
     * @param clazz The service interface to track.
     * @param filter The filter to use to select the services to track.
     * @throws InvalidSyntaxException If the tracker syntax is not correct (in the filter especially).
     */
    protected void trackService(Class<?> clazz, String filter) throws InvalidSyntaxException {
        if (!trackers.containsKey(clazz.getName())) {
            if (filter != null && filter.isEmpty()) {
                filter = null;
            }
            SingleServiceTracker tracker = new SingleServiceTracker<>(bundleContext, clazz, filter, (u, v) -> reconfigure());
            tracker.open();
            trackers.put(clazz.getName(), tracker);
        }
    }

    protected void trackService(String className, String filter) throws InvalidSyntaxException {
        if (!trackers.containsKey(className)) {
            SingleServiceTracker tracker = new SingleServiceTracker<>(bundleContext, className, filter, (u, v) -> reconfigure());
            tracker.open();
            trackers.put(className, tracker);
        }
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param clazz The service interface to get.
     * @param <T> The service type.
     * @return The actual tracker service object.
     */
    protected <T> T getTrackedService(Class<T> clazz) {
        SingleServiceTracker tracker = trackers.get(clazz.getName());
        if (tracker == null) {
            throw new IllegalStateException("Service not tracked for class " + clazz);
        }
        return clazz.cast(tracker.getService());
    }

    protected <T> ServiceReference<T> getTrackedServiceRef(Class<T> clazz) {
        SingleServiceTracker tracker = trackers.get(clazz.getName());
        if (tracker == null) {
            throw new IllegalStateException("Service not tracked for class " + clazz);
        }
        return tracker.getServiceReference();
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param mbean The MBean to register.
     * @param type The MBean type to register.
     */
    protected void registerMBean(Object mbean, String type) {
        String name = "org.apache.karaf:" + type + ",name=" + System.getProperty("karaf.name");
        registerMBeanWithName(mbean, name);
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param mbean The MBean to register.
     * @param name The MBean name.
     */
    protected void registerMBeanWithName(Object mbean, String name) {
        Hashtable<String, Object> props = new Hashtable<>();
        props.put("jmx.objectname", name);
        trackRegistration(bundleContext.registerService(getInterfaceNames(mbean), mbean, props));
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param clazz The service interface to register.
     * @param <T> The service type.
     * @param service The actual service instance to register.
     */
    protected <T> void register(Class<T> clazz, T service) {
        register(clazz, service, null);
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param clazz The service interface to register.
     * @param <T> The service type.
     * @param service The actual service instance to register.
     * @param props The service properties to register.
     */
    protected <T> void register(Class<T> clazz, T service, Dictionary<String, ?> props) {
        trackRegistration(bundleContext.registerService(clazz, service, props));
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param clazz The service interfaces to register.
     * @param service The actual service instance to register.
     */
    protected void register(Class[] clazz, Object service) {
        register(clazz, service, null);
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param clazz The service interfaces to register.
     * @param service The actual service instance to register.
     * @param props The service properties to register.
     */
    protected void register(Class[] clazz, Object service, Dictionary<String, ?> props) {
        String[] names = new String[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            names[i] = clazz[i].getName();
        }
        trackRegistration(bundleContext.registerService(names, service, props));
    }

    private void trackRegistration(ServiceRegistration registration) {
        registrations.add(registration);
    }

    protected String[] getInterfaceNames(Object object) {
        List<String> names = new ArrayList<>();
        for (Class cl = object.getClass(); cl != Object.class; cl = cl.getSuperclass()) {
            addSuperInterfaces(names, cl);
        }
        return names.toArray(new String[names.size()]);
    }

    private void addSuperInterfaces(List<String> names, Class clazz) {
        for (Class cl : clazz.getInterfaces()) {
            names.add(cl.getName());
            addSuperInterfaces(names, cl);
        }
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }

}
