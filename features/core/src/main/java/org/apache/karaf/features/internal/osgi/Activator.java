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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

import javax.management.NotCompliantMBeanException;

import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.BootFeaturesInstaller;
import org.apache.karaf.features.internal.BundleManager;
import org.apache.karaf.features.internal.FeatureConfigInstaller;
import org.apache.karaf.features.internal.FeatureFinder;
import org.apache.karaf.features.internal.FeaturesServiceImpl;
import org.apache.karaf.features.management.internal.FeaturesServiceMBeanImpl;
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

    private ServiceTracker<FeaturesListener, FeaturesListener> featuresListenerTracker;
    private FeaturesServiceImpl featuresService;
    private SingleServiceTracker<RegionsPersistence> regionsTracker;

    @Override
    protected void doOpen() throws Exception {
        trackService(URLStreamHandlerService.class, "(url.handler.protocol=mvn)");
        trackService(ConfigurationAdmin.class);

        Properties configuration = new Properties();
        File configFile = new File(System.getProperty("karaf.etc"), "org.apache.karaf.features.cfg");
        if (configFile.isFile() && configFile.canRead()) {
            try {
                configuration.load(new FileReader(configFile));
            } catch (IOException e) {
                logger.warn("Error reading configuration file " + configFile.toString(), e);
            }
        }
        updated((Dictionary) configuration);
    }

    protected void doStart() throws NotCompliantMBeanException {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        URLStreamHandlerService mvnUrlHandler = getTrackedService(URLStreamHandlerService.class);

        if (configurationAdmin == null || mvnUrlHandler == null) {
            return;
        }

        FeatureFinder featureFinder = new FeatureFinder();
        Hashtable<String, Object> props = new Hashtable<String, Object>();
        props.put(Constants.SERVICE_PID, "org.apache.karaf.features.repos");
        register(ManagedService.class, featureFinder, props);

        final BundleManager bundleManager = new BundleManager(bundleContext);
        regionsTracker = new SingleServiceTracker<RegionsPersistence>(bundleContext, RegionsPersistence.class,
                new SingleServiceTracker.SingleServiceListener() {
                    @Override
                    public void serviceFound() {
                        bundleManager.setRegionsPersistence(regionsTracker.getService());
                    }
                    @Override
                    public void serviceLost() {
                        serviceFound();
                    }
                    @Override
                    public void serviceReplaced() {
                        serviceFound();
                    }
                });
        regionsTracker.open();


        FeatureConfigInstaller configInstaller = new FeatureConfigInstaller(configurationAdmin);
        String featuresRepositories = getString("featuresRepositories", "");
        boolean respectStartLvlDuringFeatureStartup = getBoolean("respectStartLvlDuringFeatureStartup", true);
        boolean respectStartLvlDuringFeatureUninstall = getBoolean("respectStartLvlDuringFeatureUninstall", true);
        long resolverTimeout = getLong("resolverTimeout", 5000);
        String overrides = getString("overrides", new File(System.getProperty("karaf.etc"), "overrides.properties").toString());
        featuresService = new FeaturesServiceImpl(bundleManager, configInstaller);
        featuresService.setUrls(featuresRepositories);
        featuresService.setRespectStartLvlDuringFeatureStartup(respectStartLvlDuringFeatureStartup);
        featuresService.setRespectStartLvlDuringFeatureUninstall(respectStartLvlDuringFeatureUninstall);
        featuresService.setResolverTimeout(resolverTimeout);
        featuresService.setOverrides(overrides);
        featuresService.setFeatureFinder(featureFinder);
        featuresService.start();
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

        String featuresBoot = getString("featuresBoot", "");
        boolean featuresBootAsynchronous = getBoolean("featuresBootAsynchronous", false);
        BootFeaturesInstaller bootFeaturesInstaller = new BootFeaturesInstaller(bundleContext, featuresService, featuresBoot, featuresBootAsynchronous);
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
            featuresService.stop();
            featuresService = null;
        }
    }

}
