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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.parser.MethodMetadata;

/**
 * A callback allows calling a method on the component instances.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Callback {

    /**
     * Name of the method to call.
     */
    private String m_method;

    /**
     * Is the method a static method ?
     */
    private boolean m_isStatic;

    /**
     * Reference on the instance manager.
     */
    private InstanceManager m_manager;
    
    /**
     * Method object.
     */
    private Method m_methodObj;
    
    /**
     * Argument classes.
     */
    private String[] m_args;

    /**
     * Callback constructor.
     * 
     * @param method : the name of the method to call
     * @param args : argument type name
     * @param isStatic : is the method a static method
     * @param im : the instance manager of the component containing the method
     */
    public Callback(String method, String[] args, boolean isStatic, InstanceManager im) {
        m_method = method;
        m_isStatic = isStatic;
        m_manager = im;
        m_args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            // Primitive Array 
            if (args[i].endsWith("[]") && args[i].indexOf(".") == -1) {
                String arr = "";
                for (int j = 0; j < args[i].length(); j++) {
                    if (args[i].charAt(j) == '[') { arr += '['; }
                }
                int index = args[i].indexOf('[');
                m_args[i] = arr + getInternalPrimitiveType(args[i].substring(0, index));
            }
            // Non-Primitive Array 
            if (args[i].endsWith("[]") && args[i].indexOf(".") != -1) {
                String arr = "";
                for (int j = 0; j < args[i].length(); j++) {
                    if (args[i].charAt(j) == '[') { arr += '['; }
                }
                int index = args[i].indexOf('[');
                m_args[i] = arr + "L" + args[i].substring(0, index) + ";";
            }
            // Simple type 
            if (!args[i].endsWith("[]")) {
                m_args[i] = args[i];
            }
            
        }
    }
    
    /**
     * Callback constructor.
     * 
     * @param method : the name of the method to call
     * @param args : argument classes
     * @param isStatic : is the method a static method
     * @param im : the instance manager of the component containing the method
     */
    public Callback(String method, Class[] args, boolean isStatic, InstanceManager im) {
        m_method = method;
        m_isStatic = isStatic;
        m_manager = im;
        m_args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            m_args[i] = args[i].getName();
        }
    }
    
    /**
     * Constructor.
     * @param mm : Method Metadata obtain form manipulation metadata.
     * @param im : instance manager.
     */
    public Callback(MethodMetadata mm, InstanceManager im) {
        m_isStatic = false;
        m_method = mm.getMethodName();
        m_manager = im;
        String[] args = mm.getMethodArguments();
        m_args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            // Primitive Array 
            if (args[i].endsWith("[]") && args[i].indexOf(".") == -1) {
                String arr = "";
                for (int j = 0; j < args[i].length(); j++) {
                    if (args[i].charAt(j) == '[') { arr += '['; }
                }
                int index = args[i].indexOf('[');
                m_args[i] = arr + getInternalPrimitiveType(args[i].substring(0, index));
            }
            // Non-Primitive Array 
            if (args[i].endsWith("[]") && args[i].indexOf(".") != -1) {
                String arr = "";
                for (int j = 0; j < args[i].length(); j++) {
                    if (args[i].charAt(j) == '[') { arr += '['; }
                }
                int index = args[i].indexOf('[');
                m_args[i] = arr + "L" + args[i].substring(0, index) + ";";
            }
            // Simple type 
            if (!args[i].endsWith("[]")) {
                m_args[i] = args[i];
            }
        }
    }

    /**
     * Get the internal notation for primitive type.
     * @param string : Stringform of the type
     * @return the internal notation or null if not found
     */
    private String getInternalPrimitiveType(String string) {
        if (string.equalsIgnoreCase("boolean")) {
            return "Z";
        }
        if (string.equalsIgnoreCase("char")) {
            return "C";
        }
        if (string.equalsIgnoreCase("byte")) {
            return "B";
        }
        if (string.equalsIgnoreCase("short")) {
            return "S";
        }
        if (string.equalsIgnoreCase("int")) {
            return "I";
        }
        if (string.equalsIgnoreCase("float")) {
            return "F";
        }
        if (string.equalsIgnoreCase("long")) {
            return "J";
        }
        if (string.equalsIgnoreCase("double")) {
            return "D";
        }
        return null;
    }
    
    /**
     * Search the method object in the POJO by analyzing present method.
     * The name of the maethod and the argument type are checked.
     */
    private void searchMethod() {
        Method[] methods = m_manager.getClazz().getDeclaredMethods();
        for (int i = 0; m_methodObj == null && i < methods.length; i++) {
            // First check the method name
            if (methods[i].getName().equals(m_method)) {
                // Check arguments
                Class[] clazzes = methods[i].getParameterTypes();
                if (clazzes.length == m_args.length) { // Test size to avoid useless loop
                    boolean ok = true;
                    for (int j = 0; ok && j < m_args.length; j++) {
                        if (!m_args[j].equals(clazzes[j].getName())) {
                            ok = false;
                        }
                    }
                    if (ok) {
                        m_methodObj = methods[i]; // It is the looked method.
                    } 
                }

            }
        }
        if (m_methodObj == null) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "The method " + m_method + " is not found in the code");
            return;
        } else {
            m_methodObj.setAccessible(true);
        }
    }

    /**
     * Call the method.
     * 
     * @return the result of the invocation, null for void method, the last result for multi-object instance
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    public Object call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

        if (m_isStatic) {
            return m_methodObj.invoke(null, new Object[] {});
        } else {
            // Two cases :
            // - if instances already exists : call on each instances
            // - if no instance exists : create an instance
            if (m_manager.getPojoObjects().length == 0) {
                return m_methodObj.invoke(m_manager.getPojoObject(), new Object[] {});
            } else {
                Object r = null;
                for (int i = 0; i < m_manager.getPojoObjects().length; i++) {
                    r = m_methodObj.invoke(m_manager.getPojoObjects()[i], new Object[] {});
                }
                return r;
            }
        }
    }

    /**
     * Call the current callback method on the instance given in parameter.
     * 
     * @param instance : instance on which call the callback
     * @return the result of the invocation, null for void method
     * @throws NoSuchMethodException : the method was not found
     * @throws IllegalAccessException : the method cannont be called
     * @throws InvocationTargetException : an error happens in the method
     */
    public Object call(Object instance) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }
        return m_methodObj.invoke(instance, new Object[] {});
    }

    /**
     * Call the callback on the method with the argument given in parameter.
     * 
     * @param arg : the parameters
     * @return the result of the invocation, null for void method, the last result for multi-object instance
     * @throws NoSuchMethodException : the callback method is not found
     * @throws IllegalAccessException : the callbback method cannot be called
     * @throws InvocationTargetException : an error occurs inside the called
     * method
     */
    public Object call(Object[] arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }
        
        if (m_isStatic) {
            return m_methodObj.invoke(null, arg);
        } else {
            // Two cases :
            // - if instances already exists : call on each instances
            // - if no instance exists : create an instance
            if (m_manager.getPojoObjects().length == 0) {
                return m_methodObj.invoke(m_manager.getPojoObject(), arg);
            } else {
                Object r = null;
                for (int i = 0; i < m_manager.getPojoObjects().length; i++) {
                    r = m_methodObj.invoke(m_manager.getPojoObjects()[i], arg);
                }
                return r;
            }
        }
    }

    /**
     * Call the callback on the method with the argument given in parameter and
     * with the arguments given in parameter too.
     * 
     * @param instance : instance on which call the callback
     * @param arg : the argument array
     * @return the result of the invocation, null for void method
     * @throws NoSuchMethodException : the callback method is not found
     * @throws IllegalAccessException : the callbback method cannot be called
     * @throws InvocationTargetException : an error occurs inside the called
     * method
     */
    public Object call(Object instance, Object[] arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }
        
        return m_methodObj.invoke(instance, arg);
    }
}
