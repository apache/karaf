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
package org.apache.karaf.shell.impl.action.osgi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Track multiple service by its type.
 * When tracking multiple services, the dependency is always considered optional.
 *
 * @param <T> the service type (interface)..
 */
public abstract class MultiServiceTracker<T> {

    private final BundleContext ctx;
    private final Class<T> clazz;
    private String filter;
    private final Map<ServiceReference<T>, T> refs = new HashMap<>();
    private final AtomicBoolean open = new AtomicBoolean(false);

    private final ServiceListener listener = event -> {
        if (open.get()) {
            if (event.getType() == ServiceEvent.UNREGISTERING) {
                removeRef((ServiceReference<T>) event.getServiceReference());
            } else if (event.getType() == ServiceEvent.REGISTERED) {
                addRef((ServiceReference<T>) event.getServiceReference());
            }
            updateState();
        }
    };

    public MultiServiceTracker(BundleContext context, Class<T> clazz, String filter) {
        ctx = context;
        this.clazz = clazz;
        this.filter = filter;
    }

    protected abstract void updateState(List<T> services);

    public void open() {
        if (open.compareAndSet(false, true)) {
            try {
                String filterString = '(' + Constants.OBJECTCLASS + '=' + clazz.getName() + ')';
                if (filter != null && !filter.isEmpty()) {
                    filterString = "(&" + filterString + filter + ')';
                }
                ctx.addServiceListener(listener, filterString);
                Collection<ServiceReference<T>> refs = ctx.getServiceReferences(clazz, null);
                if (refs != null) {
                    for (ServiceReference<T> ref : refs) {
                        addRef(ref);
                    }
                }
            } catch (InvalidSyntaxException e) {
                // this can never happen. (famous last words :)
            }
            updateState();
        }
    }

    @SuppressWarnings("rawtypes")
    public void close() {
        if (open.compareAndSet(true, false)) {
            ctx.removeServiceListener(listener);

            List<ServiceReference> oldRefs;
            synchronized (refs) {
                oldRefs = new ArrayList<>(refs.keySet());
                refs.clear();
            }
            for (ServiceReference ref : oldRefs) {
                ctx.ungetService(ref);
            }
        }
    }

    private void updateState() {
        List<T> svcs;
        synchronized (refs) {
            svcs = new ArrayList<>(refs.values());
        }
        updateState(svcs);
    }

    private void addRef(ServiceReference<T> ref) {
        T service = ctx.getService(ref);
        synchronized (refs) {
            if (!refs.containsKey(ref)) {
                refs.put(ref, service);
                return;
            }
        }
        ctx.ungetService(ref);
    }

    private void removeRef(ServiceReference<T> ref) {
        synchronized (refs) {
            if (refs.remove(ref) == null) {
                return;
            }
        }
        ctx.ungetService(ref);
    }

}
