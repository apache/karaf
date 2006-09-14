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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URLClassLoader;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Set;
import javax.swing.JTabbedPane;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
//import javax.management.MalformedObjectNameException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import org.apache.felix.mosgi.console.ifc.Plugin;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;

public class NodePanel extends JTabbedPane implements PropertyChangeListener, ChangeListener {
  //private String repo;
  private Activator m_activator = null;
  private BundleContext m_context = null;
  
  private Hashtable pluginList = null;

  public NodePanel(Activator activator, BundleContext context){
    m_context = context;
    m_activator = activator;
    m_activator.addPropertyChangeListener(this);
    this.pluginList = new Hashtable();
    //repo = m_context.getProperty("insa.jmxconsole.repository");
    this.addChangeListener(this);
  }

  //////////////////////////////////////
  // ChangeListener Implementation
  //////////////////////////////////////
  public void stateChanged(ChangeEvent e){
//   System.out.println("coucou => "+((JTabbedPane)e.getSource()).getSelectedIndex()+":"+((JTabbedPane)e.getSource()).getSelectedComponent().getName());
   if (this.getSelectedComponent()!=null){
     m_activator.firePropertyChangedEvent(Plugin.PLUGIN_ACTIVATED, null, this.getSelectedComponent().getName());
   }
  }


  public void propertyChange(PropertyChangeEvent event) {
    if (event.getPropertyName().equals(Plugin.PLUGIN_ADDED)) {
      Plugin cp = (Plugin) event.getNewValue();
      this.add(cp.getName(), cp.getGUI());
System.out.println("add gui "+cp.getName()+" :: "+cp.getGUI());      
      this.m_activator.addPropertyChangeListener(cp);
      this.pluginList.put(cp.pluginLocation(), cp);
      
    }else if(event.getPropertyName().equals(Plugin.PLUGIN_REMOVED)) {
      Plugin cp = (Plugin) event.getNewValue();
      this.remove(cp.getGUI());
      this.m_activator.removePropertyChangeListener(cp);
//			this.pluginList.remove(cp.pluginLocation());
      
    }else if(event.getPropertyName().equals(Plugin.EMPTY_NODE)) {
System.out.println("******* Debug No node selected");
      this.clean();
    }else if (event.getPropertyName().equals(Plugin.NEW_NODE_SELECTED)) {
//System.out.println("Event NEW_NODE_SELECTED");
    /* Update the tabs (Plugin) in the JTabbedPane (NodePanel) */
      MBeanServerConnection mbsc = (MBeanServerConnection)event.getNewValue();
      try {
	this.clean();

        Set ons = mbsc.queryNames( null, null );
        for( Iterator i=ons.iterator(); i.hasNext(); ) {
          ObjectName name = ( ObjectName )i.next();
System.out.println("Queried name: "+name.toString());
          if ( "TabUI".equals(name.getDomain()) ) {
System.out.println("New tab: "+name.toString());
	    /* Get the plugin implementation via a bundle */
	    String tabBundle = (String) mbsc.getAttribute(name, "BundleName");
System.out.println("Bundle name for current Plugin: "+tabBundle);
	    if (tabBundle!=null){
              Plugin p = (Plugin) this.pluginList.get(tabBundle);
              if (p == null){
  	          Bundle b = m_context.installBundle(tabBundle);
		  b.start();
System.out.println("Bundle started");
//Thread.sleep(5000);
               }else{
System.out.println("register service plugin: " + p);
                 p.registerServicePlugin();
               }

//             ServiceReference[] sr = b.getRegisteredServices();
//             System.out.println(sr);
//             Plugin p;
//             for (int j=0 ; j < sr.length ; j++) {
//               p=(Plugin)m_context.getService(sr[j]);
//               this.add(p.getName(), p.getGUI());
//               this.m_activator.addPropertyChangeListener(p);
//             }

//               System.out.println("Delegation for this");
//               printcl = this.getClass().getClassLoader();
//               while (printcl != null) {
//                 System.out.println(printcl);
//                 printcl = printcl.getParent();
//               }
//               System.out.println("{bootstrap loader}");
//               System.out.println("");

					/* Get the tab object */
//             Object tabObj = mbsc.getAttribute(name, "Tab");
// 
//               System.out.println("Delegation for tabObj: " + tabObj);
//               printcl = tabObj.getClass().getClassLoader();
//               while (printcl != null) {
//                 System.out.println(printcl);
//                 printcl = printcl.getParent();
//               }
//               System.out.println("{bootstrap loader}");
//               System.out.println("");
  
//          System.out.println("tabObj.getName(): " + ((fr.inria.ares.managedelements.testprobe.tab.TestProbeTabUI) tabObj).getName());
              
          /* Cast the tab */  
//            Plugin tab = (Plugin)tabObj;
          /* register the tab on the JTabbedPane */
//            this.add(tab.getName(), tab.getGUI());
            }else{
	      System.out.println("No "+tabBundle+" property defined. I cannot install "+tabBundle+" tab");
            }
          }
        }
        m_activator.firePropertyChangedEvent(Plugin.NEW_NODE_READY, null, mbsc);
        m_activator.firePropertyChangedEvent(Plugin.PLUGIN_ACTIVATED, null, this.getComponentAt(0).getName());


      } catch (MBeanException e) {
          System.err.println(e);
//        } catch (MalformedObjectNameException e) {
//          System.err.println(e);
      } catch (AttributeNotFoundException e) {
        System.err.println(e);
      } catch (InstanceNotFoundException e) {
        System.err.println(e);
      } catch (ReflectionException e) {
        System.err.println(e);
      } catch (IOException e) {
        System.err.println(e);
      } catch (BundleException e) {
        System.err.println(e);
      }
      
//       catch (java.lang.InterruptedException e) {
// 	      System.err.println(e);
//       }
    }
  }

  private void clean(){
    this.removeAll();
    for ( Iterator i=pluginList.keySet().iterator(); i.hasNext();) {
     Object o=i.next();
     try {
      ((Plugin) pluginList.get(o)).unregisterServicePlugin();
     }catch(Exception e){
      System.out.println("INFO Something went wrong when unregistering the Plugin. Please control the beavior of the unregisterServicePlugin function of this tab"+o);
     }
    }
  }

}
