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
 * Annotates an Aspect Service. The aspect will be applied to any service that
 * matches the specified interface and filter. For each matching service
 * an aspect will be created based on the aspect implementation class.
 * The aspect will be registered with the same interface and properties
 * as the original service, plus any extra properties you supply here.
 * It will also inherit all dependencies, and if you declare the original
 * service as a member it will be injected.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface AspectService
{
    /**
     * Returns the service interface to apply the aspect to. By default, the directly implemented interface is used.
     * @return The service interface to apply the aspect to.
     */
    Class<?> service() default Object.class;

    /**
     * the filter condition to use with the service interface this aspect is applying to.
     * @return the filter condition to use with the service interface
     */
    String filter() default "";
    
    /**
     * Additional properties to use with the aspect service registration
     * @return additional properties to use with the aspect service registration
     */
    Property[] properties() default {};
    
    /**
     * Ranking of this aspect. Since aspects are chained, the ranking defines the order in which they are chained.
     * Chain ranking is implemented as a service ranking so service lookups automatically retrieve the top of the
     * chain.
     * 
     * @return the ranking of this aspect
     */
    int ranking();
    
    /**
     * Returns the Class of the class which acts as a factory for this Service. The default method
     * factory name is "create". If you need to invoke another method, then you can use the 
     * <code>factoryMethod</code> attribute.
     * @return the factory Class name.
     */
    Class<?> factory() default Object.class;

    /**
     * Returns the method name of the factory class which will create our Service instance.
     * @return the factory method name.
     */
    String factoryMethod() default "";
}
