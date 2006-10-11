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
import java.rmi.NoSuchObjectException;
import java.io.IOException;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXServerErrorException;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.ClientProxy;

/**
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ClientExceptionCatcher extends ClientProxy
{
   private ClientExceptionCatcher(MBeanServerConnection target)
   {
      super(target);
   }

   public static MBeanServerConnection newInstance(MBeanServerConnection target)
   {
      ClientExceptionCatcher handler = new ClientExceptionCatcher(target);
      return (MBeanServerConnection)Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[]{MBeanServerConnection.class}, handler);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      try
      {
         return super.invoke(proxy, method, args);
      }
      catch (NoSuchObjectException x)
      {
         // The connection has been already closed by the server
         throw new IOException("Connection closed by the server");
      }
      catch (Exception x)
      {
         throw x;
      }
      catch (Error x)
      {
         throw new JMXServerErrorException("Error thrown during invocation", x);
      }
   }
}
