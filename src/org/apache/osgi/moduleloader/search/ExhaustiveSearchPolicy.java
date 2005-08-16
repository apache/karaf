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
package org.apache.osgi.moduleloader.search;

import java.net.URL;

import org.apache.osgi.moduleloader.*;

/**
 * <p>
 * This class implements a <tt>ModuleLoader</tt> search policy that
 * exhaustively and linearly searches all modules when trying to load
 * a particular class or resource. As a result of this algorithm, every class loader
 * for every module is essentially identical, meaning that each will
 * load a given class or resource from the same class loader. This search policy
 * provides behavior similar to the standard <tt>CLASSPATH</tt> environment
 * variable approach. The main difference is that modules can be added
 * to the module manager at run time; thus, the class path is dynamically
 * extended. This search policy is not fully dynamic, since it does not
 * support the removal of modules at run time; if a module is removed from
 * the module manager at run time, there is no attempt to clean up its
 * loaded classes.
 * </p>
 * @see org.apache.osgi.moduleloader.SearchPolicy
 * @see org.apache.osgi.moduleloader.Module
 * @see org.apache.osgi.moduleloader.ModuleClassLoader
 * @see org.apache.osgi.moduleloader.ModuleManager
**/
public class ExhaustiveSearchPolicy implements SearchPolicy
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
     * This method finds the specified class for the specified module. It
     * finds the class by linearly asking each module in the module manager
     * for the specific class. As soon as the class is found, it is returned.
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the class.
     * @param name the name of the class being loaded.
     * @return the class if found, <tt>null</tt> otherwise.
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
                // Ignore and search modules.
            }
        }

        Module[] modules = m_mgr.getModules();
        for (int i = 0; i < modules.length; i++)
        {
            try {
                Class clazz = modules[i].getClassLoader().loadClassFromModule(name);
                if (clazz != null)
                {
                    return clazz;
                }
            } catch (Throwable th) {
            }
        }

        return null;
    }

    public Class findClassAfterModule(ClassLoader parent, Module module, String name)
    {
        return null;
    }

    /**
     * This method finds the specified resource for the specified module. It
     * finds the resource by linearly asking each module in the module manager
     * for specific resource. As soon as the resource is found, a <tt>URL</tt>
     * to it is returned.
     * @param parent the parent class loader of the delegating class loader.
     * @param module the target module that is loading the resource.
     * @param name the name of the resource being loaded.
     * @return a <tt>URL</tt> to the resource if found, <tt>null</tt> otherwise.
    **/
    public URL findResource(ClassLoader parent, Module module, String name)
    {
        // First, try to load from parent.
        if (parent != null)
        {
            URL url = parent.getResource(name);
            if (url != null)
            {
                return url;
            }
        }

        Module[] modules = m_mgr.getModules();
        for (int i = 0; i < modules.length; i++)
        {
            try {
                URL url = modules[i].getClassLoader().getResourceFromModule(name);
                if (url != null)
                {
                    return url;
                }
            } catch (Throwable th) {
            }
        }

        return null;
    }
}