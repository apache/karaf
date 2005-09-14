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
package org.apache.felix.moduleloader;

import java.io.IOException;
import java.net.URL;
import java.security.CodeSource;
import java.security.SecureClassLoader;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.Vector;

/**
 * <p>
 * Each module that is managed by a <tt>ModuleManager</tt> has a
 * <tt>ModuleClassLoader</tt> associated with it. The <tt>ModuleClassLoader</tt>
 * is responsible for loading all classes, resources, and native libraries
 * for its module. The <tt>ModuleClassLoader</tt> of a module
 * is accessed using the <tt>Module.getClassLoader()</tt> method. The
 * <tt>ModuleClassLoader</tt> uses its module's
 * <a href="ResourceSource.html"><tt>ResourceSource</tt></a>s
 * and <a href="LibrarySource.html"><tt>LibrarySource</tt></a>s
 * to perform its function.
 * </p>
 * <p>
 * When loading a class or resource, the <tt>ModuleClassLoader</tt> does
 * not immediately search its module's <tt>ResourceSource</tt>s, instead
 * it first delegates the request to the
 * <a href="SearchPolicy.html"><tt>SearchPolicy</tt></a> of the
 * <tt>ModuleManager</tt>; this allows applications to inject specific
 * class/resource loading policies. When the <tt>ModuleClassLoader</tt> delegates
 * to the search policy, the search policy uses application-specific behavior
 * to typically service the request from the <tt>ResourceSource</tt>s of
 * other modules. If the search policy returns a result, then this result is
 * returned immediately by the <tt>ModuleClassLoader</tt>; otherwise, it searches
 * the <tt>ResourceSource</tt>s its module in an attempt to satisfy the
 * original request.
 * </p>
 * <p>
 * <b><i>Important:</i></b> The search policy <i>searches</i> modules in
 * some application-specific manner in order to find a class or resource.
 * This <i>search</i> is instigated, either directly or indirectly, by calls
 * to <tt>ModuleClassLoader.loadClass()</tt> and <tt>ModuleClassLoader.getResource()</tt>,
 * respectively. In order for the search policy to load a class or resource,
 * it must <b>not</b> use <tt>ModuleClassLoader.loadClass()</tt> or
 * <tt>ModuleClassLoader.getResource()</tt> again, because this would result
 * in an infinite loop. Instead, the <tt>ModuleClassLoader</tt> offers the
 * the methods <tt>ModuleClassLoader.loadClassFromModule()</tt> and
 * <tt>ModuleClassLoader.getResourceFromModule()</tt> to search a given module
 * and to avoid an infinite loop. As an example, consider the following
 * snippet of code that implements an "exhaustive" search policy:
 * </p>
 * <pre>
 *     ...
 *     public Class findClass(Module module, String name)
 *     {
 *         Module[] modules = m_mgr.getModules();
 *         for (int i = 0; i < modules.length; i++)
 *         {
 *             try {
 *                 Class clazz = modules[i].getClassLoader().loadClassFromModule(name);
 *                 if (clazz != null)
 *                 {
 *                     return clazz;
 *                 }
 *             } catch (Throwable th) {
 *             }
 *         }
 *
 *         return null;
 *     }
 *     ...
 * </pre>
 * <p>
 * In the above code, the search policy "exhaustively" searches every module in the
 * <tt>ModuleManager</tt> to find the requested resource. Note that this policy
 * will also search the module that originated the request, which is not totally
 * necessary since returning <tt>null</tt> will cause the <tt>ModuleClassLoader</tt>
 * to search the originating module's <tt>ResourceSource</tt>s.
 * </p>
**/
public class ModuleClassLoader extends SecureClassLoader
{
    private ModuleManager m_mgr = null;
    private Module m_module = null;
    private boolean m_useParentSource = false;

    /**
     * <p>
     * Constructs an instance using the specified <tt>ModuleManager</tt>, for
     * the specified <tt>Module</tt>. This constructor is protected so that
     * it cannot be created publicly.
     * </p>
     * @param mgr the <tt>ModuleManager</tt> of the <tt>Module</tt>.
     * @param module the <tt>Module</tt> instance associated with the class loader.
    **/
    protected ModuleClassLoader(ModuleManager mgr, Module module, boolean useParentSource)
    {
        super(ModuleClassLoader.class.getClassLoader());
        m_mgr = mgr;
        m_module = module;
        m_useParentSource = useParentSource;
    }

    /**
     * <p>
     * This method is nearly an exact copy of the ClassLoader.loadClass()
     * method. The main difference is that it delegates to its associated
     * <tt>ModuleManager</tt>'s search policy before calling the
     * <tt>ClassLoader.findClass()</tt> method. Additionally, the synchronized
     * modifier was removed from the superclass method; this change was necessary
     * because ClassLoader class assumes a tree of class loaders, but the class
     * loading structure in the <tt>ModuleManager</tt> might actually be a graph
     * of class loaders; thus, it was necessary to loosen the concurrency locking
     * to allow for cycles.
     * </p>
     * @param name the class to be loaded.
     * @param resolve flag indicating whether the class should be resolved or not.
     * @return the loaded class.
     * @throws java.lang.ClassNotFoundException if the class could not be loaded.
    **/
    protected Class loadClass(String name, boolean resolve)
        throws ClassNotFoundException
    {
        // Make sure the class was not already loaded.
        Class c = findLoadedClass(name);
        // Ask the search policy for the clas before consulting the module.
        c = m_mgr.getSearchPolicy().findClassBeforeModule(getParent(), m_module, name);
        // If the search policy didn't find it, then consult the module.
        if (c == null)
        {
            c = findClass(name);
        }
        // If the module didn't find it, then consult the search policy
        // one more time.
        if (c == null)
        {
            c = m_mgr.getSearchPolicy().findClassAfterModule(getParent(), m_module, name);
        }
        // If still not found, then throw an exception.
        if (c == null)
        {
            throw new ClassNotFoundException(name);
        }
        // Otherwise resolve the class.
        if (resolve)
        {
            resolveClass(c);
        }
        return c;
    }

    /**
     * <p>
     * This method overriden from from <tt>ClassLoader</tt>.
     * It is implemented such that it loads classes from the set of
     * <tt>ResourceSource</tt>s from its associated module.
     * </p>
     * @param name the name of the resource to load.
     * @return the loaded <tt>Class</tt> object.
     * @throws java.lang.ClassNotFoundException if the class could not be loaded.
    **/
    protected Class findClass(String name) throws ClassNotFoundException
    {
        Class clazz = findLoadedClass(name);

        // If the parent is used as a source, try to
        // load the class from it.
        // TODO: This is really a hack and should be generalized somehow.
        if (m_useParentSource)
        {
            clazz = (getParent() == null) ? null : getParent().loadClass(name);
        }

        // Otherwise search for class in resource sources.
        if (clazz == null)
        {
            String actual = name.replace('.', '/') + ".class";
            ResourceSource[] sources = m_module.getResourceSources();
            for (int i = 0;
                (clazz == null) && (sources != null) && (i < sources.length);
                i++)
            {
                byte[] bytes = sources[i].getBytes(actual);
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
                                m_mgr.getSearchPolicy().definePackage(m_module, pkgName);
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
                    URL url = m_mgr.getURLPolicy().createCodeSourceURL(
                        m_mgr, m_module);

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
        }

        if (clazz != null)
        {
            return clazz;
        }

        return null;
    }

    /**
     * <p>
     * This method is used by <tt>SearchPolicy</tt> instances when they want
     * to load a class from a module. The search policy is initially invoked when
     * <tt>ModuleClassLoader.loadClass()</tt> delegates a class loading
     * request to it. In general, the ultimate goal of the search policy is to
     * return a class from another module if possible. Unfortunately, if a search
     * policy tries to directly load a class from another module's class loader, an
     * infinite loop will result because the module's class loader will delegate the
     * request back to the search policy. To avoid this situation, search policies
     * must use this method when trying to load a class from a module.
     * </p>
     * @param name the name of the class to load.
     * @return the loaded class or <tt>null</tt>.
    **/
    public Class loadClassFromModule(String name)
    {
        try
        {
            return findClass(name);
        } catch (Throwable th) {
            // Not much we can do.
// TODO: Do something with this error message.
//            System.err.println("ModuleClassLoader: " + th.getMessage());
        }
        return null;
    }

    /**
     * <p>
     * This method is nearly an exact copy of the ClassLoader.getResource()
     * method. The main difference is that it delegates to its associated
     * <tt>ModuleManager</tt>'s search policy before calling the
     * <tt>ClassLoader.findResource()</tt> method.
     * </p>
     * @param name the class to be loaded.
     * @return a URL to the resource or <tt>null</tt> if the resource was not found.
    **/
    public URL getResource(String name)
    {
        URL url = null;

        // Ask the search policy for the resource.
        if (m_mgr.getSearchPolicy() != null)
        {
            try
            {
                url = m_mgr.getSearchPolicy().findResource(getParent(), m_module, name);
            }
            catch (ResourceNotFoundException ex)
            {
                // We return null here because if SearchPolicy.findResource()
                // throws an exception we interpret that to mean that the
                // search should be stopped.
                return null;
            }
        }

        // If not found, then search locally.
        if (url == null)
        {
            url = findResource(name);
        }

        return url;
    }

    /**
     * <p>
     * This method overriden from from <tt>ClassLoader</tt>.
     * It is implemented such that it loads resources from the set of
     * <tt>ResourceSource</tt>s from its associated module.
     * </p>
     * @param name the name of the resource to load.
     * @return the <tt>URL</tt> associated with the resource or <tt>null</tt>.
    **/
    protected URL findResource(String name)
    {
        URL url = null;

        // If the parent is used as a source, try to
        // load the class from it.
        if (m_useParentSource)
        {
            url = (getParent() == null) ? null : getParent().getResource(name);
        }

        // Try to load the resource from the module's resource
        // sources.
        if (url == null)
        {
            // Remove leading slash, if present.
            if (name.startsWith("/"))
            {
                name = name.substring(1);
            }

            ResourceSource[] sources = m_module.getResourceSources();
            for (int i = 0;
                (url == null) && (sources != null) && (i < sources.length);
                i++)
            {
                if (sources[i].hasResource(name))
                {
                    url = m_mgr.getURLPolicy().createResourceURL(m_mgr, m_module, i, name);
                }
            }
        }

        return url;
    }

    /**
     * <p>
     * This method is used by <tt>SearchPolicy</tt> instances when they want
     * to load a resource from a module. The search policy is initially invoked when
     * <tt>ModuleClassLoader.loadClass()</tt> delegates a resource loading
     * request to it. In general, the ultimate goal of the search policy is to
     * return a resource from another module if possible. Unfortunately, if a search
     * policy tries to directly load a resource from another module's class loader, an
     * infinite loop will result because the module's class loader will delegate the
     * request back to the search policy. To avoid this situation, search policies
     * must use this method when trying to load a resource from a module.
     * </p>
     * @param name the name of the resource to load.
     * @return a URL to the resource or <tt>null</tt>.
    **/
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

    protected Enumeration findResources(String name)
    {
        Vector v = new Vector();
        // If the parent is used as a source, try to
        // load the class from it.
        if (m_useParentSource)
        {
            try
            {
                Enumeration e = (getParent() == null)
                    ? null : getParent().getResources(name);
                while ((e != null) && e.hasMoreElements())
                {
                    v.addElement(e.nextElement());
                }
            }
            catch (IOException ex)
            {
                // What can we do?
            }
        }

        // Remove leading slash, if present.
        if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        // Try to load the resource from the module's resource
        // sources.

        ResourceSource[] sources = m_module.getResourceSources();
        for (int i = 0; (sources != null) && (i < sources.length); i++)
        {
            if (sources[i].hasResource(name))
            {
                v.addElement(m_mgr.getURLPolicy().createResourceURL(m_mgr, m_module, i, name));
            }
        }

        return v.elements();
    }

    /**
     * <p>
     * This method overriden from from <tt>ClassLoader</tt>. It maps a library
     * name to a library path by consulting the <tt>LibrarySource</tt>s of the
     * class loader's module.
     * </p>
     * @param name the name of the library to find.
     * @return the file system path of library or <tt>null</tt>
    **/
    protected String findLibrary(String name)
    {
        // Remove leading slash, if present.
        if (name.startsWith("/"))
        {
            name = name.substring(1);
        }

        LibrarySource[] sources = m_module.getLibrarySources();
        for (int i = 0;
            (sources != null) && (i < sources.length);
            i++)
        {
            String path = sources[i].getPath(name);
            if (path != null)
            {
                return path;
            }
        }

        return null;
    }
}
