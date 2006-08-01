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
package org.apache.felix.ipojo.metadata;

/**
 * Attribute.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Attribute {

	/**
	 * Name of the attribute.
	 */
	private String m_name;

	/**
	 * Value of the attribute.
	 */
	private String m_value;

	/**
	 * Namepsace of the attribute.
	 */
	private String m_nameSpace;

	/**
     * Constructor.
	 * @param name : name of the attribute.
	 * @param value : value of the attribute.
	 */
	public Attribute(String name, String value) {
		m_name = name.toLowerCase();
		m_value = value;
		m_nameSpace = "";
	}

	/**
     * Constructor.
	 * @param name : name of the attribute.
	 * @param value : value of the attribute.
	 * @param ns : namespace of the attribute.
	 */
	public Attribute(String name, String ns, String value) {
		m_name = name.toLowerCase();
		m_value = value;
		m_nameSpace = ns;
	}

	/**
	 * @return the name
	 */
	public String getName() { return m_name; }

	/**
	 * @return the value
	 */
	public String getValue() { return m_value; }

	/**
	 * @return the namespace
	 */
	public String getNameSpace() { return m_nameSpace; }

}
