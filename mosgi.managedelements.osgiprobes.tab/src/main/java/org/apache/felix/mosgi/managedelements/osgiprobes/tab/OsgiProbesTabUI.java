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
package org.apache.felix.mosgi.managedelements.osgiprobes.tab;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Vector;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.felix.mosgi.console.ifc.Plugin;

public class OsgiProbesTabUI extends JPanel implements Plugin, BundleActivator {

  private String [] headers = {"Name", "Value"};
  private JTable innerTable;

  private BundleContext m_context = null;
  private ServiceRegistration sreg = null;

  public OsgiProbesTabUI(){
    this.innerTable=new JTable();
    this.innerTable.setPreferredScrollableViewportSize(new java.awt.Dimension(500,150));
    JScrollPane table = new JScrollPane(this.innerTable);
    this.add(table);
  }


  ///////////////////////////////////////////
  //           BundleActivator             //
  ///////////////////////////////////////////
  public void start(BundleContext context) {
    m_context = context;
    this.registerServicePlugin();
  }

  public void stop(BundleContext context) {
  }

  

  ///////////////////////////////////////////
  //           Plugin                      //
  //////////////////////////////////////////
  public void registerServicePlugin(){
    sreg = m_context.registerService(Plugin.class.getName(), this, null);
    
  }
  
  public void unregisterServicePlugin(){
    if (sreg!=null){
      sreg.unregister();   
      sreg=null;
    }
  }
  
  public String pluginLocation(){
    return m_context.getBundle().getLocation();
  }
  
  public String getName(){return "OSGi Platform";}

  public Component getGUI(){return this;}

  public void propertyChange(PropertyChangeEvent e){
    /*
     * This a static tab, each new visit will provide the same
     * information. A dynamic tab update is provided in LinuxTab
     *
     */
    if (e.getPropertyName().equals(Plugin.NEW_NODE_READY)){
      this.getProperties((MBeanServerConnection)e.getNewValue());
    }else if(e.getPropertyName().equals(Plugin.EMPTY_NODE)){
      this.innerTable.setModel(new DefaultTableModel());
      this.invalidate();
      this.validate();
    }
  }

  private void getProperties(MBeanServerConnection mbsc){
    try{
      ObjectName on=new ObjectName("TabUI:name=OsgiProbes");
      MBeanAttributeInfo attrInfo[] = mbsc.getMBeanInfo(on).getAttributes();
      Object content [][]=new String[attrInfo.length][2];
      for (int k=0;k<attrInfo.length;k++) {
        content [k][0]=attrInfo[k].getName();
        content [k][1]=mbsc.getAttribute(on, attrInfo[k].getName());
      }
      DefaultTableModel dtm=new DefaultTableModel(content, this.headers);
      this.innerTable.setModel(dtm);
      this.invalidate();
      this.validate();
    }catch (Exception e){
      e.printStackTrace();
    }
  }
}
