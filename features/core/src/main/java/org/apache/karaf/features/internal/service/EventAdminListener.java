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
package org.apache.karaf.features.internal.service;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.karaf.features.EventConstants;
import org.apache.karaf.features.FeatureEvent;
import org.apache.karaf.features.FeaturesListener;
import org.apache.karaf.features.RepositoryEvent;
import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A listener to publish events to EventAdmin
 */
public class EventAdminListener implements FeaturesListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventAdminListener.class);
    private final ServiceTracker<EventAdmin, EventAdmin> tracker;

    public EventAdminListener(BundleContext context) {
        tracker = new ServiceTracker<>(context, EventAdmin.class.getName(), null);
        tracker.open();
    }

    public void featureEvent(FeatureEvent event) {
        try {
            EventAdmin eventAdmin = tracker.getService();
            if (eventAdmin == null) {
                return;
            }
            Dictionary<String, Object> props = new Hashtable<>();
            props.put(EventConstants.TYPE, event.getType());
            props.put(EventConstants.EVENT, event);
            props.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
            props.put(EventConstants.FEATURE_NAME, event.getFeature().getName());
            props.put(EventConstants.FEATURE_VERSION, event.getFeature().getVersion());
            String topic;
            switch (event.getType()) {
            case FeatureInstalled:
                topic = EventConstants.TOPIC_FEATURES_INSTALLED;
                break;
            case FeatureUninstalled:
                topic = EventConstants.TOPIC_FEATURES_UNINSTALLED;
                break;
            default:
                throw new IllegalStateException("Unknown features event type: " + event.getType());
            }
            eventAdmin.postEvent(new Event(topic, props));
        } catch (IllegalStateException e) {
            LOGGER.warn("Unable to post event to EventAdmin", e);
        }
    }

    public void repositoryEvent(RepositoryEvent event) {
        try {
            EventAdmin eventAdmin = tracker.getService();
            if (eventAdmin == null) {
                return;
            }
            Dictionary<String, Object> props = new Hashtable<>();
            props.put(EventConstants.TYPE, event.getType());
            props.put(EventConstants.EVENT, event);
            props.put(EventConstants.TIMESTAMP, System.currentTimeMillis());
            props.put(EventConstants.REPOSITORY_URI, event.getRepository().getURI().toString());
            String topic;
            switch (event.getType()) {
            case RepositoryAdded:
                topic = EventConstants.TOPIC_REPOSITORY_ADDED;
                break;
            case RepositoryRemoved:
                topic = EventConstants.TOPIC_REPOSITORY_REMOVED;
                break;
            default:
                throw new IllegalStateException("Unknown repository event type: " + event.getType());
            }
            eventAdmin.postEvent(new Event(topic, props));
        } catch (IllegalStateException e) {
            LOGGER.warn("Unable to post event to EventAdmin", e);
        }
    }
}
