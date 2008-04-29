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
package org.apache.felix.ipojo.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.context.ServiceReferenceImpl;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Abstract dependency model. This class is the parent class of every service dependency. It manages the most part of dependency management. This
 * class creates an interface between the service tracker and the concrete dependency.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class DependencyModel implements TrackerCustomizer {

    /**
     * Dependency state : BROKEN. A broken dependency cannot be fulfilled anymore. The dependency becomes broken when a used service disappears in the
     * static binding policy.
     */
    public static final int BROKEN = -1;

    /**
     * Dependency state : UNRESOLVED. A dependency is unresolved if the dependency is not valid and no service providers are available.
     */
    public static final int UNRESOLVED = 0;

    /**
     * Dependency state : RESOLVED. A dependency is resolved if the dependency is optional or at least one provider is available.
     */
    public static final int RESOLVED = 1;

    /**
     * Binding policy : Dynamic. In this policy, services can appears and departs without special treatment.
     */
    public static final int DYNAMIC_BINDING_POLICY = 0;

    /**
     * Binding policy : Static. Once a service is used, if this service disappears the dependency becomes BROKEN. The instance needs to be recreated.
     */
    public static final int STATIC_BINDING_POLICY = 1;

    /**
     * Binding policy : Dynamic-Priority. In this policy, services can appears and departs. However, once a service with a highest ranking (according
     * to the used comparator) appears, this new service is re-injected.
     */
    public static final int DYNAMIC_PRIORITY_BINDING_POLICY = 2;

    /**
     * Does the dependency bind several providers ?
     */
    private boolean m_aggregate;

    /**
     * Is the dependency optional ?
     */
    private boolean m_optional;

    /**
     * Required specification. Cannot change once set.
     */
    private Class m_specification;

    /**
     * Comparator to sort service references.
     */
    private Comparator m_comparator;

    /**
     * LDAP filter object selecting service references form the set of providers providing the required specification.
     */
    private Filter m_filter;

    /**
     * Bundle context used by the dependency. (could be a service context).
     */
    private BundleContext m_context;

    /**
     * Listener object on which invoking the validate and invalidate methods.
     */
    private final DependencyStateListener m_listener;

    /**
     * Actual state of the dependency.
     */
    private int m_state;

    /**
     * Binding policy of the dependency.
     */
    private int m_policy = DYNAMIC_BINDING_POLICY;

    /**
     * Service tracker used by this dependency.
     */
    private Tracker m_tracker;

    /**
     * List of matching service references. This list is a subset of tracked references. This set is compute according to the filter and the match
     * method.
     */
    private final List m_matchingRefs = new ArrayList();

    /**
     * Constructor.
     * @param specification : required specification
     * @param aggregate : is the dependency aggregate ?
     * @param optional : is the dependency optional ?
     * @param filter : LDAP filter
     * @param comparator : comparator object to sort references
     * @param policy : binding policy
     * @param context : bundle context (or service context)
     * @param listener : dependency lifecycle listener to notify from dependency state changes.
     */
    public DependencyModel(Class specification, boolean aggregate, boolean optional, Filter filter, Comparator comparator, int policy,
            BundleContext context, DependencyStateListener listener) {
        m_specification = specification;
        m_aggregate = aggregate;
        m_optional = optional;
        m_filter = filter;
        m_comparator = comparator;
        m_context = context;
        m_policy = policy;
        // If the dynamic priority policy is chosen, and we have no comparator, fix it to OSGi standard service reference comparator.
        if (m_policy == DYNAMIC_PRIORITY_BINDING_POLICY && m_comparator == null) {
            m_comparator = new ServiceReferenceRankingComparator();
        }
        m_state = UNRESOLVED;
        m_listener = listener;
    }

    /**
     * Open the tracking.
     */
    public void start() {
        m_tracker = new Tracker(m_context, m_specification.getName(), this);
        m_tracker.open();
        computeDependencyState();
    }

    /**
     * Close the tracking.
     */
    public void stop() {
        if (m_tracker != null) {
            m_tracker.close();
            m_tracker = null;
        }
        m_state = UNRESOLVED;
    }

    /**
     * Is the reference set frozen (cannot change anymore) ? This method must be override by concrete dependency to support the static binding policy.
     * This method is just used by default. The method must always return false for non-static dependency.
     * @return true if the reference set is frozen.
     */
    public boolean isFrozen() {
        return false;
    }

    /**
     * Does the service reference match ? This method must be override by concrete dependency if they need to advanced testing on service reference
     * (that cannot be express in the LDAP filter). By default this method return true.
     * @param ref : tested reference.
     * @return true
     */
    public boolean match(ServiceReference ref) {
        return true;
    }

    /**
     * Compute the actual dependency state.
     */
    private void computeDependencyState() {
        if (m_state == BROKEN) { return; } // The dependency is broken ...

        boolean mustCallValidate = false;
        boolean mustCallInvalidate = false;
        synchronized (this) {
            if (m_optional || !m_matchingRefs.isEmpty()) {
                // The dependency is valid
                if (m_state == UNRESOLVED) {
                    m_state = RESOLVED;
                    mustCallValidate = true;
                }
            } else {
                // The dependency is invalid
                if (m_state == RESOLVED) {
                    m_state = UNRESOLVED;
                    mustCallInvalidate = true;
                }
            }
        }

        // Invoke callback in a non-synchronized region
        if (mustCallInvalidate) {
            invalidate();
        } else if (mustCallValidate) {
            validate();
        }

    }

    /**
     * Service tracker adding service callback. We accept the service only if we aren't broken or frozen.
     * @param ref : the new dependency.
     * @return true if the reference must be tracked.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addingService(org.osgi.framework.ServiceReference)
     */
    public boolean addingService(ServiceReference ref) {
        return !((m_state == BROKEN) || isFrozen());
    }

    /**
     * Service Tracker added service callback. If the service matches, manage the arrival.
     * @param ref : new references.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
     */
    public void addedService(ServiceReference ref) {
        if (matchAgainstFilter(ref) && match(ref)) {
            manageArrival(ref);
        }
        // Do not store the service if it doesn't match.
    }
    
    /**
     * Check if the given service reference match the current filter.
     * This method aims to avoid calling {@link Filter#match(ServiceReference)} 
     * method when manipulating a composite reference. In fact, this method throws
     * a {@link ClassCastException} on Equinox.
     * @param ref : the service reference to check.
     * @return true if the service reference matches.
     */
    private boolean matchAgainstFilter(ServiceReference ref) {
        boolean match = true;
        if (m_filter != null) {
            if (ref instanceof ServiceReferenceImpl) {
                // Can't use the match(ref) as it throw a class cast exception on Equinox.
                match = m_filter.match(((ServiceReferenceImpl) ref).getProperties());
            } else { // Non composite reference.
                match = m_filter.match(ref);
            }
        }
        return match;
    }

    /**
     * Manage the arrival of a new service reference. The reference is valid and match the filter and the match method. This method has different
     * behavior according to the binding policy.
     * @param ref : the new reference
     */
    private void manageArrival(ServiceReference ref) {

        // Create a local copy of the state and of the list size.
        int state = m_state;
        int size;

        synchronized (this) {
            m_matchingRefs.add(ref);

            // Sort the collection if needed.
            if (m_comparator != null) {
                Collections.sort(m_matchingRefs, m_comparator);
            }

            size = m_matchingRefs.size();
        }

        if (m_aggregate) {
            onServiceArrival(ref); // Always notify the arrival for aggregate dependencies.
            if (state == UNRESOLVED) { // If we was unresolved, try to validate the dependency.
                computeDependencyState();
            }
        } else { // We are not aggregate.
            if (size == 1) {
                onServiceArrival(ref); // It is the first service, so notify.
                computeDependencyState();
            } else {
                // In the case of a dynamic priority binding, we have to test if we have to update the bound reference
                if (m_policy == DYNAMIC_PRIORITY_BINDING_POLICY && m_matchingRefs.get(0) == ref) {
                    // We are sure that we have at least two references, so if the highest ranked references (first one) is the new received
                    // references,
                    // we have to unbind the used one and to bind the the new one.
                    onServiceDeparture((ServiceReference) m_matchingRefs.get(1));
                    onServiceArrival(ref);
                }
            }
        }
        // Ignore others cases
    }

    /**
     * Service tracker removed service callback. A service goes away. The depart need to be managed only if the reference was used.
     * @param ref : leaving service reference
     * @param arg1 : service object if the service was get
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void removedService(ServiceReference ref, Object arg1) {
        if (m_matchingRefs.contains(ref)) {
            manageDeparture(ref, arg1);
        }
    }

    /**
     * Manage the departure of a used service.
     * @param ref : leaving service reference
     * @param obj : service object if the service was get
     */
    private void manageDeparture(ServiceReference ref, Object obj) {
        // If we already get this service and the binding policy is static, the dependency becomes broken
        if (isFrozen() && obj != null) {
            if (m_state != BROKEN) {
                m_state = BROKEN;
                invalidate();
            }
        } else {
            synchronized (this) {
                m_matchingRefs.remove(ref);
            }
            if (obj == null) {
                computeDependencyState(); // check if the dependency stills valid.
            } else {
                onServiceDeparture(ref);
                ServiceReference newRef = getServiceReference();
                if (newRef == null) { // Check if there is another provider.
                    computeDependencyState(); // no more references.
                } else {
                    if (!m_aggregate) {
                        onServiceArrival(newRef); // Injecting the new service reference for non aggregate dependencies.
                    }
                }
            }
        }

    }

    /**
     * Service tracker modified service callback. This method must handle if the modified service should be considered as a depart or an arrival.
     * According to the dependency filter, a service can now match or can no match anymore.
     * @param ref : modified reference
     * @param arg1 : service object if already get.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void modifiedService(ServiceReference ref, Object arg1) {
        if (m_matchingRefs.contains(ref)) {
            // It's a used service. Check if the service always match.
            if (!matchAgainstFilter(ref) && match(ref)) {
                // The service does not match anymore. Call removedService.
                manageDeparture(ref, arg1);
            } else {
                onServiceModification(ref);
            }
        } else {
            // The service was not used. Check if it matches.
            if (matchAgainstFilter(ref) && match(ref)) {
                manageArrival(ref);
            }
            // Else, the service does not match.
        }
    }

    /**
     * Get the next matching service reference.
     * @return null if no more provider is available, else return the first reference from the matching set.
     */
    public ServiceReference getServiceReference() {
        synchronized (this) {
            if (m_matchingRefs.isEmpty()) {
                return null;
            } else {
                return (ServiceReference) m_matchingRefs.get(0);
            }
        }
    }

    /**
     * Get matching service references.
     * @return the sorted (if a comparator is used) array of matching service references, null if no references are available.
     */
    public ServiceReference[] getServiceReferences() {
        synchronized (this) {
            if (m_matchingRefs.isEmpty()) { return null; }
            return (ServiceReference[]) m_matchingRefs.toArray(new ServiceReference[m_matchingRefs.size()]);
        }
    }

    /**
     * Get the list of currently used service references.
     * @return the list of used reference (according to the service tracker).
     */
    public List getUsedServiceReferences() {
        synchronized (this) {
            // The list must confront actual matching services with already get services from the tracker.

            int size = m_matchingRefs.size();
            List usedByTracker = m_tracker.getUsedServiceReferences();
            if (size == 0 || usedByTracker == null) { return null; }

            List list = new ArrayList(1);
            for (int i = 0; i < size; i++) {
                if (usedByTracker.contains(m_matchingRefs.get(i))) {
                    list.add(m_matchingRefs.get(i)); // Add the service in the list.
                    if (!isAggregate()) { // IF we are not multiple, return the list when the first element is found.
                        return list;
                    }
                }
            }

            return list;
        }
    }

    /**
     * Get the number of actual matching references.
     * @return the number of matching references
     */
    public int getSize() {
        return m_matchingRefs.size();
    }

    /**
     * Concrete dependency callback. This method is called when a new service need to be re-injected in the underlying concrete dependency.
     * @param ref : service reference to inject.
     */
    public abstract void onServiceArrival(ServiceReference ref);

    /**
     * Concrete dependency callback. This method is called when a used service (already injected) is leaving.
     * @param ref : the leaving service reference.
     */
    public abstract void onServiceDeparture(ServiceReference ref);

    /**
     * This method can be override by the concrete dependency to be notified of service modification. This modification is not an arrival or a
     * departure.
     * @param ref : modified service reference.
     */
    public void onServiceModification(ServiceReference ref) {
        if (m_policy == DYNAMIC_PRIORITY_BINDING_POLICY) {
            // Check that the order has changed or not.
            int indexBefore = m_matchingRefs.indexOf(ref);
            Collections.sort(m_matchingRefs, m_comparator);
            if (indexBefore != m_matchingRefs.indexOf(ref) && ! m_aggregate) {
                // The order has changed during the sort.
                onServiceDeparture((ServiceReference) m_matchingRefs.get(1));
                onServiceArrival(ref);
            }
            
        }
    }

    /**
     * Concrete dependency callback. This method is called when the dependency is reconfigured and when this reconfiguration implies changes on the
     * matching service set ( and by the way on the injected service).
     * @param departs : service leaving the matching set.
     * @param arrivals : service arriving in the matching set.
     */
    public abstract void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals);

    /**
     * Call the listener callback to notify the new state of the current dependency.
     */
    private void invalidate() {
        m_listener.invalidate(this);
    }

    /**
     * Call the listener callback to notify the new state of the current dependency.
     */
    private void validate() {
        m_listener.validate(this);
    }

    /**
     * Get the actual state of the dependency.
     * @return : the state of the dependency.
     */
    public int getState() {
        return m_state;
    }

    /**
     * Get the tracked specification.
     * @return the Class object tracked by the dependency.
     */
    public Class getSpecification() {
        return m_specification;
    }

    /**
     * Set the required specification of this service dependency. This operation is not supported if the dependency tracking has already begun.
     * @param specification : required specification.
     */
    public void setSpecification(Class specification) {
        if (m_tracker == null) {
            m_specification = specification;
        } else {
            throw new UnsupportedOperationException("Dynamic specification change is not yet supported");
        }
    }

    /**
     * Set the filter of the dependency. This method recompute the matching set and call the onDependencyReconfiguration callback.
     * @param filter : new LDAP filter.
     */
    public void setFilter(Filter filter) { //NOPMD
        m_filter = filter;
        if (m_tracker != null) { // Tracking started ...
            List toRemove = new ArrayList();
            List toAdd = new ArrayList();
            ServiceReference usedRef = null;
            synchronized (this) {

                // Store the used service references.
                if (!m_aggregate && !m_matchingRefs.isEmpty()) {
                    usedRef = (ServiceReference) m_matchingRefs.get(0);
                }

                // Get actually all tracked references.
                ServiceReference[] refs = m_tracker.getServiceReferences();

                if (refs == null) {
                    for (int j = 0; j < m_matchingRefs.size(); j++) {
                        // All references need to be removed.
                        toRemove.add(m_matchingRefs.get(j));
                    }
                    // No more matching dependency. Clear the matching reference set.
                    m_matchingRefs.clear();
                } else {
                    // Compute matching services.
                    List matching = new ArrayList();
                    for (int i = 0; i < refs.length; i++) {
                        if (matchAgainstFilter(refs[i]) && match(refs[i])) {
                            matching.add(refs[i]);
                        }
                    }
                    // Now compare with used services.
                    for (int j = 0; j < m_matchingRefs.size(); j++) {
                        ServiceReference ref = (ServiceReference) m_matchingRefs.get(j);
                        // Check if the reference is inside the matching list:
                        if (!matching.contains(ref)) {
                            // The reference should be removed
                            toRemove.add(ref);
                        }
                    }

                    // Then remove services which do no more match.
                    m_matchingRefs.removeAll(toRemove);

                    // Then, add new matching services.

                    for (int k = 0; k < matching.size(); k++) {
                        if (!m_matchingRefs.contains(matching.get(k))) {
                            m_matchingRefs.add(matching.get(k));
                            toAdd.add(matching.get(k));
                        }
                    }

                    // Sort the collections if needed.
                    if (m_comparator != null) {
                        Collections.sort(m_matchingRefs, m_comparator);
                        Collections.sort(toAdd, m_comparator);
                        Collections.sort(toRemove, m_comparator);
                    }

                }
            }

            // Call the callback outside the sync bloc.
            if (m_aggregate) {
                ServiceReference[] rem = null;
                ServiceReference[] add = null;
                if (!toAdd.isEmpty()) {
                    add = (ServiceReference[]) toAdd.toArray(new ServiceReference[toAdd.size()]);
                }
                if (!toRemove.isEmpty()) {
                    rem = (ServiceReference[]) toRemove.toArray(new ServiceReference[toRemove.size()]);
                }
                if (rem != null || add != null) { // Notify the change only when a change is made on the matching reference list.
                    onDependencyReconfiguration(rem, add);
                }
            } else {
                // Create a local copy to avoid un-sync reference list access.
                int size;
                ServiceReference newRef = null;
                synchronized (m_matchingRefs) {
                    size = m_matchingRefs.size();
                    if (size > 0) {
                        newRef = (ServiceReference) m_matchingRefs.get(0);
                    }
                }
                // Non aggregate case.
                // If the used reference was not null
                if (usedRef == null) {
                    // The used ref was null,
                    if (size > 0) {
                        onDependencyReconfiguration(null, new ServiceReference[] { newRef });
                    } // Don't notify the change, if the set is not touched by the reconfiguration.
                } else {
                    // If the used ref disappears, inject a new service if available, else reinject null.
                    if (toRemove.contains(usedRef)) {
                        // We have to replace the service.
                        if (size > 0) {
                            onDependencyReconfiguration(new ServiceReference[] { usedRef }, new ServiceReference[] { newRef });
                        } else {
                            onDependencyReconfiguration(new ServiceReference[] { usedRef }, null);
                        }
                    } else if (m_policy == DYNAMIC_PRIORITY_BINDING_POLICY && newRef != usedRef) { //NOPMD
                        // In the case of dynamic-priority, check if the used ref is no more the highest reference
                        onDependencyReconfiguration(new ServiceReference[] { usedRef }, new ServiceReference[] { newRef });
                    }
                }
            }
            // Now, compute the new dependency state.
            computeDependencyState();
        }
    }

    /**
     * Return the dependency filter (String form).
     * @return the String form of the LDAP filter used by this dependency, null if not set.
     */
    public String getFilter() {
        if (m_filter == null) {
            return null;
        } else {
            return m_filter.toString();
        }
    }

    /**
     * Set the aggregate attribute of the current dependency. If the tracking is open, it will call arrival and departure callbacks.
     * @param isAggregate : new aggregate attribute value.
     */
    public synchronized void setAggregate(boolean isAggregate) {
        if (m_tracker == null) { // Not started ...
            m_aggregate = isAggregate;
        } else {
            // We become aggregate.
            if (!m_aggregate && isAggregate) {
                m_aggregate = true;
                // Call the callback on all non already injected service.
                if (m_state == RESOLVED) {

                    for (int i = 1; i < m_matchingRefs.size(); i++) { // The loop begin at 1, as the 0 is already injected.
                        onServiceArrival((ServiceReference) m_matchingRefs.get(i));
                    }
                }
            } else if (m_aggregate && !isAggregate) {
                m_aggregate = false;
                // We become non-aggregate.
                if (m_state == RESOLVED) {
                    for (int i = 1; i < m_matchingRefs.size(); i++) { // The loop begin at 1, as the 0 stills injected.
                        onServiceDeparture((ServiceReference) m_matchingRefs.get(i));
                    }
                }
            }
            // Else, do nothing.
        }
    }

    public boolean isAggregate() {
        return m_aggregate;
    }

    /**
     * Set the optionality attribute of the current dependency.
     * @param isOptional : the new optional attribute value.
     */
    public void setOptionality(boolean isOptional) {
        if (m_tracker == null) { // Not started ...
            m_optional = isOptional;
        } else {
            computeDependencyState();
        }
    }

    public boolean isOptional() {
        return m_optional;
    }

    /**
     * Return the used binding policy.
     * @return the current binding policy.
     */
    public int getBindingPolicy() {
        return m_policy;
    }

    /**
     * Set the binding policy. Not yet supported.
     */
    public void setBindingPolicy() {
        throw new UnsupportedOperationException("Binding Policy change is not yet supported");
        // TODO supporting dynamic policy change.
    }

    public void setComparator(Comparator cmp) {
        m_comparator = cmp;
        // NOTE: the array will be sorted at the next get.
    }

    /**
     * Set the bundle context used by this dependency. This operation is not supported if the tracker is already opened.
     * @param context : bundle context or service context to use
     */
    public void setBundleContext(BundleContext context) {
        if (m_tracker == null) { // Not started ...
            m_context = context;
        } else {
            throw new UnsupportedOperationException("Dynamic bundle (i.e. service) context change is not supported");
        }
    }

    /**
     * Get a service object for the given reference.
     * @param ref : wanted service reference
     * @return : the service object attached to the given reference
     */
    public Object getService(ServiceReference ref) {
        return m_tracker.getService(ref);
    }

    /**
     * Unget a used service reference.
     * @param ref : reference to unget.
     */
    public void ungetService(ServiceReference ref) {
        m_tracker.ungetService(ref);
    }

    /**
     * Helper method parsing the comparator attribute and returning the comparator object. If the 'comparator' attribute is not set, this method
     * returns null. If the 'comparator' attribute is set to 'osgi', this method returns the normal OSGi comparator. In other case, it tries to create
     * an instance of the declared comparator class.
     * @param dep : Element describing the dependency
     * @param context : bundle context (to load the comparator class)
     * @return the comparator object, null if not set.
     * @throws ConfigurationException the comparator class cannot be load or the comparator cannot be instantiated correctly.
     */
    public static Comparator getComparator(Element dep, BundleContext context) throws ConfigurationException {
        Comparator cmp = null;
        String comp = dep.getAttribute("comparator");
        if (comp != null) {
            if (comp.equalsIgnoreCase("osgi")) {
                cmp = new ServiceReferenceRankingComparator();
            } else {
                try {
                    Class cla = context.getBundle().loadClass(comp);
                    cmp = (Comparator) cla.newInstance();
                } catch (ClassNotFoundException e) {
                    throw new ConfigurationException("Cannot load a customized comparator : " + e.getMessage());
                } catch (IllegalAccessException e) {
                    throw new ConfigurationException("Cannot create a customized comparator : " + e.getMessage());
                } catch (InstantiationException e) {
                    throw new ConfigurationException("Cannot create a customized comparator : " + e.getMessage());
                }
            }
        }
        return cmp;
    }

    /**
     * Load the given specification class.
     * @param specification : specification class name to load
     * @param context : bundle context
     * @return : the class object for the given specification
     * @throws ConfigurationException : the class cannot be loaded correctly.
     */
    public static Class loadSpecification(String specification, BundleContext context) throws ConfigurationException {
        Class spec = null;
        try {
            spec = context.getBundle().loadClass(specification);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("A required specification cannot be loaded : " + specification);
        }
        return spec;
    }

    /**
     * Helper method parsing the binding policy. If the 'policy' attribute is not set in the dependency, the method returns the 'DYNAMIC BINDING
     * POLICY'. Accepted policy values are : dynamic, dynamic-priority and static.
     * @param dep : Element describing the dependency
     * @return : the policy attached to this dependency
     * @throws ConfigurationException : an unknown biding policy was described.
     */
    public static int getPolicy(Element dep) throws ConfigurationException {
        String policy = dep.getAttribute("policy");
        if (policy == null || policy.equalsIgnoreCase("dynamic")) {
            return DYNAMIC_BINDING_POLICY;
        } else if (policy.equalsIgnoreCase("dynamic-priority")) {
            return DYNAMIC_PRIORITY_BINDING_POLICY;
        } else if (policy.equalsIgnoreCase("static")) {
            return STATIC_BINDING_POLICY;
        } else {
            throw new ConfigurationException("Binding policy unknown : " + policy);
        }
    }

}
