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
import org.osgi.service.component.runtime.dto.ComponentConfigurationDTO;

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
import java.util.Collection;
import java.util.stream.Stream;

public class JmxComponentConfiguration {


    /**
     * The CompositeType which represents a single component
     */
    public final static CompositeType COMPONENT_CONFIGURATION = createComponentConfigurationType();

    /**
     * The TabularType which represents a list of components
     */
    public final static TabularType COMPONENT_TABLE = createComponentTableType();

    private final CompositeData data;

    public JmxComponentConfiguration(ComponentConfigurationDTO component) {
        try {
            String[] itemNames = ServiceComponentRuntimeMBean.COMPONENT_CONFIGURATION;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = component.description.bundle.id;
            itemValues[1] = component.description.name;
            itemValues[2] = getState(component);
            itemValues[3] = component.id;
            itemValues[4] = JmxProperty.tableFrom(component.properties);
            itemValues[5] = JmxSvcReference.tableFrom(component.satisfiedReferences);
            itemValues[6] = JmxSvcReference.tableFrom(component.unsatisfiedReferences);
            data = new CompositeDataSupport(COMPONENT_CONFIGURATION, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    private String getState(ComponentConfigurationDTO component) {
        switch (component.state) {
            case ComponentConfigurationDTO.UNSATISFIED_CONFIGURATION:
                return "Unsatisfied configuration";
            case ComponentConfigurationDTO.UNSATISFIED_REFERENCE:
                return "Unstatisfied reference";
            case ComponentConfigurationDTO.SATISFIED:
                return "Statisfied";
            case ComponentConfigurationDTO.ACTIVE:
                return "Active";
        }
        return "Unknown";
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(ComponentConfigurationDTO... components) {
        return tableFrom(Arrays.asList(components));
    }

    public static TabularData tableFrom(Collection<ComponentConfigurationDTO> components) {
        return tableFrom(components.stream());
    }

    public static TabularData tableFrom(Stream<ComponentConfigurationDTO> components) {
        return components
                .map(JmxComponentConfiguration::new)
                .map(JmxComponentConfiguration::asCompositeData)
                .collect(
                    () -> new TabularDataSupport(COMPONENT_TABLE),
                    TabularDataSupport::put,
                    TabularDataSupport::putAll
                );
    }

    private static CompositeType createComponentConfigurationType() {
        try {
            String description = "This type encapsulates Scr references";
            String[] itemNames = ServiceComponentRuntimeMBean.COMPONENT_CONFIGURATION;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.LONG;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;
            itemTypes[3] = SimpleType.LONG;
            itemTypes[4] = JmxProperty.PROPERTY_TABLE;
            itemTypes[5] = JmxSvcReference.SVC_REFERENCE_TABLE;
            itemTypes[6] = JmxSvcReference.SVC_REFERENCE_TABLE;

            itemDescriptions[0] = "The bundle id of the component";
            itemDescriptions[1] = "The name of the component";
            itemDescriptions[2] = "The state of the component";
            itemDescriptions[3] = "The id of the component";
            itemDescriptions[4] = "The properties of the component";
            itemDescriptions[5] = "The references of the component";
            itemDescriptions[6] = "The references of the component";

            return new CompositeType("Component", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build component type", e);
        }
    }

    private static TabularType createComponentTableType() {
        try {
            return new TabularType("Configuration", "The table of component configurations",
                    COMPONENT_CONFIGURATION, ServiceComponentRuntimeMBean.COMPONENT_CONFIGURATION);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build components table type", e);
        }
    }


}
