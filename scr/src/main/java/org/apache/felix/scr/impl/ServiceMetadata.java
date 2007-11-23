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
package org.apache.felix.scr.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.osgi.service.component.ComponentException;

/**
 * This class contains the metadata associated to a service that is provided
 * by a component
 *
 */
public class ServiceMetadata {
	
	// 112.4.6 Flag that indicates if the service is a ServiceFactory 
	private boolean m_serviceFactory = false;
	
	// List of provided interfaces
	private List m_provides = new ArrayList();
	
	// Flag that indicates if this metadata has been validated and has become immutable
	private boolean m_validated = false;
	
	/**
	 * Setter for the servicefactory attribute of the service element
	 * 
	 * @param serviceFactory
	 */
	public void setServiceFactory(boolean serviceFactory) {
		if(m_validated) {
			return;
		}			
		
		m_serviceFactory = serviceFactory;
	}
	
	/**
	 * Add a provided interface to this service
	 * 
	 * @param provide a String containing the name of the provided interface
	 */
	public void addProvide(String provide) {
		if(m_validated) {
			return;
		}			

		m_provides.add(provide);
	}
	
	/**
	 * Return the flag that defines if it is a service factory or not
	 * 
	 * @return a boolean flag
	 */
	public boolean isServiceFactory() {
		return m_serviceFactory; 
	}
	
	/**
     * Returns the implemented interfaces
     *
     * @return the implemented interfaces as a string array
     */
    public String [] getProvides() {
        String provides[] = new String[m_provides.size()];
        Iterator it = m_provides.iterator();
        int count = 0;
        while (it.hasNext())
        {
            provides[count++] = it.next().toString();
        }
        return provides;
    }
    
    /**
     * Verify if the semantics of this metadata are correct
     *
     */
    void validate() {
    	if(m_provides.size() == 0) {
    		throw new ComponentException("At least one provided interface must be given");
    	}
    }
}
