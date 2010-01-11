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
 * Annotates a method or a field for injecting a Service Dependency. When applied on a class 
 * field, optional unavailable dependencies are injected with a NullObject.
 */
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface ServiceDependency
{
    /**
     * Returns the Service dependency type (by default, the type is method parameter type).
     * @return the Service dependency type.
     */
    Class<?> service() default Object.class;

    /**
     * Returns the Service dependency OSGi filter.
     * @return The Service dependency filter.
     */
    String filter() default "";

    /**
     * Returns the class for the default implementation, if the dependency is not available.
     * @return The default class used when the dependency is not available.
     */
    Class<?> defaultImpl() default Object.class;

    /**
     * Returns whether the Service dependency is required or not.
     * @return true if the dependency is required, false if not.
     */
    boolean required() default true;

    /**
     * Returns the callback method to be invoked when the service is available. This attribute is only meaningful when 
     * the annotation is applied on a class field.
     */
    String added() default "";

    /**
     * Returns the callback method to be invoked when the service properties have changed.
     */
    String changed() default "";

    /**
     * Returns the callback method to invoke when the service is lost.
     */
    String removed() default "";
}
