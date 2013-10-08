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
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.ListenerHook.ListenerInfo;

class GuardingEventHook implements EventListenerHook {
    private final BundleContext myBundleContext;
    private final GuardProxyCatalog guardProxyCatalog;
    private final Filter servicesFilter;

    GuardingEventHook(BundleContext myBC, GuardProxyCatalog gpc, Filter securedServicesFilter) throws InvalidSyntaxException {
        myBundleContext = myBC;
        guardProxyCatalog = gpc;
        servicesFilter = securedServicesFilter;
    }

    @Override
    public void event(ServiceEvent event, Map<BundleContext, Collection<ListenerInfo>> listeners) {
        if (servicesFilter == null) {
            return;
        }

        ServiceReference<?> sr = event.getServiceReference();
        if (!servicesFilter.match(sr)) {
            return;
        }

        for (Iterator<BundleContext> i = listeners.keySet().iterator(); i.hasNext(); ) {
            BundleContext bc = i.next();
            if (myBundleContext.equals(bc) || bc.getBundle().getBundleId() == 0L) {
                // don't hide anything from this bundle or the system bundle
                continue;
            }

            if (guardProxyCatalog.handleProxificationForHook(sr)) {
                i.remove();
            }
        }
    }
}
