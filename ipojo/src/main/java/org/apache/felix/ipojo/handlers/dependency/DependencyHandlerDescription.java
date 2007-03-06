/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.ipojo.handlers.dependency;

import java.util.Iterator;

import org.apache.felix.ipojo.architecture.HandlerDescription;

/**
 * Dependency Handler Description.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class DependencyHandlerDescription extends HandlerDescription {


    /**
     * Dependencies managed by the dependency handler.
     */
    private DependencyDescription[] m_dependencies = new DependencyDescription[0];

    /**
     * Constructor.
     * @param isValid : the validity of the dependency handler.
     */
    public DependencyHandlerDescription(boolean isValid) {
        super(DependencyHandler.class.getName(), isValid);
    }

    /**
     * @return the dependencies list.
     */
    public DependencyDescription[] getDependencies() { return m_dependencies; }

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
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public String getHandlerInfo() {
        String info = "";
        for (int i = 0; i < m_dependencies.length; i++) {
            String state = "resolved";
            if (m_dependencies[i].getState() == 2) { state = "unresolved"; }
            info += "\t Dependency on " + m_dependencies[i].getInterface() + "[" + m_dependencies[i].getFilter() + "] is " + state;
            Iterator it = m_dependencies[i].getUsedServices().keySet().iterator();
            while (it.hasNext()) {
                info += "\n \t\t Uses : " + it.next();
            }
            info += "\n";
        }
        return info;
    }

}
