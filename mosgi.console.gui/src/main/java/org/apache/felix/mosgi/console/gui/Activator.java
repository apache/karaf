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
package org.apache.felix.mosgi.console.gui;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.JSplitPane;
import javax.swing.event.EventListenerList;

import org.apache.felix.mosgi.console.ifc.Plugin;
import org.apache.felix.mosgi.console.ifc.CommonPlugin;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.InvalidSyntaxException;

public class Activator implements BundleActivator {
  protected BundleContext m_context = null;
  protected ArrayList m_pluginList = null;
  protected ArrayList m_commonpluginList = null; //TODO To I need this table ?
  private EventListenerList m_listenerList = null;
  private JFrame m_frame = null;
  private NodesTree nodesTree=null;

  public Activator() {
    m_pluginList = new ArrayList();
    m_commonpluginList = new ArrayList();
    m_listenerList = new EventListenerList();
  }

  public void start(BundleContext context) {
    m_context = context;

    // Listen for factory service events.
    ServiceListener sl = new ServiceListener() {
      public void serviceChanged(ServiceEvent event) {
        ServiceReference ref = event.getServiceReference();
        Object svcObj = m_context.getService(ref);
        if (event.getType() == ServiceEvent.REGISTERED) {
           synchronized (Activator.this) {
              // !!!!!!!!!! ORDER MATTERS (Inheritance pb)
              if (!m_pluginList.contains(svcObj)) {
                 if(svcObj instanceof CommonPlugin){
                   m_commonpluginList.add(svcObj);
                   firePropertyChangedEvent(CommonPlugin.COMMON_PLUGIN_ADDED, null, svcObj);
                 }else if (svcObj instanceof Plugin){
                   m_pluginList.add(svcObj);
                   firePropertyChangedEvent(Plugin.PLUGIN_ADDED, null, svcObj);
                 }
               }
             }
           } else if (event.getType() == ServiceEvent.UNREGISTERING) {
             synchronized (Activator.this) {
               removePropertyChangeListener((PropertyChangeListener)svcObj);
               if(svcObj instanceof CommonPlugin){
                 m_commonpluginList.remove(svcObj);
                 firePropertyChangedEvent(CommonPlugin.COMMON_PLUGIN_REMOVED, null, svcObj);
               }else if (svcObj instanceof Plugin){
                 m_pluginList.remove(svcObj);
                 firePropertyChangedEvent(Plugin.PLUGIN_REMOVED, null, svcObj);
               }
             }
           } else {
             m_context.ungetService(ref);
           }
         }
        };
        try
        {
            m_context.addServiceListener(sl,
                "(|(objectClass="
                + Plugin.class.getName()
                + ")(objectClass="
                + CommonPlugin.class.getName()+"))");
        }
        catch (InvalidSyntaxException ex)
        {
            System.err.println("ShellGuiActivator: Cannot add service listener.");
            System.err.println("ShellGuiActivator: " + ex);
        }


        // Create and display the frame.
        if (m_frame == null)
        {
            m_frame=new JFrame("OSGi GUI Remote Manager");
            m_frame.setUndecorated(true);
            m_frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
            Dimension wndSize = m_frame.getToolkit().getScreenSize();
            m_frame.setBounds(wndSize.width / 8, wndSize.height / 8, 1000, 700);
            m_frame.setIconImage(Toolkit.getDefaultToolkit().getImage(m_context.getBundle().getResource("images/logo.gif")));
           
            //Right panel
            JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new NodePanel(this, context) , new CommonPanel(this));
            rightSplitPane.setOneTouchExpandable(true);
            rightSplitPane.setDividerLocation(500);

            //General Panel
            this.nodesTree = new NodesTree(this, context);
            JSplitPane gSplitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.nodesTree , rightSplitPane);
            gSplitPane.setOneTouchExpandable(true);
            gSplitPane.setDividerLocation(200);


            m_frame.getContentPane().add(gSplitPane);
            //m_frame.setResizable(false);
            m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }

        // Now try to manually initialize the plugin list
        // since some might already be available.
        //initializePlugins();
        this.nodesTree.runDiscovery();

        m_frame.setVisible(true);
    }

    private synchronized void initializePlugins() {
      try {
        // Get all model services.
        Object svcObj=null;
        ServiceReference refs[] = m_context.getServiceReferences(Plugin.class.getName(), null);
        if (refs != null) {
          // Add model services to list, ignore duplicates.
          for (int i = 0; i < refs.length; i++) {
            svcObj = m_context.getService(refs[i]);
            if (!m_pluginList.contains(svcObj)) {
              m_pluginList.add(svcObj);
              firePropertyChangedEvent(Plugin.PLUGIN_ADDED, null, (Plugin)svcObj);
            }
          }
        }
        // Get all common plugins
        refs = m_context.getServiceReferences(CommonPlugin.class.getName(), null);
        if (refs != null) {
          for (int i = 0; i < refs.length; i++) {
            svcObj = m_context.getService(refs[i]);
            if (!m_commonpluginList.contains(svcObj)) {
              m_commonpluginList.add(svcObj);
              firePropertyChangedEvent(CommonPlugin.COMMON_PLUGIN_ADDED, null, (CommonPlugin)svcObj);
            }
          }
        }
      } catch (Exception ex) {
        System.err.println("ShellGuiActivator: Error initializing model list.");
        System.err.println("ShellGuiActivator: " + ex);
        ex.printStackTrace();
      }
    }

    public void stop(BundleContext context)
    {
        if (m_frame != null)
        {   this.nodesTree.stop();
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

    public void removePropertyChangeListener(PropertyChangeListener l)
    {
        m_listenerList.remove(PropertyChangeListener.class, l);
    }

    public void firePropertyChangedEvent(String name, Object oldValue, Object newValue)
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
