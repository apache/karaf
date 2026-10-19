/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.services.url.internal;

import java.util.Dictionary;

import org.apache.karaf.features.spi.MavenResolver;
import org.apache.karaf.features.spi.MavenResolverFactory;
import org.osgi.framework.BundleContext;

/**
 * {@link MavenResolverFactory} creating the Karaf URL service Maven resolver, configured with the
 * {@link MavenConfiguration#PID} PID.
 */
public class KarafMavenResolverFactory implements MavenResolverFactory {

    private final BundleContext bundleContext;

    /**
     * Used by {@link java.util.ServiceLoader}, outside the OSGi framework: the variables of the configuration are
     * resolved with the system properties only.
     */
    public KarafMavenResolverFactory() {
        this(null);
    }

    public KarafMavenResolverFactory(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    @Override
    public String getConfigurationPid() {
        return MavenConfiguration.PID;
    }

    @Override
    public MavenResolver create(Dictionary<String, String> configuration, String propertyPrefix) {
        return new MavenResolverImpl(new MavenConfiguration(bundleContext, configuration, propertyPrefix));
    }

}
