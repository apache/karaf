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
 * Annotates an OSGi Service class.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Service
{
    /**
     * Returns the list of provided interfaces. By default, the directly implemented interfaces are provided.
     * @return The list of provided interfaces.
     */
    Class<?>[] provide() default Object.class;

    /**
     * Returns the list of provided service properties.
     * @return The list of provided service properties.
     */
    Property[] properties() default {};
    
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
