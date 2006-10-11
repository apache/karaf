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
package org.apache.felix.mosgi.jmx.agent.mx4j;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import javax.management.DynamicMBean;
import javax.management.AttributeNotFoundException;
import javax.management.MBeanException;
import javax.management.ReflectionException;
import javax.management.AttributeList;
import javax.management.MBeanInfo;
import javax.management.Attribute;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.RuntimeErrorException;
import javax.management.MBeanParameterInfo;
import javax.management.RuntimeMBeanException;

import org.apache.felix.mosgi.jmx.agent.mx4j.util.Utils;

/**
 * Utility class that allow the user to easily write DynamicMBeans. <br>
 * By extending this class, the developer does not have to implement the methods of the DynamicMBean interface, but
 * has instead to provide only the metadata (by overriding few methods) and the implementation (by implementing
 * the methods) of the MBean itself. <br>
 * The methods to override that provides metadata information are usually the following:
 * <ul>
 * <li> <code>createMBeanAttributeInfo</code>, if the MBeans has manageable attributes </li>
 * <li> <code>createMBeanOperationInfo</code>, if the MBeans has manageable operations </li>
 * <li> <code>createMBeanNotificationInfo</code>, if the MBeans has manageable notifications </li>
 * <li> <code>createMBeanConstructorInfo</code>, if the MBeans has manageable constructors </li>
 * <li> <code>getMBeanDescription</code> </li>
 * </ul>
 * For example, the following MBean only has one manageable attribute:
 * <pre>
 * public class SimpleDynamic extends AbstractDynamicMBean
 * {
 *    protected MBeanAttributeInfo[] createMBeanAttributeInfo()
 *    {
 *       return new MBeanAttributeInfo[]
 *       {
 *          new MBeanAttributeInfo("Name", String.class.getName(), "The name", true, true, false)
 *       };
 *    }
 *
 *    protected String getMBeanDescription()
 *    {
 *       return "A simple DynamicMBean";
 *    }
 *
 *    public String getName() { ... }
 *
 *    public void setName(String name) { ... }
 * }
 * </pre>
 * It is responsibility of the developer to specify the metadata <b>and</b> implement the methods specified by the
 * metadata, that will be invoked via reflection by the AbstractDynamicMBean class. For this reason, the methods
 * belonging to the MBean implementation (in the case above <code>getName()</code> and <code>setName(...)</code>)
 * must be public.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public abstract class AbstractDynamicMBean implements DynamicMBean
{
   private MBeanInfo info;
   private Object resource;

   /**
    * Only subclasses can create a new instance of an AbstractDynamicMBean.
    * @see #createMBeanConstructorInfo
    */
   protected AbstractDynamicMBean()
   {
   }

   /**
    * Returns the value of the manageable attribute, as specified by the DynamicMBean interface.
    * @see #createMBeanAttributeInfo
    */
   public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException
   {
      if (attribute == null) throw new AttributeNotFoundException("Attribute " + attribute + " not found");

      Object resource = null;
      MBeanInfo info = null;
      synchronized (this)
      {
         resource = getResourceOrThis();
         info = getMBeanInfo();
      }

      MBeanAttributeInfo[] attrs = info.getAttributes();
      if (attrs == null || attrs.length == 0) throw new AttributeNotFoundException("No attributes defined for this MBean");

      for (int i = 0; i < attrs.length; ++i)
      {
         MBeanAttributeInfo attr = attrs[i];
         if (attr == null) continue;

         if (attribute.equals(attr.getName()))
         {
            if (!attr.isReadable()) throw new ReflectionException(new NoSuchMethodException("No getter defined for attribute: " + attribute));

            // Found, invoke via reflection
            String prefix = null;
            if (attr.isIs())
               prefix = "is";
            else
               prefix = "get";

            try
            {
               return invoke(resource, prefix + attr.getName(), new Class[0], new Object[0]);
            }
            catch (InvalidAttributeValueException x)
            {
               throw new ReflectionException(x);
            }
         }
      }

      throw new AttributeNotFoundException("Attribute " + attribute + " not found");
   }

   /**
    * Returns the manageable attributes, as specified by the DynamicMBean interface.
    */
   public AttributeList getAttributes(String[] attributes)
   {
      AttributeList list = new AttributeList();

      if (attributes != null)
      {
         for (int i = 0; i < attributes.length; ++i)
         {
            String attribute = attributes[i];
            try
            {
               Object result = getAttribute(attribute);
               list.add(new Attribute(attribute, result));
            }
            catch (AttributeNotFoundException ignored)
            {
            }
            catch (MBeanException ignored)
            {
            }
            catch (ReflectionException ignored)
            {
            }
         }
      }

      return list;
   }

   /**
    * Returns the MBeaInfo, as specified by the DynamicMBean interface; the default implementation caches the value
    * returned by {@link #createMBeanInfo} (that is thus called only once).
    * @see #createMBeanInfo
    * @see #setMBeanInfo
    */
   public synchronized MBeanInfo getMBeanInfo()
   {
      if (info == null) setMBeanInfo(createMBeanInfo());
      return info;
   }

   /**
    * Returns the value of the manageable operation as specified by the DynamicMBean interface
    * @see #createMBeanOperationInfo
    */
   public Object invoke(String method, Object[] arguments, String[] params) throws MBeanException, ReflectionException
   {
      if (method == null) throw new IllegalArgumentException("Method name cannot be null");
      if (arguments == null) arguments = new Object[0];
      if (params == null) params = new String[0];

      Object resource = null;
      MBeanInfo info = null;
      synchronized (this)
      {
         resource = getResourceOrThis();
         info = getMBeanInfo();
      }

      MBeanOperationInfo[] opers = info.getOperations();
      if (opers == null || opers.length == 0) throw new ReflectionException(new NoSuchMethodException("No operations defined for this MBean"));

      for (int i = 0; i < opers.length; ++i)
      {
         MBeanOperationInfo oper = opers[i];
         if (oper == null) continue;

         if (method.equals(oper.getName()))
         {
            MBeanParameterInfo[] parameters = oper.getSignature();
            if (params.length != parameters.length) continue;

            String[] signature = new String[parameters.length];
            for (int j = 0; j < signature.length; ++j)
            {
               MBeanParameterInfo param = parameters[j];
               if (param == null)
                  signature[j] = null;
               else
                  signature[j] = param.getType();
            }

            if (Utils.arrayEquals(params, signature))
            {
               // Found the right operation
               try
               {
                  Class[] classes = Utils.loadClasses(resource.getClass().getClassLoader(), signature);
                  return invoke(resource, method, classes, arguments);
               }
               catch (ClassNotFoundException x)
               {
                  throw new ReflectionException(x);
               }
               catch (InvalidAttributeValueException x)
               {
                  throw new ReflectionException(x);
               }
            }
         }
      }

      throw new ReflectionException(new NoSuchMethodException("Operation " + method + " with signature " + Arrays.asList(params) + " is not defined for this MBean"));
   }

   /**
    * Sets the value of the manageable attribute, as specified by the DynamicMBean interface.
    * @see #createMBeanAttributeInfo
    */
   public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException
   {
      if (attribute == null) throw new AttributeNotFoundException("Attribute " + attribute + " not found");

      Object resource = null;
      MBeanInfo info = null;
      synchronized (this)
      {
         resource = getResourceOrThis();
         info = getMBeanInfo();
      }

      MBeanAttributeInfo[] attrs = info.getAttributes();
      if (attrs == null || attrs.length == 0) throw new AttributeNotFoundException("No attributes defined for this MBean");

      for (int i = 0; i < attrs.length; ++i)
      {
         MBeanAttributeInfo attr = attrs[i];
         if (attr == null) continue;

         if (attribute.getName().equals(attr.getName()))
         {
            if (!attr.isWritable()) throw new ReflectionException(new NoSuchMethodException("No setter defined for attribute: " + attribute));

            try
            {
               String signature = attr.getType();
               Class cls = Utils.loadClass(resource.getClass().getClassLoader(), signature);
               invoke(resource, "set" + attr.getName(), new Class[]{cls}, new Object[]{attribute.getValue()});
               return;
            }
            catch (ClassNotFoundException x)
            {
               throw new ReflectionException(x);
            }
         }
      }

      throw new AttributeNotFoundException("Attribute " + attribute + " not found");
   }

   /**
    * Sets the manageable attributes, as specified by the DynamicMBean interface.
    */
   public AttributeList setAttributes(AttributeList attributes)
   {
      AttributeList list = new AttributeList();

      if (attributes != null)
      {
         for (int i = 0; i < attributes.size(); ++i)
         {
            Attribute attribute = (Attribute)attributes.get(i);
            try
            {
               setAttribute(attribute);
               list.add(attribute);
            }
            catch (AttributeNotFoundException ignored)
            {
            }
            catch (InvalidAttributeValueException ignored)
            {
            }
            catch (MBeanException ignored)
            {
            }
            catch (ReflectionException ignored)
            {
            }
         }
      }

      return list;
   }

   /**
    * @deprecated Replaced by {@link #invoke(Object,String,Class[],Object[])}. <br>
    * The resource passed is the resource as set by {@link #setResource} or - if it is null - 'this' instance. <br>
    * This method is deprecated because it is not thread safe.
    */
   protected Object invoke(String name, Class[] params, Object[] args) throws InvalidAttributeValueException, MBeanException, ReflectionException
   {
      Object resource = getResourceOrThis();
      return invoke(resource, name, params, args);
   }

   /**
    * Looks up the method to call on given resource and invokes it.
    * The default implementation requires that the methods that implement attribute and operation behavior
    * on the resource object are public, but it is possible to override this behavior, and call
    * also private methods.
    * @see #findMethod
    * @see #invokeMethod
    */
   protected Object invoke(Object resource, String name, Class[] params, Object[] args) throws InvalidAttributeValueException, MBeanException, ReflectionException
   {
      try
      {
         Class cls = resource.getClass();
         Method method = findMethod(cls, name, params);
         return invokeMethod(method, resource, args);
      }
      catch (NoSuchMethodException x)
      {
         throw new ReflectionException(x);
      }
      catch (IllegalAccessException x)
      {
         throw new ReflectionException(x);
      }
      catch (IllegalArgumentException x)
      {
         throw new InvalidAttributeValueException(x.toString());
      }
      catch (InvocationTargetException x)
      {
         Throwable t = x.getTargetException();
         if (t instanceof RuntimeException)
            throw new RuntimeMBeanException((RuntimeException)t);
         else if (t instanceof Exception) throw new MBeanException((Exception)t);
         throw new RuntimeErrorException((Error)t);
      }
   }

   /**
    * Returns the (public) method with the given name and signature on the given class. <br>
    * Override to return non-public methods, or to map methods to other classes, or to map methods with
    * different signatures
    * @see #invoke(String, Class[], Object[])
    * @see #invokeMethod
    */
   protected Method findMethod(Class cls, String name, Class[] params) throws NoSuchMethodException
   {
      return cls.getMethod(name, params);
   }

   /**
    * Invokes the given method on the given resource object with the given arguments. <br>
    * Override to map methods to other objects, or to map methods with different arguments
    * @see #invoke(String, Class[], Object[])
    * @see #findMethod
    */
   protected Object invokeMethod(Method method, Object resource, Object[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException
   {
      return method.invoke(resource, args);
   }

   private Object getResourceOrThis()
   {
      Object resource = getResource();
      if (resource == null) resource = this;
      return resource;
   }

   /**
    * Returns the resource object on which invoke attribute's getters, attribute's setters and operation's methods
    * @see #setResource
    */
   protected synchronized Object getResource()
   {
      return resource;
   }

   /**
    * Specifies the resource object on which invoke attribute's getters, attribute's setters and operation's methods.
    * @see #getResource
    */
   public synchronized void setResource(Object resource)
   {
      this.resource = resource;
   }

   /**
    * Sets the MBeanInfo object cached by this instance. <br>
    * The given MBeanInfo is not cloned.
    * @see #getMBeanInfo
    */
   protected synchronized void setMBeanInfo(MBeanInfo info)
   {
      this.info = info;
   }

   /**
    * Creates the MBeanInfo for this instance, calling in succession factory methods that the user can override.
    * Information to create MBeanInfo are taken calling the following methods:
    * <ul>
    * <li><code>{@link #createMBeanAttributeInfo}</code></li>
    * <li><code>{@link #createMBeanConstructorInfo}</code></li>
    * <li><code>{@link #createMBeanOperationInfo}</code></li>
    * <li><code>{@link #createMBeanNotificationInfo}</code></li>
    * <li><code>{@link #getMBeanClassName}</code></li>
    * <li><code>{@link #getMBeanDescription}</code></li>
    * </ul>
    */
   protected MBeanInfo createMBeanInfo()
   {
      MBeanAttributeInfo[] attrs = createMBeanAttributeInfo();
      MBeanConstructorInfo[] ctors = createMBeanConstructorInfo();
      MBeanOperationInfo[] opers = createMBeanOperationInfo();
      MBeanNotificationInfo[] notifs = createMBeanNotificationInfo();
      String className = getMBeanClassName();
      String description = getMBeanDescription();
      return new MBeanInfo(className, description, attrs, ctors, opers, notifs);
   }

   /**
    * To be overridden to return metadata information about manageable attributes.
    */
   protected MBeanAttributeInfo[] createMBeanAttributeInfo()
   {
      return new MBeanAttributeInfo[0];
   }

   /**
    * To be overridden to return metadata information about manageable constructors.
    */
   protected MBeanConstructorInfo[] createMBeanConstructorInfo()
   {
      return new MBeanConstructorInfo[0];
   }

   /**
    * To be overridden to return metadata information about manageable operations.
    */
   protected MBeanOperationInfo[] createMBeanOperationInfo()
   {
      return new MBeanOperationInfo[0];
   }

   /**
    * To be overridden to return metadata information about manageable notifications.
    */
   protected MBeanNotificationInfo[] createMBeanNotificationInfo()
   {
      return new MBeanNotificationInfo[0];
   }

   /**
    * To be overridden to return metadata information about the class name of this MBean;
    * by default returns this class' name.
    */
   protected String getMBeanClassName()
   {
      return getClass().getName();
   }

   /**
    * To be overridden to return metadata information about the description of this MBean.
    */
   protected String getMBeanDescription()
   {
      return null;
   }
}
