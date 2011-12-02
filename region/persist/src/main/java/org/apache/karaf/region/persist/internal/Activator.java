/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package org.apache.karaf.region.persist.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.apache.karaf.region.persist.RegionsPersistence;
import org.apache.karaf.region.persist.internal.util.SingleServiceTracker;
import org.eclipse.equinox.region.RegionDigraph;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev:$ $Date:$
 */
public class Activator implements BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(Activator.class);

    private SingleServiceTracker<RegionDigraph> tracker;
    private final AtomicReference<RegionsPersistenceImpl> persistence = new AtomicReference<RegionsPersistenceImpl>();
    private final AtomicReference<RegionsBundleTracker> bundleTracker = new AtomicReference<RegionsBundleTracker>();
    private ServiceRegistration<RegionsPersistence> reg;

    @Override
    public void start(final BundleContext bundleContext) throws Exception {
        tracker = new SingleServiceTracker<RegionDigraph>(bundleContext, RegionDigraph.class, new SingleServiceTracker.SingleServiceListener() {
            public void serviceFound() {
                log.debug("Found RegionDigraph service, initializing");
                RegionDigraph regionDigraph = tracker.getService();
                Bundle framework = bundleContext.getBundle(0);
                RegionsPersistenceImpl persistence = null;
                try {
                    persistence = new RegionsPersistenceImpl(regionDigraph, framework);
                    reg = bundleContext.registerService(RegionsPersistence.class, persistence, null);

                    RegionsBundleTracker bundleTracker = new RegionsBundleTracker();
                    bundleTracker.start(bundleContext, persistence);
                    Activator.this.bundleTracker.set(bundleTracker);
                } catch (Exception e) {
                    log.info("Could not create RegionsPersistenceImpl", e);
                }
                Activator.this.persistence.set(persistence);
            }

            public void serviceLost() {
                if (reg != null) {
                    reg.unregister();
                    reg = null;
                }
                Activator.this.persistence.set(null);
                Activator.this.bundleTracker.set(null);
            }

            public void serviceReplaced() {
                //??
            }
        });
        tracker.open();
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        tracker.close();
        persistence.set(null);
        bundleTracker.set(null);
    }


}
