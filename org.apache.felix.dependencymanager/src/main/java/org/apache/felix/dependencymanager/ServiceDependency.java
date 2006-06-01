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

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Service dependency that can track an OSGi service.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ServiceDependency implements Dependency, ServiceTrackerCustomizer {
    private boolean m_isRequired;
    private Service m_service;
    private ServiceTracker m_tracker;
    private BundleContext m_context;
    private boolean m_isAvailable;
    private Class m_trackedServiceName;
    private Object m_nullObject;
    private String m_trackedServiceFilter;
    private ServiceReference m_trackedServiceReference;
    private boolean m_isStarted;
    private Object m_callbackInstance;
    private String m_callbackAdded;
    private String m_callbackChanged;
    private String m_callbackRemoved;
    private boolean m_autoConfig;
    private ServiceReference m_reference;
    private Object m_serviceInstance;
    
    /**
     * Creates a new service dependency.
     * 
     * @param context the bundle context
     */
    public ServiceDependency(BundleContext context) {
        m_context = context;
        m_autoConfig = true;
    }

    public boolean isRequired() {
        return m_isRequired;
    }

    public boolean isAvailable() {
        return m_isAvailable;
    }
    
    public boolean isAutoConfig() {
        return m_autoConfig;
    }

    public synchronized Object getService() {
        Object service = null;
        if (m_isStarted) {
            service = m_tracker.getService();
        }
        if (service == null) {
            service = getNullObject(); 
        }
        return service;
    }

    private Object getNullObject() {
        if (m_nullObject == null) {
            m_nullObject = Proxy.newProxyInstance(m_trackedServiceName.getClassLoader(), new Class[] {m_trackedServiceName}, new DefaultNullObject()); 
        }
        return m_nullObject;
    }
    
    public Class getInterface() {
        return m_trackedServiceName;
    }

    public synchronized void start(Service service) {
        if (m_isStarted) {
            throw new IllegalStateException("Service dependency was already started." + m_trackedServiceName);
        }
        m_service = service;
        if (m_trackedServiceName != null) {
            if (m_trackedServiceFilter != null) {
                try {
                    m_tracker = new ServiceTracker(m_context, m_context.createFilter(m_trackedServiceFilter), this);
                }
                catch (InvalidSyntaxException e) {
                    throw new IllegalStateException("Invalid filter definition for dependency.");
                }
            }
            else if (m_trackedServiceReference != null) {
                m_tracker = new ServiceTracker(m_context, m_trackedServiceReference, this);
            }
            else {
                m_tracker = new ServiceTracker(m_context, m_trackedServiceName.getName(), this);
            }
        }
        else {
            throw new IllegalStateException("Could not create tracker for dependency, no service name specified.");
        }
        m_isStarted = true;
        m_tracker.open();
    }

    public synchronized void stop(Service service) {
        if (!m_isStarted) {
            throw new IllegalStateException("Service dependency was not started.");
        }
        m_tracker.close();
        m_isStarted = false;
        m_tracker = null;
    }

    public Object addingService(ServiceReference ref) {
        Object service = m_context.getService(ref);
        // we remember these for future reference, needed for required service callbacks
        m_reference = ref;
        m_serviceInstance = service;
        return service;
    }

    public void addedService(ServiceReference ref, Object service) {
        if (makeAvailable()) {
            m_service.dependencyAvailable(this);
        }
        else {
            m_service.dependencyChanged(this);
        }
        // try to invoke callback, if specified, but only for optional dependencies
        // because callbacks for required dependencies are handled differently
        if (!isRequired()) {
            invokeAdded(ref, service);
        }
    }

    public void invokeAdded() {
        invokeAdded(m_reference, m_serviceInstance);
    }
    
    public void invokeAdded(ServiceReference reference, Object serviceInstance) {
        Object callbackInstance = getCallbackInstance();
        if ((callbackInstance != null) && (m_callbackAdded != null)) {
            try {
                invokeCallbackMethod(callbackInstance, m_callbackAdded, reference, serviceInstance);
            } catch (NoSuchMethodException e) {
                // silently ignore this
            }
        }
    }

    public void modifiedService(ServiceReference ref, Object service) {
        m_reference = ref;
        m_serviceInstance = service;
        m_service.dependencyChanged(this);
        // only invoke the changed callback if the service itself is "active"
        if (((ServiceImpl) m_service).isRegistered()) {
            invokeChanged(ref, service);
        }
    }

    public void invokeChanged(ServiceReference reference, Object serviceInstance) {
        Object callbackInstance = getCallbackInstance();
        if ((callbackInstance != null) && (m_callbackChanged != null)) {
            try {
                if (m_reference == null) {
                    Thread.dumpStack();
                }
                invokeCallbackMethod(callbackInstance, m_callbackChanged, reference, serviceInstance);
            }
            catch (NoSuchMethodException e) {
                // ignore when the service has no such method
            }
        }
    }

    public void removedService(ServiceReference ref, Object service) {
        if (makeUnavailable()) {
            m_service.dependencyUnavailable(this);
        }
        // try to invoke callback, if specified, but only for optional dependencies
        // because callbacks for required dependencies are handled differently
        if (!isRequired()) {
            invokeRemoved(ref, service);
        }
        m_context.ungetService(ref);
    }

    public void invokeRemoved() {
        invokeRemoved(m_reference, m_serviceInstance);
    }
    
    public void invokeRemoved(ServiceReference reference, Object serviceInstance) {
        Object callbackInstance = getCallbackInstance();
        if ((callbackInstance != null) && (m_callbackRemoved != null)) {
            try {
                if (m_reference == null) {
                    Thread.dumpStack();
                }
                invokeCallbackMethod(callbackInstance, m_callbackRemoved, reference, serviceInstance);
            }
            catch (NoSuchMethodException e) {
                // ignore when the service has no such method
            }
        }
    }
    
    private synchronized boolean makeAvailable() {
        if (!m_isAvailable) {
            m_isAvailable = true;
            return true;
        }
        return false;
    }
    
    private synchronized boolean makeUnavailable() {
        if ((m_isAvailable) && (m_tracker.getServiceReference() == null)) {
            m_isAvailable = false;
            return true;
        }
        return false;
    }
    
    private Object getCallbackInstance() {
        Object callbackInstance = m_callbackInstance;
        if (callbackInstance == null) {
            callbackInstance = m_service.getService();
        }
        return callbackInstance;
    }
    
    // TODO a lot of things in this method can be cached instead of done each time
    // TODO Richard had an example where he could not invoke a private method
    private void invokeCallbackMethod(Object instance, String methodName, ServiceReference reference, Object service) throws NoSuchMethodException {
        Method method = null;
        Class clazz = instance.getClass();
        AccessibleObject.setAccessible(clazz.getDeclaredMethods(), true);
        try {
            try {
                method = clazz.getDeclaredMethod(methodName, new Class[] {ServiceReference.class, Object.class});
                method.invoke(instance, new Object[] {reference, service});
            }
            catch (NoSuchMethodException e) {
                try {
                    method = clazz.getDeclaredMethod(methodName, new Class[] {ServiceReference.class});
                    method.invoke(instance, new Object[] {reference});
                } 
                catch (NoSuchMethodException e1) {
                    try {
                        method = clazz.getDeclaredMethod(methodName, new Class[] {Object.class});
                        method.invoke(instance, new Object[] {service});
                    } 
                    catch (NoSuchMethodException e2) {
                        try {
                            method = clazz.getDeclaredMethod(methodName, new Class[] {m_trackedServiceName});
                            method.invoke(instance, new Object[] {service});
                        } 
                        catch (NoSuchMethodException e3) {
                            method = clazz.getDeclaredMethod(methodName, null);
                            method.invoke(instance, null);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException e1) {
            // TODO handle this exception, probably best to ignore it
            e1.printStackTrace();
        } catch (IllegalAccessException e1) {
            // TODO handle this exception, probably best to ignore it
            e1.printStackTrace();
        } catch (InvocationTargetException e1) {
            // TODO handle this exception, probably best to ignore it
            e1.printStackTrace();
        }
    }
    
    // ----- CREATION

    /**
     * Sets the name of the service that should be tracked. 
     * 
     * @param serviceName the name of the service
     * @return this service dependency
     */
    public synchronized ServiceDependency setService(Class serviceName) {
        ensureNotActive();
        if (serviceName == null) {
            throw new IllegalArgumentException("Service name cannot be null.");
        }
        m_trackedServiceName = serviceName;
        m_trackedServiceReference = null;
        m_trackedServiceFilter = null;
        return this;
    }
    
    /**
     * Sets the name of the service that should be tracked. You can either specify
     * only the name, or the name and a filter. In the latter case, the filter is used
     * to track the service and should only return services of the type that was specified
     * in the name.
     * 
     * @param serviceName the name of the service
     * @param serviceFilter the filter condition
     * @return this service dependency
     */
    public synchronized ServiceDependency setService(Class serviceName, String serviceFilter) {
        ensureNotActive();
        if (serviceName == null) {
            throw new IllegalArgumentException("Service name cannot be null.");
        }
        m_trackedServiceName = serviceName;
        m_trackedServiceFilter = serviceFilter;
        m_trackedServiceReference = null;
        return this;
    }

    /**
     * Sets the name of the service that should be tracked. You can either specify
     * only the name, or the name and a reference. In the latter case, the service reference
     * is used to track the service and should only return services of the type that was 
     * specified in the name.
     * 
     * @param serviceName the name of the service
     * @param serviceReference the service reference to track
     * @return this service dependency
     */
    public synchronized ServiceDependency setService(Class serviceName, ServiceReference serviceReference) {
        ensureNotActive();
        if (serviceName == null) {
            throw new IllegalArgumentException("Service name cannot be null.");
        }
        m_trackedServiceName = serviceName;
        m_trackedServiceReference = serviceReference;
        m_trackedServiceFilter = null;
        return this;
    }

    /**
     * Sets the required flag which determines if this service is required or not.
     * 
     * @param required the required flag
     * @return this service dependency
     */
    public synchronized ServiceDependency setRequired(boolean required) {
        ensureNotActive();
        m_isRequired = required;
        return this;
    }

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in any attributes in the service implementation that
     * are of the same type as this dependency. Default is on.
     * 
     * @param autoConfig the value of auto config
     * @return this service dependency
     */
    public synchronized ServiceDependency setAutoConfig(boolean autoConfig) {
        ensureNotActive();
        m_autoConfig = autoConfig;
        return this;
    }
    
    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever
     * a dependency is added or removed. They are called on the service implementation.
     * 
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public synchronized ServiceDependency setCallbacks(String added, String removed) {
        return setCallbacks(null, added, null, removed);
    }
    public synchronized ServiceDependency setCallbacks(String added, String changed, String removed) {
        return setCallbacks(null, added, changed, removed);
    }
    public synchronized ServiceDependency setCallbacks(Object instance, String added, String removed) {
        return setCallbacks(instance, added, null, removed);
    }
    
    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever
     * a dependency is added or removed. They are called on the instance you provide.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public synchronized ServiceDependency setCallbacks(Object instance, String added, String changed, String removed) {
        ensureNotActive();
        m_callbackInstance = instance;
        m_callbackAdded = added;
        m_callbackChanged = changed;
        m_callbackRemoved = removed;
        return this;
    }
    
    
    private void ensureNotActive() {
        if (m_tracker != null) {
            throw new IllegalStateException("Cannot modify state while active.");
        }
    }
    
    public String toString() {
        return "ServiceDependency[" + m_trackedServiceName + " " + m_trackedServiceFilter + " " + m_isRequired + "] for " + m_service;
    }
}
