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
package org.apache.felix.dependencymanager;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A wrapper around a service registration that blocks until the
 * service registration is available.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public final class ServiceRegistrationImpl implements ServiceRegistration {
    public static final ServiceRegistrationImpl ILLEGAL_STATE = new ServiceRegistrationImpl();
    private volatile ServiceRegistration m_registration;

    public ServiceRegistrationImpl() {
        m_registration = null;
    }
    
    public ServiceReference getReference() {
        return ensureRegistration().getReference();
    }

    public void setProperties(Dictionary dictionary) {
        ensureRegistration().setProperties(dictionary);
    }

    public void unregister() {
        ensureRegistration().unregister();
    }

    public boolean equals(Object obj) {
        return ensureRegistration().equals(obj);
    }

    public int hashCode() {
        return ensureRegistration().hashCode();
    }

    public String toString() {
        return ensureRegistration().toString();
    }
    
    private synchronized ServiceRegistration ensureRegistration() {
        while (m_registration == null) {
            try {
                wait();
            }
            catch (InterruptedException ie) {
                // we were interrupted so hopefully we will now have a
                // service registration ready; if not we wait again
            }
        }
        // check if we're in an illegal state and throw an exception
        if (ILLEGAL_STATE == m_registration) {
            throw new IllegalStateException("Service is not registered.");
        }
        return m_registration;
    }

    /**
     * Sets the service registration and notifies all waiting parties.
     */
    void setServiceRegistration(ServiceRegistration registration) {
        synchronized (this) {
        	m_registration = registration;
            notifyAll();
        }
    }

    /**
     * Sets this wrapper to an illegal state, which will cause all threads
     * that are waiting for this service registration to fail.
     */
	void setIllegalState() {
        setServiceRegistration(ServiceRegistrationImpl.ILLEGAL_STATE);
	}
}
