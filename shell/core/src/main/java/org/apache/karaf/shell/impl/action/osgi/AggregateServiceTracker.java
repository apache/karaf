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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.osgi.framework.BundleContext;

/**
 * Track multiple services by their type
 */
@SuppressWarnings("rawtypes")
public abstract class AggregateServiceTracker {

    private final BundleContext bundleContext;
    private final ConcurrentMap<Class, SingleServiceTracker> singleTrackers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class, MultiServiceTracker> multiTrackers = new ConcurrentHashMap<>();
    private final ConcurrentMap<Class, Boolean> optional = new ConcurrentHashMap<>();
    private volatile State state = new State();
    private volatile boolean opening = true;

    public AggregateServiceTracker(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public <T> void trackList(final Class<T> service, String filter) {
        if (multiTrackers.get(service) == null) {
            MultiServiceTracker<T> tracker = new MultiServiceTracker<T>(bundleContext, service, filter) {
                @Override
                public void updateState(List<T> services) {
                    updateStateMulti(service, services);
                }
            };
            multiTrackers.put(service, tracker);
        }
    }

    public <T> void trackSingle(final Class<T> service, boolean optional, String filter) {
        this.optional.merge(service, optional, Boolean::logicalAnd);
        if (singleTrackers.get(service) == null) {
            SingleServiceTracker<T> tracker = new SingleServiceTracker<T>(bundleContext, service, filter) {
                @Override
                public void updateState(T oldSvc, T newSvc) {
                    updateStateSingle(service, newSvc);
                }
            };
            singleTrackers.putIfAbsent(service, tracker);
        }
    }

    
    public State open() {
        for (SingleServiceTracker tracker : singleTrackers.values()) {
            tracker.open();
        }
        for (MultiServiceTracker tracker : multiTrackers.values()) {
            tracker.open();
        }
        State state;
        synchronized (this) {
            state = this.state;
            this.opening = false;
        }
        return state;
    }

    public void close() {
        updateState(null);
        for (MultiServiceTracker tracker : multiTrackers.values()) {
            tracker.close();
        }
        for (SingleServiceTracker tracker : singleTrackers.values()) {
            tracker.close();
        }
    }

    protected abstract void updateState(State state);

    private <T> void updateStateMulti(Class<T> serviceClass, List<T> services) {
        State newState = new State();
        synchronized (this) {
            newState.multi.putAll(state.multi);
            newState.single.putAll(state.single);
            newState.multi.put(serviceClass, services);
            this.state = newState;
        }
        updateState(newState);
    }

    private <T> void updateStateSingle(Class<T> serviceClass, T service) {
        State newState = new State();
        boolean opening;
        synchronized (this) {
            newState.multi.putAll(state.multi);
            newState.single.putAll(state.single);
            if (service != null) {
                newState.single.put(serviceClass, service);
            } else {
                newState.single.remove(serviceClass);
            }
            this.state = newState;
            opening = this.opening;
        }
        if (!opening) {
            updateState(newState);
        }
    }

    public class State {

        private final Map<Class, List> multi = new HashMap<>();
        private final Map<Class, Object> single = new HashMap<>();

        public boolean isSatisfied() {
            return singleTrackers.keySet().stream()
                    .noneMatch(c -> !optional.get(c) && !single.containsKey(c));
        }

        public Map<Class, Object> getSingleServices() {
            return single;
        }

        public Map<Class, List> getMultiServices() {
            return multi;
        }

        public List<String> getMissingServices() {
            List<String> missing = new ArrayList<>();
            for (SingleServiceTracker tracker : singleTrackers.values()) {
                if (!single.containsKey(tracker.getTrackedClass())) {
                    missing.add(tracker.getTrackedClass().getName());
                }
            }
            return missing;
        }

    }

}
