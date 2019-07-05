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
package org.apache.karaf.management.internal;

import org.osgi.framework.BundleContext;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.util.tracker.ServiceTracker;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class EventAdminLoggerImpl implements EventAdminLogger {

    private final ServiceTracker<EventAdmin, EventAdmin> tracker;

    public EventAdminLoggerImpl(BundleContext context) {
        this.tracker = new ServiceTracker<>(context, EventAdmin.class.getName(), null);
        this.tracker.open();
    }

    public void close() {
        this.tracker.close();
    }

    public void log(String methodName, String[] signature, Object result, Throwable error, Object... params) {
        EventAdmin admin = tracker.getService();
        if (admin != null) {
            Map<String, Object> props = new HashMap<>();
            props.put("method", methodName);
            props.put("signature", signature);
            props.put("params", params);
            if (result != null) {
                props.put("result", result);
            }
            if (error != null) {
                props.put("error", error);
            }
            Event event = new Event("javax/management/MBeanServer/" + methodName.toUpperCase(Locale.ENGLISH), props);
            admin.postEvent(event);
        }
    }

}
