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
package org.apache.karaf.features.internal.osgi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.karaf.features.spi.MavenResolverFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

/**
 * Tracks the {@link MavenResolverFactory} services and selects the one used by the features service.
 * <p>
 * As with {@link BundleContext#getServiceReference(Class)}, the factory with the highest ranking is used and, for the
 * same ranking, the first registered one: registering another provider (installing the <code>url</code> feature next
 * to Pax URL for instance) does not change the factory in use, and when the factory in use goes away, the features
 * service falls back to the best remaining one.
 */
class MavenResolverFactoryCustomizer implements ServiceTrackerCustomizer<MavenResolverFactory, MavenResolverFactory> {

    private final BundleContext bundleContext;
    private final Consumer<MavenResolverFactory> listener;
    private final Map<ServiceReference<MavenResolverFactory>, MavenResolverFactory> factories = new HashMap<>();
    private MavenResolverFactory selected;

    MavenResolverFactoryCustomizer(BundleContext bundleContext, Consumer<MavenResolverFactory> listener) {
        this.bundleContext = bundleContext;
        this.listener = listener;
    }

    @Override
    public MavenResolverFactory addingService(ServiceReference<MavenResolverFactory> reference) {
        MavenResolverFactory factory = bundleContext.getService(reference);
        if (factory != null) {
            synchronized (factories) {
                factories.put(reference, factory);
                select();
            }
        }
        return factory;
    }

    @Override
    public void modifiedService(ServiceReference<MavenResolverFactory> reference, MavenResolverFactory factory) {
        // the ranking may have changed
        synchronized (factories) {
            select();
        }
    }

    @Override
    public void removedService(ServiceReference<MavenResolverFactory> reference, MavenResolverFactory factory) {
        synchronized (factories) {
            factories.remove(reference);
            select();
        }
        bundleContext.ungetService(reference);
    }

    private void select() {
        // ServiceReference.compareTo() orders by ranking, then by reverse service id
        MavenResolverFactory best = factories.isEmpty() ? null : factories.get(Collections.max(factories.keySet()));
        if (best != selected) {
            selected = best;
            listener.accept(best);
        }
    }

}
