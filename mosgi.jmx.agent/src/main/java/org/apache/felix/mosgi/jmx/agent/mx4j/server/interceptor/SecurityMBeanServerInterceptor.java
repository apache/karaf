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


import java.security.AccessControlException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.ArrayList;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanRegistrationException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.MBeanPermission;
import javax.management.MBeanTrustPermission;

import org.apache.felix.mosgi.jmx.agent.mx4j.server.MBeanMetaData;

/**
 * Interceptor that takes care of performing security checks (in case the SecurityManager is installed) for
 * MBeanServer to MBean calls.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class SecurityMBeanServerInterceptor extends DefaultMBeanServerInterceptor implements SecurityMBeanServerInterceptorMBean
{
   public String getType()
   {
      return "security";
   }

   public boolean isEnabled()
   {
      return true;
   }

   public void addNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback)
   {
      checkPermission(metadata.info.getClassName(), null, metadata.name, "addNotificationListener");
      super.addNotificationListener(metadata, listener, filter, handback);
   }

   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener) throws ListenerNotFoundException
   {
      checkPermission(metadata.info.getClassName(), null, metadata.name, "removeNotificationListener");
      super.removeNotificationListener(metadata, listener);
   }

   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException
   {
      checkPermission(metadata.info.getClassName(), null, metadata.name, "removeNotificationListener");
      super.removeNotificationListener(metadata, listener, filter, handback);
   }

   public void instantiate(MBeanMetaData metadata, String className, String[] params, Object[] args) throws ReflectionException, MBeanException
   {
      checkPermission(className, null, metadata.name, "instantiate");
      super.instantiate(metadata, className, params, args);
   }

   public MBeanInfo getMBeanInfo(MBeanMetaData metadata)
   {
      checkPermission(metadata.info.getClassName(), null, metadata.name, "getMBeanInfo");
      return super.getMBeanInfo(metadata);
   }

   public Object invoke(MBeanMetaData metadata, String method, String[] params, Object[] args) throws MBeanException, ReflectionException
   {
      checkPermission(metadata.info.getClassName(), method, metadata.name, "invoke");
      return super.invoke(metadata, method, params, args);
   }

   public AttributeList getAttributes(MBeanMetaData metadata, String[] attributes)
   {
      Object[] secured = filterAttributes(metadata.info.getClassName(), metadata.name, attributes, true);
      String[] array = new String[secured.length];
      for (int i = 0; i < array.length; ++i) array[i] = (String)secured[i];
      return super.getAttributes(metadata, array);
   }

   public AttributeList setAttributes(MBeanMetaData metadata, AttributeList attributes)
   {
      Object[] secured = filterAttributes(metadata.info.getClassName(), metadata.name, attributes.toArray(), false);
      AttributeList list = new AttributeList();
      for (int i = 0; i < secured.length; ++i) list.add(secured[i]);
      return super.setAttributes(metadata, list);
   }

   public Object getAttribute(MBeanMetaData metadata, String attribute) throws MBeanException, AttributeNotFoundException, ReflectionException
   {
      checkPermission(metadata.info.getClassName(), attribute, metadata.name, "getAttribute");
      return super.getAttribute(metadata, attribute);
   }

   public void setAttribute(MBeanMetaData metadata, Attribute attribute) throws MBeanException, AttributeNotFoundException, InvalidAttributeValueException, ReflectionException
   {
      checkPermission(metadata.info.getClassName(), attribute.getName(), metadata.name, "setAttribute");
      super.setAttribute(metadata, attribute);
   }

   public void registration(MBeanMetaData metadata, int operation) throws MBeanRegistrationException
   {
      switch (operation)
      {
         case PRE_REGISTER:
            checkPermission(metadata.info.getClassName(), null, metadata.name, "registerMBean");
            checkTrustRegistration(metadata.mbean.getClass());
            break;
         case POST_REGISTER_TRUE:
            // The MBean can implement MBeanRegistration and change the ObjectName
            checkPermission(metadata.info.getClassName(), null, metadata.name, "registerMBean");
            break;
         case PRE_DEREGISTER:
            checkPermission(metadata.info.getClassName(), null, metadata.name, "unregisterMBean");
            break;
         default:
            break;
      }
      super.registration(metadata, operation);
   }

   private void checkPermission(String className, String methodName, ObjectName objectname, String action)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         sm.checkPermission(new MBeanPermission(className, methodName, objectname, action));
      }
   }

   private void checkTrustRegistration(final Class cls)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm != null)
      {
         ProtectionDomain domain = (ProtectionDomain)AccessController.doPrivileged(new PrivilegedAction()
         {
            public Object run()
            {
               return cls.getProtectionDomain();
            }
         });

         MBeanTrustPermission permission = new MBeanTrustPermission("register");
         if (!domain.implies(permission))
         {
            throw new AccessControlException("Access denied " + permission + ": MBean class " + cls.getName() + " is not trusted for registration");
         }
      }
   }

   private Object[] filterAttributes(String className, ObjectName objectName, Object[] attributes, boolean isGet)
   {
      SecurityManager sm = System.getSecurityManager();
      if (sm == null) return attributes;

      ArrayList list = new ArrayList();

      for (int i = 0; i < attributes.length; ++i)
      {
         Object attribute = attributes[i];
         String name = isGet ? (String)attribute : ((Attribute)attribute).getName();

         try
         {
            checkPermission(className, name, objectName, isGet ? "getAttribute" : "setAttribute");
            list.add(attribute);
         }
         catch (SecurityException ignore)
         {
            // This is ok.  We just don't add this attribute to the list
         }
      }

      return list.toArray();
   }
}
