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

import org.apache.felix.framework.util.Util;
import org.apache.felix.framework.util.manifestparser.Capability;
import org.apache.felix.moduleloader.*;

// TODO: RESOLVER - This is a hack and doesn't really fit with abstraction.
public class R4WireFragment implements IWire
{
    private final IModule m_importer;
    private final IRequirement m_requirement;
    private final IModule m_exporter;
    private final ICapability m_capability;

    public R4WireFragment(IModule host, IModule fragment)
    {
        m_importer = host;
        m_requirement = null;
        m_exporter = fragment;
        m_capability = null;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getImporter()
     */
    public IModule getImporter()
    {
        return m_importer;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getRequirement()
     */
    public IRequirement getRequirement()
    {
        return m_requirement;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getExporter()
     */
    public IModule getExporter()
    {
        return m_exporter;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getCapability()
     */
    public ICapability getCapability()
    {
        return m_capability;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public boolean hasPackage(String pkgName)
    {
        return false;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getClass(java.lang.String)
     */
    public Class getClass(String name) throws ClassNotFoundException
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResource(java.lang.String)
     */
    public URL getResource(String name) throws ResourceNotFoundException
    {
        return null;
    }

    /* (non-Javadoc)
     * @see org.apache.felix.framework.searchpolicy.IWire#getResources(java.lang.String)
     */
    public Enumeration getResources(String name) throws ResourceNotFoundException
    {
        return null;
    }

    public String toString()
    {
        return m_importer + " -> hosts -> " + m_exporter;
    }
}