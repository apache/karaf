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
package org.apache.felix.dm.impl.dependencies;

import org.apache.felix.dm.dependencies.Dependency;

public interface DependencyService {
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
    
    public Object getService(); // is also defined on the Service interface
    public void initService(); // was an implementation method
    public boolean isRegistered(); // impl method
    public Object[] getCompositionInstances(); // impl method
}
