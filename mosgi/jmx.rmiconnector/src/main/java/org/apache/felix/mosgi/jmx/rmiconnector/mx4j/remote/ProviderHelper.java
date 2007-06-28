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
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote;

import java.security.AccessController;
import java.security.PrivilegedAction;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;

/**
 *
 * @version $Revision: 1.2 $
 */
public abstract class ProviderHelper
{
   protected static String normalizeProtocol(String protocol)
   {
      // Replace special chars as required by the spec
      String normalized = protocol.replace('+', '.');
      normalized = normalized.replace('-', '_');
      RmiConnectorActivator.log(LogService.LOG_INFO, "Normalizing protocol: " + protocol + " --> " + normalized, null);
      return normalized;
   }

   protected static String findSystemPackageList(final String key)
   {
      String providerPackages = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty(key);
         }
      });
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Packages in the system property '" + key + "': " + providerPackages,null);
      return providerPackages;
   }

   protected static Class loadClass(String className, ClassLoader loader) throws ClassNotFoundException
   {
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Loading class: " + className +" From "+loader,null);
      if (loader == null){
         loader= ProviderHelper.class.getClassLoader();
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"a new loader "+loader,null);
         
         	//Thread.currentThread().getContextClassLoader();
      }
      return loader.loadClass(className);
   }

   protected static String constructClassName(String packageName, String protocol, String className)
   {
      return new StringBuffer(packageName).append(".").append(protocol).append(".").append(className).toString();
   }

}
