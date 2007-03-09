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

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.ServiceReference;


/**
 * This class allwos the creation of callback when service dependency arrives or disappear.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
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
     * Callback method name.
     */
    private String m_callback;
    
    /**
     * Is the callback a static callback.
     */
    private boolean m_isStatic;
    
    /**
     * The instance manager.
     */
    private InstanceManager m_manager;

    /**
     * Constructor.
     * @param dep : the dependency attached to this depednency callback
     * @param method : the method to call
     * @param methodType : is the method to call a bind method or an unbind method
     * @param isStatic : is the method to call static ?
     */
    public DependencyCallback(Dependency dep, String method, int methodType, boolean isStatic) {
        m_methodType = methodType;
        m_callback = method;
        m_isStatic = isStatic;
        m_manager = dep.getDependencyHandler().getInstanceManager();
    }

    /**
     * @return the method type.
     */
    public int getMethodType() { return m_methodType; }

    /**
     * Call the callback method.
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	// Get the method object
    	Method method = m_manager.getClazz().getDeclaredMethod(m_callback, new Class[] {});
    	method.setAccessible(true);
    	
        if (m_isStatic) { 
        	method.invoke(null, new Object[] {}); 
        } else {
            // Two cases :
            // - if instances already exists : call on each instances
            // - if no instance exists : create an instance
            if (m_manager.getPojoObjects().length == 0) {
                m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] Create the first instance " + m_manager.getPojoObject());
                method.invoke(m_manager.getPojoObject(), new Object[]{});
            } else {
                for (int i = 0; i < m_manager.getPojoObjects().length; i++) {
                    method.invoke(m_manager.getPojoObjects()[i], new Object[] {});
                }
            }
        }
    }

    /**
     * Call the callback method with a service reference.
     * @param ref : the service reference to send to the method
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call(ServiceReference ref) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	// Get the method object
    	Method method = m_manager.getClazz().getDeclaredMethod(m_callback, new Class[] {ServiceReference.class});
    	method.setAccessible(true);
    	
        if (m_isStatic) { 
        	method.invoke(null, new Object[] {ref}); 
        } else {
            // Two cases :
            // - if instances already exists : call on each instances
            // - if no instance exists : create an instance
            if (m_manager.getPojoObjects().length == 0) {
                m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] Create the first instance " + m_manager.getPojoObject());
                method.invoke(m_manager.getPojoObject(), new Object[]{ref});
            } else {
                for (int i = 0; i < m_manager.getPojoObjects().length; i++) {
                    method.invoke(m_manager.getPojoObjects()[i], new Object[] {ref});
                }
            }
        }
    }

    /**
     * Call the callback method with an object.
     * @param o : the object to send to the method
     * @throws NoSuchMethodException : Method is not found in the class
     * @throws InvocationTargetException : The method is not static
     * @throws IllegalAccessException : The method can not be invoked
     */
    protected void call(Object o) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	// Get the method object
    	Method method = m_manager.getClazz().getDeclaredMethod(m_callback, new Class[] {Object.class});
    	method.setAccessible(true);
    	
        if (m_isStatic) { 
        	method.invoke(null, new Object[] {o}); 
        } else {
            // Two cases :
            // - if instances already exists : call on each instances
            // - if no instance exists : create an instance
            if (m_manager.getPojoObjects().length == 0) {
                m_manager.getFactory().getLogger().log(Logger.INFO, "[" + m_manager.getClassName() + "] Create the first instance " + m_manager.getPojoObject());
                method.invoke(m_manager.getPojoObject(), new Object[]{o});
            } else {
                for (int i = 0; i < m_manager.getPojoObjects().length; i++) {
                    method.invoke(m_manager.getPojoObjects()[i], new Object[] {o});
                }
            }
        }
    }

    /**
     * Call the callback on the given instance with no parameters.
     * @param instance : the instance on which call the callback
     * @throws NoSuchMethodException : the method is not found
     * @throws IllegalAccessException : the method could not be called
     * @throws InvocationTargetException : an error happens in the called method
     */
    protected void callOnInstance(Object instance) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException { 
    	Method method = instance.getClass().getDeclaredMethod(m_callback, new Class[] {});
    	method.setAccessible(true);
    	method.invoke(instance, new Object[] {});
   	}

    /**
     * Call the callback on the given instance with the given argument.
     * @param instance : the instance on which call the callback
     * @param ref : the service reference to send to the callback
     * @throws NoSuchMethodException : the method is not found
     * @throws IllegalAccessException : the method could not be called
     * @throws InvocationTargetException : an error happens in the called method
     * @throws InvocationTargetException
     */
    protected void callOnInstance(Object instance, ServiceReference ref) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	Method method = instance.getClass().getDeclaredMethod(m_callback, new Class[] {ServiceReference.class});
    	method.setAccessible(true);
    	method.invoke(instance, new Object[] {ref});
    }
    
    /**
     * Call the callback on the given instance with the given argument.
     * @param instance : the instance on which call the callback
     * @param o : the service object to send to the callback
     * @throws NoSuchMethodException : the method is not found
     * @throws IllegalAccessException : the method could not be called
     * @throws InvocationTargetException : an error happens in the called method
     * @throws InvocationTargetException
     */
    protected void callOnInstance(Object instance, Object o) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
    	Method method = instance.getClass().getDeclaredMethod(m_callback, new Class[] {Object.class});
    	method.setAccessible(true);
    	method.invoke(instance, new Object[] {o});
    }
}
