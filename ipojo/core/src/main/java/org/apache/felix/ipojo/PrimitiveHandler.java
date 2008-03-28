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

import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.PojoMetadata;
import org.apache.felix.ipojo.util.Logger;



/**
* Abstract class to extends for primitive handler.
* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public abstract class PrimitiveHandler extends Handler implements FieldInterceptor, MethodInterceptor {
    
    /**
     * "Primitive" Handler type (value).
     */
    public static final String HANDLER_TYPE = "primitive";
    
    /**
     * Reference on the instance manager.
     */
    private InstanceManager m_manager;
    
    
    /**
     * Factory of the instance manager. 
     */
    private ComponentFactory m_factory;
    
    /**
     * Attach the current handler to the given instance.
     * @param manager ! the instance on which the current handler will be attached.
     * @see org.apache.felix.ipojo.Handler#attach(org.apache.felix.ipojo.ComponentInstance)
     */
    protected final void attach(ComponentInstance manager) {
        m_manager = (InstanceManager) manager;
    }
    
    public final void setFactory(Factory factory) {
        m_factory = (ComponentFactory) factory;
    }
    
    public Logger getLogger() {
        return m_factory.getLogger();
    }
    
    public InstanceManager getInstanceManager() {
        return m_manager;
    }
    
    public ComponentFactory getFactory() {
        return m_factory;
    }
    
    public Element[] getMetadata() {
        return null;
    }
    
    public PojoMetadata getPojoMetadata() {
        return m_factory.getPojoMetadata();
    }
    
    /**
     * Get a plugged handler of the same container.
     * This method must be call only in the start method (or after). 
     * In the configure method, this method can not return a consistent
     * result as all handlers are not plugged. 
     * @param name : name of the handler to find (class name or qualified handler name (ns:name)). 
     * @return the handler object or null if the handler is not found.
     */
    public final Handler getHandler(String name) {
        return m_manager.getHandler(name);
    }
    
    /**
     * This method is called when a PUTFIELD operation is detected.
     * @param pojo : the pojo object setting the value
     * @param fieldName : the field name
     * @param value : the value passed to the field
     */
    public void onSet(Object pojo, String fieldName, Object value) {
        // Nothing do do in the default implementation
    }

    /**
     * This method is called when a GETFIELD operation is detected.
     * @param pojo : the pojo object getting the value
     * @param fieldName : the field name
     * @param value : the value passed to the field (by the previous call)
     * @return : the managed value of the field
     */
    public Object onGet(Object pojo, String fieldName, Object value) {
        return value;
    }
    
    /**
     * This method is called when the execution enter in a method.
     * @param pojo : pojo on which the method is called.
     * @param method : method invoked.
     * @param args arguments array.
     */
    public void onEntry(Object pojo, Method method, Object[] args) { 
        // Nothing do do in the default implementation
    }

    /**
     * This method is called when the execution exit a method (before a return or a throw).
     * If the given returned object is null, either the method is void, either it returns null.
     * You must not modified the returned object.
     * @param pojo : the pojo on which the method exits.
     * @param method : exiting method.
     * @param returnedObj : the returned object (boxed for primitive type)
     */
    public void onExit(Object pojo, Method method, Object returnedObj) { 
        // Nothing do do in the default implementation
    }
    
    /**
     * This method is called when the execution throw an exception in the given method.
     * @param pojo : the pojo on which the method was accessed.
     * @param method : invoked method.
     * @param throwable : the thrown exception
     */
    public void onError(Object pojo, Method method, Throwable throwable) {
        // Nothing do do in the default implementation
    }
    
    /**
     * This method is called when the execution of a method will terminate : 
     * just before to throw an exception or before to return.
     * OnError or OnExit was already called.
     * @param pojo : the pojo on which the method was accessed.
     * @param method : invoked method.
     */
    public void onFinally(Object pojo, Method method) {
        // Nothing do do in the default implementation
    }
    
    /**
     * This method is called when an instance of the component is created, but
     * before someone can use it.
     * @param instance : the created instance
     */
    public void onCreation(Object instance) { 
        // Nothing do do in the default implementation
    }
    
    

}
