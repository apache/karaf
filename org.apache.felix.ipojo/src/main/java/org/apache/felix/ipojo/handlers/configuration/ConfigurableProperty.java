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
package org.apache.felix.ipojo.handlers.configuration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;

import org.apache.felix.ipojo.Activator;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Configurable Property
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ConfigurableProperty {

	/**
	 * Name of the property (filed name if not set).
	 */
	private String m_name;

	/**
	 * Field of the property.
	 */
	private String m_field;

	/**
	 * Value of the property.
	 */
	private Object m_value;

	/**
	 * Configuration Handler managing this property.
	 */
	private ConfigurationHandler m_handler;

	/**
	 * Configurable Property Constructor.
	 * @param name : name of the property (optional)
	 * @param field : name of the field (mandatory)
	 * @param value : initial value of the property (optional)
	 * @param ch : configuration handler managing this configurable property
	 */
	public ConfigurableProperty(String name, String field, String value, ConfigurationHandler ch) {
		m_handler = ch;
		if (name != null) { m_name = name; }
		else { m_name = field; }
		m_field = field;

		if (value != null) { setValue(m_field, value); }

	}

	/**
     * Set the value of the property.
     * @param strValue : value of the property (String)
     */
    private void setValue(String field, String strValue) {
    	// Look for the type of the field
    	Element manipulation = m_handler.getComponentManager().getComponentMetatada().getMetadata().getElements("Manipulation")[0];
    	String type = null;
    	for (int i = 0; i < manipulation.getElements("Field").length; i++) {
    		if (field.equals(manipulation.getElements("Field")[i].getAttribute("name"))) {
    			type = manipulation.getElements("Field")[i].getAttribute("type");
    			break;
    		}
    	}

    	if (type == null) { Activator.getLogger().log(Level.SEVERE, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] The field " + field + " does not exist in the implementation"); return; }

    	Activator.getLogger().log(Level.INFO, "[" + m_handler.getComponentManager().getComponentMetatada().getClassName() + "] Set the value of the configurable property " + field + " [" + type + "] " + " with the value : " + strValue);

    	Object value = null;

        if (type.equals("string") || type.equals("String")) { value = new String(strValue); }
        if (type.equals("boolean")) { value = new Boolean(strValue); }
        if (type.equals("byte")) { value = new Byte(strValue); }
        if (type.equals("short")) { value = new Short(strValue); }
        if (type.equals("int")) { value = new Integer(strValue); }
        if (type.equals("long")) { value = new Long(strValue); }
        if (type.equals("float")) { value = new Float(strValue); }
        if (type.equals("double")) { value = new Double(strValue); }

        if (value == null) {
        	// Else it is a neither a primitive type neither a String -> create the object by calling a constructor with a string in argument.
        	try {
        		Class c = m_handler.getComponentManager().getContext().getBundle().loadClass(type);
        		Constructor cst = c.getConstructor(new Class[] {String.class});
        		value = cst.newInstance(new Object[] {strValue});
        	} catch (ClassNotFoundException e) {
        		System.err.println("Class not found exception in setValue on " + type);
        		e.printStackTrace();
        		return;
        	} catch (SecurityException e) {
        		e.printStackTrace();
        		return;
        	} catch (NoSuchMethodException e) {
        		System.err.println("Constructor not found exeption in setValue on " + type);
        		e.printStackTrace();
        		return;
        	} catch (IllegalArgumentException e) {
        		System.err.println("Argument problem to call the constructor of the type " + type);
        		e.printStackTrace();
        		return;
        	} catch (InstantiationException e) {
        		System.err.println("Instantiation problem  " + type);
        		e.printStackTrace();
        		return;
        	} catch (IllegalAccessException e) {
        		e.printStackTrace();
        	} catch (InvocationTargetException e) {
        		System.err.println("Invocation problem " + type);
        		e.printStackTrace();
        		return;
        	}
        }

        m_value = value;

    }

    /**
     * @return the name of the property.
     */
    public String getName() { return m_name; }

    /**
     * @return the field of the property.
     */
    public String getField() { return m_field; }

    /**
     * @return the value of the property.
     */
    public Object getValue() { return m_value; }

    /**
     * Fix the value of the property.
     * @param value : the new value.
     */
    public void setValue(Object value) { m_value = value; }
}
