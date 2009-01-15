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
package org.apache.felix.ipojo.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.BundleContext;

/**
 * Property class managing a managed value.
 * This class managed the method invocation as well as field injection.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Property implements FieldInterceptor {
    
    /**
     * Object used for an unvalued property.
     */
    public static final Object NO_VALUE = new Object();

    /**
     * The name of the property (field name if not set).
     * Cannot change once set.
     */
    private final String m_name;

    /**
     * The field of the property.
     * Cannot change once set.
     */
    private final String m_field;

    /**
     * The setter method of the property.
     * Cannot change once set.
     */
    private final Callback m_method;

    /**
     * The value of the property.
     */
    private Object m_value = NO_VALUE;
    
    /**
     * Flag tracking is the method was 
     * already called for the current value.
     */
    private boolean m_invoked;

    /**
     * The type of the property.
     */
    private final Class m_type;

    /**
     * The handler object to get the logger.
     */
    private final Handler m_handler;
    
    /**
     * The instance manager.
     */
    private final InstanceManager m_manager;

    /**
     * Creates a property. 
     * At least the method or the field need
     * to be specified.
     * @param name the name of the property (optional)
     * @param field the name of the field
     * @param method the method name
     * @param value the initial value of the property (optional)
     * @param type the the type of the property
     * @param manager the instance manager
     * @param handler the handler object which manage this property.
     * @throws ConfigurationException if the property value cannot be set.
     */
    public Property(String name, String field, String method, String value, String type, InstanceManager manager, Handler handler) throws ConfigurationException {
        m_handler = handler;
        m_manager = manager;
        m_field = field;

        if (name == null) {
            if (m_field == null) {
                m_name = method;
            } else {
                m_name = field;
            }
        } else {
            m_name = name;
        }
        
        m_type = computeType(type, manager.getGlobalContext());
        if (value != null) {
            m_value = create(m_type, value);
        }

        if (method != null) {
            m_method = new Callback(method, new String[] { m_type.getName() }, false, manager);
        } else {
            m_method = null;
        }

    }

    /**
     * Computes and returns the property type according to the given type name.
     * @param type the the type name
     * @param context the bundle context (used to load classes)
     * @return the class of the given type
     * @throws ConfigurationException if an error occurs when loading the type class for non-primitive types.
     */
    public static Class computeType(String type, BundleContext context) throws ConfigurationException {
        // Array :
        if (type.endsWith("[]")) {
            return computeArrayType(type, context);
        } else {
            // Syntactic sugar to avoid writing java.lang.String
            if ("string".equals(type) || "String".equals(type)) {
                return java.lang.String.class;
            } else if ("boolean".equals(type)) {
                return Boolean.TYPE;
            } else if ("byte".equals(type)) {
                return Byte.TYPE;
            } else if ("short".equals(type)) {
                return  Short.TYPE;
            } else if ("int".equals(type)) {
                return Integer.TYPE;
            } else if ("long".equals(type)) {
                return Long.TYPE;
            } else if ("float".equals(type)) {
                return Float.TYPE;
            } else if ("double".equals(type)) {
                return Double.TYPE;
            } else if ("char".equals(type)) {
                return Character.TYPE;
            } else {
                // Non array, complex type.
                try {
                    return context.getBundle().loadClass(type);
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Class not found exception in setValue on " + type + " : " + e.getMessage());
                } catch (SecurityException e) {
                    throw new ConfigurationException("Security execption in setValue on " + type + " : " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    throw new ConfigurationException("Argument issue when calling the constructor of the type " + type);
                }
            }
        }
    }

    /**
     * Gets the Class object of a type array.
     * @param type the string descriptor of the type (must end by [] )
     * @param context the bundle context (used to load classes)
     * @return the Class object of the given type array.
     * @throws ConfigurationException if the class cannot be loaded
     */
    private static Class computeArrayType(String type, BundleContext context) throws ConfigurationException {
        // Note: Harmony does't support the type[].class notation.
        // An empty array has to be created to get the class object.
        String internalType = type.substring(0, type.length() - 2);
        if ("string".equals(internalType) || "String".equals(internalType)) {
            return new String[0].getClass();
        }
        if ("boolean".equals(internalType)) {
            return new boolean[0].getClass();
        }
        if ("byte".equals(internalType)) {
            return new byte[0].getClass();
        }
        if ("short".equals(internalType)) {
            return new short[0].getClass();
        }
        if ("int".equals(internalType)) {
            return new int[0].getClass();
        }
        if ("long".equals(internalType)) {
            return new long[0].getClass();
        }
        if ("float".equals(internalType)) {
            return new float[0].getClass();
        }
        if ("double".equals(internalType)) {
            return new double[0].getClass();
        }
        if ("char".equals(internalType)) {
            return new char[0].getClass();
        }

        // Complex array type.
        try {
            Class clazz = context.getBundle().loadClass(internalType);
            Object[] object = (Object[]) Array.newInstance(clazz, 0);
            return object.getClass();
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Class not found exception in setValue on " + internalType);
        } catch (SecurityException e) {
            throw new ConfigurationException("Security Exception in setValue on " + internalType);
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument issue when calling the constructor of the type " + internalType);
        }
    }

    public String getName() {
        return m_name;
    }

    public String getField() {
        return m_field;
    }
    
    public String getType() {
        return m_type.getName();
    }

    /**
     * Gets the method name, 
     * <code>null</code> if no method.
     * @return the method name.
     */
    public String getMethod() {
        if (m_method == null) { return null; }
        return m_method.getMethod();
    }

    /**
     * Checks if the property has a method callback.
     * @return <code>true</code> if the property has a method.
     */
    public boolean hasMethod() {
        return m_method != null;
    }

    /**
     * Checks if the property has a field.
     * @return <code>true</code> if the property has a field.
     */
    public boolean hasField() {
        return m_field != null;
    }
    
    public synchronized Object getValue() {
        return m_value;
    }

    
    /**
     * Gets the NO VALUE Object.
     * This method returns the object to inject when the property
     * was not assigned to a value.
     * @param type the type of the value.
     * @return the object to inject when the property has no value.
     */
    private static Object getNoValue(Class type) {
        if (Boolean.TYPE.equals(type)) { return Boolean.FALSE; }
        if (Byte.TYPE.equals(type)) { return new Byte((byte) 0); }
        if (Short.TYPE.equals(type)) { return new Short((short) 0); }
        if (Integer.TYPE.equals(type)) { return new Integer(0); }
        if (Long.TYPE.equals(type)) { return new Long(0); }
        if (Float.TYPE.equals(type)) { return new Float(0); }
        if (Double.TYPE.equals(type)) { return new Double(0); }
        if (Character.TYPE.equals(type)) { return new Character((char) 0); }
        // If all other case, return null.
        return null;
    }
    
    /**
     * Sets the value of the property.
     * @param value the new value.
     */
    public void setValue(Object value) {
        synchronized (this) {
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
                    throw new ClassCastException("Incompatible type for the property " + m_name + " " + m_type.getName() + " expected, " 
                                                 + value.getClass() + " found");
                }
            }
            m_invoked = false;
        }
    }

    /**
     * Checks if the given value is assignable to the given type.
     * @param type the class of the type
     * @param value the object to check
     * @return <code>true</code> if the object is assignable in the property of type 'type'.
     */
    public static boolean isAssignable(Class type, Object value) {
        if (value == null || type.isInstance(value)) { // When the value is null, the assign works necessary.
            return true;
        } else if (type.isPrimitive()) {
            // Manage all boxing types.
            if (value instanceof Boolean && Boolean.TYPE.equals(type)) { return true; }
            if (value instanceof Byte && Byte.TYPE.equals(type)) { return true; }
            if (value instanceof Short && Short.TYPE.equals(type)) { return true; }
            if (value instanceof Integer && Integer.TYPE.equals(type)) { return true; }
            if (value instanceof Long && Long.TYPE.equals(type)) { return true; }
            if (value instanceof Float && Float.TYPE.equals(type)) { return true; }
            if (value instanceof Double && Double.TYPE.equals(type)) { return true; }
            if (value instanceof Character && Character.TYPE.equals(type)) { return true; }
            return false;
        } else {
            // Else return false.
            return false;
        }
    }

    /**
     * Creates an object of the given type with the given String value.
     * @param type the type of the returned object
     * @param strValue the String value.
     * @return the object of type 'type' created from the String 'value'
     * @throws ConfigurationException if the object cannot be created.
     */
    public static Object create(Class type, String strValue) throws ConfigurationException {
        if (Boolean.TYPE.equals(type)) {
            return Boolean.valueOf(strValue);
        }
        if (Byte.TYPE.equals(type)) { return new Byte(strValue); }
        if (Short.TYPE.equals(type)) { return new Short(strValue); }
        if (Integer.TYPE.equals(type)) { return new Integer(strValue); }
        if (Long.TYPE.equals(type)) { return new Long(strValue); }
        if (Float.TYPE.equals(type)) { return new Float(strValue); }
        if (Double.TYPE.equals(type)) { return new Double(strValue); }
        if (Character.TYPE.equals(type)) { return new Character(strValue.charAt(0)); }

        // Array :
        if (type.isArray()) {
            return createArrayObject(type.getComponentType(), ParseUtils.parseArrays(strValue)); 
        }
        // Else it is a neither a primitive type neither a String -> create
        // the object by calling a constructor with a string in argument.
        try {
            Constructor cst = type.getConstructor(new Class[] { String.class });
            return cst.newInstance(new Object[] { strValue });
        } catch (SecurityException e) {
            throw new ConfigurationException("Security exception during the creation of " + type + " : " + e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Constructor not found exception during the creation of " + type + " : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument issue when calling the constructor of the type " + type);
        } catch (InstantiationException e) {
            throw new ConfigurationException("Instantiation problem  " + type);
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Illegal Access " + type);
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("Invocation problem during the creation of " + type + " : " + e.getTargetException().getMessage());
        }

    }

    /**
     * Creates an array object containing the type component type from 
     * the String array 'values'.
     * @param interntype the internal type of the array.
     * @param values the String array
     * @return the array containing objects created from the 'values' array
     * @throws ConfigurationException if the array cannot be created correctly
     */
    public static Object createArrayObject(Class interntype, String[] values) throws ConfigurationException {
        if (Boolean.TYPE.equals(interntype)) {
            boolean[] bool = new boolean[values.length];
            for (int i = 0; i < values.length; i++) {
                bool[i] = Boolean.valueOf(values[i]).booleanValue();
            }
            return bool;
        }
        if (Byte.TYPE.equals(interntype)) {
            byte[] byt = new byte[values.length];
            for (int i = 0; i < values.length; i++) {
                byt[i] = new Byte(values[i]).byteValue();
            }
            return byt;
        }
        if (Short.TYPE.equals(interntype)) {
            short[] shor = new short[values.length];
            for (int i = 0; i < values.length; i++) {
                shor[i] = new Short(values[i]).shortValue();
            }
            return shor;
        }
        if (Integer.TYPE.equals(interntype)) {
            int[] ints = new int[values.length];
            for (int i = 0; i < values.length; i++) {
                ints[i] = new Integer(values[i]).intValue();
            }
            return ints;
        }
        if (Long.TYPE.equals(interntype)) {
            long[] longs = new long[values.length];
            for (int i = 0; i < values.length; i++) {
                longs[i] = new Long(values[i]).longValue();
            }
            return longs;
        }
        if (Float.TYPE.equals(interntype)) {
            float[] floats = new float[values.length];
            for (int i = 0; i < values.length; i++) {
                floats[i] = new Float(values[i]).floatValue();
            }
            return floats;
        }
        if (Double.TYPE.equals(interntype)) {
            double[] doubles = new double[values.length];
            for (int i = 0; i < values.length; i++) {
                doubles[i] = new Double(values[i]).doubleValue();
            }
            return doubles;
        }
        if (Character.TYPE.equals(interntype)) {
            char[] chars = new char[values.length];
            for (int i = 0; i < values.length; i++) {
                chars[i] = values[i].toCharArray()[0];
            }
            return chars;
        }

        // Else it is a neither a primitive type -> create the
        // object by calling a constructor with a string in argument.
        try {
            Constructor cst = interntype.getConstructor(new Class[] { String.class });
            Object[] object = (Object[]) Array.newInstance(interntype, values.length);
            for (int i = 0; i < values.length; i++) {
                object[i] = cst.newInstance(new Object[] { values[i].trim() });
            }
            return object;
        } catch (NoSuchMethodException e) {
            throw new ConfigurationException("Constructor not found exception in setValue on " + interntype.getName());
        } catch (IllegalArgumentException e) {
            throw new ConfigurationException("Argument issue when calling the constructor of the type " + interntype.getName());
        } catch (InstantiationException e) {
            throw new ConfigurationException("Instantiation problem  " + interntype.getName());
        } catch (IllegalAccessException e) {
            throw new ConfigurationException("Illegal Access Exception in  " + interntype.getName());
        } catch (InvocationTargetException e) {
            throw new ConfigurationException("Invocation problem " + interntype.getName() + " : " + e.getTargetException().getMessage());
        }
    }

    /**
     * Invokes the setter method on the given pojo object. 
     * If no specified pojo object, it calls on each created pojo object.
     * @param instance the created object (could be <code>null</code>)
     * @see org.apache.felix.ipojo.Handler#onCreation(java.lang.Object)
     */
    public synchronized void invoke(Object instance) {
        if (m_invoked) {
            return; // Already called.
        }
        
        if (m_value == NO_VALUE) {
            // Don't call method if no value
            return;
        }
        
        try {
            if (instance == null) {
                m_method.call(new Object[] { m_value });
            } else {
                m_method.call(instance, new Object[] { m_value });
            }
            m_invoked = true;
        } catch (NoSuchMethodException e) {
            m_handler.error("The method " + m_method + " does not exist in the implementation class " + m_manager.getClassName());
            m_manager.stop();
        } catch (IllegalAccessException e) {
            m_handler.error("The method " + m_method + " is not accessible in the implementation class " + m_manager.getClassName());
            m_manager.stop();
        } catch (InvocationTargetException e) {
            m_handler.error("The method " + m_method + " in the implementation class " + m_manager.getClassName() + "throws an exception : " + e.getTargetException().getMessage(), e.getTargetException());
            m_manager.setState(ComponentInstance.INVALID);
        }
    }

    /**
     * A field value is required by the object 'pojo'.
     * @param pojo the POJO object
     * @param fieldName the field
     * @param value the last value
     * @return the value if the handler want to inject this value.
     * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public synchronized Object onGet(Object pojo, String fieldName, Object value) {
        if (m_value  == NO_VALUE) {
            return getNoValue(m_type);
        }
        return m_value;
    }

    /**
     * The field 'field' receives a new value.
     * @param pojo the pojo
     * @param fieldName the field name
     * @param value the new value
     * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public synchronized void onSet(Object pojo, String fieldName, Object value) {
        if (m_value == null || ! m_value.equals(value)) {
            setValue(value);
        }
    }
    
    /**
     * Gets the handler managing the property.
     * @return the configuration handler.
     */
    public Handler getHandler() {
        return m_handler;
    }
}
