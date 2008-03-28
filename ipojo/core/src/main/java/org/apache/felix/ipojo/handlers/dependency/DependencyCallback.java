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
package org.apache.felix.ipojo.handlers.dependency;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.ServiceReference;

/**
 * This class allwos the creation of callback when service dependency arrives or
 * disappear.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyCallback extends Callback {

    /**
     * Bind method (called when a service arrives).
     */
    public static final int BIND = 0;

    /**
     * Unbind method (called when a service disappears).
     */
    public static final int UNBIND = 1;

    /**
     * Is the method a bind method or an unbind method ?
     */
    private int m_methodType;
    
    /**
     * Arguments of the callback.
     */
    private String[] m_argument;

    /**
     * Callback method name.
     */
    private String m_method;
    
    /**
     * Service Dependency. 
     */
    private Dependency m_dependency;

    /**
     * Constructor.
     * 
     * @param dep : the dependency attached to this dependency callback
     * @param method : the method to call
     * @param methodType : is the method to call a bind method or an unbind
     * method
     */
    public DependencyCallback(Dependency dep, String method, int methodType) {
        super(method, (String[]) null, false, dep.getHandler().getInstanceManager());
        m_methodType = methodType;
        m_dependency = dep;
        m_method = method;
    }


    public int getMethodType() {
        return m_methodType;
    }
    
    public String getMethodName() {
        return m_method;
    }
    
    /**
     * Set the argument type (Empty or the class name).
     * @param arg : the array of argument types.
     */
    public void setArgument(String[] arg) {
        m_argument = arg;
    }
    
    /**
     * Search the method object in the POJO by analyzing present method.
     * If not found in the pojo it tests the parent classes.
     * The name of the method and the argument type are checked.
     */
    protected void searchMethod() {
        if (m_argument != null) {
            Method[] methods = m_dependency.getHandler().getInstanceManager().getClazz().getDeclaredMethods();
            for (int i = 0; i < methods.length; i++) {
                // First check the method name
                if (methods[i].getName().equals(m_method)) {
                    // Check arguments
                    Class[] clazzes = methods[i].getParameterTypes();
                    if (clazzes.length == m_argument.length) { // Test size to avoid useless loop // NOPMD
                        int argIndex = 0;
                        for (; argIndex < m_argument.length; argIndex++) {
                            if (!m_argument[argIndex].equals(clazzes[argIndex].getName())) {
                                break;
                            }
                        }
                        if (argIndex == m_argument.length) { // If the array was completely read.
                            m_methodObj = methods[i]; // It is the looked method.
                            if (! m_methodObj.isAccessible()) { 
                                // If not accessible, try to set the accessibility.
                                m_methodObj.setAccessible(true);
                            }
                            return;
                        }
                    }

                }
            }
        }
        
        // Not found => Try parent method.
        searchParentMethod();
        
        if (m_methodObj == null) {
            // If not found, stop the instance (fatal error)
            m_dependency.getHandler().error("The method " + m_method + " cannot be called : method not found");
            m_dependency.getHandler().getInstanceManager().stop();
        } else {
            if (! m_methodObj.isAccessible()) { 
                // If not accessible, try to set the accessibility.
                m_methodObj.setAccessible(true);
            }
        }
    }
    
    /**
     * Introspect parent class to find the method.
     */
    private void searchParentMethod() {
        // look at parent classes
        Method[] methods = m_dependency.getHandler().getInstanceManager().getClazz().getMethods();
        for (int i = 0; i < methods.length; i++) {
            // First check the method name
            if (methods[i].getName().equals(m_method)) {
                // Check arguments
                Class[] clazzes = methods[i].getParameterTypes();
                switch (clazzes.length) {
                    case 0:
                        // Callback with no arguments.
                        m_methodObj = methods[i];
                        m_argument = new String[0];
                        return;
                    case 1:
                        if (clazzes[0].getName().equals(ServiceReference.class.getName())) {
                            // Callback with a service reference.
                            m_methodObj = methods[i];
                            m_argument = new String[] { ServiceReference.class.getName() };
                            return;
                        }
                        if (clazzes[0].getName().equals(m_dependency.getSpecification().getName())) {
                            // Callback with the service object.
                            m_methodObj = methods[i];
                            m_argument = new String[] { m_dependency.getSpecification().getName() };
                            return;
                        }
                        break;
                    case 2:
                        if (clazzes[0].getName().equals(m_dependency.getSpecification().getName()) && clazzes[1].getName().equals(ServiceReference.class.getName())) {
                            // Callback with two arguments.
                            m_methodObj = methods[i];
                            m_argument = new String[] { m_dependency.getSpecification().getName(), ServiceReference.class.getName() };
                            return;
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * Call the callback method with a service reference.
     * 
     * @param ref : the service reference to send to the method
     * @param obj : the service object
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call(ServiceReference ref, Object obj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }
        switch (m_argument.length) {
            case 0 :
                call(new Object[0]);
                break;
            case 1 : 
                if (m_argument[0].equals(ServiceReference.class.getName())) {
                    call(new Object[] {ref});
                } else {
                    call(new Object[] {obj});
                }
                break;
            case 2 :
                call(new Object[] {obj, ref});
                break;
            default : 
                break;
        }
    }

    /**
     * Call the callback on the given instance with the given argument.
     * 
     * @param instance : the instance on which call the callback
     * @param ref : the service reference to send to the callback
     * @param obj : the service object
     * @throws NoSuchMethodException : the method is not found
     * @throws IllegalAccessException : the method could not be called
     * @throws InvocationTargetException : an error happens in the called method
     */
    protected void callOnInstance(Object instance, ServiceReference ref, Object obj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if (m_methodObj == null) {
            searchMethod();
        }
        switch (m_argument.length) {
            case 0 :
                call(instance, new Object[0]);
                break;
            case 1 : 
                if (m_argument[0].equals(ServiceReference.class.getName())) {
                    call(instance, new Object[] {ref});
                } else {
                    call(instance, new Object[] {obj});
                }
                break;
            case 2 :
                call(instance, new Object[] {obj, ref});
                break;
            default : 
                break;
        }
    }
}
