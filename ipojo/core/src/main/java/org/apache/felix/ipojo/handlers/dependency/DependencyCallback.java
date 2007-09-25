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

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Callback;
import org.osgi.framework.ServiceReference;

/**
 * This class allwos the creation of callback when service dependency arrives or
 * disappear.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class DependencyCallback {

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
     * Argument of the callback.
     */
    private String m_argument;

    /**
     * Callback method name.
     */
    private String m_method;

    /**
     * Is the callback a static callback.
     */
    private Callback m_callback;

    /**
     * The instance manager.
     */
    private InstanceManager m_manager;

    /**
     * Constructor.
     * 
     * @param dep : the dependency attached to this depednency callback
     * @param method : the method to call
     * @param methodType : is the method to call a bind method or an unbind
     * method
     */
    public DependencyCallback(Dependency dep, String method, int methodType) {
        m_methodType = methodType;
        m_method = method;
        m_manager = dep.getDependencyHandler().getInstanceManager();
    }


    public int getMethodType() {
        return m_methodType;
    }
    
    public String getMethodName() {
        return m_method;
    }
    
    /**
     * Set the argument type (Empty or the class name).
     * @param arg : the type name or EMPTY
     */
    public void setArgument(String arg) {
        m_argument = arg;
        if ("EMPTY".equals(arg)) {
            m_callback = new Callback(m_method, new String[0], false, m_manager);
        } else {
            m_callback = new Callback(m_method, new String[] {arg}, false, m_manager);
        }
        
    }
    
    public String getArgument() {
        return m_argument;
    }

    /**
     * Call the callback method.
     * 
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        m_callback.call();
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
        if ("EMPTY".equals(m_argument)) {
            m_callback.call(new Object[] {});
        } else {
            if (m_argument.equals(ServiceReference.class.getName())) {
                m_callback.call(new Object[] {ref});
            } else {
                m_callback.call(new Object[] {obj});
            }
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
     * @throws InvocationTargetException
     */
    protected void callOnInstance(Object instance, ServiceReference ref, Object obj) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        if ("EMPTY".equals(m_argument)) {
            m_callback.call(instance, new Object[] {});
            return;
        }
        if (m_argument.equals(ServiceReference.class.getName())) {
            m_callback.call(instance, new Object[] {ref});
        } else {
            m_callback.call(instance, new Object[] {obj});
        }
    }

    /**
     * Call the callback on the given instance with the given argument.
     * 
     * @param instance : the instance on which call the callback
     * @param o : the service object to send to the callback
     * @throws NoSuchMethodException : the method is not found
     * @throws IllegalAccessException : the method could not be called
     * @throws InvocationTargetException : an error happens in the called method
     * @throws InvocationTargetException
     */
    protected void callOnInstance(Object instance, Object o) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        m_callback.call(instance, new Object[] {o});
    }
}
