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

import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;

/**
* The handler manager manages an handler instance.
* 
* @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
*/
public class HandlerManager extends InstanceManager {
    
    /**
     * Handler object (contained).
     */
    private Handler m_handler;

    /**
     * Constructor.
     * @param factory : handler factory
     * @param bc : bundle context
     * @param handlers : handler array
     */
    public HandlerManager(ComponentFactory factory, BundleContext bc, HandlerManager[] handlers) {
        super(factory, bc, handlers);
    }
    
    /**
     * Get the contained handler object.
     * If not already created it creates the object.
     * @return the handler object.
     */
    public Handler getHandler() {
        if (m_handler == null) {
            createHandlerObject();
        }
        return m_handler;
    }
    
    /**
     * Create and initialize the handler object.
     * @param ci : component instance on which the handler will be attached.
     * @param cm : component metadata.
     * @param configuration : instance configuration.
     * @throws ConfigurationException if the handler configuration failed.
     */
    public void init(ComponentInstance ci, Element cm, Dictionary configuration) throws ConfigurationException {
        createHandlerObject();
        m_handler.attach(ci);
        m_handler.configure(cm, configuration);
    }
    
    /**
     * Create the handler object.
     * This method does nothing if the object is already created.
     */
    private void createHandlerObject() {
        if (m_handler != null) { return; }
        Handler h  = (Handler) createPojoObject();
        if (h instanceof PrimitiveHandler) {
            m_handler = (PrimitiveHandler) h;
        } else {
            m_handler = (CompositeHandler) h;
        }
    }
    
    /**
     * Start the instance manager.
     */
    public synchronized void start() {
        if (m_state != STOPPED) {
            return;
        } // Instance already started

        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].addInstanceStateListener(this);
            m_handlers[i].start();
        }
        
        m_handler.start(); // Call the handler start method.
        
        for (int i = 0; i < m_handlers.length; i++) {
            if (! m_handlers[i].getHandler().isValid()) {
                setState(INVALID);
                return;
            }
        }
        setState(VALID);
    }
    
    /**
     * Stop the instance manager.
     */
    public synchronized void stop() {
        if (m_state == STOPPED) {
            return;
        } // Instance already stopped
        

        setState(INVALID);
        
        m_handler.stop();
        
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].removeInstanceStateListener(this);
            m_handlers[i].stop();
        }

        m_state = STOPPED;
        for (int i = 0; i < m_instanceListeners.size(); i++) {
            ((InstanceStateListener) m_instanceListeners.get(i)).stateChanged(this, STOPPED);
        }
    }
    
    /** 
     * Dispose the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public synchronized void dispose() {
        super.dispose();
        m_handler = null;
    }
    
    /**
     * Kill the current instance.
     * Only the factory of this instance can call this method.
     */
    protected void kill() {
        super.dispose();
        m_handler = null;
    }

}
