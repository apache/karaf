/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.handlers.dependency;

/**
 * Dependency Metadata.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class DependencyMetadata {

	/**
	 * Service Specification <=> Service contract (or interface in OSGi).
	 */
	private String m_serviceSpecification;

	/**
	 * LDAP filter.
	 */
	private String m_filter;

	/**
	 * is the dependency optional ?
	 */
	private boolean m_isOptional;

	/**
	 * is the dependency multiple ?
	 */
	private boolean m_isMultiple = false;

	/**
	 * List of dependency callbacks attached to this dependency.
	 */
	private DependencyCallback[] m_callbacks = new DependencyCallback[0];

	/**
	 * The field linking the metadata and the component implementation.
	 */
	private String m_field;

	// Constructor

	/**
     * Constructor.
	 * @param field : the field name
	 * @param service : the interface name.
	 * @param filter : the filter of the dependency
	 * @param isOptional : is the dependency optional
	 */
	public DependencyMetadata(String field, String service, String filter, boolean isOptional) {
		m_field = field;
		m_serviceSpecification = service;
		m_isOptional = isOptional;
		m_filter = filter;
	}

	/**
	 * Add a callback to the dependency.
	 * @param cb : callback to add
	 */
	public void addDependencyCallback(DependencyCallback cb) {
		 for (int i = 0; (m_callbacks != null) && (i < m_callbacks.length); i++) {
	            if (m_callbacks[i] == cb) { return; }
	        }

	        if (m_callbacks.length > 0) {
	        	DependencyCallback[] newCallbacks = new DependencyCallback[m_callbacks.length + 1];
	            System.arraycopy(m_callbacks, 0, newCallbacks, 0, m_callbacks.length);
	            newCallbacks[m_callbacks.length] = cb;
	            m_callbacks = newCallbacks;
	        }
	        else {
	        	m_callbacks = new DependencyCallback[] {cb};
	        }
	}

	// Getter

	/**
	 * @return Returns the m_field.
	 */
	public String getField() {
		return m_field;
	}

	/**
	 * @return Returns the m_filter.
	 */
	public String getFilter() {
		return m_filter;
	}

	/**
	 * @return Returns the m_isMultiple.
	 */
	public boolean isMultiple() {
		return m_isMultiple;
	}

	/**
	 * @return Returns the m_isOptional.
	 */
	public boolean isOptional() {
		return m_isOptional;
	}

	/**
	 * @return Returns the m_serviceSpecification.
	 */
	public String getServiceSpecification() {
		return m_serviceSpecification;
	}

	/**
	 * return true if the dependency is multiple.
	 */
	public void setMultiple() {
		m_isMultiple = true;
	}

	/**
     * Set the service specification.
	 * @param serviceSpecification : the dependency service specification
	 */
	public void setServiceSpecification(String serviceSpecification) {
		m_serviceSpecification = serviceSpecification;
	}

	/**
	 * @return the list of the callbacks
	 */
	public DependencyCallback[] getCallbacks() { return m_callbacks; }

}
