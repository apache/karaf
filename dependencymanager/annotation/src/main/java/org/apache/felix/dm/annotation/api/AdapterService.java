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
package org.apache.felix.dm.annotation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotates an Adapater Service. The adapter will be applied to any service that
 * matches the implemented interface and filter. For each matching service
 * an adapter will be created based on the adapter implementation class.
 * The adapter will be registered with the specified interface and existing properties
 * from the original service plus any extra properties you supply here.
 * It will also inherit all dependencies, and if you declare the original
 * service as a member it will be injected.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AdapterService
{
    /**
     * Returns the adapter service interface . By default, the directly implemented interface is used.
     * @return The service interface to apply the adapter to.
     */
    Class<?> adapterService() default Object.class;

    /**
     * The adapter service properites. They will be added to the adapted service properties.
     * @return additional properties to use with the adapter service registration
     */
    Property[] adapterProperties() default {};

    /**
     * The adapted service interface
     */
    Class<?> adapteeService();
    
    /**
     * the filter condition to use with the adapted service interface.
     * @return the filter condition to use with the adapted ervice interface
     */
    String adapteeFilter() default "";
}
