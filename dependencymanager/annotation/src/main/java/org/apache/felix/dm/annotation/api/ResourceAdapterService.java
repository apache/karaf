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
 * Annotates a class as a Resource Adapter Service. The adapter will be applied to any resource 
 * that matches the specified filter condition. For each matching resource
 * an adapter will be created based on the adapter implementation class.
 * The adapter will be registered with the specified interface and existing properties
 * from the original resource plus any extra properties you supply here.
 * It will also inherit all dependencies, and if you declare the original
 * service as a member it will be injected.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface ResourceAdapterService
{
    /**
     * The filter condition to use with the resource.
     */
    String filter();

    /**
     * The interface to use when registering adapters
     */
    Class<?> service() default Object.class;

    /**
     * Additional properties to use with the adapter service registration
     */
    Param[] properties() default {};

    /**
     * <code>true</code> if properties from the resource should be propagated to the service.
     */
    boolean propagate() default false;
}
