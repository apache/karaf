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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.properties.Properties;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.internal.management.FeaturesServiceMBeanImpl;
import org.apache.karaf.features.internal.region.DigraphHelper;
import org.apache.karaf.features.internal.region.SubsystemResolveContext;
import org.apache.karaf.features.internal.repository.AggregateRepository;
import org.apache.karaf.features.internal.repository.JsonRepository;
import org.apache.karaf.features.internal.repository.XmlRepository;
import org.apache.karaf.features.internal.service.BootFeaturesInstaller;
import org.apache.karaf.features.internal.service.EventAdminListener;
import org.apache.karaf.features.internal.service.FeatureFinder;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.apache.karaf.features.internal.service.StateStorage;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.eclipse.equinox.internal.region.CollisionHookHelper;
import org.eclipse.equinox.internal.region.StandardRegionDigraph;
import org.eclipse.equinox.internal.region.management.StandardManageableRegionDigraph;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Services(
    requires = {
            @RequireService(ConfigurationAdmin.class),
            @RequireService(Resolver.class),
            @RequireService(value = URLStreamHandlerService.class, filter = "(url.handler.protocol=mvn)")
    },
    provides = {
            @ProvideService(FeaturesService.class),
            @ProvideService(RegionDigraph.class)
    }
)
public class Activator extends BaseActivator {

    public static final String FEATURES_REPOS_PID = "org.apache.karaf.features.repos";
    public static final String FEATURES_SERVICE_CONFIG_FILE = "org.apache.karaf.features.cfg";

    private static final String STATE_FILE = "state.json";

    private ServiceTracker<FeaturesListener, FeaturesListener> featuresListenerTracker;
    private FeaturesServiceImpl featuresService;
    private StandardRegionDigraph digraph;
    private StandardManageableRegionDigraph digraphMBean;

    public Activator() {
        // Special case here, as we don't want the activator to wait for current job to finish,
        // else it would forbid the features service to refresh itself
        setSchedulerStopTimeout(0);
    }

    @Override
    protected void doOpen() throws Exception {
        super.doOpen();

        Properties configuration = new Properties();
        File configFile = new File(System.getProperty("karaf.etc"), FEATURES_SERVICE_CONFIG_FILE);
        if (configFile.isFile() && configFile.canRead()) {
            try {
                configuration.load(new FileReader(configFile));
            } catch (IOException e) {
                logger.warn("Error reading configuration file " + configFile.toString(), e);
            }
        }
        Dictionary<String, String> props = new Hashtable<>();
        for (Map.Entry<String, String> entry : configuration.entrySet()) {
            props.put(entry.getKey(), entry.getValue());
        }
        updated(props);
    }

    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        Resolver resolver = getTrackedService(Resolver.class);
        URLStreamHandlerService mvnUrlHandler = getTrackedService(URLStreamHandlerService.class);

        if (configurationAdmin == null || mvnUrlHandler == null) {
            return;
        }

        // Resolver
//        register(Resolver.class, new ResolverImpl(new Slf4jResolverLog(LoggerFactory.getLogger(ResolverImpl.class))));

        // RegionDigraph
        digraph = DigraphHelper.loadDigraph(bundleContext);
        register(ResolverHookFactory.class, digraph.getResolverHookFactory());
        register(CollisionHook.class, CollisionHookHelper.getCollisionHook(digraph));
        register(org.osgi.framework.hooks.bundle.FindHook.class, digraph.getBundleFindHook());
        register(org.osgi.framework.hooks.bundle.EventHook.class, digraph.getBundleEventHook());
        register(org.osgi.framework.hooks.service.FindHook.class, digraph.getServiceFindHook());
        register(org.osgi.framework.hooks.service.EventHook.class, digraph.getServiceEventHook());
        register(RegionDigraph.class, digraph);
        digraphMBean = new StandardManageableRegionDigraph(digraph, "org.apache.karaf", bundleContext);
        digraphMBean.registerMBean();


        FeatureFinder featureFinder = new FeatureFinder();
        Hashtable<String, Object> props = new Hashtable<>();
        props.put(Constants.SERVICE_PID, FEATURES_REPOS_PID);
        register(ManagedService.class, featureFinder, props);

        List<Repository> repositories = new ArrayList<>();
        String[] resourceRepositories = getString("resourceRepositories", "").split(",");
        long repositoryExpiration = getLong("repositoryExpiration", FeaturesService.DEFAULT_REPOSITORY_EXPIRATION);
        boolean repositoryIgnoreFailures = getBoolean("repositoryIgnoreFailures", true);
        for (String url : resourceRepositories) {
            url = url.trim();
            if (!url.isEmpty()) {
                if (url.startsWith("json:")) {
                    repositories.add(new JsonRepository(url.substring("json:".length()), repositoryExpiration, repositoryIgnoreFailures));
                } else if (url.startsWith("xml:")) {
                    repositories.add(new XmlRepository(url.substring("xml:".length()), repositoryExpiration, repositoryIgnoreFailures));
                } else {
                    logger.warn("Unrecognized resource repository: " + url);
                }
            }
        }
        Repository globalRepository;
        switch (repositories.size()) {
        case 0:
            globalRepository = null;
            break;
        case 1:
            globalRepository = repositories.get(0);
            break;
        default:
            globalRepository = new AggregateRepository(repositories);
            break;
        }

        String overrides = getString("overrides", new File(System.getProperty("karaf.etc"), "overrides.properties").toURI().toString());
        String featureResolutionRange = getString("featureResolutionRange", FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE);
        String bundleUpdateRange = getString("bundleUpdateRange", FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE);
        String updateSnapshots = getString("updateSnapshots", FeaturesService.DEFAULT_UPDATE_SNAPSHOTS);
        int downloadThreads = getInt("downloadThreads", FeaturesService.DEFAULT_DOWNLOAD_THREADS);
        long scheduleDelay = getLong("scheduleDelay", FeaturesService.DEFAULT_SCHEDULE_DELAY);
        int scheduleMaxRun = getInt("scheduleMaxRun", FeaturesService.DEFAULT_SCHEDULE_MAX_RUN);
        String blacklisted = getString("blacklisted", new File(System.getProperty("karaf.etc"), "blacklisted.properties").toURI().toString());
        String serviceRequirements = getString("serviceRequirements", FeaturesService.SERVICE_REQUIREMENTS_DEFAULT);
        StateStorage stateStorage = new StateStorage() {
            @Override
            protected InputStream getInputStream() throws IOException {
                File file = bundleContext.getDataFile(STATE_FILE);
                if (file.exists()) {
                    return new FileInputStream(file);
                } else {
                    return null;
                }
            }

            @Override
            protected OutputStream getOutputStream() throws IOException {
                File file = bundleContext.getDataFile(STATE_FILE);
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
                configurationAdmin,
                resolver,
                digraph,
                overrides,
                featureResolutionRange,
                bundleUpdateRange,
                updateSnapshots,
                serviceRequirements,
                globalRepository,
                downloadThreads,
                scheduleDelay,
                scheduleMaxRun,
                blacklisted);
        register(FeaturesService.class, featuresService);

        featuresListenerTracker = new ServiceTracker<>(
                bundleContext,
                FeaturesListener.class,
                new ServiceTrackerCustomizer<FeaturesListener, FeaturesListener>() {
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
        if (digraphMBean != null) {
            digraphMBean.unregisterMbean();
            digraphMBean = null;
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
        if (digraph != null) {
            try {
                DigraphHelper.saveDigraph(bundleContext, digraph);
            } catch (Exception e) {
                // Ignore
            }
            digraph = null;
        }
    }

}
