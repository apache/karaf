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
package org.apache.karaf.features.osgi;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Activator implements BundleActivator {

    public static final String FEATURES_SERVICE_CONFIG_FILE = "org.apache.karaf.features.cfg";

    private static final Logger LOGGER = LoggerFactory.getLogger(Activator.class);

    private BundleContext bundleContext;
    private ServiceTracker<ConfigurationAdmin, ConfigurationAdmin> configurationAdminServiceTracker;
    private ServiceTracker<FeaturesListener, FeaturesListener> featuresListenerTracker;
    private FeaturesServiceImpl featuresService;
    private ServiceRegistration<FeaturesService> featuresServiceRegistration;

    public Activator() {
    }

    public void start(final BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;

        Properties configuration = new Properties();
        File configFile = new File(System.getProperty("karaf.etc"), FEATURES_SERVICE_CONFIG_FILE);
        if (configFile.isFile() && configFile.canRead()) {
            try {
                configuration.load(new FileReader(configFile));
            } catch (IOException e) {
                LOGGER.warn("Error reading configuration file " + configFile.toString(), e);
            }
        }
        featuresService = new FeaturesServiceImpl();
        featuresService.setUrls(getString(configuration, "featuresRepositories", ""));
        featuresService.setBoot(getString(configuration, "featuresBoot", ""));
        featuresService.setBootFeaturesAsynchronous(getBool(configuration, "bootFeaturesAsynchronous", false));
        featuresService.setRespectStartLvlDuringFeatureStartup(getBool(configuration, "respectStartLvlDuringFeatureStartup", false));
        featuresService.setResolverTimeout(getLong(configuration, "resolverTimeout", 5000));
        featuresService.setBundleContext(bundleContext);

        featuresListenerTracker = new ServiceTracker<FeaturesListener, FeaturesListener>(
                bundleContext,
                FeaturesListener.class,
                new ServiceTrackerCustomizer<FeaturesListener, FeaturesListener>() {
                    public FeaturesListener addingService(ServiceReference<FeaturesListener> reference) {
                        FeaturesListener service = bundleContext.getService(reference);
                        featuresService.registerListener(service);
                        return service;
                    }
                    public void modifiedService(ServiceReference<FeaturesListener> reference, FeaturesListener service) {
                    }
                    public void removedService(ServiceReference<FeaturesListener> reference, FeaturesListener service) {
                        featuresService.unregisterListener(service);
                        bundleContext.ungetService(reference);
                    }
                }
        );

        configurationAdminServiceTracker = new ServiceTracker<ConfigurationAdmin, ConfigurationAdmin>(
                bundleContext,
                ConfigurationAdmin.class,
                new ServiceTrackerCustomizer<ConfigurationAdmin, ConfigurationAdmin>() {
                    public ConfigurationAdmin addingService(ServiceReference<ConfigurationAdmin> reference) {
                        ConfigurationAdmin configurationAdmin = bundleContext.getService(reference);
                        doStart(configurationAdmin);
                        return configurationAdmin;
                    }

                    public void modifiedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin service) {
                    }

                    public void removedService(ServiceReference<ConfigurationAdmin> reference, ConfigurationAdmin service) {
                        doStop();
                    }
                }
        );

        featuresListenerTracker.open();

        configurationAdminServiceTracker.open();
    }

    public void stop(BundleContext bundleContext) throws Exception {
        if (configurationAdminServiceTracker != null) {
            configurationAdminServiceTracker.close();
            configurationAdminServiceTracker = null;
        }
    }

    protected void doStart(ConfigurationAdmin configurationAdmin) {
        doStop();
        try {
            featuresService.setConfigAdmin(configurationAdmin);
            featuresListenerTracker.open();
            featuresService.start();
            featuresServiceRegistration = bundleContext.registerService(FeaturesService.class, featuresService, null);
        } catch (Exception e) {
            LOGGER.error("Error starting FeaturesService", e);
        }
    }

    protected void doStop() {
        if (featuresServiceRegistration != null) {
            try {
                featuresServiceRegistration.unregister();
            } catch (IllegalStateException e) {
                // Ignore
            }
        }
        featuresListenerTracker.close();
        try {
            featuresService.stop();
        } catch (Exception e) {
            LOGGER.warn("Error stopping FeaturesService", e);
        }
    }

    private static String getString(Properties configuration, String key, String def) {
        if (configuration.containsKey(key)) {
            return configuration.getProperty(key);
        } else {
            return def;
        }
    }

    private static boolean getBool(Properties configuration, String key, boolean def) {
        if (configuration.containsKey(key)) {
            return Boolean.parseBoolean(configuration.getProperty(key));
        } else {
            return def;
        }
    }

    private static long getLong(Properties configuration, String key, long def) {
        if (configuration.containsKey(key)) {
            return Long.parseLong(configuration.getProperty(key));
        } else {
            return def;
        }
    }

}
