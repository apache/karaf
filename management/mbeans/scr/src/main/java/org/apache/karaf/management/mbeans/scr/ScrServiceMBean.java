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

/**
 * The management interface for SCR Components.
 */
public interface ScrServiceMBean {
    
    
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
    void deactiveateComponent(String componentName) throws Exception;
}
