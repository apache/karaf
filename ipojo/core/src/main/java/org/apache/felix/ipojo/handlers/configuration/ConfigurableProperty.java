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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Callback;
import org.apache.felix.ipojo.util.Logger;

/**
 * Configurable Property.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
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
    private Callback m_method;

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
     * @param name : name of the property (optional)
     * @param field : name of the field
     * @param method : method name
     * @param value : initial value of the property (optional)
     * @param type : the type of the property
     * @param ch : configuration handler managing this configurable property
     * @throws ConfigurationException : occurs when the property value cannot be set.
     */
    public ConfigurableProperty(String name, String field, String method, String value, String type, ConfigurationHandler ch) throws ConfigurationException {
        m_handler = ch;

        m_field = field;

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
        } else {
            setType(type);
        }

        if (method != null) {
            m_method = new Callback(method, new String[] { m_type.getName() }, false, m_handler.getInstanceManager());
        }

    }

    /**
     * The set type method fix the property type according to the given type name.
     * @param type : the type name
     * @throws ConfigurationException if an error occurs when loading the type class for non-primitive types.
     */
    private void setType(String type) throws ConfigurationException {
        // Syntactic sugar to avoid writing java.lang.String
        if ("string".equals(type) || "String".equals(type)) {
            m_type = java.lang.String.class;
            return;
        }
        if (type.equals("boolean")) {
            m_type = Boolean.TYPE;
            return;
        }
        if ("byte".equals(type)) {
            m_type = Byte.TYPE;
            return;
        }
        if ("short".equals(type)) {
            m_type = Short.TYPE;
            return;
        }
        if ("int".equals(type)) {
            m_type = Integer.TYPE;
            return;
        }
        if ("long".equals(type)) {
            m_type = Long.TYPE;
            return;
        }
        if ("float".equals(type)) {
            m_type = Float.TYPE;
            return;
        }
        if ("double".equals(type)) {
            m_type = Double.TYPE;
            return;
        }
        if ("char".equals(type)) {
            m_type = Character.TYPE;
            return;
        }

        // Array :
        if (type.endsWith("[]")) {
            String internalType = type.substring(0, type.length() - 2);
            if ("string".equals(internalType) || "String".equals(internalType)) {
                m_type = new String[0].getClass();
                return;
            }
            if ("boolean".equals(internalType)) {
                m_type = new boolean[0].getClass();
                return;
            }
            if ("byte".equals(internalType)) {
                m_type = new byte[0].getClass();
                return;
            }
            if ("short".equals(internalType)) {
                m_type = new short[0].getClass();
                return;
            }
            if ("int".equals(internalType)) {
                m_type = new int[0].getClass();
                return;
            }
            if ("long".equals(internalType)) {
                m_type = new long[0].getClass();
                return;
            }
            if ("float".equals(internalType)) {
                m_type = new float[0].getClass();
                return;
            }
            if ("double".equals(internalType)) {
                m_type = new double[0].getClass();
                return;
            }
            if ("char".equals(internalType)) {
                m_type = new char[0].getClass();
                return;
            }

            // Complex array type.
            try {
                Class c = m_handler.getInstanceManager().getContext().getBundle().loadClass(internalType);
                Object[] ob = (Object[]) Array.newInstance(c, 0);
                m_type = ob.getClass();
                return;
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Class not found exception in setValue on " + internalType);
            } catch (SecurityException e) {
                throw new ConfigurationException("Secutiry Exception in setValue on " + internalType);
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Argument problem to call the constructor of the type " + internalType);
            }
        }

        // Non array, complex type.
        try {
            m_type = m_handler.getInstanceManager().getContext().getBundle().loadClass(type);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Class not found exception in setValue on " + type + " : " + e.getMessage());
        } catch (SecurityException e) {
            throw new ConfigurationException("Security excption in setValue on " + type + " : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument problem to call the constructor of the type " + type);
        }
    }

    /**
     * Set the value of the property.
     * @param strValue : value of the property (String)
     * @param type : type of the property
     * @throws ConfigurationException : occurs when the property value cannot be initialized.
     */
    private void setValue(String strValue, String type) throws ConfigurationException {
        Object value = null;

        // Syntactic sugar to avoid writing java.lang.String
        if ("string".equals(type) || "String".equals(type)) {
            value = new String(strValue);
            m_type = java.lang.String.class;
        }
        if (type.equals("boolean")) {
            value = new Boolean(strValue);
            m_type = Boolean.TYPE;
        }
        if ("byte".equals(type)) {
            value = new Byte(strValue);
            m_type = Byte.TYPE;
        }
        if ("short".equals(type)) {
            value = new Short(strValue);
            m_type = Short.TYPE;
        }
        if ("int".equals(type)) {
            value = new Integer(strValue);
            m_type = Integer.TYPE;
        }
        if ("long".equals(type)) {
            value = new Long(strValue);
            m_type = Long.TYPE;
        }
        if ("float".equals(type)) {
            value = new Float(strValue);
            m_type = Float.TYPE;
        }
        if ("double".equals(type)) {
            value = new Double(strValue);
            m_type = Double.TYPE;
        }
        if ("char".equals(type)) {
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
                throw new ConfigurationException("Class not found exception in setValue on " + type + " : " + e.getMessage());
            } catch (SecurityException e) {
                throw new ConfigurationException("Security excption in setValue on " + type + " : " + e.getMessage());
            } catch (NoSuchMethodException e) {
                throw new ConfigurationException("Constructor not found exeption in setValue on " + type + " : " + e.getMessage());
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Argument problem to call the constructor of the type " + type);
            } catch (InstantiationException e) {
                throw new ConfigurationException("Instantiation problem  " + type);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Illegal Access " + type);
            } catch (InvocationTargetException e) {
                throw new ConfigurationException("Invocation problem " + type + " : " + e.getTargetException().getMessage());
            }
        }

        m_value = value;

    }

    /**
     * Set array value to the current property.
     * 
     * @param internalType : type of the property
     * @param values : new property value
     * @throws ConfigurationException occurs when the array cannot be initialized
     */
    private void setArrayValue(String internalType, String[] values) throws ConfigurationException {
        if ("string".equals(internalType) || "String".equals(internalType)) {
            String[] str = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                str[i] = new String(values[i]);
            }
            m_value = str;
            m_type = new String[0].getClass();
            return;
        }
        if ("boolean".equals(internalType)) {
            boolean[] bool = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                bool[i] = new Boolean(values[i]).booleanValue();
            }
            m_value = bool;
            m_type = new boolean[0].getClass();
            return;
        }
        if ("byte".equals(internalType)) {
            byte[] byt = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                byt[i] = new Byte(values[i]).byteValue();
            }
            m_value = byt;
            m_type = new byte[0].getClass();
            return;
        }
        if ("short".equals(internalType)) {
            short[] shor = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                shor[i] = new Short(values[i]).shortValue();
            }
            m_value = shor;
            m_type = new short[0].getClass();
            return;
        }
        if ("int".equals(internalType)) {
            int[] in = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                in[i] = new Integer(values[i]).intValue();
            }
            m_value = in;
            m_type = new int[0].getClass();
            return;
        }
        if ("long".equals(internalType)) {
            long[] ll = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                ll[i] = new Long(values[i]).longValue();
            }
            m_value = ll;
            m_type = new long[0].getClass();
            return;
        }
        if ("float".equals(internalType)) {
            float[] fl = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                fl[i] = new Float(values[i]).floatValue();
            }
            m_value = fl;
            m_type = new float[0].getClass();
            return;
        }
        if ("double".equals(internalType)) {
            double[] dl = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                dl[i] = new Double(values[i]).doubleValue();
            }
            m_value = dl;
            m_type = new double[0].getClass();
            return;
        }
        if ("char".equals(internalType)) {
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
            throw new ConfigurationException("Class not found exception in setValue on " + internalType);
        } catch (SecurityException e) {
            throw new ConfigurationException("Secutiry Exception in setValue on " + internalType);
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Constructor not found exception in setValue on " + internalType);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument problem to call the constructor of the type " + internalType);
        } catch (InstantiationException e) {
            throw new ConfigurationException("Instantiation problem  " + internalType);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Illegal Access Exception in  " + internalType);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("Invocation problem " + internalType + " : " + e.getTargetException().getMessage());
        }
    }

    public String getName() {
        return m_name;
    }

    public String getField() {
        return m_field;
    }

    /**
     * Get method name, null if no method.
     * @return the method name.
     */
    public String getMethod() {
        if (m_method == null) { return null; }
        return m_method.getMethod();
    }

    /**
     * Check if the property has a method callback.
     * @return true if the property has a method.
     */
    public boolean hasMethod() {
        return m_method != null;
    }

    /**
     * Check if the property has a field.
     * @return true if the property has a field.
     */
    public boolean hasField() {
        return m_field != null;
    }

    public Object getValue() {
        return m_value;
    }

    /**
     * Fix the value of the property.
     * @param value : the new value.
     */
    public void setValue(Object value) {
        // Is the object is directly assignable to the property, affect it.
        if (isAssignable(m_type, value)) {
            m_value = value;
        } else {
            // If the object is a String, we must recreate the object from the String form
            if (value instanceof String) {
                try {
                    m_value = create(m_type, (String) value);
                } catch (ConfigurationException e) {
                    throw new ClassCastException("Incompatible type for the property " + m_name + " : " + e.getMessage());
                }
            } else {
                // Error, the given property cannot be injected.
                throw new ClassCastException("Incompatible type for the property " + m_name + " " + m_type.getName() + " expected, " + value.getClass() + " received");
            }
        }
    }
    
    /**
     * Test if the given value is assignable to the given type.
     * @param type : class of the type
     * @param value : object to check
     * @return true if the object is assignable in the property of type 'type'.
     */
    public static boolean isAssignable(Class type, Object value) {
        if (type.isInstance(value)) {
            return true;
        } else if (type.isPrimitive()) {
            // Manage all boxing types.
            if (value instanceof Boolean && type.equals(Boolean.TYPE)) {
                return true;
            }
            if (value instanceof Byte && type.equals(Byte.TYPE)) {
                return true;
            }
            if (value instanceof Short && type.equals(Short.TYPE)) {
                return true;
            }
            if (value instanceof Integer && type.equals(Integer.TYPE)) {
                return true;
            }
            if (value instanceof Long && type.equals(Long.TYPE)) {
                return true;
            }
            if (value instanceof Float && type.equals(Float.TYPE)) {
                return true;
            }
            if (value instanceof Double && type.equals(Double.TYPE)) {
                return true;
            }
            if (value instanceof Character && type.equals(Character.TYPE)) {
                return true;
            }
            return false;
        } else {
            // Else return false.
            return false;
        }
    }

    /**
     * Create an object of the given type with the given String value.
     * @param type : type of the returned object
     * @param strValue : String value.
     * @return the object of type 'type' created from the String 'value'
     * @throws ConfigurationException occurs when the object cannot be created.
     */
    public static Object create(Class type, String strValue) throws ConfigurationException {
        if (type.equals(Boolean.TYPE)) { return new Boolean(strValue); }
        if (type.equals(Byte.TYPE)) { return new Byte(strValue); }
        if (type.equals(Short.TYPE)) { return new Short(strValue); }
        if (type.equals(Integer.TYPE)) { return new Integer(strValue); }
        if (type.equals(Long.TYPE)) { return new Long(strValue); }
        if (type.equals(Float.TYPE)) { return new Float(strValue); }
        if (type.equals(Double.TYPE)) { return new Double(strValue); }
        if (type.equals(Character.TYPE)) { return new Character(strValue.charAt(0)); }

        // Array :
        if (type.isArray()) { return createArrayObject(type.getComponentType(), ParseUtils.parseArrays(strValue)); }
        // Else it is a neither a primitive type neither a String -> create
        // the object by calling a constructor with a string in argument.
        try {
            Constructor cst = type.getConstructor(new Class[] { String.class });
            return cst.newInstance(new Object[] { strValue });
        } catch (SecurityException e) {
            throw new ConfigurationException("Security exception in create on " + type + " : " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Constructor not found exception in create on " + type + " : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument problem to call the constructor of the type " + type);
        } catch (InstantiationException e) {
            throw new ConfigurationException("Instantiation problem  " + type);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Illegal Access " + type);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("Invocation problem " + type + " : " + e.getTargetException().getMessage());
        }

    }

    /**
     * Create an array object containing the type 'interntype' from the String array 'values'.
     * @param interntype : internal type of the array.
     * @param values : String array
     * @return the array containing objects created from the 'values' array
     * @throws ConfigurationException occurs when the array cannot be created correctly
     */
    public static Object createArrayObject(Class interntype, String[] values) throws ConfigurationException {
        if (interntype.equals(Boolean.TYPE)) {
            boolean[] bool = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                bool[i] = new Boolean(values[i]).booleanValue();
            }
            return bool;
        }
        if (interntype.equals(Byte.TYPE)) {
            byte[] byt = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                byt[i] = new Byte(values[i]).byteValue();
            }
            return byt;
        }
        if (interntype.equals(Short.TYPE)) {
            short[] shor = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                shor[i] = new Short(values[i]).shortValue();
            }
            return shor;
        }
        if (interntype.equals(Integer.TYPE)) {
            int[] in = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                in[i] = new Integer(values[i]).intValue();
            }
            return in;
        }
        if (interntype.equals(Long.TYPE)) {
            long[] ll = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                ll[i] = new Long(values[i]).longValue();
            }
            return ll;
        }
        if (interntype.equals(Float.TYPE)) {
            float[] fl = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                fl[i] = new Float(values[i]).floatValue();
            }
            return fl;
        }
        if (interntype.equals(Double.TYPE)) {
            double[] dl = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                dl[i] = new Double(values[i]).doubleValue();
            }
            return dl;
        }
        if (interntype.equals(Character.TYPE)) {
            char[] dl = new char[values.length];
            for (int i = 0; i < values.length; i++) {
                dl[i] = values[i].toCharArray()[0];
            }
            return dl;
        }

        // Else it is a neither a primitive type -> create the
        // object by calling a constructor with a string in argument.
        try {
            Constructor cst = interntype.getConstructor(new Class[] { String.class });
            Object[] ob = (Object[]) Array.newInstance(interntype, values.length);
            for (int i = 0; i < values.length; i++) {
                ob[i] = cst.newInstance(new Object[] { values[i].trim() });
            }
            return ob;
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Constructor not found exception in setValue on " + interntype.getName());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument problem to call the constructor of the type " + interntype.getName());
        } catch (InstantiationException e) {
            throw new ConfigurationException("Instantiation problem  " + interntype.getName());
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Illegal Access Exception in  " + interntype.getName());
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("Invocation problem " + interntype.getName() + " : " + e.getTargetException().getMessage());
        }
    }

    /**
     * Invoke the method (if specified).
     * If the invocation failed, the instance is stopped.
     */
    public void invoke() {
        try {
            m_method.call(new Object[] { m_value });
        } catch (NoSuchMethodException e) {
            m_handler.log(Logger.ERROR, "The method " + m_method + " does not exist in the class " + m_handler.getInstanceManager().getClassName());
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.log(Logger.ERROR, "The method " + m_method + " is not accessible in the class " + m_handler.getInstanceManager().getClassName());
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.log(Logger.ERROR, "The method " + m_method + " in the class " + m_handler.getInstanceManager().getClassName() + "throws an exception : " + e.getTargetException().getMessage());
            m_handler.getInstanceManager().setState(ComponentInstance.INVALID);
        }
    }

    /**
     * Handler createInstance method. This method is override to allow delayed callback invocation.
     * If the invocation failed, the instance is stopped.
     * @param instance : the created object
     * @see org.apache.felix.ipojo.Handler#objectCreated(java.lang.Object)
     */
    public void invoke(Object instance) {
        try {
            m_method.call(instance, new Object[] { m_value });
        } catch (NoSuchMethodException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "The method " + m_method + " does not exist in the class " + m_handler.getInstanceManager().getClassName());
            m_handler.getInstanceManager().stop();
        } catch (IllegalAccessException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "The method " + m_method + " is not accessible in the class " + m_handler.getInstanceManager().getClassName());
            m_handler.getInstanceManager().stop();
        } catch (InvocationTargetException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "The method " + m_method + " in the class " + m_handler.getInstanceManager().getClassName() + "throws an exception : " + e.getTargetException().getMessage());
            m_handler.getInstanceManager().setState(ComponentInstance.INVALID);
        }
    }
}
