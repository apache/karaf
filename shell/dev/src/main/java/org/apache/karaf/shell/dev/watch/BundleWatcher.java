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
import org.ops4j.pax.url.maven.commons.MavenConfigurationImpl;
import org.ops4j.pax.url.maven.commons.MavenRepositoryURL;
import org.ops4j.pax.url.mvn.ServiceConstants;
import org.ops4j.pax.url.mvn.internal.Parser;
import org.ops4j.util.property.DictionaryPropertyResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleReference;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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

    private static BundleWatcher instance = null;

    private ConfigurationAdmin configurationAdmin;

    private Boolean running = true;
    private Long interval = 10000L;
    private Map<Bundle, Long> lastModificationTimes = new HashMap<Bundle, Long>();

    /**
     * Construcotr
     */
    private BundleWatcher() {
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Returns the singleton instance.
     *
     * @return
     */
    public static synchronized BundleWatcher getInstance() {
        if (instance == null)
            instance = new BundleWatcher();
        return instance;
    }

    public void run() {
        if (logger.isDebugEnabled()) {
            logger.debug("Bundle watcher thread started");
        }
        running = true;
        while (running) {
            for (Bundle bundle : lastModificationTimes.keySet()) {
                try {
                    Long lastModifiedTime = getBundleLastModifiedTime(bundle);
                    Long oldLastModifiedTime = lastModificationTimes.get(bundle);
                    if (!lastModifiedTime.equals(oldLastModifiedTime)) {
                        URL bundleLocation = new URL(bundle.getLocation());
                        InputStream is = bundleLocation.openStream();
                        bundle.update(is);
                        is.close();
                        lastModificationTimes.put(bundle, lastModifiedTime);
                    }
                } catch (IOException ex) {
                    logger.error("Error watching bundle.", ex);
                } catch (BundleException ex) {
                    logger.error("Error updating bundle.", ex);
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
     * Adds a Bundle to the watch list.
     *
     * @param bundleList
     */
    public void add(List<Bundle> bundleList) {
        for (Bundle bundle : bundleList) {
            lastModificationTimes.put(bundle, getBundleLastModifiedTime(bundle));
        }
    }

    /**
     * Removes a bundle from the watch list.
     *
     * @param bundleList
     */
    public void remove(List<Bundle> bundleList) {
        for (Bundle bundle : bundleList) {
            lastModificationTimes.remove(bundle);
        }
    }

    /**
     * Returns the last modification time of the bundle artifact as Long.
     *
     * @param bundle
     * @return
     */
    private Long getBundleLastModifiedTime(Bundle bundle) {
        BundleContext bundleContext = null;
        ServiceReference ref = null;

        String localRepository = null;
        Long lastModificationTime = 0L;

        try {
            bundleContext = ((BundleReference) getClass().getClassLoader()).getBundle().getBundleContext();
            //Get a reference to the configuration admin.
            ref = bundleContext.getServiceReference(ConfigurationAdmin.class.getName());
            configurationAdmin = (ConfigurationAdmin) bundleContext.getService(ref);

            Configuration configuration = configurationAdmin.getConfiguration(ServiceConstants.PID);

            //Attempt to retrieve local repository location from MavenConfiguration
            if (configuration != null) {
                Dictionary dictionary = configuration.getProperties();
                MavenConfigurationImpl config = new MavenConfigurationImpl(new DictionaryPropertyResolver(dictionary), ServiceConstants.PID);

                MavenRepositoryURL localRepositoryURL = config.getLocalRepository();
                if (localRepositoryURL != null) {
                    localRepository = localRepositoryURL.getFile().getAbsolutePath();
                }
            }

            //If local repository not found assume default.
            if (localRepository == null) {
                localRepository = System.getProperty("user.home") + File.separator + ".m2" + File.separator + "repository";
            }

            Parser p = new Parser(bundle.getLocation().substring(4));
            p.getArtifactPath();

            String bundlePath = localRepository + File.separator + p.getArtifactPath();
            File bundleFile = new File(bundlePath);
            lastModificationTime = bundleFile.lastModified();

        } catch (MalformedURLException e) {
            logger.warn("ConfigAdmin service is unavailable.");
        } catch (IOException e) {
            logger.warn("ConfigAdmin service is unavailable.");
        } finally {
            bundleContext.ungetService(ref);
        }
        return lastModificationTime;
    }


    /**
     * Stops the execution of the thread and releases the singleton instance
     */
    public void stop() {
        setRunning(false);
        this.instance = null;
    }

    /**
     * Returns the list of bundles that are being watched.
     *
     * @return
     */
    public List<Bundle> getWatchList() {
        return new ArrayList(lastModificationTimes.keySet());
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
