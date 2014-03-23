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
package org.apache.karaf.log.core.internal.osgi;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.NotCompliantMBeanException;

import org.apache.karaf.log.core.LogEventFormatter;
import org.apache.karaf.log.core.LogService;
import org.apache.karaf.log.core.internal.LogEventFormatterImpl;
import org.apache.karaf.log.core.internal.LogMBeanImpl;
import org.apache.karaf.log.core.internal.LogServiceImpl;
import org.apache.karaf.log.core.internal.LruList;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.ops4j.pax.logging.spi.PaxAppender;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, ManagedService, SingleServiceTracker.SingleServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AtomicBoolean scheduled = new AtomicBoolean();
    private BundleContext bundleContext;
    private Dictionary<String, ?> configuration;
    private ServiceRegistration managedServiceRegistration;
    private SingleServiceTracker<ConfigurationAdmin> configAdminTracker;
    private ServiceRegistration<LogService> serviceRegistration;
    private ServiceRegistration<LogEventFormatter> formatterRegistration;
    private ServiceRegistration<PaxAppender> appenderRegistration;
    private ServiceRegistration mbeanRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        scheduled.set(true);

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.log");
        managedServiceRegistration = bundleContext.registerService(ManagedService.class, this, props);

        configAdminTracker = new SingleServiceTracker<ConfigurationAdmin>(
                bundleContext, ConfigurationAdmin.class, this);
        configAdminTracker.open();

        scheduled.set(false);
        reconfigure();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        configAdminTracker.close();
        managedServiceRegistration.unregister();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        this.configuration = properties;
        reconfigure();
    }

    @Override
    public void serviceFound() {
        reconfigure();
    }

    @Override
    public void serviceLost() {
        reconfigure();
    }

    @Override
    public void serviceReplaced() {
        reconfigure();
    }

    protected void reconfigure() {
        if (scheduled.compareAndSet(false, true)) {
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    scheduled.set(false);
                    doStop();
                    try {
                        doStart();
                    } catch (Exception e) {
                        LOGGER.warn("Error starting management layer", e);
                        doStop();
                    }
                }
            });
        }
    }

    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = configAdminTracker != null ? configAdminTracker.getService() : null;
        Dictionary<String, ?> config = configuration;
        if (configurationAdmin == null) {
            return;
        }

        int size = getInt(config, "size", 500);
        String pattern = getString(config, "pattern", "%d{ABSOLUTE} | %-5.5p | %-16.16t | %-32.32c{1} | %-32.32C %4L | %m%n");
        String fatalColor = getString(config, "fatalColor", "31");
        String errorColor = getString(config, "errorColor", "31");
        String warnColor = getString(config, "warnColor", "35");
        String infoColor = getString(config, "infoColor", "36");
        String debugColor = getString(config, "debugColor", "39");
        String traceColor = getString(config, "traceColor", "39");

        LruList events = new LruList(size);
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put("org.ops4j.pax.logging.appender.name", "VmLogAppender");
        appenderRegistration = bundleContext.registerService(
                PaxAppender.class, events, props);

        LogEventFormatterImpl formatter = new LogEventFormatterImpl();
        formatter.setPattern(pattern);
        formatter.setFatalColor(fatalColor);
        formatter.setErrorColor(errorColor);
        formatter.setWarnColor(warnColor);
        formatter.setInfoColor(infoColor);
        formatter.setDebugColor(debugColor);
        formatter.setTraceColor(traceColor);
        formatterRegistration = bundleContext.registerService(
                LogEventFormatter.class, formatter, null);

        LogServiceImpl logService = new LogServiceImpl(configurationAdmin, events);
        serviceRegistration = bundleContext.registerService(
                LogService.class, logService, null);


        try {
            LogMBeanImpl securityMBean = new LogMBeanImpl(logService);
            props = new Hashtable<String, Object>();
            props.put("jmx.objectname", "org.apache.karaf:type=log,name=" + System.getProperty("karaf.name"));
            mbeanRegistration = bundleContext.registerService(
                    getInterfaceNames(securityMBean),
                    securityMBean,
                    props
            );
        } catch (NotCompliantMBeanException e) {
            LOGGER.warn("Error creating Log mbean", e);
        }
    }

    protected void doStop() {
        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            serviceRegistration = null;
        }
        if (formatterRegistration != null) {
            formatterRegistration.unregister();
            formatterRegistration = null;
        }
        if (appenderRegistration != null) {
            appenderRegistration.unregister();
            appenderRegistration = null;
        }
    }

    private String[] getInterfaceNames(Object object) {
        List<String> names = new ArrayList<String>();
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

    private int getInt(Dictionary<String, ?> config, String key, int def) {
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Number) {
                return ((Number) val).intValue();
            } else if (val != null) {
                return Integer.parseInt(val.toString());
            }
        }
        return def;
    }

    private String getString(Dictionary<String, ?> config, String key, String def) {
        if (config != null) {
            Object val = config.get(key);
            if (val != null) {
                return val.toString();
            }
        }
        return def;
    }

}
