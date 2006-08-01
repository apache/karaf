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
package org.apache.felix.ipojo;

import java.util.logging.Level;

import org.apache.felix.ipojo.metadata.Element;

/**
 * Component Metadata.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ComponentMetadata {

	/**
	 * Class name of the component.
	 */
	private String m_className;

	/**
	 * Is the component an immediate component ?
	 */
	private boolean m_isImmediate = false;

    /**
	 * Metadata of the component.
	 */
	private Element m_metadata;

	/**
     * Constructor.
	 * @param metadata : metadata of the component
	 */
	public ComponentMetadata(Element metadata) {
		m_metadata = metadata;
		m_className = metadata.getAttribute("className");
		if (m_className == null) {
			Activator.getLogger().log(Level.SEVERE, "The class name of ths component cannot be setted, it does not exist in the metadata");
		}
		if (metadata.containsAttribute("immediate") && metadata.getAttribute("immediate").equals("true")) { m_isImmediate = true; }
	}

	// Getter
	/**
	 * @return the class name
	 */
	public String getClassName() { return m_className; }

	/**
	 * @return the component metadata
	 */
	public Element getMetadata() { return m_metadata; }

	/**
	 * @return true if its an immediate component
	 */
	public boolean isImmediate() { return m_isImmediate; }

}
