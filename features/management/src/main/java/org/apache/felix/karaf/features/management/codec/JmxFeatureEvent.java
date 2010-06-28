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
package org.apache.felix.karaf.features.management.codec;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.felix.karaf.features.FeatureEvent;
import org.apache.felix.karaf.features.management.FeaturesServiceMBean;

public class JmxFeatureEvent {

    public static final CompositeType FEATURE_EVENT;

    private final CompositeData data;

    public JmxFeatureEvent(FeatureEvent event) {
        try {
            String[] itemNames = FeaturesServiceMBean.FEATURE_EVENT;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = event.getFeature().getName();
            itemValues[1] = event.getFeature().getVersion();
            switch (event.getType()) {
                case FeatureInstalled:   itemValues[2] = FeaturesServiceMBean.FEATURE_EVENT_EVENT_TYPE_INSTALLED; break;
                case FeatureUninstalled: itemValues[2] = FeaturesServiceMBean.FEATURE_EVENT_EVENT_TYPE_UNINSTALLED; break;
                default: throw new IllegalStateException("Unsupported event type: " + event.getType());
            }
            data = new CompositeDataSupport(FEATURE_EVENT, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature event open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    static {
        FEATURE_EVENT = createFeatureEventType();
    }

    private static CompositeType createFeatureEventType() {
        try {
            String description = "This type identify a Karaf feature event";
            String[] itemNames = FeaturesServiceMBean.FEATURE_EVENT;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;

            itemDescriptions[0] = "The id of the feature";
            itemDescriptions[1] = "The version of the feature";
            itemDescriptions[2] = "The type of the event";

            return new CompositeType("FeatureEvent", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build featureEvent type", e);
        }
    }
}
