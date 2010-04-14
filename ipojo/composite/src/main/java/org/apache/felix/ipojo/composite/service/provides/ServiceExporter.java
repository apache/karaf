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
package org.apache.felix.ipojo.composite.service.provides;

import java.util.Comparator;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.composite.CompositeManager;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.DependencyStateListener;
import org.apache.felix.ipojo.util.SecurityHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Export an service from the scope to the parent context.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceExporter extends DependencyModel {

    /**
     * Destination context.
     */
    private BundleContext m_destination;

    /**
     * Composite Manager.
     */
    private CompositeManager m_manager;

    /**
     * Map of service reference - service registration storing exported services.
     */
    private Map/*<ServiceReference, ServiceRegistration>*/m_registrations = new HashMap();

    /**
     * Constructor.
     * 
     * @param specification : exported service specification.
     * @param filter : LDAP filter
     * @param multiple : is the export an aggregate export?
     * @param optional : is the export optional?
     * @param cmp : comparator to use in the dependency
     * @param policy : binding policy.
     * @param from : internal service context
     * @param dest : parent bundle context
     * @param listener : dependency lifecycle listener to notify when the dependency state change. 
     * @param manager : composite manager
     */
    public ServiceExporter(Class specification, Filter filter, boolean multiple, boolean optional, Comparator cmp, int policy, ServiceContext from, BundleContext dest, DependencyStateListener listener, CompositeManager manager) {
        super(specification, multiple, optional, filter, cmp, policy, from, listener, manager);

        m_destination = dest;

        m_manager = manager;

    }

    /**
     * Transform service reference property in a dictionary.
     * instance.name and factory.name are injected too.
     * @param ref : the service reference.
     * @return the dictionary containing all property of the given service reference.
     */
    private Dictionary getProps(ServiceReference ref) {
        Properties prop = new Properties();
        String[] keys = ref.getPropertyKeys();
        for (int i = 0; i < keys.length; i++) {
            prop.put(keys[i], ref.getProperty(keys[i]));
        }

        prop.put("instance.name", m_manager.getInstanceName());
        prop.put("factory.name", m_manager.getFactory().getName());

        return prop;
    }

    /**
     * Stop an exporter.
     * Remove the service listener
     * Unregister all exported services.
     */
    public void stop() {
        super.stop();
        Set refs = m_registrations.keySet();
        Iterator iterator = refs.iterator();
        while (iterator.hasNext()) {
            ServiceReference ref = (ServiceReference) iterator.next();
            ServiceRegistration reg = (ServiceRegistration) m_registrations.get(ref);
            try {
                reg.unregister();
            } catch (IllegalStateException e) {
                // The service is already unregistered
                // Do nothing silently
            }
        }
        m_registrations.clear();
    }

    /**
     * A service has been injected. Register it.
     * @param reference : the new reference.
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#addedService(org.osgi.framework.ServiceReference)
     */
    public void onServiceArrival(ServiceReference reference) {
        Object svc = getService(reference);
        // Security Check
        if (SecurityHelper.hasPermissionToRegisterService(getSpecification().getName(), m_destination)) {
            ServiceRegistration reg = m_destination
                .registerService(getSpecification().getName(), svc, getProps(reference));
            m_registrations.put(reference, reg);
        } else {
            throw new SecurityException("The bundle " + m_destination.getBundle().getBundleId() + " does not have the "
                    + "permission to register the service " + getSpecification().getName());
        }
    }

    /**
     * An exported service was modified.
     * @param reference : modified reference
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#modifiedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void onServiceModification(ServiceReference reference) {
        ServiceRegistration reg = (ServiceRegistration) m_registrations.get(reference);
        if (reg != null) {
            reg.setProperties(getProps(reference));
        }
    }

    /**
     * An exported service disappears.
     * @param reference : service reference
     * @see org.apache.felix.ipojo.util.TrackerCustomizer#removedService(org.osgi.framework.ServiceReference, java.lang.Object)
     */
    public void onServiceDeparture(ServiceReference reference) {
        ServiceRegistration reg = (ServiceRegistration) m_registrations.get(reference);
        if (reg != null) {
            reg.unregister();
        }
        m_registrations.remove(reference);
    }

    /**
     * On Dependency Reconfiguration notification method.
     * @param departs : leaving service references.
     * @param arrivals : new injected service references.
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[], org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] departs, ServiceReference[] arrivals) {
        throw new UnsupportedOperationException("Dynamic dependency reconfiguration is not supported by service exporter");
    }
}
