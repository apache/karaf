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

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A service context is the facade of a service registry. 
 * It gives the access to a service broker. All service
 * interactions should use a service context to garanty
 * the service isolation.
 * This class is a subset of {@link BundleContext} methods.
 * (methods implying interactions with the service registry).
 * So, refer to this class for further information.
 * @see BundleContext
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceContext extends BundleContext {

    /**
     * Adds a service listener.
     * The listener is added to this service context.
     * So only services from this context will be tracked.
     * @param listener the service listener to add.
     * @param filter the LDAP filter
     * @throws InvalidSyntaxException if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException;

    /**
     * Adds a service listener.
     * The listener is added to this service context.
     * So only services from this context will be tracked.
     * @param listener the service listener to add.
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    void addServiceListener(ServiceListener listener);

    /**
     * Gets the service references matching with the given query.
     * The query is executed inside this service context.
     * @param clazz the required interface
     * @param filter a LDAP filter
     * @return the array of available service references or <code>null</code>
     * if no providers are available.
     * @throws InvalidSyntaxException if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException;

    /**
     * Gets a service object.
     * The given service reference must comes from this
     * service context.
     * @param reference the required service reference 
     * @return the service object or null if the service reference is no more valid or if the service object is not accessible
     * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
     */
    Object getService(ServiceReference reference);

    /**
     * Gets a service reference for the given interface.
     * The query is executed inside this service context.
     * @param clazz the required interface name
     * @return a service reference on a available provider or 
     * <code>null</code> if no providers are available
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    ServiceReference getServiceReference(String clazz);

    /**
     * Gets service reference list for the given query.
     * The query is executed inside this service context.
     * @param clazz : the name of the required service interface
     * @param filter : LDAP filter to apply on service provider
     * @return : the array of consistent service reference or <code>null</code>
     * if no available providers
     * @throws InvalidSyntaxException if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException;

    /**
     * Registers a service inside this service context.
     * This service is then isolated inside this context.
     * @param clazzes the interfaces provided by the service.
     * @param service the service object.
     * @param properties service properties to publish
     * @return the service registration for this service publication.
     * This service registration is attached to the current service context,
     * and does not have any meaning in other contexts.
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties);

    /**
     * Registers a service inside this service context.
     * This service is then isolated inside this context.
     * @param clazz the interface provided by the service.
     * @param service the service object.
     * @param properties service properties to publish.
     * @return the service registration for this service publication. 
     * This service registration is attached to the current service context,
     * and does not have any meaning in other contexts.
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    ServiceRegistration registerService(String clazz, Object service, Dictionary properties);

    /**
     * Removes a service listener.
     * The listener must be registered inside this service context.
     * @param listener the listener to remove
     * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    void removeServiceListener(ServiceListener listener);

    /**
     * Ungets the service reference.
     * The service reference must comes from this service context.
     * @param reference the reference to unget
     * @return <code>true</code> if you are the last user of the reference.
     * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
     */
    boolean ungetService(ServiceReference reference);

}
