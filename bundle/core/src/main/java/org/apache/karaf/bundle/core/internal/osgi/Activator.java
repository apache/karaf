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
package org.apache.karaf.bundle.core.internal.osgi;

import org.apache.karaf.bundle.core.BundleService;
import org.apache.karaf.bundle.core.BundleStateService;
import org.apache.karaf.bundle.core.BundleWatcher;
import org.apache.karaf.bundle.core.internal.BundleServiceImpl;
import org.apache.karaf.bundle.core.internal.BundleWatcherImpl;
import org.apache.karaf.bundle.core.internal.BundlesMBeanImpl;
import org.apache.karaf.bundle.core.internal.MavenConfigService;
import org.apache.karaf.util.tracker.BaseActivator;
import org.apache.karaf.util.tracker.annotation.ProvideService;
import org.apache.karaf.util.tracker.annotation.RequireService;
import org.apache.karaf.util.tracker.annotation.Services;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

@Services(
        requires = @RequireService(ConfigurationAdmin.class),
        provides = @ProvideService(BundleService.class)
)
public class Activator extends BaseActivator {

    private ServiceTracker<BundleStateService, BundleStateService> bundleStateServicesTracker;
    private BundleWatcherImpl bundleWatcher;

    @Override
    protected void doStart() throws Exception {
        ConfigurationAdmin configurationAdmin = getTrackedService(ConfigurationAdmin.class);
        if (configurationAdmin == null) {
            return;
        }

        final BundleServiceImpl bundleService = new BundleServiceImpl(bundleContext);
        register(BundleService.class, bundleService);
        bundleStateServicesTracker = new ServiceTracker<>(
                bundleContext, BundleStateService.class, new ServiceTrackerCustomizer<BundleStateService, BundleStateService>() {
            @Override
            public BundleStateService addingService(ServiceReference<BundleStateService> reference) {
                BundleStateService service = bundleContext.getService(reference);
                bundleService.registerBundleStateService(service);
                return service;
            }
            @Override
            public void modifiedService(ServiceReference<BundleStateService> reference, BundleStateService service) {
            }
            @Override
            public void removedService(ServiceReference<BundleStateService> reference, BundleStateService service) {
                bundleService.unregisterBundleStateService(service);
                bundleContext.ungetService(reference);
            }
        }
        );
        bundleStateServicesTracker.open();

        bundleWatcher = new BundleWatcherImpl(bundleContext, new MavenConfigService(configurationAdmin), bundleService);
        bundleWatcher.start();
        register(BundleWatcher.class, bundleWatcher);

        BundlesMBeanImpl bundlesMBeanImpl = new BundlesMBeanImpl(bundleContext, bundleService);
        registerMBean(bundlesMBeanImpl, "type=bundle");
    }

    @Override
    protected void doStop() {
        if (bundleStateServicesTracker != null) {
            bundleStateServicesTracker.close();
            bundleStateServicesTracker = null;
        }
        super.doStop();
        if (bundleWatcher != null) {
            bundleWatcher.stop();
            bundleWatcher = null;
        }
    }

}
