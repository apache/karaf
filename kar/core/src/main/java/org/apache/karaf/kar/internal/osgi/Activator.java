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
package org.apache.karaf.kar.internal.osgi;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.NotCompliantMBeanException;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.kar.KarService;
import org.apache.karaf.kar.internal.KarServiceImpl;
import org.apache.karaf.kar.internal.KarsMBeanImpl;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
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
    private ServiceRegistration<KarService> registration;
    private ServiceRegistration mbeanRegistration;
    private ServiceRegistration<ManagedService> managedServiceRegistration;
    private SingleServiceTracker<FeaturesService> featuresServiceTracker;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        scheduled.set(true);

        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.kar");
        managedServiceRegistration = bundleContext.registerService(ManagedService.class, this, props);

        featuresServiceTracker = new SingleServiceTracker<FeaturesService>(
                bundleContext, FeaturesService.class, this);
        featuresServiceTracker.open();

        scheduled.set(false);
        reconfigure();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        featuresServiceTracker.close();
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
        FeaturesService featuresService = featuresServiceTracker.getService();
        Dictionary<String, ?> config = configuration;
        if (featuresService == null) {
            return;
        }

        boolean noAutoRefreshBundles = getBoolean(config, "noAutoRefreshBundles", false);

        KarServiceImpl karService = new KarServiceImpl(
                System.getProperty("karaf.base"),
                featuresService
        );
        karService.setNoAutoRefreshBundles(noAutoRefreshBundles);
        registration = bundleContext.registerService(KarService.class, karService, null);

        try {
            KarsMBeanImpl mbean = new KarsMBeanImpl();
            mbean.setKarService(karService);
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("jmx.objectname", "org.apache.karaf:type=kar,name=" + System.getProperty("karaf.name"));
            mbeanRegistration = bundleContext.registerService(
                    getInterfaceNames(mbean),
                    mbean,
                    props
            );
        } catch (NotCompliantMBeanException e) {
            LOGGER.warn("Error creating Kars mbean", e);
        }
    }

    protected void doStop() {
        if (mbeanRegistration != null) {
            mbeanRegistration.unregister();
            mbeanRegistration = null;
        }
        if (registration != null) {
            registration.unregister();
            registration = null;
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

    private boolean getBoolean(Dictionary<String, ?> config, String key, boolean def) {
        if (config != null) {
            Object val = config.get(key);
            if (val instanceof Boolean) {
                return (Boolean) val;
            } else if (val != null) {
                return Boolean.parseBoolean(val.toString());
            }
        }
        return def;
    }

}
