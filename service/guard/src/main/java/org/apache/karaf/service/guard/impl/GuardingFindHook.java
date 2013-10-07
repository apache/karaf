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

import java.util.Collection;
import java.util.Iterator;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.FindHook;

class GuardingFindHook implements FindHook {
    private final BundleContext myBundleContext;
    private final GuardProxyCatalog guardProxyCatalog;
    private final Filter servicesFilter;

    GuardingFindHook(BundleContext myBC, GuardProxyCatalog gpc, Filter securedServicesFilter) {
        myBundleContext = myBC;
        guardProxyCatalog = gpc;
        servicesFilter = securedServicesFilter;
    }

    @Override
    public void find(BundleContext context, String name, String filter, boolean allServices,
            Collection<ServiceReference<?>> references) {
        if (servicesFilter == null) {
            return;
        }

        if (myBundleContext.equals(context) || context.getBundle().getBundleId() == 0) {
            // don't hide anything from this bundle or the system bundle
            return;
        }

        for (Iterator<ServiceReference<?>> i = references.iterator(); i.hasNext(); ) {
            ServiceReference<?> sr = i.next();

            if (!servicesFilter.match(sr)) {
                continue;
            }

            if (guardProxyCatalog.handleProxificationForHook(sr)) {
                i.remove();
            }
        }
    }
}
