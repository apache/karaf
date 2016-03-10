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

import javax.management.MBeanException;
import javax.management.openmbean.TabularData;

/**
 * The management interface for SCR Components.
 */
public interface ScrServiceMBean {

    String COMPONENT_ID = "Id";
    String COMPONENT_NAME = "Name";
    String COMPONENT_STATE = "State";
    String COMPONENT_PROPERTIES = "Properties";
    String COMPONENT_REFERENCES = "References";

    String PROPERTY_KEY = "Key";
    String PROPERTY_VALUE = "Value";

    String REFERENCE_NAME = "Name";
    String REFERENCE_SATISFIED = "Satisfied";

    String REFERENCE_CARDINALITY = "Cardinality";
    String REFERENCE_CARDINALITY_SINGLE = "Single";
    String REFERENCE_CARDINALITY_MULTIPLE = "Multiple";
    String REFERENCE_AVAILABILITY = "Availability";
    String REFERENCE_AVAILABILITY_OPTIONAL = "Optional";
    String REFERENCE_AVAILABILITY_MANDATORY = "Mandatory";

    String REFERENCE_POLICY = "Policy";
    String REFERENCE_POLICY_DYNAMIC = "Dynamic";
    String REFERENCE_POLICY_STATIC = "Static";

    String REFERENCE_BOUND_SERVICES = "Bound Services";

    /**
     * The item names in the CompositeData representing a component
     */
    String[] COMPONENT = {COMPONENT_ID, COMPONENT_NAME, COMPONENT_STATE,
            COMPONENT_PROPERTIES, COMPONENT_REFERENCES};

    String[] PROPERTY = {PROPERTY_KEY, PROPERTY_VALUE};

    String[] REFERENCE = {REFERENCE_NAME, REFERENCE_SATISFIED, REFERENCE_CARDINALITY, REFERENCE_AVAILABILITY, REFERENCE_POLICY, REFERENCE_BOUND_SERVICES};

    /**
     * Display a {@link TabularData} with all the component details.
     *
     * @return A {@link TabularData} containing all SCR components.
     */
    TabularData getComponents();

    /**
     * Present a {@code String} array of components currently registered with the SCR.
     *
     * @return A {@code String[]} containing all SCR components ID.
     */
    String[] listComponents();

    /**
     * Verify if the named component is currently in an ACTIVE state.
     *
     * @param componentName The component name.
     * @return True if the component is ACTIVE, otherwise false.
     * @throws MBeanException If the check fails.
     */
    boolean isComponentActive(String componentName) throws MBeanException;

    /**
     * Return the named components state.
     *
     * @param componentName The component name.
     * @return The component status.
     */
    int componentState(String componentName);

    /**
     * Activate a component that is currently in a DISABLED state.
     *
     * @param componentName The component name.
     */
    void activateComponent(String componentName);

    /**
     * Disable a component that is not in an ACTIVE state.
     *
     * @param componentName The component name.
     */
    void deactivateComponent(String componentName);

}
