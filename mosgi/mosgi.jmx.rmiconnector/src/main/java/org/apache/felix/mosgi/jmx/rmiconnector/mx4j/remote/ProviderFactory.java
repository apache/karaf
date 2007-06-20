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

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXConnectorServerProvider;
import javax.management.remote.JMXProviderException;
import javax.management.remote.JMXServiceURL;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.MX4JRemoteConstants;
import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.ProviderHelper;

/**
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.2 $
 */
public class ProviderFactory extends ProviderHelper
{
   public static JMXConnectorProvider newJMXConnectorProvider(JMXServiceURL url, Map env) throws IOException
   {
      // Yes, throw NPE if url is null (spec compliant)
      String protocol = normalizeProtocol(url.getProtocol());
      String providerPackages = findProviderPackageList(env, JMXConnectorFactory.PROTOCOL_PROVIDER_PACKAGES);
      ClassLoader classLoader = findProviderClassLoader(env, JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER);
      JMXConnectorProvider provider = (JMXConnectorProvider)loadProvider(providerPackages, protocol, MX4JRemoteConstants.CLIENT_PROVIDER_CLASS, classLoader);
      return provider;
   }

   public static JMXConnectorServerProvider newJMXConnectorServerProvider(JMXServiceURL url, Map env) throws IOException
   {
      // Yes, throw NPE if url is null (spec compliant)
      String protocol = normalizeProtocol(url.getProtocol());
      String providerPackages = findProviderPackageList(env, JMXConnectorServerFactory.PROTOCOL_PROVIDER_PACKAGES);
      ClassLoader classLoader = findProviderClassLoader(env, JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER);
      JMXConnectorServerProvider provider = (JMXConnectorServerProvider)loadProvider(providerPackages, protocol, MX4JRemoteConstants.SERVER_PROVIDER_CLASS, classLoader);
      return provider;
   }

   private static String findEnvironmentProviderPackageList(Map environment, String key) throws JMXProviderException
   {
      String providerPackages = null;
      if (environment != null)
      {
         Object pkgs = environment.get(key);
         RmiConnectorActivator.log(LogService.LOG_DEBUG, "Provider packages in the environment: " + pkgs, null);
         if (pkgs != null && !(pkgs instanceof String)) throw new JMXProviderException("Provider package list must be a string");
         providerPackages = (String)pkgs;
      }
      return providerPackages;
   }

   private static String findProviderPackageList(Map environment, final String providerPkgsKey) throws JMXProviderException
   {
      // 1. Look in the environment
      // 2. Look for system property
      // 3. Use implementation's provider

      String providerPackages = findEnvironmentProviderPackageList(environment, providerPkgsKey);

      if (providerPackages == null)
      {
         providerPackages = findSystemPackageList(providerPkgsKey);
      }

      if (providerPackages != null && providerPackages.trim().length() == 0) throw new JMXProviderException("Provider package list cannot be an empty string");

      if (providerPackages == null)
         providerPackages = MX4JRemoteConstants.PROVIDER_PACKAGES;
      else
         providerPackages += MX4JRemoteConstants.PROVIDER_PACKAGES_SEPARATOR + MX4JRemoteConstants.PROVIDER_PACKAGES;

      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Provider packages list is: " + providerPackages,null);

      return providerPackages;
   }

   private static ClassLoader findProviderClassLoader(Map environment, String providerLoaderKey)
   {

      ClassLoader classLoader = null;
      if (environment != null)
      {
         Object loader = environment.get(providerLoaderKey);
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Provider classloader in the environment: " + loader, null);
         if (loader != null && !(loader instanceof ClassLoader)) throw new IllegalArgumentException("Provider classloader is not a ClassLoader");
         classLoader = (ClassLoader)loader;
      }

      if (classLoader == null)
      {
         //classLoader = Thread.currentThread().getContextClassLoader();
         classLoader = ProviderFactory.class.getClassLoader();
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Provider classloader (was null) in the environment: " + classLoader, null);
      }

      // Add the classloader as required by the spec
      environment.put(JMXConnectorFactory.PROTOCOL_PROVIDER_CLASS_LOADER, classLoader);
      RmiConnectorActivator.log(LogService.LOG_WARNING,"Provider classloader added to the environment", null);

      return classLoader;
   }

   private static Object loadProvider(String packages, String protocol, String className, ClassLoader loader) throws JMXProviderException, MalformedURLException
   {
      StringTokenizer tokenizer = new StringTokenizer(packages, MX4JRemoteConstants.PROVIDER_PACKAGES_SEPARATOR);
      while (tokenizer.hasMoreTokens())
      {
         String pkg = tokenizer.nextToken().trim();
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Provider package: " + pkg, null);

         // The spec states the package cannot be empty
         if (pkg.length() == 0) throw new JMXProviderException("Empty package list not allowed: " + packages);

         String providerClassName = constructClassName(pkg, protocol, className);

         Class providerClass = null;
         try
         {
            providerClass = loadClass(providerClassName, loader);
         }
         catch (ClassNotFoundException x)
         {
            RmiConnectorActivator.log(LogService.LOG_DEBUG,"Provider class " + providerClassName + " not found, continuing with next package",null);
            continue;
         }
         catch (Exception x)
         {
            RmiConnectorActivator.log(LogService.LOG_WARNING,"Cannot load provider class " + providerClassName, x);
            throw new JMXProviderException("Cannot load provider class " + providerClassName, x);
         }

         try
         {
            return providerClass.newInstance();
         }
         catch (Exception x)
         {
            RmiConnectorActivator.log(LogService.LOG_WARNING,"Cannot instantiate provider class " + providerClassName, x);
            throw new JMXProviderException("Cannot instantiate provider class " + providerClassName, x);
         }
      }

      // Nothing found
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Could not find provider for protocol " + protocol + " in package list '" + packages + "'", null);
      throw new MalformedURLException("Could not find provider for protocol " + protocol + " in package list '" + packages + "'");
   }
}
