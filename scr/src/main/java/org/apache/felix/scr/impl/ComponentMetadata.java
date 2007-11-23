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
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

import org.osgi.service.component.ComponentException;

/**
 * This class holds the information associated to a component in the descriptor *  */
public class ComponentMetadata {
	// 112.4.3: A Globally unique component name (required)
	private String m_name;
	
	// 112.4.3: Controls whether the component is enabled when the bundle is started. (optional, default is true).
	private boolean m_enabled = true;
	
	// 112.4.3: Factory identified. If set to a non empty string, it indicates that the component is a factory component (optional).
	private String m_factory = null;
	
	// 112.4.3: Controls whether component configurations must be immediately activated after becoming 
	// satisfied or whether activation should be delayed. (optional, default value depends
	// on whether the component has a service element or not).
	private Boolean m_immediate = null;
	
    // 112.4.4 Implementation Element (required)
    private String m_implementationClassName = null;
    
    // Associated properties (0..*)
    private Dictionary m_properties = new Hashtable();
    
    // List of Property metadata - used while building the meta data
    // while validating the properties contained in the PropertyMetadata
    // instances are copied to the m_properties Dictionary while this
    // list will be cleared
    private List m_propertyMetaData = new ArrayList();
    
    // Provided services (0..1)
    private ServiceMetadata m_service = null;
    
    // List of service references, (required services 0..*)
    private List m_references = new ArrayList();
    
    // Flag that is set once the component is verified (its properties cannot be changed)
    private boolean m_validated = false;
        
       
    /////////////////////////////////////////// SETTERS //////////////////////////////////////
    
    /**
     * Setter for the name
     * 
     * @param name
     */
    public void setName(String name) {
    	if(m_validated) {
    		return;
    	}
    	m_name = name;
    }
    
    /**
     * Setter for the enabled property
     * 
     * @param enabled
     */
    public void setEnabled(boolean enabled) {
    	if(m_validated) {
    		return;
    	}
    	m_enabled = enabled;
    }
    
    /**
     * 
     * @param factoryIdentifier
     */
    public void setFactoryIdentifier(String factoryIdentifier) {
    	if(m_validated) {
    		return;
    	}
    	m_factory = factoryIdentifier;
    }
    
    /**
     * Setter for the immediate property
     * 
     * @param immediate
     */
    public void setImmediate(boolean immediate) {
    	if(m_validated) {
    		return;
    	}
    	m_immediate = immediate ? Boolean.TRUE : Boolean.FALSE;
    }   
    
    /**
     * Sets the name of the implementation class
     * 
     * @param implementationClassName a class name
     */
    public void setImplementationClassName(String implementationClassName) {
    	if(m_validated) {
    		return;
    	}
        m_implementationClassName = implementationClassName;
    }

    /**
     * Used to add a property to the instance
     *
     * @param newProperty a property metadata object
     */
    public void addProperty(PropertyMetadata newProperty) {
    	if(m_validated) {
    		return;
    	}
    	if(newProperty == null) {
    		throw new IllegalArgumentException ("Cannot add a null property");
    	}
    	m_propertyMetaData.add(newProperty);
    }

    /**
     * Used to set a ServiceMetadata object.
     *
     * @param service a ServiceMetadata
     */
    public void setService(ServiceMetadata service) {
    	if(m_validated) {
    		return;
    	}
        m_service = service;
    }

    /**
     * Used to add a reference metadata to the component
     *
     * @param newReference a new ReferenceMetadata to be added
     */
    public void addDependency(ReferenceMetadata newReference) {
    	if(newReference == null) {
    		throw new IllegalArgumentException ("Cannot add a null ReferenceMetadata");
    	}
        m_references.add(newReference);
    }

    
    /////////////////////////////////////////// GETTERS //////////////////////////////////////
    
    /**
     * Returns the name of the component
     * 
     * @return A string containing the name of the component 
     */
    public String getName() {
    	return m_name;
    }
    
    /**
     * Returns the value of the enabled flag 
     * 
     * @return a boolean containing the value of the enabled flag
     */
    public boolean isEnabled() {
    	return m_enabled;
    }

    /**
     * Returns the factory identifier
     * 
     * @return A string containing a factory identifier or null
     */
    public String getFactoryIdentifier() {
    	return m_factory;
    }
    
    /**
     * Returns the flag that defines the activation policy for the component.
     * <p>
     * This method may only be trusted after this instance has been validated
     * by the {@link #validate()} call. Else it will either return the value
     * of an explicitly set "immediate" attribute or return false if a service
     * element is set or true otherwise. This latter default value deduction
     * may be unsafe while the descriptor has not been completely read.
     * 
     * 
     * @return a boolean that defines the activation policy
     */
    public boolean isImmediate() {
        // return explicit value if known
        if ( m_immediate != null ) {
            return m_immediate.booleanValue();
        }

        // deduce default value from service element presence
        return m_service == null;
    }
    
    /**
     * Returns the name of the implementation class
     *
     * @return the name of the implementation class
     */
    public String getImplementationClassName() {
        return m_implementationClassName;
    }

    /**
     * Returns the associated ServiceMetadata
     * 
     * @return a ServiceMetadata object or null if the Component does not provide any service
     */
    public ServiceMetadata getServiceMetadata() {
    	return m_service;
    }

    /**
     * Returns the properties.
     *
     * @return the properties as a Dictionary
     */
    public Dictionary getProperties() {
        return m_properties;
    }

    /**
     * Returns the dependency descriptors
     *
     * @return a Collection of dependency descriptors
     */
    public List getDependencies() {
        return m_references;
    }

    /**
     * Test to see if this service is a factory
     *
     * @return true if it is a factory, false otherwise
     */
    public boolean isFactory() {
        return m_factory != null;
    }
    
    /**
     * Method used to verify if the semantics of this metadata are correct
     */
    void validate() {
    	
        // First check if the properties are valid (and extract property values)
        Iterator propertyIterator = m_propertyMetaData.iterator();
    	while ( propertyIterator.hasNext() ) {
    	    PropertyMetadata propMeta = (PropertyMetadata) propertyIterator.next();
            propMeta.validate();
            m_properties.put(propMeta.getName(), propMeta.getValue());
        }
    	m_propertyMetaData.clear();
    	
    	// Check that the provided services are valid too
    	if(m_service != null) {
    		m_service.validate();
    	}
    	
    	// Check that the references are ok
    	Iterator referenceIterator = m_references.iterator();
    	while ( referenceIterator.hasNext() ) {
    		((ReferenceMetadata)referenceIterator.next()).validate();
    	}
    	    	
    	// 112.10 The name of the component is required
    	if( m_name == null ) {
    		throw new ComponentException("The component name has not been set");
    	}
    	
    	// 112.10 There must be one implementation element and the class atribute is required
    	if ( m_implementationClassName == null ) {
    		throw new ComponentException("The implementation class name has not been set for this component");
    	}
    	
    	// 112.2.3 A delayed component specifies a service, is not specified to be a factory component
    	// and does not have the immediate attribute of the component element set to true.
    	if ( m_immediate != null && isImmediate() == false && m_service == null ) {
            throw new ComponentException( "Component '" + m_name
                + "' is specified as being delayed but does not provide any service." );
        }    	
    	
    	if ( m_factory != null && isImmediate() == false) {
    		throw new ComponentException("A factory cannot be a delayed component");
    	}
    	
    	// 112.4.6 The serviceFactory attribute (of a provided service) must not be true if 
    	// the component is a factory component or an immediate component
    	if ( m_service != null ) {
            if ( m_service.isServiceFactory() && ( isFactory() || isImmediate() ) ) {
                throw new ComponentException( "A ServiceFactory service cannot be a factory or immediate component" );
            }
        }

    	
    	m_validated = true;
    	// TODO: put a similar flag on the references and the services
    }

}
