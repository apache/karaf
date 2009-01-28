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
import java.util.List;

import org.osgi.framework.ServiceRegistration;

/**
 * Service interface.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Service {
    /**
     * Adds a new dependency to this service.
     * 
     * @param dependency the dependency to add
     * @return this service
     */
    public Service add(Dependency dependency);
    
    /**
     * Removes a dependency from this service.
     * 
     * @param dependency the dependency to remove
     * @return this service
     */
    public Service remove(Dependency dependency);

    /**
     * Sets the public interface under which this service should be registered
     * in the OSGi service registry.
     *  
     * @param serviceName the name of the service interface
     * @param properties the properties for this service
     * @return this service
     */
    public Service setInterface(String serviceName, Dictionary properties);
    
    /**
     * Sets the public interfaces under which this service should be registered
     * in the OSGi service registry.
     *  
     * @param serviceNames the names of the service interface
     * @param properties the properties for this service
     * @return this service
     */
    public Service setInterface(String[] serviceNames, Dictionary properties);
    
    /**
     * Sets the implementation for this service. You can actually specify
     * an instance you have instantiated manually, or a <code>Class</code>
     * that will be instantiated using its default constructor when the
     * required dependencies are resolved (effectively giving you a lazy
     * instantiation mechanism).
     * 
     * There are four special methods that are called when found through
     * reflection to give you some life-cycle management options:
     * <ol>
     * <li><code>init()</code> is invoked right after the instance has been
     * created, and before any dependencies are resolved, and can be used to
     * initialize the internal state of the instance</li>
     * <li><code>start()</code> is invoked after the required dependencies
     * are resolved and injected, and before the service is registered</li>
     * <li><code>stop()</code> is invoked right after the service is
     * unregistered</li>
     * <li><code>destroy()</code> is invoked after all dependencies are
     * removed</li>
     * </ol>
     * In short, this allows you to initialize your instance before it is
     * registered, perform some post-initialization and pre-destruction code
     * as well as final cleanup. If a method is not defined, it simply is not
     * called, so you can decide which one(s) you need. If you need even more
     * fine-grained control, you can register as a service state listener too.
     * 
     * @param implementation the implementation
     * @return this service
     * @see ServiceStateListener
     */
    public Service setImplementation(Object implementation);
    
    /**
     * Returns a list of dependencies.
     * 
     * @return a list of dependencies
     */
    public List getDependencies();
    
    /**
     * Returns the service registration for this service. The method
     * will return <code>null</code> if no service registration is
     * available.
     * 
     * @return the service registration
     */
    public ServiceRegistration getServiceRegistration();
    
    /**
     * Returns the service instance for this service. The method will
     * return <code>null</code> if no service instance is available.
     * 
     * @return the service instance
     */
    public Object getService();

    /**
     * Returns the service properties associated with the service.
     * 
     * @return the properties or <code>null</code> if there are none
     */
    public Dictionary getServiceProperties();
    
    /**
     * Sets the service properties associated with the service. If the service
     * was already registered, it will be updated.
     * 
     * @param serviceProperties the properties
     */
    public void setServiceProperties(Dictionary serviceProperties);
    
    /**
     * Sets the names of the methods used as callbacks. These methods, when found, are
     * invoked as part of the life-cycle management of the service implementation. The
     * methods should not have any parameters.
     * 
     * @param init the name of the init method
     * @param start the name of the start method
     * @param stop the name of the stop method
     * @param destroy the name of the destroy method
     * @return the service instance
     */
    public Service setCallbacks(String init, String start, String stop, String destroy);

    // listener
    /**
     * Adds a service state listener to this service.
     * 
     * @param listener the state listener
     */
    public void addStateListener(ServiceStateListener listener);

    /**
     * Removes a service state listener from this service.
     * 
     * @param listener the state listener
     */
    public void removeStateListener(ServiceStateListener listener);
    
    // events, must be fired when the dependency is started/active
    
    /**
     * Will be called when the dependency becomes available.
     * 
     * @param dependency the dependency
     */
    public void dependencyAvailable(Dependency dependency);
    
    /**
     * Will be called when the dependency changes.
     * 
     * @param dependency the dependency
     */
    public void dependencyUnavailable(Dependency dependency);
    
    /**
     * Will be called when the dependency becomes unavailable.
     * 
     * @param dependency the dependency
     */
    public void dependencyChanged(Dependency dependency);

    /**
     * Starts the service. This activates the dependency tracking mechanism
     * for this service.
     */
    public void start();
    
    /**
     * Stops the service. This deactivates the dependency tracking mechanism
     * for this service.
     */
    public void stop();
    
    /**
     * Sets the factory to use to create the implementation. You can specify
     * both the factory class and method to invoke. The method should return
     * the implementation, and can use any method to create it. Actually, this
     * can be used together with <code>setComposition</code> to create a
     * composition of instances that work together to implement a service. The
     * factory itself can also be instantiated lazily by not specifying an
     * instance, but a <code>Class</code>.
     * 
     * @param factory the factory instance or class
     * @param createMethod the name of the create method
     */
	public Service setFactory(Object factory, String createMethod);
	
	/**
	 * Sets the factory to use to create the implementation. You specify the
	 * method to invoke. The method should return the implementation, and can
	 * use any method to create it. Actually, this can be used together with
	 * <code>setComposition</code> to create a composition of instances that
	 * work together to implement a service.
	 * <p>
	 * Note that currently, there is no default for the factory, so please use
	 * <code>setFactory(factory, createMethod)</code> instead.
	 * 
	 * @param createMethod the name of the create method
	 */
	public Service setFactory(String createMethod);
	
	/**
	 * Sets the instance and method to invoke to get back all instances that
	 * are part of a composition and need dependencies injected. All of them
	 * will be searched for any of the dependencies. The method that is
	 * invoked must return an <code>Object[]</code>.
	 * 
	 * @param instance the instance that has the method
	 * @param getMethod the method to invoke
	 */
	public Service setComposition(Object instance, String getMethod);
	
	/**
	 * Sets the method to invoke on the service implementation to get back all
	 * instances that are part of a composition and need dependencies injected.
	 * All of them will be searched for any of the dependencies. The method that
	 * is invoked must return an <code>Object[]</code>.
	 * 
	 * @param getMethod the method to invoke
	 */
	public Service setComposition(String getMethod);
}
