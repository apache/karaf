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
 * Annotates a method or a field for injecting a Temporal Service Dependency.
 * A Temporal Service dependency can block the caller thread between service updates. Only
 * useful for required stateless dependencies that can be replaced transparently.
 * A Dynamic Proxy is used to wrap the actual service dependency. When the dependency goes 
 * away, an attempt is made to replace it with another one which satisfies the service dependency 
 * criteria. If no service replacement is available, then any method invocation (through the 
 * dynamic proxy) will block during a configurable timeout. On timeout, an unchecked 
 * <code>IllegalStateException</code> exception is raised (but the service is not deactivated).<p>
 * 
 * <b>Only supports required dependencies</b>
 *
 * <p> Sample Code:<p>
 * <blockquote>
 * 
 * <pre>
 * @Service
 * class MyServer implements Runnable {
 *   @TemporalServiceDependency(timeout=15000)
 *   MyDependency _dependency;.
 *   
 *   @Start
 *   void start() {
 *     (new Thread(this)).start();
 *   }
 *   
 *   public void run() {
 *     try {
 *       _dependency.doWork();
 *     } catch (IllegalStateException e) {
 *       t.printStackTrace();
 *     }
 *   }   
 * </pre>
 * 
 * </blockquote>
 */
@Retention(RetentionPolicy.CLASS)
@Target( { ElementType.METHOD, ElementType.FIELD })
public @interface TemporalServiceDependency
{
    /**
     * Sets the timeout for this temporal dependency. Specifying a timeout value of zero means that there is no timeout period,
     * and an invocation on a missing service will fail immediately.
     * 
     * @param timeout the dependency timeout value greater or equals to 0
     * @throws IllegalArgumentException if the timeout is negative
     * @return this temporal dependency
     */
    long timeout() default 30000L;

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
     * Returns the callback method to be invoked when the service is available. This attribute is only meaningful when 
     * the annotation is applied on a class field.
     */
    String added() default "";
}
