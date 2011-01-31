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
package org.apache.karaf.shell.dev.watch;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.Parser;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Runnable singleton which watches at the defined location for bundle updates.
 */
public class BundleWatcher implements Runnable, BundleListener {

    private final Logger logger = LoggerFactory.getLogger(BundleWatcher.class);

    private BundleContext bundleContext;
    private ConfigurationAdmin configurationAdmin;

    private AtomicBoolean running = new AtomicBoolean(false);
    private long interval = 1000L;
    private List<String> watchURLs = new CopyOnWriteArrayList<String>();
    private AtomicInteger counter = new AtomicInteger(0);


    /**
     * Construcotr
     */
    public BundleWatcher() {
    }

    public void bundleChanged(BundleEvent event) {
        if (event.getType() == BundleEvent.INSTALLED
                || event.getType() == BundleEvent.UNINSTALLED) {
            counter.incrementAndGet();
        }
    }

    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Bundle watcher thread started");
        }
        int oldCounter = -1;
        Set<Bundle> watchedBundles = new HashSet<Bundle>();
        while (running.get() && watchURLs.size()>0) {
            if (oldCounter != counter.get()) {
                oldCounter = counter.get();
                watchedBundles.clear();
                for (String bundleURL : watchURLs) {
                    for (Bundle bundle : getBundlesByURL(bundleURL)) {
                        watchedBundles.add(bundle);
                    }
                }
            }
            if (watchedBundles.size()>0) {
                File localRepository = getLocalRepository();
                for (Bundle bundle : watchedBundles) {
                    try {
                        File location = getBundleExternalLocation(localRepository, bundle);
                        if (location != null
                                && location.exists()
                                && location.lastModified() > bundle.getLastModified())
                        {
                            InputStream is = new FileInputStream(location);
                            try {
                                System.out.println("[Watch] Updating watched bundle: " + bundle.getSymbolicName() + " (" + bundle.getVersion() + ")");
                                bundle.update(is);
                            } finally {
                                is.close();
                            }
                        }
                    } catch (IOException ex) {
                        logger.error("Error watching bundle.", ex);
                    } catch (BundleException ex) {
                        logger.error("Error updating bundle.", ex);
                    }
                }
            }
            try {
                Thread.sleep(interval);
            } catch (InterruptedException ex) {
                running.set(false);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Bundle watcher thread stopped");
        }
    }

    /**
     * Adds a Bundle URLs to the watch list.
     * @param url
     */
    public void add(String url) {
        boolean shouldStart = running.get() && (watchURLs.size()==0);
        if (!watchURLs.contains(url)) {
            watchURLs.add(url);
            counter.incrementAndGet();
        }
        if (shouldStart) {
            Thread thread = new Thread(this);
            thread.start();
        }
    }

    /**
     * Removes a bundle URLs from the watch list.
     * @param url
     */
    public void remove(String url) {
        watchURLs.remove(url);
        counter.incrementAndGet();
    }

    /**
     * Returns the location of the Bundle inside the local maven repository.
     * @param bundle
     * @return
     */
    public File getBundleExternalLocation(File localRepository, Bundle bundle) {
        try {
            Parser p = new Parser(bundle.getLocation().substring(4));
            return new File(localRepository.getPath() + File.separator + p.getArtifactPath());
        } catch (MalformedURLException e) {
            logger.error("Could not parse artifact path for bundle" + bundle.getSymbolicName(), e);
        }
        return null;
    }

    public File getLocalRepository() {
        // Attempt to retrieve local repository location from MavenConfiguration
        MavenConfiguration configuration = retrieveMavenConfiguration();
        if (configuration != null) {
            MavenRepositoryURL localRepositoryURL = configuration.getLocalRepository();
            if (localRepositoryURL != null) {
                return localRepositoryURL.getFile().getAbsoluteFile();
            }
        }
        // If local repository not found assume default.
        String localRepo = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        return new File(localRepo).getAbsoluteFile();
    }

    protected MavenConfiguration retrieveMavenConfiguration() {
        MavenConfiguration mavenConfiguration = null;
        try {
            Configuration configuration = configurationAdmin.getConfiguration(ServiceConstants.PID);
            if (configuration != null) {
                Dictionary dictonary = configuration.getProperties();
                if (dictonary != null) {
                    DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(dictonary);
                    mavenConfiguration = new MavenConfigurationImpl(resolver, ServiceConstants.PID);
                }
            }
        } catch (IOException e) {
            logger.error("Error retrieving maven configuration",e);
        }
        return mavenConfiguration;
    }

    /**
     * Returns the bundles that match
     * @param url
     * @return
     */
    public List<Bundle> getBundlesByURL(String url) {
        List<Bundle> bundleList = new ArrayList<Bundle>();
        try {
            Long id = Long.parseLong(url);
            Bundle bundle = bundleContext.getBundle(id);
            if (bundle != null) {
                bundleList.add(bundle);
            }
        } catch (NumberFormatException e) {
            for (int i = 0; i < bundleContext.getBundles().length; i++) {
                Bundle bundle = bundleContext.getBundles()[i];
                if (isMavenSnapshotUrl(bundle.getLocation()) && wildCardMatch(bundle.getLocation(), url)) {
                    bundleList.add(bundle);
                }
            }
        }
        return bundleList;
    }

    protected boolean isMavenSnapshotUrl(String url) {
        return url.startsWith("mvn:") && url.contains("-SNAPSHOT");
    }

    /**
     * Matches text using a pattern containing wildcards.
     *
     * @param text
     * @param pattern
     * @return
     */
    protected boolean wildCardMatch(String text, String pattern) {
        String[] cards = pattern.split("\\*");
        // Iterate over the cards.
        for (String card : cards) {
            int idx = text.indexOf(card);
            // Card not detected in the text.
            if (idx == -1) {
                return false;
            }

            // Move ahead, towards the right of the text.
            text = text.substring(idx + card.length());
        }
        return true;
    }


    public void start() {
        if (running.compareAndSet(false, true)) {
            if (watchURLs.size()>0) {
                Thread thread = new Thread(this);
                thread.start();
            }
        }
    }

    /**
     * Stops the execution of the thread and releases the singleton instance
     */
    public void stop() {
        running.set(false);
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public List<String> getWatchURLs() {
        return watchURLs;
    }

    public void setWatchURLs(List<String> watchURLs) {
        this.watchURLs = watchURLs;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public boolean isRunning() {
        return running.get();
    }

}
