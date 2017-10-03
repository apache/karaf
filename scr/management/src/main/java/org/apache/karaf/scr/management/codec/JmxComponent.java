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

import org.apache.karaf.scr.management.ScrServiceMBean;
import org.apache.karaf.scr.management.internal.ScrService;

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

public class JmxComponent {


    /**
     * The CompositeType which represents a single component
     */
    public final static CompositeType COMPONENT = createComponentType();

    /**
     * The TabularType which represents a list of components
     */
    public final static TabularType COMPONENT_TABLE = createComponentTableType();

    private final CompositeData data;
    
    public JmxComponent(ScrService.Component component) {
        try {
            String[] itemNames = ScrServiceMBean.COMPONENT;
            Object[] itemValues = new Object[itemNames.length];
            itemValues[0] = component.getId();
            itemValues[1] = component.getName();
            itemValues[2] = getState(component);
            itemValues[3] = JmxProperty.tableFrom(component.getProperties());
            itemValues[4] = JmxReference.tableFrom(component.getReferences());
            data = new CompositeDataSupport(COMPONENT, itemNames, itemValues);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Cannot form feature open data", e);
        }
    }

    public CompositeData asCompositeData() {
        return data;
    }

    public static TabularData tableFrom(ScrService.Component... components) {
        return tableFrom(Arrays.asList(components));
    }

    public static TabularData tableFrom(Iterable<ScrService.Component> components) {
        TabularDataSupport table = new TabularDataSupport(COMPONENT_TABLE);
        for (ScrService.Component component : components) {
            table.put(new JmxComponent(component).asCompositeData());
        }
        return table;
    }

    private static CompositeType createComponentType() {
        try {
            String description = "This type encapsulates Scr references";
            String[] itemNames = ScrServiceMBean.COMPONENT;
            OpenType[] itemTypes = new OpenType[itemNames.length];
            String[] itemDescriptions = new String[itemNames.length];
            itemTypes[0] = SimpleType.LONG;
            itemTypes[1] = SimpleType.STRING;
            itemTypes[2] = SimpleType.STRING;
            itemTypes[3] = JmxProperty.PROPERTY_TABLE;
            itemTypes[4] = JmxReference.REFERENCE_TABLE;

            itemDescriptions[0] = "The id of the component";
            itemDescriptions[1] = "The name of the component";
            itemDescriptions[2] = "The state of the component";
            itemDescriptions[3] = "The properties of the component";
            itemDescriptions[4] = "The references of the component";

            return new CompositeType("Component", description, itemNames,
                    itemDescriptions, itemTypes);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build component type", e);
        }
    }

    private static TabularType createComponentTableType() {
        try {
            return new TabularType("Component", "The table of all components",
                    COMPONENT, ScrServiceMBean.COMPONENT);
        } catch (OpenDataException e) {
            throw new IllegalStateException("Unable to build components table type", e);
        }
    }


    /**
     * Returns a literal for the {@link ScrService.Component} state.
     * @param component     The target {@link ScrService.Component}.
     * @return
     */
    private static String getState(ScrService.Component component) {
        switch (component.getState()) {
            case ScrService.Component.STATE_ACTIVE:
                return "Active";
            case ScrService.Component.STATE_ACTIVATING:
                return "Activating";
            case ScrService.Component.STATE_DEACTIVATING:
                return "Deactivating";
            case ScrService.Component.STATE_DISABLED:
                return "Disabled";
            case ScrService.Component.STATE_DISABLING:
                return "Disabling";
            case ScrService.Component.STATE_DISPOSED:
                return "Disposed";
            case ScrService.Component.STATE_DISPOSING:
                return "Disposing";
            case ScrService.Component.STATE_ENABLING:
                return "Enabling";
            case ScrService.Component.STATE_FACTORY:
                return "Factory";
            case ScrService.Component.STATE_REGISTERED:
                return "Registered";
            case ScrService.Component.STATE_UNSATISFIED:
                return "Unsatisfied";
        }
        return "Unknown";
    }
}
