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
package org.apache.karaf.management.mbeans.scr;

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
    String[] COMPONENT = { COMPONENT_ID, COMPONENT_NAME, COMPONENT_STATE,
            COMPONENT_PROPERTIES, COMPONENT_REFERENCES };

    String[] PROPERTY = { PROPERTY_KEY, PROPERTY_VALUE };

    String[] REFERENCE = { REFERENCE_NAME, REFERENCE_SATISFIED, REFERENCE_CARDINALITY, REFERENCE_AVAILABILITY, REFERENCE_POLICY, REFERENCE_BOUND_SERVICES};

    /**
     * Displays a {@link TabularData} with all the component details.
     * @return
     * @throws Exception
     */
    TabularData getComponents() throws Exception;
    
    /**
     * Presents a {@ String} array of components currently registered with the SCR.
     * 
     * @return String[]
     * @throws Exception
     */
    String[] listComponents() throws Exception;

    /**
     * Verifies if the named component is currently in an ACTIVE state.
     * 
     * @param componentName the components name
     * @return true if ACTIVE, otherwise false
     * @throws Exception
     */
    boolean isComponentActive(String componentName) throws Exception;

    /**
     * Returns the named components state
     * 
     * @param componentName the components name
     * @return
     * @throws Exception
     */
    int componentState(String componentName) throws Exception;

    /**
     * Activates a component that is currently in a DISABLED state.
     * 
     * @param componentName the components name
     * @throws Exception
     */
    void activateComponent(String componentName) throws Exception;

    /**
     * Disables a component that is not in an ACTIVE state.
     * 
     * @param componentName the components name
     * @throws Exception
     */
    @Deprecated
    void deactiveateComponent(String componentName) throws Exception;

    /**
     * Disables a component that is not in an ACTIVE state.
     *
     * @param componentName the components name
     * @throws Exception
     */
    void deactivateComponent(String componentName) throws Exception;
}
