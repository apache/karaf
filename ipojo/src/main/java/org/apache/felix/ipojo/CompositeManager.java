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

import org.apache.felix.ipojo.architecture.Architecture;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.composite.CompositeServiceContext;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * iPOJO Composite manager. The composite manager class manages one instance of
 * a component type which is a composition. It manages component lifecycle, and
 * handlers...
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class CompositeManager implements ComponentInstance {

    /**
     * Parent factory (ComponentFactory).
     */
    private ComponentFactory m_factory;

    /**
     * Name of the component instance.
     */
    private String m_name;

    /**
     * The context of the component.
     */
    private BundleContext m_context;

    /**
     * Composite Handler list.
     */
    private CompositeHandler[] m_handlers = new CompositeHandler[0];

    /**
     * Component state (STOPPED at the beginning).
     */
    private int m_state = STOPPED;

    /**
     * Component type information.
     */
    private ComponentDescription m_componentDesc;

    /**
     * Internal service context of the composition.
     */
    private CompositeServiceContext m_internalContext;

    // Constructor
    /**
     * Construct a new Component Manager.
     * 
     * @param factory : the factory managing the instance manager
     * @param bc : the bundle context to give to the instance
     */
    public CompositeManager(ComponentFactory factory, BundleContext bc) {
        m_factory = factory;
        m_context = bc;
        // Initialize the service context.
        m_internalContext = new CompositeServiceContext(m_context, this);
        m_factory.getLogger().log(Logger.INFO, "[Bundle " + m_context.getBundle().getBundleId() + "] Create an instance manager from the factory " + m_factory);
    }

    /**
     * Configure the instance manager. Stop the existings handler, clear the
     * handler list, change the metadata, recreate the handler
     * 
     * @param cm : the component type metadata
     * @param configuration : the configuration of the instance
     */
    public void configure(Element cm, Dictionary configuration) {
        // Stop all previous registred handler
        if (m_handlers.length != 0) {
            stop();
        }

        // Clear the handler list
        m_handlers = new CompositeHandler[0];

        // ComponentInfo initialization
        m_componentDesc = new ComponentDescription(m_factory.getName());

        // Add the name
        m_name = (String) configuration.get("name");

        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < IPojoConfiguration.INTERNAL_COMPOSITE_HANDLERS.length; i++) {
            // Create a new instance
            try {
                CompositeHandler h = (CompositeHandler) IPojoConfiguration.INTERNAL_COMPOSITE_HANDLERS[i].newInstance();
                h.configure(this, cm, configuration);
            } catch (InstantiationException e) {
                m_factory.getLogger().log(Logger.ERROR,
                        "[" + m_name + "] Cannot instantiate the handler " + IPojoConfiguration.INTERNAL_HANDLERS[i] + " : " + e.getMessage());
            } catch (IllegalAccessException e) {
                m_factory.getLogger().log(Logger.ERROR,
                        "[" + m_name + "] Cannot instantiate the handler " + IPojoConfiguration.INTERNAL_HANDLERS[i] + " : " + e.getMessage());
            }
        }

        // Look for namespaces
        for (int i = 0; i < cm.getNamespaces().length; i++) {
            if (!cm.getNamespaces()[i].equals("")) {
                // It is not an internal handler, try to load it
                try {
                    Class c = m_context.getBundle().loadClass(cm.getNamespaces()[i]);
                    CompositeHandler h = (CompositeHandler) c.newInstance();
                    h.configure(this, cm, configuration);
                } catch (ClassNotFoundException e) {
                    m_factory.getLogger()
                            .log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                } catch (InstantiationException e) {
                    m_factory.getLogger()
                            .log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                } catch (IllegalAccessException e) {
                    m_factory.getLogger()
                            .log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                }
            }
        }
    }

    /**
     * Return the component type description of this instance.
     * @return the component type information.
     * @see org.apache.felix.ipojo.ComponentInstance#getComponentDescription()
     */
    public ComponentDescription getComponentDescription() {
        return m_componentDesc;
    }

    /**
     * Return the instance description of this instance.
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public synchronized InstanceDescription getInstanceDescription() {
        int componentState = getState();
        InstanceDescription instanceDescription = new InstanceDescription(m_name, componentState, getContext().getBundle().getBundleId(), m_componentDesc);
        CompositeHandler[] handlers = getRegistredCompositeHandlers();
        for (int i = 0; i < handlers.length; i++) {
            instanceDescription.addHandler(handlers[i].getDescription());
        }

        // Get instances description of internal instance
        ServiceReference[] refs;
        try {
            refs = m_internalContext.getServiceReferences(Architecture.class.getName(), null);
            if (refs != null) {
                for (int i = 0; i < refs.length; i++) {
                    Architecture arch = (Architecture) m_internalContext.getService(refs[i]);
                    instanceDescription.addInstance(arch.getInstanceDescription());
                    m_internalContext.ungetService(refs[i]);
                }
            }
        } catch (InvalidSyntaxException e) {
            e.printStackTrace(); // Should not happen
        }
        return instanceDescription;
    }

    /**
     * REturn the list of handlers plugged on this instace.
     * @return the list of the registred handlers.
     */
    public CompositeHandler[] getRegistredCompositeHandlers() {
        return m_handlers;
    }

    /**
     * Return a specified handler.
     * 
     * @param name : class name of the handler to find
     * @return : the handler, or null if not found
     */
    public CompositeHandler getCompositeHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getClass().getName().equalsIgnoreCase(name)) {
                return m_handlers[i];
            }
        }
        return null;
    }

    // ===================== Lifecycle management =====================

    /**
     * Start the instance manager.
     */
    public void start() {
        if (m_state != STOPPED) {
            return;
        } // Instance already started

        // Start all the handlers
        m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] Start the instance manager with " + m_handlers.length + " handlers");

        // The new state of the component is UNRESOLVED
        m_state = INVALID;

        m_internalContext.start(); // Turn on the factory tracking

        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].start();
        }

        // Defines the state of the component :
        checkInstanceState();
    }

    /**
     * Stop the instance manager.
     */
    public void stop() {
        if (m_state == STOPPED) {
            return;
        } // Instance already stopped

        setState(INVALID);
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].stop();
        }

        m_internalContext.stop(); // Turn off the factory tracking
        m_state = STOPPED;
    }
    
    /** 
     * Dispose the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public void dispose() {
        if (m_state != STOPPED) {
            stop();
        }
        
        m_factory.stopped(this);

        // Cleaning
        m_factory = null;
        m_name = null;
        m_context = null;
        m_handlers = null;
        m_componentDesc = null;
    }

    /**
     * Set the state of the component. 
     * Ff the state changed call the stateChanged(int) method on the handlers.
     * @param state : new state
     */
    public void setState(int state) {
        if (m_state != state) {

            // Log the state change
            if (state == INVALID) {
                m_factory.getLogger().log(Logger.INFO, "[" + m_name + "]  State -> INVALID");
            }
            if (state == VALID) {
                m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] State -> VALID");
            }

            // The state changed call the handler stateChange method
            m_state = state;
            for (int i = m_handlers.length - 1; i > -1; i--) {
                m_handlers[i].stateChanged(state);
            }
        }
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
        return m_state != STOPPED;
    }

    // ===================== end Lifecycle management =====================

    // ================== Class & Instance management ===================

    /**
     * Get the factory which create this instance.
     * @return the factory of the component
     * @see org.apache.felix.ipojo.ComponentInstance#getFactory()
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }

    // ======================== Handlers Management ======================

    /**
     * Register the given handler to the current instance manager.
     * 
     * @param h : the handler to register
     */
    public void register(CompositeHandler h) {
        for (int i = 0; (m_handlers != null) && (i < m_handlers.length); i++) {
            if (m_handlers[i] == h) {
                return;
            }
        }

        if (m_handlers != null) {
            CompositeHandler[] newList = new CompositeHandler[m_handlers.length + 1];
            System.arraycopy(m_handlers, 0, newList, 0, m_handlers.length);
            newList[m_handlers.length] = h;
            m_handlers = newList;
        }
    }

    /**
     * Unregister the given handler.
     * 
     * @param h : the handler to unregiter
     */
    public void unregister(CompositeHandler h) {
        int idx = -1;
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i] == h) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_handlers.length - 1) == 0) {
                m_handlers = new CompositeHandler[0];
            } else {
                CompositeHandler[] newList = new CompositeHandler[m_handlers.length - 1];
                System.arraycopy(m_handlers, 0, newList, 0, idx);
                if (idx < newList.length) {
                    System.arraycopy(m_handlers, idx + 1, newList, idx, newList.length - idx);
                }
                m_handlers = newList;
            }
        }
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
     * Check the state of all handlers.
     */
    public void checkInstanceState() {
        m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] Check the instance state");
        boolean isValid = true;
        for (int i = 0; i < m_handlers.length; i++) {
            boolean b = m_handlers[i].isValid();
            isValid = isValid && b;
        }

        // Update the component state if necessary
        if (!isValid && m_state == VALID) {
            // Need to update the state to UNRESOLVED
            setState(INVALID);
            return;
        }
        if (isValid && m_state == INVALID) {
            setState(VALID);
        }
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
     * Reconfigure the current instance.
     * @param configuration : the new instance ocnfiguration.
     * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary configuration) {
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].reconfigure(configuration);
        }
    }

    /**
     * Get the internal service context of this instance.
     * @return the internal service context.
     */
    public ServiceContext getServiceContext() {
        return m_internalContext;
    }
}
