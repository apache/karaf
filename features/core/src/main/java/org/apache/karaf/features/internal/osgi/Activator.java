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
package org.apache.karaf.features.internal.osgi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.management.NotCompliantMBeanException;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.BootFeaturesInstaller;
import org.apache.karaf.features.internal.BundleManager;
import org.apache.karaf.features.internal.FeatureConfigInstaller;
import org.apache.karaf.features.internal.FeatureFinder;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.karaf.features.management.internal.FeaturesServiceMBeanImpl;
import org.apache.karaf.region.persist.RegionsPersistence;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator, SingleServiceTracker.SingleServiceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private BundleContext bundleContext;
    private SingleServiceTracker<RegionsPersistence> regionsPersistenceTracker;
    private SingleServiceTracker<URLStreamHandlerService> mvnUrlHandlerTracker;
    private SingleServiceTracker<ConfigurationAdmin> configurationAdminTracker;
    private ServiceTracker<FeaturesListener, FeaturesListener> featuresListenerTracker;

    private FeaturesServiceImpl featuresService;
    private ServiceRegistration<ManagedService> featureFinderRegistration;
    private ServiceRegistration<FeaturesService> featuresServiceRegistration;
    private ServiceRegistration featuresServiceMBeanRegistration;

    @Override
    public void start(BundleContext context) throws Exception {
        bundleContext = context;
        regionsPersistenceTracker = new SingleServiceTracker<RegionsPersistence>(
                bundleContext, RegionsPersistence.class, this
        );
        mvnUrlHandlerTracker = new SingleServiceTracker<URLStreamHandlerService>(
                bundleContext, URLStreamHandlerService.class, "(url.handler.protocol=mvn)", this
        );
        configurationAdminTracker = new SingleServiceTracker<ConfigurationAdmin>(
                bundleContext, ConfigurationAdmin.class, this
        );
        regionsPersistenceTracker.open();
        mvnUrlHandlerTracker.open();
        configurationAdminTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        configurationAdminTracker.close();
        mvnUrlHandlerTracker.close();
        regionsPersistenceTracker.close();
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);
    }

    protected void doStart() {
        ConfigurationAdmin configurationAdmin = configurationAdminTracker.getService();
        RegionsPersistence regionsPersistence = regionsPersistenceTracker.getService();
        URLStreamHandlerService mvnUrlHandler = mvnUrlHandlerTracker.getService();

        if (configurationAdmin == null || mvnUrlHandler == null) {
            return;
        }

        Properties configuration = new Properties();
        File configFile = new File(System.getProperty("karaf.etc"), "org.apache.karaf.features.cfg");
        if (configFile.isFile() && configFile.canRead()) {
            try {
                configuration.load(new FileReader(configFile));
            } catch (IOException e) {
                LOGGER.warn("Error reading configuration file " + configFile.toString(), e);
            }
        }

        FeatureFinder featureFinder = new FeatureFinder();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.features.repos");
        featureFinderRegistration = bundleContext.registerService(ManagedService.class, featureFinder, props);

        BundleManager bundleManager = new BundleManager(bundleContext, regionsPersistence);
        FeatureConfigInstaller configInstaller = new FeatureConfigInstaller(configurationAdmin);
        String featuresRepositories = getString(configuration, "featuresRepositories", "");
        boolean respectStartLvlDuringFeatureStartup = getBoolean(configuration, "respectStartLvlDuringFeatureStartup", true);
        boolean respectStartLvlDuringFeatureUninstall = getBoolean(configuration, "respectStartLvlDuringFeatureUninstall", true);
        long resolverTimeout = getLong(configuration, "resolverTimeout", 5000);
        String overrides = getString(configuration, "overrides", new File(System.getProperty("karaf.etc"), "overrides.properties").toString());
        featuresService = new FeaturesServiceImpl(bundleManager, configInstaller);
        featuresService.setUrls(featuresRepositories);
        featuresService.setRespectStartLvlDuringFeatureStartup(respectStartLvlDuringFeatureStartup);
        featuresService.setRespectStartLvlDuringFeatureUninstall(respectStartLvlDuringFeatureUninstall);
        featuresService.setResolverTimeout(resolverTimeout);
        featuresService.setOverrides(overrides);
        featuresService.setFeatureFinder(featureFinder);
        featuresService.start();
        featuresServiceRegistration = bundleContext.registerService(FeaturesService.class, featuresService, null);

        featuresListenerTracker = new ServiceTracker<FeaturesListener, FeaturesListener>(
                bundleContext, FeaturesListener.class, new ServiceTrackerCustomizer<FeaturesListener, FeaturesListener>() {
            @Override
            public FeaturesListener addingService(ServiceReference<FeaturesListener> reference) {
                FeaturesListener service = bundleContext.getService(reference);
                featuresService.registerListener(service);
                return service;
            }
            @Override
            public void modifiedService(ServiceReference<FeaturesListener> reference, FeaturesListener service) {
            }
            @Override
            public void removedService(ServiceReference<FeaturesListener> reference, FeaturesListener service) {
                featuresService.unregisterListener(service);
                bundleContext.ungetService(reference);
            }
        }
        );
        featuresListenerTracker.open();

        String featuresBoot = getString(configuration, "featuresBoot", "");
        boolean featuresBootAsynchronous = getBoolean(configuration, "featuresBootAsynchronous", false);
        BootFeaturesInstaller bootFeaturesInstaller = new BootFeaturesInstaller(bundleContext, featuresService, featuresBoot, featuresBootAsynchronous);
        bootFeaturesInstaller.start();

        try {
            FeaturesServiceMBeanImpl featuresServiceMBean = new FeaturesServiceMBeanImpl();
            featuresServiceMBean.setBundleContext(bundleContext);
            featuresServiceMBean.setFeaturesService(featuresService);
            props = new Hashtable<String, Object>();
            props.put("jmx.objectname", "org.apache.karaf:type=feature,name=" + System.getProperty("karaf.name"));
            featuresServiceMBeanRegistration = bundleContext.registerService(
                    getInterfaceNames(featuresServiceMBean),
                    featuresServiceMBean,
                    props
            );
        } catch (NotCompliantMBeanException e) {
            LOGGER.warn("Error creating FeaturesService mbean", e);
        }
    }

    protected void doStop() {
        if (featuresListenerTracker != null) {
            featuresListenerTracker.close();
            featuresListenerTracker = null;
        }
        if (featureFinderRegistration != null) {
            featureFinderRegistration.unregister();
            featureFinderRegistration = null;
        }
        if (featuresServiceRegistration != null) {
            featuresServiceRegistration.unregister();
            featuresServiceRegistration = null;
        }
        if (featuresServiceMBeanRegistration != null) {
            featuresServiceMBeanRegistration.unregister();
            featuresServiceMBeanRegistration = null;
        }
        if (featuresService != null) {
            featuresService.stop();
            featuresService = null;
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

    private boolean getBoolean(Properties configuration, String key, boolean value) {
        return Boolean.parseBoolean(getString(configuration, key, Boolean.toString(value)));
    }

    private long getLong(Properties configuration, String key, long value) {
        return Long.parseLong(getString(configuration, key, Long.toString(value)));
    }

}
