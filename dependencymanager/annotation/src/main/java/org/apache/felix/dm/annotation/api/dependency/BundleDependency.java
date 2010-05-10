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
package org.apache.felix.dm.annotation.api.dependency;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.osgi.framework.Bundle;

/**
 * Annotates a method for a bundle dependency.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface BundleDependency
{
    /**
     * Returns the callback method to be invoked when the service have changed.
     */
    String changed() default "";

    /**
     * Returns the callback method to invoke when the service is lost.
     */
    String removed() default "";
    
    /**
     * Returns whether the dependency is required or not.
     */
    boolean required() default true;
    
    /**
     * Returns the filter dependency
     */
    String filter() default "";
     
    /**
     * Returns the bundle state mask
     */
    int stateMask() default Bundle.INSTALLED | Bundle.RESOLVED | Bundle.ACTIVE;

    /**
     * @TODO
     */
    boolean propagate() default false;
}
