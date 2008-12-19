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
package org.apache.felix.ipojo;


/**
 * iPOJO Service Factory is a special service factory handling to get the 
 * instance consuming the service. The mechanism is equivalent to the OSGi Service
 * Factory.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public interface IPOJOServiceFactory {
    
    /**
     * Gets a service object.
     * @param instance the instance asking the for the service object.
     * @return the service object.
     */
    Object getService(ComponentInstance instance);
    
    /**
     * Un-gets a service object.
     * @param instance the instance un-getting the service object.
     * @param svcObject the service object used
     */
    void ungetService(ComponentInstance instance, Object svcObject);
}
