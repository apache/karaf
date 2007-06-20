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

import javax.management.MBeanInfo;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

/**
 * Objects of this class hold metadata information about MBeans.
 *
 * @author <a href="mailto:biorn_steedom@users.sourceforge.net">Simone Bordet</a>
 * @version $Revision: 1.1.1.1 $
 */
public class MBeanMetaData
{
	/**
	 * The MBean instance.
	 */
	public Object mbean;

	/**
	 * The classloader of the MBean
	 */
	public ClassLoader classloader;

	/**
	 * The ObjectInstance of the MBean
	 */
	public ObjectInstance instance;

	/**
	 * The ObjectName of the MBean
	 */
	public ObjectName name;

	/**
	 * The MBeanInfo of the MBean
	 */
	public MBeanInfo info;

	/**
	 * True if the MBean is dynamic
	 */
	public boolean dynamic;

	/**
	 * True if the MBean is standard
	 */
	public boolean standard;

	/**
	 * The management interface of the MBean, if it is a standard MBean
	 */
	public Class management;

	/**
	 * The invoker for the MBean, if it is a standard MBean
	 */
	public MBeanInvoker invoker;
}
