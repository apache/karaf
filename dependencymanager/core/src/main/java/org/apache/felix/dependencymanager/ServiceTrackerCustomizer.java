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
 * A modified version of a normal service tracker customizer. This one has an
 * extra callback "addedservice" that is invoked after the service has been added
 * to the tracker (and therefore is accessible through the tracker API).
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceTrackerCustomizer {
    public Object addingService(ServiceReference ref);
    public void addedService(ServiceReference ref, Object service);
    public void modifiedService(ServiceReference ref, Object service);
    public void removedService(ServiceReference ref, Object service);
}
