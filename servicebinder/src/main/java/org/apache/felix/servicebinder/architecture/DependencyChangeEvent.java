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
package org.apache.felix.servicebinder.architecture;

import org.apache.felix.servicebinder.DependencyMetadata;

/**
 * An event thrown when a dependency changes
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class DependencyChangeEvent
{
    public static final int DEPENDENCY_CREATED = 0;
    public static final int DEPENDENCY_VALID = 1;
    public static final int DEPENDENCY_INVALID = 2;
    public static final int DEPENDENCY_DESTROYED = 3;

    /**
     * 
     * @uml.property name="m_dep"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private Dependency m_dep;

    /**
     * 
     * @uml.property name="m_meta"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private DependencyMetadata m_meta;

    private int m_state;

    public DependencyChangeEvent(Dependency dep, DependencyMetadata meta,int newState)
    {
        m_dep = dep;
        m_meta = meta;
        m_state = newState;
    }

    public Dependency getDependency()
    {
        return m_dep;
    }

    public DependencyMetadata getDependencyMetadata()
    {
        return m_meta;
    }

    public int getState()
    {
        return m_state;
    }
}
