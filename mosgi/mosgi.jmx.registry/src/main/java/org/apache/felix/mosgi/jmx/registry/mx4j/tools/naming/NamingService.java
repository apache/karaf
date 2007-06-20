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
package org.apache.felix.mosgi.jmx.registry.mx4j.tools.naming;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import javax.management.ObjectName;

import org.osgi.service.log.LogService;

import org.apache.felix.framework.cache.BundleCache;

public class NamingService implements BundleActivator,NamingServiceIfc {
  private String version=null;
  private ObjectName namingServiceName=null;

  private ServiceRegistration sReg=null;
  private Registry m_registry=null;
  private BundleContext bc=null;

  public void start(BundleContext bc) throws Exception {
    this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    this.bc=bc;
    String profile=bc.getProperty(BundleCache.CACHE_PROFILE_PROP);
    if (profile==null){
      profile=System.getProperty(BundleCache.CACHE_PROFILE_PROP);
    }
    String rmiPortS=bc.getProperty("mosgi.jmxconsole.rmiport."+profile);
    int rmiPort=1099;
    if (rmiPortS!=null){
      rmiPort=Integer.parseInt(rmiPortS);
    }
    try {
      this.log(LogService.LOG_INFO, "Running rmiregistry on "+rmiPort,null);
      Thread.currentThread().setContextClassLoader(this.getClass().getClassLoader()); //!! Absolutely nececary for RMIClassLoading to work
      m_registry=LocateRegistry.createRegistry(rmiPort);
      //java.rmi.server.RemoteServer.setLog(System.out);
    } catch (Exception e) {
      this.bc=null;
      throw new BundleException("Impossible to start rmiregistry");
    }
    sReg=bc.registerService(NamingServiceIfc.class.getName(), this, null);
    this.log(LogService.LOG_INFO, "RMI Registry started "+version,null);
  }

  public void stop(BundleContext bc) throws Exception {
    this.log(LogService.LOG_INFO, "Stopping RMI Registry "+version,null);
    UnicastRemoteObject.unexportObject(m_registry, true);
    this. m_registry = null;
    sReg.unregister();    
    this.sReg=null;
    this.log(LogService.LOG_INFO, "RMI Stopped "+version,null);
    this.bc=null;
  }

  private void log(int prio, String message, Throwable t){
    if (this.bc!=null){
      ServiceReference logSR=this.bc.getServiceReference(LogService.class.getName());
      if (logSR!=null){
        ((LogService)this.bc.getService(logSR)).log(prio, message, t);
      }else{
        System.out.println("No Log Service");
      }
    }else{
      System.out.println(NamingService.class.getName()+": No bundleContext");
    }
  }
}
