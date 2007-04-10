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
package org.apache.felix.ipojo;

import java.util.Dictionary;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A service context give the access the a service registry.
 * All service interaction should use this service context.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public interface ServiceContext {
	
	/**
	 * @param listener
	 * @param filter
	 * @throws InvalidSyntaxException
	 */
	void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException;
	
	/**
	 * @param listener
	 */
	void addServiceListener(ServiceListener listener);
	
	/**
	 * @param clazz
	 * @param filter
	 * @return
	 * @throws InvalidSyntaxException
	 */
	ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException;
	
	/**
	 * @param reference
	 * @return
	 */
	Object getService(ServiceReference reference);
	
	/**
	 * @param clazz
	 * @return
	 */
	ServiceReference getServiceReference(String clazz);
	
	/**
	 * @param clazz
	 * @param filter
	 * @return
	 * @throws InvalidSyntaxException
	 */
	ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException;
	
	/**
	 * @param clazzes
	 * @param service
	 * @param properties
	 * @return
	 */
	ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties);
	
	/**
	 * @param clazz
	 * @param service
	 * @param properties
	 * @return
	 */
	ServiceRegistration registerService(String clazz, Object service, Dictionary properties);
	
	/**
	 * @param listener
	 */
	void removeServiceListener(ServiceListener listener);
	
	/**
	 * @param reference
	 * @return
	 */
	boolean ungetService(ServiceReference reference);
	
	/**
	 * @return the component instance who use this service context.
	 */
	ComponentInstance getComponentInstance();
	
	/**
	 * Set the component instance using the service context.
	 * @param ci
	 */
	//void setComponentInstance(ComponentInstance ci);

}
