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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseActivator implements BundleActivator, Runnable, ThreadFactory {

    private static final Pattern STRING_ARRAY_SPLITTER = Pattern.compile("\\s*,\\s*");

    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected BundleContext bundleContext;

    protected ExecutorService executor = new ThreadPoolExecutor(0, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), this);
    private final AtomicBoolean scheduled = new AtomicBoolean();

    private long schedulerStopTimeout = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);

    private final Queue<ServiceRegistration<?>> registrations = new ConcurrentLinkedQueue<>();
    private final Map<String, SingleServiceTracker<?>> trackers = new HashMap<>();
    private ServiceRegistration<?> managedServiceRegistration;
    private volatile Dictionary<String, ?> configuration;

    /** What the activator has been started with, {@code null} if it is not started. */
    private volatile State started;
    /** What {@link #doStart()} is currently working with, {@code null} outside of {@link #doStart()}. */
    private volatile State starting;

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
        State state = new State();
        if (managedServiceRegistration == null && state.hasAllServices()) {
            try {
                doStart(state);
            } catch (Throwable e) {
                logger.warn("Error starting activator", e);
                started = null;
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
        if (!executor.awaitTermination(schedulerStopTimeout, TimeUnit.MILLISECONDS) && schedulerStopTimeout > 0) {
            logger.warn("Executor did not terminate within {} milliseconds", schedulerStopTimeout);
        }
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
        trackers.values().forEach(SingleServiceTracker::close);
    }

    protected void doStart() throws Exception {
    }

    protected void doStop() {
        ServiceRegistration<?> reg;
        while ((reg = registrations.poll()) != null) {
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
                State state = starting;
                if (state != null) {
                    state.configuration = properties;
                }
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

    /**
     * Implements method from {@link org.osgi.service.cm.ManagedService} without implementing the actual interface
     * @see org.osgi.service.cm.ManagedService#updated(Dictionary)
     */
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

    protected String[] getStringArray(String configKey, String defaultValue) {
        Object value = null;
        if (configuration != null) {
            value = configuration.get(configKey);
        }
        if (value == null) {
            value = defaultValue;
        }
        if (value == null) {
            return null;
        }
        if (value instanceof String[]) {
            return (String[]) value;
        } else if (value instanceof Iterable) {
            return StreamSupport.stream(((Iterable<?>) value).spliterator(), false)
                    .map(Object::toString).toArray(String[]::new);
        } else {
            return STRING_ARRAY_SPLITTER.split(value.toString());
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
        // The state is read after the flag has been reset: a change happening from now on
        // schedules another run, and a change that happened before is part of this state.
        State state = new State();
        if (state.sameAs(started)) {
            // The change this run has been scheduled for was already visible to the previous
            // start, so there is nothing to do. Restarting would needlessly unregister, and
            // register again, everything that has just been started.
            return;
        }
        doStop();
        try {
            doStart(state);
        } catch (Exception e) {
            logger.warn("Error starting activator", e);
            started = null;
            doStop();
        }
    }

    private void doStart(State state) throws Exception {
        started = state;
        starting = state;
        try {
            doStart();
        } finally {
            starting = null;
        }
    }

    /**
     * The configuration and the tracked services at a given point in time.
     * <p>
     * {@link #doStart()} works on such a state rather than on the trackers, so that
     * what the activator has been started with is known for sure.
     */
    private final class State {

        private volatile Dictionary<String, ?> configuration = BaseActivator.this.configuration;
        private final Map<String, ServiceReference<?>> references = new HashMap<>();
        private final Map<String, Object> services = new HashMap<>();

        State() {
            for (Map.Entry<String, SingleServiceTracker<?>> entry : trackers.entrySet()) {
                references.put(entry.getKey(), entry.getValue().getServiceReference());
                services.put(entry.getKey(), entry.getValue().getService());
            }
        }

        boolean hasAllServices() {
            return services.values().stream().allMatch(Objects::nonNull);
        }

        boolean sameAs(State other) {
            if (other == null || configuration != other.configuration) {
                return false;
            }
            for (String name : services.keySet()) {
                if (services.get(name) != other.services.get(name)
                        || !Objects.equals(references.get(name), other.references.get(name))) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Called in {@link #doOpen()}.
     *
     * @param clazz The service interface to track.
     * @throws InvalidSyntaxException If the tracker syntax is not correct.
     */
    protected void trackService(Class<?> clazz) throws InvalidSyntaxException {
        trackServiceInternal(clazz.getName(), null);
    }

    /**
     * Called in {@link #doOpen()}.
     *
     * @param clazz The service interface to track.
     * @param filter The filter to use to select the services to track.
     * @throws InvalidSyntaxException If the tracker syntax is not correct (in the filter especially).
     */
    protected void trackService(Class<?> clazz, String filter) throws InvalidSyntaxException {
        trackServiceInternal(clazz.getName(), filter);
    }

    protected void trackService(String className, String filter) throws InvalidSyntaxException {
        trackServiceInternal(className, filter);
    }

    private void trackServiceInternal(String className, String filter) throws InvalidSyntaxException {
        if (!trackers.containsKey(className)) {
            SingleServiceTracker<?> tracker = new SingleServiceTracker<>(bundleContext, className, filter, (u, v) -> reconfigure());
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
        SingleServiceTracker<?> tracker = trackers.get(clazz.getName());
        if (tracker == null) {
            throw new IllegalStateException("Service not tracked for class " + clazz);
        }
        State state = starting;
        return clazz.cast(state != null ? state.services.get(clazz.getName()) : tracker.getService());
    }

    @SuppressWarnings("unchecked")
    protected <T> ServiceReference<T> getTrackedServiceRef(Class<T> clazz) {
        SingleServiceTracker<?> tracker = trackers.get(clazz.getName());
        if (tracker == null) {
            throw new IllegalStateException("Service not tracked for class " + clazz);
        }
        State state = starting;
        return (ServiceReference<T>) (state != null
            ? state.references.get(clazz.getName())
            : tracker.getServiceReference());
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
    protected void register(Class<?>[] clazz, Object service) {
        register(clazz, service, null);
    }

    /**
     * Called in {@link #doStart()}.
     *
     * @param clazz The service interfaces to register.
     * @param service The actual service instance to register.
     * @param props The service properties to register.
     */
    protected void register(Class<?>[] clazz, Object service, Dictionary<String, ?> props) {
        String[] names = new String[clazz.length];
        for (int i = 0; i < clazz.length; i++) {
            names[i] = clazz[i].getName();
        }
        trackRegistration(bundleContext.registerService(names, service, props));
    }

    private void trackRegistration(ServiceRegistration<?> registration) {
        registrations.add(registration);
    }

    protected String[] getInterfaceNames(Object object) {
        Set<String> names = new LinkedHashSet<>();
        for (Class<?> cl = object.getClass(); cl != Object.class; cl = cl.getSuperclass()) {
            addSuperInterfaces(names, cl);
        }
        return names.toArray(new String[0]);
    }

    private void addSuperInterfaces(Set<String> names, Class<?> clazz) {
        for (Class<?> cl : clazz.getInterfaces()) {
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
