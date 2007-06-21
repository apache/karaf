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
import java.security.ProtectionDomain;
import java.security.SecureClassLoader;
import java.util.Enumeration;

import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IContentLoader;
import org.apache.felix.moduleloader.ResourceNotFoundException;

public class ContentClassLoader extends SecureClassLoader
{
    private ContentLoaderImpl m_contentLoader = null;
    private ProtectionDomain m_protectionDomain = null;

    public ContentClassLoader(ContentLoaderImpl contentLoader,
        ProtectionDomain protectionDomain)
    {
        m_contentLoader = contentLoader;
        m_protectionDomain = protectionDomain;
    }

    public IContentLoader getContentLoader()
    {
        return m_contentLoader;
    }

    protected Class loadClassFromModule(String name)
        throws ClassNotFoundException
    {
        // Ask the search policy for the clas before consulting the module.
        Class clazz = findClass(name);

        // If not found, then throw an exception.
        if (clazz == null)
        {
            throw new ClassNotFoundException(name);
        }
        return clazz;
    }

    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        Class clazz = null;

        // Make sure the class was not already loaded.
        synchronized (this)
        {    
            clazz = findLoadedClass(name);
        }

        if (clazz == null)
        {
            // Ask the search policy for the class.
            clazz = m_contentLoader.getSearchPolicy().findClass(name);
        }

        // If still not found, then throw an exception.
        if (clazz == null)
        {
            throw new ClassNotFoundException(name);
        }
        // Otherwise resolve the class.
        if (resolve)
        {
            resolveClass(clazz);
        }
        return clazz;
    }

    protected Class findClass(String name) throws ClassNotFoundException
    {
        // Do a quick check here to see if we can short-circuit this
        // entire process if the class was already loaded.
        Class clazz = null;
        synchronized (this)
        {
            clazz = findLoadedClass(name);
        }

        // Search for class in module.
        if (clazz == null)
        {
            String actual = name.replace('.', '/') + ".class";
            byte[] bytes = null;

            // Check the module class path.
            for (int i = 0;
                (bytes == null) &&
                (i < m_contentLoader.getClassPath().length); i++)
            {
                bytes = m_contentLoader.getClassPath()[i].getEntry(actual);
            }

            if (bytes != null)
            {
                // Before we actually attempt to define the class, grab
                // the lock for this class loader and make sure than no
                // other thread has defined this class in the meantime.
                synchronized (this)
                {
                    clazz = findLoadedClass(name);

                    if (clazz == null)
                    {
                        // We need to try to define a Package object for the class
                        // before we call defineClass(). Get the package name and
                        // see if we have already created the package.
                        String pkgName = Util.getClassPackage(name);
                        if (pkgName.length() > 0)
                        {
                            if (getPackage(pkgName) == null)
                            {
                                Object[] params =
                                    m_contentLoader.getSearchPolicy()
                                        .definePackage(pkgName);
                                if (params != null)
                                {
                                    definePackage(
                                        pkgName,
                                        (String) params[0],
                                        (String) params[1],
                                        (String) params[2],
                                        (String) params[3],
                                        (String) params[4],
                                        (String) params[5],
                                        null);
                                }
                                else
                                {
                                    definePackage(pkgName, null, null,
                                        null, null, null, null, null);
                                }
                            }
                        }

                        // If we have a security context, then use it to
                        // define the class with it for security purposes,
                        // otherwise define the class without a protection domain.
                        if (m_protectionDomain != null)
                        {
                            clazz = defineClass(name, bytes, 0, bytes.length,
                                m_protectionDomain);
                        }
                        else
                        {
                            clazz = defineClass(name, bytes, 0, bytes.length);
                        }
                    }
                }
            }
        }

        return clazz;
    }

    public URL getResourceFromModule(String name)
    {
        try
        {
            return findResource(name);
        }
        catch (Throwable th)
        {
            // Ignore and just return null.
        }
        return null;
    }

    public URL getResource(String name)
    {
        // Ask the search policy for the class before consulting the module.
        try
        {
            return m_contentLoader.getSearchPolicy().findResource(name);
        }
        catch (ResourceNotFoundException ex)
        {
        }
        return null;
    }

    protected URL findResource(String name)
    {
        return m_contentLoader.getResource(name);
    }

    protected Enumeration findResources(String name)
    {
        return m_contentLoader.getResources(name);
    }

    protected String findLibrary(String name)
    {
        return m_contentLoader.getSearchPolicy().findLibrary(name);
    }

    public String toString()
    {
        return m_contentLoader.toString();
    }
}
