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
package org.apache.felix.ipojo.handlers.providedService;


/**
 * Provided Service Metadata.
 * @author Clément Escoffier
 */
public class ProvidedServiceMetadata {

	 /**
     * Factory Policy : SINGLETON_FACTORY.
     */
    public static final int SINGLETON_FACTORY = 0;

    /**
     * Factory policy : SERVICE_FACTORY.
     */
    public static final int SERVICE_FACTORY = 1;

    /**
     * Factory policy : COMPONENT_FACTORY.
     * TODO : Component_factory behavior
     */
    public static final int COMPONENT_FACTORY = 2;

	/**
	 * At this time, it is only the java interface full name.
	 */
	private String[] m_serviceSpecification = new String[0];

	/**
	 * List of proeprty metadata.
	 */
	private PropertyMetadata[] m_properties = new PropertyMetadata[0];

	/**
	 * Foactory policy.
	 */
	private int m_factoryPolicy = SINGLETON_FACTORY;

	//CONSTRUCTOR :

	/**
     * Constructor.
	 * @param specification : service specification (i.e. the interface)
	 * @param factoryPolicy : the facotry policy.
	 */
	public ProvidedServiceMetadata(String[] specification, int factoryPolicy) {
		m_serviceSpecification = specification;
		m_factoryPolicy = factoryPolicy;
	}

	// GETTERS :


	/**
	 * @return the service specification (i.e. the interface)
	 */
	public String[] getServiceSpecification() { return m_serviceSpecification; }

	/**
	 * @return the property metadata list.
	 */
	public PropertyMetadata[] getProperties() { return m_properties; }

	/**
	 * @return the factory policy.
	 */
	public int getFactoryPolicy() { return m_factoryPolicy; }

	// SETTERS  :

	/**
     * Add the given property metadata to the property metadata list.
	 * @param p : property metdata to add
	 */
	protected void addProperty(PropertyMetadata p) {
        for (int i = 0; (m_properties != null) && (i < m_properties.length); i++) {
            if (m_properties[i] == p) { return; }
        }

        if (m_properties.length > 0) {
            PropertyMetadata[] newProp = new PropertyMetadata[m_properties.length + 1];
            System.arraycopy(m_properties, 0, newProp, 0, m_properties.length);
            newProp[m_properties.length] = p;
            m_properties = newProp;
        }
        else {
        	m_properties = new PropertyMetadata[] {p};
        }
	}
}
