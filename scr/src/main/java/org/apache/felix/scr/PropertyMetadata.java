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
package org.apache.felix.scr;

import org.osgi.service.component.ComponentException;

/**
 * A property descriptor that contains the information for properties
 * defined in the descriptor
 *
 */
public class PropertyMetadata {
	
	// Name of the property (required)
	private String m_name;
	
	// Type of the property (optional)
	private String m_type = "String";
	
	// Value of the type (optional)
	private Object m_value;
	
	// Flag that indicates if this PropertyMetadata has been validated and thus has become immutable
	private boolean m_validated = false;

	/**
	 * Set the name
	 * 
	 * @param name
	 */
	public void setName(String name) {
		if (m_validated == true) {
			return;
		}
		
		m_name = name;
	}
	

	/**
	 * Set the type
	 * 
	 * @param type
	 */
	public void setType(String type) {
		if (m_validated == true) {
			return;
		}
		m_type = type;
	}
		
	/**
	 * Set the value
	 * 
	 * @param value
	 */
	public void setValue(String value) {
		if (m_validated == true) {
			return;
		}
		
		// 112.4.5 Parsing of the value is done by the valueOf(String) method (P. 291)
		// Should the type accept lowercase too?
		if(m_type.equals("String")) {
			m_value = String.valueOf(value);
        }
		else if(m_type.equals("Long")) {
			m_value = Long.valueOf(value);
        }
		else if(m_type.equals("Double")) {
			m_value = Double.valueOf(value);
        }
		else if(m_type.equals("Float")) {
			m_value = Float.valueOf(value);
        }
		else if(m_type.equals("Integer")) {
			m_value = Integer.valueOf(value);
        }
		else if(m_type.equals("Byte")) {
			m_value = Byte.valueOf(value);
        }
		else if(m_type.equals("Char")) {
			//TODO: verify if this is adequate for Characters
			m_value = Byte.valueOf(value);
        }
		else if(m_type.equals("Boolean")) {
			m_value = Boolean.valueOf(value);
        }
		else if(m_type.equals("Short")) {
			m_value = Short.valueOf(value);
        }
		else {
			throw new IllegalArgumentException("Undefined property type '"+m_type+"'");
		}
	}

    /**
     * Get the name of the property
     * 
     * @return the name of the property
     */
    public String getName() {
        return m_name;
    }

    /**
     * Get the type of the property
     * 
     * @return the type of the property
     */
    public String getType() {
        return m_type;
    }

    /**
     * Get the value of the property
     * 
     * @return the value of the property as an Object
     */
    public Object getValue() {
        return m_value;
    }

    /**
     * Method used to verify if the semantics of this metadata are correct
     */
    public void validate(){
    	if(m_name == null)
    	{
    		throw new ComponentException("Property name attribute is mandatory");
    	}
    }
}
