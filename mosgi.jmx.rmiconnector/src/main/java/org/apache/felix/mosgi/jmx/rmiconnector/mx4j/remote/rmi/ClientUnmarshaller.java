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
package org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.rmi;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.management.MBeanServerConnection;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.ClientProxy;

/**
 * An MBeanServerConnection proxy that performs the setting of the appropriate context classloader
 * to allow classloading of classes sent by the server but not known to the client, in methods like
 * {@link MBeanServerConnection#getAttribute}, {@link MBeanServerConnection#invoke} and so on.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ClientUnmarshaller extends ClientProxy
{
   private final ClassLoader classLoader;

   private ClientUnmarshaller(MBeanServerConnection target, ClassLoader loader)
   {
      super(target);
      this.classLoader = loader;
   }

   public static MBeanServerConnection newInstance(MBeanServerConnection target, ClassLoader loader)
   {
      ClientUnmarshaller handler = new ClientUnmarshaller(target, loader);
      return (MBeanServerConnection)Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[]{MBeanServerConnection.class}, handler);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if (classLoader == null)
      {
         return chain(proxy, method, args);
      }
      else
      {
         ClassLoader old = Thread.currentThread().getContextClassLoader();
         try
         {
            setContextClassLoader(classLoader);
            return chain(proxy, method, args);
         }
         finally
         {
            setContextClassLoader(old);
         }
      }
   }

   private Object chain(Object proxy, Method method, Object[] args) throws Throwable
   {
      return super.invoke(proxy, method, args);
   }

   private void setContextClassLoader(final ClassLoader loader)
   {
      AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            Thread.currentThread().setContextClassLoader(loader);
            return null;
         }
      });
   }
}
