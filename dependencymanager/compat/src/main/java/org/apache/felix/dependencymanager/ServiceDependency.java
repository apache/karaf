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

import org.osgi.framework.ServiceReference;

/**
 * Service dependency that can track an OSGi service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public abstract class ServiceDependency implements Dependency
{
    /**
     * Sets the name of the service that should be tracked. 
     * 
     * @param serviceName the name of the service
     * @return this service dependency
     */
    public abstract ServiceDependency setService(Class serviceName);

    /**
     * Sets the name of the service that should be tracked. You can either specify
     * only the name, or the name and a filter. In the latter case, the filter is used
     * to track the service and should only return services of the type that was specified
     * in the name. To make sure of this, the filter is actually extended internally to
     * filter on the correct name.
     * 
     * @param serviceName the name of the service
     * @param serviceFilter the filter condition
     * @return this service dependency
     */
    public abstract ServiceDependency setService(Class serviceName, String serviceFilter);

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
    public abstract ServiceDependency setService(Class serviceName,
        ServiceReference serviceReference);

    /**
     * Sets the default implementation for this service dependency. You can use this to supply
     * your own implementation that will be used instead of a Null Object when the dependency is
     * not available. This is also convenient if the service dependency is not an interface
     * (which would cause the Null Object creation to fail) but a class.
     * 
     * @param implementation the instance to use or the class to instantiate if you want to lazily
     *     instantiate this implementation
     * @return this service dependency
     */
    public abstract ServiceDependency setDefaultImplementation(Object implementation);

    /**
     * Sets the required flag which determines if this service is required or not.
     * 
     * @param required the required flag
     * @return this service dependency
     */
    public abstract ServiceDependency setRequired(boolean required);

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in any attributes in the service implementation that
     * are of the same type as this dependency. Default is on.
     * 
     * @param autoConfig the value of auto config
     * @return this service dependency
     */
    public abstract ServiceDependency setAutoConfig(boolean autoConfig);

    /**
     * Sets auto configuration for this service. Auto configuration allows the
     * dependency to fill in the attribute in the service implementation that
     * has the same type and instance name.
     * 
     * @param instanceName the name of attribute to auto config
     * @return this service dependency
     */
    public abstract ServiceDependency setAutoConfig(String instanceName);

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. When you specify callbacks, the default auto configuration 
     * feature is automatically turned off, because we're assuming you don't need it in this 
     * case. If you need both features (callbacks + autoconfig), Then, you must explicitly invoke 
     * setAutoConfig(true) and setCallbacks methods.
     * 
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public ServiceDependency setCallbacks(String added, String removed)
    {
        return setCallbacks(null, added, null, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. When you specify callbacks, the default auto 
     * configuration feature is automatically turned off, because we're assuming you don't 
     * need it in this case. If you need both features (callbacks + autoconfig),
     * then, you must explicitly invoke setAutoConfig(true) and setCallbacks methods.
     * 
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public ServiceDependency setCallbacks(String added, String changed, String removed)
    {
        return setCallbacks(null, added, changed, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added or removed. They are called on the instance you provide. When you
     * specify callbacks, the default auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case. If you need both features (callbacks + autoconfig),
     * then, you must explicitly invoke setAutoConfig(true) and setCallbacks methods.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public ServiceDependency setCallbacks(Object instance, String added, String removed)
    {
        return setCallbacks(instance, added, null, removed);
    }

    /**
     * Sets the callbacks for this service. These callbacks can be used as hooks whenever a
     * dependency is added, changed or removed. They are called on the instance you provide. When you
     * specify callbacks, the default auto configuration feature is automatically turned off, because
     * we're assuming you don't need it in this case. If you need both features (callbacks + autoconfig),
     * then, you must explicitly invoke setAutoConfig(true) and setCallbacks methods.
     * 
     * @param instance the instance to call the callbacks on
     * @param added the method to call when a service was added
     * @param changed the method to call when a service was changed
     * @param removed the method to call when a service was removed
     * @return this service dependency
     */
    public abstract ServiceDependency setCallbacks(Object instance, String added, String changed,
        String removed);

    /**
     * Returns <code>true>code> if auto configuration is enabled for this dependency.
     * Auto configuration means that a dependency is injected in the service instance
     * when it's available, and if it's unavailable, a "null object" will be inserted
     * instead.
     * 
     * @return <code>true>code> if auto configuration is enabled for this dependency
     */
    public abstract boolean isAutoConfig();

    /**
     * Returns <code>true</code> if the dependency is available.
     * 
     * @return <code>true</code> if the dependency is available
     */
    public abstract boolean isAvailable();

    /**
     * Returns <code>true</code> if this a required dependency. Required dependencies
     * are dependencies that must be available before the service can be activated.
     * 
     * @return <code>true</code> if the dependency is required
     */
    public abstract boolean isRequired();
}
