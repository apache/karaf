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

import java.net.URL;
import java.util.Enumeration;

import org.apache.felix.moduleloader.*;

public class R4SearchPolicy implements ISearchPolicy
{
    private R4SearchPolicyCore m_policyCore = null;
    private IModule m_module = null;

    public R4SearchPolicy(R4SearchPolicyCore policyCore, IModule module)
    {
        m_policyCore = policyCore;
        m_module = module;
    }

    public Object[] definePackage(String name)
    {
        return m_policyCore.definePackage(m_module, name);
    }

    public Class findClass(String name)
        throws ClassNotFoundException
    {
        return m_policyCore.findClass(m_module, name);
    }

    public URL findResource(String name)
        throws ResourceNotFoundException
    {
        return m_policyCore.findResource(m_module, name);
    }

    public Enumeration findResources(String name)
        throws ResourceNotFoundException
    {
        return m_policyCore.findResources(m_module, name);
    }

    public String findLibrary(String name)
    {
        return m_policyCore.findLibrary(m_module, name);
    }

    public String toString()
    {
        return m_module.toString();
    }
}