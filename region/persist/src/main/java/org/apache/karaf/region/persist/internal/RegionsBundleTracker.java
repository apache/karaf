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

import org.apache.karaf.region.persist.RegionsPersistence;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @version $Rev:$ $Date:$
 */
public class RegionsBundleTracker {
    private static final Logger log = LoggerFactory.getLogger(RegionsBundleTracker.class);

    private BundleTracker bundleTracker;
    private RegionsPersistence regionsPersistence;

    void start(BundleContext bundleContext, RegionsPersistence regionsPersistence) {
        this.regionsPersistence = regionsPersistence;
        int stateMask = Bundle.INSTALLED;
        bundleTracker = new BundleTracker(bundleContext, stateMask, new BundleTrackerCustomizer() {
            @Override
            public Object addingBundle(Bundle bundle, BundleEvent bundleEvent) {
                return RegionsBundleTracker.this.addingBundle(bundle);
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent bundleEvent, Object o) {
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent bundleEvent, Object o) {
            }
        });
        bundleTracker.open();
    }

    private Object addingBundle(Bundle bundle) {
        String region = bundle.getHeaders().get("Region");
        if (region != null) {
            try {
                regionsPersistence.install(bundle, region);
                log.debug("Installed bundle " + bundle + " in region " + region);
                return bundle;
            } catch (BundleException e) {
                log.info("Could not install bundle " + bundle + " in region " + region, e);
            }
        }
        return null;
    }

    void stop() {
        bundleTracker.close();
    }

}
