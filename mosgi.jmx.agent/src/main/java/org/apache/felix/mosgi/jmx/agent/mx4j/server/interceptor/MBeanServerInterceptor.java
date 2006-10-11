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


import java.util.List;

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
 * MBeanServer --&gt; MBean interceptor.
 * These interceptors are used internally to implement MBeanServer functionality prior to call
 * MBeans, and can be used to customize MBeanServer implementation by users.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface MBeanServerInterceptor
{
   /**
    * Constant used to specify the status of the MBean registration in {@link #registration}
    */
	public static final int PRE_REGISTER = 1;
   /**
    * Constant used to specify the status of the MBean registration in {@link #registration}
    */
	public static final int POST_REGISTER_TRUE = 2;
   /**
    * Constant used to specify the status of the MBean registration in {@link #registration}
    */
	public static final int POST_REGISTER_FALSE = 3;
   /**
    * Constant used to specify the status of the MBean registration in {@link #registration}
    */
	public static final int PRE_DEREGISTER = 4;
   /**
    * Constant used to specify the status of the MBean registration in {@link #registration}
    */
	public static final int POST_DEREGISTER = 5;

	/**
	 * A concise string that tells the type of this interceptor
	 */
	public String getType();

   /**
    * Sets the chain of interceptors on this interceptor. This interceptor will use this list to
    * find the interceptor in the chain after itself
    * @param interceptors The list of interceptors
    */
	public void setChain(List interceptors);

   /**
    * Adds the given notification listener to the MBean, along with the given filter and handback
    */
	public void addNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback);

   /**
    * Removes the given notification listener from the MBean.
    */
	public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener) throws ListenerNotFoundException;
   /**
    * Removes the given notification listener from the MBean, specified by the given filter and handback.
    */
   public void removeNotificationListener(MBeanMetaData metadata, NotificationListener listener, NotificationFilter filter, Object handback) throws ListenerNotFoundException;

   /**
    * Instantiate the given className passing the given arguments to the constructor with the given signature
    */
	public void instantiate(MBeanMetaData metadata, String className, String[] params, Object[] args) throws ReflectionException, MBeanException;

   /**
    * Calls the specified {@link javax.management.MBeanRegistration} method on the MBean instance.
    */
	public void registration(MBeanMetaData metadata, int operation) throws MBeanRegistrationException;

   /**
    * Calls getMBeanInfo on the MBean instance (only on DynamicMBeans).
    */
	public MBeanInfo getMBeanInfo(MBeanMetaData metadata);

   /**
    * Invokes the specified MBean operation on the MBean instance
    */
	public Object invoke(MBeanMetaData metadata, String method, String[] params, Object[] args) throws MBeanException, ReflectionException;

   /**
    * Gets the specified attributes values from the MBean instance.
    */
	public AttributeList getAttributes(MBeanMetaData metadata, String[] attributes);

   /**
    * Sets the specified attributes values on the MBean instance.
    */
	public AttributeList setAttributes(MBeanMetaData metadata, AttributeList attributes);

   /**
    * Gets the specified attribute value from the MBean instance.
    */
	public Object getAttribute(MBeanMetaData metadata, String attribute) throws MBeanException, AttributeNotFoundException, ReflectionException;

   /**
    * Sets the specified attribute value on the MBean instance.
    */
	public void setAttribute(MBeanMetaData metadata, Attribute attribute) throws MBeanException, AttributeNotFoundException, InvalidAttributeValueException, ReflectionException;
}
