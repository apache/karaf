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

import java.util.Dictionary;

import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;

/**
 * Handler Abstract Class.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class Handler {
    
    /**
     * Handler namespace property.
     */
    public static final String HANDLER_NAMESPACE_PROPERTY = "handler.namespace";
    
    /**
     * Handler name property. 
     */
    public static final String HANDLER_NAME_PROPERTY = "handler.name";
    
    /**
     * Handler type property. 
     */
    public static final String HANDLER_TYPE_PROPERTY = "handler.type";

    /**
     * Handler priority.
     */
    public static final String HANDLER_LEVEL_PROPERTY = "handler.level";
    
    /**
     * Log method.
     * @param level : message level (Logger class constant)
     * @param message : message to log
     */
    public abstract void log(int level, String message);
    
    /**
     * Log method.
     * @param level : message level (Logger class constant)
     * @param message : message to log
     * @param ex : exception to attach to the message
     */
    public abstract void log(int level, String message, Throwable ex);
    
    /**
     * Get a plugged handler of the same container.
     * This method must be call only in the start method (or after). 
     * In the configure method, this method can not return a consistent
     * result as all handlers are not plugged. 
     * @param name : name of the handler to find (class name or qualified handler name (ns:name)). 
     * @return the handler object or null if the handler is not found.
     */
    public abstract Handler getHandler(String name);
    
    /**
     * Attach the current handler object to the given component instance.
     * An attached handler becomes a part of the instance container.
     * @param ci : the component instance on which the current handler will be attached.
     */
    protected abstract void attach(ComponentInstance ci);
    
    /**
     * Check if the current handler is valid.
     * This method must not be override.
     * @return true if the handler is valid.
     */
    public final boolean isValid() {
        if (this instanceof Pojo) {
            return ((Pojo) this).getComponentInstance().getState() == ComponentInstance.VALID;
        } else {
            log(Logger.ERROR, "The handler is not a POJO : " + this.getClass().getName());
            return false;
        }
    }
    
    
    /**
     * Get the component instance of the current handler.
     * @return : the component instance.
     */
    public final ComponentInstance getInstance() {
        if (this instanceof Pojo) {
            return ((Pojo) this).getComponentInstance();
        } else {
            log(Logger.ERROR, "The handler is not a POJO : " + this.getClass().getName());
            return null;
        }
    }
    
    /**
     * Initialize component factory.
     * This method aims to gather component factory properties. Each handler wanting to contribute need to override this 
     * method and add properties to the given component description.
     * @param cd : component description.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : if the metadata are not correct (early detection).
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) throws ConfigurationException {  }

    /**
     * Configure the handler.
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @throws ConfigurationException : if the metadata are not correct.
     */
    public abstract void configure(Element metadata, Dictionary configuration) throws ConfigurationException;

    /**
     * Stop the handler : stop the management.
     */
    public abstract void stop();

    /**
     * Start the handler : start the management.
     */
    public abstract void start();

    /**
     * This method is called when the component state changed.
     * @param state : the new state
     */
    public void stateChanged(int state) { }

    /**
     * Return the current handler description.
     * The simplest description contains only the name and the validity of the handler.
     * @return the description of the handler..
     */
    public HandlerDescription getDescription() {
        return new HandlerDescription(this);
    }

    /**
     * The instance is reconfiguring.
     * @param configuration : New instance configuration.
     */
    public void reconfigure(Dictionary configuration) { }
}
