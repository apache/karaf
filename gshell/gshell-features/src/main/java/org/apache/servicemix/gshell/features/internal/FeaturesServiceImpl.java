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
package org.apache.servicemix.gshell.features.internal;

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
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.gshell.features.Feature;
import org.apache.servicemix.gshell.features.FeaturesService;
import org.apache.servicemix.gshell.features.Repository;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.Version;
import org.osgi.framework.BundleException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;
import org.osgi.service.prefs.PreferencesService;
import org.springframework.osgi.context.BundleContextAware;

/**
 * The Features service implementation.
 * Adding a repository url will load the features contained in this repository and
 * create dummy sub shells.  When invoked, these commands will prompt the user for
 * installing the needed bundles.
 *
 */
public class FeaturesServiceImpl implements FeaturesService, BundleContextAware {

    private static final String ALIAS_KEY = "_alias_factory_pid";

    private static final Log LOGGER = LogFactory.getLog(FeaturesServiceImpl.class);

    private BundleContext bundleContext;
    private ConfigurationAdmin configAdmin;
    private PreferencesService preferences;
    private Set<URI> uris;
    private Map<URI, RepositoryImpl> repositories = new HashMap<URI, RepositoryImpl>();
    private Map<String, Feature> features;
    private Map<String, Set<Long>> installed = new HashMap<String, Set<Long>>();
    private String boot;

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

    public PreferencesService getPreferences() {
        return preferences;
    }

    public void setPreferences(PreferencesService preferences) {
        this.preferences = preferences;
    }

    public void setUrls(String uris) throws URISyntaxException {
        String[] s = uris.split(",");
        this.uris = new HashSet<URI>();
        for (int i = 0; i < s.length; i++) {
            this.uris.add(new URI(s[i]));
        }
    }

    public void setBoot(String boot) {
        this.boot = boot;
    }

    public void addRepository(URI uri) throws Exception {
        internalAddRepository(uri);
        saveState();
    }

    protected void internalAddRepository(URI uri) throws Exception {
        RepositoryImpl repo = new RepositoryImpl(uri);
        repositories.put(uri, repo);
        features = null;
    }

    public void removeRepository(URI uri) {
        internalRemoveRepository(uri);
        saveState();
    }

    public void internalRemoveRepository(URI uri) {
        repositories.remove(uri);
        features = null;
    }

    public Repository[] listRepositories() {
        Collection<RepositoryImpl> repos = repositories.values();
        return repos.toArray(new Repository[repos.size()]);
    }

    public void installFeature(String name) throws Exception {
        FeatureImpl f = (FeatureImpl) getFeature(name);
        if (f == null) {
            throw new Exception("No feature named '" + name + "' available");
        }
        for (String dependency : f.getDependencies()) {
            installFeature(dependency);
        }
        for (String config : f.getConfigurations().keySet()) {
            Dictionary<String,String> props = new Hashtable<String, String>(f.getConfigurations().get(config));
            String[] pid = parsePid(config);
            if (pid[1] != null) {
                props.put(ALIAS_KEY, pid[1]);
            }
            Configuration cfg = getConfiguration(configAdmin, pid[0], pid[1]);
            if (cfg.getBundleLocation() != null) {
                cfg.setBundleLocation(null);
            }
            cfg.update(props);
        }
        Set<Long> bundles = new HashSet<Long>();
        for (String bundleLocation : f.getBundles()) {
            Bundle b = installBundleIfNeeded(bundleLocation);
            b.start();
            bundles.add(b.getBundleId());
        }
        installed.put(name, bundles);
        f.setInstalled(true);
        saveState();
    }

    protected Bundle installBundleIfNeeded(String bundleLocation) throws IOException, BundleException {
        LOGGER.debug("Checking " + bundleLocation);
        InputStream is = new BufferedInputStream(new URL(bundleLocation).openStream());
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
        if (!installed.containsKey(name)) {
            throw new Exception("Feature named '" + name + "' is not installed");
        }
        // Grab all the bundles installed by this feature
        // and remove all those who will still be in use.
        // This gives this list of bundles to uninstall.
        Set<Long> bundles = installed.remove(name);
        for (Set<Long> b : installed.values()) {
            bundles.removeAll(b);
        }
        for (long bundleId : bundles) {
            getBundleContext().getBundle(bundleId).uninstall();
        }
        saveState();
    }

    public String[] listFeatures() throws Exception {
        Collection<String> features = new ArrayList<String>();
        for (Repository repo : repositories.values()) {
            for (Feature f : repo.getFeatures()) {
            	String installStatus = f.isInstalled() ? "installed  " : "uninstalled";
            	String version = f.getVersion();
            	switch (version.length()) {
            	case 1: version = "       " + version;
            	case 2: version = "      " + version;
            	case 3: version = "     " + version;
            	case 4: version = "    " + version;
            	case 5: version = "   " + version;
            	case 6: version = "  " + version;
            	case 7: version = " " + version;
            	}
                features.add("[" + installStatus + "] " + " [" + version + "] " + f.getName());
            }
        }
        return features.toArray(new String[features.size()]);
    }

    public String[] listInstalledFeatures() {
        return installed.keySet().toArray(new String[installed.size()]);
    }

    protected Feature getFeature(String name) throws Exception {
        return getFeatures().get(name);
    }

    protected Map<String, Feature> getFeatures() throws Exception {
        if (features == null) {
            Map<String, Feature> map = new HashMap<String, Feature>();
            for (Repository repo : repositories.values()) {
                for (Feature f : repo.getFeatures()) {
                    map.put(f.getName(), f);
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
        if (boot != null) {
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
        if (factoryPid != null) {
            Configuration[] configs = configurationAdmin.listConfigurations("(|(" + ALIAS_KEY + "=" + pid + ")(.alias_factory_pid=" + factoryPid + "))");
            if (configs == null || configs.length == 0) {
                return configurationAdmin.createFactoryConfiguration(pid, null);
            } else {
                return configs[0];
            }
        } else {
            return configurationAdmin.getConfiguration(pid, null);
        }
    }
    
    protected void saveState() {
        try {
            Preferences prefs = preferences.getUserPreferences("FeaturesServiceState");
            saveSet(prefs.node("repositories"), repositories.keySet());
            saveMap(prefs.node("features"), installed);
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

    protected void saveMap(Preferences node, Map<String, Set<Long>> map) throws BackingStoreException {
        node.clear();
        for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
            String key = entry.getKey();
            String val = createValue(entry.getValue());
            node.put(key, val);
        }
    }

    protected Map<String, Set<Long>> loadMap(Preferences node) throws BackingStoreException {
        Map<String, Set<Long>> map = new HashMap<String, Set<Long>>();
        for (String key : node.keys()) {
            String val = node.get(key, null);
            Set<Long> set = readValue(val);
            map.put(key, set);
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

}
