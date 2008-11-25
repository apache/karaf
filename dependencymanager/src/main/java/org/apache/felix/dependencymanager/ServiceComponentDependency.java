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
 * Describes a service component dependency.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface ServiceComponentDependency {
    public static final String[] STATE_NAMES = { 
        "optional unavailable", 
        "optional available", 
        "required unavailable", 
        "required available" 
        };
    public static final int STATE_UNAVAILABLE_OPTIONAL = 0;
    public static final int STATE_AVAILABLE_OPTIONAL = 1;
    public static final int STATE_UNAVAILABLE_REQUIRED = 2;
    public static final int STATE_AVAILABLE_REQUIRED = 3;
    public String getName();
    public String getType();
    public int getState();
}
