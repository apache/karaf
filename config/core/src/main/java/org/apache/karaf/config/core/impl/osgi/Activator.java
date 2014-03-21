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
package org.apache.karaf.config.core.impl.osgi;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.management.NotCompliantMBeanException;

import org.apache.karaf.config.core.ConfigRepository;
import org.apache.karaf.config.core.impl.ConfigMBeanImpl;
import org.apache.karaf.config.core.impl.ConfigRepositoryImpl;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, SingleServiceTracker.SingleServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private BundleContext bundleContext;
    private SingleServiceTracker<ConfigurationAdmin> configurationAdminTracker;

    private ServiceRegistration<ConfigRepository> configRepositoryRegistration;
    private ServiceRegistration configRepositoryMBeanRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        configurationAdminTracker = new SingleServiceTracker<ConfigurationAdmin>(
                bundleContext, ConfigurationAdmin.class, this
        );
        configurationAdminTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        configurationAdminTracker.close();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    protected void doStart() {
        ConfigurationAdmin configurationAdmin = configurationAdminTracker.getService();

        if (configurationAdmin == null) {
            return;
        }

        ConfigRepository configRepository = new ConfigRepositoryImpl(configurationAdmin);
        configRepositoryRegistration = bundleContext.registerService(ConfigRepository.class, configRepository, null);

        try {
            ConfigMBeanImpl configMBean = new ConfigMBeanImpl();
            configMBean.setConfigRepo(configRepository);
            Hashtable<String, Object> props = new Hashtable<String, Object>();
            props.put("jmx.objectname", "org.apache.karaf:type=config,name=" + System.getProperty("karaf.name"));
            configRepositoryMBeanRegistration = bundleContext.registerService(
                    getInterfaceNames(configMBean),
                    configMBean,
                    props
            );
        } catch (NotCompliantMBeanException e) {
            LOGGER.warn("Error creating ConfigRepository mbean", e);
        }
    }

    protected void doStop() {
        if (configRepositoryRegistration != null) {
            configRepositoryRegistration.unregister();
            configRepositoryRegistration = null;
        }
        if (configRepositoryMBeanRegistration != null) {
            configRepositoryMBeanRegistration.unregister();
            configRepositoryMBeanRegistration = null;
        }
    }

    @Override
    public void serviceFound() {
        executor.submit(new Runnable() {
            @Override
            public void run() {
                doStop();
                try {
                    doStart();
                } catch (Exception e) {
                    LOGGER.warn("Error starting FeaturesService", e);
                    doStop();
                }
            }
        });
    }

    @Override
    public void serviceLost() {
        serviceFound();
    }

    @Override
    public void serviceReplaced() {
        serviceFound();
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

    private String getString(Properties configuration, String key, String value) {
        return configuration.getProperty(key, value);
    }

}
