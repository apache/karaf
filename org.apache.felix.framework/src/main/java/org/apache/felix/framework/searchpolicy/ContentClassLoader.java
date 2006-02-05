/*
 *   Copyright 2006 The Apache Software Foundation
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
package org.apache.felix.framework.searchpolicy;

import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Enumeration;

import org.apache.felix.framework.Logger;
import org.apache.felix.framework.util.Util;
import org.apache.felix.moduleloader.IContentLoader;
import org.apache.felix.moduleloader.ResourceNotFoundException;

public class ContentClassLoader extends SecureClassLoader
{
    private ContentLoaderImpl m_contentLoader = null;

    ContentClassLoader(ContentLoaderImpl contentLoader)
    {
        m_contentLoader = contentLoader;
    }

    public IContentLoader getContentLoader()
    {
        return m_contentLoader;
    }

    public Class loadClassFromModule(String name)
        throws ClassNotFoundException
    {
        // Ask the search policy for the clas before consulting the module.
        Class clazz = findClass(name);

        // If not found, then throw an exception.
        if (clazz == null)
        {
            throw new ClassNotFoundException(name);
        }
        resolveClass(clazz);
        return clazz;
    }

    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        // Make sure the class was not already loaded.
        Class clazz = findLoadedClass(name);
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
        Class clazz = findLoadedClass(name);

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

                // Get the code source URL for this class. For concurrency
                // purposes, we are performing this call outside of the
                // synchronized block below since we call out to application
                // code, which might in turn need to call back into the
                // module loader code. Because of this, it is better to
                // not be holding any locks before making the call.
                URL url = null;
// TODO: FIX CODE SOURCE URL
//                URL url = m_mgr.getURLPolicy().createCodeSourceURL(
//                    m_mgr, m_module);

                // If we have a valid code source URL, then use it to
                // define the class for security purposes, otherwise
                // define the class without a code source.
                if (url != null)
                {
                    CodeSource cs = new CodeSource(url, (Certificate[]) null);
                    clazz = defineClass(name, bytes, 0, bytes.length, cs);
                }
                else
                {
                    clazz = defineClass(name, bytes, 0, bytes.length);
                }
            }
        }

        if (clazz != null)
        {
            return clazz;
        }

        return null;
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
            m_contentLoader.getLogger().log(
                Logger.LOG_WARNING,
                ex.getMessage(),
                ex);
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
}