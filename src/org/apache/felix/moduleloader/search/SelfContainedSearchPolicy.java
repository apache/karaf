/*
 *   Copyright 2005 The Apache Software Foundation
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
package org.apache.felix.moduleloader.search;

import java.net.URL;

import org.apache.felix.moduleloader.*;

/**
 * <p>
 * This class implements a <tt>ModuleLoader</tt> search policy that
 * assumes that all modules are self-contained. In other words, when
 * loading a class or resource for a particular module, only that
 * particular module's resource sources are search. No classes or
 * resources are shared among modules.
 * </p>
 * @see org.apache.felix.moduleloader.SearchPolicy
 * @see org.apache.felix.moduleloader.Module
 * @see org.apache.felix.moduleloader.ModuleClassLoader
 * @see org.apache.felix.moduleloader.ModuleManager
**/
public class SelfContainedSearchPolicy implements SearchPolicy
{
    private ModuleManager m_mgr = null;

    /**
     * This method is part of the <tt>SearchPolicy</tt> interface.
     * This method is called by the <tt>ModuleManager</tt> once to
     * give the search policy instance a reference to its associated
     * module manager. This method should be implemented such that
     * it cannot be called twice; calling this method a second time
     * should produce an illegal state exception.
     * @param mgr the module manager associated with this search policy.
     * @throws java.lang.IllegalStateException if the method is called
     *         more than once.
    **/
    public void setModuleManager(ModuleManager mgr)
        throws IllegalStateException
    {
        if (m_mgr == null)
        {
            m_mgr = mgr;
        }
        else
        {
            throw new IllegalStateException("Module manager is already initialized");
        }
    }

    public Object[] definePackage(Module module, String pkgName)
    {
        return null;
    }

    /**
     * Simply returns <tt>null</tt> which forces the module class
     * loader to only search the target module's resource sources
     * for the specified class.
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return <tt>null</tt>.
    **/
    public Class findClassBeforeModule(ClassLoader parent, Module module, String name)
    {
        // First, try to load from parent.
        if (parent != null)
        {
            try
            {
                Class c = parent.loadClass(name);
                if (c != null)
                {
                    return c;
                }
            }
            catch (ClassNotFoundException ex)
            {
                // Ignore.
            }
        }

        return null;
    }

    public Class findClassAfterModule(ClassLoader parent, Module module, String name)
    {
        return null;
    }

    /**
     * Simply returns <tt>null</tt> which forces the module class
     * loader to only search the target module's resource sources
     * for the specified resource.
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the resource being loaded.
     * @return <tt>null</tt>.
    **/
    public URL findResource(ClassLoader parent, Module module, String name)
    {
        if (parent != null)
        {
            return parent.getResource(name);
        }
        return null;
    }
}