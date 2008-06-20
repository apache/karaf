/* 
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

import org.apache.felix.mosgi.console.ifc.Plugin;
import org.apache.felix.mosgi.console.ifc.CommonPlugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URLClassLoader;
import java.rmi.server.RMIClassLoader;
import java.rmi.server.RMIClassLoaderSpi;
import java.security.SecureClassLoader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.Set;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JTabbedPane;
import java.lang.IllegalStateException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
//import javax.management.MalformedObjectNameException;

public class NodePanel extends JTabbedPane implements PropertyChangeListener, ChangeListener {
  //private String repo;
  private Activator a=null;
  private BundleContext m_context=null;
  
  private Hashtable pluginList=null;

  public NodePanel(Activator activator, BundleContext context){
    this.m_context=context;
    this.a=activator;
    a.addPropertyChangeListener(this);
    this.pluginList=new Hashtable();
    //repo = m_context.getProperty("mosgi.jmxconsole.repository");
    this.addChangeListener(this);
  }

  //////////////////////////////////////
  // ChangeListener Implementation    //
  //////////////////////////////////////
  public void stateChanged(ChangeEvent e){
    //   System.out.println("coucou => "+((JTabbedPane)e.getSource()).getSelectedIndex()+":"+((JTabbedPane)e.getSource()).getSelectedComponent().getName());
    if (this.getSelectedComponent()!=null){
      a.firePropertyChangedEvent(Plugin.PLUGIN_ACTIVATED, null, this.getSelectedComponent().getName());
    }
  }

  /////////////////////////////////////////
  //   PropertyChangeListener Impl.      //
  /////////////////////////////////////////
  public void propertyChange(PropertyChangeEvent event) {
    //System.out.println("   PCE : "+event.getPropertyName());
    if (event.getPropertyName().equals(Plugin.PLUGIN_ADDED)) {
      Plugin cp=(Plugin) event.getNewValue();
      this.add(cp.getName(), cp.getGUI());
      System.out.println("Add gui \""+cp.getName()+"\" into NodePanel"/*+" :: "+cp.getGUI()*/);      
      this.a.addPropertyChangeListener(cp);
      this.pluginList.put(cp.pluginLocation(), cp); 
    }else if(event.getPropertyName().equals(Plugin.PLUGIN_REMOVED)) {
      Plugin cp = (Plugin) event.getNewValue();
      String cpLoc=cp.pluginLocation();
      this.remove(cp.getGUI());
      //this.a.removePropertyChangeListener(cp);
      //this.pluginList.remove(cpLoc);
    }else if(event.getPropertyName().equals(Plugin.EMPTY_NODE)) {
      //System.out.println("******* Debug No node selected");
      this.clean();
    }else if (event.getPropertyName().equals(Plugin.NEW_NODE_SELECTED)) {
      /* Update the tabs (Plugin) in the JTabbedPane (NodePanel) */
      MBeanServerConnection mbsc = (MBeanServerConnection)event.getNewValue();
      String connString = (String) event.getOldValue();
      startMBeanTabBundles(connString, mbsc);
    }else if (event.getPropertyName().equals(CommonPlugin.COMMON_PLUGIN_ADDED)) {
      Iterator it = Gateway.HT_GATEWAY.values().iterator();
      while (it.hasNext()) {
        Gateway g = (Gateway) it.next();
	if ( g.isConnected() ) {
          a.firePropertyChangedEvent(Plugin.NEW_NODE_CONNECTION, g.toString(), g.getMbsc());
        }
      }
    }
  }

  private void startMBeanTabBundles(String connString, MBeanServerConnection mbsc) {
    try {
      this.clean();
      Set set_on = mbsc.queryNames( null, null ); // ioe
      Object[] ons=set_on.toArray();
      int oldUnstartedBundleNbr=0;

      do {
        Vector v_unstartedBundle=new Vector();
        oldUnstartedBundleNbr=ons.length;
	for (int i=0;i<ons.length;i++) {
	  ObjectName name= (ObjectName) ons[i];
          if ( "TabUI".equals(name.getDomain()) ) {
            /* Get the plugin implementation via a bundle */
	    try {
              String tabBundle = (String) mbsc.getAttribute(name, "BundleName"); // mbe, anfe, be, infe, re
              if (tabBundle!=null){
                Plugin p = (Plugin) this.pluginList.get(tabBundle);
                if (p == null){
                  Bundle b = m_context.installBundle(tabBundle); // be
                  try {
	            b.start(); // be2
                    System.out.println(" - Bundle started: \""+name.toString()+"\" - "+tabBundle);
	          }catch(BundleException be2) { // be2
                    System.out.println(" - Unable to start: \""+name.toString()+"\" - "+tabBundle);
		    be2.printStackTrace();
		    v_unstartedBundle.add(name);
	          }
                }else {
                  System.out.println(" - Register service plugin: "+p);
                  p.registerServicePlugin();
                }
/*              ServiceReference[] sr = b.getRegisteredServices();
 *              System.out.println(sr);
 *              Plugin p;
 *              for (int j=0 ; j < sr.length ; j++) {
 *                p=(Plugin)m_context.getService(sr[j]);
 *                this.add(p.getName(), p.getGUI());
 *                this.a.addPropertyChangeListener(p);
 *              }
 *
 *              System.out.println("Delegation for this");
 *              printcl = this.getClass().getClassLoader();
 *              while (printcl != null) {
 *                System.out.println(printcl);
 *                printcl = printcl.getParent();
 *              }
 *              System.out.println("{bootstrap loader}");
 *              System.out.println("");
 *
 *              // Get the tab object
 *              Object tabObj = mbsc.getAttribute(name, "Tab");
 * 
 *              System.out.println("Delegation for tabObj: " + tabObj);
 *              printcl = tabObj.getClass().getClassLoader();
 *              while (printcl != null) {
 *                System.out.println(printcl);
 *                printcl = printcl.getParent();
 *              }
 *              System.out.println("{bootstrap loader}");
 *              System.out.println("");
 *  
 *              System.out.println("tabObj.getName(): " + ((fr.inria.ares.managedelements.testprobe.tab.TestProbeTabUI) tabObj).getName());
 *              
 *              // Cast the tab   
 *              Plugin tab = (Plugin)tabObj;
 *              // register the tab on the JTabbedPane 
 *              this.add(tab.getName(), tab.getGUI());
 */
              } else{
	        System.out.println(" - No bundleName attribute defined for \""+name.toString()+"\". I cannot install tab");
              }
            }catch (MBeanException mbe) { mbe.printStackTrace(); // mbe
	    }catch (AttributeNotFoundException anfe) { anfe.printStackTrace(); // anfe
	    }catch (BundleException be) { be.printStackTrace(); // be
	    }catch (InstanceNotFoundException infe) { infe.printStackTrace(); // infe
	    }catch (ReflectionException re) { re.printStackTrace(); // re
	    }
	  }
        }
	ons=v_unstartedBundle.toArray();
      }while ( oldUnstartedBundleNbr != ons.length );

    }catch (IOException ioe) { ioe.printStackTrace(); // ioe
    }
    a.firePropertyChangedEvent(Plugin.NEW_NODE_READY, connString, mbsc);
    a.firePropertyChangedEvent(Plugin.PLUGIN_ACTIVATED, null, this.getComponentAt(0).getName());
  }

  private void clean(){
    this.removeAll();
    Vector pluginList_tmp=new Vector();
    for ( Iterator i=pluginList.keySet().iterator(); i.hasNext();) {
      Object o=i.next();
      pluginList_tmp.add(o);
    }
    for ( int i = 0 ; i < pluginList_tmp.size() ; i++) {
      Plugin p=(Plugin) pluginList.get(pluginList_tmp.elementAt(i));
      try {
        p.unregisterServicePlugin();
      } catch (Exception ex) {
	//System.out.println("\""+p.getName()+"\" : "+ex);
      }
    } 
  }

}
