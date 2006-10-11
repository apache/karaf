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

import java.io.IOException;
import java.rmi.MarshalledObject;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.loading.ClassLoaderRepository;
import javax.management.remote.NotificationResult;
import javax.management.remote.rmi.RMIConnection;
import javax.security.auth.Subject;

import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.NotificationTuple;
import org.apache.felix.mosgi.jmx.rmiconnector.mx4j.remote.RemoteNotificationServerHandler;

/**
 * An RMIConnection that "converts" remote calls to {@link MBeanServer} calls,
 * performing unwrapping of parameters and/or the needed actions.
 *
 * @see mx4j.remote.rmi.ClientInvoker
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @author <a href="mailto:btscully@users.sourceforge.net">Brian Scully</a>
 * @version $Revision: 1.1.1.1 $
 */
public class RMIConnectionInvoker implements RMIConnection
{
   private final MBeanServer server;
   private final ClassLoader defaultLoader;
   private final RemoteNotificationServerHandler notificationHandler;

   public RMIConnectionInvoker(MBeanServer server, ClassLoader defaultLoader, Map environment)
   {
      this.server = server;
      this.defaultLoader = defaultLoader;
      // TODO: here we hardcoded the handler for notifications. Maybe worth to make it pluggable ?
      this.notificationHandler = new RMIRemoteNotificationServerHandler(environment);
   }

   public String getConnectionId() throws IOException
   {
      throw new Error("getConnectionId() must not be propagated along the invocation chain");
   }

   public void close() throws IOException
   {
      throw new Error("close() must not be propagated along the invocation chain");
   }

   public ObjectInstance createMBean(String className, ObjectName name, Subject delegate)
           throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           IOException
   {
      return server.createMBean(className, name);
   }

   public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Subject delegate)
           throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           InstanceNotFoundException,
           IOException
   {
      return server.createMBean(className, name, loaderName);
   }

   public ObjectInstance createMBean(String className, ObjectName name, MarshalledObject params, String[] signature, Subject delegate)
           throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           IOException
   {
      Object[] args = (Object[])RMIMarshaller.unmarshal(params, new RepositoryClassLoader(server.getClassLoaderRepository()), defaultLoader);
      return server.createMBean(className, name, args, signature);
   }

   public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, MarshalledObject params, String[] signature, Subject delegate)
           throws ReflectionException,
           InstanceAlreadyExistsException,
           MBeanRegistrationException,
           MBeanException,
           NotCompliantMBeanException,
           InstanceNotFoundException,
           IOException
   {
      Object[] args = (Object[])RMIMarshaller.unmarshal(params, server.getClassLoader(loaderName), defaultLoader);
      return server.createMBean(className, name, loaderName, args, signature);
   }

   public void unregisterMBean(ObjectName name, Subject delegate) throws InstanceNotFoundException, MBeanRegistrationException, IOException
   {
      server.unregisterMBean(name);
   }

   public ObjectInstance getObjectInstance(ObjectName name, Subject delegate) throws InstanceNotFoundException, IOException
   {
      return server.getObjectInstance(name);
   }

   public Set queryMBeans(ObjectName name, MarshalledObject query, Subject delegate) throws IOException
   {
      QueryExp filter = (QueryExp)RMIMarshaller.unmarshal(query, null, defaultLoader);
      return server.queryMBeans(name, filter);
   }

   public Set queryNames(ObjectName name, MarshalledObject query, Subject delegate) throws IOException
   {
      QueryExp filter = (QueryExp)RMIMarshaller.unmarshal(query, null, defaultLoader);
      return server.queryNames(name, filter);
   }

   public boolean isRegistered(ObjectName name, Subject delegate) throws IOException
   {
      return server.isRegistered(name);
   }

   public Integer getMBeanCount(Subject delegate) throws IOException
   {
      return server.getMBeanCount();
   }

   public Object getAttribute(ObjectName name, String attribute, Subject delegate)
           throws MBeanException,
           AttributeNotFoundException,
           InstanceNotFoundException,
           ReflectionException,
           IOException
   {
      return server.getAttribute(name, attribute);
   }

   public AttributeList getAttributes(ObjectName name, String[] attributes, Subject delegate)
           throws InstanceNotFoundException, ReflectionException, IOException
   {
      return server.getAttributes(name, attributes);
   }

   public void setAttribute(ObjectName name, MarshalledObject attribute, Subject delegate)
           throws InstanceNotFoundException,
           AttributeNotFoundException,
           InvalidAttributeValueException,
           MBeanException,
           ReflectionException,
           IOException
   {
      Attribute attrib = (Attribute)RMIMarshaller.unmarshal(attribute, server.getClassLoaderFor(name), defaultLoader);
      server.setAttribute(name, attrib);
   }

   public AttributeList setAttributes(ObjectName name, MarshalledObject attributes, Subject delegate)
           throws InstanceNotFoundException,
           ReflectionException,
           IOException
   {
      AttributeList attribs = (AttributeList)RMIMarshaller.unmarshal(attributes, server.getClassLoaderFor(name), defaultLoader);
      return server.setAttributes(name, attribs);
   }

   public Object invoke(ObjectName name, String operationName, MarshalledObject params, String[] signature, Subject delegate)
           throws InstanceNotFoundException,
           MBeanException,
           ReflectionException,
           IOException
   {
      Object[] args = (Object[])RMIMarshaller.unmarshal(params, server.getClassLoaderFor(name), defaultLoader);
      return server.invoke(name, operationName, args, signature);
   }

   public String getDefaultDomain(Subject delegate) throws IOException
   {
      return server.getDefaultDomain();
   }

   public String[] getDomains(Subject delegate) throws IOException
   {
      return server.getDomains();
   }

   public MBeanInfo getMBeanInfo(ObjectName name, Subject delegate) throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException
   {
      return server.getMBeanInfo(name);
   }

   public boolean isInstanceOf(ObjectName name, String className, Subject delegate) throws InstanceNotFoundException, IOException
   {
      return server.isInstanceOf(name, className);
   }

   public void addNotificationListener(ObjectName name, ObjectName listener, MarshalledObject filter, MarshalledObject handback, Subject delegate)
           throws InstanceNotFoundException, IOException
   {
      NotificationFilter f = (NotificationFilter)RMIMarshaller.unmarshal(filter, server.getClassLoaderFor(name), defaultLoader);
      Object h = RMIMarshaller.unmarshal(handback, server.getClassLoaderFor(name), defaultLoader);
      server.addNotificationListener(name, listener, f, h);
   }

   public void removeNotificationListener(ObjectName name, ObjectName listener, Subject delegate)
           throws InstanceNotFoundException, ListenerNotFoundException, IOException
   {
      server.removeNotificationListener(name, listener);
   }

   public void removeNotificationListener(ObjectName name, ObjectName listener, MarshalledObject filter, MarshalledObject handback, Subject delegate)
           throws InstanceNotFoundException, ListenerNotFoundException, IOException
   {
      NotificationFilter f = (NotificationFilter)RMIMarshaller.unmarshal(filter, server.getClassLoaderFor(name), defaultLoader);
      Object h = RMIMarshaller.unmarshal(handback, server.getClassLoaderFor(name), defaultLoader);
      server.removeNotificationListener(name, listener, f, h);
   }

   public Integer[] addNotificationListeners(ObjectName[] names, MarshalledObject[] filters, Subject[] delegates) throws InstanceNotFoundException, IOException
   {
      ArrayList ids = new ArrayList();
      for (int i = 0; i < names.length; ++i)
      {
         ObjectName name = names[i];
         MarshalledObject filter = filters[i];
         NotificationFilter f = (NotificationFilter)RMIMarshaller.unmarshal(filter, server.getClassLoaderFor(name), defaultLoader);
         Integer id = notificationHandler.generateListenerID(name, f);
         NotificationListener listener = notificationHandler.getServerNotificationListener();
         server.addNotificationListener(name, listener, f, id);
         notificationHandler.addNotificationListener(id, new NotificationTuple(name, listener, f, id));
         ids.add(id);
      }
      return (Integer[])ids.toArray(new Integer[ids.size()]);
   }

   public void removeNotificationListeners(ObjectName name, Integer[] listenerIDs, Subject delegate) throws InstanceNotFoundException, ListenerNotFoundException, IOException
   {
      for (int i = 0; i < listenerIDs.length; ++i)
      {
         Integer id = listenerIDs[i];
         NotificationTuple tuple = notificationHandler.getNotificationListener(id);
         server.removeNotificationListener(name, tuple.getNotificationListener(), tuple.getNotificationFilter(), tuple.getHandback());
         notificationHandler.removeNotificationListener(id);
      }
   }

   public NotificationResult fetchNotifications(long clientSequenceNumber, int maxNotifications, long timeout) throws IOException
   {
      return notificationHandler.fetchNotifications(clientSequenceNumber, maxNotifications, timeout);
   }

   private static class RepositoryClassLoader extends SecureClassLoader
   {
      private final ClassLoaderRepository repository;

      private RepositoryClassLoader(ClassLoaderRepository repository)
      {
         this.repository = repository;
      }

      public Class loadClass(String name) throws ClassNotFoundException
      {
         return repository.loadClass(name);
      }
   }
}
