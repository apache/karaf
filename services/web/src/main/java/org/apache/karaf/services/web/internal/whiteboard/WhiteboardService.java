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
package org.apache.karaf.services.web.internal.whiteboard;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * A service registered for the whiteboard: a servlet, a resource, a filter, a listener or a servlet context
 * helper.
 * <p>
 * A modification of the service properties increments the change count, so that the deployed element is
 * replaced.
 */
public final class WhiteboardService implements Comparable<WhiteboardService> {

    public enum Kind {
        SERVLET, RESOURCE, FILTER, LISTENER, HELPER
    }

    private final Kind kind;
    private final ServiceReference<?> reference;
    private volatile int changeCount;

    WhiteboardService(Kind kind, ServiceReference<?> reference) {
        this.kind = kind;
        this.reference = reference;
    }

    public Kind getKind() {
        return kind;
    }

    public ServiceReference<?> getReference() {
        return reference;
    }

    public long getServiceId() {
        return (Long) reference.getProperty(Constants.SERVICE_ID);
    }

    public Bundle getBundle() {
        return reference.getBundle();
    }

    public long getBundleId() {
        Bundle bundle = reference.getBundle();
        return bundle == null ? -1 : bundle.getBundleId();
    }

    int getChangeCount() {
        return changeCount;
    }

    void changed() {
        changeCount++;
    }

    /**
     * @return a key identifying this service and its properties, a modification of the properties changing it.
     */
    String key() {
        return getServiceId() + ":" + changeCount;
    }

    public String getString(String key) {
        Object value = reference.getProperty(key);
        if (value == null) {
            return null;
        }
        String[] values = toStrings(value);
        return values.length == 0 ? null : values[0];
    }

    public String[] getStrings(String key) {
        Object value = reference.getProperty(key);
        return value == null ? new String[0] : toStrings(value);
    }

    public boolean getBoolean(String key) {
        String value = getString(key);
        return value != null && Boolean.parseBoolean(value.trim());
    }

    /**
     * @return the properties with the given prefix, without the prefix, as init parameters.
     */
    public Map<String, String> getInitParameters(String prefix) {
        Map<String, String> parameters = new LinkedHashMap<>();
        for (String key : reference.getPropertyKeys()) {
            if (key.startsWith(prefix) && key.length() > prefix.length()) {
                parameters.put(key.substring(prefix.length()), getString(key));
            }
        }
        return parameters;
    }

    private static String[] toStrings(Object value) {
        List<String> values = new ArrayList<>();
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                addValue(values, item);
            }
        } else if (value.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(value); i++) {
                addValue(values, Array.get(value, i));
            }
        } else {
            addValue(values, value);
        }
        return values.toArray(new String[0]);
    }

    private static void addValue(List<String> values, Object item) {
        if (item != null && !item.toString().trim().isEmpty()) {
            values.add(item.toString().trim());
        }
    }

    /**
     * Orders the services by ranking, the highest first, then by service id, the lowest first.
     */
    @Override
    public int compareTo(WhiteboardService other) {
        return other.reference.compareTo(reference);
    }

    @Override
    public String toString() {
        return kind + " " + getServiceId();
    }

}
