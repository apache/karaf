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
package org.apache.karaf.audit;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import org.apache.karaf.audit.layout.GelfLayout;
import org.apache.karaf.audit.layout.Rfc3164Layout;
import org.apache.karaf.audit.layout.Rfc5424Layout;
import org.apache.karaf.audit.layout.SimpleLayout;
import org.apache.karaf.audit.logger.FileEventLogger;
import org.apache.karaf.audit.logger.JulEventLogger;
import org.apache.karaf.audit.logger.UdpEventLogger;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.Managed;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;

import javax.security.auth.Subject;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

@Services(requires = @RequireService(EventAdmin.class))
@Managed("org.apache.karaf.audit")
public class Activator extends BaseActivator implements ManagedService {

    public static final String FILTER = "filter";
    public static final String QUEUE_TYPE = "queue.type";
    public static final String QUEUE_SIZE = "queue.size";
    public static final String RUNNER_IDLE_TIMEOUT = "runner.idle-timeout";
    public static final String RUNNER_FLUSH_TIMEOUT = "runner.flush-timeout";
    public static final String FILE_PREFIX = "file.";
    public static final String FILE_LAYOUT = FILE_PREFIX + "layout";
    public static final String FILE_ENABLED = FILE_PREFIX + "enabled";
    public static final String FILE_TARGET = FILE_PREFIX + "target";
    public static final String FILE_ENCODING = FILE_PREFIX + "encoding";
    public static final String FILE_POLICY = FILE_PREFIX + "policy";
    public static final String FILE_FILES = FILE_PREFIX + "files";
    public static final String FILE_COMPRESS = FILE_PREFIX + "compress";
    public static final String UDP_PREFIX = "udp.";
    public static final String UDP_LAYOUT = UDP_PREFIX + "layout";
    public static final String UDP_ENABLED = UDP_PREFIX + "enabled";
    public static final String UDP_HOST = UDP_PREFIX + "host";
    public static final String UDP_PORT = UDP_PREFIX + "port";
    public static final String UDP_ENCODING = UDP_PREFIX + "encoding";
    public static final String TCP_PREFIX = "tcp.";
    public static final String TCP_LAYOUT = TCP_PREFIX + "layout";
    public static final String TCP_ENABLED = TCP_PREFIX + "enabled";
    public static final String TCP_HOST = TCP_PREFIX + "host";
    public static final String TCP_PORT = TCP_PREFIX + "port";
    public static final String TCP_ENCODING = TCP_PREFIX + "encoding";
    public static final String JUL_PREFIX = "jul.";
    public static final String JUL_LAYOUT = JUL_PREFIX + "layout";
    public static final String JUL_ENABLED = JUL_PREFIX + "enabled";
    public static final String JUL_LOGGER = JUL_PREFIX + "logger";
    public static final String JUL_LEVEL = JUL_PREFIX + "level";
    public static final String TOPICS = "topics";

    private static final EventImpl STOP_EVENT = new EventImpl(new Event("stop", Collections.emptyMap()));


    private BlockingQueue<EventImpl> queue;
    private volatile Thread runner;
    private List<EventLogger> eventLoggers;
    private Filter filter;

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        queue = createQueue();
        eventLoggers = createLoggers();
        filter = createFilter();
        final Dictionary<String, Object> props = new Hashtable<>();
        props.put(EventConstants.EVENT_TOPIC, getTopics());
        register(EventHandler.class, this::handleEvent, props);
        if (!queue.isEmpty()) {
            startRunner();
        }
    }

    private String[] getTopics() {
        return getString(TOPICS, "*").split("\\s*,\\s*");
    }

    private Filter createFilter() throws InvalidSyntaxException {
        String str = getString(FILTER, null);
        return str != null ? FrameworkUtil.createFilter(str) : null;
    }

    @SuppressWarnings("unchecked")
    private BlockingQueue<EventImpl> createQueue() throws Exception {
        String type = getString(QUEUE_TYPE, null);
        int size = getInt(QUEUE_SIZE, 1024);
        if ("ArrayBlockingQueue".equals(type)) {
            return new ArrayBlockingQueue<>(size);
        } else if ("DisruptorBlockingQueue".equals(type)) {
            return new DisruptorBlockingQueue(size);
        } else if (type != null) {
            logger.warn("Unknown queue type: " + type + "");
        }
        try {
            return new DisruptorBlockingQueue(size);
        } catch (NoClassDefFoundError t) {
            return new ArrayBlockingQueue<>(size);
        }
    }

    private List<EventLogger> createLoggers() throws Exception {
        try {
            List<EventLogger> loggers = new ArrayList<>();
            if (getBoolean(FILE_ENABLED, true)) {
                String path = getString(FILE_TARGET, System.getProperty("karaf.data") + "/log/audit.txt");
                String encoding = getString(FILE_ENCODING, "UTF-8");
                String policy = getString(FILE_POLICY, "size(8mb)");
                int files = getInt(FILE_FILES, 32);
                boolean compress = getBoolean(FILE_COMPRESS, true);
                EventLayout layout = createLayout(getString(FILE_LAYOUT, FILE_LAYOUT));
                loggers.add(new FileEventLogger(path, encoding, policy, files, compress, this, layout, TimeZone.getDefault()));
            }
            if (getBoolean(UDP_ENABLED, false)) {
                String host = getString(UDP_HOST, "localhost");
                int port = getInt(UDP_PORT, 514);
                String encoding = getString(UDP_ENCODING, "UTF-8");
                EventLayout layout = createLayout(getString(UDP_LAYOUT, UDP_LAYOUT));
                loggers.add(new UdpEventLogger(host, port, encoding, layout));
            }
            if (getBoolean(TCP_ENABLED, false)) {
                String host = getString(TCP_HOST, "localhost");
                int port = getInt(TCP_PORT, 0);
                String encoding = getString(TCP_ENCODING, "UTF-8");
                EventLayout layout = createLayout(getString(TCP_LAYOUT, TCP_LAYOUT));
                loggers.add(new UdpEventLogger(host, port, encoding, layout));
            }
            if (getBoolean(JUL_ENABLED, false)) {
                String logger = getString(Activator.JUL_LOGGER, "audit");
                String level = getString(Activator.JUL_LEVEL, "info");
                EventLayout layout = createLayout(getString(JUL_LAYOUT, JUL_LAYOUT));
                loggers.add(new JulEventLogger(logger, level, layout));
            }
            return loggers;
        } catch (IOException e) {
            throw new Exception("Error creating audit logger", e);
        }
    }

    private EventLayout createLayout(String prefix) {
        String type = getString(prefix + ".type", "simple");
        switch (type) {
            case "simple":
                return new SimpleLayout();
            case "rfc3164":
                return new Rfc3164Layout(getInt(prefix + ".facility", 16),
                        getInt(prefix + ".priority", 5),
                        getInt(prefix + ".enterprise", Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER),
                        TimeZone.getDefault(),
                        Locale.ENGLISH);
            case "rfc5424":
                return new Rfc5424Layout(getInt(prefix + ".facility", 16),
                                         getInt(prefix + ".priority", 5),
                                         getInt(prefix + ".enterprise", Rfc5424Layout.DEFAULT_ENTERPRISE_NUMBER),
                                         TimeZone.getDefault());
            case "gelf":
                return new GelfLayout();
            default:
                try {
                    return createCustomLayout(type);
                } catch (Exception e) {
                    logger.error("Error creating layout: " + type + ". Using a simple layout.", e);
                    return new SimpleLayout();
                }
        }
    }

    private EventLayout createCustomLayout(String type) throws ClassNotFoundException, InstantiationException, IllegalAccessException, java.lang.reflect.InvocationTargetException {
        Class<?> clazz = Class.forName(type);
        Constructor<?> cnsMap = null;
        Constructor<?> cnsDef = null;
        Object layout = null;
        try {
            cnsMap = clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException e) {
            // ignore
        }
        try {
            cnsDef = clazz.getConstructor();
        } catch (NoSuchMethodException e) {
            // ignore
        }
        if (cnsMap != null) {
            Map<String, Object> params = new HashMap<>();
            for (Enumeration<String> e = getConfiguration().keys(); e.hasMoreElements(); ) {
                String key = e.nextElement();
                Object val = getConfiguration().get(key);
                params.put(key, val);
            }
            layout = cnsMap.newInstance(params);
        } else if (cnsDef != null) {
            layout = cnsDef.newInstance();
        } else {
            throw new IllegalArgumentException("Unable to find a supported constructor");
        }
        if (layout instanceof EventLayout) {
            return (EventLayout) layout;
        } else {
            throw new IllegalArgumentException("The built layout does not implement " + EventLayout.class.getName());
        }
    }

    @Override
    protected void doStop() {
        Thread runner = this.runner;
        if (runner != null && runner.isAlive()) {
            try {
                queue.add(STOP_EVENT);
                runner.join(5000);
                if (runner.isAlive()) {
                    runner.interrupt();
                }
            } catch (InterruptedException e) {
                logger.debug("Error waiting for audit runner buffer stop");
            }
        }
        List<EventLogger> eventLoggers = this.eventLoggers;
        if (eventLoggers != null) {
            for (EventLogger eventLogger : eventLoggers) {
                try {
                    eventLogger.close();
                } catch (IOException e) {
                    logger.debug("Error closing audit logger", e);
                }
            }
            this.eventLoggers = null;
        }
        super.doStop();
    }

    private void handleEvent(Event event) {
        try {
            EventImpl ev = new EventImpl(event);
            if (filter == null || filter.matches(ev.getFilterMap())) {
                queue.put(new EventImpl(event));
                startRunner();
            }
        } catch (InterruptedException e) {
            logger.debug("Interrupted while putting event in queue", e);
        }
    }

    private void startRunner() {
        if (eventLoggers != null && !eventLoggers.isEmpty() && runner == null) {
            synchronized (this) {
                if (runner == null) {
                    runner = new Thread(this::consume, "audit-logger");
                    runner.start();
                }
            }
        }
    }

    private void consume() {
        long maxIdle = getLong(RUNNER_IDLE_TIMEOUT, TimeUnit.MINUTES.toMillis(1));
        long flushDelay = getLong(RUNNER_FLUSH_TIMEOUT, TimeUnit.MILLISECONDS.toMillis(100));
        try {
            List<EventLogger> eventLoggers = this.eventLoggers;
            BlockingQueue<EventImpl> queue = this.queue;
            EventImpl event;
            while ((event = queue.poll(maxIdle, TimeUnit.MILLISECONDS)) != null) {
                if (event == STOP_EVENT) {
                    return;
                }
                for (EventLogger eventLogger : eventLoggers) {
                    eventLogger.write(event);
                }
                if (flushDelay > 0) {
                    while ((event = queue.poll(flushDelay, TimeUnit.MILLISECONDS)) != null) {
                        if (event == STOP_EVENT) {
                            return;
                        }
                        for (EventLogger eventLogger : eventLoggers) {
                            eventLogger.write(event);
                        }
                    }
                }
                for (EventLogger eventLogger : eventLoggers) {
                    eventLogger.flush();
                }
            }
        } catch (Throwable e) {
            logger.warn("Error writing audit log", e);
        } finally {
            runner = null;
        }
    }

    static class EventImpl implements org.apache.karaf.audit.Event {
        private final Event event;
        private final long timestamp;
        private final String type;
        private final String subtype;

        EventImpl(Event event) {
            this.event = event;
            this.timestamp = _timestamp();
            this.type = _type();
            this.subtype = _subtype();
        }

        @Override
        public long timestamp() {
            return timestamp;
        }

        private long _timestamp() {
            Long l = (Long) event.getProperty("timestamp");
            return l != null ? l : System.currentTimeMillis();
        }

        @Override
        public Subject subject() {
            return (Subject) event.getProperty("subject");
        }

        @Override
        public String type() {
            return type;
        }

        private String _type() {
            switch (event.getTopic()) {
                case "org/apache/karaf/shell/console/EXECUTED":
                    return TYPE_SHELL;
                case "org/osgi/service/log/LogEntry/LOG_ERROR":
                case "org/osgi/service/log/LogEntry/LOG_WARNING":
                case "org/osgi/service/log/LogEntry/LOG_INFO":
                case "org/osgi/service/log/LogEntry/LOG_DEBUG":
                case "org/osgi/service/log/LogEntry/LOG_OTHER":
                    return TYPE_LOG;
                case "org/osgi/framework/ServiceEvent/REGISTERED":
                case "org/osgi/framework/ServiceEvent/MODIFIED":
                case "org/osgi/framework/ServiceEvent/UNREGISTERING":
                    return TYPE_SERVICE;
                case "org/osgi/framework/BundleEvent/INSTALLED":
                case "org/osgi/framework/BundleEvent/STARTED":
                case "org/osgi/framework/BundleEvent/STOPPED":
                case "org/osgi/framework/BundleEvent/UPDATED":
                case "org/osgi/framework/BundleEvent/UNINSTALLED":
                case "org/osgi/framework/BundleEvent/RESOLVED":
                case "org/osgi/framework/BundleEvent/UNRESOLVED":
                case "org/osgi/framework/BundleEvent/STARTING":
                case "org/osgi/framework/BundleEvent/STOPPING":
                    return TYPE_BUNDLE;
                case "org/apache/karaf/login/ATTEMPT":
                case "org/apache/karaf/login/SUCCESS":
                case "org/apache/karaf/login/FAILURE":
                case "org/apache/karaf/login/LOGOUT":
                    return TYPE_LOGIN;
                case "javax/management/MBeanServer/CREATEMBEAN":
                case "javax/management/MBeanServer/REGISTERMBEAN":
                case "javax/management/MBeanServer/UNREGISTERMBEAN":
                case "javax/management/MBeanServer/GETOBJECTINSTANCE":
                case "javax/management/MBeanServer/QUERYMBEANS":
                case "javax/management/MBeanServer/ISREGISTERED":
                case "javax/management/MBeanServer/GETMBEANCOUNT":
                case "javax/management/MBeanServer/GETATTRIBUTE":
                case "javax/management/MBeanServer/GETATTRIBUTES":
                case "javax/management/MBeanServer/SETATTRIBUTE":
                case "javax/management/MBeanServer/SETATTRIBUTES":
                case "javax/management/MBeanServer/INVOKE":
                case "javax/management/MBeanServer/GETDEFAULTDOMAIN":
                case "javax/management/MBeanServer/GETDOMAINS":
                case "javax/management/MBeanServer/ADDNOTIFICATIONLISTENER":
                case "javax/management/MBeanServer/GETMBEANINFO":
                case "javax/management/MBeanServer/ISINSTANCEOF":
                case "javax/management/MBeanServer/INSTANTIATE":
                case "javax/management/MBeanServer/DESERIALIZE":
                case "javax/management/MBeanServer/GETCLASSLOADERFOR":
                case "javax/management/MBeanServer/GETCLASSLOADER":
                    return TYPE_JMX;
                case "org/osgi/framework/FrameworkEvent/STARTED":
                case "org/osgi/framework/FrameworkEvent/ERROR":
                case "org/osgi/framework/FrameworkEvent/PACKAGES_REFRESHED":
                case "org/osgi/framework/FrameworkEvent/STARTLEVEL_CHANGED":
                case "org/osgi/framework/FrameworkEvent/WARNING":
                case "org/osgi/framework/FrameworkEvent/INFO":
                case "org/osgi/framework/FrameworkEvent/STOPPED":
                case "org/osgi/framework/FrameworkEvent/STOPPED_UPDATE":
                case "org/osgi/framework/FrameworkEvent/STOPPED_BOOTCLASSPATH_MODIFIED":
                case "org/osgi/framework/FrameworkEvent/WAIT_TIMEDOUT":
                    return TYPE_FRAMEWORK;
                case "org/osgi/service/web/DEPLOYING":
                case "org/osgi/service/web/DEPLOYED":
                case "org/osgi/service/web/UNDEPLOYING":
                case "org/osgi/service/web/UNDEPLOYED":
                    return TYPE_WEB;
                case "org/apache/karaf/features/repositories/ADDED":
                case "org/apache/karaf/features/repositories/REMOVED":
                    return TYPE_REPOSITORIES;
                case "org/apache/karaf/features/features/INSTALLED":
                case "org/apache/karaf/features/features/UNINSTALLED":
                    return TYPE_FEATURES;
                case "org/osgi/service/blueprint/container/CREATING":
                case "org/osgi/service/blueprint/container/CREATED":
                case "org/osgi/service/blueprint/container/DESTROYING":
                case "org/osgi/service/blueprint/container/DESTROYED":
                case "org/osgi/service/blueprint/container/FAILURE":
                case "org/osgi/service/blueprint/container/GRACE_PERIOD":
                case "org/osgi/service/blueprint/container/WAITING":
                    return TYPE_BLUEPRINT;
                default:
                    return TYPE_UNKNOWN;
            }
        }

        @Override
        public String subtype() {
            return subtype;
        }

        private String _subtype() {
            String topic = event.getTopic();
            String subtype = topic.substring(topic.lastIndexOf('/') + 1).toLowerCase(Locale.ENGLISH);
            if (subtype.startsWith("log_")) {
                subtype = subtype.substring("log_".length());
            }
            return subtype;
        }

        @Override
        public Iterable<String> keys() {
            String[] keys = event.getPropertyNames();
            Arrays.sort(keys);
            return () -> new Iterator<String>() {
                String next;
                int index = -1;
                @Override
                public boolean hasNext() {
                    if (next != null) {
                        return true;
                    }
                    while (++index < keys.length) {
                        switch (keys[index]) {
                            case "timestamp":
                            case "event.topics":
                            case "subject":
                            case "type":
                            case "subtype":
                                break;
                            default:
                                next = keys[index];
                                return true;
                        }
                    }
                    return false;
                }
                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    String str = next;
                    next = null;
                    return str;
                }
            };
        }

        @Override
        public Object getProperty(String key) {
            return event.getProperty(key);
        }

        Map<String, Object> getFilterMap() {
            return new AbstractMap<String, Object>() {
                @Override
                public Set<Entry<String, Object>> entrySet() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public Object get(Object key) {
                    String s = key.toString();
                    switch (s) {
                        case "timestamp":
                            return timestamp();
                        case "type":
                            return type();
                        case "subtype":
                            return subtype();
                        case "subject":
                            return subject();
                        default:
                            return event.getProperty(s);
                    }
                }
            };
        }

    }

}
