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
 * Describes a service component. Service components form descriptions of services
 * that are managed by the dependency manager. They can be used to query their state
 * for monitoring tools. The dependency manager shell command is an example of
 * such a tool.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceComponent {
    /** Names for the states of this component. */
    public static final String[] STATE_NAMES = { "unregistered", "registered" };
    /** State constant for an unregistered component. */
    public static final int STATE_UNREGISTERED = 0;
    /** State constant for a registered component. */
    public static final int STATE_REGISTERED = 1;
    /** Returns a list of dependencies associated with this service component. */
    public ServiceComponentDependency[] getComponentDependencies();
    /** Returns the name of this service component. */
    public String getName();
    /** Returns the state of this service component. */
    public int getState();
}
