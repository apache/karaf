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

import java.security.PrivilegedAction;
import java.security.AccessController;

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
import javax.management.ReflectionException;

import org.apache.felix.mosgi.jmx.agent.mx4j.server.MBeanMetaData;

/**
 * This interceptor sets the context class loader to the proper value for incoming calls.
 * It saves the current context class loader, set the context class loader to be the MBean's class loader for
 * the current call, and on return re-set the context class loader to the previous value
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ContextClassLoaderMBeanServerInterceptor extends DefaultMBeanServerInterceptor
{
   public ContextClassLoaderMBeanServerInterceptor()
   {
      // Disabled by default
      setEnabled(false);
   }

   public String getType()
   {
      return "contextclassloader";
   }

   public void addNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback)
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               super.addNotificationListener(metadata, listener, filter, handback);
               return;
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      super.addNotificationListener(metadata, listener, filter, handback);
   }

   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener) throws ListenerNotFoundException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               super.removeNotificationListener(metadata, listener);
               return;
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      super.removeNotificationListener(metadata, listener);
   }

   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               super.removeNotificationListener(metadata, listener, filter, handback);
               return;
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      super.removeNotificationListener(metadata, listener, filter, handback);
   }

   public void instantiate(MBeanMetaData metadata, String className, String[] params, Object[] args) throws ReflectionException, MBeanException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               super.instantiate(metadata, className, params, args);
               return;
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      super.instantiate(metadata, className, params, args);
   }

   public void registration(MBeanMetaData metadata, int operation) throws MBeanRegistrationException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               super.registration(metadata, operation);
               return;
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      super.registration(metadata, operation);
   }

   public MBeanInfo getMBeanInfo(MBeanMetaData metadata)
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               return super.getMBeanInfo(metadata);
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      return super.getMBeanInfo(metadata);
   }

   public Object invoke(MBeanMetaData metadata, String method, String[] params, Object[] args) throws MBeanException, ReflectionException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               return super.invoke(metadata, method, params, args);
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      return super.invoke(metadata, method, params, args);
   }

   public AttributeList getAttributes(MBeanMetaData metadata, String[] attributes)
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               return super.getAttributes(metadata, attributes);
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      return super.getAttributes(metadata, attributes);
   }

   public AttributeList setAttributes(MBeanMetaData metadata, AttributeList attributes)
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               return super.setAttributes(metadata, attributes);
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      return super.setAttributes(metadata, attributes);
   }

   public Object getAttribute(MBeanMetaData metadata, String attribute) throws MBeanException, AttributeNotFoundException, ReflectionException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               return super.getAttribute(metadata, attribute);
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      return super.getAttribute(metadata, attribute);
   }

   public void setAttribute(MBeanMetaData metadata, Attribute attribute) throws MBeanException, AttributeNotFoundException, InvalidAttributeValueException, ReflectionException
   {
      if (isEnabled())
      {
         ClassLoader context = getContextClassLoader();
         if (metadata.classloader != context)
         {
            try
            {
               setContextClassLoader(metadata.classloader);
               super.setAttribute(metadata, attribute);
               return;
            }
            finally
            {
               setContextClassLoader(context);
            }
         }
      }

      super.setAttribute(metadata, attribute);
   }

   private ClassLoader getContextClassLoader()
   {
      return Thread.currentThread().getContextClassLoader();
   }

   private void setContextClassLoader(final ClassLoader cl)
   {
      AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            Thread.currentThread().setContextClassLoader(cl);
            return null;
         }
      });
   }
}
