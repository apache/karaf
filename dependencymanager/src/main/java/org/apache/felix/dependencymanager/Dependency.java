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

/**
 * Generic dependency for a service. A dependency can be required or not.
 * A dependency will be activated by the service it belongs to. The service
 * will call the <code>start(Service service)</code> and 
 * <code>stop(Service service)</code> methods.
 * 
 * After it has been started, a dependency must callback
 * the associated service's <code>dependencyAvailable()</code> and 
 * <code>dependencyUnavailable()</code>
 * methods. State changes of the dependency itself may only be made as long as
 * the dependency is not 'active', meaning it is associated with a running service.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface Dependency {
    /**
     * Returns <code>true</code> if this a required dependency. Required dependencies
     * are dependencies that must be available before the service can be activated.
     * 
     * @return <code>true</code> if the dependency is required
     */
    public boolean isRequired();
    
    /**
     * Returns <code>true</code> if the dependency is available.
     * 
     * @return <code>true</code> if the dependency is available
     */
    public boolean isAvailable();
    
    /**
     * Starts tracking the dependency. This activates some implementation
     * specific mechanism to do the actual tracking. If the tracking discovers
     * that the dependency becomes available, it should call 
     * <code>dependencyAvailable()</code> on the service.
     * 
     * @param service the service that is associated with this dependency
     */
    public void start(Service service);
    
    /**
     * Stops tracking the dependency. This deactivates the tracking. If the
     * dependency was available, the tracker should call 
     * <code>dependencyUnavaible()</code> before stopping itself to ensure
     * that dependencies that aren't "active" are unavailable.
     */
    public void stop(Service service);
}
