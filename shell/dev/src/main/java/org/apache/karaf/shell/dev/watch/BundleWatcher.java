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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ops4j.pax.url.maven.commons.MavenConfiguration;
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.Parser;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Runnable singleton which watches at the defined location for bundle updates.
 */
public class BundleWatcher implements Runnable {

    private static Log logger = LogFactory.getLog(BundleWatcher.class);

    private ConfigurationAdmin configurationAdmin;

    private Boolean running = false;
    private Long interval = 10000L;
    private List<String> watchURLs = new ArrayList<String>();
    private Map<Bundle, Long> lastModificationTimes = new HashMap<Bundle, Long>();


    /**
     * Construcotr
     */
    public BundleWatcher() {
    }

    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Bundle watcher thread started");
        }
        while (running) {
            for (String bundleURL : watchURLs) {
                for (Bundle bundle : getBundlesByURL(bundleURL)) {
                    try {
                        Long lastModifiedTime = getBundleLastModifiedTime(bundle);
                        Long oldLastModifiedTime = lastModificationTimes.get(bundle);
                        if (!lastModifiedTime.equals(oldLastModifiedTime)) {
                            String externalLocation = getBundleExternalLocation(bundle);
                            if (externalLocation != null) {
                                File f = new File(externalLocation);
                                InputStream is = new FileInputStream(f);
                                bundle.update(is);
                                is.close();
                                lastModificationTimes.put(bundle, lastModifiedTime);
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
            } catch (Exception ex) {
                running = false;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Bundle watcher thread stopped");
        }
    }

    /**
     * Adds a Bundle URLs to the watch list.
     * @param urls
     */
    public void add(String urls) {
        watchURLs.add(urls);
        for (Bundle bundle : getBundlesByURL(urls)) {
            lastModificationTimes.put(bundle, getBundleLastModifiedTime(bundle));
        }
    }

    /**
     * Removes a bundle URLs from the watch list.
     * @param urls
     */
    public void remove(String urls) {
        watchURLs.remove(urls);
        for (Bundle bundle : getBundlesByURL(urls)) {
            lastModificationTimes.remove(bundle);
        }
    }

    /**
     * Returns the last modification time of the bundle artifact as Long.
     * @param bundle
     * @return
     */
    private Long getBundleLastModifiedTime(Bundle bundle) {
        Long lastModificationTime = 0L;
        String bundleExternalLocation = getBundleExternalLocation(bundle);
        if (bundleExternalLocation != null) {
            File bundleFile = new File(bundleExternalLocation);
            lastModificationTime = bundleFile.lastModified();
        }
        return lastModificationTime;
    }

    /**
     * Returns the location of the Bundle inside the local maven repository.
     * @param bundle
     * @return
     */
    public String getBundleExternalLocation(Bundle bundle) {
        Parser p = null;
        String bundleExternalLocation = null;
        String localRepository = null;

        //Attempt to retrieve local repository location from MavenConfiguration
        MavenConfiguration configuration = retrieveMavenConfiguration();
        if (configuration != null) {
            MavenRepositoryURL localRepositoryURL = configuration.getLocalRepository();
            if (localRepositoryURL != null) {
                localRepository = localRepositoryURL.getFile().getAbsolutePath();
            }
        }

        //If local repository not found assume default.
        if (localRepository == null) {
            localRepository = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
        }

        try {
            p = new Parser(bundle.getLocation().substring(4));
            bundleExternalLocation = localRepository + File.separator + p.getArtifactPath();
        } catch (MalformedURLException e) {
            logger.error("Could not parse artifact path for bundle" + bundle.getSymbolicName(), e);
        }
        return bundleExternalLocation;
    }


    public MavenConfiguration retrieveMavenConfiguration() {
        MavenConfiguration mavenConfiguration=null;

        try {
            Configuration configuration = configurationAdmin.getConfiguration(ServiceConstants.PID);
            if(configuration != null){
              Dictionary dictonary = configuration.getProperties();
              if(dictonary != null) {
                  DictionaryPropertyResolver resolver = new DictionaryPropertyResolver(dictonary);
                  mavenConfiguration = new MavenConfigurationImpl(resolver,ServiceConstants.PID);
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
        BundleContext bundleContext = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();
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
                if (wildCardMatch(bundle.getLocation(), url)) {
                    bundleList.add(bundle);
                }
            }
        }
        return bundleList;
    }

    /**
     * Matches text using a pattern containing wildchards.
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
        if (!running) {
            Thread thread = new Thread(this);
            setRunning(true);
            thread.start();
        }
    }

    /**
     * Stops the execution of the thread and releases the singleton instance
     */
    public void stop() {
        setRunning(false);
    }

    public ConfigurationAdmin getConfigurationAdmin() {
        return configurationAdmin;
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    public List<String> getWatchURLs() {
        return watchURLs;
    }

    public void setWatchURLs(List<String> watchURLs) {
        this.watchURLs = watchURLs;
    }

    public Long getInterval() {
        return interval;
    }

    public void setInterval(Long interval) {
        this.interval = interval;
    }

    public Boolean isRunning() {
        return running;
    }

    public void setRunning(Boolean running) {
        this.running = running;
    }
}
