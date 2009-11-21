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
 * This interface can be used to register a service state listener. Service
 * state listeners are called whenever a service state changes. You get notified
 * when the service is starting, started, stopping and stopped. Each callback
 * includes a reference to the service in question.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceStateListener {
    /**
     * Called when the service is starting. At this point, the required
     * dependencies have been injected, but the service has not been registered
     * yet.
     * 
     * @param service the service
     */
    public void starting(Service service);
    
    /**
     * Called when the service is started. At this point, the service has been
     * registered.
     * 
     * @param service the service
     */
    public void started(Service service);
    
    /**
     * Called when the service is stopping. At this point, the service is still
     * registered.
     * 
     * @param service the service
     */
    public void stopping(Service service);
    
    /**
     * Called when the service is stopped. At this point, the service has been
     * unregistered.
     * 
     * @param service the service
     */
    public void stopped(Service service);
}
