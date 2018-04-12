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
package org.apache.karaf.scr.management.codec;

import org.apache.karaf.scr.management.ServiceComponentRuntimeMBean;
import org.osgi.service.component.runtime.dto.ComponentDescriptionDTO;

import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import java.util.Arrays;

public class JmxComponentDescription {


    /**
     * The CompositeType which represents a single component
     */
    public final static CompositeType COMPONENT = createComponentType();

    /**
     * The TabularType which represents a list of components
     */
    public final static TabularType COMPONENT_TABLE = createComponentTableType();

    private final CompositeData data;
    
    public JmxComponentDescription(ComponentDescriptionDTO component) {
        try {
            String[] itemNames = ServiceComponentRuntimeMBean.COMPONENT_DESCRIPTION;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = component.bundle.id;
            itemValues[1] = component.name;
            itemValues[2] = component.factory;
            itemValues[3] = component.scope;
            itemValues[4] = component.implementationClass;
            itemValues[5] = component.defaultEnabled;
            itemValues[6] = component.immediate;
            itemValues[7] = component.serviceInterfaces;
            itemValues[8] = JmxProperty.tableFrom(component.properties);
            itemValues[9] = JmxReference.tableFrom(component.references);
            itemValues[10] = component.activate;
            itemValues[11] = component.deactivate;
            itemValues[12] = component.modified;
            itemValues[13] = component.configurationPolicy;
            itemValues[14] = component.configurationPid;
            data = new CompositeDataSupport(COMPONENT, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(ComponentDescriptionDTO... components) {
        return tableFrom(Arrays.asList(components));
    }

    public static TabularData tableFrom(Iterable<ComponentDescriptionDTO> components) {
        TabularDataSupport table = new TabularDataSupport(COMPONENT_TABLE);
        for (ComponentDescriptionDTO component : components) {
            table.put(new JmxComponentDescription(component).asCompositeData());
        }
        return table;
    }

    private static CompositeType createComponentType() {
        try {
            String description = "This type encapsulates Scr references";
            String[] itemNames = ServiceComponentRuntimeMBean.COMPONENT_DESCRIPTION;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.LONG;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;
            itemTypes[3] = SimpleType.STRING;
            itemTypes[4] = SimpleType.STRING;
            itemTypes[5] = SimpleType.BOOLEAN;
            itemTypes[6] = SimpleType.BOOLEAN;
            itemTypes[7] = new ArrayType<String>(1, SimpleType.STRING);
            itemTypes[8] = JmxProperty.PROPERTY_TABLE;
            itemTypes[9] = JmxReference.REFERENCE_TABLE;
            itemTypes[10] = SimpleType.STRING;
            itemTypes[11] = SimpleType.STRING;
            itemTypes[12] = SimpleType.STRING;
            itemTypes[13] = SimpleType.STRING;
            itemTypes[14] = new ArrayType<String>(1, SimpleType.STRING);

            itemDescriptions[0] = "The bundle id of the component";
            itemDescriptions[1] = "The name of the component";
            itemDescriptions[2] = "factory";
            itemDescriptions[3] = "scope";
            itemDescriptions[4] = "implementationClass";
            itemDescriptions[5] = "defaultEnabled";
            itemDescriptions[6] = "immediate";
            itemDescriptions[7] = "serviceInterfaces";
            itemDescriptions[8] = "properties";
            itemDescriptions[9] = "references";
            itemDescriptions[10] = "activate";
            itemDescriptions[11] = "deactivate";
            itemDescriptions[12] = "modified";
            itemDescriptions[13] = "configurationPolicy";
            itemDescriptions[14] = "configurationPid";

            return new CompositeType("Component", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build component type", e);
        }
    }

    private static TabularType createComponentTableType() {
        try {
            return new TabularType("ComponentDescription", "The table of all components",
                    COMPONENT, ServiceComponentRuntimeMBean.COMPONENT_DESCRIPTION);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build components table type", e);
        }
    }


}
