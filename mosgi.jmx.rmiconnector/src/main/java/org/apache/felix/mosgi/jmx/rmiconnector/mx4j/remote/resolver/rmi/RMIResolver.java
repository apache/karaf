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
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.resolver.rmi;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.MalformedURLException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.Remote;
import java.util.Hashtable;
import java.util.Map;

import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.management.remote.rmi.RMIServer;
import javax.management.remote.rmi.RMIServerImpl;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.osgi.service.log.LogService;

import org.apache.felix.mosgi.jmx.rmiconnector.RmiConnectorActivator;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.ConnectionResolver;
import org.apache.felix.mosgi.jmx.agent.mx4j.util.Base64Codec;

/**
 * Resolver for RMI/JRMP protocol.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class RMIResolver extends ConnectionResolver
{
   private static final String JNDI_CONTEXT = "/jndi/";
   private static final String STUB_CONTEXT = "/stub/";


//********************************************************************************************************************//
// CLIENT METHODS


   public Object lookupClient(JMXServiceURL url, Map environment) throws IOException
   {
      return lookupRMIServerStub(url, environment);
   }

   public Object bindClient(Object client, Map environment) throws IOException
   {
      // JRMP does not need anything special
      return client;
   }

   protected RMIServer lookupRMIServerStub(JMXServiceURL url, Map environment) throws IOException
   {
      String path = url.getURLPath();
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"JMXServiceURL for lookup is: '" + url + "'", null);

      if (path != null)
      {
         if (path.startsWith(JNDI_CONTEXT))
         {
            return lookupStubInJNDI(url, environment);
         }

         return decodeStub(url, environment);
      }

      throw new MalformedURLException("Unsupported lookup " + url);
   }

   private RMIServer lookupStubInJNDI(JMXServiceURL url, Map environment) throws IOException
   {

      String path = url.getURLPath();
      String name = path.substring(JNDI_CONTEXT.length());
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"Looking up RMI stub in JNDI under name " + name, null);

      InitialContext ctx = null;
      try
      {
         ctx = new InitialContext(new Hashtable(environment));
         Object stub = ctx.lookup(name);
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Found RMI stub in JNDI " + stub, null);
         return narrowRMIServerStub(stub);
      }
      catch (NamingException x)
      {
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Cannot lookup RMI stub in JNDI", x);
         throw new IOException(x.toString());
      }
      finally
      {
         try
         {
            if (ctx != null) ctx.close();
         }
         catch (NamingException x)
         {
            RmiConnectorActivator.log(LogService.LOG_DEBUG,"Cannot close InitialContext", x);
         }
      }
   }

   protected RMIServer narrowRMIServerStub(Object stub)
   {
      return (RMIServer)stub;
   }

   protected RMIServer decodeStub(JMXServiceURL url, Map environment) throws IOException
   {
      String path = url.getURLPath();
      if (path.startsWith(STUB_CONTEXT))
      {
         byte[] encoded = path.substring(STUB_CONTEXT.length()).getBytes();
         if (!Base64Codec.isArrayByteBase64(encoded)) throw new IOException("Encoded stub form is not a valid Base64 sequence: " + url);
         byte[] decoded = Base64Codec.decodeBase64(encoded);
         ByteArrayInputStream bais = new ByteArrayInputStream(decoded);
         ObjectInputStream ois = null;
         try
         {
            ois = new ObjectInputStream(bais);
            return (RMIServer)ois.readObject();
         }
         catch (ClassNotFoundException x)
         {
            throw new IOException("Cannot decode stub from " + url + ": " + x);
         }
         finally
         {
            if (ois != null) ois.close();
         }
      }
      throw new MalformedURLException("Unsupported binding: " + url);
   }


//********************************************************************************************************************//
// SERVER METHODS


   public Object createServer(JMXServiceURL url, Map environment) throws IOException
   {
      return createRMIServer(url, environment);
   }

   protected RMIServerImpl createRMIServer(JMXServiceURL url, Map environment) throws IOException
   {
      int port = url.getPort();
      RMIClientSocketFactory clientFactory = (RMIClientSocketFactory)environment.get(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE);
      RMIServerSocketFactory serverFactory = (RMIServerSocketFactory)environment.get(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE);
      return new RMIJRMPServerImpl(port, clientFactory, serverFactory, environment);
   }

   public JMXServiceURL bindServer(Object server, JMXServiceURL url, Map environment) throws IOException
   {
      // See javax/management/remote/rmi/package-summary.html

      RMIServerImpl rmiServer = (RMIServerImpl)server;

      RmiConnectorActivator.log(LogService.LOG_DEBUG,"JMXServiceURL for binding is: '" + url + "'",null);

      if (isEncodedForm(url))
      {
         String path = encodeStub(rmiServer, environment);
         return new JMXServiceURL(url.getProtocol(), url.getHost(), url.getPort(), path);
      }

      String jndiURL = parseJNDIForm(url);
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"JMXServiceURL path for binding is: '" + jndiURL + "'", null);

      InitialContext ctx = null;
      try
      {
         ctx = new InitialContext(new Hashtable(environment));
         boolean rebind = Boolean.valueOf((String)environment.get(RMIConnectorServer.JNDI_REBIND_ATTRIBUTE)).booleanValue();
         if (rebind)
            ctx.rebind(jndiURL, rmiServer.toStub());
         else
            ctx.bind(jndiURL, rmiServer.toStub());
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Bound " + rmiServer + " to " + jndiURL, null);
         return url;
      }
      catch (NamingException x)
      {
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Cannot bind server " + rmiServer + " to " + jndiURL, x);
         throw new IOException(x.toString());
      }
      finally
      {
         try
         {
            if (ctx != null) ctx.close();
         }
         catch (NamingException x)
         {
            RmiConnectorActivator.log(LogService.LOG_DEBUG,"Cannot close InitialContext", x);
         }
      }
   }

   protected String encodeStub(RMIServerImpl rmiServer, Map environment) throws IOException
   {
      Remote stub = rmiServer.toStub();
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = null;
      try
      {
         oos = new ObjectOutputStream(baos);
         oos.writeObject(stub);
      }
      finally
      {
         if (oos != null) oos.close();
      }
      byte[] bytes = baos.toByteArray();
      byte[] encoded = Base64Codec.encodeBase64(bytes);
      // Since the bytes are base 64 bytes, the encoding in creating the string is not important: any will work
      return STUB_CONTEXT + new String(encoded);
   }

   protected boolean isEncodedForm(JMXServiceURL url)
   {
      String path = url.getURLPath();
      if (path == null || path.length() == 0 || path.equals("/") || path.startsWith(STUB_CONTEXT)) return true;
      return false;
   }

   private String parseJNDIForm(JMXServiceURL url) throws MalformedURLException
   {
      String path = url.getURLPath();
      if (path.startsWith(JNDI_CONTEXT))
      {
         String jndiURL = path.substring(JNDI_CONTEXT.length());
         if (jndiURL == null || jndiURL.length() == 0) throw new MalformedURLException("No JNDI URL specified: " + url);
         return jndiURL;
      }
      throw new MalformedURLException("Unsupported binding: " + url);
   }

   public void unbindServer(Object server, JMXServiceURL url, Map environment) throws IOException
   {
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"JMXServiceURL for unbinding is: '" + url + "'", null);
      // The server was not bound to JNDI (the stub was encoded), just return
      if (isEncodedForm(url))
      {
         destroyServer(server, environment);
         return;
      }

      String jndiURL = parseJNDIForm(url);
      RmiConnectorActivator.log(LogService.LOG_DEBUG,"JMXServiceURL path for binding is: '" + jndiURL + "'",null);

      InitialContext ctx = null;
      try
      {
         ctx = new InitialContext(new Hashtable(environment));
         ctx.unbind(jndiURL);
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Unbound " + server + " from " + jndiURL, null);
      }
      catch (NamingException x)
      {
         RmiConnectorActivator.log(LogService.LOG_DEBUG,"Cannot unbind server " + server + " to " + jndiURL, x);
         throw new IOException(x.toString());
      }
      finally
      {
         try
         {
            if (ctx != null) ctx.close();
         }
         catch (NamingException x)
         {
            RmiConnectorActivator.log(LogService.LOG_DEBUG,"Cannot close InitialContext", x);
         }
      }
   }

   protected void destroyServer(Object server, Map environment) throws IOException
   {
   }
}
