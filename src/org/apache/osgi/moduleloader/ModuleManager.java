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
package org.apache.osgi.moduleloader;

import java.util.*;

/**
 * <p>
 * The <tt>ModuleManager</tt> class is the core facility for defining a
 * re-usable, policy-driven class loader for applications that require
 * flexible class loading mechanisms. The <tt>ModuleManager</tt> is not
 * class loader itself, but it supports the concept of a
 * <a href="Module.html"><tt>Module</tt></a>,
 * which is a unit of organization for application classes and resources.
 * The <tt>ModuleManager</tt> has only a handful of methods that allow
 * an application to add, remove, reset, and query modules; the intent
 * is to place as few assumptions in the <tt>ModuleManager</tt> as possible.
 * </p>
 * <p>
 * The idea is simple, allow the application to map itself into modules
 * however it sees fit and let the <tt>ModuleManager</tt> assume the
 * responsibility of managing the modules and loading classes and resources
 * from them as necessary via <a href="ModuleClassLoader.html"><tt>ModuleClassLoader</tt></a>s
 * that are associated with each module. In order to achieve this goal, though, the
 * <tt>ModuleManager</tt> must make at least one assumption on behalf of
 * the application. This assumption is that the loading of classes and resources
 * from the available modules must happen using a search algorithm
 * that is particular to the application itself. As a result of this assumption,
 * the <tt>ModuleManager</tt> requires that the application provide a concrete
 * implementation of the <a href="SearchPolicy.html"><tt>SearchPolicy</tt></a>
 * interface.
 * </p>
 * <p>
 * The search policy allows the <tt>ModuleLoader</tt> to let applications inject
 * their own particular class loading policies, without dictating strict or
 * constraining base assumptions. Of course, it is likely that many applications
 * will use the same or very similar search policies. Because of this, another
 * goal of the <tt>ModuleLoader</tt> approach is to foster a common library of
 * search policies that applications are free to use or customize as they see
 * fit. These common search policies are analagous to patterns, where each search
 * policy is viewable as a <i>class loading pattern</i>. Some initial search
 * policies included with the <tt>ModuleLoader</tt> are
 * <a href="search/ExhaustiveSearchPolicy.html"><tt>ExhaustiveSearchPolicy</tt></a>,
 * <a href="search/SelfContainedSearchPolicy.html"><tt>SelfContainedSearchPolicy</tt></a>, and
 * <a href="search/ImportSearchPolicy.html"><tt>ImportSearchPolicy</tt></a>.
 * </p>
 * <p>
 * Due to the fact that class loaders are tied to security and resource loading,
 * the search policy alone is not sufficient for the <tt>ModuleLoader</tt> to
 * perform its function. To fulfill these other purposes, the <tt>ModuleLoader</tt>
 * introduces another policy interface, called the <a href="URLPolicy.html"><tt>URLPolicy</tt></a>.
 * The <tt>URLPolicy</tt> allows the application to inject its particular policy
 * for to purposes:
 * </p>
 * <ol>
 *   <li>Creating the <tt>URL</tt> associated with loading a resource, such as
 *       the <tt>URL</tt> returned from a call to <tt>Class.getResource()</tt>.
 *   </li>
 *   <li>Creating the <tt>URL</tt> that will be associated with a class's
 *       <tt>CodeSource</tt> when defining the class for purposes of security
 *       and assigning permissions.
 *   </li>
 * </ol>
 * <p>
 * The <tt>ModuleLoader</tt> defines a default <tt>URLPolicy</tt>, called
 * <a href="DefaultURLPolicy.html"><tt>DefaultURLPolicy</tt></a>, that provides
 * a simple <tt>URLStreamHandler</tt> for accessing resources inside of modules
 * and that returns <tt>null</tt> for the <tt>CodeSource</tt> <tt>URL</tt>.
 * Applications only need to supply their own <tt>URLPolicy</tt> if the default
 * one does not provide the appropriate behavior.
 * </p>
 * <p>
 * It is possible for an application to create multiple instances of the
 * <tt>ModuleManager</tt> within a single JVM, but it is not possible to
 * share modules across multiple <tt>ModuleManager</tt>s. A given <tt>ModuleManager</tt>
 * can only have one <tt>SelectionPolicy</tt> and one <tt>URLPolicy</tt>.
 * </p>
 * @see org.apache.osgi.moduleloader.Module
 * @see org.apache.osgi.moduleloader.ModuleClassLoader
 * @see org.apache.osgi.moduleloader.SearchPolicy
 * @see org.apache.osgi.moduleloader.URLPolicy
 * @see org.apache.osgi.moduleloader.DefaultURLPolicy
**/
public class ModuleManager
{
    private List m_moduleList = new ArrayList();
    private Map m_moduleMap = new HashMap();
    private SearchPolicy m_searchPolicy = null;
    private URLPolicy m_urlPolicy = null;
    private ModuleListener[] m_listeners = null;
    private static final ModuleListener[] m_noListeners = new ModuleListener[0];

    /**
     * <p>
     * Constructs a <tt>ModuleManager</tt> instance using the specified
     * search policy and the default <tt>URL</tt> policy.
     * </p>
     * @param searchPolicy the search policy that the instance should use.
     * @see org.apache.osgi.moduleloader.SearchPolicy
    **/
    public ModuleManager(SearchPolicy searchPolicy)
    {
        this(searchPolicy, null);
    }

    /**
     * <p>
     * Constructs a <tt>ModuleManager</tt> instance using the specified
     * search policy and the specified <tt>URL</tt> policy.
     * </p>
     * @param searchPolicy the search policy that the instance should use.
     * @param urlPolicy the <tt>URL</tt> policy that the instance should use.
     * @see org.apache.osgi.moduleloader.SearchPolicy
     * @see org.apache.osgi.moduleloader.URLPolicy
    **/
    public ModuleManager(SearchPolicy searchPolicy, URLPolicy urlPolicy)
    {
        m_listeners = m_noListeners;
        m_searchPolicy = searchPolicy;
        m_searchPolicy.setModuleManager(this);

        if (urlPolicy == null)
        {
            m_urlPolicy = new DefaultURLPolicy();
        }
        else
        {
            m_urlPolicy = urlPolicy;
        }
    }

    /**
     * <p>
     * Returns the <tt>URL</tt> policy used by this instance.
     * </p>
     * @return the <tt>URL</tt> policy used by this instance.
     * @see org.apache.osgi.moduleloader.URLPolicy
    **/
    public URLPolicy getURLPolicy()
    {
        return m_urlPolicy;
    }

    /**
     * <p>
     * Returns the search policy used by this instance.
     * </p>
     * @return the search policy used by this instance.
     * @see org.apache.osgi.moduleloader.SearchPolicy
    **/
    public SearchPolicy getSearchPolicy()
    {
        return m_searchPolicy;
    }

    /**
     * <p>
     * Returns an array of all modules being managed by the
     * <tt>ModuleManager</tt> instance. The array contains a snapshot of
     * all modules in the <tt>ModuleManager</tt> at the time when this
     * method was called.
     * </p>
     * @return an array of all modules being managed by the <tt>ModuleManager</tt>
     *         instance.
     * @see org.apache.osgi.moduleloader.Module
    **/
    public synchronized Module[] getModules()
    {
        Module[] modules = new Module[m_moduleList.size()];
        return (Module[]) m_moduleList.toArray(modules);
    }

    /**
     * <p>
     * Returns a module associated with the specified identifier.
     * </p>
     * @param id the identifier for the module to be retrieved.
     * @return the module associated with the identifier or <tt>null</tt>.
     * @see org.apache.osgi.moduleloader.Module
    **/
    public synchronized Module getModule(String id)
    {
        return (Module) m_moduleMap.get(id);
    }

    /**
     * <p>
     * Adds a module to the module manager. The module will have the specified
     * unique identifier, with the associated attributes, resource sources, and
     * library sources. If the identifier is not unique, then an exception is
     * thrown.
     * </p>
     * @param id the unique identifier of the new module.
     * @param attributes an array of key-value attribute pairs to
     *        associate with the module.
     * @param resSources an array of <tt>ResourceSource</tt>s to associate
     *        with the module.
     * @param libSources an array of <tt>LibrarySource</tt>s to associate
     *        with the module.
     * @return the newly created module.
     * @throws java.lang.IllegalArgumentException if the module identifier
     *         is not unique.
     * @see org.apache.osgi.moduleloader.Module
     * @see org.apache.osgi.moduleloader.ResourceSource
     * @see org.apache.osgi.moduleloader.LibrarySource
    **/
    public Module addModule(String id, Object[][] attributes,
        ResourceSource[] resSources, LibrarySource[] libSources)
    {
        return addModule(id, attributes, resSources, libSources, false);
    }

    public Module addModule(String id, Object[][] attributes,
        ResourceSource[] resSources, LibrarySource[] libSources,
        boolean useParentSource)
    {
        Module module = null;

        // Use a synchronized block instead of synchronizing the
        // method, so we can fire our event outside of the block.
        synchronized (this)
        {
            if (m_moduleMap.get(id) == null)
            {
                module = new Module(this, id, attributes, resSources, libSources, useParentSource);
                m_moduleList.add(module);
                m_moduleMap.put(id, module);
            }
            else
            {
                throw new IllegalArgumentException("Module ID must be unique.");
            }
        }

        // Fire event here instead of inside synchronized block.
        fireModuleAdded(module);

        return module;
    }

    /**
     * <p>
     * Resets a given module. In resetting a module, the module's associated
     * class loader is thrown away; it is the application's responsibility to
     * determine when and how that application code stops using classes (and
     * subsequent instances) from the class loader of the reset module.
     * This method allows the associated elements of the module (i.e.,
     * attributes, resource sources, and library sources) to be changed also;
     * if these elements have not changed then they simply need to be passed
     * back in from the existing module. This method is useful in situations
     * where the underlying module needs to be changed at run time, such as
     * might be necessary if a module was updated.
     * </p>
     * <p>
     * The same effect could be achieved by first removing and then re-adding
     * a module, but with one subtle different. By removing and then re-adding
     * a module, a new module is created and, thus, all existing references
     * become invalid. By explicitly having this method, the <tt>ModuleManager</tt>
     * maintains the integrity of the module reference, which is more intuitive
     * in the case where an updated module is intended to be the same module,
     * only updated.
     * </p>
     * @param module the module reset.
     * @param attributes an array of key-value attribute pairs to
     *        associate with the module.
     * @param resSources an array of <tt>ResourceSource</tt>s to associate
     *        with the module.
     * @param libSources an array of <tt>LibrarySource</tt>s to associate
     *        with the module.
     * @see org.apache.osgi.moduleloader.Module
     * @see org.apache.osgi.moduleloader.ResourceSource
     * @see org.apache.osgi.moduleloader.LibrarySource
    **/
    public void resetModule(
        Module module, Object[][] attributes,
        ResourceSource[] resSources, LibrarySource[] libSources)
    {
        // Use a synchronized block instead of synchronizing the
        // method, so we can fire our event outside of the block.
        synchronized (this)
        {
            module = (Module) m_moduleMap.get(module.getId());
            if (module != null)
            {
                module.reset(attributes, resSources, libSources);
            }
            else
            {
                // Don't fire event.
                return;
            }
        }

        // Fire event here instead of inside synchronized block.
        fireModuleReset(module);
    }

    /**
     * <p>
     * Removes the specified module from the <tt>ModuleManager</tt>. Removing
     * a module only removed the module from the <tt>ModuleManager</tt>. It is
     * the application's responsibility to determine when and how application code
     * stop using classes (and subsequent instances) that were loaded from
     * the class loader of the removed module.
     * </p>
     * @param module the module to remove.
    **/
    public void removeModule(Module module)
    {
        // Use a synchronized block instead of synchronizing the
        // method, so we can fire our event outside of the block.
        synchronized (this)
        {
            if (m_moduleMap.get(module.getId()) != null)
            {
                // Remove from data structures.
                m_moduleList.remove(module);
                m_moduleMap.remove(module.getId());

                // Dispose of the module.
                module.dispose();
            }
            else
            {
                // Don't fire event.
                return;
            }
        }

        // Fire event here instead of inside synchronized block.
        fireModuleRemoved(module);
    }

    /**
     * <p>
     * Adds a listener to the <tt>ModuleManager</tt> to listen for
     * module added, reset, and removed events.
     * </p>
     * @param l the <tt>ModuleListener</tt> to add.
    **/
    public void addModuleListener(ModuleListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }

        // Use the m_noListeners object as a lock.
        synchronized (m_noListeners)
        {
            // If we have no listeners, then just add the new listener.
            if (m_listeners == m_noListeners)
            {
                m_listeners = new ModuleListener[] { l };
            }
            // Otherwise, we need to do some array copying.
            // Notice, the old array is always valid, so if
            // the dispatch thread is in the middle of a dispatch,
            // then it has a reference to the old listener array
            // and is not affected by the new value.
            else
            {
                ModuleListener[] newList = new ModuleListener[m_listeners.length + 1];
                System.arraycopy(m_listeners, 0, newList, 0, m_listeners.length);
                newList[m_listeners.length] = l;
                m_listeners = newList;
            }
        }
    }

    /**
     * <p>
     * Removes a listener from the <tt>ModuleManager</tt>.
     * </p>
     * @param l the <tt>ModuleListener</tt> to remove.
    **/
    public void removeModuleListener(ModuleListener l)
    {
        // Verify listener.
        if (l == null)
        {
            throw new IllegalArgumentException("Listener is null");
        }

        // Use the m_noListeners object as a lock.
        synchronized (m_noListeners)
        {
            // Try to find the instance in our list.
            int idx = -1;
            for (int i = 0; i < m_listeners.length; i++)
            {
                if (m_listeners[i].equals(l))
                {
                    idx = i;
                    break;
                }
            }

            // If we have the instance, then remove it.
            if (idx >= 0)
            {
                // If this is the last listener, then point to empty list.
                if (m_listeners.length == 1)
                {
                    m_listeners = m_noListeners;
                }
                // Otherwise, we need to do some array copying.
                // Notice, the old array is always valid, so if
                // the dispatch thread is in the middle of a dispatch,
                // then it has a reference to the old listener array
                // and is not affected by the new value.
                else
                {
                    ModuleListener[] newList = new ModuleListener[m_listeners.length - 1];
                    System.arraycopy(m_listeners, 0, newList, 0, idx);
                    if (idx < newList.length)
                    {
                        System.arraycopy(m_listeners, idx + 1, newList, idx,
                            newList.length - idx);
                    }
                    m_listeners = newList;
                }
            }
        }
    }

    /**
     * <p>
     * Fires an event indicating that the specified module was added
     * to the <tt>ModuleManager</tt>.
     * </p>
     * @param module the module that was added.
    **/
    protected void fireModuleAdded(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ModuleListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(this, module);
            }
            listeners[i].moduleAdded(event);
        }
    }

    /**
     * <p>
     * Fires an event indicating that the specified module was reset.
     * </p>
     * @param module the module that was reset.
    **/
    protected void fireModuleReset(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ModuleListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(this, module);
            }
            listeners[i].moduleReset(event);
        }
    }

    /**
     * <p>
     * Fires an event indicating that the specified module was removed
     * from the <tt>ModuleManager</tt>.
     * </p>
     * @param module the module that was removed.
    **/
    protected void fireModuleRemoved(Module module)
    {
        // Event holder.
        ModuleEvent event = null;

        // Get a copy of the listener array, which is guaranteed
        // to not be null.
        ModuleListener[] listeners = m_listeners;

        // Loop through listeners and fire events.
        for (int i = 0; i < listeners.length; i++)
        {
            // Lazily create event.
            if (event == null)
            {
                event = new ModuleEvent(this, module);
            }
            listeners[i].moduleRemoved(event);
        }
    }
}