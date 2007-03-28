/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.dependencymanager;

import java.util.Dictionary;

import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * A wrapper around a service registration that blocks until the
 * service registration is available.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ServiceRegistrationImpl implements ServiceRegistration {
    public static final ServiceRegistrationImpl ILLEGAL_STATE = new ServiceRegistrationImpl();
    private ServiceRegistration m_registration;

    public ServiceRegistrationImpl() {
        m_registration = null;
    }
    
    public ServiceReference getReference() {
        ensureRegistration();
        return m_registration.getReference();
    }

    public void setProperties(Dictionary dictionary) {
        ensureRegistration();
        m_registration.setProperties(dictionary);
    }

    public void unregister() {
        ensureRegistration();
        m_registration.unregister();
    }

    public boolean equals(Object obj) {
        ensureRegistration();
        return m_registration.equals(obj);
    }

    public int hashCode() {
        ensureRegistration();
        return m_registration.hashCode();
    }

    public String toString() {
        ensureRegistration();
        return m_registration.toString();
    }
    
    private synchronized void ensureRegistration() {
        while (m_registration == null) {
            try {
                wait();
            }
            catch (InterruptedException ie) {
                // we were interrupted so hopefully we will now have a
                // service registration ready; if not we wait again
            }
        }
        if (ILLEGAL_STATE == m_registration) {
            throw new IllegalStateException("Service is not registered.");
        }
    }

    void setServiceRegistration(ServiceRegistration registration) {
        m_registration = registration;
        synchronized (this) {
            notifyAll();
        }
    }
}
