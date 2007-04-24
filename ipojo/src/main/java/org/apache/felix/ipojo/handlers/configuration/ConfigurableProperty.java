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
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Logger;

/**
 * Configurable Property.
 * 
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
     * Method of the property.
     */
    private String m_method;

    /**
     * Value of the property.
     */
    private Object m_value;

    /**
     * Type of the property.
     */
    private Class m_type;

    /**
     * Configuration Handler managing this property.
     */
    private ConfigurationHandler m_handler;

    /**
     * Configurable Property Constructor. At least the method or the field need
     * to be referenced.
     * 
     * @param name :
     *            name of the property (optional)
     * @param field :
     *            name of the field
     * @param method :
     *            method name
     * @param value :
     *            initial value of the property (optional)
     * @param type :
     *            the type of the property
     * @param ch :
     *            configuration handler managing this configurable property
     */
    public ConfigurableProperty(String name, String field, String method, String value, String type,
            ConfigurationHandler ch) {
        m_handler = ch;

        m_field = field;
        m_method = method;

        if (name != null) {
            m_name = name;
        } else {
            if (m_field != null) {
                m_name = field;
            } else {
                m_name = method;
            }
        }
        m_field = field;

        if (value != null) {
            setValue(value, type);
        }

    }

    /**
     * Set the value of the property.
     * 
     * @param strValue :
     *            value of the property (String)
     * @param type :
     *            type of the property
     */
    private void setValue(String strValue, String type) {
        Object value = null;

        if (type.equals("string") || type.equals("String")) {
            value = new String(strValue);
            m_type = java.lang.String.class;
        }
        if (type.equals("boolean")) {
            value = new Boolean(strValue);
            m_type = Boolean.TYPE;
        }
        if (type.equals("byte")) {
            value = new Byte(strValue);
            m_type = Byte.TYPE;
        }
        if (type.equals("short")) {
            value = new Short(strValue);
            m_type = Short.TYPE;
        }
        if (type.equals("int")) {
            value = new Integer(strValue);
            m_type = Integer.TYPE;
        }
        if (type.equals("long")) {
            value = new Long(strValue);
            m_type = Long.TYPE;
        }
        if (type.equals("float")) {
            value = new Float(strValue);
            m_type = Float.TYPE;
        }
        if (type.equals("double")) {
            value = new Double(strValue);
            m_type = Double.TYPE;
        }
        if (type.equals("char")) {
            value = new Character(strValue.charAt(0));
            m_type = Character.TYPE;
        }

        // Array :
        if (type.endsWith("[]")) {
            String internalType = type.substring(0, type.length() - 2);
            setArrayValue(internalType, ParseUtils.parseArrays(strValue));
            return;
        }

        if (value == null) {
            // Else it is a neither a primitive type neither a String -> create
            // the object by calling a constructor with a string in argument.
            try {
                m_type = m_handler.getInstanceManager().getContext().getBundle().loadClass(type);
                Constructor cst = m_type.getConstructor(new Class[] { String.class });
                value = cst.newInstance(new Object[] { strValue });
            } catch (ClassNotFoundException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "Class not found exception in setValue on " + type + " : " + e.getMessage());
                return;
            } catch (SecurityException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "Security excption in setValue on " + type + " : " + e.getMessage());
                return;
            } catch (NoSuchMethodException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "Constructor not found exeption in setValue on " + type + " : " + e.getMessage());
                return;
            } catch (IllegalArgumentException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "Argument problem to call the constructor of the type " + type);
                return;
            } catch (InstantiationException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "Instantiation problem  " + type);
                return;
            } catch (IllegalAccessException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "Illegal Access " + type);
                return;
            } catch (InvocationTargetException e) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "Invocation problem " + type);
                return;
            }
        }

        m_value = value;

    }

    /**
     * Set array value to the current property.
     * 
     * @param internalType :
     *            type of the property
     * @param values :
     *            new property value
     */
    private void setArrayValue(String internalType, String[] values) {
        if (internalType.equals("string") || internalType.equals("String")) {
            String[] str = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                str[i] = new String(values[i]);
            }
            m_value = str;
            m_type = new String[0].getClass();
            return;
        }
        if (internalType.equals("boolean")) {
            boolean[] bool = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                bool[i] = new Boolean(values[i]).booleanValue();
            }
            m_value = bool;
            m_type = new boolean[0].getClass();
            return;
        }
        if (internalType.equals("byte")) {
            byte[] byt = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                byt[i] = new Byte(values[i]).byteValue();
            }
            m_value = byt;
            m_type = new byte[0].getClass();
            return;
        }
        if (internalType.equals("short")) {
            short[] shor = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                shor[i] = new Short(values[i]).shortValue();
            }
            m_value = shor;
            m_type = new short[0].getClass();
            return;
        }
        if (internalType.equals("int")) {
            int[] in = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                in[i] = new Integer(values[i]).intValue();
            }
            m_value = in;
            m_type = new int[0].getClass();
            return;
        }
        if (internalType.equals("long")) {
            long[] ll = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                ll[i] = new Long(values[i]).longValue();
            }
            m_value = ll;
            m_type = new long[0].getClass();
            return;
        }
        if (internalType.equals("float")) {
            float[] fl = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                fl[i] = new Float(values[i]).floatValue();
            }
            m_value = fl;
            m_type = new float[0].getClass();
            return;
        }
        if (internalType.equals("double")) {
            double[] dl = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                dl[i] = new Double(values[i]).doubleValue();
            }
            m_value = dl;
            m_type = new double[0].getClass();
            return;
        }
        if (internalType.equals("char")) {
            char[] dl = new char[values.length];
            for (int i = 0; i < values.length; i++) {
                dl[i] = values[i].toCharArray()[0];
            }
            m_value = dl;
            m_type = new char[0].getClass();
            return;
        }

        // Else it is a neither a primitive type neither a String -> create the
        // object by calling a constructor with a string in argument.
        try {
            Class c = m_handler.getInstanceManager().getContext().getBundle().loadClass(internalType);
            Constructor cst = c.getConstructor(new Class[] { String.class });
            Object[] ob = (Object[]) Array.newInstance(c, values.length);
            for (int i = 0; i < values.length; i++) {
                ob[i] = cst.newInstance(new Object[] { values[i].trim() });
            }
            m_value = ob;
            m_type = ob.getClass();
            return;
        } catch (ClassNotFoundException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Class not found exception in setValue on " + internalType);
        } catch (SecurityException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Secutiry Exception in setValue on " + internalType);
        } catch (NoSuchMethodException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Constructor not found exception in setValue on " + internalType);
        } catch (IllegalArgumentException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Argument problem to call the constructor of the type " + internalType);
        } catch (InstantiationException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Instantiation problem  " + internalType);
        } catch (IllegalAccessException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Illegal Access Exception in  " + internalType);
        } catch (InvocationTargetException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Invocation problem " + internalType);
        }
    }

    public String getName() {
        return m_name;
    }

    public String getField() {
        return m_field;
    }

    public String getMethod() {
        return m_method;
    }

    public Object getValue() {
        return m_value;
    }

    /**
     * Fix the value of the property.
     * 
     * @param value :
     *            the new value.
     */
    public void setValue(Object value) {
        m_value = value;
    }

    /**
     * Invoke the method (if specified).
     */
    public void invoke() {
        Callback cb = new Callback(m_method, new String[] { m_type.getName() }, false, m_handler.getInstanceManager());
        try {
            cb.call(new Object[] { m_value });
        } catch (NoSuchMethodException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(
                    Logger.ERROR, "The method " + m_method + " does not exist in the class "
                            + m_handler.getInstanceManager().getClassName());
            return;
        } catch (IllegalAccessException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(
                    Logger.ERROR,
                    "The method " + m_method + " is not accessible in the class "
                            + m_handler.getInstanceManager().getClassName());
            return;
        } catch (InvocationTargetException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(
                    Logger.ERROR,
                    "The method " + m_method + " in the class " + m_handler.getInstanceManager().getClassName()
                            + "thorws an exception : " + e.getMessage());
            return;
        }
    }

    /**
     * Handler createInstance method. This method is overided to allow delayed
     * callback invocation.
     * 
     * @param instance :
     *            the created object
     * @see org.apache.felix.ipojo.Handler#createInstance(java.lang.Object)
     */
    public void invoke(Object instance) {
        Callback cb = new Callback(m_method, new String[] { m_type.getName() }, false, m_handler.getInstanceManager());
        try {
            cb.call(instance, new Object[] { m_value });
        } catch (NoSuchMethodException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(
                    Logger.ERROR, "The method " + m_method + " does not exist in the class "
                            + m_handler.getInstanceManager().getClassName());
            return;
        } catch (IllegalAccessException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(
                    Logger.ERROR,
                    "The method " + m_method + " is not accessible in the class "
                            + m_handler.getInstanceManager().getClassName());
            return;
        } catch (InvocationTargetException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(
                    Logger.ERROR,
                    "The method " + m_method + " in the class " + m_handler.getInstanceManager().getClassName()
                            + "thorws an exception : " + e.getMessage());
            return;
        }
    }
}
