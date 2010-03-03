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

import org.osgi.framework.Bundle;

/**
 * Annotates a Bundle Adapter Service class. The adapter will be applied to any bundle that
 * matches the specified bundle state mask and filter condition. For each matching
 * bundle an adapter will be created based on the adapter implementation class.
 * The adapter will be registered with the specified interface and existing properties 
 * from the original resource plus any extra properties you supply here.
 * It will also inherit all dependencies, and if you declare the original
 * bundle as a member it will be injected.
 */
public @Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
@interface BundleAdapterService
{
    /**
     * The filter used to match a given bundle.
     */
    String filter();
    
    /**
     * the bundle state mask to apply
     */
    int stateMask() default Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;
    
    /**
     * The interface to use when registering adapters. By default, the interface directly implemented
     * by the annotated class is used.
     */
    Class<?> service() default Object.class;
    
    /**
     * Additional properties to use with the service registration
     */
    Param[] properties() default {};
    
    /**
     * Specifies if properties from the bundle should be propagated to the service.
     */
    boolean propagate() default true;
}
