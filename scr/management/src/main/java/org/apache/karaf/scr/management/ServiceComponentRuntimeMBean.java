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
package org.apache.karaf.scr.management;

import javax.management.openmbean.TabularData;

/**
 * The management interface for SCR Components.
 */
public interface ServiceComponentRuntimeMBean {

    String COMPONENT_BUNDLE_ID = "BundleId";
    String COMPONENT_NAME = "Name";
    String COMPONENT_FACTORY = "Factory";
    String COMPONENT_SCOPE = "Scope";
    String COMPONENT_IMPLEMENTATION_CLASS = "ImplementationClass";
    String COMPONENT_DEFAULT_ENABLED = "DefaultEnabled";
    String COMPONENT_IMMEDIATE = "Immediate";
    String COMPONENT_SERVICE_INTERFACES = "ServiceInterfaces";
    String COMPONENT_PROPERTIES = "Properties";
    String COMPONENT_REFERENCES = "References";
    String COMPONENT_ACTIVATE = "Activate";
    String COMPONENT_DEACTIVATE = "Deactivate";
    String COMPONENT_MODIFIED = "Modified";
    String COMPONENT_CONFIGURATION_POLICY = "ConfigurationPolicy";
    String COMPONENT_CONFIGURATION_PID = "ConfigurationId";
    String COMPONENT_STATE = "State";
    String COMPONENT_ID = "Id";
    String COMPONENT_SATISFIED_REFERENCES = "SatisfiedReferences";
    String COMPONENT_UNSATISFIED_REFERENCES = "UnsatisfiedReferences";

    String PROPERTY_KEY = "Key";
    String PROPERTY_VALUE = "Value";

    /*
               itemValues[0] = reference.name;
            itemValues[1] = reference.interfaceName;
            itemValues[2] = reference.cardinality;
            itemValues[3] = reference.policy;
            itemValues[4] = reference.policyOption;
            itemValues[5] = reference.target;
            itemValues[6] = reference.bind;
            itemValues[7] = reference.unbind;
            itemValues[8] = reference.updated;
            itemValues[9] = reference.field;
            itemValues[10] = reference.fieldOption;
            itemValues[11] = reference.scope;

     */
    String REFERENCE_NAME = "Name";
    String REFERENCE_INTERFACE_NAME = "InterfaceName";
    String REFERENCE_CARDINALITY = "Cardinality";
    String REFERENCE_POLICY = "Policy";
    String REFERENCE_POLICY_OPTION = "PolicyOption";
    String REFERENCE_TARGET = "Target";
    String REFERENCE_BIND = "Bind";
    String REFERENCE_UNBIND = "Unbind";
    String REFERENCE_UPDATED = "Updated";
    String REFERENCE_FIELD = "Field";
    String REFERENCE_FIELD_OPTION = "FieldOption";
    String REFERENCE_SCOPE = "Scope";

    String REFERENCE_BOUND_SERVICES = "BoundServices";

    /**
     * The item names in the CompositeData representing a component
     */
    String[] COMPONENT_DESCRIPTION = {COMPONENT_BUNDLE_ID, COMPONENT_NAME, COMPONENT_FACTORY,
            COMPONENT_SCOPE, COMPONENT_IMPLEMENTATION_CLASS, COMPONENT_DEFAULT_ENABLED,
            COMPONENT_IMMEDIATE, COMPONENT_SERVICE_INTERFACES,
            COMPONENT_PROPERTIES, COMPONENT_REFERENCES,
            COMPONENT_ACTIVATE, COMPONENT_DEACTIVATE, COMPONENT_MODIFIED,
            COMPONENT_CONFIGURATION_POLICY, COMPONENT_CONFIGURATION_PID};

    String[] COMPONENT_CONFIGURATION = {COMPONENT_BUNDLE_ID, COMPONENT_NAME,
            COMPONENT_STATE, COMPONENT_ID,
            COMPONENT_PROPERTIES,
            COMPONENT_SATISFIED_REFERENCES, COMPONENT_UNSATISFIED_REFERENCES
    };

    String[] PROPERTY = {PROPERTY_KEY, PROPERTY_VALUE};

    String[] REFERENCE = {REFERENCE_NAME, REFERENCE_INTERFACE_NAME,
            REFERENCE_CARDINALITY, REFERENCE_POLICY,
            REFERENCE_POLICY_OPTION, REFERENCE_TARGET,
            REFERENCE_BIND, REFERENCE_UNBIND, REFERENCE_UPDATED,
            REFERENCE_FIELD, REFERENCE_FIELD_OPTION, REFERENCE_SCOPE};

    String[] SVC_REFERENCE = {REFERENCE_NAME, REFERENCE_TARGET, REFERENCE_BOUND_SERVICES};

    /**
     * Display a {@link TabularData} with all the component details.
     *
     * @return A {@link TabularData} containing all SCR components.
     */
    TabularData getComponents();

    /**
     * Display a {@link TabularData} with all the component configurations details.
     *
     * @return A {@link TabularData} containing all SCR components.
     */
    TabularData getComponentConfigs();

    /**
     * Present a {@code String} array of components currently registered with the SCR.
     *
     * @return A {@code String[]} containing all SCR components ID.
     */
    TabularData getComponentConfigs(long bundleId, String componentName);

    /**
     * Check if the named component is currently enabled.
     *
     * @param componentName The component name.
     * @return True if the component is ACTIVE, otherwise false.
     */
    boolean isComponentEnabled(long bundleId, String componentName);

    /**
     * Enable a component.
     *
     * @param componentName The component name.
     */
    void enableComponent(long bundleId, String componentName);

    /**
     * Disable a component.
     *
     * @param componentName The component name.
     */
    void disableComponent(long bundleId, String componentName);

}
