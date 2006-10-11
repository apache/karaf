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
package org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.http;

import java.util.Date;
import java.io.IOException;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.felix.mosgi.jmx.httpconnector.mx4j.tools.adaptor.AdaptorServerSocketFactory;

/**
 * Management interface for the HttpAdaptor MBean.
 * @author <a href="mailto:tibu@users.sourceforge.net">Carlos Quiroz</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface HttpAdaptorMBean
{
   /**
    * Sets the value of the server's port
    * @param port the new port's value
    */
   public void setPort(int port);

   /**
    * Returns the port where the server is running on. Default is 8080
    * @return HTTPServer's port
    */
   public int getPort();

   /**
    * Sets the host name where the server will be listening
    * @param host Server's host
    */
   public void setHost(java.lang.String host);

   /**
    * Return the host name the server will be listening to. If null the server listen at the localhost
    * @return the current hostname
    */
   java.lang.String getHost();

   /**
    * Sets the Authentication Method.
    * @param method none/basic/digest
    */
   public void setAuthenticationMethod(String method);

   /**
    * Authentication Method
    * @return authentication method
    */
   public String getAuthenticationMethod();

   /**
    * Sets the object which will post process the XML results.
    * The last value set between the setPostProcessor and setPostProcessorName will be the valid one
    * @param processor a Post processor object
    */
   public void setProcessor(ProcessorMBean processor);

   /**
    * Returns the Processor set by {@link #setProcessor}
    */
   public ProcessorMBean getProcessor();

   /**
    * Sets the classname of the object which will post process the XML results.
    * The adaptor will try to build the object and use the processor name ObjectName to register it.
    * The class name has to implements mx4j.tools.adaptor.http.ProcessorMBean and be MBean compliant
    * @param processorClass a Post processor object
    */
   public void setProcessorClass(String processorClass);

   /**
    * Sets the object name of the PostProcessor MBean. If ProcessorClass is set the processor will be created
    * @param processorName a Post processor object
    */
   public void setProcessorNameString(String processorName) throws MalformedObjectNameException;

   /**
    * Sets the object name which will post process the XML result. The last value set between the setPostProcessor and setPostProcessorName will be the valid one. The MBean will be verified to be of instance HttpPostProcessor
    * @param processorName The new processorName value
    */
   public void setProcessorName(ObjectName processorName);

   /**
    * Returns the ObjectName of the processor set by {@link #setProcessorName}
    */
   public ObjectName getProcessorName();

   /**
    * Sets the object which create the server sockets
    * @param factory the socket factory
    */
   public void setSocketFactory(AdaptorServerSocketFactory factory);

   /**
    * Sets the factory's object name which will create the server sockets
    * @param factoryName the socket factory
    */
   public void setSocketFactoryName(ObjectName factoryName);

   /**
    * Sets the factory's object name which will create the server sockets
    * @param factoryName the socket factory
    */
   public void setSocketFactoryNameString(String factoryName) throws MalformedObjectNameException;

   /**
    * Indicates whether the server's running
    * @return The active value
    */
   public boolean isActive();

   /**
    * Starting date
    * @return The date when the server was started
    */
   public Date getStartDate();

   /**
    * Requests count
    * @return The total of requests served so far
    */
   public long getRequestsCount();

   /**
    * Gets the HttpAdaptor version
    * @return HttpAdaptor's version
    */
   public String getVersion();

   /**
    * Adds a command processor object
    */
   public void addCommandProcessor(String path, HttpCommandProcessor processor);

   /**
    * Adds a command processor object by class
    */
   public void addCommandProcessor(String path, String processorClass);

   /**
    * Removes a command processor object by class
    */
   public void removeCommandProcessor(String path);

   /**
    * Starts the server
    */
   public void start() throws IOException;

   /**
    * Stops the HTTP daemon
    */
   public void stop();

   /**
    * Adds an authorization pair as username/password
    */
   public void addAuthorization(String username, String password);
}
