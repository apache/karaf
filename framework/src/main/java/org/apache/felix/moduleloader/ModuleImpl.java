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
package org.apache.felix.moduleloader;

import java.net.URL;
import java.util.Enumeration;

import org.apache.felix.framework.Logger;

public class ModuleImpl implements IModule
{
    private Logger m_logger = null;
    private String m_id = null;
    private boolean m_removalPending = false;
    private IModuleDefinition m_md = null;
    private IContentLoader m_contentLoader = null;
    private IWire[] m_wires = null;
    private IModule[] m_dependents = new IModule[0];
    private Object m_securityContext = null;

    ModuleImpl(Logger logger, String id, IModuleDefinition md)
    {
        m_logger = logger;
        m_id = id;
        m_md = md;
    }

    public String getId()
    {
        return m_id;
    }

    public IModuleDefinition getDefinition()
    {
        return m_md;
    }

    public IContentLoader getContentLoader()
    {
        return m_contentLoader;
    }

    protected void setContentLoader(IContentLoader contentLoader)
    {
        m_contentLoader = contentLoader;
    }

    protected void setSecurityContext(Object securityContext)
    {
        m_securityContext = securityContext;
    }

    public synchronized IWire[] getWires()
    {
        return m_wires;
    }

    public synchronized void setWires(IWire[] wires)
    {
        // Remove module from old wire modules' dependencies.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            ((ModuleImpl) m_wires[i].getExporter()).removeDependent(this);
        }
        m_wires = wires;
        // Add module to new wire modules' dependencies.
        for (int i = 0; (wires != null) && (i < wires.length); i++)
        {
            ((ModuleImpl) m_wires[i].getExporter()).addDependent(this);
        }
    }

    public synchronized void addDependent(IModule module)
    {
        // Make sure the dependent module is not already present.
        for (int i = 0; i < m_dependents.length; i++)
        {
            if (m_dependents[i].equals(module))
            {
                return;
            }
        }
        IModule[] tmp = new IModule[m_dependents.length + 1];
        System.arraycopy(m_dependents, 0, tmp, 0, m_dependents.length);
        tmp[m_dependents.length] = module;
        m_dependents = tmp;
    }

    public synchronized void removeDependent(IModule module)
    {
        // Make sure the dependent module is not already present.
        for (int i = 0; i < m_dependents.length; i++)
        {
            if (m_dependents[i].equals(module))
            {
                // If this is the module, then point to empty list.
                if ((m_dependents.length - 1) == 0)
                {
                    m_dependents = new IModule[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    IModule[] tmp = new IModule[m_dependents.length - 1];
                    System.arraycopy(m_dependents, 0, tmp, 0, i);
                    if (i < tmp.length)
                    {
                        System.arraycopy(
                            m_dependents, i + 1, tmp, i, tmp.length - i);
                    }
                    m_dependents = tmp;
                }
            }
        }
    }

    public synchronized IModule[] getDependents()
    {
        return m_dependents;
    }

    public boolean isRemovalPending()
    {
        return m_removalPending;
    }

    public void setRemovalPending(boolean removalPending)
    {
        m_removalPending = removalPending;
    }

    public Class getClass(String name)
    {
        try
        {
            return m_contentLoader.getSearchPolicy().findClass(name);
        }
        catch (ClassNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    public URL getResource(String name)
    {
        try
        {
            return m_contentLoader.getSearchPolicy().findResource(name);
        }
        catch (ResourceNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    public Enumeration getResources(String name)
    {
        try
        {
           return m_contentLoader.getSearchPolicy().findResources(name);
        }
        catch (ResourceNotFoundException ex)
        {
            m_logger.log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
        }
        return null;
    }

    public String toString()
    {
        return m_id;
    }

    public Object getSecurityContext()
    {
        return m_securityContext;
    }
}