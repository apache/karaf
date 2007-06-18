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
package org.apache.felix.servicebinder.architecture;

/**
 *
 * A service to provide an architectural vision of the instances created by the
 * service binder
 *
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ArchitectureService
{
    /**
     * Get a list of all the available instance references
     *
     * @return a List containing all of the instance references
    **/
    public Instance [] getInstances();

    /**
     * Add a service binder listener
     *
     * @param listener a ServiceBinderListener to add to the Architecture service
    **/
    public void addServiceBinderListener(ServiceBinderListener listener);

    /**
     * Remove a service binder listener
     *
     * @param listener the listener to be removed
    **/
    public void removeServiceBinderListener(ServiceBinderListener listener);
}
