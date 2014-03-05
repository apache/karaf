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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Track multiple service by its type.
 *
 * @param <T>
 */
public final class MultiServiceTracker<T> {

    private final BundleContext ctx;
    private final String className;
    private final List<T> services = new CopyOnWriteArrayList<T>();
    private final List<ServiceReference> refs = new CopyOnWriteArrayList<ServiceReference>();
    private final AtomicBoolean open = new AtomicBoolean(false);
    private final Satisfiable serviceListener;
    private Filter filter;

    private final ServiceListener listener = new ServiceListener() {
        public void serviceChanged(ServiceEvent event) {
            if (open.get()) {
                if (event.getType() == ServiceEvent.UNREGISTERING) {
                    removeRef(event.getServiceReference());
                } else if (event.getType() == ServiceEvent.REGISTERED) {
                    addRef(event.getServiceReference());
                }
            }
        }
    };

    public MultiServiceTracker(BundleContext context, Class<T> clazz, Satisfiable sl) {
        ctx = context;
        this.className = clazz.getName();
        serviceListener = sl;
    }

    public List<T> getServices() {
        return services;
    }

    public List<ServiceReference> getServiceReferences() {
        return refs;
    }

    public void open() {
        if (open.compareAndSet(false, true)) {
            try {
                String filterString = '(' + Constants.OBJECTCLASS + '=' + className + ')';
                if (filter != null) filterString = "(&" + filterString + filter + ')';
                ctx.addServiceListener(listener, filterString);
                ServiceReference[] refs = ctx.getServiceReferences(className, filter != null ? filter.toString() : null);
                if (refs != null) {
                    for (ServiceReference ref : refs) {
                        addRef(ref);
                    }
                }
            } catch (InvalidSyntaxException e) {
                // this can never happen. (famous last words :)
            }
            serviceListener.found();
        }
    }

    private void addRef(ServiceReference ref) {
        T service = (T) ctx.getService(ref);
        synchronized (refs) {
            if (refs.add(ref)) {
                services.add(service);
                return;
            }
        }
        ctx.ungetService(ref);
        serviceListener.updated();
    }

    private void removeRef(ServiceReference ref) {
        synchronized (refs) {
            if (!refs.remove(ref)) {
                return;
            }
        }
        ctx.ungetService(ref);
        serviceListener.updated();
    }

    public void close() {
        if (open.compareAndSet(true, false)) {
            ctx.removeServiceListener(listener);

            List<ServiceReference> oldRefs;
            synchronized (refs) {
                oldRefs = new ArrayList<ServiceReference>(refs);
                refs.clear();
                services.clear();
            }
            for (ServiceReference ref : oldRefs) {
                ctx.ungetService(ref);
            }
        }
    }

    public boolean isSatisfied() {
        return true;
    }

    public String getClassName() {
        return className;
    }

}
