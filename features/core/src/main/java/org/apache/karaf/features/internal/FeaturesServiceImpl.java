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
package org.apache.karaf.features.internal;

import static java.lang.String.format;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.utils.version.VersionRange;
import org.apache.felix.utils.version.VersionTable;
import org.apache.karaf.features.BundleInfo;
import org.apache.karaf.features.Conditional;
import org.apache.karaf.features.Dependency;
import org.apache.karaf.features.Feature;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.features.Repository;
import org.apache.karaf.features.RepositoryEvent;
import org.apache.karaf.features.Resolver;
import org.apache.karaf.features.internal.BundleManager.BundleInstallerResult;
import org.apache.karaf.util.collections.CopyOnWriteArrayIdentityList;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.framework.startlevel.BundleStartLevel;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Features service implementation.
 * Adding a repository url will load the features contained in this repository and
 * create dummy sub shells.  When invoked, these commands will prompt the user for
 * installing the needed bundles.
 */
public class FeaturesServiceImpl implements FeaturesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);

    private final BundleManager bundleManager;
    private final FeatureConfigInstaller configManager;

    private long resolverTimeout = 5000;
    private Set<URI> uris;
    private Map<URI, Repository> repositories = new HashMap<URI, Repository>();
    private Map<String, Map<String, Feature>> features;
    private Map<Feature, Set<Long>> installed = new HashMap<Feature, Set<Long>>();
    private List<FeaturesListener> listeners = new CopyOnWriteArrayIdentityList<FeaturesListener>();
    private ThreadLocal<Repository> repo = new ThreadLocal<Repository>();
    private EventAdminListener eventAdminListener;
    
    public FeaturesServiceImpl(BundleManager bundleManager) {
        this(bundleManager, null);
    }
    
    public FeaturesServiceImpl(BundleManager bundleManager, FeatureConfigInstaller configManager) {
        this.bundleManager = bundleManager;
        this.configManager = configManager;
    }
    
    public long getResolverTimeout() {
        return resolverTimeout;
    }

    public void setResolverTimeout(long resolverTimeout) {
        this.resolverTimeout = resolverTimeout;
    }

    public void registerListener(FeaturesListener listener) {
        listeners.add(listener);
        for (Repository repository : listRepositories()) {
            listener.repositoryEvent(new RepositoryEvent(repository, RepositoryEvent.EventType.RepositoryAdded, true));
        }
        for (Feature feature : listInstalledFeatures()) {
            listener.featureEvent(new FeatureEvent(feature, FeatureEvent.EventType.FeatureInstalled, true));
        }
    }

    public void unregisterListener(FeaturesListener listener) {
        listeners.remove(listener);
    }

    public void setUrls(String uris) throws URISyntaxException {
        String[] s = uris.split(",");
        this.uris = new HashSet<URI>();
        for (String value : s) {
            value = value.trim();
            if (!value.isEmpty()) {
                this.uris.add(new URI(value));
            }
        }
    }

    /**
     * Validate a features repository XML.
     *
     * @param uri the features repository URI.
     */
    public void validateRepository(URI uri) throws Exception {
        
        FeatureValidationUtil.validate(uri);
    }

    /**
     * Add a features repository.
     *
     * @param uri the features repository URI.
     * @throws Exception in case of adding failure.
     */
    public void addRepository(URI uri) throws Exception {
        this.addRepository(uri, false);
    }

    /**
     * Add a features repository.
     *
     * @param uri the features repository URI.
     * @param install if true, install all features contained in the features repository.
     * @throws Exception in case of adding failure.
     */
    public void addRepository(URI uri, boolean install) throws Exception {
        if (!repositories.containsKey(uri)) {
            Repository repositoryImpl = this.internalAddRepository(uri);
            saveState();
            if (install) {
                for (Feature feature : repositoryImpl.getFeatures()) {
                    installFeature(feature, EnumSet.noneOf(Option.class));
                }
            }
        } else {
            refreshRepository(uri, install);
        }
    }
    
    /**
     * Refresh a features repository.
     *
     * @param uri the features repository URI.
     * @throws Exception in case of refresh failure.
     */
    protected void refreshRepository(URI uri) throws Exception {
        this.refreshRepository(uri, false);
    }

    /**
     * Refresh a features repository.
     *
     * @param uri the features repository URI.
     * @param install if true, install all features in the features repository.
     * @throws Exception in case of refresh failure.
     */
    protected void refreshRepository(URI uri, boolean install) throws Exception {
        try {
            removeRepository(uri, install);
            addRepository(uri, install);
        } catch (Exception e) {
            //get chance to restore previous, fix for KARAF-4
            restoreRepository(uri);
            throw new Exception("Unable to refresh features repository " + uri, e);
        }
    }

    /**
     * Add a features repository into the internal container.
     *
     * @param uri the features repository URI.
     * @return the internal <code>RepositoryImpl</code> representation.
     * @throws Exception in case of adding failure.
     */
    protected Repository internalAddRepository(URI uri) throws Exception {
        validateRepository(uri);
        RepositoryImpl repo = new RepositoryImpl(uri);
        repositories.put(uri, repo);
        repo.load();
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryAdded, false));
        features = null;
        return repo;
        
    }

    /**
     * Remove a features repository.
     *
     * @param uri the features repository URI.
     * @throws Exception in case of remove failure.
     */
    public void removeRepository(URI uri) throws Exception {
        this.removeRepository(uri, false);
    }

    /**
     * Remove a features repository.
     *
     * @param uri the features repository URI.
     * @param uninstall if true, uninstall all features from the features repository.
     * @throws Exception in case of remove failure.
     */
    public void removeRepository(URI uri, boolean uninstall) throws Exception {
        if (repositories.containsKey(uri)) {
            if (uninstall) {
                Repository repositoryImpl = repositories.get(uri);
                for (Feature feature : repositoryImpl.getFeatures()) {
                    this.uninstallFeature(feature.getName(), feature.getVersion());
                }
            }
            internalRemoveRepository(uri);
            saveState();
        }
    }

    /**
     * Remove a features repository from the internal container.
     *
     * @param uri the features repository URI.
     */
    protected void internalRemoveRepository(URI uri) {
        Repository repo = repositories.remove(uri);
        this.repo.set(repo);
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryRemoved, false));
        features = null;
    }

    /**
     * Restore a features repository.
     *
     * @param uri the features repository URI.
     * @throws Exception in case of restore failure.
     */
    public void restoreRepository(URI uri) throws Exception {
    	repositories.put(uri, repo.get());
    	callListeners(new RepositoryEvent(repo.get(), RepositoryEvent.EventType.RepositoryAdded, false));
        features = null;
    }

    /**
     * Get the list of features repository.
     *
     * @return the list of features repository.
     */
    public Repository[] listRepositories() {
        Collection<Repository> repos = repositories.values();
        return repos.toArray(new Repository[repos.size()]);
    }
    
    @Override
    public Repository getRepository(String repoName) {
        for (Repository repo : this.repositories.values()) {
            if (repoName.equals(repo.getName())) {
                return repo;
            }
        }
        return null;
    }

    /**
     * Install a feature identified by a name.
     *
     * @param name the name of the feature.
     * @throws Exception in case of install failure.
     */
    public void installFeature(String name) throws Exception {
    	installFeature(name, org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION);
    }

    /**
     * Install a feature identified by a name, including a set of options.
     *
     * @param name the name of the feature.
     * @param options the installation options.
     * @throws Exception in case of install failure.
     */
    public void installFeature(String name, EnumSet<Option> options) throws Exception {
        installFeature(name, org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION, options);
    }

    /**
     * Install a feature identified by a name and a version.
     *
     * @param name the name of the feature.
     * @param version the version of the feature.
     * @throws Exception in case of install failure.
     */
    public void installFeature(String name, String version) throws Exception {
        installFeature(name, version, EnumSet.noneOf(Option.class));
    }

    /**
     * Install a feature identified by a name and a version, including a set of options.
     *
     * @param name the name of the feature.
     * @param version the version of the feature.
     * @param options the installation options.
     * @throws Exception in case of install failure.
     */
    public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        Feature f = getFeature(name, version);
        if (f == null) {
            throw new Exception("No feature named '" + name
            		+ "' with version '" + version + "' available");
        }
        installFeature(f, options);
    }

    /**
     * Install a feature including a set of options.
     *
     * @param feature the <code>Feature</code> to install.
     * @param options the installation options set.
     * @throws Exception in case of install failure.
     */
    public void installFeature(Feature feature, EnumSet<Option> options) throws Exception {
        installFeatures(Collections.singleton(feature), options);
    }

    /**
     * Install a set of features, including a set of options.
     *
     * @param features a set of <code>Feature</code>.
     * @param options the installation options set.
     * @throws Exception in case of install failure.
     */
    public void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {
        InstallationState state = new InstallationState();
        InstallationState failure = new InstallationState();
        boolean verbose = options.contains(FeaturesService.Option.Verbose);
        try {
            // Install everything
            for (Feature f : features) {
                InstallationState s = new InstallationState();
            	try {
                    doInstallFeature(s, f, verbose);
                    doInstallFeatureConditionals(s, f, verbose);
                    state.bundleInfos.putAll(s.bundleInfos);
                    state.bundles.addAll(s.bundles);
                    state.features.putAll(s.features);
                    state.installed.addAll(s.installed);

                    //Check if current feature satisfies the conditionals of existing features
                    for (Feature installedFeature : listInstalledFeatures()) {
                        for (Conditional conditional : installedFeature.getConditional()) {
                            if (dependenciesSatisfied(conditional.getCondition(), state)) {
                                doInstallFeatureConditionals(s, installedFeature, verbose);
                            }
                        }
                    }
                    state.bundleInfos.putAll(s.bundleInfos);
                    state.bundles.addAll(s.bundles);
                    state.features.putAll(s.features);
                    state.installed.addAll(s.installed);
            	} catch (Exception e) {
                    failure.bundles.addAll(s.bundles);
                    failure.features.putAll(s.features);
                    failure.installed.addAll(s.installed);
                    if (options.contains(Option.ContinueBatchOnFailure)) {
                        LOGGER.warn("Error when installing feature {}: {}", f.getName(), e);
                    } else {
                        throw e;
                    }
            	}
            }
            bundleManager.refreshBundles(state.bundles, state.installed, options);
            // Start all bundles
            for (Bundle b : state.bundles) {
                LOGGER.info("Starting bundle: {}", b.getSymbolicName());
                startBundle(state, b);
            }
            // Clean up for batch
            if (!options.contains(Option.NoCleanIfFailure)) {
                failure.installed.removeAll(state.bundles);
                for (Bundle b : failure.installed) {
                    try {
                        b.uninstall();
                    } catch (Exception e2) {
                        // Ignore
                    }
                }
            }
            for (Feature f : features) {
                callListeners(new FeatureEvent(f, FeatureEvent.EventType.FeatureInstalled, false));
            }
            for (Map.Entry<Feature, Set<Long>> e : state.features.entrySet()) {
                installed.put(e.getKey(), e.getValue());
            }
            saveState();
        } catch (Exception e) {
            boolean noCleanIfFailure = options.contains(Option.NoCleanIfFailure);
            cleanUpOnFailure(state, failure, noCleanIfFailure);
            throw e;
        }
    }

    /**
     * Start a bundle.
     *
     * @param state the current bundle installation state.
     * @param bundle the bundle to start.
     * @throws Exception in case of start failure.
     */
	private void startBundle(InstallationState state, Bundle bundle) throws Exception {
		if (!isFragment(bundle)) {
		    // do not start bundles that are persistently stopped
		    if (state.installed.contains(bundle)
		            || (bundle.getState() != Bundle.STARTING && bundle.getState() != Bundle.ACTIVE
		                    && bundle.adapt(BundleStartLevel.class).isPersistentlyStarted())) {
		    	// do no start bundles when user request it
		    	Long bundleId = bundle.getBundleId();
		    	BundleInfo bundleInfo = state.bundleInfos.get(bundleId);
		        if (bundleInfo == null || bundleInfo.isStart()) {
		            try {
		                bundle.start();
		            } catch (BundleException be) {
		                String msg = format("Could not start bundle %s in feature(s) %s: %s", bundle.getLocation(), getFeaturesContainingBundleList(bundle), be.getMessage());
		                throw new Exception(msg, be);
		            }
		    	}
		    }
		}
	}
	
	private boolean isFragment(Bundle b) {
	    @SuppressWarnings("rawtypes")
        Dictionary d = b.getHeaders();
        String fragmentHostHeader = (String) d.get(Constants.FRAGMENT_HOST);
        return fragmentHostHeader != null && fragmentHostHeader.trim().length() > 0;
	}

	private void cleanUpOnFailure(InstallationState state, InstallationState failure, boolean noCleanIfFailure) {
		// cleanup on error
		if (!noCleanIfFailure) {
		    // Uninstall everything
		    for (Bundle b : state.installed) {
		        try {
		            b.uninstall();
		        } catch (Exception e2) {
		            // Ignore
		        }
		    }
		    for (Bundle b : failure.installed) {
		        try {
		            b.uninstall();
		        } catch (Exception e2) {
		            // Ignore
		        }
		    }
		} else {
		    // Force start of bundles so that they are flagged as persistently started
		    for (Bundle b : state.installed) {
		        try {
		            b.start();
		        } catch (Exception e2) {
		            // Ignore
		        }
		    }
		}
	}

    protected void doInstallFeature(InstallationState state, Feature feature, boolean verbose) throws Exception {
        LOGGER.debug("Installing feature " + feature.getName() + " " + feature.getVersion());
        if (verbose) {
            System.out.println("Installing feature " + feature.getName() + " " + feature.getVersion());
        }
        for (Dependency dependency : feature.getDependencies()) {
            installFeatureDependency(dependency, state, verbose);
        }
        if (configManager != null) {
            configManager.installFeatureConfigs(feature, verbose);
        }
        Set<Long> bundles = new TreeSet<Long>();
        
        for (BundleInfo bInfo : resolve(feature)) {
            int startLevel = getBundleStartLevel(bInfo.getStartLevel(),feature.getStartLevel());
            BundleInstallerResult result = bundleManager.installBundleIfNeeded(bInfo.getLocation(), startLevel, feature.getRegion());
            state.bundles.add(result.bundle);
            if (result.isNew) {
                state.installed.add(result.bundle);
            }
            if (verbose) {
                if (result.isNew) {
                    System.out.println("Found installed bundle: " + result.bundle);
                } else {
                    System.out.println("Installing bundle " + bInfo.getLocation());
                }
            }

            bundles.add(result.bundle.getBundleId());
            state.bundleInfos.put(result.bundle.getBundleId(), bInfo);

        }
        state.features.put(feature, bundles);
    }

    private int getBundleStartLevel(int bundleStartLevel, int featureStartLevel) {
        return (bundleStartLevel > 0) ? bundleStartLevel : featureStartLevel;
    }

    protected void doInstallFeatureConditionals(InstallationState state, Feature feature,  boolean verbose) throws Exception {
        //Check conditions of the current feature.
        for (Conditional conditional : feature.getConditional()) {

            if (dependenciesSatisfied(conditional.getCondition(), state)) {
                InstallationState s = new InstallationState();
                doInstallFeature(s, conditional.asFeature(feature.getName(), feature.getVersion()), verbose);
                state.bundleInfos.putAll(s.bundleInfos);
                state.bundles.addAll(s.bundles);
                state.features.putAll(s.features);
                state.installed.addAll(s.installed);
            }
        }
    }

    private void installFeatureDependency(Dependency dependency, InstallationState state, boolean verbose)
            throws Exception {
        Feature fi = getFeatureForDependency(dependency);
        if (fi == null) {
            throw new Exception("No feature named '" + dependency.getName()
                    + "' with version '" + dependency.getVersion() + "' available");
        }
        if (state.features.containsKey(fi)) {
            LOGGER.debug("Feature {} with version {} is already being installed", fi.getName(), fi.getVersion());
        } else {
            doInstallFeature(state, fi, verbose);
        }
    }
    
    protected List<BundleInfo> resolve(Feature feature) throws Exception {
        String resolver = feature.getResolver();
        // If no resolver is specified, we expect a list of uris
        if (resolver == null || resolver.length() == 0) {
        	return feature.getBundles();
        }
        boolean optional = false;
        if (resolver.startsWith("(") && resolver.endsWith(")")) {
            resolver = resolver.substring(1, resolver.length() - 1);
            optional = true;
        }
        

        @SuppressWarnings("unchecked")
        ServiceTracker<Resolver, Resolver> tracker = bundleManager.createServiceTrackerForResolverName(resolver);
        if (tracker == null) {
            return feature.getBundles();
        }
        tracker.open();
        try {
            if (optional) {
                Resolver r = (Resolver) tracker.getService();
                if (r != null) {
                    return r.resolve(feature);
                } else {
                    LOGGER.debug("Optional resolver '" + resolver + "' not found, using the default resolver");
                    return feature.getBundles();
                }
            } else {
                Resolver r = (Resolver) tracker.waitForService(resolverTimeout);
                if (r == null) {
                    throw new Exception("Unable to find required resolver '" + resolver + "'");
                }
                return r.resolve(feature);
            }
        } finally {
            tracker.close();
        }
    }

    public void uninstallFeature(String name) throws Exception {
        List<String> versions = new ArrayList<String>();
        for (Feature f : installed.keySet()) {
            if (name.equals(f.getName())) {
                versions.add(f.getVersion());
            }
        }
        if (versions.size() == 0) {
            throw new Exception("Feature named '" + name + "' is not installed");
        } else if (versions.size() > 1) {
            StringBuilder sb = new StringBuilder();
            sb.append("Feature named '").append(name).append("' has multiple versions installed (");
            for (int i = 0; i < versions.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(versions.get(i));
            }
            sb.append("). Please specify the version to uninstall.");
            throw new Exception(sb.toString());
        }
        uninstallFeature(name, versions.get(0));
    }
    
    public void uninstallFeature(String name, String version) throws Exception {
    	Feature feature = getFeature(name, version);
        if (feature == null || !installed.containsKey(feature)) {
            throw new Exception("Feature named '" + name 
            		+ "' with version '" + version + "' is not installed");
        }
        // Grab all the bundles installed by this feature
        // and remove all those who will still be in use.
        // This gives this list of bundles to uninstall.
        Set<Long> bundles = installed.remove(feature);

        //Also remove bundles installed as conditionals
        for (Conditional conditional : feature.getConditional()) {
            bundles.addAll(installed.remove(conditional.asFeature(feature.getName(),feature.getVersion())));
        }

        for (Set<Long> b : installed.values()) {
            bundles.removeAll(b);
        }
        bundleManager.uninstallBundles(bundles);
        callListeners(new FeatureEvent(feature, FeatureEvent.EventType.FeatureUninstalled, false));
        saveState();
    }

    public Feature[] listFeatures() throws Exception {
        Collection<Feature> features = new ArrayList<Feature>();
        for (Map<String, Feature> featureWithDifferentVersion : getFeatures().values()) {
			for (Feature f : featureWithDifferentVersion.values()) {
                features.add(f);
            }
        }
        return features.toArray(new Feature[features.size()]);
    }

    public Feature[] listInstalledFeatures() {
        Set<Feature> result = installed.keySet();
        return result.toArray(new Feature[result.size()]);
    }

    public boolean isInstalled(Feature f) {
        return installed.containsKey(f);
    }

    public Feature getFeature(String name) throws Exception {
        return getFeature(name, org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION);
    }

    public Feature getFeature(String name, String version) throws Exception {
        if (version != null) {
            version = version.trim();
        }
        Map<String, Feature> versions = getFeatures().get(name);
        if (versions == null || versions.isEmpty()) {
            return null;
        } else {
            Feature feature = versions.get(version);
            if (feature == null) {
                if (org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION.equals(version)) {
                    Version latest = new Version(cleanupVersion(version));
                    for (String available : versions.keySet()) {
                        Version availableVersion = new Version(cleanupVersion(available));
                        if (availableVersion.compareTo(latest) > 0) {
                            feature = versions.get(available);
                            latest = availableVersion;
                        }
                    }
                } else {
                    Version latest = new Version(cleanupVersion(org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION));
                    VersionRange versionRange = new VersionRange(version, true, true);
                    for (String available : versions.keySet()) {
                        Version availableVersion = new Version(cleanupVersion(available));
                        if (availableVersion.compareTo(latest) > 0 && versionRange.contains(availableVersion)) {
                            feature = versions.get(available);
                            latest = availableVersion;
                        }
                    }
                }
            }
            return feature;
        }
    }

    protected Map<String, Map<String, Feature>> getFeatures() throws Exception {
        if (features == null) {
        	//the outer map's key is feature name, the inner map's key is feature version       
            Map<String, Map<String, Feature>> map = new HashMap<String, Map<String, Feature>>();
            // Two phase load:
            // * first load dependent repositories
            for (;;) {
                boolean newRepo = false;
                for (Repository repo : listRepositories()) {
                    for (URI uri : repo.getRepositories()) {
                        if (!repositories.containsKey(uri)) {
                            internalAddRepository(uri);
                            newRepo = true;
                        }
                    }
                }
                if (!newRepo) {
                    break;
                }
            }
            // * then load all features
            for (Repository repo : repositories.values()) {
                for (Feature f : repo.getFeatures()) {
                	if (map.get(f.getName()) == null) {
                		Map<String, Feature> versionMap = new HashMap<String, Feature>();
                		versionMap.put(f.getVersion(), f);
                		map.put(f.getName(), versionMap);
                	} else {
                		map.get(f.getName()).put(f.getVersion(), f);
                	}
                }
            }
            features = map;
        }
        return features;
    }

	private void initState() {
        if (!loadState()) {
            if (uris != null) {
                for (URI uri : uris) {
                    try {
                    	internalAddRepository(uri);
                    } catch (Exception e) {
                        LOGGER.warn(format("Unable to add features repository %s at startup", uri), e);    
                    }
                }
            }
            saveState();
        }
	}
    
    public void start() throws Exception {
        this.eventAdminListener = bundleManager.createAndRegisterEventAdminListener();
        initState();
    }

    public void stop() throws Exception {
        uris = new HashSet<URI>(repositories.keySet());
        while (!repositories.isEmpty()) {
            internalRemoveRepository(repositories.keySet().iterator().next());
        }
    }

    protected void saveState() {
        OutputStream os = null;
        try {
            File file = bundleManager.getDataFile("FeaturesServiceState.properties");
            Properties props = new Properties();
            saveSet(props, "repositories.", repositories.keySet());
            saveMap(props, "features.", installed);
            os = new FileOutputStream(file);
            props.store(new FileOutputStream(file), "FeaturesService State");
        } catch (Exception e) {
            LOGGER.error("Error persisting FeaturesService state", e);
        } finally {
            closeStream(os);
        }
    }

    private void closeStream(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (IOException e) {
                // Ignore
            }
        }
    }

    protected boolean loadState() {
        try {
            File file = bundleManager.getDataFile("FeaturesServiceState.properties");
            if (!file.exists()) {
                return false;
            }
            Properties props = new Properties();
            InputStream is = new FileInputStream(file);
            try {
                props.load(is);
            } finally {
                is.close();
            }
            Set<URI> repositories = loadSet(props, "repositories.");
            for (URI repo : repositories) {
            	try {
            		internalAddRepository(repo);
            	} catch (Exception e) {
            		LOGGER.warn(format("Unable to add features repository %s at startup", repo), e);
            	}
            }
            installed = loadMap(props, "features.");
            for (Feature f : installed.keySet()) {
                callListeners(new FeatureEvent(f, FeatureEvent.EventType.FeatureInstalled, true));
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("Error loading FeaturesService state", e);
        }
        return false;
    }

    protected void saveSet(Properties props, String prefix, Set<URI> set) {
        List<URI> l = new ArrayList<URI>(set);
        props.clear();
        props.put(prefix + "count", Integer.toString(l.size()));
        for (int i = 0; i < l.size(); i++) {
            props.put(prefix + "item." + i, l.get(i).toString());
        }
    }

    protected Set<URI> loadSet(Properties props, String prefix) {
        Set<URI> l = new HashSet<URI>();
        String countStr = (String) props.get(prefix + "count");
        if (countStr != null) {
            int count = Integer.parseInt(countStr);
            for (int i = 0; i < count; i++) {
                l.add(URI.create((String) props.get(prefix + "item." + i)));
            }
        }
        return l;
    }

    protected void saveMap(Properties props, String prefix, Map<Feature, Set<Long>> map) {
        for (Map.Entry<Feature, Set<Long>> entry : map.entrySet()) {
            Feature key = entry.getKey();
            String val = createValue(entry.getValue());
            props.put(prefix + key.toString(), val);
        }
    }

    protected Map<Feature, Set<Long>> loadMap(Properties props, String prefix) {
        Map<Feature, Set<Long>> map = new HashMap<Feature, Set<Long>>();
        for (@SuppressWarnings("rawtypes") Enumeration e = props.propertyNames(); e.hasMoreElements();) {
            String key = (String) e.nextElement();
            if (key.startsWith(prefix)) {
                String val = (String) props.get(key);
                Set<Long> set = readValue(val);
                map.put(org.apache.karaf.features.internal.model.Feature.valueOf(key.substring(prefix.length())), set);
            }
        }
        return map;
    }

    protected String createValue(Set<Long> set) {
        StringBuilder sb = new StringBuilder();
        for (long i : set) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(i);
        }
        return sb.toString();
    }

    protected Set<Long> readValue(String val) {
        Set<Long> set = new HashSet<Long>();
        if (val != null && val.length() != 0) {
        	for (String str : val.split(",")) {
        		set.add(Long.parseLong(str));
        	}
        }
        return set;
    }

    protected void callListeners(FeatureEvent event) {
        if (eventAdminListener != null) {
            eventAdminListener.featureEvent(event);
        }
        for (FeaturesListener listener : listeners) {
            listener.featureEvent(event);
        }
    }

    protected void callListeners(RepositoryEvent event) {
        if (eventAdminListener != null) {
            eventAdminListener.repositoryEvent(event);
        }
        for (FeaturesListener listener : listeners) {
            listener.repositoryEvent(event);
        }
    }

    static Pattern fuzzyVersion  = Pattern.compile("(\\d+)(\\.(\\d+)(\\.(\\d+))?)?([^a-zA-Z0-9](.*))?",
                                                   Pattern.DOTALL);
    static Pattern fuzzyModifier = Pattern.compile("(\\d+[.-])*(.*)",
                                                   Pattern.DOTALL);

    /**
     * Clean up version parameters. Other builders use more fuzzy definitions of
     * the version syntax. This method cleans up such a version to match an OSGi
     * version.
     *
     * @param version possibly bundles-non-compliant version
     * @return osgi compliant version
     */
    static public String cleanupVersion(String version) {
        Matcher m = fuzzyVersion.matcher(version);
        if (m.matches()) {
            StringBuffer result = new StringBuffer();
            String d1 = m.group(1);
            String d2 = m.group(3);
            String d3 = m.group(5);
            String qualifier = m.group(7);

            if (d1 != null) {
                result.append(d1);
                if (d2 != null) {
                    result.append(".");
                    result.append(d2);
                    if (d3 != null) {
                        result.append(".");
                        result.append(d3);
                        if (qualifier != null) {
                            result.append(".");
                            cleanupModifier(result, qualifier);
                        }
                    } else if (qualifier != null) {
                        result.append(".0.");
                        cleanupModifier(result, qualifier);
                    }
                } else if (qualifier != null) {
                    result.append(".0.0.");
                    cleanupModifier(result, qualifier);
                }
                return result.toString();
            }
        }
        return version;
    }

    static void cleanupModifier(StringBuffer result, String modifier) {
        Matcher m = fuzzyModifier.matcher(modifier);
        if (m.matches())
            modifier = m.group(2);

        for (int i = 0; i < modifier.length(); i++) {
            char c = modifier.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || c == '_' || c == '-')
                result.append(c);
        }
    }

    public Set<Feature> getFeaturesContainingBundle (Bundle bundle) throws Exception {
        Set<Feature> features = new HashSet<Feature>();
        for (Map<String, Feature> featureMap : this.getFeatures().values()) {
            for (Feature f : featureMap.values()) {
                for (BundleInfo bi : f.getBundles()) {
                    if (bi.getLocation().equals(bundle.getLocation())) {
                        features.add(f);
                        break;
                    }
                }
            }
        }
        return features;
    }

    private String getFeaturesContainingBundleList(Bundle bundle) throws Exception {
        Set<Feature> features = getFeaturesContainingBundle(bundle);
        StringBuilder buffer = new StringBuilder();
        Iterator<Feature> iter = features.iterator();
        while (iter.hasNext()) {
            Feature feature= iter.next();
            buffer.append(feature.getId());
            if (iter.hasNext()) {
                buffer.append(", ");
            }
        }
        return buffer.toString();
    }

    /**
     * Returns the {@link Feature} that matches the {@link Dependency}.
     * @param dependency
     * @return
     * @throws Exception
     */
    private Feature getFeatureForDependency(Dependency dependency) throws Exception {
        VersionRange range = org.apache.karaf.features.internal.model.Feature.DEFAULT_VERSION.equals(dependency.getVersion())
                ? VersionRange.ANY_VERSION : new VersionRange(dependency.getVersion(), true, true);
        Feature fi = null;
        for (Feature f : installed.keySet()) {
            if (f.getName().equals(dependency.getName())) {
                Version v = VersionTable.getVersion(f.getVersion());
                if (range.contains(v)) {
                    if (fi == null || VersionTable.getVersion(fi.getVersion()).compareTo(v) < 0) {
                        fi = f;
                    }
                }
            }
        }
        if (fi == null) {
            Map<String, Feature> avail = getFeatures().get(dependency.getName());
            if (avail != null) {
                for (Feature f : avail.values()) {
                    Version v = VersionTable.getVersion(f.getVersion());
                    if (range.contains(v)) {
                        if (fi == null || VersionTable.getVersion(fi.getVersion()).compareTo(v) < 0) {
                            fi = f;
                        }
                    }
                }
            }
        }
        return fi;
    }

    /**
     * Estimates if the {@link List} of {@link Dependency} is satisfied.
     * The method will look into {@link Feature}s that are already installed or now being installed (if {@link InstallationState} is provided (not null)).
     * @param dependencies
     * @param state
     * @return
     */
    private boolean dependenciesSatisfied(List<? extends Dependency> dependencies, InstallationState state) throws Exception {
       boolean satisfied = true;
       for (Dependency dep : dependencies) {
           Feature f = getFeatureForDependency(dep);
           if (f != null && !isInstalled(f) && (state != null && !state.features.keySet().contains(f))) {
               satisfied = false;
           }
       }
       return satisfied;
    }

}
