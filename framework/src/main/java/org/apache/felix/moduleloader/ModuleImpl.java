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
import org.apache.felix.framework.searchpolicy.ContentLoaderImpl;
import org.apache.felix.framework.util.FelixConstants;

public class ModuleImpl implements IModule
{
    private Logger m_logger = null;
    private String m_id = null;
    private IModuleDefinition m_md = null;
    private IContentLoader m_contentLoader = null;
    private IModule[] m_fragments = null;
    private IWire[] m_wires = null;
    private IModule[] m_dependentHosts = new IModule[0];
    private IModule[] m_dependentImporters = new IModule[0];
    private IModule[] m_dependentRequirers = new IModule[0];
    private volatile boolean m_isStale = false;

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

    public synchronized IModule[] getFragments()
    {
        return m_fragments;
    }

    public synchronized void attachFragments(IModule[] fragments) throws Exception
    {
        // Remove module from old fragment dependencies.
        // We will generally only remove module fragment
        // dependencies when we are uninstalling the module.
        for (int i = 0; (m_fragments != null) && (i < m_fragments.length); i++)
        {
            ((ModuleImpl) m_fragments[i]).removeDependentHost(this);
        }

        // Update the dependencies on the new fragments.
        m_fragments = fragments;

        // We need to add ourself as a dependent of each fragment
        // module. We also need to create an array of fragment contents
        // to attach to our content loader.
        if (m_fragments != null)
        {
            IContent[] fragmentContents = new IContent[m_fragments.length];
            for (int i = 0; (m_fragments != null) && (i < m_fragments.length); i++)
            {
                ((ModuleImpl) m_fragments[i]).addDependentHost(this);
                fragmentContents[i] =
                    m_fragments[i].getContentLoader().getContent()
                        .getEntryAsContent(FelixConstants.CLASS_PATH_DOT);
            }
            // Now attach the fragment contents to our content loader.
            ((ContentLoaderImpl) m_contentLoader).attachFragmentContents(fragmentContents);
        }
    }

    public synchronized IWire[] getWires()
    {
        return m_wires;
    }

    public synchronized void setWires(IWire[] wires)
    {
        // Remove module from old wire modules' dependencies,
        // since we are no longer dependent on any the moduels
        // from the old wires.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            if (m_wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).removeDependentRequirer(this);
            }
            else if (m_wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).removeDependentImporter(this);
            }
        }

        m_wires = wires;

        // Add ourself as a dependent to the new wires' modules.
        for (int i = 0; (m_wires != null) && (i < m_wires.length); i++)
        {
            if (m_wires[i].getCapability().getNamespace().equals(ICapability.MODULE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).addDependentRequirer(this);
            }
            else if (m_wires[i].getCapability().getNamespace().equals(ICapability.PACKAGE_NAMESPACE))
            {
                ((ModuleImpl) m_wires[i].getExporter()).addDependentImporter(this);
            }
        }
    }

    public synchronized IModule[] getDependentHosts()
    {
        return m_dependentHosts;
    }

    public synchronized void addDependentHost(IModule module)
    {
        m_dependentHosts = addDependent(m_dependentHosts, module);
    }

    public synchronized void removeDependentHost(IModule module)
    {
        m_dependentHosts = removeDependent(m_dependentHosts, module);
    }

    public synchronized IModule[] getDependentImporters()
    {
        return m_dependentImporters;
    }

    public synchronized void addDependentImporter(IModule module)
    {
        m_dependentImporters = addDependent(m_dependentImporters, module);
    }

    public synchronized void removeDependentImporter(IModule module)
    {
        m_dependentImporters = removeDependent(m_dependentImporters, module);
    }

    public synchronized IModule[] getDependentRequirers()
    {
        return m_dependentRequirers;
    }

    public synchronized void addDependentRequirer(IModule module)
    {
        m_dependentRequirers = addDependent(m_dependentRequirers, module);
    }

    public synchronized void removeDependentRequirer(IModule module)
    {
        m_dependentRequirers = removeDependent(m_dependentRequirers, module);
    }

    public synchronized IModule[] getDependents()
    {
        IModule[] dependents = new IModule[
            m_dependentHosts.length + m_dependentImporters.length + m_dependentRequirers.length];
        System.arraycopy(
            m_dependentHosts,
            0,
            dependents,
            0,
            m_dependentHosts.length);
        System.arraycopy(
            m_dependentImporters,
            0,
            dependents,
            m_dependentHosts.length,
            m_dependentImporters.length);
        System.arraycopy(
            m_dependentRequirers,
            0,
            dependents,
            m_dependentHosts.length + m_dependentImporters.length,
            m_dependentRequirers.length);
        return dependents;
    }

    public Class getClass(String name) throws ClassNotFoundException
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
            throw ex;
        }
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

    public boolean isStale()
    {
        return m_isStale;
    }

    public void setStale()
    {
        m_isStale = true;
    }

    public String toString()
    {
        return m_id;
    }

    private static IModule[] addDependent(IModule[] modules, IModule module)
    {
        // Make sure the dependent module is not already present.
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i].equals(module))
            {
                return modules;
            }
        }
        IModule[] tmp = new IModule[modules.length + 1];
        System.arraycopy(modules, 0, tmp, 0, modules.length);
        tmp[modules.length] = module;
        return tmp;
    }

    private static IModule[] removeDependent(IModule[] modules, IModule module)
    {
        IModule[] tmp = modules;

        // Make sure the dependent module is not already present.
        for (int i = 0; i < modules.length; i++)
        {
            if (modules[i].equals(module))
            {
                // If this is the module, then point to empty list.
                if ((modules.length - 1) == 0)
                {
                    tmp = new IModule[0];
                }
                // Otherwise, we need to do some array copying.
                else
                {
                    tmp = new IModule[modules.length - 1];
                    System.arraycopy(modules, 0, tmp, 0, i);
                    if (i < tmp.length)
                    {
                        System.arraycopy(modules, i + 1, tmp, i, tmp.length - i);
                    }
                }
                break;
            }
        }

        return tmp;
    }
}
