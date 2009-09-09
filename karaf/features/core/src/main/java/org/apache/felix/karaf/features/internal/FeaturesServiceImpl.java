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
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        RepositoryImpl repo = new RepositoryImpl(uri);
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
        Feature f = getFeature(name, version);
        if (f == null) {
            throw new Exception("No feature named '" + name 
            		+ "' with version '" + version + "' available");
        }
        for (Feature dependency : f.getDependencies()) {
        	installFeature(dependency.getName(), dependency.getVersion());
        }
        for (String config : f.getConfigurations().keySet()) {
            Dictionary<String,String> props = new Hashtable<String, String>(f.getConfigurations().get(config));
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
        for (String bundleLocation : f.getBundles()) {
            Bundle b = installBundleIfNeeded(bundleLocation);
            bundles.add(b.getBundleId());
        }
        for (long id : bundles) {
            Bundle b = bundleContext.getBundle(id);
            // do not start fragment bundles.
            Dictionary d = b.getHeaders();
            String fragmentHostHeader = (String) d.get(Constants.FRAGMENT_HOST);
            if (fragmentHostHeader == null || fragmentHostHeader.trim().length() == 0) {
                b.start();
            }
        }
        callListeners(new FeatureEvent(f, FeatureEvent.EventType.FeatureInstalled, false));
        installed.put(f, bundles);
        saveState();
    }

    protected Bundle installBundleIfNeeded(String bundleLocation) throws IOException, BundleException {
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
            return getBundleContext().installBundle(bundleLocation, is);
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
                    internalAddRepository(uri);
                }
            }
            saveState();
        }
        if (boot != null && !bootFeaturesInstalled) {
            new Thread() {
                public void run() {
                    String[] list = boot.split(",");
                    for (String f : list) {
                        if (f.length() > 0) {
                            try {
                                installFeature(f);
                            } catch (Exception e) {
                                LOGGER.error("Error installing boot feature " + f, e);
                            }
                        }
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
        for (String str : val.split(",")) {
            set.add(Long.parseLong(str));
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

}
