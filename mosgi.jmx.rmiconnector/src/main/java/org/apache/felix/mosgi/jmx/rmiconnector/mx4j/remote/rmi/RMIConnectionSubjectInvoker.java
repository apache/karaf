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
import java.rmi.MarshalledObject;
import java.security.PrivilegedExceptionAction;
import java.security.AccessControlContext;
import java.util.ArrayList;

import javax.management.ObjectName;
import javax.management.remote.JMXServerErrorException;
import javax.management.remote.rmi.RMIConnection;
import javax.security.auth.Subject;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.MX4JRemoteUtils;

/**
 * An RMIConnection proxy that wraps the call into a {@link Subject#doAsPrivileged} invocation,
 * in order to execute the code under subject-based security, and to perform subject delegation.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class RMIConnectionSubjectInvoker extends RMIConnectionProxy
{
   public static RMIConnection newInstance(RMIConnection nested, Subject subject, AccessControlContext context)
   {
      RMIConnectionSubjectInvoker handler = new RMIConnectionSubjectInvoker(nested, subject, context);
      return (RMIConnection)Proxy.newProxyInstance(handler.getClass().getClassLoader(), new Class[] {RMIConnection.class}, handler);
   }

   private final Subject subject;
   private final AccessControlContext context;

   private RMIConnectionSubjectInvoker(RMIConnection nested, Subject subject, AccessControlContext context)
   {
      super(nested);
      this.subject = subject;
      this.context = context;
   }

   public Object invoke(final Object proxy, final Method method, final Object[] args)
           throws Throwable
   {
      String methodName = method.getName();
      if ("fetchNotifications".equals(methodName)) return chain(proxy, method, args);

      if ("addNotificationListeners".equals(methodName))
      {
         Subject[] delegates = (Subject[])args[args.length - 1];
         if (delegates == null || delegates.length == 0) return chain(proxy, method, args);

         if (delegates.length == 1) return subjectInvoke(proxy, method, args, delegates[0]);

         ArrayList ids = new ArrayList();
         for (int i = 0; i < delegates.length; ++i)
         {
            ObjectName name = ((ObjectName[])args[0])[i];
            MarshalledObject filter = ((MarshalledObject[])args[1])[i];
            Subject delegate = delegates[i];
            Object[] newArgs = new Object[] {new ObjectName[] {name}, new MarshalledObject[] {filter}, new Subject[] {delegate}};
            Integer id = ((Integer[])subjectInvoke(proxy, method, newArgs, delegate))[0];
            ids.add(id);
         }
         return (Integer[])ids.toArray(new Integer[ids.size()]);
      }

      // For all other methods, the subject is always the last argument
      Subject delegate = (Subject)args[args.length - 1];

      return subjectInvoke(proxy, method, args, delegate);
   }

   private Object subjectInvoke(final Object proxy, final Method method, final Object[] args, Subject delegate) throws Exception
   {
      return MX4JRemoteUtils.subjectInvoke(subject, delegate, context, new PrivilegedExceptionAction()
      {
         public Object run() throws Exception
         {
            return chain(proxy, method, args);
         }
      });
   }

   private Object chain(Object proxy, Method method, Object[] args) throws Exception
   {
      try
      {
         return super.invoke(proxy, method, args);
      }
      catch (Throwable x)
      {
         if (x instanceof Exception) throw (Exception)x;
         throw new JMXServerErrorException("Error thrown during invocation", (Error)x);
      }
   }
}
