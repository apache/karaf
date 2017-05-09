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
package org.apache.karaf.features.extension;

import java.util.Arrays;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.SynchronousBundleListener;
import org.osgi.framework.hooks.resolver.ResolverHookFactory;
import org.osgi.framework.wiring.FrameworkWiring;

public class Activator implements BundleActivator, SynchronousBundleListener {
    private static final String WIRING_PATH = "wiring";
    private StoredWiringResolver resolver;
    private BundleContext context;

    @Override
    public void start(BundleContext context) throws Exception {
        this.context = context;
        resolver = new StoredWiringResolver(context.getDataFile(WIRING_PATH).toPath());
        context.addBundleListener(this);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        context.removeBundleListener(this);
    }

    @Override
    public void bundleChanged(BundleEvent event) {
        if (event.getBundle().getBundleId() == 0 && event.getType() == BundleEvent.STARTED) {
            resolveAll();
        } else if (event.getType() == BundleEvent.RESOLVED) {
            resolver.update(event.getBundle());
        } else if (event.getType() == BundleEvent.UNRESOLVED) {
            resolver.delete(event.getBundle());
        }
    }

    private void resolveAll() {
        ServiceRegistration<ResolverHookFactory> registration = context
            .registerService(ResolverHookFactory.class, (triggers) -> resolver, null);
        try {
            context.getBundle().adapt(FrameworkWiring.class)
                .resolveBundles(Arrays.asList(context.getBundles()));
        } finally {
            registration.unregister();
        }
    }
}
