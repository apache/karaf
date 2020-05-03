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
package org.apache.karaf.service.interceptor.impl.runtime.hook;

import static org.apache.karaf.service.interceptor.impl.runtime.ComponentProperties.INTERCEPTORS_PROPERTY;

import java.util.Collection;
import java.util.Map;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.hooks.service.EventListenerHook;
import org.osgi.framework.hooks.service.FindHook;
import org.osgi.framework.hooks.service.ListenerHook;

public class InterceptedInstancesHooks implements FindHook, EventListenerHook {
    private final long bundleId;

    public InterceptedInstancesHooks(final long bundleId) {
        this.bundleId = bundleId;
    }

    // replaced services are not forward to listeners except the bundle owning the replacer and #0 (optional for the test)
    @Override
    public void event(final ServiceEvent event, final Map<BundleContext, Collection<ListenerHook.ListenerInfo>> listeners) {
        if (isIntercepted(event.getServiceReference())) {
            listeners.keySet().removeIf(this::isNeitherFrameworkNorSelf);
        }
    }

    // remove replaced services to keep only replacements
    @Override
    public void find(final BundleContext context, final String name, final String filter,
                     final boolean allServices, final Collection<ServiceReference<?>> references) {
        if (isNeitherFrameworkNorSelf(context)) {
            references.removeIf(this::isIntercepted);
        }
    }

    private boolean isNeitherFrameworkNorSelf(final BundleContext b) {
        final long id = b.getBundle().getBundleId();
        return id != 0 && id != bundleId;
    }

    private boolean isIntercepted(final ServiceReference<?> serviceReference) {
        return Boolean.parseBoolean(String.valueOf(serviceReference.getProperty(INTERCEPTORS_PROPERTY)));
    }
}
