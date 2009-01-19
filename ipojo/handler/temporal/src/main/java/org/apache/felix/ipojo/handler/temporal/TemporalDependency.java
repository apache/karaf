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
package org.apache.felix.ipojo.handler.temporal;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.felix.ipojo.FieldInterceptor;
import org.apache.felix.ipojo.MethodInterceptor;
import org.apache.felix.ipojo.Nullable;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.handler.temporal.ServiceUsage.Usage;
import org.apache.felix.ipojo.handlers.dependency.NullableObject;
import org.apache.felix.ipojo.util.DependencyModel;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;

/**
 * Temporal dependency. A temporal dependency waits (block) for the availability
 * of the service. If no provider arrives in the specified among of time, a
 * runtime exception is thrown.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class TemporalDependency extends DependencyModel implements
        FieldInterceptor, MethodInterceptor {

    /**
     * The timeout.
     */
    private long m_timeout;

    /**
     * The default implementation.
     */
    private String m_di;

    /**
     * The {@link Nullable} object or Default-Implementation instance if used.
     */
    private Object m_nullableObject;

    /**
     * The handler managing this dependency.
     */
    private PrimitiveHandler m_handler;

    /**
     * The timeout policy. Null injects null, {@link Nullable} injects a nullable object or
     * an array with a nullable object, Default-Implementation injects an object
     * created from the specified injected implementation or an array with it
     * Empty array inject an empty array (must be an aggregate dependency) No
     * policy (0) throw a runtime exception when the timeout occurs *
     */
    private int m_policy;
    
    /**
     * The dependency is injected as a collection.
     * The field must be of the {@link Collection} type
     */
    private boolean m_collection;
    
    /**
     * Enables the proxy mode.
     */
    private boolean m_proxy;
    
    /**
     * Service Usage (Thread Local).
     */
    private ServiceUsage m_usage;

    /**
     * The proxy object.
     * This field is used for scalar proxied temporal dependency. 
     */
    private Object m_proxyObject;


    /**
     * Creates a temporal dependency.
     * @param spec the service specification
     * @param agg is the dependency aggregate ?
     * @param collection the dependency field is a collection
     * @param proxy enable the proxy-mode
     * @param filter the LDAP filter
     * @param context service context
     * @param timeout timeout
     * @param handler Handler managing this dependency
     * @param defaultImpl class used as default-implementation
     * @param policy onTimeout policy
     */
    public TemporalDependency(Class spec, boolean agg, boolean collection, boolean proxy, Filter filter,
            BundleContext context, long timeout, int policy,
            String defaultImpl, TemporalHandler handler) {
        super(spec, agg, true, filter, null,
                DependencyModel.DYNAMIC_BINDING_POLICY, context, handler, handler.getInstanceManager());
        m_di = defaultImpl;
        m_policy = policy;
        m_timeout = timeout;
        m_handler = handler;
        m_collection = collection;
        m_proxy = proxy;
        if (! proxy) { // No proxy => initialize the Thread local.
            m_usage = new ServiceUsage();
        } else if (proxy && ! agg) { // Scalar proxy => Create the proxy.
            ProxyFactory proxyFactory = new ProxyFactory(this.getSpecification().getClassLoader(), this.getClass().getClassLoader());
            m_proxyObject = proxyFactory.getProxy(getSpecification(), this);
        }
    }

    /**
     * The dependency has been reconfigured.
     * @param arg0 new service references
     * @param arg1 old service references
     * @see org.apache.felix.ipojo.util.DependencyModel#onDependencyReconfiguration(org.osgi.framework.ServiceReference[],
     *      org.osgi.framework.ServiceReference[])
     */
    public void onDependencyReconfiguration(ServiceReference[] arg0,
            ServiceReference[] arg1) {
        throw new UnsupportedOperationException(
                "Reconfiguration not yet supported");
    }

    /**
     * A provider arrives.
     * @param ref service reference of the new provider.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceArrival(org.osgi.framework.ServiceReference)
     */
    public synchronized void onServiceArrival(ServiceReference ref) {
        // Notify if a thread is waiting.
        notifyAll();
    }

    /**
     * A provider leaves.
     * @param arg0 leaving service references.
     * @see org.apache.felix.ipojo.util.DependencyModel#onServiceDeparture(org.osgi.framework.ServiceReference)
     */
    public void onServiceDeparture(ServiceReference arg0) {  }

    /**
     * The code require a value of the monitored field. If providers are
     * available, the method return service object(s) immediately. Else, the
     * thread is blocked until an arrival. If no provider arrives during the
     * among of time specified, the method throws a Runtime Exception.
     * @param arg0 POJO instance asking for the service
     * @param arg1 field name
     * @param arg2 previous value
     * @return the object to inject.
     * @see org.apache.felix.ipojo.FieldInterceptor#onGet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public synchronized Object onGet(Object arg0, String arg1, Object arg2) {
        // Check if the Thread local as a value
        if (! m_proxy) {
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack > 0) {
                return usage.m_object;
            }
        }
        
        ServiceReference[] refs = getServiceReferences();
        if (refs != null) {
            // Immediate return.
            return getServiceObjects(refs);
        } else {
            // Begin to wait ...            
            long enter = System.currentTimeMillis();
            boolean exhausted = false;
            synchronized (this) {
                while (getServiceReference() == null && !exhausted) {
                    try {
                        wait(1);
                    } catch (InterruptedException e) {
                        // We was interrupted ....
                    } finally {
                        long end = System.currentTimeMillis();
                        exhausted = (end - enter) > m_timeout;
                    }
                }
            }
            // Check
            if (exhausted) {
                return onTimeout();
            } else {
                refs = getServiceReferences();
                return getServiceObjects(refs);
            }
        }
    }
    
    /**
     * A POJO method will be invoked.
     * @param pojo : Pojo object
     * @param method : called method
     * @param args : arguments
     * @see org.apache.felix.ipojo.MethodInterceptor#onEntry(java.lang.Object, java.lang.reflect.Method, java.lang.Object[])
     */
    public void onEntry(Object pojo, Method method, Object[] args) {
        if (m_usage != null) {
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack > 0) {
                usage.inc();
                m_usage.set(usage); // Set the Thread local as value has been modified
            }
        }
    }

    /**
     * A POJO method has thrown an error.
     * This method does nothing and wait for the finally.
     * @param pojo : POJO object.
     * @param method : Method object.
     * @param throwable : thrown error
     * @see org.apache.felix.ipojo.MethodInterceptor#onError(java.lang.Object, java.lang.reflect.Method, java.lang.Throwable)
     */
    public void onError(Object pojo, Method method, Throwable throwable) {
        // Nothing to do  : wait onFinally
    }

    /**
     * A POJO method has returned.
     * @param pojo : POJO object.
     * @param method : Method object.
     * @param returnedObj : returned object (null for void method)
     * @see org.apache.felix.ipojo.MethodInterceptor#onExit(java.lang.Object, java.lang.reflect.Method, java.lang.Object)
     */
    public void onExit(Object pojo, Method method, Object returnedObj) {
        // Nothing to do  : wait onFinally        
    }
    
    /**
     * A POJO method is finished.
     * @param pojo : POJO object.
     * @param method : Method object.
     * @see org.apache.felix.ipojo.MethodInterceptor#onFinally(java.lang.Object, java.lang.reflect.Method)
     */
    public void onFinally(Object pojo, Method method) {
        if (m_usage != null) {
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack > 0) {
                if (usage.dec()) {
                    // Exit the method flow => Release all objects
                    usage.clear();
                    m_usage.set(usage); // Set the Thread local as value has been modified
                }
            }
        }
    }
    
    /**
     * Creates and returns object to inject in the dependency.
     * This method handles aggregate, collection and proxy cases.
     * @param refs the available service references
     * @return the object to inject. Can be a 'simple' object, a proxy, 
     * a collection or an array.
     */
    private Object getServiceObjects(ServiceReference [] refs) {
        if (m_proxy) {
            if (m_proxyObject == null) { // Not aggregate.
                return new ServiceCollection(this);
            } else {
                return m_proxyObject;
            }
        } else {
            // Initialize the thread local object is not already touched.
            Usage usage = (Usage) m_usage.get();
            if (usage.m_stack == 0) { // uninitialized usage.
                if (isAggregate()) {
                    if (m_collection) {
                        Collection svc = new ArrayList(refs.length);  // Use an array list as collection implementation. 
                        for (int i = 0; i < refs.length; i++) {
                            svc.add(getService(refs[i]));
                        }
                        usage.m_object = svc;
                    } else {
                        Object[] svc = (Object[]) Array.newInstance(getSpecification(),
                                refs.length);
                        for (int i = 0; i < svc.length; i++) {
                            svc[i] = getService(refs[i]);
                        }
                        usage.m_object = svc;
                    }
                } else {
                    usage.m_object = getService(refs[0]);
                }
                usage.inc(); // Start the caching, so set the stack level to 1
                m_usage.set(usage);
            }
            return usage.m_object;
        }
        
    }
    
    /**
     * Called by the proxy to get a service object to delegate a method.
     * This methods manages the waited time and on timeout policies.
     * @return a service object or a nullable/default-implmentation object.
     */
    public Object getService() {
        ServiceReference ref = getServiceReference();
        if (ref != null) {
            return getService(ref); // Return immediately the service object.
        } else {
            // Begin to wait ...
            long enter = System.currentTimeMillis();
            boolean exhausted = false;
            synchronized (this) {
                while (ref == null && !exhausted) {
                    try {
                        wait(1);
                    } catch (InterruptedException e) {
                        // We was interrupted ....
                    } finally {
                        long end = System.currentTimeMillis();
                        exhausted = (end - enter) > m_timeout;
                        ref = getServiceReference();
                    }
                }
            }
            // Check
            if (exhausted) {
                Object obj =  onTimeout(); // Throw the Runtime Exception
                if (obj == null) {
                    throw new NullPointerException("No service available"); // NPE if null.
                } else {
                    return obj; // Return a nullable or DI
                }
            } else {
               // If not exhausted, ref is not null.
                return getService(ref);
            }
        }
    }

    /**
     * Start method. Initializes the nullable object.
     * @see org.apache.felix.ipojo.util.DependencyModel#start()
     */
    public void start() {
        super.start();
        switch (m_policy) {
            case TemporalHandler.NULL:
                m_nullableObject = null;
                break;
            case TemporalHandler.NULLABLE:
                // To load the proxy we use the POJO class loader. Indeed, this
                // classloader imports iPOJO (so can access to Nullable) and has
                // access to the service specification.
                try {
                    m_nullableObject = Proxy.newProxyInstance(m_handler
                            .getInstanceManager().getClazz().getClassLoader(),
                            new Class[] { getSpecification(), Nullable.class },
                            new NullableObject()); // NOPMD
                    if (isAggregate()) {
                        if (m_collection) {
                            List list = new ArrayList(1);
                            list.add(m_nullableObject);
                            m_nullableObject = list;
                        } else {
                            Object[] array = (Object[]) Array.newInstance(
                                getSpecification(), 1);
                            array[0] = m_nullableObject;
                            m_nullableObject = array;
                        }
                    }
                } catch (NoClassDefFoundError e) {
                    // A NoClassDefFoundError is thrown if the specification
                    // uses a
                    // class not accessible by the actual instance.
                    // It generally comes from a missing import.
                    throw new IllegalStateException(
                            "Cannot create the Nullable object, a referenced class cannot be loaded: "
                                    + e.getMessage());
                }

                break;
            case TemporalHandler.DEFAULT_IMPLEMENTATION:
                // Create the default-implementation object.
                try {
                    Class clazz = m_handler.getInstanceManager().getContext()
                            .getBundle().loadClass(m_di);
                    m_nullableObject = clazz.newInstance();
                } catch (IllegalAccessException e) {
                    throw new IllegalStateException(
                            "Cannot load the default-implementation " + m_di
                                    + " : " + e.getMessage());
                } catch (InstantiationException e) {
                    throw new IllegalStateException(
                            "Cannot load the default-implementation " + m_di
                                    + " : " + e.getMessage());
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "Cannot load the default-implementation " + m_di
                                    + " : " + e.getMessage());
                }
                if (isAggregate()) {
                    if (m_collection) {
                        List list = new ArrayList(1);
                        list.add(m_nullableObject);
                        m_nullableObject = list;
                    } else {
                        Object[] array = (Object[]) Array.newInstance(
                            getSpecification(), 1);
                        array[0] = m_nullableObject;
                        m_nullableObject = array;
                    }
                }
                break;
            case TemporalHandler.EMPTY:
                if (! m_collection) {
                    m_nullableObject = Array.newInstance(getSpecification(), 0);
                } else { // Empty collection
                    m_nullableObject = new ArrayList(0);
                }
                break;
            default: // Cannot occurs
                break;
        }
    }

    /**
     * Stop method. Just releases the reference on the nullable object.
     * @see org.apache.felix.ipojo.util.DependencyModel#stop()
     */
    public void stop() {
        super.stop();
        m_nullableObject = null;
        m_proxyObject = null;
    }

    /**
     * The monitored field receives a value. Nothing to do.
     * @param arg0 POJO setting the value.
     * @param arg1 field name
     * @param arg2 received value
     * @see org.apache.felix.ipojo.FieldInterceptor#onSet(java.lang.Object, java.lang.String, java.lang.Object)
     */
    public void onSet(Object arg0, String arg1, Object arg2) { }

    /**
     * Implements the timeout policy according to the specified configuration.
     * @return the object to return when the timeout occurs.
     */
    Object onTimeout() {
        switch (m_policy) {
            case TemporalHandler.NULL:
            case TemporalHandler.NULLABLE:
            case TemporalHandler.DEFAULT_IMPLEMENTATION:
            case TemporalHandler.EMPTY:
                return m_nullableObject;
            default:
                // Throws a runtime exception
                throw new RuntimeException("Service "
                        + getSpecification().getName()
                        + " unavailable : timeout");
        }
    }
    
    long getTimeout() {
        return m_timeout;
    }
    
    /**
     * Creates proxy object for proxied scalar dependencies.
     */
    private class ProxyFactory extends ClassLoader {
        
        /**
         * Instance classloader, used to load specification and dependent classes.
         */
        private ClassLoader m_instanceCL;
        
        /**
         * Handler classloader, used to load the temporal dependency class. 
         */
        private ClassLoader m_handlerCL;
        
        /**
         * Creates the proxy classloader.
         * @param parent1 the instance classloader.
         * @param parent2 the handler classloader.
         */
        public ProxyFactory(ClassLoader parent1, ClassLoader parent2) {
            this.m_instanceCL = parent1;
            this.m_handlerCL = parent2;
        }
        
        /**
         * Loads a proxy class generated for the given (interface) class.
         * @param clazz the service specification to proxy
         * @return the Class object of the proxy.
         */
        protected Class getProxyClass(Class clazz) {
            byte[] clz = ProxyGenerator.dumpProxy(clazz); // Generate the proxy.
            return defineClass(clazz.getName() + "$$Proxy", clz, 0, clz.length);
        }
        
        /**
         * Create a proxy object for the given specification. The proxy
         * uses the given temporal dependency to get the service object.  
         * @param spec the service specification (interface)
         * @param dep the temporal dependency used to get the service
         * @return the proxy object.
         */
        public Object getProxy(Class spec, TemporalDependency dep) {
            try {
                Class clazz = getProxyClass(getSpecification());
                Constructor constructor = clazz.getConstructor(new Class[] {dep.getClass()}); // The proxy constructor
                return constructor.newInstance(new Object[] {dep});
            } catch (Throwable e) {
                m_handler.error("Cannot create the proxy object", e);
                m_handler.getInstanceManager().stop();
                return null;
            }
        }
        
        /**
         * Loads the given class.
         * This class use the classloader of the specification class
         * or the handler class loader.
         * @param name the class name
         * @return the class object
         * @throws ClassNotFoundException if the class is not found by the two classloaders.
         * @see java.lang.ClassLoader#loadClass(java.lang.String)
         */
        public Class loadClass(String name) throws ClassNotFoundException {
            try {
                return m_instanceCL.loadClass(name);
            } catch (ClassNotFoundException e) {
                return m_handlerCL.loadClass(name);
            }
        }
    }  
    

}
