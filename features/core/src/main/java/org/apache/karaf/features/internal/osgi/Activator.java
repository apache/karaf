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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.felix.resolver.ResolverImpl;
import org.apache.felix.utils.properties.Properties;
import org.apache.felix.utils.repository.AggregateRepository;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.RegionDigraphPersistence;
import org.apache.karaf.features.internal.management.FeaturesServiceMBeanImpl;
import org.apache.karaf.features.internal.region.DigraphHelper;
import org.apache.karaf.features.internal.repository.JsonRepository;
import org.apache.karaf.features.internal.repository.XmlRepository;
import org.apache.karaf.features.internal.resolver.Slf4jResolverLog;
import org.apache.karaf.features.internal.service.BootFeaturesInstaller;
import org.apache.karaf.features.internal.service.EventAdminListener;
import org.apache.karaf.features.internal.service.FeatureConfigInstaller;
import org.apache.karaf.features.internal.service.FeatureRepoFinder;
import org.apache.karaf.features.internal.service.FeaturesServiceConfig;
import org.apache.karaf.features.internal.service.FeaturesServiceImpl;
import org.apache.karaf.features.internal.service.BundleInstallSupport;
import org.apache.karaf.features.internal.service.BundleInstallSupportImpl;
import org.apache.karaf.features.internal.service.StateStorage;
import org.apache.karaf.util.ThreadUtils;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.eclipse.equinox.internal.region.CollisionHookHelper;
import org.eclipse.equinox.internal.region.StandardRegionDigraph;
import org.eclipse.equinox.internal.region.management.StandardManageableRegionDigraph;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.bundle.CollisionHook;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.service.repository.Repository;
import org.osgi.service.resolver.Resolver;
import org.osgi.service.url.URLStreamHandlerService;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.LoggerFactory;

@Services(
    requires = {
            @RequireService(ConfigurationAdmin.class),
            @RequireService(value = URLStreamHandlerService.class, filter = "(url.handler.protocol=mvn)")
    },
    provides = {
            @ProvideService(FeaturesService.class),
            @ProvideService(RegionDigraph.class),
            @ProvideService(RegionDigraphPersistence.class)
    }
)
public class Activator extends BaseActivator {

    static final String FEATURES_SERVICE_CONFIG = "org.apache.karaf.features";
    public static final String FEATURES_SERVICE_CONFIG_FILE = FEATURES_SERVICE_CONFIG + ".cfg";
    public static final String FEATURES_SERVICE_PROCESSING_FILE = "org.apache.karaf.features.xml";
    public static final String FEATURES_SERVICE_PROCESSING_VERSIONS_FILE = "versions.properties";

    private static final String STATE_FILE = "state.json";

    private ServiceTracker<FeaturesListener, FeaturesListener> featuresListenerTracker;
    private FeaturesServiceImpl featuresService;
    private StandardManageableRegionDigraph digraphMBean;
    private BundleInstallSupport installSupport;
    private ExecutorService executorService;

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

        Dictionary<String, Object> props = new Hashtable<>();

        if (!configuration.isEmpty()) {
            for (Map.Entry<String, String> entry : configuration.entrySet()) {
                props.put(entry.getKey(), entry.getValue());
            }
        } else {
            // work around https://issues.apache.org/jira/browse/KARAF-6866
            // org.apache.karaf.features.cfg might have been read empty
            // but ConfigurationAdmin also has all values available:
            ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
            if (configurationAdmin != null) {
                Configuration featuresServiceConfig = configurationAdmin.getConfiguration(FEATURES_SERVICE_CONFIG);
                if (featuresServiceConfig != null) {
                    props = featuresServiceConfig.getProperties();
                }
            }
        }

        updated(props);
    }

    protected void doStart() throws Exception {
        BundleContext systemBundleContext = bundleContext.getBundle(0).getBundleContext();
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        int resolverThreads = getInt("resolverThreads", Runtime.getRuntime().availableProcessors());
        executorService = new ThreadPoolExecutor(0, resolverThreads,
                1L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                ThreadUtils.namedThreadFactory("resolver"));
        Resolver resolver = new ResolverImpl(new Slf4jResolverLog(LoggerFactory.getLogger(ResolverImpl.class)), executorService);
        URLStreamHandlerService mvnUrlHandler = getTrackedService(URLStreamHandlerService.class);

        if (configurationAdmin == null || mvnUrlHandler == null) {
            return;
        }

        StandardRegionDigraph dg = DigraphHelper.loadDigraph(bundleContext);
        DigraphHelper.verifyUnmanagedBundles(bundleContext, dg);
        registerRegionDiGraph(dg);
        boolean configCfgStore = getBoolean("configCfgStore", FeaturesService.DEFAULT_CONFIG_CFG_STORE);
        FeatureConfigInstaller configInstaller = new FeatureConfigInstaller(configurationAdmin, configCfgStore);
        installSupport = new BundleInstallSupportImpl(
                    bundleContext.getBundle(),
                    bundleContext,
                    systemBundleContext,
                    getTrackedServiceRef(ConfigurationAdmin.class).getBundle(),
                    configInstaller,
                    dg);
        register(RegionDigraphPersistence.class, () -> installSupport.saveDigraph());

        FeatureRepoFinder featureFinder = new FeatureRepoFinder();
        register(ManagedService.class, featureFinder, FeatureRepoFinder.getServiceProperties());

        Repository globalRepository = getGlobalRepository();
        FeaturesServiceConfig cfg = getConfig();
        StateStorage stateStorage = createStateStorage();
        featuresService = new FeaturesServiceImpl(
                stateStorage,
                featureFinder,
                configurationAdmin,
                resolver,
                installSupport,
                globalRepository,
                cfg);
        try {
            EventAdminListener eventAdminListener = new EventAdminListener(bundleContext);
            featuresService.registerListener(eventAdminListener);
        } catch (Throwable t) {
            // No EventAdmin support in this case 
        }
        register(FeaturesService.class, featuresService);

        featuresListenerTracker = createFeatureListenerTracker();
        featuresListenerTracker.open();

        FeaturesServiceMBeanImpl featuresServiceMBean = new FeaturesServiceMBeanImpl();
        featuresServiceMBean.setBundleContext(bundleContext);
        featuresServiceMBean.setFeaturesService(featuresService);
        registerMBean(featuresServiceMBean, "type=feature");

        String[] featuresRepositories = getStringArray("featuresRepositories", "");
        String featuresBoot = getString("featuresBoot", "");
        boolean featuresBootAsynchronous = getBoolean("featuresBootAsynchronous", false);
        BootFeaturesInstaller bootFeaturesInstaller = new BootFeaturesInstaller(
                bundleContext, featuresService,
                featuresRepositories, featuresBoot, featuresBootAsynchronous);
        bootFeaturesInstaller.start();
    }

    private Repository getGlobalRepository() {
        List<Repository> repositories = new ArrayList<>();
        String[] resourceRepositories = getStringArray("resourceRepositories", "");
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
        return globalRepository;
    }

    private FeaturesServiceConfig getConfig() {
        String karafEtc = System.getProperty("karaf.etc");
        return new FeaturesServiceConfig(
            getString("overrides", new File(karafEtc, "overrides.properties").toURI().toString()),
            getString("featureResolutionRange", FeaturesService.DEFAULT_FEATURE_RESOLUTION_RANGE),
            getString("bundleUpdateRange", FeaturesService.DEFAULT_BUNDLE_UPDATE_RANGE),
            getString("updateSnapshots", FeaturesService.DEFAULT_UPDATE_SNAPSHOTS.getValue()),
            getInt("downloadThreads", FeaturesService.DEFAULT_DOWNLOAD_THREADS),
            getLong("scheduleDelay", FeaturesService.DEFAULT_SCHEDULE_DELAY),
            getInt("scheduleMaxRun", FeaturesService.DEFAULT_SCHEDULE_MAX_RUN),
            getString("blacklisted", new File(karafEtc, "blacklisted.properties").toURI().toString()),
            getString("featureProcessing", new File(karafEtc, FEATURES_SERVICE_PROCESSING_FILE).toURI().toString()),
            getString("featureProcessingVersions", new File(karafEtc, FEATURES_SERVICE_PROCESSING_VERSIONS_FILE).toURI().toString()),
            getString("serviceRequirements", FeaturesService.ServiceRequirementsBehavior.Default.getValue()),
            getBoolean("autoRefresh", FeaturesService.DEFAULT_AUTO_REFRESH));
    }

    private StateStorage createStateStorage() {
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
        return stateStorage;
    }

    @SuppressWarnings("deprecation")
    private void registerRegionDiGraph(StandardRegionDigraph dg) throws BundleException {
        Dictionary<String, Object> ranking = new Hashtable<>();
        ranking.put(Constants.SERVICE_RANKING, 1000);
        register(ResolverHookFactory.class, dg.getResolverHookFactory());
        register(CollisionHook.class, CollisionHookHelper.getCollisionHook(dg));
        register(org.osgi.framework.hooks.bundle.FindHook.class, dg.getBundleFindHook(), ranking);
        register(org.osgi.framework.hooks.bundle.EventHook.class, dg.getBundleEventHook(), ranking);
        register(org.osgi.framework.hooks.service.FindHook.class, dg.getServiceFindHook(), ranking);
        register(org.osgi.framework.hooks.service.EventHook.class, dg.getServiceEventHook(), ranking);
        register(RegionDigraph.class, dg);
        
        if (getBoolean("digraphMBean", FeaturesService.DEFAULT_DIGRAPH_MBEAN)) {
            StandardManageableRegionDigraph dgmb = digraphMBean = new StandardManageableRegionDigraph(dg, "org.apache.karaf", bundleContext);
            dgmb.registerMBean();
        }

        DigraphHelper.verifyUnmanagedBundles(bundleContext, dg);
    }

    private ServiceTracker<FeaturesListener, FeaturesListener> createFeatureListenerTracker() {
        return new ServiceTracker<>(
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
                        if (featuresService != null && service != null) {
                            featuresService.unregisterListener(service);
                        }
                        if (bundleContext != null && reference != null) {
                            bundleContext.ungetService(reference);
                        }
                    }
                }
        );
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
        if (installSupport != null) {
            installSupport.unregister();
            installSupport.saveDigraph();
            installSupport = null;
        }
    }

}
