/* 
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.    
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

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Activator implements BundleActivator {
  protected BundleContext m_context = null;

  protected ArrayList m_pluginList = null;
  protected ArrayList m_commonpluginList = null;  //TODO Do I need this table ?
  private EventListenerList m_listenerList = null;
  
  private JFrame m_frame = null;
  private NodesTree nodesTree=null;

  public Activator() {
    m_pluginList = new ArrayList();
    m_commonpluginList = new ArrayList();
    m_listenerList = new EventListenerList();
  }

  ///////////////////////////////////////
  //         BundleActivator           //
  ///////////////////////////////////////
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
    try {
      m_context.addServiceListener(sl,
				   "(|(objectClass="
				   + Plugin.class.getName()
				   + ")(objectClass="
				   + CommonPlugin.class.getName()+"))");
    }
    catch (InvalidSyntaxException ex) {
      System.err.println("ShellGuiActivator: Cannot add service listener.");
      System.err.println("ShellGuiActivator: " + ex);
    }

    // Create and display the frame.
    if (m_frame == null) {
      m_frame=new JFrame("OSGi GUI Remote Manager");
      m_frame.setUndecorated(true);
      m_frame.getRootPane().setWindowDecorationStyle(JRootPane.FRAME);
      m_frame.setIconImage(Toolkit.getDefaultToolkit().getImage(m_context.getBundle().getResource("images/logo.gif")));
      //m_frame.setResizable(false);
      //m_frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      // TODO : add a windowListener and use a Preferences service to save screen size
      m_frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent we) {
          JFrame jf=(JFrame) we.getWindow();
          System.out.println(" Console.gui : window closing ("+jf.getSize().height+"*"+jf.getSize().width+")");
        }
        public void windowClosed(WindowEvent we) {
          JFrame jf=(JFrame) we.getWindow();
          System.out.println(" Console.gui : window closed ("+jf.getSize().height+"*"+jf.getSize().width+")");
        }
      });
	    
      Dimension maxdim = m_frame.getToolkit().getScreenSize();
      int m_width=maxdim.width-100;
      int m_height=maxdim.height-100;
      m_frame.setBounds( (int) ((maxdim.width-m_width)/2), 20, m_width, m_height);
           
      //Right panel
      JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new NodePanel(this, context) , new CommonPanel(this));
      rightSplitPane.setOneTouchExpandable(true);
      rightSplitPane.setDividerLocation((int) (m_height*2/3));

      //General Panel
      this.nodesTree = new NodesTree(this, context);
      JSplitPane gSplitPane=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.nodesTree , rightSplitPane);
      gSplitPane.setOneTouchExpandable(true);
      gSplitPane.setDividerLocation((int) (m_width/4));

      m_frame.getContentPane().add(gSplitPane);
    }

    // Now try to manually initialize the plugin list
    // since some might already be available.
    // initializePlugins();
    
    m_frame.setVisible(true);
    this.nodesTree.runDiscovery();
  }
 
  public void stop(BundleContext context) {
    if (m_frame != null) {
      this.nodesTree.stop();
      m_frame.setVisible(false);
      m_frame.dispose();
      m_frame = null;
    }
  }

  ////////////////////////////////////
  //
  ////////////////////////////////////
  /*private synchronized void initializePlugins() { // Never used ?
    System.out.println("??? private synchronized void initializePlugins() ???");
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
  }*/

  //////////////////////////////
  // Event methods.           //
  //////////////////////////////
  public void addPropertyChangeListener(PropertyChangeListener l) {
    m_listenerList.add(PropertyChangeListener.class, l);
  }

  public void removePropertyChangeListener(PropertyChangeListener l) {
    m_listenerList.remove(PropertyChangeListener.class, l);
  }

  public void firePropertyChangedEvent(String name, Object oldValue, Object newValue) {
    //System.out.println("[Gui Activator] fire PCE("+name+","+oldValue+","+newValue+")");
    PropertyChangeEvent event = null;
    // Guaranteed to return a non-null array
    Object[] listeners = m_listenerList.getListenerList();

    // Process the listeners last to first, notifying
    // those that are interested in this event 
    for (int i = listeners.length - 2; i >= 0; i -= 2) {
      if (listeners[i] == PropertyChangeListener.class) {
        // Lazily create the event:
        if (event == null) {
          event = new PropertyChangeEvent(this, name, oldValue, newValue);
        }
        ((PropertyChangeListener) listeners[i + 1]).propertyChange(event);
      }
    }
  }

}
