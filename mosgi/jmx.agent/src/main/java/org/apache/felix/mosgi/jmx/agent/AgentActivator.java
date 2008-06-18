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
package org.apache.felix.mosgi.jmx.agent;

import java.util.StringTokenizer;

//import java.lang.management.ManagementFactory;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.InvalidSyntaxException;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import javax.management.MBeanServerFactory;
import javax.management.DynamicMBean;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanRegistrationException;
import javax.management.NotCompliantMBeanException;

import org.osgi.service.log.LogService;
import org.apache.felix.framework.cache.BundleCache;

public class AgentActivator implements BundleActivator, ServiceListener {
  private MBeanServer server;
  private ServiceRegistration serverRegistration;
  static private BundleContext bc;
  private String version = null;

  //BundleActivator interface

  public void start(BundleContext context) throws Exception  {
    AgentActivator.bc=context;
    this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    AgentActivator.log(LogService.LOG_INFO, "Starting JMX Agent "+version,null);
    StringTokenizer st=new StringTokenizer(System.getProperty("java.version"), ".");
    st.nextToken();
    int minorVersion = Integer.parseInt(st.nextToken());
    this.startAgent(minorVersion);
    this.registerExistingMBeans();
    bc.addServiceListener(this);
  }

  public void stop(BundleContext context) throws Exception {
    AgentActivator.log(LogService.LOG_INFO, "JMX Agent stopping "+version,null);
//    MBeanServerFactory.releaseMBeanServer(this.server);
    this.unregisterExistingMBeans();
    this.serverRegistration.unregister();
    AgentActivator.log(LogService.LOG_INFO, "JMX Agent stopped "+version,null);
    AgentActivator.bc=null;
    this.serverRegistration=null;
    this.server=null;
  }

  // Service Listener Interface
  public void serviceChanged(ServiceEvent serviceEvent) {
    ServiceReference serviceReference = serviceEvent.getServiceReference();
    if (isMBean(serviceReference)) {
      if (serviceEvent.getType() == ServiceEvent.REGISTERED) {
         this.register(serviceReference);
      } else if (serviceEvent.getType() == ServiceEvent.UNREGISTERING) {
         this.unregister(serviceReference);
      } else if (serviceEvent.getType() == ServiceEvent.MODIFIED) {
         this.unregister(serviceReference);
         this.register(serviceReference);
      }
    }
  }
  
  private static void log(int prio, String message, Throwable t){
    if (AgentActivator.bc!=null){
      ServiceReference logSR=AgentActivator.bc.getServiceReference(LogService.class.getName());
      if (logSR!=null){
        ((LogService)AgentActivator.bc.getService(logSR)).log(prio, message, t);
      }else{
        System.out.println("No Log Service");
      }
    }else{
      System.out.println(AgentActivator.class.getName()+": No bundleContext");
    }
  }

  private void startAgent(int minor){
    if ( minor >= 5 ){
      this.server = java.lang.management.ManagementFactory.getPlatformMBeanServer();
      AgentActivator.log(LogService.LOG_DEBUG, "A jdk1.5 agent started "+this.server,null);
    }else {
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
      this.server = MBeanServerFactory.createMBeanServer();
      AgentActivator.log(LogService.LOG_DEBUG, "A lightweight agent started "+this.server,null);
    }
    this.serverRegistration=bc.registerService(MBeanServer.class.getName(), this.server, null);
    AgentActivator.log(LogService.LOG_INFO, "JMX Agent started "+version,null);
  }

  private boolean isMBean(ServiceReference serviceReference) {
    String[] objectClasses = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
    if (objectClasses == null){
      return false;
    }
    int i = 0;
    for (; i < objectClasses.length; i++) {
      if (objectClasses[i].endsWith("MBean")){
        break;
      }
        // "Static MBean interfaces" ends by "MBean"
      if (objectClasses[i].equals(DynamicMBean.class.getName())){
        break;
      }
    }
    if (i == objectClasses.length){
      return false;
    }
    return true;
  }

  private void registerExistingMBeans() {
    ServiceReference[] serviceReferences = null;
    try {
      serviceReferences = bc.getServiceReferences(null, null);
    } catch (InvalidSyntaxException e) {
      // Never Thrown
    }
    if (serviceReferences == null){
      return;
    }
    for (int i = 0; i < serviceReferences.length; i++) {
      if (isMBean(serviceReferences[i])){
        this.register(serviceReferences[i]);
      }
    }
  }

  private void unregisterExistingMBeans() {
    ServiceReference[] serviceReferences = null;
    try {
      serviceReferences = bc.getServiceReferences(null, null);
    } catch (InvalidSyntaxException e) {
      // never thrown
    }
    if (serviceReferences == null){
      return;
    }
    for (int i = 0; i < serviceReferences.length; i++) {
      if (isMBean(serviceReferences[i])){
        unregister(serviceReferences[i]);
      }
    }
  } 

  private void register(ServiceReference serviceReference) {
    String name = this.getObjectNameString(serviceReference);

    Object mbean = bc.getService(serviceReference);
    ObjectName objectName = null;
    try {
      // Unique identification of MBeans
      objectName = new ObjectName(name);
    } catch (MalformedObjectNameException e) {
      e.printStackTrace();
    } catch (NullPointerException e) {
      e.printStackTrace();
    }
    try {
      // Uniquely identify the MBean and register it with the MBeanServer
      server.registerMBean(mbean, objectName);
    } catch (InstanceAlreadyExistsException e1) {
      e1.printStackTrace();
    } catch (MBeanRegistrationException e1) {
      e1.printStackTrace();
    } catch (NotCompliantMBeanException e1) {
      e1.printStackTrace();
    }
  }

  private void unregister(ServiceReference serviceReference) {
    String name = getObjectNameString(serviceReference);
    try {
      // TODO check if unregisterMBean occurs really
      server.unregisterMBean(new ObjectName(name));
    } catch (InstanceNotFoundException e) {
      // do nothing;
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private  String getObjectNameString(ServiceReference serviceReference) {
    String objectName = (String) serviceReference.getProperty(org.apache.felix.mosgi.jmx.agent.Constants.OBJECTNAME);
    if (objectName != null){
      // If objectName starts with the colon character (:), the domain part of the object name is the domain of the agent.
      if(objectName.startsWith(":")){
        // invokes the getDefaultDomain() method of the Framework class to obtain this information. 
        objectName=server.getDefaultDomain()+objectName;
      }
      return objectName;
    }

System.out.println("No "+org.apache.felix.mosgi.jmx.agent.Constants.OBJECTNAME+" constant for "+serviceReference+" MBean. Trying to build it");

    String[] objectClasses = (String[]) serviceReference.getProperty(Constants.OBJECTCLASS);
    if (objectClasses == null){
      return null;
    }
    int i = 0;
    for (; i < objectClasses.length; i++) {
      if (objectClasses[i].endsWith("MBean")){
        break;
      }
    }
    // TODO do nothing if there is several MBean inplemented interfaces
    if (i == objectClasses.length){
      return null;
    }
    
    StringBuffer sb=new StringBuffer(server.getDefaultDomain());
    sb.append(":");
    sb.append("BundleId=");
    sb.append(serviceReference.getBundle().getBundleId());
    sb.append(", ServiceId=");
    sb.append(serviceReference.getProperty(Constants.SERVICE_ID));
    sb.append(", ObjectClass=");
    sb.append(objectClasses[i]);
    Object servicePID=serviceReference.getProperty(Constants.SERVICE_PID);
    if (servicePID==null){
      sb.append(", servicePID=NA");
    }else{
      sb.append(", servicePID=");
      sb.append(servicePID);
    }
System.out.println("==>"+sb.toString());
    return sb.toString();
    
  }
}
