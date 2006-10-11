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
package org.apache.felix.mosgi.jmx.agent.mx4j.server;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.HashMap;

import javax.management.DynamicMBean;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.NotificationBroadcaster;
import javax.management.loading.MLet;

import org.apache.felix.mosgi.jmx.agent.mx4j.MBeanDescription;
import org.apache.felix.mosgi.jmx.agent.mx4j.MBeanDescriptionAdapter;
import org.apache.felix.mosgi.jmx.agent.mx4j.MX4JSystemKeys;
import org.apache.felix.mosgi.jmx.agent.mx4j.log.Log;
import org.apache.felix.mosgi.jmx.agent.mx4j.log.Logger;
import org.apache.felix.mosgi.jmx.agent.mx4j.util.Utils;

/**
 * Introspector for MBeans. <p>
 * Main purposes of this class are:
 * <ul>
 * <li> Given an mbean, gather all information regarding it into a {@link MBeanMetaData} instance, see {@link #introspect}
 * <li> Given an introspected MBeanMetaData, decide if the MBean is compliant or not.
 * <li> Act as a factory for {@link MBeanInvoker}s
 * </ul>
 *
 * The following system properties are used to control this class' behavior:
 * <ul>
 * <li> mx4j.strict.mbean.interface, if set to 'no' then are treated as standard MBeans also classes that implement
 * management interfaces beloging to different packages or that are inner classes; otherwise are treated as MBeans
 * only classes that implement interfaces whose name if the fully qualified name of the MBean class + "MBean"
 * <li> mx4j.mbean.invoker, if set to the qualified name of an implementation of the {@link MBeanInvoker} interface,
 * then an instance of the class will be used to invoke methods on standard MBeans. By default the generated-on-the-fly
 * MBeanInvoker is used; to revert to the version that uses reflection, for example,
 * use mx4j.mbean.invoker = {@link ReflectedMBeanInvoker mx4j.server.ReflectedMBeanInvoker}
 * </ul>
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MBeanIntrospector
{
   private static MBeanDescriptionAdapter DEFAULT_DESCRIPTION = new MBeanDescriptionAdapter();

   private boolean m_useExtendedMBeanInterfaces = false;
   private boolean m_bcelClassesAvailable = false;
   private final String m_customMBeanInvoker;

   public MBeanIntrospector()
   {
      String strict = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty(MX4JSystemKeys.MX4J_STRICT_MBEAN_INTERFACE);
         }
      });
      if (strict != null && !Boolean.valueOf(strict).booleanValue())
      {
         m_useExtendedMBeanInterfaces = true;
      }

      // Try to see if BCEL classes are present
   /* SFR : removed BCEL management
      try
      {
         getClass().getClassLoader().loadClass("org.apache.bcel.generic.Type");
         m_bcelClassesAvailable = true;
      }
      catch (Throwable ignored)
      {
      }
   */

      // See if someone specified which MBean invoker to use
      m_customMBeanInvoker = (String)AccessController.doPrivileged(new PrivilegedAction()
      {
         public Object run()
         {
            return System.getProperty(MX4JSystemKeys.MX4J_MBEAN_INVOKER);
         }
      });
   }

   /**
    * Introspect the given mbean, storing the results in the given metadata.
    * It expects that the mbean field and the classloader field are not null
    * @see #isMBeanCompliant
    */
   public void introspect(MBeanMetaData metadata)
   {
      introspectType(metadata);
      introspectMBeanInfo(metadata);
   }

   /**
    * Returns whether the given already introspected metadata is compliant.
    * Must be called after {@link #introspect}
    */
   public boolean isMBeanCompliant(MBeanMetaData metadata)
   {
      return isMBeanClassCompliant(metadata) && isMBeanTypeCompliant(metadata) && isMBeanInfoCompliant(metadata);
   }

   /**
    * Used by the test cases, invoked via reflection, keep it private.
    * Introspect the mbean and returns if it's compliant
    */
   private boolean testCompliance(MBeanMetaData metadata)
   {
      introspect(metadata);
      return isMBeanCompliant(metadata);
   }

   private boolean isMBeanClassCompliant(MBeanMetaData metadata)
   {
      // From JMX 1.1: no requirements (can be abstract, non public and no accessible constructors)
      return true;
   }

   private boolean isMBeanTypeCompliant(MBeanMetaData metadata)
   {
      Logger logger = getLogger();

      if (metadata.standard && metadata.dynamic)
      {
         if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBean is both standard and dynamic");
         return false;
      }
      if (!metadata.standard && !metadata.dynamic)
      {
         if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBean is not standard nor dynamic");
         return false;
      }

      return true;
   }

   private boolean isMBeanInfoCompliant(MBeanMetaData metadata)
   {
      Logger logger = getLogger();

      if (metadata.info == null)
      {
         if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBeanInfo is null");
         return false;
      }
      return true;
   }

   private void introspectType(MBeanMetaData metadata)
   {
      // Some information is already provided (StandardMBean)
      if (metadata.standard)
      {
         introspectStandardMBean(metadata);
         return;
      }

      if (metadata.mbean instanceof DynamicMBean)
      {
         metadata.dynamic = true;
         return;
      }
      else
      {
         metadata.dynamic = false;
         // Continue and see if it's a plain standard MBean
      }

      // We have a plain standard MBean, introspect it
      introspectStandardMBean(metadata);
   }

   private void introspectStandardMBean(MBeanMetaData metadata)
   {
      if (metadata.management != null)
      {
         // Be sure the MBean implements the management interface
         if (metadata.management.isInstance(metadata.mbean))
         {
            if (metadata.invoker == null) metadata.invoker = createInvoker(metadata);
            return;
         }
         else
         {
            // Not compliant, reset the values
            metadata.standard = false;
            metadata.management = null;
            metadata.invoker = null;
            return;
         }
      }
      else
      {
         Class cls = metadata.mbean.getClass();
         for (Class c = cls; c != null; c = c.getSuperclass())
         {
            Class[] intfs = c.getInterfaces();
            for (int i = 0; i < intfs.length; ++i)
            {
               Class intf = intfs[i];

               if (implementsMBean(c.getName(), intf.getName()))
               {
                  // OK, found the MBean interface for this class
                  metadata.standard = true;
                  metadata.management = intf;
                  metadata.invoker = createInvoker(metadata);
                  return;
               }
            }
         }

         // Management interface not found, it's not compliant, reset the values
         metadata.standard = false;
         metadata.management = null;
         metadata.invoker = null;
      }
   }

   private void introspectMBeanInfo(MBeanMetaData metadata)
   {
      if (metadata.dynamic)
      {
         metadata.info = getDynamicMBeanInfo(metadata);
      }
      else if (metadata.standard)
      {
         metadata.info = createStandardMBeanInfo(metadata);
      }
      else
      {
         // Not a valid MBean, reset the MBeanInfo: this will cause an exception later
         metadata.info = null;
      }
   }

   private MBeanInfo getDynamicMBeanInfo(MBeanMetaData metadata)
   {
      Logger logger = getLogger();

      MBeanInfo info = null;
      
	  try {
		 info = ((DynamicMBean) metadata.mbean).getMBeanInfo();
	  } catch (Exception x) {
	     if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("getMBeanInfo threw: " + x.toString());
	  }
	  
      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Dynamic MBeanInfo is: " + info);

      if (info == null)
      {
         if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBeanInfo cannot be null");
         return null;
      }

      return info;
   }

   private MBeanInfo createStandardMBeanInfo(MBeanMetaData metadata)
   {
      // This is a non-standard extension: description for standard MBeans
      MBeanDescription description = createMBeanDescription(metadata);

      MBeanConstructorInfo[] ctors = createMBeanConstructorInfo(metadata, description);
      if (ctors == null) return null;
      MBeanAttributeInfo[] attrs = createMBeanAttributeInfo(metadata, description);
      if (attrs == null) return null;
      MBeanOperationInfo[] opers = createMBeanOperationInfo(metadata, description);
      if (opers == null) return null;
      MBeanNotificationInfo[] notifs = createMBeanNotificationInfo(metadata);
      if (notifs == null) return null;

      return new MBeanInfo(metadata.mbean.getClass().getName(), description.getMBeanDescription(), attrs, ctors, opers, notifs);
   }

   private MBeanDescription createMBeanDescription(MBeanMetaData metadata)
   {
      // This is a non-standard extension

      Logger logger = getLogger();
      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Looking for standard MBean description...");

      Class mbeanClass = metadata.mbean.getClass();

      for (Class cls = mbeanClass; cls != null; cls = cls.getSuperclass())
      {
         String clsName = cls.getName();
         if (clsName.startsWith("java.")) break;

         // Use full qualified name only
         String descrClassName = clsName + "MBeanDescription";
         // Try to load the class
         try
         {
            Class descrClass = null;
            ClassLoader loader = metadata.classloader;
            if (loader != null)
            {
               // Optimize lookup of the description class in case of MLets: we lookup the description class
               // only in the classloader of the mbean, not in the whole CLR (since MLets delegates to the CLR)
               if (loader.getClass() == MLet.class)
                  descrClass = ((MLet)loader).loadClass(descrClassName, null);
               else
                  descrClass = loader.loadClass(descrClassName);
            }
            else
            {
               descrClass = Class.forName(descrClassName, false, null);
            }

            Object descrInstance = descrClass.newInstance();
            if (descrInstance instanceof MBeanDescription)
            {
               MBeanDescription description = (MBeanDescription)descrInstance;
               if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Found provided standard MBean description: " + description);
               return description;
            }
         }
         catch (ClassNotFoundException ignored)
         {
         }
         catch (InstantiationException ignored)
         {
         }
         catch (IllegalAccessException ignored)
         {
         }
      }

      MBeanDescription description = DEFAULT_DESCRIPTION;
      if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Cannot find standard MBean description, using default: " + description);
      return description;
   }

   private MBeanOperationInfo[] createMBeanOperationInfo(MBeanMetaData metadata, MBeanDescription description)
   {
      ArrayList operations = new ArrayList();

      Method[] methods = metadata.management.getMethods();
      for (int j = 0; j < methods.length; ++j)
      {
         Method method = methods[j];
         if (!Utils.isAttributeGetter(method) && !Utils.isAttributeSetter(method))
         {
            String descr = description == null ? null : description.getOperationDescription(method);
            Class[] params = method.getParameterTypes();
            MBeanParameterInfo[] paramsInfo = new MBeanParameterInfo[params.length];
            for (int k = 0; k < params.length; ++k)
            {
               Class param = params[k];
               String paramName = description == null ? null : description.getOperationParameterName(method, k);
               String paramDescr = description == null ? null : description.getOperationParameterDescription(method, k);
               paramsInfo[k] = new MBeanParameterInfo(paramName, param.getName(), paramDescr);
            }
            MBeanOperationInfo info = new MBeanOperationInfo(method.getName(), descr, paramsInfo, method.getReturnType().getName(), MBeanOperationInfo.UNKNOWN);
            operations.add(info);
         }
      }

      return (MBeanOperationInfo[])operations.toArray(new MBeanOperationInfo[operations.size()]);
   }

   private MBeanAttributeInfo[] createMBeanAttributeInfo(MBeanMetaData metadata, MBeanDescription description)
   {
      Logger logger = getLogger();

      HashMap attributes = new HashMap();
      HashMap getterNames = new HashMap();

      Method[] methods = metadata.management.getMethods();
      for (int j = 0; j < methods.length; ++j)
      {
         Method method = methods[j];
         if (Utils.isAttributeGetter(method))
         {
            String name = method.getName();
            boolean isIs = name.startsWith("is");

            String attribute = null;
            if (isIs)
               attribute = name.substring(2);
            else
               attribute = name.substring(3);

            String descr = description == null ? null : description.getAttributeDescription(attribute);

            MBeanAttributeInfo info = (MBeanAttributeInfo)attributes.get(attribute);

            if (info != null)
            {
               // JMX spec does not allow overloading attributes.
               // If an attribute with the same name already exists the MBean is not compliant
               if (!info.getType().equals(method.getReturnType().getName()))
               {
                  if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBean is not compliant: has overloaded attribute " + attribute);
                  return null;
               }
               else
               {
                  // They return the same value, 
                  if (getterNames.get(name) != null)
                  {
                  	// This is the case of an attribute being present in multiple interfaces
                  	// Ignore all but the first, since they resolve to the same method anyways
                 	continue;                  	 
                  }
                  
				  // there is a chance that one is a get-getter and one is a is-getter
				  // for a boolean attribute. In this case, the MBean is not compliant.
                  if (info.isReadable())
                  {
                     if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBean is not compliant: has overloaded attribute " + attribute);
                     return null;
                  }

                  // MBeanAttributeInfo is already present due to a setter method, just update its readability
                  info = new MBeanAttributeInfo(attribute, info.getType(), info.getDescription(), true, info.isWritable(), isIs);
               }
            }
            else
            {
               info = new MBeanAttributeInfo(attribute, method.getReturnType().getName(), descr, true, false, isIs);
            }

            // Replace if exists
            attributes.put(attribute, info);
			getterNames.put(name,method);
         }
         else if (Utils.isAttributeSetter(method))
         {
            String name = method.getName();
            String attribute = name.substring(3);

            String descr = description == null ? null : description.getAttributeDescription(attribute);

            MBeanAttributeInfo info = (MBeanAttributeInfo)attributes.get(attribute);

            if (info != null)
            {
               // JMX spec does not allow overloading attributes.
               // If an attribute with the same name already exists the MBean is not compliant
               if (!info.getType().equals(method.getParameterTypes()[0].getName()))
               {
                  if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("MBean is not compliant: has overloaded attribute " + attribute);
                  return null;
               }
               else
               {
                  // MBeanAttributeInfo is already present due to a getter method, just update its writability
                  info = new MBeanAttributeInfo(info.getName(), info.getType(), info.getDescription(), info.isReadable(), true, info.isIs());
               }
            }
            else
            {
               info = new MBeanAttributeInfo(attribute, method.getParameterTypes()[0].getName(), descr, false, true, false);
            }

            // Replace if exists
            attributes.put(attribute, info);
         }
      }

      return (MBeanAttributeInfo[])attributes.values().toArray(new MBeanAttributeInfo[attributes.size()]);
   }

   private MBeanNotificationInfo[] createMBeanNotificationInfo(MBeanMetaData metadata)
   {
      MBeanNotificationInfo[] notifs = null;
      if (metadata.mbean instanceof NotificationBroadcaster)
      {
         notifs = ((NotificationBroadcaster)metadata.mbean).getNotificationInfo();
      }
      if (notifs == null) notifs = new MBeanNotificationInfo[0];
      return notifs;
   }

   private MBeanConstructorInfo[] createMBeanConstructorInfo(MBeanMetaData metadata, MBeanDescription descrs)
   {
      Class mbeanClass = metadata.mbean.getClass();

      Constructor[] ctors = mbeanClass.getConstructors();
      MBeanConstructorInfo[] constructors = new MBeanConstructorInfo[ctors.length];
      for (int i = 0; i < ctors.length; ++i)
      {
         Constructor constructor = ctors[i];
         String descr = descrs == null ? null : descrs.getConstructorDescription(constructor);
         Class[] params = constructor.getParameterTypes();
         MBeanParameterInfo[] paramsInfo = new MBeanParameterInfo[params.length];
         for (int j = 0; j < params.length; ++j)
         {
            Class param = params[j];
            String paramName = descrs == null ? null : descrs.getConstructorParameterName(constructor, j);
            String paramDescr = descrs == null ? null : descrs.getConstructorParameterDescription(constructor, j);
            paramsInfo[j] = new MBeanParameterInfo(paramName, param.getName(), paramDescr);
         }

         String ctorName = constructor.getName();
         MBeanConstructorInfo info = new MBeanConstructorInfo(ctorName.substring(ctorName.lastIndexOf('.') + 1), descr, paramsInfo);
         constructors[i] = info;
      }
      return constructors;
   }

   private boolean implementsMBean(String clsName, String intfName)
   {
      if (intfName.equals(clsName + "MBean")) return true;

      if (m_useExtendedMBeanInterfaces)
      {
         // Check also that the may be in different packages and/or inner classes

         // Trim packages
         int clsDot = clsName.lastIndexOf('.');
         if (clsDot > 0) clsName = clsName.substring(clsDot + 1);
         int intfDot = intfName.lastIndexOf('.');
         if (intfDot > 0) intfName = intfName.substring(intfDot + 1);
         // Try again
         if (intfName.equals(clsName + "MBean")) return true;

         // Trim inner classes
         int clsDollar = clsName.lastIndexOf('$');
         if (clsDollar > 0) clsName = clsName.substring(clsDollar + 1);
         int intfDollar = intfName.lastIndexOf('$');
         if (intfDollar > 0) intfName = intfName.substring(intfDollar + 1);
         // Try again
         if (intfName.equals(clsName + "MBean")) return true;
      }

      // Give up
      return false;
   }

   private MBeanInvoker createInvoker(MBeanMetaData metadata)
   {
      Logger logger = getLogger();

      if (m_customMBeanInvoker != null)
      {
         if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Custom MBeanInvoker class is: " + m_customMBeanInvoker);
         try
         {
            MBeanInvoker mbeanInvoker = (MBeanInvoker)Thread.currentThread().getContextClassLoader().loadClass(m_customMBeanInvoker).newInstance();
            if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Using custom MBeanInvoker: " + mbeanInvoker);
            return mbeanInvoker;
         }
         catch (Exception x)
         {
            if (logger.isEnabledFor(Logger.DEBUG)) logger.debug("Cannot instantiate custom MBeanInvoker, using default", x);
         }
      }

/* SFR
      if (m_bcelClassesAvailable)
      {
         MBeanInvoker mbeanInvoker = BCELMBeanInvoker.create(metadata);
         if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Using default BCEL MBeanInvoker for MBean " + metadata.name + ", " + mbeanInvoker);
         return mbeanInvoker;
      }
      else
      {
*/
         MBeanInvoker mbeanInvoker = new ReflectedMBeanInvoker();
         if (logger.isEnabledFor(Logger.TRACE)) logger.trace("Using default Reflection MBeanInvoker for MBean " + metadata.name + ", " + mbeanInvoker);
         return mbeanInvoker;
/* SFR     } */
   }

   private Logger getLogger()
   {
      return Log.getLogger(getClass().getName());
   }
}
