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

/**
 * This interface can be used to register a service state listener. Service
 * state listeners are called whenever a service state changes. You get notified
 * when the service is starting, started, stopping and stopped. Each callback
 * includes a reference to the service in question.
 * 
 * @author Marcel Offermans
 */
public interface ServiceStateListener {
    /**
     * Called when the service is starting.
     * 
     * @param service the service
     */
    public void starting(Service service);
    /**
     * Called when the service is started.
     * 
     * @param service the service
     */
    public void started(Service service);
    /**
     * Called when the service is stopping.
     * 
     * @param service the service
     */
    public void stopping(Service service);
    /**
     * Called when the service is stopped.
     * 
     * @param service the service
     */
    public void stopped(Service service);
}
