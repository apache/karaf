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
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.IPojoContext;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.composite.CompositeServiceContext;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;

/**
 * Represent a service dependency of the component instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
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
     * Dependency ID (declared ID, if not declare use the specification).
     */
    private String m_id;

    /**
     * Is the dependency a multiple dependency ?
     */
    private boolean m_isAggregate = false;

    /**
     * Is the Dependency an optional dependency ?
     */
    private boolean m_isOptional = false;

    /**
     * LDAP Filter of the Dependency (String form).
     */
    private String m_strFilter;
    
    /**
     * Is the dependency a service level dependency.
     */
    private boolean m_isServiceLevelRequirement = false;
    
    /**
     * Resolution policy.
     */
    private int m_policy = PolicyServiceContext.LOCAL_AND_GLOBAL; 

    /**
     * Array of Service Objects. When cardinality = 1 : just the first element
     * is returned When cardinality = ?..n : all the array is returned
     * m_services : Array
     */
    private Object[] m_services = new Object[0];

    /**
     * Array of service references.
     * m_ref : Array
     */
    private List m_references = new ArrayList();
    
    /**
     * Array of service reference containing used service references. 
     */
    private List m_usedReferences = new ArrayList();

    /**
     * State of the dependency. 0 : stopped, 1 : valid, 2 : invalid. 
     * m_state : int
     */
    private int m_state;

    /**
     * True if the reference list change after the creation of a service object
     * array.
     */
    private boolean m_change;

    /**
     * Class of the dependency. 
     * Useful to create in the case of multiple dependency
     */
    private Class m_clazz;

    /**
     * LDAP Filter of the dependency.
     */
    private Filter m_filter;
    
    /**
     * Service Context in which resolving the dependency.
     */
    private ServiceContext m_serviceContext;

    /**
     * Dependency constructor. After the creation the dependency is not started.
     * 
     * @param dh : the dependency handler managing this dependency
     * @param field : field of the dependency
     * @param spec : required specification
     * @param filter : LDAP filter of the dependency
     * @param isOptional : is the dependency an optional dependency ?
     * @param isAggregate : is the dependency an aggregate dependency
     * @param id : id of the dependency, may be null
     * @param policy : resolution policy
     */
    public Dependency(DependencyHandler dh, String field, String spec, String filter, boolean isOptional, boolean isAggregate, String id, int policy) {
        m_handler = dh;
        m_field = field;
        m_specification = spec;
        m_isOptional = isOptional;
        m_strFilter = filter;
        m_isAggregate = isAggregate;
        if (m_id == null) {
            m_id = m_specification;
        } else {
            m_id = id;
        }
        if (policy != -1) {
            m_policy = policy;
        }
        // Fix the policy according to the level
        if ((m_policy == PolicyServiceContext.LOCAL_AND_GLOBAL || m_policy == PolicyServiceContext.LOCAL) && ! ((((IPojoContext) m_handler.getInstanceManager().getContext()).getServiceContext()) instanceof CompositeServiceContext)) {
            // We are not in a composite : BOTH | STRICT => GLOBAL
            m_policy = PolicyServiceContext.GLOBAL;
        }
    }

    public String getField() {
        return m_field;
    }


    public String getSpecification() {
        return m_specification;
    }


    public boolean isOptional() {
        return m_isOptional;
    }


    public boolean isAggregate() {
        return m_isAggregate;
    }

    /**
     * Set the dependency to aggregate.
     */
    protected void setAggregate() {
        m_isAggregate = true;
    }

    /**
     * Set the tracked specification for this dependency.
     * 
     * @param spec : the tracked specification (interface name)
     */
    protected void setSpecification(String spec) {
        m_specification = spec;
    }

    /**
     * Add a callback to the dependency.
     * 
     * @param cb : callback to add
     */
    protected void addDependencyCallback(DependencyCallback cb) {
        if (m_callbacks.length > 0) {
            DependencyCallback[] newCallbacks = new DependencyCallback[m_callbacks.length + 1];
            System.arraycopy(m_callbacks, 0, newCallbacks, 0, m_callbacks.length);
            newCallbacks[m_callbacks.length] = cb;
            m_callbacks = newCallbacks;
        } else {
            m_callbacks = new DependencyCallback[] { cb };
        }
    }


    public String getFilter() {
        return m_strFilter;
    }


    public DependencyHandler getDependencyHandler() {
        return m_handler;
    }

    /**
     * Build the List [service reference] of used services.
     * @return the used service.
     */
    public List getUsedServices() {
        List list = new ArrayList();
        if (m_isAggregate) {
            list.addAll(m_usedReferences);
            return list;
        } else {
            if (m_usedReferences.size() != 0 && m_services.length != 0) {
                list.add(m_usedReferences.get(0));
            } 
        }
        return list;
    }

    /**
     * A dependency is satisfied if it is optional or there is useful references.
     * 
     * @return true is the dependency is satisfied
     */
    protected boolean isSatisfied() {
        return m_isOptional || ! m_references.isEmpty();
    }

    /**
     * This method is called by the replaced code in the component
     * implementation class. Construct the service object list is necessary.
     * 
     * @return null or a service object or a list of service object according to
     * the dependency.
     */
    protected Object get() {
        try {
            // 1 : Test if there is any change in the reference list :
            if (!m_change) {
                if (!m_isAggregate) {
                    if (m_services.length > 0) {
                        return m_services[0];
                    }
                } else {
                    return m_services;
                }
            }

            // 2 : Else there is a change in the list -> recompute the
            // m_services array
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO,
                    "[" + m_handler.getInstanceManager().getClassName() + "] Create a service array of " + m_clazz.getName());
            

            buildServiceObjectArray();

            m_change = false;
            // m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO,
            // "[" + m_handler.getInstanceManager().getClassName() + "] Create
            // an array with the size " + m_services.length);

            // 3 : The service object list is populated, I return either the
            // first service object, either the array.
            // Return null or an empty array if no service are found.
            if (!m_isAggregate) {
                if (m_services.length > 0) {
                    return m_services[0];
                } else {
                    // Load the nullable class
                    // String[] segment =
                    // m_metadata.getServiceSpecification().split("[.]");
                    // String className = "org.apache.felix.ipojo." +
                    // segment[segment.length - 1] + "Nullable";
                    String className = m_specification + "Nullable";
                    // m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO,
                    // "[" + m_handler.getInstanceManager().getClassName() + "]
                    // Try to load the nullable class for " +
                    // getMetadata().getServiceSpecification() + " -> " +
                    // className);
                    Class nullableClazz = m_handler.getNullableClass(className);

                    if (nullableClazz == null) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.INFO,
                                "[" + m_handler.getInstanceManager().getClassName() + "] Cannot load the nullable class to return a dependency object for "
                                        + m_field + " -> " + m_specification);
                        return null;
                    }

                    // The nullable class is loaded, create the object and
                    // return it
                    Object instance = nullableClazz.newInstance();
                    // m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO,
                    // "[" + m_handler.getInstanceManager().getClassName() + "]
                    // Nullable object created for " +
                    // getMetadata().getServiceSpecification() + " -> " +
                    // instance);
                    return instance;
                }
            } else { // Multiple dependency
                return m_services;
            }
        } catch (Exception e) {
            // There is a problem in the dependency resolving (like in stopping
            // method)
            if (!m_isAggregate) {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "[" + m_handler.getInstanceManager().getClassName() + "] Return null, an exception was throwed in the get method", e);
                return null;
            } else {
                m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                        "[" + m_handler.getInstanceManager().getClassName() + "] Return an empty array, an exception was throwed in the get method", e);
                return Array.newInstance(m_clazz, 0);
            }
        }
    }

    /**
     * Create the service object array according to the resolving policy.
     */
    private void buildServiceObjectArray() {
        if (m_isAggregate) {
            m_services = (Object[]) Array.newInstance(m_clazz, m_references.size());
            for (int i = 0; i < m_references.size(); i++) {
                m_services[i] = getService((ServiceReference) m_references.get(i));
            }
        } else {
            if (m_references.size() == 0) {
                m_services = new Object[0];
            } else {
                m_services = new Object[] { getService((ServiceReference) m_references.get(0)) };
            }
        }
    }

    /**
     * Method called when a service event occurs.
     * 
     * @see org.osgi.framework.ServiceListener#serviceChanged(org.osgi.framework.ServiceEvent)
     * @param event :
     *            the received service event
     */
    public void serviceChanged(ServiceEvent event) {
        synchronized (this) {

            // If a service goes way.
            if (event.getType() == ServiceEvent.UNREGISTERING) {
                if (m_references.contains(event.getServiceReference())) {
                    departureManagement(event.getServiceReference());
                }
                return;
            }

            // If a service arrives
            if (event.getType() == ServiceEvent.REGISTERED) {
                if (m_filter.match(event.getServiceReference())) {
                    arrivalManagement(event.getServiceReference());
                }
                return;
            }
            // If a service is modified
            if (event.getType() == ServiceEvent.MODIFIED) {
                if (m_filter.match(event.getServiceReference())) {
                    if (!m_references.contains(event.getServiceReference())) {
                        arrivalManagement(event.getServiceReference());
                    }
                } else {
                    if (m_references.contains(event.getServiceReference())) {
                        departureManagement(event.getServiceReference());
                    }
                }
                return;
            }

        }
    }

    /**
     * Method called when a service arrives.
     * 
     * @param ref : the arriving service reference
     */
    private void arrivalManagement(ServiceReference ref) {
        m_references.add(ref);
        if (isSatisfied()) {
            m_state = RESOLVED;
            if (m_isAggregate || ! m_references.isEmpty()) {
                m_change = true;
                callBindMethod(ref);
            }
        }
        m_handler.checkContext();
    }

    /**
     * Method called when a service goes away.
     * 
     * @param ref : the leaving service reference
     */
    private void departureManagement(ServiceReference ref) {
        // Call unbind method
        boolean hasChanged = false;
        if (m_usedReferences.contains(ref)) {
            callUnbindMethod(ref);
            // Unget the service reference
            ungetService(ref);
            hasChanged = true;
        }

        // Remove from the list (remove on both to be sure.
        m_references.remove(ref);

        // Is the state valid or invalid
        if (m_references.isEmpty() && !m_isOptional) {
            m_state = UNRESOLVED;
        } else {
            m_state = RESOLVED;
        }

        // Is there any change ?
        if (!m_isAggregate) {
            if (hasChanged) {
                m_change = true;
                if (!m_references.isEmpty()) {
                    callBindMethod((ServiceReference) m_references.get(0));
                }
            } else {
                m_change = false;
            }
        } else {
            m_change = true;
        }

        m_handler.checkContext();
        return;
    }

    /**
     * Call unbind callback method.
     * 
     * @param ref : reference to send (if accepted) to the method
     */
    private void callUnbindMethod(ServiceReference ref) {
        if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
            for (int i = 0; i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.UNBIND) {
                    try {
                        m_callbacks[i].call(ref, getService(ref));
                    } catch (NoSuchMethodException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " does not exist in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (IllegalAccessException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR,
                                "The method " + m_callbacks[i].getMethodName() + " is not accessible in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR,
                                "The method " + m_callbacks[i].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
                                        + "throws an exception : " + e.getMessage());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Get a service object for the given reference according to the resolving policy.
     * @param ref : service reference
     * @return the service object
     */
    private Object getService(ServiceReference ref) {
        if (!m_usedReferences.contains(ref)) {
            m_usedReferences.add(ref);
        }
        return m_serviceContext.getService(ref);
    }
    
    /**
     * Unget the given service reference according to the resolving policy.
     * @param ref : service reference to unget
     */
    private void ungetService(ServiceReference ref) {
        m_usedReferences.remove(ref);
        m_serviceContext.ungetService(ref);
    }

    /**
     * Call the bind method.
     * 
     * @param instance : instance on which calling the bind method.
     */
    protected void callBindMethod(Object instance) {
        // Check optional case : nullable object case : do not call bind on nullable object
        if (m_isOptional && m_references.isEmpty()) {
            return;
        }

        if (m_isAggregate) {
            for (int i = 0; i < m_references.size(); i++) {
                for (int j = 0; j < m_callbacks.length; j++) {
                    if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                        try {
                            m_callbacks[j].callOnInstance(instance, (ServiceReference) m_references.get(i), getService((ServiceReference) m_references.get(i)));
                        } catch (NoSuchMethodException e) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(
                                    Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " does not exist in the class "
                                            + m_handler.getInstanceManager().getClassName());
                            return;
                        } catch (IllegalAccessException e) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(
                                    Logger.ERROR,
                                    "The method " + m_callbacks[j].getMethodName() + " is not accessible in the class "
                                            + m_handler.getInstanceManager().getClassName());
                            return;
                        } catch (InvocationTargetException e) {
                            m_handler.getInstanceManager().getFactory().getLogger().log(
                                    Logger.ERROR,
                                    "The method " + m_callbacks[j].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
                                            + "thorws an exception : " + e.getMessage());
                            return;
                        }
                    }
                }
            }
        } else {
            for (int j = 0; j < m_callbacks.length; j++) {
                if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                    try {
                        m_callbacks[j].callOnInstance(instance, (ServiceReference) m_references.get(0), getService((ServiceReference) m_references.get(0)));
                    } catch (NoSuchMethodException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " does not exist in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (IllegalAccessException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR,
                                "The method " + m_callbacks[j].getMethodName() + " is not accessible in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR,
                                "The method " + m_callbacks[j].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
                                        + "thorws an exception : " + e.getMessage());
                        return;
                    }
                }
            }
        }
    }

    /**
     * Call bind method with the service reference in parameter (if accepted).
     * 
     * @param ref : the service reference of the new service
     */
    private void callBindMethod(ServiceReference ref) {
        // call bind method :
        if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
            for (int i = 0; i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.BIND) {
                    try {
                        m_callbacks[i].call(ref, getService(ref));
                    } catch (NoSuchMethodException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " does not exist in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (IllegalAccessException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR,
                                "The method " + m_callbacks[i].getMethodName() + " is not accessible in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.getInstanceManager().getFactory().getLogger().log(
                                Logger.ERROR,
                                "The method " + m_callbacks[i].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
                                        + "throws an exception : " + e.getMessage());
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
        
        m_serviceContext = new PolicyServiceContext(m_handler.getInstanceManager().getGlobalContext(), m_handler.getInstanceManager().getLocalServiceContext(), m_policy);
        
        // Construct the filter with the objectclass + filter
        String classnamefilter = "(objectClass=" + m_specification + ")";
        String filter = "";
        if (!m_strFilter.equals("")) {
            filter = "(&" + classnamefilter + m_strFilter + ")";
        } else {
            filter = classnamefilter;
        }

        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO,
                "[" + m_handler.getInstanceManager().getClassName() + "] Start a dependency on : " + m_specification + " with " + m_strFilter);
        m_state = UNRESOLVED;

        try {
            m_clazz = m_handler.getInstanceManager().getContext().getBundle().loadClass(m_specification);
        } catch (ClassNotFoundException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "Cannot load the interface class for the dependency " + m_field + " [" + m_specification + "]");
        }

        try {
            // Look if the service is already present :
            if (lookForServiceReferences(m_specification, filter)) {
                m_state = RESOLVED;
            }
            // Register a listener :
            m_serviceContext.addServiceListener(this);
         
            m_filter = m_handler.getInstanceManager().getContext().createFilter(filter); // Store the filter
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO, "[" + m_handler.getInstanceManager().getClassName() + "] Create a filter from : " + filter);
            m_change = true;
        } catch (InvalidSyntaxException e1) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "[" + m_handler.getInstanceManager().getClassName() + "] A filter is malformed : " + filter + " - " + e1.getMessage());
        }
    }

    /**
     * Look for available service.
     * @param specification : required specification.
     * @param filter : LDAP Filter
     * @return true if at least one service is found.
     */
    private boolean lookForServiceReferences(String specification, String filter) {
        boolean success = false; // Are the query fulfilled ?
        try {
            ServiceReference[] refs = m_serviceContext.getServiceReferences(specification, filter);
            if (refs != null) {
                success = true;
                for (int i = 0; i < refs.length; i++) {
                    m_references.add(refs[i]);
                }
            }
        } catch (InvalidSyntaxException e) {
            m_handler.getInstanceManager().getFactory().getLogger().log(Logger.ERROR,
                    "The requirement on " + m_specification + " does not have a vlid filter : " + e.getMessage());
        }
        return success;
    }

    /**
     * Stop the dependency.
     */
    public void stop() {
        m_serviceContext.removeServiceListener(this);
        
        m_handler.getInstanceManager().getFactory().getLogger().log(Logger.INFO,
                "[" + m_handler.getInstanceManager().getInstanceName() + "] Stop a dependency on : " + m_specification + " with " + m_strFilter + " (" + m_handler.getInstanceManager() + ")");
        m_state = UNRESOLVED;

        // Unget all services references
        for (int i = 0; i < m_usedReferences.size(); i++) {
            ungetService((ServiceReference) m_usedReferences.get(i));
        }

        m_references.clear();
        m_usedReferences.clear();
        m_clazz = null;
        m_services = new Object[0];
    }

    /**
     * Return the state of the dependency.
     * 
     * @return the state of the dependency (1 : valid, 2 : invalid)
     */
    public int getState() {
        if (m_isOptional) { 
            return RESOLVED;
        } else { 
            return m_state;
        }
    }

    /**
     * Return the list of used service reference.
     * 
     * @return the service reference list.
     */
    public List getServiceReferences() {
        List refs = new ArrayList();
        refs.addAll(m_references);
        return refs;
    }
    
    protected DependencyCallback[] getCallbacks() {
        return m_callbacks;
    }

    /**
     * Set that this dependency is a service level dependency.
     * This forces the scoping policy to be STRICT. 
     * @param b
     */
    public void setServiceLevelDependency() {
        m_isServiceLevelRequirement = true;
        m_policy = PolicyServiceContext.LOCAL;
    }

    public String getId() {
        return m_id;
    }
    
    public boolean isServiceLevelRequirement() {
        return m_isServiceLevelRequirement;
    }

}
