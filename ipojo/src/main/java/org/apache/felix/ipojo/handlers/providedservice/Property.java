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
package org.apache.felix.ipojo.handlers.providedservice;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Logger;

/**
 * Represent a property i.e. a set : [name, type, value].
 * A property can be attached to a field.
 * The value of the property is thefield value.
 * When the value change, the published value change too.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Property {

    /**
     * A property is link with a service.
     * This field represent this provided service.
     * m_providedService : ProvidedService
     */
    private ProvidedService m_providedService;

    /**
     * Value of the property (before we know the type).
     */
    private Object m_value;
    
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
    private String m_initialValue;

    /**
     * Property constructor.
     * @param ps : the provided service
     * @param pm : metadata of the property
     */
    public Property(ProvidedService ps, String name, String field, String type, String value, Element manipulation) {
        m_providedService = ps;
        m_name = name;
        m_field = field;
        m_type = type;
        m_initialValue = value;

        // Dynamic property case :
        if (m_field != null) {
            if (m_name == null) { m_name = m_field; }
        }
        
        // Check type if not already set
        if(m_type == null) {
        	if(field == null) { ps.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "The property "+ m_name + " has neither type neither field."); return; }
                for (int j = 0; j < manipulation.getElements("Field").length; j++) {
                    if (field.equals(manipulation.getElements("Field")[j].getAttribute("name"))) {
                        m_type = manipulation.getElements("Field")[j].getAttribute("type");
                        break;
                    }
                }
                if (m_type == null) { m_providedService.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + ps.getInstanceManager().getClassName() + "] A declared property was not found in the class : " + m_field); }
        }
        
        if (m_initialValue != null) { setValue(m_initialValue); }
    }

    /**
     * Property constructor.
     * This constructor is used only for non-field property (property not attached to a field).
     * @param ps : the provided service
     * @param name : the name of the property
     * @param value : the value of the property
     */
    public Property(ProvidedService ps, String name, Object value) {
        m_providedService = ps;
        m_name = name;
        m_type = value.getClass().getName();
        m_value = value;
    }

    /**
     * @return the Object value of the property
     */
    protected Object get() { return m_value; }

    /**
     * This method is automaticaly called when the value of the property is changed.
     * Set the value of a property.
     * @param s : the new value of the property (in String)
     */
    protected void set(String s) {
        setValue(s);
        m_providedService.update();
    }

    /**
     * This method is called when the value of the property is changed.
     * Set the value of a property.
     * @param o : the new value of the property (object)
     */
    protected void set(Object o) {
        m_value = o;
        m_providedService.update();
    }

    /**
     * Set the provided service of this property.
     * @param ps : the provided service to attached.
     */
    void setProvidedService(ProvidedService ps) {
        m_providedService = ps;
    }

    /**
     * Set the value of the property.
     * @param value : value of the property (String)
     */
    private void setValue(String value) {

        // Array :
        if (m_type.endsWith("[]")) {
            String internalType = m_type.substring(0, m_type.length() - 2);
            setArrayValue(internalType, ParseUtils.parseArrays(value));
            return;
        }

        // Simple :
        if (m_type.equals("string") || m_type.equals("String")) { m_value = new String(value); return; }
        if (m_type.equals("boolean")) { m_value = new Boolean(value); return; }
        if (m_type.equals("byte")) { m_value = new Byte(value); return; }
        if (m_type.equals("short")) { m_value = new Short(value); return; }
        if (m_type.equals("int")) { m_value = new Integer(value); return; }
        if (m_type.equals("long")) { m_value = new Long(value); return; }
        if (m_type.equals("float")) { m_value = new Float(value); return; }
        if (m_type.equals("double")) { m_value = new Double(value); return; }

        // Else it is a neither a primitive type neither a String -> create the object by calling a constructor with a string in argument.
        try {
            Class c = m_providedService.getInstanceManager().getContext().getBundle().loadClass(m_type);
            //Class string = m_providedService.getComponentManager().getContext().getBundle().loadClass("java.lang.String");
            Constructor cst = c.getConstructor(new Class[] {String.class});
            m_value = cst.newInstance(new Object[] {value});
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found exception in setValue on " + m_type);
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("Constructor not found exeption in setValue on " + m_type);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Argument problem to call the constructor of the type " + m_type);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Instantiation problem  " + m_type);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Invocation problem " + m_type);
            e.printStackTrace();
        }
    }

    /**
     * Set a array value to the current property.
     * @param internalType : internal array type
     * @param values : the new value
     */
    private void setArrayValue(String internalType, String[] values) {
        if (internalType.equals("string") || internalType.equals("String")) { m_value = values; return; }
        if (internalType.equals("boolean")) {
            boolean[] bool = new boolean[values.length];
            for (int i = 0; i < values.length; i++) { bool[i] = new Boolean(values[i]).booleanValue(); }
            m_value = bool;
            return;
        }
        if (internalType.equals("byte")) {
            byte[] byt = new byte[values.length];
            for (int i = 0; i < values.length; i++) { byt[i] = new Byte(values[i]).byteValue(); }
            m_value = byt;
            return;
        }
        if (internalType.equals("short")) {
            short[] shor = new short[values.length];
            for (int i = 0; i < values.length; i++) { shor[i] = new Short(values[i]).shortValue(); }
            m_value = shor;
            return;
        }
        if (internalType.equals("int")) {
            int[] in = new int[values.length];
            for (int i = 0; i < values.length; i++) { in[i] = new Integer(values[i]).intValue(); }
            m_value = in;
            return;
        }
        if (internalType.equals("long")) {
            long[] ll = new long[values.length];
            for (int i = 0; i < values.length; i++) { ll[i] = new Long(values[i]).longValue(); }
            m_value = ll;
            return;
        }
        if (internalType.equals("float")) {
            float[] fl = new float[values.length];
            for (int i = 0; i < values.length; i++) { fl[i] = new Float(values[i]).floatValue(); }
            m_value = fl;
            return; }
        if (internalType.equals("double")) {
            double[] dl = new double[values.length];
            for (int i = 0; i < values.length; i++) { dl[i] = new Double(values[i]).doubleValue(); }
            m_value = dl;
            return; }

        // Else it is a neither a primitive type neither a String -> create the object by calling a constructor with a string in argument.
        try {
            Class c = m_providedService.getInstanceManager().getContext().getBundle().loadClass(internalType);
            Constructor cst = c.getConstructor(new Class[] {String.class});
            Object[] ob = (Object[]) Array.newInstance(c, values.length);
            for (int i = 0; i < values.length; i++) {
                ob[i] = cst.newInstance(new Object[] {values[i]});
            }
            m_value = ob;
            return;
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found exception in setValue on " + internalType);
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            System.err.println("Constructor not found exeption in setValue on " + internalType);
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            System.err.println("Argument problem to call the constructor of the type " + internalType);
            e.printStackTrace();
        } catch (InstantiationException e) {
            System.err.println("Instantiation problem  " + internalType);
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            System.err.println("Invocation problem " + internalType);
            e.printStackTrace();
        }
    }

    /**
     * @return the value of the property.
     */
    public Object getValue() { return m_value; }
    
    /**
     * @return the name of the property
     */
    public String getName() { return m_name; }
    
    /**
     * @return the field name of the property (null if the property has no field).
     */
    protected String getField() { return m_field; }

	/**
	 * Set the type of the property.
	 * @param type : the type to attached to the property
	 */
	public void setType(String type) { m_type = type; }
	
	/**
	 * @return the type of the property.
	 */
	public String getType() { return m_type; }

	/**
	 * @return the initial value of the property.
	 */
	public String getInitialValue() { return m_initialValue; }
	
}
