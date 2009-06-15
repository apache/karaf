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
package org.apache.felix.mosgi.jmx.rmiconnector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceEvent;

import org.osgi.service.log.LogService;

import org.apache.felix.framework.cache.BundleCache;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.felix.mosgi.jmx.registry.mx4j.tools.naming.NamingServiceIfc;  

import java.io.IOException;
import java.net.InetAddress;

public class RmiConnectorActivator implements BundleActivator, ServiceListener{
  public static BundleContext bc;

  private ObjectName connectorServerName=new ObjectName("RmiConnector:name=RMIConnector");

  private JMXConnectorServer connectorServer;
  private MBeanServer mbs;
  private NamingServiceIfc nsi;
  private ServiceReference mBeanServerSR, namingServiceIfcSR;

  private String version=null;

  public RmiConnectorActivator() throws javax.management.MalformedObjectNameException {}
  
  ////////////////////////////////////////////////////
  //          BundleActivator                       //
  ////////////////////////////////////////////////////
  public void start(BundleContext bc) throws Exception{
		this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    RmiConnectorActivator.bc=bc;
    this.mBeanServerSR=bc.getServiceReference(MBeanServer.class.getName());
    this.namingServiceIfcSR=bc.getServiceReference(NamingServiceIfc.class.getName());
    RmiConnectorActivator. bc.addServiceListener(this, "(|(objectClass="+NamingServiceIfc.class.getName()+")"+"(objectClass="+MBeanServer.class.getName()+"))");
    if (this.mBeanServerSR!=null && this.namingServiceIfcSR!=null){
      this.startRmiConnector();
    } else {
      if (this.mBeanServerSR == null){
        RmiConnectorActivator.log(LogService.LOG_WARNING,"No JMX Agent found",null);
      }else if (this.namingServiceIfcSR==null){
        RmiConnectorActivator.log(LogService.LOG_WARNING,"No RMI Registry found", null);
      }
    }
  }

  public void stop(BundleContext bc) throws Exception {
    this.stopRmiConnector();
    RmiConnectorActivator.bc=null;
  }

  //////////////////////////////////////////////////////
  //          ServiceListener                        //
  //////////////////////////////////////////////////////
  public void serviceChanged(ServiceEvent serviceevent) {
    ServiceReference servicereference= serviceevent.getServiceReference();
    String [] ast=(String[])(servicereference.getProperty("objectClass"));
    String as=ast[0];
    switch (serviceevent.getType()) {
      case ServiceEvent.REGISTERED :
        if (as.equals(NamingServiceIfc.class.getName())){
          this.namingServiceIfcSR=servicereference;
        }else if (as.equals(MBeanServer.class.getName())){
          this.mBeanServerSR=servicereference;
        }
        if (this.namingServiceIfcSR!=null && this.mBeanServerSR!=null){
          try{
            this.startRmiConnector();
          }catch (Exception e){
            this.log(LogService.LOG_ERROR, "cannot start rmi connector", e);
          }
        }
        break;
      case ServiceEvent.UNREGISTERING :
        try{
          this.stopRmiConnector();
        }catch (Exception e){
          this.log(LogService.LOG_ERROR, "cannot stop rmi connector", e);
        }
        break;
    }
  }

  public static void log(int prio, String message, Throwable t){
    if (RmiConnectorActivator.bc!=null){
      ServiceReference logSR=RmiConnectorActivator.bc.getServiceReference(LogService.class.getName());
      if (logSR!=null){
        ((LogService)RmiConnectorActivator.bc.getService(logSR)).log(prio, message, t);
      }else{
        System.out.println("No Log Service");
      }
    }else{
      System.out.println(RmiConnectorActivator.class.getName()+": No bundleContext");
    }
  }

  private void startRmiConnector() throws Exception{
    String profile=bc.getProperty(BundleCache.CACHE_PROFILE_PROP);
    if (profile==null){
      profile=System.getProperty(BundleCache.CACHE_PROFILE_PROP);
		}
    String rmiPort=bc.getProperty("mosgi.jmxconsole.rmiport."+profile);
    if (rmiPort==null){
      rmiPort="1099";
    }
    String url="service:jmx:rmi:///jndi/rmi://"+ InetAddress.getLocalHost().getHostAddress()+":"+rmiPort+"/"+profile;
    RmiConnectorActivator.log(LogService.LOG_INFO, "jmx connexion string ==> "+url, null);
    
    RmiConnectorActivator.log(LogService.LOG_INFO, "Starting JMX Rmi Connector "+version,null);
    this.mbs=(MBeanServer)bc.getService(this.mBeanServerSR);
    this.nsi=(NamingServiceIfc)bc.getService(this.namingServiceIfcSR);
    JMXServiceURL address=new JMXServiceURL(url);
/*
    Map environment = new HashMap();
    environment.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.rmi.registry.RegistryContextFactory");
    environment.put(Context.PROVIDER_URL, "rmi://localhost:"+rmiPort);
    environment.put(JMXConnectorServerFactory.PROTOCOL_PROVIDER_CLASS_LOADER,this.getClass().getClassLoader());
  */

/* Loggin 
Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader());
java.util.logging.ConsoleHandler ch=new java.util.logging.ConsoleHandler();
ch.setLevel(java.util.logging.Level.FINEST);
java.util.logging.Logger.getLogger("javax.management.remote.misc").setLevel(java.util.logging.Level.FINEST);
java.util.logging.Logger.getLogger("javax.management.remote.rmi").setLevel(java.util.logging.Level.FINEST);
java.util.logging.Logger.getLogger("javax.management.remote.rmi").addHandler(ch);
java.util.logging.Logger.getLogger("javax.management.remote.misc").addHandler(ch);
*/

		/*
		java.util.Map env = new java.util.HashMap();
		env.put(JMXConnectorServerFactory.PROTOCOL_PROVIDER_CLASS_LOADER, this.getClass().getClassLoader());
		env.put("jmx.remote.protocol.provider.pkgs", "mx4j.remote.provider");
		*/

    this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(address, null, this.mbs);

    RmiConnectorActivator.log(LogService.LOG_DEBUG, "===> "+this.connectorServer, null);
    RmiConnectorActivator.log(LogService.LOG_DEBUG, "======> "+this.connectorServer.getMBeanServer(), null);

//    this.mbs.registerMBean(this.connectorServer, this.connectorServerName);
//    this.connectorServer = JMXConnectorServerFactory.newJMXConnectorServer(address, null, java.lang.management.ManagementFactory.getPlatformMBeanServer());
    this.connectorServer.start();

    RmiConnectorActivator.log(LogService.LOG_INFO, "JMX Rmi Connector started "+version,null);
  }

  private void stopRmiConnector() throws Exception {
    RmiConnectorActivator.log(LogService.LOG_INFO, "Stopping JMX Rmi connector "+version,null);
    if (this.connectorServer!=null){
      try {
        // The first call to stop() will close any open connections, but will
        // throw an exception if there were open connections.
        this.connectorServer.stop();
      }catch(IOException e){
        // Exception probably thrown because there were open connections. When
        // this exception is thrown, the server has already attempted to close
        // all client connections, try stopping again.
        this.connectorServer.stop();
      }
      this.connectorServer=null;
    }

    if (this.mbs!=null){
//SFR   this.mbs.unregisterMBean(this.connectorServerName);
      this.mbs=null;
    }

    this.nsi=null;

    if (this.mBeanServerSR!=null){
      RmiConnectorActivator.bc.ungetService(this.mBeanServerSR);
      this.mBeanServerSR=null;
    }
    if (this.namingServiceIfcSR!=null){
      RmiConnectorActivator.bc.ungetService(this.namingServiceIfcSR);
      this.namingServiceIfcSR=null;
    }
    this.connectorServerName=null;
    RmiConnectorActivator.log(LogService.LOG_INFO, "Rmi Connector stopped "+version,null);
  }
}
