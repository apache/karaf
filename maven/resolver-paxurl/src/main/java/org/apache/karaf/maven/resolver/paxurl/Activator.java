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

import java.io.IOException;
import java.util.Hashtable;

import org.apache.karaf.features.spi.MavenResolver;
import org.apache.karaf.features.spi.MavenResolverFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * Registers the pax-url backed {@link MavenResolverFactory}, so that the features service can
 * resolve <code>mvn:</code> URIs, along with a ready to use {@link MavenResolver} for the
 * components that just want to resolve an artifact.
 */
public class Activator implements BundleActivator {

    private ServiceRegistration<MavenResolverFactory> factoryRegistration;
    private ServiceRegistration<MavenResolver> resolverRegistration;
    private LazyMavenResolver resolver;

    @Override
    public void start(BundleContext context) {
        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put("provider", "pax-url-aether");

        MavenResolverFactory factory = new PaxUrlMavenResolverFactory();
        factoryRegistration = context.registerService(MavenResolverFactory.class, factory, properties);

        resolver = new LazyMavenResolver(context, factory);
        resolverRegistration = context.registerService(MavenResolver.class, resolver, properties);
    }

    @Override
    public void stop(BundleContext context) {
        if (resolverRegistration != null) {
            resolverRegistration.unregister();
            resolverRegistration = null;
        }
        if (factoryRegistration != null) {
            factoryRegistration.unregister();
            factoryRegistration = null;
        }
        if (resolver != null) {
            try {
                resolver.close();
            } catch (IOException e) {
                // the resolver is going away anyway
            }
            resolver = null;
        }
    }

}
