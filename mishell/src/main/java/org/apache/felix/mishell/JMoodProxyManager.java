/* 
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.mishell;

import java.util.List;

import javax.management.MBeanServerConnection;

import org.apache.felix.jmxintrospector.MBean;
import org.apache.felix.jmxintrospector.MBeanProxyManager;

/**
 * This class extends the MBeanProxyManager to include methods that are aware that they are connecting
 * to an MBeanServer that has JMood mbeans. This is the most natural place to add some utility methods
 * for script engines, since scripts will have a JMoodProxyManager named 'manager' as the entry point.  
 *
 */
public class JMoodProxyManager extends MBeanProxyManager {
	public List<Object> getControllers(){
		return findAll("type=controller");
	}
	public List<Object> getBundlesAt(Object controller){
		MBeanServerConnection server=((MBean)controller).getMBeanServer();
		return findAll("type=bundle", server);
	}
	public List<Object> getServicesAt(Object controller){
		MBeanServerConnection server=((MBean)controller).getMBeanServer();
		return findAll("type=service", server);
	}
	public List<Object> getPackagesAt(Object controller){
		MBeanServerConnection server=((MBean)controller).getMBeanServer();
		return findAll("type=package", server);
	}
	
}
