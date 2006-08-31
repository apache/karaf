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
package org.apache.felix.mosgi.managedelements.memoryprobe.tab;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

import org.osgi.framework.BundleContext;

import java.beans.PropertyChangeEvent;
import javax.management.MBeanServerConnection;

import org.apache.felix.mosgi.console.ifc.Plugin;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.BundleActivator;


public class MemoryProbeTabUI extends JPanel implements Plugin, BundleActivator {
  MemoryUsageLineChartComponent mulcc;
  JPanel innerP;

  static BundleContext bc;
  static MBeanServerConnection mbs;
  ServiceRegistration sreg;

  ///////////////////////////////////////////
  //           BundleActivator             //
  ///////////////////////////////////////////
  public void start(BundleContext context) {
    this.bc = context;
    this.registerServicePlugin();
  }

  public void stop(BundleContext context) { }

  ///////////////////////////////////////////
  //           Plugin                      //
  //////////////////////////////////////////
  public void registerServicePlugin(){
    sreg = bc.registerService(Plugin.class.getName(), this, null);
  }

  public void unregisterServicePlugin(){
    sreg.unregister();
  }

  public String pluginLocation(){
    return bc.getBundle().getLocation();
  }

  public String getName(){return "JVM Memory";}

  public Component getGUI(){return this;}

  public void propertyChange(PropertyChangeEvent e){
    /*
     * This a static tab, each new visit will provide the same
     * information. A dynamic tab update is provided in LinuxTab
     *
     */
    if (e.getPropertyName().equals(Plugin.NEW_NODE_READY)){
      this.mbs=(MBeanServerConnection)e.getNewValue();
      innerP=new JPanel(new BorderLayout());
      innerP.add(this.mulcc=new MemoryUsageLineChartComponent(),BorderLayout.CENTER);
      this.add(innerP, BorderLayout.CENTER);
      this.updateUI();
    }else if(e.getPropertyName().equals(Plugin.EMPTY_NODE)){
      if (mulcc!=null){
        this.mulcc.pauseSchedule();
        this.innerP.remove(this.mulcc);
        this.mulcc=null;
        this.remove(innerP);
      }
      this.invalidate();
      this.validate();
      this.updateUI();
    }
  }

}
