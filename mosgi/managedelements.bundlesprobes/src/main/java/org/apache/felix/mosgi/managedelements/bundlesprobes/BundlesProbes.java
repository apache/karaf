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
package org.apache.felix.mosgi.managedelements.bundlesprobes;

/**
 * TODO : Should listen to Agent Service lifecycle
 *        Need to change ObjectName
 *        Should listen to serviceLifecycle
**/

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.NotificationBroadcasterSupport;
import javax.management.AttributeChangeNotification;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import org.osgi.service.log.LogService;

public class BundlesProbes extends NotificationBroadcasterSupport implements BundleActivator, BundlesProbesMBean, ServiceListener, BundleListener {

  private String version = null;
  private static String TAB_NAME_STRING="TabUI:name=BundlesProbes";

  private ObjectName tabName = null;
  private MBeanServer server = null;
  private BundleContext bc = null;
  private ServiceRegistration sr = null;


  ////////////////////////////////////////////////////////
  //     TabIfc (from BundlesProbesMBean)                  //
  ////////////////////////////////////////////////////////
  public String getBundleName() {
    return this.bc.getProperty("mosgi.jmxconsole.tab.url.bundlesprobestab");
  }


  ////////////////////////////////////////////////////////
  //       BundleActivator                              //
  ////////////////////////////////////////////////////////
  public void start(BundleContext context) throws Exception  {
    this.bc=context;
    this.log(LogService.LOG_INFO, "Starting BundlesProbe MBean " + this.version,null);
    java.util.Properties p = new java.util.Properties();
    p.put(org.apache.felix.mosgi.jmx.agent.Constants.OBJECTNAME, TAB_NAME_STRING);
    this.sr = this.bc.registerService(BundlesProbesMBean.class.getName(), this, p);
    this.log(LogService.LOG_INFO, "BundlesProbes MBean "+this.version+" started", null);
  }

  public void stop(BundleContext context) {
    this.log(LogService.LOG_INFO, "Stopping BundlesProbes MBean "+this.version, null);
    this.sr.unregister();
    this.sr=null;
    this.log(LogService.LOG_INFO, "BundlesProbes MBean "+this.version+" stopped", null);
    this.bc=null;
  }

  ////////////////////////////////////////////////////////
  //       BundlesProbesMBean                              //
  ////////////////////////////////////////////////////////
  public Vector bundleList() {
    Bundle[] bl = this.bc.getBundles();
    Vector bundleList = new Vector();
    Vector bundle = null;
    for (int i = 0; i < bl.length; i++) {
      bundle = new Vector();
      String enum1 = (String) bl[i].getHeaders().get(Constants.BUNDLE_NAME);
      long id = bl[i].getBundleId();
      bundle.add(new Long(id));
      int state = bl[i].getState();
      switch (state) {
      case Bundle.ACTIVE:
        bundle.add(new String("ACTIVE"));
        break;
      case Bundle.INSTALLED:
        bundle.add(new String("INSTALLED"));
        break;
      case Bundle.RESOLVED:
        bundle.add(new String("RESOLVED"));
        break;
      case Bundle.UNINSTALLED:
        bundle.add(new String("UNINSTALLED"));
        break;
      default:
        bundle.add(new String("RESOLVED"));
      }
      bundle.add(enum1);
      bundleList.add(bundle);
    }
    return bundleList;
  }
  
  public void startService(Long [] id) {
     try {
       bc.getBundle(id[0].longValue()).start();
     } catch (BundleException e) {
       e.printStackTrace();
     }
   }
 
   public void stopService(Long [] id) {
     try {
       bc.getBundle(id[0].longValue()).stop();
     } catch (BundleException e) {
       e.printStackTrace();
     }
   }
 
   public void install(String location) {
     try {
       bc.installBundle(location);
     } catch (BundleException e) {
       e.printStackTrace();
     }
   }
 
   public void uninstall(Long [] id) {
     try {
       bc.getBundle(id[0].longValue()).uninstall();
     } catch (BundleException e) {
       e.printStackTrace();
     }
   }
  
   public void update(Long [] id) {
     try {
       bc.getBundle(id[0].longValue()).update();
     } catch (BundleException e) {
       e.printStackTrace();
     }
   }

  ////////////////////////////////////////////////////////
  //       ServiceListener                              //
  ////////////////////////////////////////////////////////
  public void serviceChanged(ServiceEvent event) {
    ServiceReference sref=event.getServiceReference();
    Object service=bc.getService(sref);
    if (this.server==null && event.getType()==ServiceEvent.REGISTERED && service instanceof MBeanServer){
      //this.connectToAgent(sref);
    }
    if (this.server!=null){
      if(event.getType()==ServiceEvent.UNREGISTERING && service instanceof MBeanServer){
        //this.disconnectFromAgent();
      }else{
        this.sendRemoteNotification(ServiceEvent.class.getName(),sref.getBundle().getBundleId(), event.getType(), (String)sref.getBundle().getHeaders().get(Constants.BUNDLE_NAME));
      }
    }
  }

  ////////////////////////////////////////////////////////
  //       BundleListener                               //
  ////////////////////////////////////////////////////////
  public void bundleChanged(BundleEvent event) {
    if (this.server!=null){
      Bundle b=event.getBundle();
      //SFR this.sendRemoteNotification(BundleEvent.class.getName(), b.getBundleId(), b.getState(), (String)b.getHeaders().get(Constants.BUNDLE_NAME));
System.out.println("Evenement bundle "+b.getBundleId()+" : "+event.getType());
      this.sendRemoteNotification(BundleEvent.class.getName(), b.getBundleId(), event.getType(), (String)b.getHeaders().get(Constants.BUNDLE_NAME));
		}
  }

  private void sendRemoteNotification(String className, long id, int type, String name){
    StringBuffer str = new StringBuffer(className);
    str.append(":");
    str.append(id);
    str.append(":");
    str.append(type);
    str.append(":");
    str.append(name);
    super.sendNotification(new AttributeChangeNotification(this.tabName, 0, 0,str.toString(),null, "Bundle", null, null));
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
      System.out.println(this.getClass().getName()+".log: No bundleContext");
    }
  }

}
