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

import java.util.Iterator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

/**
 * Looks up the {@link MavenResolverFactory} provider to use.
 */
public final class MavenResolvers {

    private MavenResolvers() { }

    /**
     * Look up the factory provided by the class loader of this class.
     *
     * @return the factory.
     * @throws IllegalStateException if no provider is available.
     */
    public static MavenResolverFactory factory() {
        return factory(MavenResolvers.class.getClassLoader());
    }

    /**
     * Look up the factory provided by the given class loader.
     *
     * @param classLoader the class loader to search for providers.
     * @return the factory.
     * @throws IllegalStateException if no provider is available.
     */
    public static MavenResolverFactory factory(ClassLoader classLoader) {
        MavenResolverFactory factory = findFactory(classLoader);
        if (factory != null) {
            return factory;
        }
        throw new IllegalStateException("No " + MavenResolverFactory.class.getName()
                + " provider found. Make sure a Maven resolver provider is available.");
    }

    /**
     * Look up a factory without failing when no provider is available.
     *
     * @return the factory, or <code>null</code> if none is available.
     */
    public static MavenResolverFactory findFactory() {
        return findFactory(MavenResolvers.class.getClassLoader());
    }

    /**
     * Look up a factory provided by the given class loader without failing when no provider is available.
     *
     * @param classLoader the class loader to search for providers.
     * @return the factory, or <code>null</code> if none is available.
     */
    public static MavenResolverFactory findFactory(ClassLoader classLoader) {
        try {
            Iterator<MavenResolverFactory> factories =
                    ServiceLoader.load(MavenResolverFactory.class, classLoader).iterator();
            if (factories.hasNext()) {
                return factories.next();
            }
        } catch (ServiceConfigurationError e) {
            throw new IllegalStateException("Unable to load a " + MavenResolverFactory.class.getName()
                    + " provider. Make sure a Maven resolver provider is available.", e);
        }
        return null;
    }

}
