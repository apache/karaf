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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Implement this inteface to give descriptions to standard MBean. <p>
 * The MX4J implementation will look, for every standard MBean, for a class with name composed by
 * the fully qualified MBean class name + "MBeanDescription".
 * If such a class is found, the MX4J implementation will call its methods to retrieve description
 * information about the MBean itself.
 * MBean descriptions are built-in in DynamicMBean, but not in standard MBeans.
 * The <a href="http://xdoclet.sourceforge.net">XDoclet</a>  tool is used to automate the process of
 * generating the MBeanDescription classes for a given MBean, along with the MBean interface.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public interface MBeanDescription
{
   /**
    * Should return the description of the MBean.
    * For example: "This MBean is the rmiregistry service"
    */
	public String getMBeanDescription();
   /**
    * Should return the description for the given constructor of the MBean.
    * For example: "Creates an rmiregistry instance on the specified port"
    */
	public String getConstructorDescription(Constructor ctor);
   /**
    * Should return the name of the constructor's parameter for the given constructor and parameter index.
    * For example: "port"
    */
	public String getConstructorParameterName(Constructor ctor, int index);
   /**
    * Should return the description for the constructor's parameter for the given constructor and parameter index.
    * For example: "The port on which the rmiregistry will wait on for client requests"
    */
	public String getConstructorParameterDescription(Constructor ctor, int index);
   /**
    * Should return the description for the specified attribute.
    * For example: "The port on which the rmiregistry will wait on for client requests"
    */
	public String getAttributeDescription(String attribute);
   /**
    * Should return the description for the specified operation.
    * For example: "Binds the given object to the given name"
    */
	public String getOperationDescription(Method operation);
   /**
    * Should return the name of the operation's parameter for the given operation and parameter index.
    * For example: "bindName"
    */
	public String getOperationParameterName(Method method, int index);
   /**
    * Should return the description for the operations's parameter for the given operation and parameter index.
    * For example: "The name to which the object will be bound to"
    */
	public String getOperationParameterDescription(Method method, int index);
}
