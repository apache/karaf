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
package org.apache.karaf.service.guard.impl;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// This bundle is quite low-level and benefits from starting early in the process. Therefore it does not depend
// on Blueprint but rather uses direct OSGi framework APIs and Service Trackers ...
public class Activator implements BundleActivator {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    GuardingEventHook guardingEventHook;
    GuardingFindHook guardingFindHook;
    GuardProxyCatalog guardProxyCatalog;

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        String f = System.getProperty(GuardProxyCatalog.KARAF_SECURED_SERVICES_SYSPROP);
        Filter securedServicesFilter;
        if (f == null) {
            // no services need to be secured
            logger.info("No role-based security for services as its system property is not set: {}", GuardProxyCatalog.KARAF_SECURED_SERVICES_SYSPROP);
            return;
        } else {
            securedServicesFilter = bundleContext.createFilter(f);
            logger.info("Adding role-based security to services with filter: {}", f);
        }

        guardProxyCatalog = new GuardProxyCatalog(bundleContext);

        guardingEventHook = new GuardingEventHook(bundleContext, guardProxyCatalog, securedServicesFilter);
        bundleContext.registerService(EventListenerHook.class, guardingEventHook, null);

        guardingFindHook = new GuardingFindHook(bundleContext, guardProxyCatalog, securedServicesFilter);
        bundleContext.registerService(FindHook.class, guardingFindHook, null);

    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        if (guardProxyCatalog != null) {
            guardProxyCatalog.close();
        }
    }

}
