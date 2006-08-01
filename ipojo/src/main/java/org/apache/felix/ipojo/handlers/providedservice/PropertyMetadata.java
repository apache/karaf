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
package org.apache.felix.ipojo.handlers.providedservice;

/**
 * Property metadata : either static either dynamic.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class PropertyMetadata {

	/**
	 * Field of the property.
	 */
	private String m_field;

	/**
	 * Name of the property.
	 */
	private String m_name;

	/**
	 * Type of the property.
	 */
	private String m_type;

	/**
	 * String value of the property (initial value).
	 */
	private String m_value;

	//Constructor

	/**
     * Constructor.
	 * @param name : name of the property
	 * @param field : field of the property
	 * @param type : type of the property
	 * @param value : initial value of the property
	 */
	public PropertyMetadata(String name, String field, String type, String value) {
		m_name = name;
		m_field = field;
		m_type = type;
		m_value = value;

		// Dynamic property case :
		if (m_field != null) {
			if (m_name == null) { m_name = m_field; }
		}
	}

	/**
	 * @return the field name.
	 */
	public String getField() { return m_field; };

	/**
	 * @return the property name.
	 */
	public String getName() { return m_name; };

	/**
	 * @return the type of the property.
	 */
	public String getType() { return m_type; };

	/**
	 * @return the initial value.
	 */
	public String getValue() { return m_value; }

	/**
     * Set the type of the property (dynamic property only).
	 * @param type : the type of the property.
	 */
	public void setType(String type) { m_type = type; }


}
