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
package org.apache.karaf.services.web.internal.wab;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.karaf.services.web.internal.ModelListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

/**
 * Tracks the web application bundles.
 */
public class WabExtender implements BundleTrackerCustomizer<WebApplication> {

    private final BundleTracker<WebApplication> tracker;
    private final ModelListener listener;
    // updated before the listener is notified, the tracker only knows a bundle once addingBundle returns
    private final Map<Bundle, WebApplication> applications = new ConcurrentHashMap<>();

    public WabExtender(BundleContext bundleContext, ModelListener listener) {
        this.listener = listener;
        this.tracker = new BundleTracker<>(bundleContext, Bundle.STARTING | Bundle.ACTIVE, this);
    }

    public void open() {
        tracker.open();
    }

    public void close() {
        tracker.close();
    }

    /**
     * @return the web application bundles ready to be deployed, ordered by bundle id.
     */
    public List<WebApplication> getApplications() {
        List<WebApplication> ready = new ArrayList<>();
        for (WebApplication application : applications.values()) {
            if (application.isReady()) {
                ready.add(application);
            }
        }
        ready.sort(Comparator.comparingLong(a -> a.getBundle().getBundleId()));
        return ready;
    }

    @Override
    public WebApplication addingBundle(Bundle bundle, BundleEvent event) {
        String contextPath = bundle.getHeaders("").get(WebApplication.WEB_CONTEXT_PATH);
        if (contextPath == null) {
            return null;
        }
        WebApplication application = new WebApplication(bundle, contextPath);
        applications.put(bundle, application);
        listener.changed(false);
        return application;
    }

    @Override
    public void modifiedBundle(Bundle bundle, BundleEvent event, WebApplication application) {
        listener.changed(false);
    }

    @Override
    public void removedBundle(Bundle bundle, BundleEvent event, WebApplication application) {
        applications.remove(bundle);
        listener.changed(true);
    }

}
