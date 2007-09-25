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
 * Composite Handler Abstract Class. An composite handler need implements these
 * method to be notified of lifecycle change...
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class CompositeHandler extends Handler {
    
    /**
     * Composite Handler type.
     */
    public static final String HANDLER_TYPE = "composite";
    
    /**
     * Reference on the composite manager.
     */
    private CompositeManager m_manager;
    
    /**
     * Set the manager.
     * This method me be called only once time.
     * @param cm : the composite manager.
     */
    public final void attach(ComponentInstance cm) {
        m_manager = (CompositeManager) cm;
    }
    
    public final CompositeManager getCompositeManager() {
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
     * @param name : name of the handler to find (class name). 
     * @return the composite handler object or null if the handler is not found.
     */
    public final Handler getHandler(String name) {
        return m_manager.getCompositeHandler(name);
    }
    
}
