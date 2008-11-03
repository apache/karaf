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

import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;

/**
 * Handler Abstract Class.
 * A handler is a 'piece' of 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class Handler {

    /**
     * The handler namespace property.
     */
    public static final String HANDLER_NAMESPACE_PROPERTY = "handler.namespace";

    /**
     * The handler name property. 
     */
    public static final String HANDLER_NAME_PROPERTY = "handler.name";

    /**
     * The handler type property. 
     */
    public static final String HANDLER_TYPE_PROPERTY = "handler.type";

    /**
     * The handler priority.
     */
    public static final String HANDLER_LEVEL_PROPERTY = "handler.level";

    /**
     * The current handler validity.
     * This impacts directly the instance state.
     */
    protected boolean m_isValid = true;

    /**
     * The HandlerManager managing the current handler.
     */
    protected HandlerManager m_instance;

    /**
     * Sets the factory attached to this handler object.
     * This method must be override to depend on each component factory type. 
     * @param factory the factory to attach.
     */
    public abstract void setFactory(Factory factory);

    /**
     * Gets the logger to use in the handler.
     * This method must be override to depend on each component factory type logging policy.
     * @return the logger.
     */
    public abstract Logger getLogger();

    /**
     * Log method (warning).
     * Log a warning message to the handler logger.
     * @param message the message to log
     */
    public final void warn(String message) {
        getLogger().log(Logger.WARNING, message);
    }

    /**
     * Log method (error).
     * Log an error message to the handler logger.
     * @param message the message to log
     */
    public final void error(String message) {
        getLogger().log(Logger.ERROR, message);
    }

    /**
     * Log method (info).     
     * Log an info message to the handler logger.
     * @param message the message to log
     */
    public final void info(String message) {
        getLogger().log(Logger.INFO, message);
    }
    
    /**
     * Log method (debug).     
     * Log a debug message to the handler logger.
     * @param message the message to log
     */
    public final void debug(String message) {
        getLogger().log(Logger.DEBUG, message);
    }

    /**
     * Log method (warning).
     * Log a warning message to the handler logger.
     * The message is sent with an attached exception.
     * @param message the message to log
     * @param exception the exception to attach with the message
     */
    public final void warn(String message, Throwable exception) {
        getLogger().log(Logger.WARNING, message, exception);
    }

    /**
     * Log method (error).
     * Log an error message to the handler logger.
     * The message is sent with an attached exception.
     * @param message the message to log
     * @param exception the exception to attach to the message
     */
    public final void error(String message, Throwable exception) {
        getLogger().log(Logger.ERROR, message, exception);
    }

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
     * Attaches the current handler object to the given component instance.
     * An attached handler becomes a part of the instance container.
     * @param instance the component instance on which the current handler will be attached.
     */
    protected abstract void attach(ComponentInstance instance);

    /**
     * Checks if the current handler is valid.
     * This check tests the handler validity.
     * This method must not be override.
     * @return <code>true</code> if the handler is valid.
     */
    public final boolean isValid() {
        return ((Pojo) this).getComponentInstance().getState() == ComponentInstance.VALID;
    }

    /**
     * Sets the validity of the current handler.
     * @param isValid if <code>true</code> the handler becomes valid, else it becomes invalid.
     */
    public final void setValidity(boolean isValid) {
        if (m_isValid != isValid) {
            m_isValid = isValid;
            HandlerManager instance = getHandlerManager();
            if (isValid) {
                instance.stateChanged(instance, ComponentInstance.VALID);
            } else {
                instance.stateChanged(instance, ComponentInstance.INVALID);
            }
        }
    }

    /**
     * Is the current handler valid.
     * @return <code>true</code> if the handler is valid, <code>false</code> otherwise.
     */
    public final boolean getValidity() {
        return m_isValid;
    }

    /**
     * Gets the component instance of the current handler.
     * @return the component instance.
     */
    public final HandlerManager getHandlerManager() {
        if (m_instance != null) { return m_instance; }
        m_instance = (HandlerManager) ((Pojo) this).getComponentInstance();
        return m_instance;
    }

    /**
     * Initializes the component factory.
     * This method aims to collect component factory properties. 
     * Each handler wanting to contribute needs to override this 
     * method and adds properties to the given component description.
     * By default, this method does nothing.
     * @param typeDesc the component description.
     * @param metadata the component type metadata.
     * @throws ConfigurationException if the metadata are not correct (early detection).
     */
    public void initializeComponentFactory(ComponentTypeDescription typeDesc, Element metadata) throws ConfigurationException {
        // The default implementation does nothing.
    }

    /**
     * Configures the handler.
     * @param metadata the metadata of the component
     * @param configuration the instance configuration
     * @throws ConfigurationException if the metadata are not correct.
     */
    public abstract void configure(Element metadata, Dictionary configuration) throws ConfigurationException;

    /**
     * Stops the handler
     * This method stops the management.
     */
    public abstract void stop();

    /**
     * Starts the handler
     * This method starts the management.
     */
    public abstract void start();

    /**
     * This method is called when the component state changed.
     * By default, this method does nothing.
     * @param state the new instance state {@link ComponentInstance}
     */
    public void stateChanged(int state) {
        // The default implementation does nothing.
    }

    /**
     * Returns the current handler description.
     * The simplest description contains only the name and the validity of the handler.
     * If the handler override this method, it can customize the description. 
     * By default, this method returns the simplest description.
     * @return the description of the handler.
     */
    public HandlerDescription getDescription() {
        return new HandlerDescription(this);
    }

    /**
     * Reconfigures the instance.
     * This method is called, when the instance is under reconfiguration.
     * The reconfiguration does not stops the instance, and so the handler supporting
     * the reconfiguration must override this method and handles this case. 
     * By default, this method does nothing.
     * @param configuration the new instance configuration.
     */
    public void reconfigure(Dictionary configuration) {
        // The default implementation does nothing.
    }
}
