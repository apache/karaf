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
package org.apache.karaf.management.mbeans.scr.codec;


import org.apache.karaf.management.mbeans.scr.ScrServiceMBean;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.util.Dictionary;
import java.util.Enumeration;

public class JmxProperty {


    /**
     * The CompositeType which represents a single property
     */
    public final static CompositeType PROPERTY = createPropertyType();

    /**
     * The TabularType which represents a list of properties
     */
    public final static TabularType PROPERTY_TABLE = createPropertyTableType();

    private final CompositeData data;

    public JmxProperty(String key, String value) {
        try {
            String[] itemNames = ScrServiceMBean.PROPERTY;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = key;
            itemValues[1] = value;
            data = new CompositeDataSupport(PROPERTY, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form property open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(Dictionary properties) {
        TabularDataSupport table = new TabularDataSupport(PROPERTY_TABLE);
        Enumeration p = properties.keys();
        while (p.hasMoreElements()) {
            Object key = p.nextElement();
            Object value = properties.get(key);
            table.put(new JmxProperty(String.valueOf(key), String.valueOf(value)).asCompositeData());
        }
        return table;
    }

    private static CompositeType createPropertyType() {
        try {
            String description = "This type encapsulates Scr properties";
            String[] itemNames = ScrServiceMBean.PROPERTY;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.STRING;
            itemTypes[1] = SimpleType.STRING;

            itemDescriptions[0] = "The property key";
            itemDescriptions[1] = "The property value";

            return new CompositeType("Property", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build property type", e);
        }
    }

    private static TabularType createPropertyTableType() {
        try {
            return new TabularType("References", "The table of all properties",
                    PROPERTY, ScrServiceMBean.PROPERTY);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build properties table type", e);
        }
    }
}
