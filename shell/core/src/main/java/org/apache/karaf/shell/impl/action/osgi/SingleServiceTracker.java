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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Track a single service by its type.
 * When tracking a single service, the dependency is always considered mandatory.
 *
 * @param <T> the service type (interface).
 */
public abstract class SingleServiceTracker<T> {

    private final BundleContext ctx;
    private final Class<T> clazz;
    private String filter;
    private final AtomicReference<T> service = new AtomicReference<>();
    private final AtomicReference<ServiceReference<T>> ref = new AtomicReference<>();
    private final AtomicBoolean open = new AtomicBoolean(false);

    private final ServiceListener listener = event -> {
        if (open.get()) {
            if (event.getType() == ServiceEvent.UNREGISTERING) {
                ServiceReference<T> deadRef = (ServiceReference<T>) event.getServiceReference();
                if (deadRef.equals(ref.get())) {
                    findMatchingReference(deadRef);
                }
            } else if (event.getType() == ServiceEvent.REGISTERED && ref.get() == null) {
                findMatchingReference(null);
            }
        }
    };

    public SingleServiceTracker(BundleContext context, Class<T> clazz, String filter) {
        ctx = context;
        this.clazz = clazz;
        this.filter = filter;
    }

    protected abstract void updateState(T oldSvc, T newSvc);

    public void open() {
        if (open.compareAndSet(false, true)) {
            try {
                String filterString = '(' + Constants.OBJECTCLASS + '=' + clazz.getName() + ')';
                if (filter != null && !filter.isEmpty()) {
                    filterString = "(&" + filterString + filter + ')';
                }
                ctx.addServiceListener(listener, filterString);
                findMatchingReference(null);
            } catch (InvalidSyntaxException e) {
                // this can never happen. (famous last words :)
            }
        }
    }

    public void close() {
        if (open.compareAndSet(true, false)) {
            ctx.removeServiceListener(listener);

            synchronized (this) {
                ServiceReference<T> deadRef = ref.getAndSet(null);
                service.set(null);
                if (deadRef != null) ctx.ungetService(deadRef);
            }
        }
    }

    private void findMatchingReference(ServiceReference<T> original) {
        boolean clear = true;
        ServiceReference<T> ref = ctx.getServiceReference(clazz);
        if (ref != null) {
            T service = ctx.getService(ref);
            if (service != null) {
                clear = false;

                // We do the unget out of the lock so we don't exit this class while holding a lock.
                if (!!!update(original, ref, service)) {
                    ctx.ungetService(ref);
                }
            }
        } else if (original == null) {
            clear = false;
        }

        if (clear) {
            update(original, null, null);
        }
    }

    private boolean update(ServiceReference<T> deadRef, ServiceReference<T> newRef, T service) {
        boolean result = false;

        // Make sure we don't try to get a lock on null
        Object lock;
        // we have to choose our lock.
        if (newRef != null) lock = newRef;
        else if (deadRef != null) lock = deadRef;
        else lock = this;

        T old = null;
        // This lock is here to ensure that no two threads can set the ref and service
        // at the same time.
        synchronized (lock) {
            if (open.get()) {
                result = this.ref.compareAndSet(deadRef, newRef);
                if (result) {
                    old = this.service.getAndSet(service);
                }
            }
        }

        updateState(old, service);

        return result;
    }

    public Class<?> getTrackedClass() {
        return clazz;
    }

}
