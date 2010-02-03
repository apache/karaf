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
 * Annotates a class for describing Properties MetaType informations.
 * The corresponding OSGI-INF/metatype/metatype.xml will be generated.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MetaType
{
    /**
     * Return the PIDs (for ManagedServices) for which this metatype is applying.
     * @return the PIDS for which this metatype is applying.
     */
    String[] pids() default {};

    /**
     * Points to the Properties file that can localize this MetaType informations.
     */
    String localization() default "";

    /**
     * Returns the id of this MetaType meta type.
     */
    String id() default "";

    /**
     * Return a description of this metatype. The description may be localized.
     * @return The description of this meta type.
     */
    String description() default "";

    /**
     * Return the name of this meta type. The name may be localized.
     * @return The name of this meta type
     */
    String name() default "";

    /**
     * Returns the attribute definitions for this meta type.
     */
    Attribute[] attributes();
}
