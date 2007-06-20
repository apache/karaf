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
package org.apache.felix.mosgi.jmx.agent.mx4j.server.interceptor;


import javax.management.MBeanException;
import javax.management.ObjectName;

/**
 * Management interface for the MBeanServerInterceptorConfigurator MBean.
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface MBeanServerInterceptorConfiguratorMBean
{
   /**
    * Appends the given interceptor, provided by the client, to the existing interceptor chain.
    * @see #registerInterceptor
    */
   public void addInterceptor(MBeanServerInterceptor interceptor);

   /**
    * Appends the given interceptor, provided by the client, to the existing interceptor chain and registers it as MBean.
    * @see #addInterceptor
    */
   public void registerInterceptor(MBeanServerInterceptor interceptor, ObjectName name) throws MBeanException;

   /**
    * Removes all the interceptors added via {@link #addInterceptor(MBeanServerInterceptor interceptor)}.
    * @see #addInterceptor
    */
   public void clearInterceptors();

   /**
    * Starts this configurator, so that the MBeanServer is now able to accept incoming calls.
    * @see #stop
    * @see #isRunning
    */
   public void start();

   /**
    * Stops this configurator, so that the MBeanServer is not able to accept incoming calls.
    * @see #start
    */
   public void stop();

   /**
    * Returns whether this configurator is running and thus if the MBeanServer can accept incoming calls
    * @see #start
    */
   public boolean isRunning();
}
