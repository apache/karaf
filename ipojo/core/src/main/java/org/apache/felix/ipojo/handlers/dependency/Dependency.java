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
import org.apache.felix.ipojo.util.Tracker;
import org.apache.felix.ipojo.util.TrackerCustomizer;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Represent a service dependency of the component instance.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class Dependency implements TrackerCustomizer {

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
     * Array of service references.
     * m_ref : Array
     */
    //private List m_references = new ArrayList();
    
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
     * Thread Local.
     */
    private ServiceUsage m_usage = new ServiceUsage();

    /**
     * Service Tracker.
     */
    private Tracker m_tracker;

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
     * Build the Set [service reference] of used services.
     * @return the used service.
     */
    public List getUsedServices() {
        List set = new ArrayList();
        for (int i = 0; i < m_usedReferences.size(); i++) {
            if (! set.contains(m_usedReferences.get(i))) {
                set.add(m_usedReferences.get(i));
            }
        }
        return set;
    }

//    /**
//     * A dependency is satisfied if it is optional or there is useful references.
//     * 
//     * @return true is the dependency is satisfied
//     */
//    protected boolean isSatisfied() {
//        return m_isOptional || m_tracker.size() != 0;
//    }

    /**
     * This method is called by the replaced code in the component
     * implementation class. Construct the service object list is necessary.
     * 
     * @return null or a service object or a list of service object according to
     * the dependency.
     */
    protected Object get() {
        // Initialize the thread local object is not already touched.
        if (m_usage.getObjects().isEmpty()) {
            if (isAggregate()) {
                synchronized (m_tracker) {
                    ServiceReference[] refs = m_tracker.getServiceReferences();
                    for (int i = 0; refs != null && i < refs.length; i++) {
                        m_usage.getReferences().add(refs[i]);
                        m_usage.getObjects().add(getService(refs[i]));
                    }
                }
            } else {
                if (m_tracker.size() == 0) {
                    // Load the nullable class
                    String className = m_specification + "Nullable";
                    Class nullableClazz = m_handler.getNullableClass(className);

                    if (nullableClazz == null) {
                        m_handler.log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Cannot load the nullable class to return a dependency object for " + m_field + " -> " + m_specification);
                        return null;
                    }

                    // The nullable class is loaded, create the object and add it
                    Object instance = null;
                    try {
                        instance = nullableClazz.newInstance();
                    } catch (IllegalAccessException e) {
                        // There is a problem in the dependency resolving (like in stopping method)
                        if (m_isAggregate) {
                            m_handler.log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Return an empty array, an exception was throwed in the get method", e);
                            return Array.newInstance(m_clazz, 0);
                        } else {
                            m_handler.log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Return null, an exception was throwed in the get method", e);
                            return null;
                        }
                    } catch (InstantiationException e) {
                        // There is a problem in the dependency resolving (like in stopping
                        // method)
                        if (m_isAggregate) {
                            m_handler.log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Return an empty array, an exception was throwed in the get method", e);
                            return Array.newInstance(m_clazz, 0);
                        } else {
                            m_handler.log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] Return null, an exception was throwed in the get method", e);
                            return null;
                        }
                    }
                    m_usage.getObjects().add(instance);
                } else {
                    m_usage.getReferences().add(m_tracker.getServiceReference());
                    m_usage.getObjects().add(getService(m_tracker.getServiceReference()));
                }
            }
            m_usage.setStackLevel(1);
        }

        if (m_isAggregate) { // Multiple dependency
            return (Object[]) m_usage.getObjects().toArray((Object[]) Array.newInstance(m_clazz, 0));
        } else {
            return m_usage.getObjects().get(0);
        }
    }

    /**
     * Call unbind callback method.
     * @param ref : reference to send (if accepted) to the method
     */
    private void callUnbindMethod(ServiceReference ref) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects().length > 0) {
            for (int i = 0; i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.UNBIND) {
                    try {
                        m_callbacks[i].call(ref, getService(ref));
                    } catch (NoSuchMethodException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " does not exist in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (IllegalAccessException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " is not accessible in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
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
        return m_tracker.getService(ref);
    }
    
    /**
     * Unget the given service reference according to the resolving policy.
     * @param ref : service reference to unget
     */
    private void ungetService(ServiceReference ref) {
        m_serviceContext.ungetService(ref);
    }

    /**
     * Call the bind method.
     * @param instance : instance on which calling the bind method.
     */
    protected void callBindMethod(Object instance) {
        if (m_tracker == null) { return; }
        // Check optional case : nullable object case : do not call bind on nullable object
        if (m_isOptional && m_tracker.size() == 0) {
            return;
        }

        if (m_isAggregate) {
            synchronized (m_tracker) {
                ServiceReference[] refs = m_tracker.getServiceReferences();
                for (int i = 0; i < refs.length; i++) {
                    for (int j = 0; j < m_callbacks.length; j++) {
                        if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                            try {
                                m_callbacks[j].callOnInstance(instance, refs[i], getService(refs[i]));
                            } catch (NoSuchMethodException e) {
                                m_handler.log(Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " does not exist in the class "
                                                + m_handler.getInstanceManager().getClassName());
                                return;
                            } catch (IllegalAccessException e) {
                                m_handler.log(Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " is not accessible in the class "
                                                + m_handler.getInstanceManager().getClassName());
                                return;
                            } catch (InvocationTargetException e) {
                                m_handler.log(Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
                                                + "thorws an exception : " + e.getMessage());
                                return;
                            }
                        }
                    }
                }
            }
        } else {
            for (int j = 0; j < m_callbacks.length; j++) {
                if (m_callbacks[j].getMethodType() == DependencyCallback.BIND) {
                    try {
                        ServiceReference ref = m_tracker.getServiceReference();
                        m_callbacks[j].callOnInstance(instance, ref, getService(ref));
                    } catch (NoSuchMethodException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " does not exist in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (IllegalAccessException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " is not accessible in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[j].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
                                        + "throws an exception : " + e.getMessage());
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
       // if (m_handler.getInstanceManager().getState() == InstanceManager.VALID) {
        if (m_handler.getInstanceManager().getState() > InstanceManager.STOPPED && m_handler.getInstanceManager().getPojoObjects().length > 0) {
            for (int i = 0; i < m_callbacks.length; i++) {
                if (m_callbacks[i].getMethodType() == DependencyCallback.BIND) {
                    try {
                        m_callbacks[i].call(ref, getService(ref));
                    } catch (NoSuchMethodException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " does not exist in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (IllegalAccessException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " is not accessible in the class "
                                        + m_handler.getInstanceManager().getClassName());
                        return;
                    } catch (InvocationTargetException e) {
                        m_handler.log(Logger.ERROR, "The method " + m_callbacks[i].getMethodName() + " in the class " + m_handler.getInstanceManager().getClassName()
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
        if ("".equals(m_strFilter)) {
            filter = classnamefilter;
        } else {
            filter = "(&" + classnamefilter + m_strFilter + ")";
        }

        m_state = UNRESOLVED;

        try {
            m_clazz = m_handler.getInstanceManager().getContext().getBundle().loadClass(m_specification);
        } catch (ClassNotFoundException e) {
            m_handler.log(Logger.ERROR, "Cannot load the interface class for the dependency " + m_field + " [" + m_specification + "]");
            return;
        }

        try {
            m_filter = m_handler.getInstanceManager().getContext().createFilter(filter); // Store the filter
        } catch (InvalidSyntaxException e1) {
            m_handler.log(Logger.ERROR, "[" + m_handler.getInstanceManager().getClassName() + "] A filter is malformed : " + filter + " - " + e1.getMessage());
            return;
        }
        
        m_tracker = new Tracker(m_serviceContext, m_filter, this);
        m_tracker.open();
        if (m_tracker.size() == 0 && !m_isOptional) {
            m_state = UNRESOLVED;
        } else {
            m_state = RESOLVED;
        }
    }

    /**
     * Stop the dependency.
     */
    public void stop() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
        
        m_handler.log(Logger.INFO, "[" + m_handler.getInstanceManager().getInstanceName() + "] Stop a dependency on : " + m_specification + " with " + m_strFilter + " (" + m_handler.getInstanceManager() + ")");
        m_state = UNRESOLVED;

        // Unget all services references
        for (int i = 0; i < m_usedReferences.size(); i++) {
            ungetService((ServiceReference) m_usedReferences.get(i));
        }

        m_usedReferences.clear();
        m_clazz = null;
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
        if (m_tracker.size() != 0) {
            return m_tracker.getServiceReferencesList();
        } else {
            return new ArrayList();
        }
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

    /**
     * Method called when a thread enters in a method.
     * @param method : method id.
     */
    public void entry(String method) {
        if (! m_usage.getObjects().isEmpty()) {
            int level = m_usage.getStackLevel();
            m_usage.setStackLevel(level++);
        }
    }
    
    /**
     * Method called when a thread exits a method.
     * @param method : the method id.
     */
    public void exit(String method) {
        if (! m_usage.getObjects().isEmpty()) {
            int level = m_usage.getStackLevel();
            level = level - 1;
            if (level == 0) {
                // Exit the method flow => Release all object
                m_usage.getObjects().clear();
                List refs = m_usage.getReferences();
                refs.clear();
            }
        }
    }

   /**
    * A new service is detected. This method check the reference against the stored filter.
    * @param ref : new service reference.
    * @return the service object, null is the service object is rejected.
    * @see org.osgi.util.tracker.ServiceTrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
    */
    public boolean addingService(ServiceReference ref) {
        if (m_filter.match(ref)) {
            m_state = RESOLVED;
            if (m_isAggregate || m_tracker.size() == 0) {
                callBindMethod(ref);
            }
            
            m_handler.checkContext();
            return true;
        }
        return false;
    }

    /**
     * A used service is modified.
     * @param ref : modified reference
     * @param arg1 : service object (returned when added) 
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference ref, Object arg1) { }

    /**
     * A used service disappears.
     * @param ref : implicated service.
     * @param arg1 : service object.
     * @see org.osgi.util.tracker.ServiceTrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference ref, Object arg1) {
        // Call unbind method
        boolean hasChanged = false;
        if (m_usedReferences.contains(ref)) {
            callUnbindMethod(ref);
            // Unget the service reference
            ungetService(ref);
            m_usedReferences.remove(ref);
            hasChanged = true;
        }

        // Is the state valid or invalid, the reference is already removed
        if (m_tracker.size() == 0 && !m_isOptional) {
            m_state = UNRESOLVED;
        } else {
            m_state = RESOLVED;
        }

        // Is there any change ?
        if (!m_isAggregate) {
            if (hasChanged) { // The used reference has been removed
                if (m_tracker.size() != 0) {
                    callBindMethod(m_tracker.getServiceReference());
                }
            }
        }

        m_handler.checkContext();
        return;
    }

}
