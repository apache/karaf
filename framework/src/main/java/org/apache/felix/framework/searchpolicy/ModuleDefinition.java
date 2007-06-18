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
package org.apache.felix.framework.searchpolicy;

import org.apache.felix.framework.util.manifestparser.R4Library;
import org.apache.felix.moduleloader.*;

public class ModuleDefinition implements IModuleDefinition
{
    public ICapability[] m_capabilities = null;
    public IRequirement[] m_requirements = null;
    public IRequirement[] m_dynamicRequirements = null;
    private R4Library[] m_libraries = null;

    public ModuleDefinition(
        ICapability[] capabilities, IRequirement[] requirements,
        IRequirement[] dynamicRequirements, R4Library[] libraries)
    {
        m_capabilities = capabilities;
        m_requirements = requirements;
        m_dynamicRequirements = dynamicRequirements;
        m_libraries = libraries;
    }

    public ICapability[] getCapabilities()
    {
// TODO: RB - These should probably all return copies of the array.
        return m_capabilities;
    }

    public IRequirement[] getRequirements()
    {
        return m_requirements;
    }

    public IRequirement[] getDynamicRequirements()
    {
        return m_dynamicRequirements;
    }

    // TODO: EXPERIMENTAL - Experimental implicit wire concept to try
    //       to deal with code generation.
    public void setDynamicRequirements(IRequirement[] reqs)
    {
        m_dynamicRequirements = reqs;
    }

    public R4Library[] getLibraries()
    {
        return m_libraries;
    }
}