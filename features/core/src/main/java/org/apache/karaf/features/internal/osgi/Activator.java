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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.service.EventAdminListener;
import org.apache.karaf.features.internal.service.FeatureConfigInstaller;
import org.apache.karaf.features.internal.service.FeatureFinder;
import org.apache.karaf.features.internal.service.BootFeaturesInstaller;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.apache.karaf.features.internal.service.StateStorage;
import org.apache.karaf.features.internal.management.FeaturesServiceMBeanImpl;
import org.apache.karaf.features.RegionsPersistence;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.SingleServiceTracker;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class Activator extends BaseActivator {

    public static final String FEATURES_REPOS_PID = "org.apache.karaf.features.repos";
    public static final String FEATURES_SERVICE_CONFIG_FILE = "org.apache.karaf.features.cfg";

    private ServiceTracker<FeaturesListener, FeaturesListener> featuresListenerTracker;
    private FeaturesServiceImpl featuresService;
    private SingleServiceTracker<RegionsPersistence> regionsTracker;

    public Activator() {
        // Special case here, as we don't want the activator to wait for current job to finish,
        // else it would forbid the features service to refresh itself
        setSchedulerStopTimeout(0);
    }

    @Override
    protected void doOpen() throws Exception {
        trackService(URLStreamHandlerService.class, "(url.handler.protocol=mvn)");
        trackService(ConfigurationAdmin.class);

        Properties configuration = new Properties();
        File configFile = new File(System.getProperty("karaf.etc"), FEATURES_SERVICE_CONFIG_FILE);
        if (configFile.isFile() && configFile.canRead()) {
            try {
                configuration.load(new FileReader(configFile));
            } catch (IOException e) {
                logger.warn("Error reading configuration file " + configFile.toString(), e);
            }
        }
        updated((Dictionary) configuration);
    }

    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        URLStreamHandlerService mvnUrlHandler = getTrackedService(URLStreamHandlerService.class);

        if (configurationAdmin == null || mvnUrlHandler == null) {
            return;
        }

        FeatureFinder featureFinder = new FeatureFinder();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, FEATURES_REPOS_PID);
        register(ManagedService.class, featureFinder, props);

        // TODO: region support
//        final BundleManager bundleManager = new BundleManager(bundleContext);
//        regionsTracker = new SingleServiceTracker<RegionsPersistence>(bundleContext, RegionsPersistence.class,
//                new SingleServiceTracker.SingleServiceListener() {
//                    @Override
//                    public void serviceFound() {
//                        bundleManager.setRegionsPersistence(regionsTracker.getService());
//                    }
//                    @Override
//                    public void serviceLost() {
//                        serviceFound();
//                    }
//                    @Override
//                    public void serviceReplaced() {
//                        serviceFound();
//                    }
//                });
//        regionsTracker.open();


        FeatureConfigInstaller configInstaller = new FeatureConfigInstaller(configurationAdmin);
        // TODO: honor respectStartLvlDuringFeatureStartup and respectStartLvlDuringFeatureUninstall
//        boolean respectStartLvlDuringFeatureStartup = getBoolean("respectStartLvlDuringFeatureStartup", true);
//        boolean respectStartLvlDuringFeatureUninstall = getBoolean("respectStartLvlDuringFeatureUninstall", true);
        String overrides = getString("overrides", new File(System.getProperty("karaf.etc"), "overrides.properties").toURI().toString());
        String featureResolutionRange = getString("featureResolutionRange", FeaturesServiceImpl.DEFAULT_FEATURE_RESOLUTION_RANGE);
        String bundleUpdateRange = getString("bundleUpdateRange", FeaturesServiceImpl.DEFAULT_BUNDLE_UPDATE_RANGE);
        String updateSnapshots = getString("updateSnapshots", FeaturesServiceImpl.DEFAULT_UPDATE_SNAPSHOTS);
        StateStorage stateStorage = new StateStorage() {
            @Override
            protected InputStream getInputStream() throws IOException {
                File file = bundleContext.getDataFile("FeaturesServiceState.properties");
                if (file.exists()) {
                    return new FileInputStream(file);
                } else {
                    return null;
                }
            }

            @Override
            protected OutputStream getOutputStream() throws IOException {
                File file = bundleContext.getDataFile("FeaturesServiceState.properties");
                return new FileOutputStream(file);
            }
        };
        EventAdminListener eventAdminListener;
        try {
            eventAdminListener = new EventAdminListener(bundleContext);
        } catch (Throwable t) {
            eventAdminListener = null;
        }
        featuresService = new FeaturesServiceImpl(
                                bundleContext.getBundle(),
                                bundleContext.getBundle(0).getBundleContext(),
                                stateStorage,
                                featureFinder,
                                eventAdminListener,
                                configInstaller,
                                overrides,
                                featureResolutionRange,
                                bundleUpdateRange,
                                updateSnapshots);
        register(FeaturesService.class, featuresService);

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

        String featuresRepositories = getString("featuresRepositories", "");
        String featuresBoot = getString("featuresBoot", "");
        boolean featuresBootAsynchronous = getBoolean("featuresBootAsynchronous", false);
        BootFeaturesInstaller bootFeaturesInstaller = new BootFeaturesInstaller(
                bundleContext, featuresService,
                featuresRepositories, featuresBoot, featuresBootAsynchronous);
        bootFeaturesInstaller.start();

        FeaturesServiceMBeanImpl featuresServiceMBean = new FeaturesServiceMBeanImpl();
        featuresServiceMBean.setBundleContext(bundleContext);
        featuresServiceMBean.setFeaturesService(featuresService);
        registerMBean(featuresServiceMBean, "type=feature");
    }

    protected void doStop() {
        if (regionsTracker != null) {
            regionsTracker.close();
            regionsTracker = null;
        }
        if (featuresListenerTracker != null) {
            featuresListenerTracker.close();
            featuresListenerTracker = null;
        }
        super.doStop();
        if (featuresService != null) {
            featuresService = null;
        }
    }

}
