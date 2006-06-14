/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.architecture;

import java.util.HashMap;


/**
 * Component Description.
 * @author Clement Escoffier
 */
public class ComponentDescription {

    /**
     * The Component class name.
     * This String is the identifier of the component.
     */
    private String m_className;

    /**
     * List of provided service.
     */
    private ProvidedServiceDescription[] m_provideServices = new ProvidedServiceDescription[0];

    /**
     * List of dependencies.
     */
    private DependencyDescription[] m_dependencies = new DependencyDescription[0];

    /**
     * Hashmap [Instance reference, service reference] of the used service.
     */
    private HashMap m_usedServices = new HashMap();

    /**
     * Created Instances of the components.
     */
    private String[] m_instances = new String[0];

    /**
     * State of the component (VALID / UNRESOLVED).
     */
    private int m_state;

    /**
     * Constructor.
     * @param name : the name of the component (the class name).
     * @param state : the state of the component.
     */
    public ComponentDescription(String name, int state) {
        m_className = name;
        m_state = state;
        m_usedServices.clear();
        m_instances = new String[0];
    }

    /**
     * @return the created instances
     */
    public String[] getInstances() { return m_instances; }

    /**
     * Set the instances array.
     */
    public void setInstances(String[] instances) { m_instances = instances; }

    /**
     * @return : the class name of the component
     */
    public String getClassName() { return m_className; }

    /**
     * @return the live dependency list
     */
    public DependencyDescription[] getDependencies() { return m_dependencies; }

    /**
     * @return the live provided service list
     */
    public ProvidedServiceDescription[] getProvideServices() { return m_provideServices; }

    /**
     * Add a dependency.
     * @param dep : the dependency to add
     */
    public void addDependency(DependencyDescription dep) {
        // Verify that the dependency description is not already in the array.
        for (int i = 0; (i < m_dependencies.length); i++) {
            if (m_dependencies[i] == dep) {
                return; //NOTHING TO DO, the description is already in the array
            }
        }
            // The component Description is not in the array, add it
            DependencyDescription[] newDep = new DependencyDescription[m_dependencies.length + 1];
            System.arraycopy(m_dependencies, 0, newDep, 0, m_dependencies.length);
            newDep[m_dependencies.length] = dep;
            m_dependencies = newDep;
    }

    /**
     * Add a provided service.
     * @param pds : the provided service to add
     */
    public void addProvidedService(ProvidedServiceDescription pds) {
        //Verify that the provided service description is not already in the array.
        for (int i = 0; (i < m_provideServices.length); i++) {
            if (m_provideServices[i] == pds) {
                return; //NOTHING DO DO, the description is already in the array
            }
        }

            // The component Description is not in the array, add it
            ProvidedServiceDescription[] newPSD = new ProvidedServiceDescription[m_provideServices.length + 1];
            System.arraycopy(m_provideServices, 0, newPSD, 0, m_provideServices.length);
            newPSD[m_provideServices.length] = pds;
            m_provideServices = newPSD;

    }

    /**
     * Set the state of the component.
     * @param i : the state
     */
    public void setState(int i) {
        m_state = i;
    }

    /**
     * @return the state of the component.
     */
    public int getState() {
        return m_state;
    }


}
