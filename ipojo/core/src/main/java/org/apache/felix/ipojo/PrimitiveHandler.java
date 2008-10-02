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
package org.apache.felix.ipojo;

import java.lang.reflect.Method;

import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Logger;


/**
* This class defines common mechanisms of primitive handlers.
* Primitive handlers are handler composing the container of primitive
* component instances (declared by the 'component' element inside the
* iPOJO descriptor).
* Note that this class also defines default method implementation.
* Classes overriding this class can change the behavior of those methods.
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public abstract class PrimitiveHandler extends Handler implements FieldInterceptor, MethodInterceptor {
    
    /**
     * The "Primitive" Handler type (value).
     */
    public static final String HANDLER_TYPE = "primitive";
    
    /**
     * The reference on the instance manager.
     */
    private InstanceManager m_manager;
    
    
    /**
     * The factory of the instance manager. 
     */
    private ComponentFactory m_factory;
    
    /**
     * Attaches the current handler to the given instance.
     * @param manager the instance on which the current handler will be attached.
     * @see org.apache.felix.ipojo.Handler#attach(org.apache.felix.ipojo.ComponentInstance)
     */
    protected final void attach(ComponentInstance manager) {
        m_manager = (InstanceManager) manager;
    }
    
    /**
     * Sets the factory of the managed instance.
     * @param factory the factory 
     * @see org.apache.felix.ipojo.Handler#setFactory(org.apache.felix.ipojo.Factory)
     */
    public final void setFactory(Factory factory) {
        m_factory = (ComponentFactory) factory;
    }
    
    /**
     * gets the logger of the managed instance.
     * @return the logger to use to log messages.
     * @see org.apache.felix.ipojo.Handler#getLogger()
     */
    public Logger getLogger() {
        return m_factory.getLogger();
    }
    
    /**
     * Gets the instance manager managing the instance.
     * @return the instance manager
     */
    public InstanceManager getInstanceManager() {
        return m_manager;
    }
    
    /**
     * Gets the factory which creates the managed instance.
     * @return the factory which creates the managed instance.
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }
    
    /**
     * Gets the PojoMetadata of the content of the managed
     * instance. This method allows getting manipulation
     * metadata.
     * @return the information about the content of the
     * managed instance.
     */
    public PojoMetadata getPojoMetadata() {
        return m_factory.getPojoMetadata();
    }
    
    /**
     * Gets a plugged handler of the same container.
     * This method must be called only in the start method (or after). 
     * In the configure method, this method can be inconsistent
     * as all handlers are not initialized. 
     * @param name the name of the handler to find (class name or qualified
     * handler name (<code>ns:name</code>)). 
     * @return the handler object or <code>null</code> if the handler is not found.
     */
    public final Handler getHandler(String name) {
        return m_manager.getHandler(name);
    }
    
    /**
     * Callback method called when a managed field 
     * receives a new value. The given pojo can be
     * null if the new value is set by another handler.
     * This default implementation does nothing. 
     * @param pojo the pojo object setting the value
     * @param fieldName the field name
     * @param value the value received by the field
     * @see FieldInterceptor#onSet(Object, String, Object)
     */
    public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing to do in the default implementation
    }

    /**
     * Callback method called when a managed field
     * asks for a value.
     * The default implementation returned the previously
     * injected value.
     * @param pojo the pojo object requiring the value
     * @param fieldName the field name
     * @param value the value passed to the field (by the previous call)
     * @return the value to inject, of the previous value if the handler
     * don't want to inject a value.
     * @see FieldInterceptor#onGet(Object, String, Object)
     */
    public Object onGet(Object pojo, String fieldName, Object value) {
        return value;
    }
    
    /**
     * Callback method called when a method will be invoked.
     * This default implementation does nothing. 
     * @param pojo the pojo on which the method is called.
     * @param method the method invoked.
     * @param args the arguments array.
     * @see MethodInterceptor#onEntry(Object, Method, Object[])
     */
    public void onEntry(Object pojo, Method method, Object[] args) { 
        // Nothing to do in the default implementation
    }

    /**
     * Callback method called when a method ends.
     * This method is called when a thread exits a method (before a return or a throw).
     * If the given returned object is <code>null</code>, either the method is 
     * <code>void</code>, or it returns <code>null</code>.
     * You must not modified the returned object.
     * The default implementation does nothing.
     * @param pojo the pojo on which the method exits.
     * @param method the exiting method.
     * @param returnedObj the returned object (boxed for primitive type)
     * @see MethodInterceptor#onExit(Object, Method, Object)
     */
    public void onExit(Object pojo, Method method, Object returnedObj) { 
        // Nothing to do in the default implementation
    }
    
    /**
     * Callback method called when an error occurs.
     * This method is called when the execution throws an exception 
     * in the given method.
     * The default implementation does nothing.
     * @param pojo the pojo on which the method was accessed.
     * @param method the invoked method.
     * @param throwable the thrown exception
     * @see org.apache.felix.ipojo.MethodInterceptor#onError(java.lang.Object, java.lang.reflect.Method, java.lang.Throwable)
     */
    public void onError(Object pojo, Method method, Throwable throwable) {
        // Nothing to do in the default implementation
    }
    
    /**
     * Callback method called when the execution of a method will terminate : 
     * just before to throw an exception or before to return.
     * {@link MethodInterceptor#onExit(Object, Method, Object)} or
     * {@link MethodInterceptor#onError(Object, Method, Throwable)} 
     * were already called.
     * This default implementation does nothing.
     * @param pojo the pojo on which the method was accessed.
     * @param method the invoked method.
     */
    public void onFinally(Object pojo, Method method) {
        // Nothing to do in the default implementation
    }
    
    /**
     * Callback method called when an instance of the component is created, but
     * before someone can use it.
     * The default implementation does nothing.
     * @param instance the created instance
     */
    public void onCreation(Object instance) { 
        // Nothing to do in the default implementation
    }
    
    

}
