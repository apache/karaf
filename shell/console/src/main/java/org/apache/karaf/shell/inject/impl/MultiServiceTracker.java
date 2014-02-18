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
package org.apache.karaf.shell.inject.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.osgi.framework.BundleContext;

/**
 * Track multiple services by their type
 */
public class MultiServiceTracker {

    private final BundleContext bundleContext;
    private final Satisfiable satisfiable;
    private final ConcurrentMap<Class, SingleServiceTracker> trackers = new ConcurrentHashMap<Class, SingleServiceTracker>();
    private final AtomicInteger count = new AtomicInteger();

    public MultiServiceTracker(BundleContext bundleContext, Satisfiable satisfiable) {
        this.bundleContext = bundleContext;
        this.satisfiable = satisfiable;
    }

    @SuppressWarnings("unchecked")
    public void track(Class service) {
        if (trackers.get(service) == null) {
            SingleServiceTracker tracker = new SingleServiceTracker(bundleContext, service, new Satisfiable() {
                @Override
                public void found() {
                    if (count.incrementAndGet() == trackers.size()) {
                        satisfiable.found();
                    }
                }

                @Override
                public void updated() {
                    if (count.get() == trackers.size()) {
                        satisfiable.updated();
                    }
                }

                @Override
                public void lost() {
                    if (count.getAndDecrement() == trackers.size()) {
                        satisfiable.lost();
                    }
                }

            });
            trackers.putIfAbsent(service, tracker);
        }
    }

    public <T> T getService(Class<T> clazz) {
        SingleServiceTracker tracker = trackers.get(clazz);
        return tracker != null ? clazz.cast(tracker.getService()) : null;
    }

    public void open() {
        for (SingleServiceTracker tracker : trackers.values()) {
            tracker.open();
        }
    }

    public void close() {
        for (SingleServiceTracker tracker : trackers.values()) {
            tracker.close();
        }
    }

}
