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

/**
* Abstract class to extends for primitive handler.
* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public abstract class PrimitiveHandler extends Handler {
    
    /**
     * "Primitive" Handler type (value).
     */
    public static final String HANDLER_TYPE = "primitive";
    
    /**
     * Reference on the instance manager.
     */
    private InstanceManager m_manager;
    
    /**
     * Attach the current handler to the given instance.
     * @param im ! the instance on which the current handler will be attached.
     * @see org.apache.felix.ipojo.Handler#attach(org.apache.felix.ipojo.ComponentInstance)
     */
    protected final void attach(ComponentInstance im) {
        m_manager = (InstanceManager) im;
    }
    
    public InstanceManager getInstanceManager() {
        return m_manager;
    }
    
    /**
     * Log method.
     * @param level : message level (Logger class constant)
     * @param message : message to log
     */
    public void log(int level, String message) {
        m_manager.getFactory().getLogger().log(level, message);
    }
    
    /**
     * Log method.
     * @param level : message level (Logger class constant)
     * @param message : message to log
     * @param ex : exception to attach to the message
     */
    public void log(int level, String message, Throwable ex) {
        m_manager.getFactory().getLogger().log(level, message, ex);
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
     * @param fieldName : the field name
     * @param value : the value passed to the field
     */
    public void setterCallback(String fieldName, Object value) { }

    /**
     * This method is called when a GETFIELD operation is detected.
     * @param fieldName : the field name
     * @param value : the value passed to the field (by the previous handler)
     * @return : the managed value of the field
     */
    public Object getterCallback(String fieldName, Object value) {
        return value;
    }
    
    /**
     * This method is called when the execution enter in a method.
     * @param methodId : the method identifier
     */
    public void entryCallback(String methodId) { }

    /**
     * This method is called when the execution exit a method (before a return or a throw).
     * If the given returned object is an instance of Exception, this means that the method throwed this exception.
     * If the given returned object is null, either the method is void, either it returns null.
     * You must not modified the returned object.
     * @param methodId : the method identifier
     * @param returnedObj : the returned object (boxed for primitive type)
     */
    public void exitCallback(String methodId, Object returnedObj) { }
    
    /**
     * This method is called when an instance of the component is created, but
     * before someone can use it.
     * @param instance : the created instance
     */
    public void objectCreated(Object instance) { }
    
    

}
