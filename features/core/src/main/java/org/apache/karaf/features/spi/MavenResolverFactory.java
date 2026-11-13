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
package org.apache.karaf.features.spi;

import java.util.Dictionary;

/**
 * Creates {@link MavenResolver} instances.
 *
 * <p>Providers are discovered with {@link java.util.ServiceLoader} (see
 * {@link MavenResolvers}), so a provider must declare itself in
 * <code>META-INF/services/org.apache.karaf.features.spi.MavenResolverFactory</code>.</p>
 */
public interface MavenResolverFactory {

    /**
     * The ConfigAdmin PID holding this provider's configuration. It is also the default
     * prefix of the configuration properties.
     *
     * @return the configuration PID.
     */
    String getConfigurationPid();

    /**
     * Create a resolver.
     *
     * @param configuration the resolver configuration.
     * @param propertyPrefix the prefix of the keys to read from <code>configuration</code>.
     * @return a new resolver.
     */
    MavenResolver create(Dictionary<String, String> configuration, String propertyPrefix);

    /**
     * Create a resolver, reading the configuration under {@link #getConfigurationPid()}.
     *
     * @param configuration the resolver configuration.
     * @return a new resolver.
     */
    default MavenResolver create(Dictionary<String, String> configuration) {
        return create(configuration, getConfigurationPid());
    }

}
