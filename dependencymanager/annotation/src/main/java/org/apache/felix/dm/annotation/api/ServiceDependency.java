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
     * The type if the service this dependency is applying on. By default, the type is method parameter or the class field type.
     */
    Class<?> service() default Object.class;

    /**
     * The Service dependency OSGi filter.
     */
    String filter() default "";

    /**
     * The class for the default implementation, if the dependency is not available.
     */
    Class<?> defaultImpl() default Object.class;

    /**
     * Whether the Service dependency is required or not.
     */
    boolean required() default true;

    /**
     * The callback method to be invoked when the service is available. This attribute is only meaningful when 
     * the annotation is applied on a class field.
     */
    String added() default "";

    /**
     * The callback method to be invoked when the service properties have changed.
     */
    String changed() default "";

    /**
     * The callback method to invoke when the service is lost.
     */
    String removed() default "";
    
    /** 
     * The max time in millis to wait for the dependency availability. 
     * Specifying a positive number allow to block the caller thread between service updates. Only
     * useful for required stateless dependencies that can be replaced transparently.
     * A Dynamic Proxy is used to wrap the actual service dependency (which must be an interface). 
     * When the dependency goes away, an attempt is made to replace it with another one which satisfies 
     * the service dependency criteria. If no service replacement is available, then any method invocation 
     * (through the dynamic proxy) will block during a configurable timeout. On timeout, an unchecked 
     * <code>IllegalStateException</code> exception is raised (but the service is not deactivated).<p>
     * Notice that the changed/removed callbacks are not used when the timeout parameter is > -1.
     * <p> 
     * 
     * -1 means no timeout at all (default). 0 means that invocation on a missing service will fail 
     * immediately. A positive number represents the max timeout in millis to wait for the service availability.
     * 
     * <p> Sample Code:<p>
     * <blockquote><pre>
     * &#64;Service
     * class MyServer implements Runnable {
     *   &#64;ServiceDependency(timeout=15000)
     *   MyDependency _dependency;.
     *   
     *   &#64;Start
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
     * </pre></blockquote>
     */
    long timeout() default -1;
    
    /**
     * The name used when dynamically configuring this dependency from the init method.
     * Specifying this attribute allows to dynamically configure the dependency 
     * <code>filter</code> and <code>required</code> flag from the Service's init method.
     * All unamed dependencies will be injected before the init() method; so from the init() method, you can
     * then pick up whatever information needed from already injected (unamed) dependencies, and configure dynamically
     * your named dependencies, which will then be calculated once the init() method returns.
     * 
     * <p> Usage example of a Service whose dependency filter is configured from ConfigAdmin:
     * 
     * <blockquote><pre>
     *  &#47;**
     *    * A Service whose service dependency filter/require attribute may be configured from ConfigAdmin
     *    *&#47;
     *  &#64;Service
     *  class X {
     *      private Dictionary m_config;
     *      
     *      &#64;ConfigurationDependency(pid="MyPid")
     *      void configure(Dictionary conf) {
     *           // Initialize our service from config ...
     *           
     *           // And store the config for later usage (from our init method)
     *           m_config = config;
     *      }
     * 
     *      // The returned Map will be used to configure our "dependency1" Dependency.
     *      &#64;Init
     *      Map init() {
     *          return new HashMap() {{
     *              put("dependency1.filter", m_config.get("filter"));
     *              put("dependency1.required", m_config.get("required"));
     *          }};
     *      } 
     *      
     *      &#64;ServiceDependency(name="dependency1") 
     *      void bindOtherService(OtherService other) {
     *         // the filter and required flag will be configured from our init method.
     *      }
     *  }
     *  </pre></blockquote>
     */
    String name() default "";
}
