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
 * A callback allows invoking a method on a POJO.
 * This class supports both public, protected and private methods of the
 * implementation class. This class also supports public method from super class.
 * The {@link Method} object is computed once and this computation is delayed
 * to the first invocation.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Callback {

    /**
     * The method object.
     * Computed at the first call.
     */
    protected Method m_methodObj;

    /**
     * The name of the method to call.
     */
    private String m_method;

    /**
     * Is the method a static method ?
     * This implies calling the method on <code>null</code>
     */
    private boolean m_isStatic;

    /**
     * The reference on the instance manager.
     * Used to get POJO objects.
     */
    private InstanceManager m_manager;

    /**
     * The argument classes.
     * This array contains the list of argument class names.
     */
    private String[] m_args;

    /**
     * Creates a Callback.
     * If the argument array is not null the reflection type are computed.
     * @see {@link Callback#computeArguments(String[])}
     * @param method the name of the method to call
     * @param args the argument type name, or <code>null</code> if no arguments
     * @param isStatic is the method a static method
     * @param manager the instance manager of the component containing the method
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
     * Creates a Callback.
     * @param method the the name of the method to call
     * @param args the argument classes
     * @param isStatic the is the method a static method
     * @param manager the the instance manager of the component containing the method
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
     * Creates a Callback.
     * @param method the {@link MethodMetadata} obtained from manipulation
     * metadata ({@link PojoMetadata}).
     * @param manager the instance manager.
     */
    public Callback(MethodMetadata method, InstanceManager manager) {
        m_isStatic = false;
        m_method = method.getMethodName();
        m_manager = manager;
        computeArguments(method.getMethodArguments());
    }

    /**
     * Computes arguments of the method.
     * This method computes "reflection type" from given argument.
     * @see FieldMetadata#getReflectionType(String)
     * @param args the arguments of the method.
     */
    private void computeArguments(String[] args) {
        m_args = new String[args.length];
        for (int i = 0; i < args.length; i++) {
            m_args[i] = FieldMetadata.getReflectionType(args[i]);
        }
    }

    /**
     * Searches the {@link Method} in the given method arrays.
     * @param methods the method array in which the method should be found
     * @return the method object or <code>null</code> if not found
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
     * Searches the {@link Method} object in the POJO by analyzing implementation
     * class methods. The name of the method and the argument type are checked.
     * @throws NoSuchMethodException if the method cannot be found either in the
     * implementation class or in parent classes.
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
     * Invokes the method without arguments.
     * If several the instance contains several objects, the method is invoked
     * on every objects.
     * @return the result of the invocation, <code>null</code> for <code>void</code>
     * method, the last result for multi-object instance
     * @throws NoSuchMethodException if Method is not found in the class
     * @throws InvocationTargetException if the method throws an exception
     * @throws IllegalAccessException if the method can not be invoked
     */
    public Object call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return call(new Object[0]);
    }

    /**
     * Invokes the method without arguments.
     * The method is invokes on the specified object.
     * @param instance the instance on which call the callback
     * @return the result of the invocation, <code>null</code> for
     * <code>void</code> method
     * @throws NoSuchMethodException if the method was not found
     * @throws IllegalAccessException if the method cannot be called
     * @throws InvocationTargetException if an error happens in the method
     */
    public Object call(Object instance) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return call(instance, new Object[0]);
    }

    /**
     * Invokes the method on every created objects with the specified
     * arguments.
     * @param arg the method arguments
     * @return the result of the invocation, <code>null</code> for 
     * <code>void</code> method, the last result for instance containing
     * several objects.
     * @throws NoSuchMethodException if the callback method is not found
     * @throws IllegalAccessException if the callback method cannot be called
     * @throws InvocationTargetException if an error is thrown by the called method
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
                return  m_methodObj.invoke(m_manager.getPojoObject(), arg);
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
     * Invokes the method on the given object with the specified
     * arguments.
     * @param instance the instance on which call the method
     * @param arg the argument array
     * @return the result of the invocation, <code>null</code> for 
     * <code>void</code> method
     * @throws NoSuchMethodException if the callback method is not found
     * @throws IllegalAccessException if the callback method cannot be called
     * @throws InvocationTargetException if an error is thrown by the called method
     */
    public Object call(Object instance, Object[] arg) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }

        return m_methodObj.invoke(instance, arg);
    }

    /**
     * Gets the method name.
     * @return the method name
     */
    public String getMethod() {
        return m_method;
    }
}
