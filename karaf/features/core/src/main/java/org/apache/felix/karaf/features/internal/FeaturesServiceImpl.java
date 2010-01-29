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
package org.apache.felix.karaf.features.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.karaf.features.FeaturesService;
import org.apache.felix.karaf.features.Feature;
import org.apache.felix.karaf.features.Repository;
import org.apache.felix.karaf.features.FeaturesListener;
import org.apache.felix.karaf.features.FeatureEvent;
import org.apache.felix.karaf.features.RepositoryEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;

import static java.lang.String.format;

/**
 * The Features service implementation.
 * Adding a repository url will load the features contained in this repository and
 * create dummy sub shells.  When invoked, these commands will prompt the user for
 * installing the needed bundles.
 *
 */
public class FeaturesServiceImpl implements FeaturesService {

    public static final String CONFIG_KEY = "org.apache.felix.karaf.features.configKey";

    private static final Logger LOGGER = LoggerFactory.getLogger(FeaturesServiceImpl.class);

    private BundleContext bundleContext;
    private ConfigurationAdmin configAdmin;
    private PackageAdmin packageAdmin;
    private StartLevel startLevel;
    private PreferencesService preferences;
    private Set<URI> uris;
    private Map<URI, RepositoryImpl> repositories = new HashMap<URI, RepositoryImpl>();
    private Map<String, Map<String, Feature>> features;
    private Map<Feature, Set<Long>> installed = new HashMap<Feature, Set<Long>>();
    private String boot;
    private boolean bootFeaturesInstalled;
    private List<FeaturesListener> listeners = new CopyOnWriteArrayList<FeaturesListener>();

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public ConfigurationAdmin getConfigAdmin() {
        return configAdmin;
    }

    public void setConfigAdmin(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public PackageAdmin getPackageAdmin() {
        return packageAdmin;
    }

    public void setPackageAdmin(PackageAdmin packageAdmin) {
        this.packageAdmin = packageAdmin;
    }

    public PreferencesService getPreferences() {
        return preferences;
    }

    public void setPreferences(PreferencesService preferences) {
        this.preferences = preferences;
    }

    public StartLevel getStartLevel() {
        return startLevel;
    }

    public void setStartLevel(StartLevel startLevel) {
        this.startLevel = startLevel;
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
            this.uris.add(new URI(value));
        }
    }

    public void setBoot(String boot) {
        this.boot = boot;
    }

    public void addRepository(URI uri) throws Exception {
        if (!repositories.containsKey(uri)) {
            internalAddRepository(uri);
            saveState();
        }
    }

    protected RepositoryImpl internalAddRepository(URI uri) throws Exception {
    	RepositoryImpl repo = null;
        repo = new RepositoryImpl(uri);
        repo.load();
        repositories.put(uri, repo);
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryAdded, false));
        features = null;
        return repo;
        
    }

    public void removeRepository(URI uri) {
        if (repositories.containsKey(uri)) {
            internalRemoveRepository(uri);
            saveState();
        }
    }

    public void internalRemoveRepository(URI uri) {
        Repository repo = repositories.remove(uri);
        callListeners(new RepositoryEvent(repo, RepositoryEvent.EventType.RepositoryRemoved, false));
        features = null;
    }

    public Repository[] listRepositories() {
        Collection<RepositoryImpl> repos = repositories.values();
        return repos.toArray(new Repository[repos.size()]);
    }

    public void installAllFeatures(URI uri) throws Exception {
        RepositoryImpl repo = internalAddRepository(uri);
        for (Feature f : repo.getFeatures()) {
            installFeature(f.getName(), f.getVersion());
        }
        internalRemoveRepository(uri);            
    }

    public void uninstallAllFeatures(URI uri) throws Exception {
        RepositoryImpl repo = internalAddRepository(uri);
        for (Feature f : repo.getFeatures()) {
            uninstallFeature(f.getName(), f.getVersion());
        }
        internalRemoveRepository(uri);            
    }

    public void installFeature(String name) throws Exception {
    	installFeature(name, FeatureImpl.DEFAULT_VERSION);
    }

    public void installFeature(String name, String version) throws Exception {
        installFeature(name, version, EnumSet.noneOf(Option.class));
    }

    public void installFeature(String name, String version, EnumSet<Option> options) throws Exception {
        Feature f = getFeature(name, version);
        if (f == null) {
            throw new Exception("No feature named '" + name
            		+ "' with version '" + version + "' available");
        }
        installFeature(f, options);
    }

    public void installFeature(Feature f, EnumSet<Option> options) throws Exception {
        installFeatures(Collections.singleton(f), options);
    }

    public void installFeatures(Set<Feature> features, EnumSet<Option> options) throws Exception {
        InstallationState state = new InstallationState();
        InstallationState failure = new InstallationState();
        try {
            // Install everything
            for (Feature f : features) {
                InstallationState s = new InstallationState();
            	try {
                    doInstallFeature(s, f);
                    state.bundles.addAll(s.bundles);
                    state.features.putAll(s.features);
                    state.installed.addAll(s.installed);
            	} catch (Exception e) {
                    failure.bundles.addAll(s.bundles);
                    failure.features.putAll(s.features);
                    failure.installed.addAll(s.installed);
                    if (options.contains(Option.ContinueBatchOnFailure)) {
                        LOGGER.info("Error when installing feature {}: {}", f.getName(), e);
                    } else {
                        throw e;
                    }
            	}
            }
            // Find bundles to refresh
            boolean print = options.contains(Option.PrintBundlesToRefresh);
            boolean refresh = !options.contains(Option.NoAutoRefreshBundles);
            if (print || refresh) {
                Set<Bundle> bundlesToRefresh = findBundlesToRefresh(state);
                StringBuilder sb = new StringBuilder();
                for (Bundle b : bundlesToRefresh) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(b.getSymbolicName()).append(" (").append(b.getBundleId()).append(")");
                }
                LOGGER.info("Bundles to refresh: {}", sb.toString());
                if (!bundlesToRefresh.isEmpty()) {
                    if (print) {
                        if (refresh) {
                            System.out.println("Refreshing bundles " + sb.toString());
                        } else {
                            System.out.println("The following bundles may need to be refreshed: " + sb.toString());
                        }
                    }
                    if (refresh) {
                        LOGGER.info("Refreshing bundles: {}", sb.toString());
                        getPackageAdmin().refreshPackages(bundlesToRefresh.toArray(new Bundle[bundlesToRefresh.size()]));
                    }
                }
            }
            // Start all bundles
            for (Bundle b : state.bundles) {
                // do not start fragment bundles
                Dictionary d = b.getHeaders();
                String fragmentHostHeader = (String) d.get(Constants.FRAGMENT_HOST);
                if (fragmentHostHeader == null || fragmentHostHeader.trim().length() == 0) {
                    // do not start bundles that are persistently stopped
                    if (state.installed.contains(b)
                            || (b.getState() != Bundle.STARTING && b.getState() != Bundle.ACTIVE
                                    && getStartLevel().isBundlePersistentlyStarted(b))) {
                        try {
                            b.start();
                        } catch (BundleException be) {
                            String[] msgdata = new String[]{
                                b.getLocation(),
                                getFeaturesContainingBundleList(b),
                                be.getMessage()
                            };
                            String msg = MessageFormatter.arrayFormat("Could not start bundle {} in feature(s) {}: {}", msgdata);
                            throw new Exception(msg, be);
                        }
                    }
                }
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
        } catch (Exception e) {
            // cleanup on error
            if (!options.contains(Option.NoCleanIfFailure)) {
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
            // rethrow exception
            throw e;
        }
        for (Feature f : features) {
            callListeners(new FeatureEvent(f, FeatureEvent.EventType.FeatureInstalled, false));
        }
        for (Map.Entry<Feature, Set<Long>> e : state.features.entrySet()) {
            installed.put(e.getKey(), e.getValue());
        }
        saveState();
    }

    protected static class InstallationState {
        final Set<Bundle> installed = new HashSet<Bundle>();
        final List<Bundle> bundles = new ArrayList<Bundle>();
        final Map<Feature, Set<Long>> features = new HashMap<Feature, Set<Long>>();
    }

    protected void doInstallFeature(InstallationState state, Feature feature) throws Exception {
        for (Feature dependency : feature.getDependencies()) {
            Feature f = getFeature(dependency.getName(), dependency.getVersion());
            if (f == null) {
                throw new Exception("No feature named '" + dependency.getName()
                        + "' with version '" + dependency.getVersion() + "' available");
            }
        	doInstallFeature(state, f);
        }
        for (String config : feature.getConfigurations().keySet()) {
            Dictionary<String,String> props = new Hashtable<String, String>(feature.getConfigurations().get(config));
            String[] pid = parsePid(config);
            String key = (pid[1] == null ? pid[0] : pid[0] + "-" + pid[1]);
            props.put(CONFIG_KEY, key);
            Configuration cfg = getConfiguration(configAdmin, pid[0], pid[1]);
            if (cfg.getBundleLocation() != null) {
                cfg.setBundleLocation(null);
            }
            cfg.update(props);
        }
        Set<Long> bundles = new HashSet<Long>();
        for (String bundleLocation : feature.getBundles()) {
            Bundle b = installBundleIfNeeded(state, bundleLocation);
            bundles.add(b.getBundleId());
        }
        state.features.put(feature, bundles);
    }

    protected Set<Bundle> findBundlesToRefresh(InstallationState state) {
        // First pass: include all bundles contained in these features
        Set<Bundle> bundles = new HashSet<Bundle>(state.bundles);
        bundles.removeAll(state.installed);
        if (bundles.isEmpty()) {
            return bundles;
        }
        // Second pass: for each bundle, check if there is any unresolved optional package that could be resolved
        Map<Bundle, List<HeaderParser.PathElement>> imports = new HashMap<Bundle, List<HeaderParser.PathElement>>();
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext();) {
            Bundle b = it.next();
            String importsStr = (String) b.getHeaders().get(Constants.IMPORT_PACKAGE);
            if (importsStr == null) {
                it.remove();
            } else {
                List<HeaderParser.PathElement> importsList = HeaderParser.parseHeader(importsStr);
                for (Iterator<HeaderParser.PathElement> itp = importsList.iterator(); itp.hasNext();) {
                    HeaderParser.PathElement p = itp.next();
                    String resolution = p.getDirective(Constants.RESOLUTION_DIRECTIVE);
                    if (!Constants.RESOLUTION_OPTIONAL.equals(resolution)) {
                        itp.remove();
                    }
                }
                if (importsList.isEmpty()) {
                    it.remove();
                } else {
                    imports.put(b, importsList);
                }
            }
        }
        if (bundles.isEmpty()) {
            return bundles;
        }
        // Third pass: compute a list of packages that are exported by our bundles and see if
        //             some exported packages can be wired to the optional imports
        List<HeaderParser.PathElement> exports = new ArrayList<HeaderParser.PathElement>();
        for (Bundle b : state.installed) {
            String exportsStr = (String) b.getHeaders().get(Constants.EXPORT_PACKAGE);
            if (exportsStr != null) {
                List<HeaderParser.PathElement> exportsList = HeaderParser.parseHeader(exportsStr);
                exports.addAll(exportsList);
            }
        }
        for (Iterator<Bundle> it = bundles.iterator(); it.hasNext();) {
            Bundle b = it.next();
            List<HeaderParser.PathElement> importsList = imports.get(b);
            for (Iterator<HeaderParser.PathElement> itpi = importsList.iterator(); itpi.hasNext();) {
                HeaderParser.PathElement pi = itpi.next();
                boolean matching = false;
                for (HeaderParser.PathElement pe : exports) {
                    if (pi.getName().equals(pe.getName())) {
                        String evStr = pe.getAttribute(Constants.VERSION_ATTRIBUTE);
                        String ivStr = pi.getAttribute(Constants.VERSION_ATTRIBUTE);
                        Version exported = evStr != null ? Version.parseVersion(evStr) : Version.emptyVersion;
                        VersionRange imported = ivStr != null ? VersionRange.parse(ivStr) : VersionRange.infiniteRange;
                        if (imported.isInRange(exported)) {
                            matching = true;
                            break;
                        }
                    }
                }
                if (!matching) {
                    itpi.remove();
                }
            }
            if (importsList.isEmpty()) {
                it.remove();
            } else {
                LOGGER.debug("Refeshing bundle {} ({}) to solve the following optional imports", b.getSymbolicName(), b.getBundleId());
                for (HeaderParser.PathElement p : importsList) {
                    LOGGER.debug("    {}", p);
                }

            }
        }
        return bundles;
    }

    protected Bundle installBundleIfNeeded(InstallationState state, String bundleLocation) throws IOException, BundleException {
        LOGGER.debug("Checking " + bundleLocation);
        InputStream is;
        try {
            is = new BufferedInputStream(new URL(bundleLocation).openStream());
        } catch (RuntimeException e) {
            LOGGER.error(e.getMessage());
            throw e;
        }
        try {
            is.mark(256 * 1024);
            JarInputStream jar = new JarInputStream(is);
            Manifest m = jar.getManifest();
            String sn = m.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
            String vStr = m.getMainAttributes().getValue(Constants.BUNDLE_VERSION);
            Version v = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
            for (Bundle b : bundleContext.getBundles()) {
                if (b.getSymbolicName() != null && b.getSymbolicName().equals(sn)) {
                    vStr = (String) b.getHeaders().get(Constants.BUNDLE_VERSION);
                    Version bv = vStr == null ? Version.emptyVersion : Version.parseVersion(vStr);
                    if (v.equals(bv)) {
                        LOGGER.debug("  found installed bundle: " + b);
                        state.bundles.add(b);
                        return b;
                    }
                }
            }
            try {
                is.reset();
            } catch (IOException e) {
                is.close();
                is = new BufferedInputStream(new URL(bundleLocation).openStream());
            }
            LOGGER.debug("Installing bundle " + bundleLocation);
            Bundle b = getBundleContext().installBundle(bundleLocation, is);
            state.bundles.add(b);
            state.installed.add(b);
            return b;
        } finally {
            is.close();
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
        for (Set<Long> b : installed.values()) {
            bundles.removeAll(b);
        }
        for (long bundleId : bundles) {
            Bundle b = getBundleContext().getBundle(bundleId);
            if (b != null) {
                b.uninstall();
            }
        }
        if (getPackageAdmin() != null) {
            getPackageAdmin().refreshPackages(null);
        }
        callListeners(new FeatureEvent(feature, FeatureEvent.EventType.FeatureInstalled, false));
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

    protected Feature getFeature(String name, String version) throws Exception {
        if (version != null) {
            version = version.trim();
        }
        Map<String, Feature> versions = getFeatures().get(name);
        if (versions == null || versions.isEmpty()) {
            return null;
        } else {
            Feature feature = versions.get(version);
            if (feature == null && FeatureImpl.DEFAULT_VERSION.equals(version)) {
                Version latest = new Version(cleanupVersion(version));
                for (String available : versions.keySet()) {
                    Version availableVersion = new Version(cleanupVersion(available));
                    if (availableVersion.compareTo(latest) > 0) {
                        feature = versions.get(available);
                        latest = availableVersion;
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

    public void start() throws Exception {
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
        if (boot != null && !bootFeaturesInstalled) {
            new Thread() {
                public void run() {
                    String[] list = boot.split(",");
                    Set<Feature> features = new HashSet<Feature>();
                    for (String f : list) {
                        if (f.length() > 0) {
                            try {
                                Feature feature = getFeature(f, FeatureImpl.DEFAULT_VERSION);
                                if (feature != null) {
                                    features.add(feature);
                                } else {
                                    LOGGER.error("Error installing boot feature " + f + ": feature not found");
                                }
                            } catch (Exception e) {
                                LOGGER.error("Error installing boot feature " + f, e);
                            }
                        }
                    }
                    try {
                        installFeatures(features, EnumSet.of(Option.NoCleanIfFailure, Option.ContinueBatchOnFailure));
                    } catch (Exception e) {
                        LOGGER.error("Error installing boot features", e);
                    }
                    bootFeaturesInstalled = true;
                    saveState();
                }
            }.start();
        }
    }

    public void stop() throws Exception {
        uris = new HashSet<URI>(repositories.keySet());
        while (!repositories.isEmpty()) {
            internalRemoveRepository(repositories.keySet().iterator().next());
        }
    }

    protected String[] parsePid(String pid) {
        int n = pid.indexOf('-');
        if (n > 0) {
            String factoryPid = pid.substring(n + 1);
            pid = pid.substring(0, n);
            return new String[]{pid, factoryPid};
        } else {
            return new String[]{pid, null};
        }
    }

    protected Configuration getConfiguration(ConfigurationAdmin configurationAdmin,
                                             String pid, String factoryPid) throws IOException, InvalidSyntaxException {
        Configuration oldConfiguration = findExistingConfiguration(configurationAdmin, pid, factoryPid);
        if (oldConfiguration != null) {
            return oldConfiguration;
        } else {
            if (factoryPid != null) {
                return configurationAdmin.createFactoryConfiguration(pid, null);
            } else {
                return configurationAdmin.getConfiguration(pid, null);
            }
        }
    }

    protected Configuration findExistingConfiguration(ConfigurationAdmin configurationAdmin,
                                                      String pid, String factoryPid) throws IOException, InvalidSyntaxException {
        String key = (factoryPid == null ? pid : pid + "-" + factoryPid);
        String filter = "(" + CONFIG_KEY + "=" + key + ")";
        Configuration[] configurations = configurationAdmin.listConfigurations(filter);
        if (configurations != null && configurations.length > 0)
        {
            return configurations[0];
        }
        else
        {
            return null;
        }
    }
    
    protected void saveState() {
        try {
            Preferences prefs = preferences.getUserPreferences("FeaturesServiceState");
            saveSet(prefs.node("repositories"), repositories.keySet());
            saveMap(prefs.node("features"), installed);
            prefs.putBoolean("bootFeaturesInstalled", bootFeaturesInstalled);
            prefs.flush();
        } catch (Exception e) {
            LOGGER.error("Error persisting FeaturesService state", e);
        }
    }

    protected boolean loadState() {
        try {
            Preferences prefs = preferences.getUserPreferences("FeaturesServiceState");
            if (prefs.nodeExists("repositories")) {
                Set<URI> repositories = loadSet(prefs.node("repositories"));
                for (URI repo : repositories) {
                    internalAddRepository(repo);
                }
                installed = loadMap(prefs.node("features"));
                for (Feature f : installed.keySet()) {
                    callListeners(new FeatureEvent(f, FeatureEvent.EventType.FeatureInstalled, true));
                }
                bootFeaturesInstalled = prefs.getBoolean("bootFeaturesInstalled", false);
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Error loading FeaturesService state", e);
        }
        return false;
    }

    protected void saveSet(Preferences node, Set<URI> set) throws BackingStoreException {
        List<URI> l = new ArrayList<URI>(set);
        node.clear();
        node.putInt("count", l.size());
        for (int i = 0; i < l.size(); i++) {
            node.put("item." + i, l.get(i).toString());
        }
    }

    protected Set<URI> loadSet(Preferences node) {
        Set<URI> l = new HashSet<URI>();
        int count = node.getInt("count", 0);
        for (int i = 0; i < count; i++) {
            l.add(URI.create(node.get("item." + i, null)));
        }
        return l;
    }

    protected void saveMap(Preferences node, Map<Feature, Set<Long>> map) throws BackingStoreException {
        node.clear();
        for (Map.Entry<Feature, Set<Long>> entry : map.entrySet()) {
            Feature key = entry.getKey();
            String val = createValue(entry.getValue());
            node.put(key.toString(), val);
        }
    }

    protected Map<Feature, Set<Long>> loadMap(Preferences node) throws BackingStoreException {
        Map<Feature, Set<Long>> map = new HashMap<Feature, Set<Long>>();
        for (String key : node.keys()) {
            String val = node.get(key, null);
            Set<Long> set = readValue(val);
            map.put(FeatureImpl.valueOf(key), set);
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
        for (FeaturesListener listener : listeners) {
            listener.featureEvent(event);
        }
    }

    protected void callListeners(RepositoryEvent event) {
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
     * @param version
     * @return
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

    public Set<Feature> getFeaturesContainingBundle (Bundle bundle) {
        Set<Feature> features = new HashSet<Feature>();
        for (Map<String, Feature> featureMap : this.features.values()) {
            for (Feature f : featureMap.values()) {
                if (f.getBundles().contains(bundle.getLocation())) {
                    features.add(f);
                }
            }
        }
        return features;
    }

    private String getFeaturesContainingBundleList(Bundle bundle) {
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
}
