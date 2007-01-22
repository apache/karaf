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
package org.apache.felix.ipojo.handlers.dependency;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Represent a service dependency of the component instance.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class Dependency implements ServiceListener {

    /**
     * Dependency State : RESOLVED.
     */
    public static final int RESOLVED = 1;

    /**
     * Dependency State : UNRESOLVED.
     */
    public static final int UNRESOLVED = 2;

    /**
     * Reference on the Dependency Handler.
     */
    private DependencyHandler m_handler;
    
    /**
     * Field of the dependency.
     */
    private String m_field;
    
    /**
     * List of dependency callback. 
     */
    private DependencyCallback[] m_callbacks = new DependencyCallback[0];
    
    /**
     * Service Specification required by the dependency.
     */
    private String m_specification;
    
    /**
     * Is the dependency a multiple dependency ?
     */
    private boolean m_isMultiple = false;
    
    /**
     * Is the Dependency an optional dependency ?
     */
    private boolean m_isOptional = false;
    
    /**
     * LDAP Filter of the Dependency (String form).
     */
    private String m_strFilter;
    

    /**
     * Array of Service Objects.
     * When cardinality = 1 : just the first element is returned
     * When cardinality = ?..n : all the array is returned
     * m_services : Array
     */
    private Object[] m_services = new Object[0];

    /**
     * Array of service references.
     * m_ref : Array
     */
    private ServiceReference[] m_ref = new ServiceReference[0];

    /**
     * State of the dependency.
     * 0 : stopped, 1 : valid, 2 : invalid.
     * m_state : int
     */
    private int m_state;

    /**
     * True if the reference list change after the creation of a service object array.
     */
    private boolean m_change;

    /**
     * Class of the dependency.
     * Usefull to create in the case of multiple dependency
     */
    private Class m_clazz;

    /**
     * LDAP Filter of the dependency.
     */
    private Filter m_filter;


    /**
     * Dependency contructor. After the creation the dependency is not started.
     * @param dh : the dependency handler managing this dependency
     * @param dm : the depednency metadata
     */
    public Dependency(DependencyHandler dh, String field, String spec, String filter, boolean isOptional) {
        m_handler = dh;
        m_field = field;
        m_specification = spec;
        m_isOptional = isOptional;
        m_strFilter = filter;
    }

    /**
     * @return the field attached to the dependency.
     */
    public String getField() { return m_field; }
    
    /**
     * @return the specification tracked by the dependency.
     */
    public String getSpecification() { return m_specification; }
    
    /**
     * @return true is the dependency is optional.
     */
    public boolean isOptional() { return m_isOptional; }
    
    /**
     * @return true if the dependency is multiple.
     */
    public boolean isMultiple() { return m_isMultiple; }
    
    /**
     * Set the dependency to multiple.
     */
    protected void setMultiple() { m_isMultiple = true; }
    
    /**
     * Set the tracked specification for this dependency.
     * @param spec : the tracked specification (interface name)
     */
    protected void setSpecification(String spec) { m_specification = spec; }
    
    /**
     * Add a callback to the dependency.
     * @param cb : callback to add
     */
    protected void addDependencyCallback(DependencyCallback cb) {
        if (m_callbacks.length > 0) {
            DependencyCallback[] newCallbacks = new DependencyCallback[m_callbacks.length + 1];
            System.arraycopy(m_callbacks, 0, newCallbacks, 0, m_callbacks.length);
            newCallbacks[m_callbacks.length] = cb;
            m_callbacks = newCallbacks;
        }
        else {
            m_callbacks = new DependencyCallback[] {cb};
        }
    }
    
    public String getFilter() { return m_strFilter; }

    /**
     * @return the dependency handler of this dependency.
     */
    public DependencyHandler getDependencyHandler() { return m_handler; }

    /**
     * @return the used service.
     */
    public HashMap getUsedServices() {
        HashMap hm = new HashMap();
        if (m_isMultiple) {
            for (int i = 0; i < m_ref.length; i++) {
                if (i < m_services.length) { hm.put(((Object) m_services[i]).toString(), m_ref[i]); }
            }
        } else {
            if (m_ref.length != 0 && m_services.length != 0) { hm.put((m_services[0]).toString(), m_ref[0]); }
        }
        return hm;
    }

    /**
     * A dependency is satisfied if it is optional of ref.length != 0.
     * @return true is the dependency is satified
     */
    protected boolean isSatisfied() {
        return m_isOptional || m_ref.length != 0;
    }

    /**
     * This method is called by the replaced code in the component implementation class.
     * Construct the service object list is necessary.
     * @return null or a service object or a list of service object according to the dependency.
     */
    protected Object get() {
       // m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Call get for a dependency on : " + m_metadata.getServiceSpecification()
       //         + " Multiple : " + m_metadata.isMultiple() + " Optional : " + m_metadata.isOptional());
        try {

            // 1 : Test if there is any change in the reference list :
            if (!m_change) {
                if (!m_isMultiple) {
                    if (m_services.length > 0) {
                        return m_services[0]; }
                }
                else {
                    return m_services;
                }
            }

            // 2 : Else there is a change in the list -> recompute the m_services array
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Create a service array of " + m_clazz.getName());
            m_services = (Object[]) Array.newInstance(m_clazz, m_ref.length);

            for (int i = 0; i < m_ref.length; i++) {
                m_services[i] = m_handler.getInstanceManager().getContext().getService(m_ref[i]);
            }

            m_change = false;
   //         m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Create an array with the size " + m_services.length);


            // 3 : The service object list is populated, I return either the first service object, either the array.
            // Return null or an empty array if no service are found.
            if (!m_isMultiple) {
                if (m_services.length > 0) {
                    return m_services[0];
                } else {
                    // Load the nullable class
                    //String[] segment = m_metadata.getServiceSpecification().split("[.]");
                    //String className = "org.apache.felix.ipojo." + segment[segment.length - 1] + "Nullable";
                	String className = m_specification + "Nullable";
            //        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Try to load the nullable class for " + getMetadata().getServiceSpecification() + " -> " + className);
                    Class nullableClazz = m_handler.getNullableClass(className);

                    if (nullableClazz == null) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Cannot load the nullable class to return a dependency object for " + m_field + " -> " + m_specification);
                        return null;
                    }

                    // The nullable class is loaded, create the object and return it
                    Object instance = nullableClazz.newInstance();
             //       m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Nullable object created for " + getMetadata().getServiceSpecification() + " -> " + instance);
                    return instance;
                }
            }
            else { // Multiple dependency
                return m_services;
            }
        } catch (Exception e) {
            // There is a problem in the dependency resolving (like in stopping method)
            if (!m_isMultiple) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Return null, an exception was throwed in the get method", e);
                return null; }
            else {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Return an empty array, an exception was throwed in the get method", e);
                return Array.newInstance(m_clazz, 0); }
        }
    }

    /**
     * Method calld when a service event is throwed.
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     * @param event : the received service event
     */
    public void serviceChanged(ServiceEvent event) {
        synchronized (this) {

            // If a service goes way.
            if (event.getType() == ServiceEvent.UNREGISTERING) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] A service is gone -> " + event.getServiceReference().getBundle());
                if (containsSR(event.getServiceReference())) { departureManagement(event.getServiceReference()); }
                return;
            }

            // If a service arrives
            if (event.getType() == ServiceEvent.REGISTERED) {
                if (m_filter.match(event.getServiceReference())) { arrivalManagement(event.getServiceReference()); return; }
                else { return; }
            }

            // If a service is modified
            if (event.getType() == ServiceEvent.MODIFIED) {
                if (m_filter.match(event.getServiceReference())) {
                    m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] A service with a filter matching is arrived -> " + event.getServiceReference().getBundle());
                    if (!containsSR(event.getServiceReference())) { arrivalManagement(event.getServiceReference()); }
                }
                else {
                    m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] A service with a filter matching has gone -> " + event.getServiceReference().getBundle());
                    if (containsSR(event.getServiceReference())) { departureManagement(event.getServiceReference()); }
                }
                return;
            }

        }
    }

    /**
     * Method called when a service arrives.
     * @param ref : the arriving service reference
     */
    private void arrivalManagement(ServiceReference ref) {
        // Add the new service inside the ref list
        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Add a service for a dependency");
        addReference(ref);
        if (isSatisfied()) {
            m_state = RESOLVED;
            if (m_isMultiple || m_ref.length == 1) { m_change = true; callBindMethod(ref); }
        }
        m_handler.checkContext();
    }

    /**
     * Method called when a service goes away.
     * @param ref : the leaving service reference
     */
    private void departureManagement(ServiceReference ref) {
        // Call unbind method
        if (!m_isMultiple && ref == m_ref[0]) { callUnbindMethod(ref); }
        if (m_isMultiple) { callUnbindMethod(ref); }

        // Unget the service reference
        m_handler.getInstanceManager().getContext().ungetService(ref);
        int index = removeReference(ref);

        // Is the state valid or invalid
        if (m_ref.length == 0 && !m_isOptional) {
            m_state = UNRESOLVED;
        }
        if (m_ref.length == 0 && m_isOptional) {
            m_state = RESOLVED;
        }
        // Is there any change ?
        if (!m_isMultiple && index == 0) {
            m_change = true;
            if (m_ref.length != 0) { callBindMethod(m_ref[0]); }
        }
        if (!m_isMultiple && index != 0) { m_change = false; }
        if (m_isMultiple) { m_change = true;  }

        m_handler.checkContext();
        return;
    }

    /**
     * Call unbind callback method.
     * @param ref : reference to send (if accepted) to the method
     */
    private void callUnbindMethod(ServiceReference ref) {
        if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
            for (int i = 0; i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.UNBIND) {
                    // Try to call the bind method with a service reference inside
                    try {
                        m_callbacks[i].call(ref);
                    } catch (NoSuchMethodException e) {
                        // The method was not found : try without service reference
                        try {
                            m_callbacks[i].call();
                        } catch (NoSuchMethodException e1) {
                            // The method was not found : try with the service object
                            try {
                                m_callbacks[i].call(m_handler.getInstanceManager().getContext().getService(ref));
                            } catch (NoSuchMethodException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Unbind method not found", e2);
                                return;
                            } catch (IllegalAccessException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on unbind method", e2);
                                return;
                            } catch (InvocationTargetException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the unbind method", e2);
                                return;
                            }
                        } catch (IllegalAccessException e1) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on unbind method", e1);
                            return;
                        } catch (InvocationTargetException e1) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the unbind method", e1);
                            return;
                        }

                    } catch (IllegalAccessException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e);
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Call the bind method.
     * @param instance : instance on which calling the bind method.
     */
    protected void callBindMethod(Object instance) {
        // Check optional case : nullable object case : do not call bind on nullable object
        if (m_isOptional && m_ref.length == 0) { return; }


        if (m_isMultiple) {
            for (int i = 0; i < m_ref.length; i++) {
                for (int j = 0; j < m_callbacks.length; j++) {
                    if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                        // Try to call the bind method with a service reference inside
                        try {
                            m_callbacks[j].callOnInstance(instance, m_ref[i]);
                        } catch (NoSuchMethodException e) {
                            // The method was not found : try without service reference
                            try {
                                m_callbacks[j].callOnInstance(instance);
                            } catch (NoSuchMethodException e1) {
                                // The method was not found : try with the service object
                                try {
                                    m_callbacks[j].callOnInstance(instance, m_handler.getInstanceManager().getContext().getService(m_ref[i]));
                                } catch (NoSuchMethodException e2) {
                                    m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Bind method not found", e2);
                                    return;
                                } catch (IllegalAccessException e2) {
                                    m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e2);
                                    return;
                                } catch (InvocationTargetException e2) {
                                    m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e2);
                                    return;
                                }
                            } catch (IllegalAccessException e1) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e1);
                                return;
                            } catch (InvocationTargetException e1) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e1);
                                return;
                            }
                        } catch (IllegalAccessException e) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e);
                            return;
                        } catch (InvocationTargetException e) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method" + e);
                            return;
                        }
                    }
                }
            }
        } else {
            for (int j = 0; j < m_callbacks.length; j++) {
                if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                    // Try to call the bind method with a service reference inside
                    try {
                        m_callbacks[j].callOnInstance(instance, m_ref[0]);
                    } catch (NoSuchMethodException e) {
                        // The method was not found : try without service reference
                        try {
                            m_callbacks[j].callOnInstance(instance);
                        } catch (NoSuchMethodException e1) {
                            // The method was not found : try with the service object
                            try {
                                m_callbacks[j].callOnInstance(instance, m_handler.getInstanceManager().getContext().getService(m_ref[0]));
                            } catch (NoSuchMethodException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Bind method not found", e2);
                                return;
                            } catch (IllegalAccessException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e2);
                                return;
                            } catch (InvocationTargetException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e2);
                                return;
                            }
                        } catch (IllegalAccessException e1) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e1);
                            return;
                        } catch (InvocationTargetException e1) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e1);
                            return;
                        }

                    } catch (IllegalAccessException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e);
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Call bind method with the service reference in parameter (if accepted).
     * @param ref : the service reference of the new service
     */
    private void callBindMethod(ServiceReference ref) {
        // call bind method :
        if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
            for (int i = 0; i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.BIND) {
                    // Try to call the bind method with a service reference inside
                    try {
                        m_callbacks[i].call(ref);
                    } catch (NoSuchMethodException e) {
                        // The method was not found : try without service reference
                        try {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Call the Bind method");
                            m_callbacks[i].call();
                        } catch (NoSuchMethodException e1) {
                            // The method was not found : try with the service object
                            try {
                                m_callbacks[i].call(m_handler.getInstanceManager().getContext().getService(ref));
                            } catch (NoSuchMethodException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Bind method not found", e2);
                                return;
                            } catch (IllegalAccessException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e2);
                                return;
                            } catch (InvocationTargetException e2) {
                                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e2);
                                return;
                            }
                        } catch (IllegalAccessException e1) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e1);
                            return;
                        } catch (InvocationTargetException e1) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e1);
                            return;
                        }

                    } catch (IllegalAccessException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Illegal access on bind method", e);
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Dependency Callback Error : Invocation Target Exception in the bind method", e);
                        return;
                    }
                }
            }
        }
    }

    /**
     * Start the dependency.
     */
    public void start() {
        // Construct the filter with the objectclass + filter
        String classnamefilter = "(objectClass=" + m_specification + ")";
        String filter = "";
        if (!m_strFilter.equals("")) {
            filter = "(&" + classnamefilter + m_strFilter + ")";
        }
        else {
            filter = classnamefilter;
        }

        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Start a dependency on : " + m_specification + " with " + m_strFilter);
        m_state = UNRESOLVED;

        try {
            m_clazz = m_handler.getInstanceManager().getContext().getBundle().loadClass(m_specification);
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot load the interface class for the dependency " + m_field + " [" + m_specification + "]");
            e.printStackTrace();
        }

        try {
            // Look if the service is already present :
            ServiceReference[] sr = m_handler.getInstanceManager().getContext().getServiceReferences(m_specification, filter);
            if (sr != null) {
                for (int i = 0; i < sr.length; i++) { addReference(sr[i]); }
                m_state = RESOLVED;
            }
            // Register a listener :
            m_handler.getInstanceManager().getContext().addServiceListener(this);
            m_filter = m_handler.getInstanceManager().getContext().createFilter(filter); // Store the filter
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Create a filter from : " + filter);
            m_change = true;
        }
        catch (InvalidSyntaxException e1) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] A filter is malformed : " + filter);
            e1.printStackTrace();
        }
    }

    /**
     * Stop the dependency.
     */
    public void stop() {
        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Stop a dependency on : " + m_specification + " with " + m_strFilter);
        m_state = UNRESOLVED;

        // Unget all services references
        for (int i = 0; i < m_ref.length; i++) {
            m_handler.getInstanceManager().getContext().ungetService(m_ref[i]);
        }

        m_ref = new ServiceReference[0];
        m_handler.getInstanceManager().getContext().removeServiceListener(this);
        m_clazz = null;
        m_services = new Object[0];
    }

    /**
     * Return the state of the dependency.
     * @return the state of the dependency (1 : valid, 2 : invalid)
     */
    public int getState() {
        return ( m_isOptional ) ? 1 : m_state;
    }

    /**
     * Return the list of service reference.
     * @return the service reference list.
     */
    public ServiceReference[] getServiceReferences() {
        return m_ref;
    }

    /**
     * Add a service reference in the current list.
     * @param r : the new service reference to add
     */
    private void addReference(ServiceReference r) {
        for (int i = 0; (m_ref != null) && (i < m_ref.length); i++) {
            if (m_ref[i] == r) {
                return;
            }
        }

        if (m_ref != null) {
            ServiceReference[] newSR = new ServiceReference[m_ref.length + 1];
            System.arraycopy(m_ref, 0, newSR, 0, m_ref.length);
            newSR[m_ref.length] = r;
            m_ref = newSR;
        }
        else {
            m_ref = new ServiceReference[] {r};
        }
    }

    /**
     * Find if a service registration il already registred.
     * @param sr : the service registration to find.
     * @return true if the service registration is already in the array
     */
    private boolean containsSR(ServiceReference sr) {
        for (int i = 0; i < m_ref.length; i++) {
            if (m_ref[i] == sr) {
                return true;
            }
        }
        return false;
    }

    /**
     * Remove a service reference in the current list.
     * @param r : the new service reference to remove
     * @return the index of the founded element, or -1 if the element is not found
     */
    private int removeReference(ServiceReference r) {
        if (m_ref == null) {
            m_ref = new ServiceReference[0];
        }

        int idx = -1;
        for (int i = 0; i < m_ref.length; i++) {
            if (m_ref[i] == r) {
                idx = i;
                break;
            }
        }

        if (idx >= 0) {
            // If this is the module, then point to empty list.
            if ((m_ref.length - 1) == 0) {
                m_ref = new ServiceReference[0];
            }
            // Otherwise, we need to do some array copying.
            else {
                ServiceReference[] newSR = new ServiceReference[m_ref.length - 1];
                System.arraycopy(m_ref, 0, newSR, 0, idx);
                if (idx < newSR.length)             {
                    System.arraycopy(
                            m_ref, idx + 1, newSR, idx, newSR.length - idx);
                }
                m_ref = newSR;
            }
        }
        return idx;
    }


}
