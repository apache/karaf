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
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import org.apache.felix.karaf.features.RepositoryEvent;
import org.apache.felix.karaf.features.management.FeaturesServiceMBean;

public class JmxRepositoryEvent {

    public static final CompositeType REPOSITORY_EVENT;

    private final CompositeData data;

    public JmxRepositoryEvent(RepositoryEvent event) {
        try {
            String[] itemNames = FeaturesServiceMBean.REPOSITORY_EVENT;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = event.getRepository().getName();
            itemValues[1] = event.getRepository().getURI().toString();
            switch (event.getType()) {
                case RepositoryAdded:   itemValues[2] = FeaturesServiceMBean.REPOSITORY_EVENT_EVENT_TYPE_ADDED; break;
                case RepositoryRemoved: itemValues[2] = FeaturesServiceMBean.REPOSITORY_EVENT_EVENT_TYPE_REMOVED; break;
                default: throw new IllegalStateException("Unsupported event type: " + event.getType());
            }
            data = new CompositeDataSupport(REPOSITORY_EVENT, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form repository event open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    static {
        REPOSITORY_EVENT = createRepositoryEventType();
    }

    private static CompositeType createRepositoryEventType() {
        try {
            String description = "This type identify a Karaf repository event";
            String[] itemNames = FeaturesServiceMBean.REPOSITORY_EVENT;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;

            itemDescriptions[0] = "The name of the repository";
            itemDescriptions[1] = "The uri of the repository";
            itemDescriptions[2] = "The type of event";

            return new CompositeType("RepositoryEvent", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build repositoryEvent type", e);
        }
    }
}