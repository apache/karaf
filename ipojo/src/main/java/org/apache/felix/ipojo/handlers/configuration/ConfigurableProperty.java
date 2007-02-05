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
package org.apache.felix.ipojo.handlers.configuration;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.parser.ParseUtils;

/**
 * Configurable Property.
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
    public ConfigurableProperty(String name, String field, String value, String type, ConfigurationHandler ch) {
        m_handler = ch;
        if (name != null) { m_name = name; }
        else { m_name = field; }
        m_field = field;

        if (value != null) { setValue(m_field, value, type); }

    }

    /**
     * Set the value of the property.
     * @param strValue : value of the property (String)
     */
    private void setValue(String field, String strValue, String type) {
        Object value = null;

        if (type.equals("string") || type.equals("String")) { value = new String(strValue); }
        if (type.equals("boolean")) { value = new Boolean(strValue); }
        if (type.equals("byte")) { value = new Byte(strValue); }
        if (type.equals("short")) { value = new Short(strValue); }
        if (type.equals("int")) { value = new Integer(strValue); }
        if (type.equals("long")) { value = new Long(strValue); }
        if (type.equals("float")) { value = new Float(strValue); }
        if (type.equals("double")) { value = new Double(strValue); }
        // Array :
        if (type.endsWith("[]")) {
            String internalType = type.substring(0, type.length() - 2);
            setArrayValue(internalType, ParseUtils.parseArrays(strValue));
            return;
        }

        if (value == null) {
            // Else it is a neither a primitive type neither a String -> create the object by calling a constructor with a string in argument.
            try {
                Class c = m_handler.getInstanceManager().getContext().getBundle().loadClass(type);
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
     * Set array value to the current property.
     * @param internalType : type of the property
     * @param values : new property value
     */
    private void setArrayValue(String internalType, String[] values) {
        if (internalType.equals("string") || internalType.equals("String")) { 
        	String[] str = new String[values.length];
        	for (int i = 0; i < values.length; i++) { str[i] = new String(values[i]); }
        	m_value = str;
        	return;
        }
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
            Class c = m_handler.getInstanceManager().getContext().getBundle().loadClass(internalType);
            Constructor cst = c.getConstructor(new Class[] {String.class});
            Object[] ob = (Object[]) Array.newInstance(c, values.length);
            for (int i = 0; i < values.length; i++) {
                ob[i] = cst.newInstance(new Object[] {values[i].trim()});
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
