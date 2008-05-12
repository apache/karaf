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
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
 * The instance manager class manages one instance of a component type. It manages component lifecycle, component instance creation and handlers.
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
    protected final HandlerManager[] m_handlers;

    /**
     * Component state (STOPPED at the beginning).
     */
    protected int m_state = STOPPED;

    /**
     * Instance State Listener List.
     */
    protected List m_listeners = null;

    /**
     * Parent factory (ComponentFactory).
     */
    private final ComponentFactory m_factory;

    /**
     * The context of the component.
     */
    private final BundleContext m_context;

    /**
     * Map [field, field interceptor list] storing handlers interested by the field.
     * Once configured, this map can't change.
     */
    private Map m_fieldRegistration;

    /**
     * Map [method identifier, method interceptor list] storing handlers interested by the method.
     * Once configure this map can't change.
     */
    private Map m_methodRegistration;

    /**
     * Manipulated class.
     * Once set, this field doesn't change.
     */
    private Class m_clazz;

    /**
     * Instances of the components.
     */
    private List m_pojoObjects;

    /**
     * Factory method. Contains the name of the static method used to create POJO objects.
     * Once set, this field is immutable.
     */
    private String m_factoryMethod = null;

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
    private Map m_fields = new HashMap();

    /**
     * Map method [id=>method].
     */
    private Map m_methods = new HashMap();

    /**
     * Construct a new Component Manager.
     * @param factory : the factory managing the instance manager
     * @param context : the bundle context to give to the instance
     * @param handlers : handlers array
     */
    public InstanceManager(ComponentFactory factory, BundleContext context, HandlerManager[] handlers) {
        m_factory = factory;
        m_context = context;
        m_handlers = handlers;
    }

    /**
     * Configure the instance manager. Stop the existing handler, clear the handler list, change the metadata, recreate the handlers
     * @param metadata : the component type metadata
     * @param configuration : the configuration of the instance
     * @throws ConfigurationException : occurs if the metadata are not correct
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        m_className = metadata.getAttribute("classname");

        // Add the name
        m_name = (String) configuration.get("name");

        // Get the factory method if presents.
        m_factoryMethod = (String) metadata.getAttribute("factory-method");

        // Create the standard handlers and add these handlers to the list
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].init(this, metadata, configuration);
        }
    }

    /**
     * Get the description of the current instance.
     * @return the instance description.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceDescription()
     */
    public InstanceDescription getInstanceDescription() {
        int componentState = getState();
        InstanceDescription desc =
                new InstanceDescription(m_name, componentState, getContext().getBundle().getBundleId(), m_factory.getComponentDescription());

        synchronized (this) { // Must be synchronized, it access to the m_pojoObjects list.
            if (m_pojoObjects != null) {
                String[] objects = new String[m_pojoObjects.size()];
                for (int i = 0; i < m_pojoObjects.size(); i++) {
                    objects[i] = m_pojoObjects.get(i).toString();
                }
                desc.setCreatedObjects(objects);
            }
        }

        Handler[] handlers = getRegistredHandlers();
        for (int i = 0; i < handlers.length; i++) {
            desc.addHandler(handlers[i].getDescription());
        }
        return desc;
    }

    /**
     * Get the list of handlers plugged on the instance.
     * This method does not need a synchronized block as the handler set is constant.
     * @return the handler array of plugged handlers.
     */
    public Handler[] getRegistredHandlers() {
        Handler[] handler = new Handler[m_handlers.length];
        for (int i = 0; i < m_handlers.length; i++) {
            handler[i] = m_handlers[i].getHandler();
        }
        return handler;
    }

    /**
     * Return a specified handler.
     * This must does not need a synchronized block as the handler set is constant.
     * @param name : class name of the handler to find or its qualified name (namespace:name)
     * @return : the handler, or null if not found
     */
    public Handler getHandler(String name) {
        for (int i = 0; i < m_handlers.length; i++) {
            HandlerFactory fact = (HandlerFactory) m_handlers[i].getHandler().getHandlerManager().getFactory();
            if (fact.getHandlerName().equals(name)) {
                return m_handlers[i].getHandler();
            }
        }
        return null;
    }

    /**
     * Give access to a field value to the first created pojo.
     * This method process by analyzing both managed fields and pojo fields (by reflection).
     * If no pojo were already created try only on managed fields.
     * @param fieldName : field name.
     * @return the field value, null is returned if the value is managed and not already set.
     */
    public synchronized Object getFieldValue(String fieldName) {
        if (m_pojoObjects == null) {
            return getFieldValue(fieldName, null);
        } else {
            return getFieldValue(fieldName, m_pojoObjects.get(0)); // Use the first pojo.
        }
    }

    /**
     * Give access to a field value to the given created pojo.
     * This method process by analyzing both managed fields and pojo fields (by reflection).
     * If the given pojo is null, try only on managed fields.
     * @param fieldName : field name.
     * @param pojo : the pojo on which computing field value.
     * @return the field value, null is returned if the value is managed and not already set.
     */
    public synchronized Object getFieldValue(String fieldName, Object pojo) {
        Object setByContainer = null;
        
        if (m_fields != null) {
            setByContainer = m_fields.get(fieldName);
        }
        
        if (setByContainer == null && pojo != null) { // In the case of no given pojo, return null.
            // If null either the value was not already set or has the null value.
            try {
                Field field = pojo.getClass().getDeclaredField(fieldName);
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                return field.get(pojo);
            } catch (SecurityException e) {
                m_factory.getLogger().log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            } catch (NoSuchFieldException e) {
                m_factory.getLogger().log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            } catch (IllegalArgumentException e) {
                m_factory.getLogger().log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            } catch (IllegalAccessException e) {
                m_factory.getLogger().log(Logger.ERROR, "Cannot reflect on field " + fieldName + " to obtain the value : " + e.getMessage());
            }
            return null;
        } else {
            return setByContainer;
        }
    }

    /**
     * Start the instance manager.
     */
    public void start() {
        synchronized (this) {
            if (m_state != STOPPED) { // Instance already started
                return;
            } else {
                m_state = -2; // Temporary state.
            }
        }
        
        for (int i = 0; i < m_handlers.length; i++) {
            m_handlers[i].addInstanceStateListener(this);
            try {
                m_handlers[i].start();
            } catch (IllegalStateException e) {
                m_factory.getLogger().log(Logger.ERROR, e.getMessage());
                stop();
                throw e;
            }
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
    public void stop() {
        List listeners = null;
        synchronized (this) {
            if (m_state == STOPPED) { // Instance already stopped
                return;
            } 
            m_stateQueue.clear();
            m_inTransition = false;
        }
        
        setState(INVALID); // Must be called outside a synchronized block.

        // Stop all the handlers
        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].removeInstanceStateListener(this);
            m_handlers[i].stop();
        }
        
        synchronized (this) {
            m_state = STOPPED;
            if (m_listeners != null) {
                listeners = new ArrayList(m_listeners); // Stack confinement
            }
            m_pojoObjects = null;
        }

        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                ((InstanceStateListener) listeners.get(i)).stateChanged(this, STOPPED);
            }
        }
    }

    /**
     * Dispose the instance.
     * @see org.apache.felix.ipojo.ComponentInstance#dispose()
     */
    public void dispose() {
        List listeners = null;
        int state = -2;
        synchronized (this) {
            state = m_state; // Stack confinement
            if (m_listeners != null) {
                listeners = new ArrayList(m_listeners); // Stack confinement
            }
            m_listeners = null;
        }
        
        if (state > STOPPED) { // Valid or invalid
            stop(); // Does not hold the lock.
        }
        
        synchronized (this) {
            m_state = DISPOSED;
        }

        for (int i = 0; listeners != null && i < listeners.size(); i++) {
            ((InstanceStateListener) listeners.get(i)).stateChanged(this, DISPOSED);
        }

        for (int i = m_handlers.length - 1; i > -1; i--) {
            m_handlers[i].dispose();
        }

        synchronized (this) {
            m_factory.disposed(this);
            m_fields.clear();
            m_fieldRegistration = new HashMap();
            m_methodRegistration = new HashMap();
            m_clazz = null;
        }
    }

    /**
     * Set the state of the component instance. if the state changed call the stateChanged(int) method on the handlers. This method has a reentrant
     * mechanism. If in the flow of the first call the method is called another times, the second call is stored and executed after the first one is
     * finished.
     * @param state : the new state
     */
    public void setState(int state) {
        int originalState = -2;
        List listeners = null;
        synchronized (this) {
            if (m_inTransition) {
                m_stateQueue.add(new Integer(state));
                return;
            }

            if (m_state != state) {
                m_inTransition = true;
                originalState = m_state; // Stack confinement.
                m_state = state;
                if (m_listeners != null) {
                    listeners = new ArrayList(m_listeners); // Stack confinement.
                }
            }
        }

        // This section can be executed only by one thread at the same time. The m_inTransition pseudo semaphore block access to this section.
        if (m_inTransition) { // Check that we are really changing.
            if (state > originalState) {
                // The state increases (Stopped = > IV, IV => V) => invoke handlers from the higher priority to the lower
                try {
                    for (int i = 0; i < m_handlers.length; i++) {
                        m_handlers[i].getHandler().stateChanged(state);
                    }
                } catch (IllegalStateException e) {
                    // When an illegal state exception happens, the instance manager must be stopped immediately.
                    stop();
                    return;
                }
            } else {
                // The state decreases (V => IV, IV = > Stopped, Stopped => Disposed)
                try {
                    for (int i = m_handlers.length - 1; i > -1; i--) {
                        m_handlers[i].getHandler().stateChanged(state);
                    }
                } catch (IllegalStateException e) {
                    // When an illegal state exception happens, the instance manager must be stopped immediately.
                    stop();
                    return;
                }
            }
        }

        if (listeners != null) {
            for (int i = 0; i < listeners.size(); i++) {
                ((InstanceStateListener) listeners.get(i)).stateChanged(this, state);
            }
        }

        synchronized (this) {
            m_inTransition = false;
            if (!m_stateQueue.isEmpty()) {
                int newState = ((Integer) (m_stateQueue.remove(0))).intValue();
                setState(newState);
            }
        }
    }

    /**
     * Get the actual state of the instance.
     * @return the actual state of the component instance.
     * @see org.apache.felix.ipojo.ComponentInstance#getState()
     */
    public synchronized int getState() {
        return m_state;
    }

    /**
     * Check if the instance if started.
     * @return true if the instance is started.
     * @see org.apache.felix.ipojo.ComponentInstance#isStarted()
     */
    public synchronized boolean isStarted() {
        return m_state > STOPPED;
    }

    /**
     * Register an instance state listener.
     * @param listener : listener to register.
     * @see org.apache.felix.ipojo.ComponentInstance#addInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public synchronized void addInstanceStateListener(InstanceStateListener listener) {
        if (m_listeners == null) {
            m_listeners = new ArrayList();
        }
        m_listeners.add(listener);
    }

    /**
     * Unregister an instance state listener.
     * @param listener : listener to unregister.
     * @see org.apache.felix.ipojo.ComponentInstance#removeInstanceStateListener(org.apache.felix.ipojo.InstanceStateListener)
     */
    public synchronized void removeInstanceStateListener(InstanceStateListener listener) {
        if (m_listeners != null) {
            m_listeners.remove(listener);
            if (m_listeners.isEmpty()) {
                m_listeners = null;
            }
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
     * Get the array of object created by the instance.
     * @return the created instance of the component instance.
     */
    public synchronized Object[] getPojoObjects() {
        if (m_pojoObjects == null) {
            return null;
        }
        return m_pojoObjects.toArray(new Object[m_pojoObjects.size()]);
    }

    /**
     * Create an instance of the component. This method need to be called one time only for singleton provided service
     * @return a new instance
     */
    public Object createPojoObject() {
        if (m_clazz == null) {
            load();
        }

        // The following code doesn't need to be synchronized as is deal only with immutable fields.
        Object instance = null;
        if (m_factoryMethod == null) {
            // No factory-method, we use the constructor.
            try {
                // Try to find if there is a constructor with a bundle context as parameter :
                try {
                    Constructor cst = m_clazz.getDeclaredConstructor(new Class[] { InstanceManager.class, BundleContext.class });
                    if (! cst.isAccessible()) {
                        cst.setAccessible(true);
                    }
                    Object[] args = new Object[] { this, m_context };
                    onEntry(null, m_className,  new Object[] {m_context});
                    instance = cst.newInstance(args);
                    onExit(null, m_className, instance);
                } catch (NoSuchMethodException e) {
                    // Create an instance if no instance are already created with <init>()BundleContext
                    if (instance == null) {
                        Constructor cst = m_clazz.getDeclaredConstructor(new Class[] { InstanceManager.class });
                        if (! cst.isAccessible()) {
                            cst.setAccessible(true);
                        }
                        Object[] args = new Object[] {this};
                        onEntry(null, m_className, new Object[0]);
                        instance = cst.newInstance(args);
                        onExit(null, m_className, instance);
                    }
                }
            } catch (IllegalAccessException e) {
                m_factory.getLogger().log(Logger.ERROR,
                                          "[" + m_name + "] createInstance -> The POJO constructor is not accessible : " + e.getMessage());
                stop();
            } catch (SecurityException e) {
                m_factory.getLogger().log(
                                          Logger.ERROR,
                                          "["
                                                  + m_name
                                                  + "] createInstance -> The Component Instance is not accessible (security reason) : "
                                                  + e.getMessage());
                stop();
            } catch (InvocationTargetException e) {
                m_factory.getLogger().log(
                                          Logger.ERROR,
                                          "["
                                                  + m_name
                                                  + "] createInstance -> Cannot invoke the constructor method (illegal target) : "
                                                  + e.getTargetException().getMessage());
                onError(null, m_className, e.getTargetException());
                stop();
            } catch (NoSuchMethodException e) {
                m_factory.getLogger().log(Logger.ERROR,
                                          "[" + m_name + "] createInstance -> Cannot invoke the constructor (method not found) : " + e.getMessage());
                stop();
            } catch (IllegalArgumentException e) {
                m_factory.getLogger().log(Logger.ERROR,
                                          "[" + m_name + "] createInstance -> The POJO constructor invocation failed : " + e.getMessage());
                stop();
            } catch (InstantiationException e) {
                m_factory.getLogger().log(Logger.ERROR,
                                          "[" + m_name + "] createInstance -> The POJO constructor invocation failed : " + e.getMessage());
                stop();
            }
        } else {
            try {
                // Build the pojo object with the factory-method.
                Method factory = null;
                // Try with the bundle context
                try {
                    factory = m_clazz.getDeclaredMethod(m_factoryMethod, new Class[] { BundleContext.class });
                    if (! factory.isAccessible()) {
                        factory.setAccessible(true);
                    }
                    Object[] args = new Object[] { m_context };
                    onEntry(null, m_className, args);
                    instance = factory.invoke(null, new Object[] { m_context });
                } catch (NoSuchMethodException e1) {
                    // Try without the bundle context
                    try {
                        factory = m_clazz.getDeclaredMethod(m_factoryMethod, new Class[0]);
                        if (! factory.isAccessible()) {
                            factory.setAccessible(true);
                        }
                        Object[] args = new Object[0];
                        onEntry(null, m_className, args);
                        instance = factory.invoke(null, args);
                    } catch (NoSuchMethodException e2) {
                        // Error : factory-method not found
                        m_factory.getLogger().log(
                                                  Logger.ERROR,
                                                  "["
                                                          + m_name
                                                          + "] createInstance -> Cannot invoke the factory-method (method not found) : "
                                                          + e2.getMessage());
                        stop();
                    }
                }

                // Now call the setInstanceManager method.
                Method method = instance.getClass().getDeclaredMethod("_setInstanceManager", new Class[] { InstanceManager.class });
                if (!method.isAccessible()) {
                    method.setAccessible(true);
                }
                method.invoke(instance, new Object[] { this });
                onExit(null, m_className, instance);

            } catch (SecurityException e) {
                // Error : invocation failed
                m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the factory-method : " + e.getMessage());
                stop();
            } catch (IllegalArgumentException e) {
                // Error : arguments mismatch
                m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the factory-method : " + e.getMessage());
                stop();
            } catch (IllegalAccessException e) {
                // Error : illegal access
                m_factory.getLogger().log(Logger.ERROR, "[" + m_name + "] createInstance -> Cannot invoke the factory-method : " + e.getMessage());
                stop();
            } catch (InvocationTargetException e) {
                // Error : invocation failed
                m_factory.getLogger().log(Logger.ERROR,
                                          "[" + m_name + "] createInstance -> The factory-method returns an exception : " + e.getTargetException());
                onError(null, m_className, e.getTargetException());
                stop();
            } catch (NoSuchMethodException e) {
                // Error : _setInstanceManager method is missing
                m_factory.getLogger()
                        .log(
                             Logger.ERROR,
                             "["
                                     + m_name
                                     + "] createInstance -> Cannot invoke the factory-method (the _setInstanceManager method does not exist) : "
                                     + e.getMessage());
                stop();
            }

        }
        
        

        // Add the new instance in the instance list.
        synchronized (this) {
            if (m_pojoObjects == null) {
                m_pojoObjects = new ArrayList(1);
            }
            m_pojoObjects.add(instance);
        }
        // Call createInstance on Handlers :
        for (int i = 0; i < m_handlers.length; i++) {
            ((PrimitiveHandler) m_handlers[i].getHandler()).onCreation(instance);
        }
        
        return instance;
    }

    /**
     * Get the first object created by the instance. If no object created, create and return one object.
     * @return the instance of the component instance to use for singleton component
     */
    public Object getPojoObject() {
        Object pojo = null;
        synchronized (this) {
            if (m_pojoObjects != null) {
                pojo = m_pojoObjects.get(0); // Stack confinement
            }
        }
        
        if (pojo == null) {
            return createPojoObject(); // This method must be called without the lock.
        } else {
            return pojo;
        }
    }

    /**
     * Get the manipulated class.
     * The method does not need to be synchronized.
     * Reassigning the internal class will use the same class object.
     * @return the manipulated class
     */
    public Class getClazz() {
        if (m_clazz == null) {
            load();
        }
        return m_clazz;
    }

    /**
     * Register an handler. The handler will be notified of event on each field given in the list.
     * @param handler : the handler to register
     * @param fields : the field metadata list
     * @param methods : the method metadata list
     * @deprecated use register(FieldMetadata fm, FieldInterceptor fi) and register(MethodMetadata mm, MethodInterceptor mi) instead. 
     */
    public void register(PrimitiveHandler handler, FieldMetadata[] fields, MethodMetadata[] methods) {
        for (int i = 0; fields != null && i < fields.length; i++) {
            register(fields[i], handler);
        }
        for (int i = 0; methods != null && i < methods.length; i++) {
            register(methods[i], handler);
        }

    }
    
    /**
     * Register a field interceptor.
     * @param field : intercepted field
     * @param interceptor : interceptor
     */
    public void register(FieldMetadata field, FieldInterceptor interceptor) {
        if (m_fieldRegistration == null) {
            m_fieldRegistration = new HashMap();
            m_fieldRegistration.put(field.getFieldName(), new FieldInterceptor[] { interceptor });
        } else {
            FieldInterceptor[] list = (FieldInterceptor[]) m_fieldRegistration.get(field.getFieldName());
            if (list == null) {
                m_fieldRegistration.put(field.getFieldName(), new FieldInterceptor[] { interceptor });
            } else {
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == interceptor) {
                        return;
                    }
                }
                FieldInterceptor[] newList = new FieldInterceptor[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = interceptor;
                m_fieldRegistration.put(field.getFieldName(), newList);
            }
        }
    }
    
    /**
     * Register a method interceptor.
     * @param method : intercepted method
     * @param interceptor : interceptor
     */
    public void register(MethodMetadata method, MethodInterceptor interceptor) {
        if (m_methodRegistration == null) {
            m_methodRegistration = new HashMap();
            m_methodRegistration.put(method.getMethodIdentifier(), new MethodInterceptor[] { interceptor });
        } else {
            MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(method.getMethodIdentifier());
            if (list == null) {
                m_methodRegistration.put(method.getMethodIdentifier(), new MethodInterceptor[] { interceptor });
            } else {
                for (int j = 0; j < list.length; j++) {
                    if (list[j] == interceptor) {
                        return;
                    }
                }
                MethodInterceptor[] newList = new MethodInterceptor[list.length + 1];
                System.arraycopy(list, 0, newList, 0, list.length);
                newList[list.length] = interceptor;
                m_methodRegistration.put(method.getMethodIdentifier(), newList);
            }
        }
    }

    /**
     * This method is called by the manipulated class each time that a GETFIELD instruction is found. The method ask to each handler which value need
     * to be returned.
     * @param pojo : the pojo object on which the field was get
     * @param fieldName : the field name on which the GETFIELD instruction is called
     * @return the value decided by the last asked handler (throw a warning if two fields decide two different values)
     */
    public Object  onGet(Object pojo, String fieldName) {
        Object initialValue = null;
        synchronized (this) { // Stack confinement.
            initialValue = m_fields.get(fieldName);
        }
        Object result = initialValue;
        // Get the list of registered handlers

        FieldInterceptor[] list = (FieldInterceptor[]) m_fieldRegistration.get(fieldName); // Immutable list.
        for (int i = 0; list != null && i < list.length; i++) {
            // Call onGet outside of a synchronized block.
            Object handlerResult = list[i].onGet(null, fieldName, initialValue);
            if (handlerResult == initialValue) {
                continue; // Non-binding case (default implementation).
            } else {
                if (result != initialValue) {
                    if ((handlerResult != null && !handlerResult.equals(result)) || (result != null && handlerResult == null)) {
                        m_factory.getLogger().log(
                                                  Logger.WARNING,
                                                  "A conflict was detected on the injection of "
                                                          + fieldName);
                    }
                }
                result = handlerResult;
            }
        }

        if ((result != null && !result.equals(initialValue)) || (result == null && initialValue != null)) {
            // A change occurs => notify the change
            synchronized (this) {
                m_fields.put(fieldName, result);
            }
            // Call onset outside of a synchronized block.
            for (int i = 0; list != null && i < list.length; i++) {
                list[i].onSet(null, fieldName, result);
            }
        }

        return result;
    }

    /**
     * Dispatch entry method event on registered handler.
     * @param pojo : the pojo object on which method is invoked.
     * @param methodId : method id
     * @param args : argument array
     */
    public void onEntry(Object pojo, String methodId, Object[] args) {
        if (m_methodRegistration == null) { // Immutable field.
            return;
        }
        MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(methodId);
        Method method = getMethodById(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onEntry(pojo, method, args); // Outside a synchronized block.
        }
    }

    /**
     * Dispatch exit method event on registered handler. The given returned object is an instance of Exception if the method has launched an
     * exception. If the given object is null, either the method returns void, either the method has returned null.
     * @param pojo : the pojo object on which the method was invoked
     * @param methodId : method id
     * @param result : returned object.
     */
    public void onExit(Object pojo, String methodId, Object result) {
        if (m_methodRegistration == null) {
            return;
        }
        MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(methodId);
        Method method = getMethodById(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onExit(pojo, method, result);
        }
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onFinally(pojo, method);
        }
    }

    /**
     * Dispatch error method event on registered handler. The given returned object is an instance of Exception if the method has thrown an exception.
     * If the given object is null, either the method returns void, either the method has returned null.
     * @param pojo : the pojo object on which the method was invoked
     * @param methodId : method id
     * @param error : throwable object.
     */
    public void onError(Object pojo, String methodId, Throwable error) {        
        if (m_methodRegistration == null) {
            return;
        }
        MethodInterceptor[] list = (MethodInterceptor[]) m_methodRegistration.get(methodId);
        Method method = getMethodById(methodId);
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onError(pojo, method, error);
        }
        for (int i = 0; list != null && i < list.length; i++) {
            list[i].onFinally(pojo, method);
        }
    }

    /**
     * Get method object by id.
     * @param methodId : method id
     * @return : the method object or null if the method cannot be found.
     */
    private Method getMethodById(String methodId) {
        // Not necessary synchronized as recomputing the methodID will give the same Method twice.
        Method method = (Method) m_methods.get(methodId);
        if (method == null) {
            Method[] mets = m_clazz.getDeclaredMethods();
            for (int i = 0; i < mets.length; i++) {
                // Check if the method was not already computed. If not, compute the Id and check.
                if (!m_methods.containsValue(mets[i]) && (MethodMetadata.computeMethodId(mets[i]).equals(methodId))) {
                    // Store the new methodId
                    m_methods.put(methodId, mets[i]);
                    return mets[i];
                }
            }
            // If not found, it is a constructor, return null in this case.
            if (methodId.equals(m_clazz.getName())) {
                // Constructor.
                return null;
            }
            // Cannot happen
            m_factory.getLogger().log(Logger.ERROR, "A methodID cannot be associate with a POJO method : " + methodId);
            return null;
        } else {
            return method;
        }
    }

    /**
     * This method is called by the manipulated class each time that a PUTFILED instruction is found. the method send to each handler the new value.
     * @param pojo : the pojo object on which the field was set
     * @param fieldName : the field name on which the PUTFIELD instruction is called
     * @param objectValue : the value of the field
     */
    public void onSet(Object pojo, String fieldName, Object objectValue) {
        Object value = null; // Stack confinement
        synchronized (this) {
            value = m_fields.get(fieldName);
        }
        if ((value != null && !value.equals(objectValue)) || (value == null && objectValue != null)) {
            synchronized (this) {
                m_fields.put(fieldName, objectValue);
            }
            FieldInterceptor[] list = (FieldInterceptor[]) m_fieldRegistration.get(fieldName);
            for (int i = 0; list != null && i < list.length; i++) {
                list[i].onSet(null, fieldName, objectValue); // Outside a synchronized block.
            }
        }
    }

    /**
     * Get the bundle context used by this component instance.
     * @return the context of the component.
     * @see org.apache.felix.ipojo.ComponentInstance#getContext()
     */
    public BundleContext getContext() {
        return m_context; // Immutable
    }

    public BundleContext getGlobalContext() {
        return ((IPojoContext) m_context).getGlobalContext(); // Immutable
    }

    public ServiceContext getLocalServiceContext() {
        return ((IPojoContext) m_context).getServiceContext(); // Immutable
    }

    /**
     * Get the instance name.
     * @return the instance name.
     * @see org.apache.felix.ipojo.ComponentInstance#getInstanceName()
     */
    public String getInstanceName() {
        return m_name; // Immutable
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
        // We synchronized the state computation.
        synchronized (this) {
            if (m_state == INVALID) {
                // Try to revalidate the instance after reconfiguration
                for (int i = 0; i < m_handlers.length; i++) {
                    if (m_handlers[i].getState() != VALID) {
                        return;
                    }
                }
                setState(VALID);
            }
        }
    }

    /**
     * Get the implementation class of the component type.
     * This method does not need to be synchronized as the
     * class name is constant once set. 
     * @return the class name of the component type.
     */
    public String getClassName() {
        return m_className;
    }

    /**
     * State Change listener callback. This method is notified at each time a plugged handler becomes invalid.
     * @param instance : changing instance
     * @param newState : new state
     * @see org.apache.felix.ipojo.InstanceStateListener#stateChanged(org.apache.felix.ipojo.ComponentInstance, int)
     */
    public void stateChanged(ComponentInstance instance, int newState) {
        int state;
        synchronized (this) {
            if (m_state <= STOPPED) {
                return;
            } else {
                state = m_state; // Stack confinement
            }
        }

        // Update the component state if necessary
        if (newState == INVALID && state == VALID) {
            // Need to update the state to UNRESOLVED
            setState(INVALID);
            return;
        }
        if (newState == VALID && state == INVALID) {
            // An handler becomes valid => check if all handlers are valid
            for (int i = 0; i < m_handlers.length; i++) {
                if (m_handlers[i].getState() != VALID) {
                    return;
                }
            }
            setState(VALID);
            return;
        }
    }

    /**
     * Get the list of registered fields. This method is invoked by the POJO itself.
     * @return the set of registered fields.
     */
    public Set getRegistredFields() {
        if (m_fieldRegistration == null) {
            return null;
        }
        return m_fieldRegistration.keySet();
    }

    /**
     * Get the list of registered methods. This method is invoked by the POJO itself.
     * @return the set of registered methods.
     */
    public Set getRegistredMethods() {
        if (m_methodRegistration == null) {
            return null;
        } else {
            return m_methodRegistration.keySet();
        }
    }
}
