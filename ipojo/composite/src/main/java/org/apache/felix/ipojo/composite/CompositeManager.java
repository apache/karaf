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
package org.apache.felix.ipojo.composite;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ComponentFactory;
import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.InstanceStateListener;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * iPOJO Composite manager. The composite manager class manages one instance of
 * a component type which is a composition. It manages component lifecycle, and
 * handlers...
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class CompositeManager implements ComponentInstance, InstanceStateListener {

    /**
     * The context of the component.
     */
    private final BundleContext m_context;

    /**
     * Parent factory (ComponentFactory).
     */
    private final CompositeFactory m_factory;

    /**
     * Composite Handler list.
     */
    private HandlerManager[] m_handlers;

    /**
     * Instance State Listener List.
     */
    private List m_listeners = new ArrayList();

    /**
     * Internal service context of the composition.
     */
    private CompositeServiceContext m_internalContext;
    
    /**
     * Name of the component instance.
     */
    private String m_name;

    /**
     * Component state (STOPPED at the beginning).
     */
    private int m_state = STOPPED;

    /**
     * Construct a new Component Manager.
     * @param factory : the factory managing the instance manager
     * @param context : the bundle context to give to the instance
     * @param handlers : the handlers to plug
     */
    public CompositeManager(CompositeFactory factory, BundleContext context, HandlerManager[] handlers) {
        m_factory = factory;
        m_context = context;
        // Initialize the service context.
        m_internalContext = new CompositeServiceContext(m_context, this);
        m_handlers = handlers;
    }

    /**
     * Plug the given handler to the current container.
     * @param handler : the handler to plug.
     */
    public synchronized void addCompositeHandler(HandlerManager handler) {
        if (m_handlers.length > 0) {
            HandlerManager[] newInstances = new HandlerManager[m_handlers.length + 1];
            System.arraycopy(m_handlers, 0, newInstances, 0, m_handlers.length);
            newInstances[m_handlers.length] = handler;
            m_handlers = newInstances;
        } else {
            m_handlers = new HandlerManager[] { handler };
        }
    }

    /**
     * Add an instance to the created instance list.
     * @param listener : the instance state listener to add.
     * @see org.apache.felix.ipojo.ComponentInstance#addInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void addInstanceStateListener(InstanceStateListener listener) {
        synchronized (m_listeners) {
            m_listeners.add(listener);
        }
    }

    /**
     * Configure the instance manager. Stop the existing handler, clear the
     * handler list, change the metadata, recreate the handler
     * 
     * @param metadata : the component type metadata
     * @param configuration : the configuration of the instance
     * @throws ConfigurationException : occurs when the component type are incorrect.
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {        
        // Add the name
        m_name = (String) configuration.get("name");
        
        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].init(this, metadata, configuration);
        }
    }
    
    /** 
     * Dispose the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public void dispose() {
        if (m_state > STOPPED) { stop(); }
        
        for (int i = 0; i < m_listeners.size(); i++) {
            ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, DISPOSED);
        }
        
        m_factory.disposed(this);

        // Cleaning
        m_state = DISPOSED;
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }
        m_handlers = new HandlerManager[0];
        m_listeners.clear();
    }

    /**
     * Return a specified handler.
     * @param name : class name of the handler to find
     * @return : the handler, or null if not found
     */
    public CompositeHandler getCompositeHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            HandlerFactory fact = (HandlerFactory) m_handlers[i].getFactory();
            if (fact.getHandlerName().equals(name) || fact.getComponentDescription().getClassName().equals(name)) {
                return (CompositeHandler) m_handlers[i].getHandler();
            }
        }
        return null;
    }

    /**
     * Get the bundle context used by this instance.
     * @return the parent context of the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#getContext()
     */
    public BundleContext getContext() {
        return m_context;
    }

    /**
     * Get the factory which create this instance.
     * @return the factory of the component
     * @see org.apache.felix.ipojo.ComponentInstance#getFactory()
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }
    
    /**
     * Get the global bundle context.
     * @return the global bundle context.
     */
    public BundleContext getGlobalContext() {
        IPojoContext context = (IPojoContext) m_context;
        return context.getGlobalContext();
    }
    
    /**
     * Return the instance description of this instance.
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public InstanceDescription getInstanceDescription() {
        InstanceDescription desc = new InstanceDescription(m_name, m_state, getContext().getBundle().getBundleId(), m_factory.getComponentDescription());
        CompositeHandler[] handlers = getRegistredCompositeHandlers();
        for (int i = 0; i < handlers.length; i++) {
            desc.addHandler(handlers[i].getDescription());
        }

        // Get instances description of internal instance
        ServiceReference[] refs;
        try {
            refs = m_internalContext.getServiceReferences(Architecture.class.getName(), null);
            if (refs != null) {
                for (int i = 0; i < refs.length; i++) {
                    Architecture arch = (Architecture) m_internalContext.getService(refs[i]);
                    desc.addInstance(arch.getInstanceDescription());
                    m_internalContext.ungetService(refs[i]);
                }
            }
        } catch (InvalidSyntaxException e) {
            // Cannot happen
        }
        return desc;
    }

    /**
     * Get the instance name.
     * @return the instance name
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
     */
    public String getInstanceName() {
        return m_name;
    }

    /**
     * Get the parent service context.
     * @return the parent service context.
     */
    public ServiceContext getParentServiceContext() {
        IPojoContext context = (IPojoContext) m_context;
        return context.getServiceContext();
    }

    /**
     * REturn the list of handlers plugged on this instance.
     * @return the list of the registered handlers.
     */
    public CompositeHandler[] getRegistredCompositeHandlers() {
        CompositeHandler[] handler = new CompositeHandler[m_handlers.length];
        for (int i = 0; i < m_handlers.length; i++) {
            handler[i] = (CompositeHandler) m_handlers[i].getHandler();
        }
        return handler;
    }
    
    /**
     * Get the internal service context of this instance.
     * @return the internal service context.
     */
    public ServiceContext getServiceContext() {
        return m_internalContext;
    }
    
    /**
     * Get the actual state of the instance.
     * @return the actual state of the instance
     * @see org.apache.felix.ipojo.ComponentInstance#getState()
     */
    public int getState() {
        return m_state;
    }
    
    /**
     * Check if the instance is started.
     * @return true if the instance is started.
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public boolean isStarted() {
        return m_state > STOPPED;
    }

    /**
     * Reconfigure the current instance.
     * @param configuration : the new instance configuration.
     * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary configuration) {
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].reconfigure(configuration);
        }
    }

    /**
     * Remove an instance state listener.
     * @param listener : the listener to remove
     * @see org.apache.felix.ipojo.ComponentInstance#removeInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void removeInstanceStateListener(InstanceStateListener listener) {
        synchronized (m_listeners) {
            m_listeners.remove(listener);
        }
    }

    /**
     * Set the state of the component. 
     * if the state changed call the stateChanged(int) method on the handlers.
     * @param state : new state
     */
    public void setState(int state) {
        if (m_state != state) {
            if (state > m_state) {
                // The state increases (Stopped = > IV, IV => V) => invoke handlers from the higher priority to the lower
                m_state = state;
                for (int i = 0; i < m_handlers.length; i++) {
                    m_handlers[i].getHandler().stateChanged(state);
                }
            } else {
                // The state decreases (V => IV, IV = > Stopped, Stopped => Disposed)
                m_state = state;
                for (int i = m_handlers.length - 1; i > -1; i--) {
                    m_handlers[i].getHandler().stateChanged(state);
                }
            }
            
            for (int i = 0; i < m_listeners.size(); i++) {
                ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, state);
            }
        }
    }

    /**
     * Start the instance manager.
     */
    public synchronized void start() {
        if (m_state > STOPPED) {
            return;
        } // Instance already started


        // The new state of the component is UNRESOLVED
        m_state = INVALID;

        m_internalContext.start(); // Turn on the factory tracking

        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].start();
            m_handlers[i].addInstanceStateListener(this);
        }
        
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getState() != VALID) {
                setState(INVALID);
                return;
            }
        }
        setState(VALID);
        
    }

    /**
     * State Change listener callback.
     * This method is notified at each time a plugged handler becomes invalid.
     * @param instance : changing instance 
     * @param newState : new state
     * @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public synchronized void stateChanged(ComponentInstance instance, int newState) {
        if (m_state <= STOPPED) { return; }
     
        // Update the component state if necessary
        if (newState == INVALID && m_state == VALID) {
            // Need to update the state to UNRESOLVED
            setState(INVALID);
            return;
        }
        if (newState == VALID && m_state == INVALID) {
            // An handler becomes valid => check if all handlers are valid
            boolean isValid = true;
            for (int i = 0; i < m_handlers.length; i++) {
                isValid = isValid && m_handlers[i].getState() == VALID;
            }
            
            if (isValid) { setState(VALID); }
        }
        if (newState == DISPOSED) {
            kill();
        }
    }

    /**
     * Stop the instance manager.
     */
    public synchronized void stop() {
        if (m_state <= STOPPED) {
            return;
        } // Instance already stopped

        setState(INVALID);
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].removeInstanceStateListener(this);
            m_handlers[i].stop();
        }

        m_internalContext.stop(); // Turn off the factory tracking
        m_state = STOPPED;
        
        for (int i = 0; i < m_listeners.size(); i++) {
            ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, STOPPED);
        }
    }
    
    /**
     * Kill the current instance.
     * Only the factory of this instance can call this method.
     */
    protected synchronized void kill() {
        if (m_state > STOPPED) { stop(); }
        
        for (int i = 0; i < m_listeners.size(); i++) {
            ((InstanceStateListener) m_listeners.get(i)).stateChanged(this, DISPOSED);
        }

        // Cleaning
        m_state = DISPOSED;
        
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }
        m_handlers = new HandlerManager[0];
        m_listeners.clear();
    }
}
