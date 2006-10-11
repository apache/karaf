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
import java.util.Map;
import java.util.StringTokenizer;

import javax.management.remote.JMXServiceURL;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.MX4JRemoteConstants;
import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.ProviderHelper;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;

/**
 * ConnectionResolver handles the details of creating connections for different protocols.
 * Subclasses for the specific protocol are found using a mechanism very similar to the
 * one specified by {@link javax.management.remote.JMXConnectorFactory}. Here a subclass
 * has a fully qualified name specified like this:
 * <package>.resolver.<protocol>.<PROTOCOL>Resolver, for example
 * {@link mx4j.remote.resolver.rmi.RMIResolver}
 * This class is used from both the client and the server.
 * The former uses it to lookup stubs or connections to the server side; the latter uses it
 * to create server instances and make them availale to clients, for example via JNDI.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public abstract class ConnectionResolver extends ProviderHelper
{
   /**
    * Returns a subclass of ConnectionResolver for the specified protocol.
    */
   public static ConnectionResolver getInstance(String proto) {
      String protocol = normalizeProtocol(proto);
      String resolverPackages = findResolverPackageList();
//sfr      return loadResolver(resolverPackages, protocol, Thread.currentThread().getContextClassLoader());
      return loadResolver(resolverPackages, protocol, ConnectionResolver.class.getClassLoader());
   }

   private static String findResolverPackageList() {
      String packages = findSystemPackageList(MX4JRemoteConstants.PROTOCOL_RESOLVER_PACKAGES);
      if (packages == null)
         packages = MX4JRemoteConstants.RESOLVER_PACKAGES;
      else
         packages += MX4JRemoteConstants.RESOLVER_PACKAGES_SEPARATOR + MX4JRemoteConstants.RESOLVER_PACKAGES;
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Resolver packages list is: " + packages, null);
      return packages;
   }

   private static ConnectionResolver loadResolver(String packages, String protocol, ClassLoader loader) {
      StringTokenizer tokenizer = new StringTokenizer(packages, MX4JRemoteConstants.RESOLVER_PACKAGES_SEPARATOR);
      while (tokenizer.hasMoreTokens()) {
         String pkg = tokenizer.nextToken().trim();
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Resolver package: " + pkg,null);
         if (pkg.length() == 0) continue;

         String className = protocol.toUpperCase() + MX4JRemoteConstants.RESOLVER_CLASS;
         String resolverClassName = constructClassName(pkg, protocol, className);

         Class resolverClass = null;
         try
         {
            resolverClass = loadClass(resolverClassName, loader);
         }
         catch (ClassNotFoundException x)
         {
            RmiConnectorActivator.log(LogService.LOG_DEBUG,"Resolver class " + resolverClassName + " not found, continuing with next package",null);
            continue;
         }
         catch (Exception x)
         {
            RmiConnectorActivator.log(LogService.LOG_WARNING, "Cannot load resolver class " + resolverClassName, x);
            return null;
         }

         try
         {
            return (ConnectionResolver)resolverClass.newInstance();
         }
         catch (Exception x)
         {
            RmiConnectorActivator.log(LogService.LOG_WARNING,"Cannot instantiate resolver class " + resolverClassName, x);
            return null;
         }
      }

      // Nothing found
       RmiConnectorActivator.log(LogService.LOG_DEBUG,"Could not find resolver for protocol " + protocol + " in package list '" + packages + "'", null);
      return null;
   }

   /**
    * Looks up a connection to the server side as specified in the given JMXServiceURL.
    * This method is used by {@link javax.management.remote.JMXConnector}s.
    */
   public abstract Object lookupClient(JMXServiceURL url, Map environment) throws IOException;

   /**
    * Connects the client returned by {@link #lookupClient} to the server side
    */
   public abstract Object bindClient(Object client, Map environment) throws IOException;

   /**
    * Creates an instance of the server as specified in the given JMXServiceURL.
    * It is only a factory method, it should just return a fresh instance of the server;
    * other methods are responsible to make it available to clients (for example exporting it).
    * This method is used by {@link javax.management.remote.JMXConnectorServer}s.
    * @see #bindServer
    */
   public abstract Object createServer(JMXServiceURL url, Map environment) throws IOException;

   /**
    * Binds the server created by {@link #createServer} to a place specified in the JMXServiceURL.
    * @return a new JMXServiceURL that specifies where the server has been bound to.
    * @see #unbindServer
    */
   public abstract JMXServiceURL bindServer(Object server, JMXServiceURL url, Map environment) throws IOException;

   /**
    * Unbinds the server created by {@link #createServer} from the place specified in the JMXServiceURL.
    * @see #bindServer
    */
   public abstract void unbindServer(Object server, JMXServiceURL address, Map environment) throws IOException;
}
