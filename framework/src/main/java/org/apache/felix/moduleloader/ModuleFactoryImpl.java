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

import java.util.HashMap;
import java.util.Map;

import org.apache.felix.framework.Logger;

public class ModuleFactoryImpl implements IModuleFactory
{
    private Logger m_logger = null;
    private Map m_moduleMap = new HashMap();

    private ModuleListener[] m_listeners = null;
    private static final ModuleListener[] m_noListeners = new ModuleListener[0];

    public ModuleFactoryImpl(Logger logger)
    {
        m_logger = logger;
        m_listeners = m_noListeners;
    }

    public synchronized IModule[] getModules()
    {
        return (IModule[]) m_moduleMap.values().toArray(new IModule[m_moduleMap.size()]);
    }

    public synchronized IModule getModule(String id)
    {
        return (IModule) m_moduleMap.get(id);
    }

    public IModule createModule(String id, IModuleDefinition md)
    {
        IModule module = null;

        // Use a synchronized block instead of synchronizing the
        // method, so we can fire our event outside of the block.
        synchronized (this)
        {
            if (m_moduleMap.get(id) == null)
            {
                module = new ModuleImpl(m_logger, id, md);
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
     * This is an experimental method that is likely to change or go
     * away - so don't use it for now.
     *
     * Note to self, we need to think about what the implications of
     * this are and whether we are fine with them.
     */
    public void refreshModule(IModule module)
    {
        boolean fire = false;

        synchronized (this)
        {
            fire = (m_moduleMap.get(module.getId()) != null);
        }

        if (fire)
        {
            fireModuleRefreshed(module);
        }
    }

    public void removeModule(IModule module)
    {
        // Use a synchronized block instead of synchronizing the
        // method, so we can fire our event outside of the block.
        synchronized (this)
        {
            if (m_moduleMap.get(module.getId()) != null)
            {
                // Remove from data structures.
                m_moduleMap.remove(module.getId());
                // Close the module's content loader to deinitialize it.
                // TODO: This is not really the best place for this, but at
                // least it is centralized with the call to IContentLoader.open()
                // when a module's content loader is set below.
                ((ModuleImpl) module).getContentLoader().close();
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

    public void setContentLoader(IModule module, IContentLoader contentLoader)
    {
        synchronized (this)
        {
            ((ModuleImpl) module).setContentLoader(contentLoader);
            // Open the module's content loader to initialize it.
            // TODO: This is not really the best place for this, but at
            // least it is centralized with the call to IContentLoader.close()
            // when the module is removed above.
            contentLoader.open();
        }
    }

    public void setSecurityContext(IModule module, Object securityContext)
    {
        synchronized (this)
        {
            ((ModuleImpl) module).setSecurityContext(securityContext);
        }
    }

    /**
     * <p>
     * Adds a listener to the <tt>IModuleFactory</tt> to listen for
     * module added and removed events.
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
     * Removes a listener from the <tt>IModuleFactory</tt>.
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
     * to the <tt>IModuleFactory</tt>.
     * </p>
     * @param module the module that was added.
    **/
    protected void fireModuleAdded(IModule module)
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
     * Fires an event indicating that the specified module was removed
     * from the <tt>IModuleFactory</tt>.
     * </p>
     * @param module the module that was removed.
    **/
    protected void fireModuleRemoved(IModule module)
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

    /**
     * This is an experimental method that is likely to change or go
     * away - so don't use it for now.
     *
     * Note to self, we need to think about what the implications of
     * this are and whether we are fine with them.
     */
    protected void fireModuleRefreshed(IModule module)
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
            listeners[i].moduleRefreshed(event);
        }
    }
}