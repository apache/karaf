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


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.InvalidAttributeValueException;
import javax.management.JMRuntimeException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;

import org.apache.felix.mosgi.jmx.agent.mx4j.ImplementationException;
import org.apache.felix.mosgi.jmx.agent.mx4j.util.Utils;
import org.apache.felix.mosgi.jmx.agent.mx4j.util.MethodTernaryTree;

/**
 * MBeanInvoker that uses reflection to invoke on MBean instances.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class ReflectedMBeanInvoker implements MBeanInvoker
{
   private static final String[] EMPTY_PARAMS = new String[0];
   private static final Object[] EMPTY_ARGS = new Object[0];

   private final Map attributes = new HashMap();
   private final Map attributeNames = new HashMap();
   private final MethodTernaryTree operations = new MethodTernaryTree();
   private final MethodTernaryTree methods = new MethodTernaryTree();

   public Object invoke(MBeanMetaData metadata, String method, String[] params, Object[] args) throws MBeanException, ReflectionException
   {
      MBeanOperationInfo oper = getStandardOperationInfo(metadata, method, params);
      if (oper != null)
      {
         try
         {
            return invokeImpl(metadata, method, params, args);
         }
         catch (IllegalArgumentException x)
         {
            throw new RuntimeOperationsException(x);
         }
      }
      else
      {
         throw new ReflectionException(new NoSuchMethodException("Operation " + method + " does not belong to the management interface"));
      }
   }

   public Object getAttribute(MBeanMetaData metadata, String attribute) throws MBeanException, AttributeNotFoundException, ReflectionException
   {
      MBeanAttributeInfo attr = getStandardAttributeInfo(metadata, attribute, false);
      if (attr != null)
      {
         String attributeName = getAttributeName(attr, true);
         try
         {
            return invokeImpl(metadata, attributeName, EMPTY_PARAMS, EMPTY_ARGS);
         }
         catch (IllegalArgumentException x)
         {
            // Never thrown, since there are no arguments
            throw new ImplementationException();
         }
      }
      else
      {
         throw new AttributeNotFoundException(attribute);
      }
   }

   public void setAttribute(MBeanMetaData metadata, Attribute attribute) throws MBeanException, AttributeNotFoundException, InvalidAttributeValueException, ReflectionException
   {
      String name = attribute.getName();
      MBeanAttributeInfo attr = getStandardAttributeInfo(metadata, name, true);
      if (attr != null)
      {
         String attributeName = getAttributeName(attr, false);
         try
         {
            invokeImpl(metadata, attributeName, new String[]{attr.getType()}, new Object[]{attribute.getValue()});
         }
         catch (IllegalArgumentException x)
         {
            throw new InvalidAttributeValueException("Invalid value for attribute " + name + ": " + attribute.getValue());
         }
      }
      else
      {
         throw new AttributeNotFoundException(name);
      }
   }

   protected Object invokeImpl(MBeanMetaData metadata, String method, String[] signature, Object[] args) throws ReflectionException, MBeanException, IllegalArgumentException
   {
      Method m = getStandardManagementMethod(metadata, method, signature);

      try
      {
         return m.invoke(metadata.mbean, args);
      }
      catch (IllegalAccessException x)
      {
         throw new ReflectionException(x);
      }
      catch (InvocationTargetException x)
      {
         Throwable t = x.getTargetException();
         if (t instanceof Error) throw new RuntimeErrorException((Error)t);
         if (t instanceof JMRuntimeException) throw (JMRuntimeException)t;
         if (t instanceof RuntimeException) throw new RuntimeMBeanException((RuntimeException)t);
         throw new MBeanException((Exception)t);
      }
   }

   private MBeanAttributeInfo getStandardAttributeInfo(MBeanMetaData metadata, String attribute, boolean isWritable)
   {
      MBeanAttributeInfo attr = null;
      synchronized (attributes)
      {
         attr = (MBeanAttributeInfo)attributes.get(attribute);
      }
      if (attr != null)
      {
         if (isWritable && attr.isWritable()) return attr;
         if (!isWritable && attr.isReadable()) return attr;
      }
      else
      {
         MBeanAttributeInfo[] attrs = metadata.info.getAttributes();
         if (attrs != null)
         {
            for (int i = 0; i < attrs.length; ++i)
            {
               attr = attrs[i];
               String name = attr.getName();
               if (attribute.equals(name))
               {
                  synchronized (attributes)
                  {
                     attributes.put(attribute, attr);
                  }
                  if (isWritable && attr.isWritable()) return attr;
                  if (!isWritable && attr.isReadable()) return attr;
               }
            }
         }
      }
      return null;
   }

   private MBeanOperationInfo getStandardOperationInfo(MBeanMetaData metadata, String method, String[] signature)
   {
      MBeanOperationInfo oper = null;

      synchronized (operations)
      {
         oper = (MBeanOperationInfo)operations.get(method, signature);
      }

      if (oper != null) return oper;

      // The MBeanOperationInfo is not in the cache, look it up
      MBeanInfo info = metadata.info;
      MBeanOperationInfo[] opers = info.getOperations();
      if (opers != null)
      {
         for (int i = 0; i < opers.length; ++i)
         {
            oper = opers[i];
            String name = oper.getName();
            if (method.equals(name))
            {
               // Same method name, check number of parameters
               MBeanParameterInfo[] params = oper.getSignature();
               if (signature.length == params.length)
               {
                  boolean match = true;
                  for (int j = 0; j < params.length; ++j)
                  {
                     MBeanParameterInfo param = params[j];
                     if (!signature[j].equals(param.getType()))
                     {
                        match = false;
                        break;
                     }
                  }
                  if (match)
                  {
                     synchronized (operations)
                     {
                        operations.put(method, signature, oper);
                     }
                     return oper;
                  }
               }
            }
         }
      }
      return null;
   }

   private Method getStandardManagementMethod(MBeanMetaData metadata, String name, String[] signature) throws ReflectionException
   {
      Method method = null;
      synchronized (methods)
      {
         method = (Method)methods.get(name, signature);
      }
      if (method != null) return method;

      // Method is not in cache, look it up
      try
      {
         Class[] params = Utils.loadClasses(metadata.classloader, signature);
         method = metadata.mbean.getClass().getMethod(name, params);
         synchronized (methods)
         {
            methods.put(name, signature, method);
         }
         return method;
      }
      catch (ClassNotFoundException x)
      {
         throw new ReflectionException(x);
      }
      catch (NoSuchMethodException x)
      {
         throw new ReflectionException(x);
      }
   }

   private String getAttributeName(MBeanAttributeInfo attribute, boolean getter)
   {
      AttributeName attributeName = null;
      String name = attribute.getName();
      synchronized (attributeNames)
      {
         attributeName = (AttributeName)attributeNames.get(name);
      }
      if (attributeName == null)
      {
         String prefix = attribute.isIs() ? "is" : "get";
         attributeName = new AttributeName(prefix + name, "set" + name);
         synchronized (attributeNames)
         {
            attributeNames.put(name, attributeName);
         }
      }

      if (getter) return attributeName.getter;
      return attributeName.setter;
   }

   private static class AttributeName
   {
      private final String getter;
      private final String setter;

      public AttributeName(String getter, String setter)
      {
         this.getter = getter;
         this.setter = setter;
      }
   }
}
