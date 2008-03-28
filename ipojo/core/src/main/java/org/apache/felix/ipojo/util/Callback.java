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
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;

/**
 * A callback allows calling a method on the component instances.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Callback {

    /**
     * Method object.
     */
    protected Method m_methodObj;

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
     * Argument classes.
     */
    private String[] m_args;

    /**
     * Callback constructor.
     * @param method : the name of the method to call
     * @param args : argument type name
     * @param isStatic : is the method a static method
     * @param manager : the instance manager of the component containing the method
     */
    public Callback(String method, String[] args, boolean isStatic, InstanceManager manager) {
        m_method = method;
        m_isStatic = isStatic;
        m_manager = manager;
        if (args != null) {
            computeArguments(args);
        }
    }

    /**
     * Callback constructor.
     * @param method : the name of the method to call
     * @param args : argument classes
     * @param isStatic : is the method a static method
     * @param manager : the instance manager of the component containing the method
     */
    public Callback(String method, Class[] args, boolean isStatic, InstanceManager manager) {
        m_method = method;
        m_isStatic = isStatic;
        m_manager = manager;
        m_args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            m_args[i] = args[i].getName();
        }
    }

    /**
     * Constructor.
     * @param method : Method Metadata obtain form manipulation metadata.
     * @param manager : instance manager.
     */
    public Callback(MethodMetadata method, InstanceManager manager) {
        m_isStatic = false;
        m_method = method.getMethodName();
        m_manager = manager;
        computeArguments(method.getMethodArguments());
    }

    /**
     * Compute arguments of the method.
     * @param args : arguments of the method.
     */
    private void computeArguments(String[] args) {
        m_args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            m_args[i] = FieldMetadata.getReflectionType(args[i]);
        }
    }

    /**
     * Search the looked method in the given method arrays.
     * @param methods : method array in which we need to look
     * @return the method object or null if not found
     */
    private Method searchMethodInMethodArray(Method[] methods) {
        for (int i = 0; i < methods.length; i++) {
            // First check the method name
            if (methods[i].getName().equals(m_method)) {
                // Check arguments
                Class[] clazzes = methods[i].getParameterTypes();
                if (clazzes.length == m_args.length) { // Test size to avoid useless loop
                    int argIndex = 0;
                    for (; argIndex < m_args.length; argIndex++) {
                        if (!m_args[argIndex].equals(clazzes[argIndex].getName())) {
                            break;
                        }
                    }
                    if (argIndex == m_args.length) { // No mismatch detected. 
                        return methods[i]; // It is the looked method.
                    }
                }
            }
        }
        return null;
    }

    /**
     * Search the method object in the POJO by analyzing present method. The name of the method and the argument type are checked.
     * @throws NoSuchMethodException : occurs when the method cannot be found either in the pojo class either in parent classes.
     */
    protected void searchMethod() throws NoSuchMethodException {
        Method[] methods = m_manager.getClazz().getDeclaredMethods();
        m_methodObj = searchMethodInMethodArray(methods);

        if (m_methodObj == null) { // look at parent classes
            methods = m_manager.getClazz().getMethods();
            m_methodObj = searchMethodInMethodArray(methods);
        }

        if (m_methodObj == null) {
            throw new NoSuchMethodException(m_method);
        } else {
            if (! m_methodObj.isAccessible()) { 
                // If not accessible, try to set the accessibility.
                m_methodObj.setAccessible(true);
            }
        }
    }

    /**
     * Call the method.
     * @return the result of the invocation, null for void method, the last result for multi-object instance
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    public Object call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return call(new Object[0]);
    }

    /**
     * Call the current callback method on the instance given in parameter.
     * @param instance : instance on which call the callback
     * @return the result of the invocation, null for void method
     * @throws NoSuchMethodException : the method was not found
     * @throws IllegalAccessException : the method cannot be called
     * @throws InvocationTargetException : an error happens in the method
     */
    public Object call(Object instance) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return call(instance, new Object[0]);
    }

    /**
     * Call the callback on the method with the argument given in parameter.
     * @param arg : the parameters
     * @return the result of the invocation, null for void method, the last result for multi-object instance
     * @throws NoSuchMethodException : the callback method is not found
     * @throws IllegalAccessException : the callback method cannot be called
     * @throws InvocationTargetException : an error occurs inside the called method
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
            if (m_manager.getPojoObjects() == null) {
                return m_methodObj.invoke(m_manager.getPojoObject(), arg);
            } else {
                Object newObject = null;
                for (int i = 0; i < m_manager.getPojoObjects().length; i++) {
                    newObject = m_methodObj.invoke(m_manager.getPojoObjects()[i], arg);
                }
                return newObject;
            }
        }
    }

    /**
     * Call the callback on the method with the argument given in parameter and with the arguments given in parameter too.
     * @param instance : instance on which call the callback
     * @param arg : the argument array
     * @return the result of the invocation, null for void method
     * @throws NoSuchMethodException : the callback method is not found
     * @throws IllegalAccessException : the callback method cannot be called
     * @throws InvocationTargetException : an error occurs inside the called method
     */
    public Object call(Object instance, Object[] arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

        return m_methodObj.invoke(instance, arg);
    }

    public String getMethod() {
        return m_method;
    }
}
