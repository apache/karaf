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
package org.apache.karaf.features.internal.download;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.concurrent.ScheduledExecutorService;

import org.apache.karaf.features.internal.download.impl.KarafMavenArtifactResolver;
import org.apache.karaf.features.internal.download.impl.MavenArtifactResolver;
import org.apache.karaf.features.internal.download.impl.MavenDownloadManager;
import org.apache.karaf.features.internal.download.impl.PaxMavenArtifactResolver;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public final class DownloadManagers {

    private DownloadManagers() { }

    public static DownloadManager createDownloadManager(MavenArtifactResolver resolver, ScheduledExecutorService executorService) {
        return createDownloadManager(resolver, executorService, 0, 0);
    }

    public static DownloadManager createDownloadManager(MavenArtifactResolver resolver, ScheduledExecutorService executorService,
                                                        long scheduleDelay, int scheduleMaxRun) {
        return new MavenDownloadManager(resolver, executorService, scheduleDelay, scheduleMaxRun);
    }

    /**
     * Create the resolver used to download the Maven artifacts: the Maven resolver of the Karaf URL service when it's
     * available, a pax-url-aether resolver otherwise.
     *
     * @param configurationAdmin used to get the pax-url-aether configuration (can be <code>null</code>)
     * @return the resolver
     * @throws IOException if the configuration can't be read
     */
    public static MavenArtifactResolver createMavenResolver(ConfigurationAdmin configurationAdmin) throws IOException {
        Bundle bundle = FrameworkUtil.getBundle(DownloadManagers.class);
        BundleContext bundleContext = bundle != null ? bundle.getBundleContext() : null;
        if (bundleContext != null && bundleContext.getServiceReference(KarafMavenArtifactResolver.SERVICE) != null) {
            return new KarafMavenArtifactResolver(bundleContext);
        }
        return PaxMavenArtifactResolver.create(getConfiguration(configurationAdmin, PaxMavenArtifactResolver.PID));
    }

    private static Dictionary<String, String> getConfiguration(ConfigurationAdmin configurationAdmin, String pid) throws IOException {
        Hashtable<String, String> props = new Hashtable<>();
        if (configurationAdmin != null) {
            Configuration config = configurationAdmin.getConfiguration(pid, null);
            if (config != null) {
                Dictionary<String, Object> cfg = config.getProcessedProperties(null);
                if (cfg != null) {
                    for (Enumeration<String> e = cfg.keys(); e.hasMoreElements(); ) {
                        String key = e.nextElement();
                        Object val = cfg.get(key);
                        if (key != null) {
                            props.put(key, val.toString());
                        }
                    }
                }
            }
        }
        return props;
    }
}
