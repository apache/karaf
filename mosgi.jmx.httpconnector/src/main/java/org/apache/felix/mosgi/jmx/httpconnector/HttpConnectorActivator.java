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
package org.apache.felix.mosgi.jmx.httpconnector;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.BundleException;

import org.osgi.service.log.LogService;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.http.HttpAdaptor;
import org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.http.XSLTProcessor;


public class HttpConnectorActivator implements BundleActivator{
  public static BundleContext bc;
  private HttpAdaptor http;
  private ObjectName httpName;
  private ObjectName processorName;
  private String version=null;

  public void start(BundleContext bc) throws Exception{
    this.version=(String)bc.getBundle().getHeaders().get(Constants.BUNDLE_VERSION);
    ServiceReference sr = bc.getServiceReference(javax.management.MBeanServer.class.getName());
    if (sr!=null){
      HttpConnectorActivator.bc=bc;

      HttpConnectorActivator.log(LogService.LOG_INFO, "Starting JMX Http Connector "+version,null);
      MBeanServer server= (MBeanServer)bc.getService(sr);
      
      String httpPort = bc.getProperty("org.osgi.service.http.port");
      if (httpPort==null){
        httpPort="8080";
      }

      httpName = new ObjectName("HtmlAdaptor:name=HttpAdaptor,port=" + httpPort);
      http = new HttpAdaptor();

      http.setPort((new Integer(httpPort)).intValue());
      http.setHost("0.0.0.0"); //For external access 
  
      processorName = new ObjectName("HtmlAdaptor:name=XSLTProcessor");
      XSLTProcessor processor=new XSLTProcessor();

      server.registerMBean(processor, processorName);
      server.registerMBean(http, httpName);

      http.setProcessorName(processorName);
      http.start();
      HttpConnectorActivator.log(LogService.LOG_INFO, "Started JMX Http Connector "+version,null);
    }else {
      throw new BundleException("No JMX Agent found");
    }
  }

  public void stop(BundleContext bc) throws Exception {
    HttpConnectorActivator.log(LogService.LOG_INFO, "Stopping JMX Http connector "+version,null);
    this.http.stop();
    ServiceReference sr = bc.getServiceReference(javax.management.MBeanServer.class.getName());
    if (sr!=null){
      MBeanServer server= (MBeanServer)bc.getService(sr);
      server.unregisterMBean(processorName);
      server.unregisterMBean(httpName);
    }
    HttpConnectorActivator.log(LogService.LOG_INFO, "JMX Http Connector stopped "+version,null);
    HttpConnectorActivator.bc=null;
  }

  private static void log(int prio, String message, Throwable t){
    if (HttpConnectorActivator.bc!=null){
      ServiceReference logSR=HttpConnectorActivator.bc.getServiceReference(LogService.class.getName());
      if (logSR!=null){
        ((LogService)HttpConnectorActivator.bc.getService(logSR)).log(prio, message, t);
      }else{
        System.out.println("No Log Service");
      }
    }else{
      System.out.println(org.apache.felix.mosgi.jmx.httpconnector.HttpConnectorActivator.class.getName()+": No bundleContext");
    }
  }

}
