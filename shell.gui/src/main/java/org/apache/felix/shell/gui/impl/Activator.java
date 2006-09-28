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
package org.apache.felix.shell.gui.impl;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.event.EventListenerList;

import org.apache.felix.shell.gui.Plugin;
import org.osgi.framework.*;

public class Activator implements BundleActivator
{
    private BundleContext m_context = null;
    private List m_pluginList = null;
    private EventListenerList m_listenerList = null;
    private JFrame m_frame = null;

    public static final String PLUGIN_LIST_PROPERTY = "pluginList";

    public Activator()
    {
        m_pluginList = new ArrayList();
        m_listenerList = new EventListenerList();
    }

    public synchronized int getPluginCount()
    {
        if (m_pluginList == null)
        {
            return 0;
        }
        return m_pluginList.size();
    }

    public synchronized Plugin getPlugin(int i)
    {
        if ((i < 0) || (i >= getPluginCount()))
        {
            return null;
        }
        return (Plugin) m_pluginList.get(i);
    }

    public synchronized boolean pluginExists(Plugin plugin)
    {
        for (int i = 0; i < m_pluginList.size(); i++)
        {
            if (m_pluginList.get(i) == plugin)
            {
                return true;
            }
        }
        return false;
    }

    //
    // Bundle activator methods.
    //

    public void start(BundleContext context)
    {
        m_context = context;

        // Listen for factory service events.
        ServiceListener sl = new ServiceListener() {
            public void serviceChanged(ServiceEvent event)
            {
                ServiceReference ref = event.getServiceReference();
                Object svcObj = m_context.getService(ref);
                if ((event.getType() == ServiceEvent.REGISTERED) &&
                    (svcObj instanceof Plugin))
                {
                    synchronized (Activator.this)
                    {
                        // Check for duplicates.
                        if (!m_pluginList.contains(svcObj))
                        {
                            m_pluginList.add(svcObj);
                            firePropertyChangedEvent(
                                PLUGIN_LIST_PROPERTY, null, null);
                        }
                    }
                }
                else if ((event.getType() == ServiceEvent.UNREGISTERING) &&
                    (svcObj instanceof Plugin))
                {
                    synchronized (Activator.this)
                    {
                        m_pluginList.remove(svcObj);
                        firePropertyChangedEvent(
                            PLUGIN_LIST_PROPERTY, null, null);
                    }
                }
                else
                {
                    m_context.ungetService(ref);
                }
            }
        };
        try
        {
            m_context.addServiceListener(sl,
                "(objectClass="
                + org.apache.felix.shell.gui.Plugin.class.getName()
                + ")");
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println("ShellGuiActivator: Cannot add service listener.");
            System.err.println("ShellGuiActivator: " + ex);
        }

        // Now try to manually initialize the plugin list
        // since some might already be available.
        initializePlugins();

        // Create and display the frame.
        if (m_frame == null)
        {
            ShellPanel panel = new ShellPanel(this);
            m_frame = new JFrame("Felix GUI Shell");
            m_frame.getContentPane().setLayout(new BorderLayout());
            m_frame.getContentPane().add(panel);
            m_frame.pack();
            m_frame.setSize(700, 400);
            m_frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            m_frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent event)
                {
                    if (m_context.getBundle().getState() == Bundle.ACTIVE)
                    {
                        try
                        {
                            m_context.getBundle().stop();
                        }
                        catch (Exception ex)
                        {
                            System.err.println("ShellGuiActivator: " + ex);
                        }
                    }
                }
            });
        }

        m_frame.setVisible(true);
    }

    private synchronized void initializePlugins()
    {
        try
        {
            // Get all model services.
            ServiceReference refs[] = m_context.getServiceReferences(
                org.apache.felix.shell.gui.Plugin.class.getName(), null);
            if (refs != null)
            {
                // Add model services to list, ignore duplicates.
                for (int i = 0; i < refs.length; i++)
                {
                    Object svcObj = m_context.getService(refs[i]);
                    if (!m_pluginList.contains(svcObj))
                    {
                        m_pluginList.add(svcObj);
                    }
                }
                firePropertyChangedEvent(
                    PLUGIN_LIST_PROPERTY, null, null);
            }
        }
        catch (Exception ex)
        {
            System.err.println("ShellGuiActivator: Error initializing model list.");
            System.err.println("ShellGuiActivator: " + ex);
            ex.printStackTrace();
        }
    }

    public void stop(BundleContext context)
    {
        if (m_frame != null)
        {
            m_frame.setVisible(false);
            m_frame.dispose();
            m_frame = null;
        }
    }

    //
    // Event methods.
    //

    public void addPropertyChangeListener(PropertyChangeListener l)
    {
        m_listenerList.add(PropertyChangeListener.class, l);
    }

    public void removeFooListener(PropertyChangeListener l)
    {
        m_listenerList.remove(PropertyChangeListener.class, l);
    }

    protected void firePropertyChangedEvent(String name, Object oldValue, Object newValue)
    {
        PropertyChangeEvent event = null;

        // Guaranteed to return a non-null array
        Object[] listeners = m_listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2)
        {
            if (listeners[i] == PropertyChangeListener.class)
            {
                // Lazily create the event:
                if (event == null)
                {
                    event = new PropertyChangeEvent(this, name, oldValue, newValue);
                }
                ((PropertyChangeListener) listeners[i + 1]).propertyChange(event);
            }
        }
    }
}