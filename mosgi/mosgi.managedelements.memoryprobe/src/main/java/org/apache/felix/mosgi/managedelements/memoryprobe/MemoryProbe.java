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
package org.apache.felix.mosgi.managedelements.memoryprobe;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import javax.management.ObjectName;
import javax.management.MBeanServer;

public class MemoryProbe implements BundleActivator, MemoryProbeMBean {
  private static final String tabNameString = "TabUI:name=MemoryProbe";
  private ObjectName tabName=null;
  private ServiceRegistration sr=null;
  private BundleContext bc=null;

  ////////////////////////////////////////////////////////
  //     TabIfc (from OsgiProbesMBean)                  //
  ////////////////////////////////////////////////////////
  public String getBundleName() {
    return this.bc.getProperty("mosgi.jmxconsole.tab.url.memoryprobetab");
  }

  ////////////////////////////////////////////////////////
  //       BundleActivator                              //
  ////////////////////////////////////////////////////////
  public void start(BundleContext context) throws Exception  {
    this.bc=context;
    this.tabName=new ObjectName(tabNameString);
/*
    this.server=(MBeanServer)this.bc.getService(sr);
    this.server.registerMBean(this, tabName);
*/
    java.util.Properties p=new java.util.Properties();
    p.put(org.apache.felix.mosgi.jmx.agent.Constants.OBJECTNAME, this.tabNameString);
    sr=this.bc.registerService(MemoryProbeMBean.class.getName(), this, p);
  }

  public void stop(BundleContext context) throws Exception  {
    this.tabName=null;
/*
    this.server=(MBeanServer)this.bc.getService(sr);
    this.server.registerMBean(this, tabName);
*/
    this.sr.unregister();
    this.sr=null;
    this.bc=null;
  }
}
