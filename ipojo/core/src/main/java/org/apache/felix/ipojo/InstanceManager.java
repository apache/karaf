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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.architecture.InstanceDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.MethodMetadata;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * The instance manager class manages one instance of a component type. It
 * manages component lifecycle, component instance creation and handlers.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class InstanceManager implements ComponentInstance, InstanceStateListener {

    /**
     * Name of the component instance.
     */
    protected String m_name;

    /**
     * Name of the component type implementation class.
     */
    protected String m_className;
    
    /**
     * Handler list.
     */
    protected HandlerManager[] m_handlers = new HandlerManager[0];

    /**
     * Component state (STOPPED at the beginning).
     */
    protected int m_state = STOPPED;
    
    /**
     * Instance State Listener List.
     */
    protected List m_instanceListeners = new ArrayList();
    
    /**
     * Parent factory (ComponentFactory).
     */
    private ComponentFactory m_factory;

    /**
     * The context of the component.
     */
    private BundleContext m_context;

    /**
     * Map [field, handler list] storing handlers interested by the field.
     */
    private Map m_fieldRegistration = new HashMap();
    
    /**
     * Map [method identifier, handler list] storing handlers interested by the method.
     */
    private Map m_methodRegistration = new HashMap();

    /**
     * Manipulated class.
     */
    private Class m_clazz;

    /**
     * Instances of the components.
     */
    private Object[] m_pojoObjects = new Object[0];

   /**
    * Is the component instance state changing?
    */
    private boolean m_inTransition = false;
    
    /**
     * Queue of stored state changed. 
     */
    private List m_stateQueue = new ArrayList();
    
    /**
     * Map of [field, value], storing POJO field value.
     */
    private Map m_map = new HashMap();

    /**
     * Construct a new Component Manager.
     * 
     * @param factory : the factory managing the instance manager
     * @param bc : the bundle context to give to the instance
     * @param handlers : handlers array
     */
    public InstanceManager(ComponentFactory factory, BundleContext bc, HandlerManager[] handlers) {
        m_factory = factory;
        m_context = bc;
        m_handlers = handlers;
    }

    /**
     * Configure the instance manager. Stop the existing handler, clear the
     * handler list, change the metadata, recreate the handlers
     * 
     * @param cm : the component type metadata
     * @param configuration : the configuration of the instance
     * @throws ConfigurationException : occurs if the metadata are not correct
     */
    public void configure(Element cm, Dictionary configuration) throws ConfigurationException {
        m_className = cm.getAttribute("className");
        
        // Add the name
        m_name = (String) configuration.get("name");
        
        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].init(this, cm, configuration);
        }
    }

    /**
     * Get the description of the current instance. 
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public InstanceDescription getInstanceDescription() {
        int componentState = getState();
        InstanceDescription instanceDescription = new InstanceDescription(m_name, componentState, getContext().getBundle().getBundleId(), m_factory.getComponentDescription());

        String[] objects = new String[getPojoObjects().length];
        for (int i = 0; i < getPojoObjects().length; i++) {
            objects[i] = "" + getPojoObjects()[i];
        }
        instanceDescription.setCreatedObjects(objects);

        Handler[] handlers = getRegistredHandlers();
        for (int i = 0; i < handlers.length; i++) {
            instanceDescription.addHandler(handlers[i].getDescription());
        }
        return instanceDescription;
    }

    /**
     * Get the list of handlers plugged on the instance.
     * @return the handler array of plugged handlers.
     */
    public Handler[] getRegistredHandlers() {
        Handler[] h = new Handler[m_handlers.length];
        for (int i = 0; i < m_handlers.length; i++) {
            h[i] = m_handlers[i].getHandler();
        }
        return h;
    }

    /**
     * Return a specified handler.
     * 
     * @param name : class name of the handler to find or its qualified name (namespace:name)
     * @return : the handler, or null if not found
     */
    public Handler getHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            HandlerFactory fact = (HandlerFactory) m_handlers[i].getHandler().getInstance().getFactory();
            if (fact.getHandlerName().equals(name)) {
                return m_handlers[i].getHandler();
            }
        }
        return null;
    }

    /**
     * Start the instance manager.
     */
    public synchronized void start() {
        if (m_state != STOPPED) { // Instance already started
            return;
        } 
        
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].addInstanceStateListener(this);
            m_handlers[i].start();
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
     * Stop the instance manager.
     */
    public synchronized void stop() {
        if (m_state == STOPPED) {
            return;
        } // Instance already stopped
        
        setState(INVALID);
        
        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].removeInstanceStateListener(this);
            m_handlers[i].stop();
        }
        
        m_pojoObjects = new Object[0];

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
        if (m_state > STOPPED) { // Valid or invalid
            stop();
        }
        
        m_state = DISPOSED;
        
        for (int i = 0; i < m_instanceListeners.size(); i++) {
            ((InstanceStateListener) m_instanceListeners.get(i)).stateChanged(this, DISPOSED);
        }
        
        m_factory.disposed(this);
        
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }
        
        m_map.clear();
        m_handlers = new HandlerManager[0];
        m_fieldRegistration = new HashMap();
        m_methodRegistration = new HashMap();
        m_clazz = null;
        m_inTransition = false;
        m_instanceListeners.clear();
    }
    
    /**
     * Kill the current instance.
     * Only the factory of this instance can call this method.
     */
    protected void kill() {
        if (m_state > STOPPED) {
            stop();
        }
        
        for (int i = 0; i < m_instanceListeners.size(); i++) {
            ((InstanceStateListener) m_instanceListeners.get(i)).stateChanged(this, DISPOSED);
        }

        // Cleaning
        m_state = DISPOSED;
        
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].dispose();
        }
        
        m_map.clear();
        m_handlers = new HandlerManager[0];
        m_fieldRegistration = new HashMap();
        m_methodRegistration = new HashMap();
        m_clazz = null;
        m_inTransition = false;
        m_instanceListeners.clear();
    }
    
    /**
     * Set the state of the component instance.
     * if the state changed call the stateChanged(int) method on the handlers.
     * This method has a reentrant mechanism. If in the flow of the first call the method is called another times, 
     * the second call is stored and executed after the first one is finished.
     * @param state : the new state
     */
    public synchronized void setState(int state) {
        if (m_inTransition) {
            m_stateQueue.add(new Integer(state)); 
            return;
        }
        
        if (m_state != state) {
            m_inTransition = true;

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
            
            for (int i = 0; i < m_instanceListeners.size(); i++) {
                ((InstanceStateListener) m_instanceListeners.get(i)).stateChanged(this, state);
            }
        }
        
        m_inTransition = false;
        if (! m_stateQueue.isEmpty()) {
            int newState = ((Integer) (m_stateQueue.remove(0))).intValue();
            setState(newState);
        }
    }

    /**
     * Get the actual state of the instance.
     * @return the actual state of the component instance.
     * @see org.apache.felix.ipojo.ComponentInstance#getState()
     */
    public int getState() {
        return m_state;
    }

    /**
     * Check if the instance if started.
     * @return true if the instance is started.
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public boolean isStarted() {
        return m_state > STOPPED;
    }
    
    /**
     * Register an instance state listener.
     * @param listener : listener to register.
     * @see org.apache.felix.ipojo.ComponentInstance#addInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void addInstanceStateListener(InstanceStateListener listener) {
        synchronized (m_instanceListeners) {
            m_instanceListeners.add(listener);
        }
    }

    /**
     * Unregister an instance state listener.
     * @param listener : listener to unregister.
     * @see org.apache.felix.ipojo.ComponentInstance#removeInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public void removeInstanceStateListener(InstanceStateListener listener) {
        synchronized (m_instanceListeners) {
            m_instanceListeners.remove(listener);
        }
    }

    /**
     * Get the factory which create the current instance.
     * @return the factory of the component
     * @see org.apache.felix.ipojo.ComponentInstance#getFactory()
     */
    public ComponentFactory getFactory() {
        return m_factory;
    }

    /**
     * Load the manipulated class.
     */
    private void load() {
        try {
            m_clazz = m_factory.loadClass(m_className);
        } catch (ClassNotFoundException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] Class not found during the loading phase : " + e.getMessage());
            stop();
            return;
        }
    }

    /**
     * Add an instance to the created instance list.
     * @param o : the instance to add
     */
    private synchronized void addInstance(Object o) {
        for (int i = 0; (m_pojoObjects != null) && (i < m_pojoObjects.length); i++) {
            if (m_pojoObjects[i] == o) {
                return;
            }
        }

        if (m_pojoObjects.length > 0) {
            Object[] newInstances = new Object[m_pojoObjects.length + 1];
            System.arraycopy(m_pojoObjects, 0, newInstances, 0, m_pojoObjects.length);
            newInstances[m_pojoObjects.length] = o;
            m_pojoObjects = newInstances;
        } else {
            m_pojoObjects = new Object[] { o };
        }
    }

    /**
     * Get the array of object created by the instance.
     * @return the created instance of the component instance.
     */
    public Object[] getPojoObjects() {
        return m_pojoObjects;
    }

    /**
     * Delete the created instance (remove it from the list, to allow the
     * garbage collector to eat the instance).
     * 
     * @param o : the instance to delete
     */
    public synchronized void deletePojoObject(Object o) {
        int idx = -1;
        for (int i = 0; i < m_pojoObjects.length; i++) {
            if (m_pojoObjects[i] == o) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            if ((m_pojoObjects.length - 1) == 0) {
                m_pojoObjects = new Object[0];
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
     * Create an instance of the component. This method need to be called one
     * time only for singleton provided service
     * 
     * @return a new instance
     */
    public Object createPojoObject() {

        if (m_clazz == null) {
            load();
        }
        Object instance = null;
        try {
            // Try to find if there is a constructor with a bundle context as
            // parameter :
            try {
                Constructor constructor = m_clazz.getConstructor(new Class[] { InstanceManager.class, BundleContext.class });
                constructor.setAccessible(true);
                instance = constructor.newInstance(new Object[] { this, m_context });
            } catch (NoSuchMethodException e) {
                instance = null;
            }

            // Create an instance if no instance are already created with
            // <init>()BundleContext
            if (instance == null) {
                Constructor constructor = m_clazz.getConstructor(new Class[] { InstanceManager.class });
                constructor.setAccessible(true);
                instance = constructor.newInstance(new Object[] { this });
            }

        } catch (InstantiationException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance cannot be instancied : " + e.getMessage());
            stop();
        } catch (IllegalAccessException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance is not accessible : " + e.getMessage());
            stop();
        } catch (SecurityException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> The Component Instance is not accessible (security reason) : " + e.getMessage());
            stop();
        } catch (InvocationTargetException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the constructor method (illegal target) : " + e.getMessage());
            e.printStackTrace();
            stop();
        } catch (NoSuchMethodException e) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the constructor (method not found) : " + e.getMessage());
            stop();
        }
        if (instance == null) {
            m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot create the instance");
            stop();
        }

        // Register the new instance
        addInstance(instance);
        // Call createInstance on Handlers :
        for (int i = 0; i < m_handlers.length; i++) {
            ((PrimitiveHandler) m_handlers[i].getHandler()).objectCreated(instance);
        }
        return instance;
    }

    /**
     * Get the first object created by the instance.
     * If no object created, create and return one object.
     * @return the instance of the component instance to use for singleton component
     */
    public synchronized Object getPojoObject() {
        if (m_pojoObjects.length == 0) {
            createPojoObject();
        }
        return m_pojoObjects[0];
    }

    /**
     * Get the manipulated class.
     * @return the manipulated class
     */
    public Class getClazz() {
        if (m_clazz == null) {
            load();
        }
        return m_clazz;
    }

    /**
     * Register an handler. The handler will be notified of event on each field
     * given in the list.
     * 
     * @param h : the handler to register
     * @param fields : the field metadata list
     * @param methods : the method metadata list
     */
    public void register(PrimitiveHandler h, FieldMetadata[] fields, MethodMetadata[] methods) {
        for (int i = 0; fields != null && i < fields.length; i++) {
            if (m_fieldRegistration.get(fields[i].getFieldName()) == null) {
                m_fieldRegistration.put(fields[i].getFieldName(), new PrimitiveHandler[] { h });
            } else {
                PrimitiveHandler[] list = (PrimitiveHandler[]) m_fieldRegistration.get(fields[i].getFieldName());
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        return;
                    }
                }
                PrimitiveHandler[] newList = new PrimitiveHandler[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = h;
                m_fieldRegistration.put(fields[i].getFieldName(), newList);
            }
        }
        for (int i = 0; methods != null && i < methods.length; i++) {
            if (m_methodRegistration.get(methods[i].getMethodIdentifier()) == null) {
                m_methodRegistration.put(methods[i].getMethodIdentifier(), new PrimitiveHandler[] { h });
            } else {
                PrimitiveHandler[] list = (PrimitiveHandler[]) m_methodRegistration.get(methods[i].getMethodIdentifier());
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        return;
                    }
                }
                PrimitiveHandler[] newList = new PrimitiveHandler[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = h;
                m_methodRegistration.put(methods[i].getMethodIdentifier(), newList);
            }
        }
        
    }

    /**
     * Unregister an handler for the field list. The handler will not be
     * notified of field access but is always register on the instance manager.
     * 
     * @param h : the handler to unregister.
     * @param fields : the field metadata list
     * @param methods : the method metadata list
     */
    public void unregister(PrimitiveHandler h, FieldMetadata[] fields, MethodMetadata[] methods) {
        for (int i = 0; i < fields.length; i++) {
            if (m_fieldRegistration.get(fields[i].getFieldName()) == null) {
                break;
            } else {
                PrimitiveHandler[] list = (PrimitiveHandler[]) m_fieldRegistration.get(fields[i].getFieldName());
                int idx = -1;
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        idx = j;
                        break;
                    }
                }

                if (idx >= 0) {
                    if ((list.length - 1) == 0) {
                        list = new PrimitiveHandler[0];
                    } else {
                        PrimitiveHandler[] newList = new PrimitiveHandler[list.length - 1];
                        System.arraycopy(list, 0, newList, 0, idx);
                        if (idx < newList.length) {
                            System.arraycopy(list, idx + 1, newList, idx, newList.length - idx);
                        }
                        list = newList;
                    }
                    m_fieldRegistration.put(fields[i].getFieldName(), list);
                }
            }
        }
        for (int i = 0; i < methods.length; i++) {
            if (m_methodRegistration.get(methods[i].getMethodIdentifier()) == null) {
                break;
            } else {
                PrimitiveHandler[] list = (PrimitiveHandler[]) m_methodRegistration.get(methods[i].getMethodIdentifier());
                int idx = -1;
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == h) {
                        idx = j;
                        break;
                    }
                }

                if (idx >= 0) {
                    if ((list.length - 1) == 0) {
                        list = new PrimitiveHandler[0];
                    } else {
                        PrimitiveHandler[] newList = new PrimitiveHandler[list.length - 1];
                        System.arraycopy(list, 0, newList, 0, idx);
                        if (idx < newList.length) {
                            System.arraycopy(list, idx + 1, newList, idx, newList.length - idx);
                        }
                        list = newList;
                    }
                    m_methodRegistration.put(methods[i].getMethodIdentifier(), list);
                }
            }
        }
    }
    
    public Set getRegistredFields() {
        return m_fieldRegistration.keySet();
    }
    
    public Set getRegistredMethods() {
        return m_methodRegistration.keySet();
    }

    /**
     * This method is called by the manipulated class each time that a GETFIELD
     * instruction is found. The method ask to each handler which value need to
     * be returned.
     * 
     * @param fieldName : the field name on which the GETFIELD instruction is
     * called
     * @return the value decided by the last asked handler (throw a warning if
     * two fields decide two different values)
     */
    public synchronized Object getterCallback(String fieldName) {
        Object initialValue = m_map.get(fieldName);
        Object result = initialValue;
        // Get the list of registered handlers
        PrimitiveHandler[] list = (PrimitiveHandler[]) m_fieldRegistration.get(fieldName);
        for (int i = 0; list != null && i < list.length; i++) {
            Object handlerResult = list[i].getterCallback(fieldName, initialValue);
            if (handlerResult == initialValue) {
                continue; // Non-binding case (default implementation).
            } else {
                if (result != initialValue) {
                    if ((handlerResult != null && ! handlerResult.equals(result)) || (result != null && handlerResult == null)) {
                        m_factory.getLogger().log(Logger.WARNING, "A conflict was detected on the injection of " + fieldName + " - return the last value from " + list[i].getInstance().getInstanceName());
                    }
                }
                result = handlerResult;
            }
        }
        
        if ((result != null && ! result.equals(initialValue)) || (result == null && initialValue != null)) {
            // A change occurs => notify the change
            m_map.put(fieldName, result);
            for (int i = 0; list != null && i < list.length; i++) {
                list[i].setterCallback(fieldName, result);
            }
        }
        
        return result;        
    }
    
    /**
     * Dispatch entry method event on registered handler.
     * @param methodId : method id
     */
    public synchronized void entryCallback(String methodId) {
        PrimitiveHandler[] list = (PrimitiveHandler[]) m_methodRegistration.get(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].entryCallback(methodId);
        }
    }

    /**
     * Dispatch exit method event on registered handler.
     * The given returned object is an instance of Exception if the method has launched an exception.
     * If the given object is null, either the method returns void, either the method has returned null.
     * @param methodId : method id
     * @param e : returned object.
     */
    public synchronized void exitCallback(String methodId, Object e) {
        PrimitiveHandler[] list = (PrimitiveHandler[]) m_methodRegistration.get(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].exitCallback(methodId, e);
        }
    }

    /**
     * This method is called by the manipulated class each time that a PUTFILED
     * instruction is found. the method send to each handler the new value.
     * 
     * @param fieldName : the field name on which the PUTFIELD instruction is
     * called
     * @param objectValue : the value of the field
     */
    public synchronized void setterCallback(String fieldName, Object objectValue) {
        Object o = m_map.get(fieldName);
        if ((o != null && ! o.equals(objectValue)) || (o == null && objectValue != null)) {
            m_map.put(fieldName, objectValue);
            PrimitiveHandler[] list = (PrimitiveHandler[]) m_fieldRegistration.get(fieldName);
            for (int i = 0; list != null && i < list.length; i++) {
                list[i].setterCallback(fieldName, objectValue);
            }
        }
    }

    /**
     * Get the bundle context used by this component instance.
     * @return the context of the component.
     * @see org.apache.felix.ipojo.ComponentInstance#getContext()
     */
    public BundleContext getContext() {
        return m_context;
    }
    
    public BundleContext getGlobalContext() {
        return ((IPojoContext) m_context).getGlobalContext();
    }
    
    public ServiceContext getLocalServiceContext() {
        return ((IPojoContext) m_context).getServiceContext();
    }

    /**
     * Get the instance name.
     * @return the instance name.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
     */
    public String getInstanceName() {
        return m_name;
    }

    /**
     * Reconfigure the current instance.
     * @param configuration : the new configuration to push
     * @see org.apache.felix.ipojo.ComponentInstance#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary configuration) {
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].getHandler().reconfigure(configuration);
        }
    }

    /**
     * Get the implementation class of the component type.
     * @return the class name of the component type.
     */
    public String getClassName() {
        return m_className;
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
            for (int i = 0; i < m_handlers.length; i++) {
                if (m_handlers[i].getState() != VALID) { return; }
            }
            setState(VALID);
            return;
        }        
    }
}
