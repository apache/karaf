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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Dictionary;
import java.util.HashMap;

import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * The instance manager class manages one instance of a component type.
 * It manages component lifecycle, component instance creation and handlers.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class InstanceManager implements ComponentInstance {

    /**
     * Parent factory (ComponentFactory).
     */
    private ComponentFactory m_factory;
    
    /**
     * Name of the component instance.
     */
    private String m_name;
    
    /**
     * Name of the component type implementation class.
     */
    private String m_className;

    /**
     * The context of the component.
     */
    private BundleContext m_context;

    /**
     * Handler list.
     */
    private Handler[] m_handlers = new Handler[0];

    /**
     * Map [field, handler list] storing handlers interested by the field.
     */
    private HashMap m_fieldRegistration = new HashMap();

    /**
     * Component state (STOPPED at the beginning).
     */
    private int m_state = STOPPED;

    /**
     * Manipulatd clazz.
     */
    private Class m_clazz;

    /**
     * Instances of the components.
     */
    private Object[] m_pojoObjects = new Object[0];

    /**
     * Component type information.
     */
    private ComponentDescription m_componentDesc;

    // Constructor
    /**
     * Construct a new Component Manager.
     * @param factory : the factory managing the instance manager
     * @param bc : the bundle context to give to the instance
     */
    public InstanceManager(ComponentFactory factory, BundleContext bc) {
        m_factory = factory;
        m_context = bc;
        m_factory.getLogger().log(Logger.INFO, "[Bundle " + m_context.getBundle().getBundleId() + "] Create an instance manager from the factory " + m_factory);
    }

    /**
     * Configure the instance manager.
     * Stop the existings handler, clear the handler list, change the metadata, recreate the handlers
     * @param cm : the component type metadata
     * @param configuration : the configuration of the instance
     */
    public void configure(Element cm, Dictionary configuration) {
        // Stop all previous registred handler
        if (m_handlers.length != 0) { stop(); }

        // Clear the handler list
        m_handlers = new Handler[0];

        // Set the component-type metadata
        m_className = cm.getAttribute("className");
        if (m_className == null) {
            m_factory.getLogger().log(Logger.ERROR, "The class name of the component cannot be setted, it does not exist in the metadata");
        }

        // ComponentInfo initialization
        m_componentDesc = new ComponentDescription(m_factory.getName(), m_className);
        
        // Add the name
        m_name = (String) configuration.get("name");

        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < IPojoConfiguration.INTERNAL_HANDLERS.length; i++) {
            // Create a new instance
            try {
                Handler h = (Handler) IPojoConfiguration.INTERNAL_HANDLERS[i].newInstance();
                h.configure(this, cm, configuration);
            } catch (InstantiationException e) {
                m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + IPojoConfiguration.INTERNAL_HANDLERS[i] + " : " + e.getMessage());
            } catch (IllegalAccessException e) {
                m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + IPojoConfiguration.INTERNAL_HANDLERS[i] + " : " + e.getMessage());
            }
        }

        // Look for namespaces
        for (int i = 0; i < cm.getNamespaces().length; i++) {
            if (!cm.getNamespaces()[i].equals("")) {
                // It is not an internal handler, try to load it
                try {
                    Class c = m_context.getBundle().loadClass(cm.getNamespaces()[i]);
                    Handler h = (Handler) c.newInstance();
                    h.configure(this, cm, configuration);
                } catch (ClassNotFoundException e) {
                    m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                } catch (InstantiationException e) {
                    m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                } catch (IllegalAccessException e) {
                    m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Cannot instantiate the handler " + cm.getNamespaces()[i] + " : " + e.getMessage());
                }
            }
        }
    }

    /**
     * @return the component type information.
     */
    public ComponentDescription getComponentDescription() { return m_componentDesc; }
    
    /**
     * @return the instance description.
     */
    public InstanceDescription getInstanceDescription() {
    	int componentState = getState();
        InstanceDescription instanceDescription = new InstanceDescription(m_name, m_className, componentState, getContext().getBundle().getBundleId());

        String[] objects = new String[getPojoObjects().length];
        for (int i = 0; i < getPojoObjects().length; i++) {
        	objects[i] = getPojoObjects()[i].toString();
        }
        instanceDescription.setCreatedObjects(objects);

        Handler[] handlers = getRegistredHandlers();
        for (int i = 0; i < handlers.length; i++) {
        	instanceDescription.addHandler(handlers[i].getDescription());
        }
        return instanceDescription;
    }

    /**
     * @return the list of the registred handlers.
     */
    public Handler[] getRegistredHandlers() { return m_handlers; }

    /**
     * Return a specified handler.
     * @param name : class name of the handler to find
     * @return : the handler, or null if not found
     */
    public Handler getHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i].getClass().getName().equalsIgnoreCase(name)) { return m_handlers[i]; }
        }
        return null;
    }
    
    /**
     * @return the component instance name.
     */
    public String getComponentName() { return m_name; }
    
    /**
     * @return the implementation class name of the instance.
     */
    public String getClassName() { return m_className; }

    // ===================== Lifecycle management =====================

    /**
     * Start the instance manager.
     */
    public void start() {
    	if (m_state != STOPPED) { return; } // Instance already started
    	
        // Start all the handlers
        m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] Start the instance manager with " + m_handlers.length + " handlers");

        // The new state of the component is UNRESOLVED
        m_state = INVALID;

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
    	if (m_state == STOPPED) { return; } // Instance already stopped
    	
        setState(INVALID);
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].stop();
        }
        m_pojoObjects = new Object[0];
        
        m_state = STOPPED;
    }

    /**
     * Set the state of the component.
     * if the state changed call the stateChanged(int) method on the handlers
     */
    public void setState(int state) {
        if (m_state != state) {

            // Log the state change
            if (state == INVALID) { m_factory.getLogger().log(Logger.INFO, "[" + m_name + "]  State -> INVALID"); }
            if (state == VALID) { m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] State -> VALID"); }

            // The state changed call the handler stateChange method
            m_state = state;
            for (int i = m_handlers.length - 1; i > -1; i--) {
                m_handlers[i].stateChanged(state);
            }
        }
    }

    /**
     * @return the actual state of the component.
     */
    public int getState() { return m_state; }
    
    /**
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public boolean isStarted() { return m_state != STOPPED; }

    // ===================== end Lifecycle management =====================

    // ================== Class & Instance management ===================

    /**
     * @return the factory of the component
     */
    public ComponentFactory getFactory() { return m_factory; }

    /**
     * Load the manipulated class.
     */
    private void load() {
        try {
            m_clazz = m_factory.loadClass(m_className);
        } catch (ClassNotFoundException  e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Class not found during the loading phase : " + e.getMessage());
            return;
        }
    }

    /**
     * @return true if the class is loaded
     */
    private boolean isLoaded() {
        return (m_clazz != null);
    }

    /**
     * Add an instance to the created instance list.
     * @param o : the instance to add
     */
    private void addInstance(Object o) {
        for (int i = 0; (m_pojoObjects != null) && (i < m_pojoObjects.length); i++) {
            if (m_pojoObjects[i] == o) { return; }
        }

        if (m_pojoObjects.length > 0) {
            Object[] newInstances = new Object[m_pojoObjects.length + 1];
            System.arraycopy(m_pojoObjects, 0, newInstances, 0, m_pojoObjects.length);
            newInstances[m_pojoObjects.length] = o;
            m_pojoObjects = newInstances;
        } else {
            m_pojoObjects = new Object[] {o};
        }
    }

    /**
     * Remove an instance from the created instance list. The instance will be eated by the garbage collector.
     * @param o : the instance to remove
     */
    private void removeInstance(Object o) {
        int idx = -1;
        for (int i = 0; i < m_pojoObjects.length; i++) {
            if (m_pojoObjects[i] == o) { idx = i; break; }
        }

        if (idx >= 0) {
            if ((m_pojoObjects.length - 1) == 0) { 
            	m_pojoObjects = new Element[0]; 
            } else {
                Object[] newInstances = new Object[m_pojoObjects.length - 1];
                System.arraycopy(m_pojoObjects, 0, newInstances, 0, idx);
                if (idx < newInstances.length) {
                    System.arraycopy(m_pojoObjects, idx + 1, newInstances, idx, newInstances.length - idx); 
                }
                m_pojoObjects = newInstances;
            }
        }
    }

    /**
     * @return the created instance of the component.
     */
    public Object[] getPojoObjects() { return m_pojoObjects; }

    /**
     * Delete the created instance (remove it from the list, to allow the garbage collector to eat the instance).
     * @param o : the instance to delete
     */
    public void deletePojoObject(Object o) { removeInstance(o); }

    /**
     * Create an instance of the component.
     * This method need to be called one time only for singleton provided service
     * @return a new instance
     */
    public Object createPojoObject() {

        if (!isLoaded()) { load(); }
        Object instance = null;
        try {

            // Try to find if there is a constructor with a bundle context as parameter :
            try {
                Constructor constructor = m_clazz.getConstructor(new Class[] {InstanceManager.class, BundleContext.class});
                constructor.setAccessible(true);
                instance = constructor.newInstance(new Object[] {this, m_context});
            } catch (NoSuchMethodException e) { instance = null; }

            // Create an instance if no instance are already created with <init>()BundleContext
            if (instance == null) {
                Constructor constructor = m_clazz.getConstructor(new Class[] {InstanceManager.class});
                constructor.setAccessible(true);
                instance = constructor.newInstance(new Object[] {this});
            }

        } catch (InstantiationException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance cannot be instancied : " + e.getMessage());
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance is not accessible : " + e.getMessage());
            e.printStackTrace();
        } catch (SecurityException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance is not accessible (security reason) : " + e.getMessage());
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the constructor method (illegal target) : " + e.getMessage());
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the constructor (method not found) : " + e.getMessage());
            e.printStackTrace();
        }

        m_factory.getLogger().log(Logger.INFO, "[" + m_name + "] createInstance -> Return the instance " + instance);

        // Register the new instance
        addInstance(instance);
        // Call createInstance on Handlers :
        for (int i = 0; i < m_handlers.length; i++) { m_handlers[i].createInstance(instance); }
        return instance;
    }

    /**
     * @return the instance of the component to use for singleton component
     */
    public Object getPojoObject() {
        if (m_pojoObjects.length == 0) { createPojoObject(); }
        return m_pojoObjects[0];
    }

    /**
     * @return the manipulated class
     */
    public Class getClazz() {
        if (!isLoaded()) { load(); }
        return m_clazz;
    }

    //  ================== end Class & Instance management ================

    //  ======================== Handlers Management ======================

    /**
     * Register the given handler to the current instance manager.
     * @param h : the handler to register
     */
    public void register(Handler h) {
        for (int i = 0; (m_handlers != null) && (i < m_handlers.length); i++) {
            if (m_handlers[i] == h) {
                return;
            }
        }

        if (m_handlers != null) {
            Handler[] newList = new Handler[m_handlers.length + 1];
            System.arraycopy(m_handlers, 0, newList, 0, m_handlers.length);
            newList[m_handlers.length] = h;
            m_handlers = newList;
        }
    }

    /**
     * Register an handler.
     * The handler will be notified of event on each field given in the list.
     * @param h : the handler to register
     * @param fields : the fields list
     */
    public void register(Handler h, String[] fields) {
        register(h);
        for (int i = 0; i < fields.length; i++) {
            if (m_fieldRegistration.get(fields[i]) == null) {
                m_fieldRegistration.put(fields[i], new Handler[] {h});
            } else {
                Handler[] list = (Handler[]) m_fieldRegistration.get(fields[i]);
                for (int j = 0; j < list.length; j++) { 
                	if (list[j] == h) { 
                		return;
                	} 
                }
                Handler[] newList = new Handler[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = h;
                m_fieldRegistration.put(fields[i], newList);
            }
        }
    }

    /**
     * Unregister an handler for the field list.
     * The handler will not be notified of field access but is allways register on the instance manager.
     * @param h : the handler to unregister.
     * @param fields : the fields list
     */
    public void unregister(Handler h, String[] fields) {
        for (int i = 0; i < fields.length; i++) {
            if (m_fieldRegistration.get(fields[i]) == null) { 
            	break;
            } else {
                Handler[] list = (Handler[]) m_fieldRegistration.get(fields[i]);
                int idx = -1;
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        idx = j;
                        break;
                    }
                }

                if (idx >= 0) {
                    if ((list.length - 1) == 0) {
                        list = new Handler[0];
                    } else {
                        Handler[] newList = new Handler[list.length - 1];
                        System.arraycopy(list, 0, newList, 0, idx);
                        if (idx < newList.length)             {
                            System.arraycopy(
                                    list, idx + 1, newList, idx, newList.length - idx);
                        }
                        list = newList;
                    }
                    m_fieldRegistration.put(fields[i], list);
                }
            }
        }
    }

    /**
     * Unregister the given handler.
     * @param h : the handler to unregiter
     */
    public void unregister(Handler h) {
        int idx = -1;
        for (int i = 0; i < m_handlers.length; i++) {
            if (m_handlers[i] == h) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_handlers.length - 1) == 0) {
                m_handlers = new Handler[0];
            } else {
                Handler[] newList = new Handler[m_handlers.length - 1];
                System.arraycopy(m_handlers, 0, newList, 0, idx);
                if (idx < newList.length)             {
                    System.arraycopy(
                            m_handlers, idx + 1, newList, idx, newList.length - idx);
                }
                m_handlers = newList;
            }
        }
    }

    /**
     * This method is called by the manipulated class each time that a GETFIELD instruction is found.
     * The method ask to each handler which value need to be returned.
     * @param fieldName : the field name on which the GETFIELD instruction is called
     * @param initialValue : the value of the field in the code
     * @return the value decided by the last asked handler (throw a warining if two fields decide two different values)
     */
    public Object getterCallback(String fieldName, Object initialValue) {
        Object result = null;
        // Get the list of registered handlers
        Handler[] list = (Handler[]) m_fieldRegistration.get(fieldName);
        for (int i = 0; list != null && i < list.length; i++) {
            Object handlerResult = list[i].getterCallback(fieldName, initialValue);
            if (handlerResult != initialValue) { result = handlerResult; }
        }

        if (result != null) {
            return result;
        } else {
            return initialValue;
        }
    }

    /**
     * This method is called by the manipulated class each time that a PUTFILED instruction is found.
     * the method send to each handler the new value.
     * @param fieldName : the field name on which the PUTFIELD instruction is called
     * @param objectValue : the value of the field
     */
    public void setterCallback(String fieldName, Object objectValue) {
        // Get the list of registered handlers
        Handler[] list = (Handler[]) m_fieldRegistration.get(fieldName);

        for (int i = 0; list != null && i < list.length; i++) {
            list[i].setterCallback(fieldName, objectValue);
        }
    }

    /**
     * @return the context of the component.
     */
    public BundleContext getContext() { return m_context; }

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
            m_pojoObjects = new Object[0];
            return;
        }
        if (isValid && m_state == INVALID) { setState(VALID); }
    }

	/**
	 * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
	 */
	public String getInstanceName() { return m_name; }

	/**
	 * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
	 */
	public void reconfigure(Dictionary configuration) {
		for (int i = 0; i < m_handlers.length; i++) {
	        m_handlers[i].reconfigure(configuration);
	    }
	}


    // ======================= end Handlers Management =====================

}
