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
 * A service context give the access the a service broker. All service
 * interaction should use this service context.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceContext extends BundleContext {

    /**
     * Add a service listener.
     * @param listener : the service listener to add.
     * @param filter : the LDAP filter
     * @throws InvalidSyntaxException : occurs when the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener, java.lang.String)
     */
    void addServiceListener(ServiceListener listener, String filter) throws InvalidSyntaxException;

    /**
     * Add a service listener.
     * @param listener : the service listener to add.
     * @see org.osgi.framework.BundleContext#addServiceListener(org.osgi.framework.ServiceListener)
     */
    void addServiceListener(ServiceListener listener);

    /**
     * Get the service references matching with the given query.
     * @param clazz : Required interface
     * @param filter : LDAP filter
     * @return the array of available service reference
     * @throws InvalidSyntaxException : occurs if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#getAllServiceReferences(java.lang.String, java.lang.String)
     */
    ServiceReference[] getAllServiceReferences(String clazz, String filter) throws InvalidSyntaxException;

    /**
     * Get a service object.
     * @param reference : the required service reference 
     * @return the service object or null if the service reference is no more valid or if the service object is not accessible
     * @see org.osgi.framework.BundleContext#getService(org.osgi.framework.ServiceReference)
     */
    Object getService(ServiceReference reference);

    /**
     * Get a service reference for the given interface.
     * @param clazz : the required interface name
     * @return a service reference on a available provider or null if no provider available
     * @see org.osgi.framework.BundleContext#getServiceReference(java.lang.String)
     */
    ServiceReference getServiceReference(String clazz);

    /**
     * Get service reference list for the given query.
     * @param clazz : the name of the required service interface
     * @param filter : LDAP filter to apply on service provider
     * @return : the array of consistent service reference or null if no available provider
     * @throws InvalidSyntaxException : occurs if the LDAP filter is malformed
     * @see org.osgi.framework.BundleContext#getServiceReferences(java.lang.String, java.lang.String)
     */
    ServiceReference[] getServiceReferences(String clazz, String filter) throws InvalidSyntaxException;

    /**
     * Register a service.
     * @param clazzes : interfaces provided by the service.
     * @param service : the service object.
     * @param properties : service properties.
     * @return the service registration for this service publication.
     * @see org.apache.felix.ipojo.ServiceContext#registerService(java.lang.String[], java.lang.Object, java.util.Dictionary)
     */
    ServiceRegistration registerService(String[] clazzes, Object service, Dictionary properties);

    /**
     * Register a service.
     * @param clazz : interface provided by the service.
     * @param service : the service object.
     * @param properties : service properties.
     * @return the service registration for this service publication.
     * @see org.osgi.framework.BundleContext#registerService(java.lang.String, java.lang.Object, java.util.Dictionary)
     */
    ServiceRegistration registerService(String clazz, Object service, Dictionary properties);

    /**
     * Remove a service listener.
     * @param listener : the listener to remove
     * @see org.osgi.framework.BundleContext#removeServiceListener(org.osgi.framework.ServiceListener)
     */
    void removeServiceListener(ServiceListener listener);

    /**
     * Unget the service reference.
     * @param reference : the reference to unget
     * @return true if you are the last user of the reference
     * @see org.osgi.framework.BundleContext#ungetService(org.osgi.framework.ServiceReference)
     */
    boolean ungetService(ServiceReference reference);


}
