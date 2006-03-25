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
package org.apache.felix.servicebinder;

/**
 * A property descriptor that contains the information for properties
 * defined in the meta-data file.
 *
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class PropertyMetadata
{
	String name;
	String type;
	Object value;

	/**
	 * Create a PropertyMetadata object
	 *
	 * @param   name the name of the property
	 * @param   type the type of the property (string, boolean, byte, char, short, int, long, float or double)
	 * @param   val the value of the property
	 */
	public PropertyMetadata(String name, String type, String val)
	{
		this.name = name;
		type.toLowerCase();
		this.type = type;
		value = null;

		if(type.equals("string") || type.equals("String"))
        {
			value = new String(val);
        }
		else if(type.equals("boolean"))
        {
			value = new Boolean(val);
        }
		else if(type.equals("byte"))
        {
			value = new Byte(val);
        }
		else if(type.equals("char"))
        {
			value = new Byte(val);
        }
		else if(type.equals("short"))
        {
			value = new Short(val);
        }
		else if(type.equals("int"))
        {
			value = new Integer(val);
        }
		else if(type.equals("long"))
        {
			value = new Long(val);
        }
		else if(type.equals("float"))
        {
			value = new Float(val);
        }
		else if(type.equals("double"))
        {
			value = new Double(val);
        }
	}

    /**
     * Get the name of the property
     * 
     * @return the name of the property
     * 
     * @uml.property name="name"
     */
    public String getName() {
        return name;
    }

    /**
     * Get the type of the property
     * 
     * @return the type of the property
     * 
     * @uml.property name="type"
     */
    public String getType() {
        return type;
    }

    /**
     * Get the value of the property
     * 
     * @return the value of the property as an Object
     * 
     * @uml.property name="value"
     */
    public Object getValue() {
        return value;
    }
}
