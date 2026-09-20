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
package org.apache.karaf.maven.resolver.paxurl;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

import org.apache.karaf.features.spi.MavenResolver;
import org.apache.karaf.features.spi.MavenResolverFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * A {@link MavenResolver} that builds its delegate on first use, from the ConfigAdmin
 * configuration of the factory's PID.
 *
 * <p>Creating it lazily means this bundle does not have to wait for ConfigAdmin to start, and
 * configuration changes made before the first resolution are picked up.</p>
 */
public class LazyMavenResolver implements MavenResolver {

    private final BundleContext bundleContext;
    private final MavenResolverFactory factory;

    private volatile MavenResolver delegate;

    public LazyMavenResolver(BundleContext bundleContext, MavenResolverFactory factory) {
        this.bundleContext = bundleContext;
        this.factory = factory;
    }

    private MavenResolver delegate() throws IOException {
        MavenResolver resolver = delegate;
        if (resolver == null) {
            synchronized (this) {
                resolver = delegate;
                if (resolver == null) {
                    resolver = factory.create(readConfiguration());
                    delegate = resolver;
                }
            }
        }
        return resolver;
    }

    private Dictionary<String, String> readConfiguration() throws IOException {
        Hashtable<String, String> properties = new Hashtable<>();
        ServiceReference<ConfigurationAdmin> reference =
                bundleContext.getServiceReference(ConfigurationAdmin.class);
        if (reference == null) {
            return properties;
        }
        try {
            ConfigurationAdmin configurationAdmin = bundleContext.getService(reference);
            if (configurationAdmin == null) {
                return properties;
            }
            Configuration configuration = configurationAdmin.getConfiguration(factory.getConfigurationPid(), null);
            if (configuration != null) {
                Dictionary<String, Object> cfg = configuration.getProcessedProperties(null);
                if (cfg != null) {
                    for (Enumeration<String> keys = cfg.keys(); keys.hasMoreElements(); ) {
                        String key = keys.nextElement();
                        Object value = cfg.get(key);
                        if (key != null && value != null) {
                            properties.put(key, value.toString());
                        }
                    }
                }
            }
            return properties;
        } finally {
            bundleContext.ungetService(reference);
        }
    }

    @Override
    public File resolve(String url) throws IOException {
        return delegate().resolve(url);
    }

    @Override
    public File resolve(String url, Exception previousException) throws IOException {
        return delegate().resolve(url, previousException);
    }

    @Override
    public File resolve(String groupId, String artifactId, String classifier, String extension, String version)
            throws IOException {
        return delegate().resolve(groupId, artifactId, classifier, extension, version);
    }

    @Override
    public RetryChance isRetryableException(Exception exception) {
        MavenResolver resolver = delegate;
        return resolver == null ? RetryChance.UNKNOWN : resolver.isRetryableException(exception);
    }

    @Override
    public void close() throws IOException {
        MavenResolver resolver = delegate;
        delegate = null;
        if (resolver != null) {
            resolver.close();
        }
    }

}
