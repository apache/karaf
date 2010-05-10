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
package org.apache.felix.dm.annotation.api.adapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.PropertyMetaData;

/**
 * Annotates a class that acts as a Factory Configuration Adapter Service. For each new <code>Config Admin</code> factory configuration matching
 * the specified factoryPid, an instance of this service will be created.
 * The adapter will be registered with the specified interface, and with the specified adapter service properties.
 * Depending on the <code>propagate</code> parameter, every public factory configuration properties 
 * (which don't start with ".") will be propagated along with the adapter service properties. 
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface FactoryConfigurationAdapterService
{
    /**
     * Returns the factory pid whose configurations will instantiate the annotated service class. (By default, the pid is the 
     * service class name).
     */
    String factoryPid() default "";

    /**
     * The Update method to invoke (defaulting to "updated"), when a factory configuration is created or updated
     */
    String updated() default "updated";

    /**
     * Returns true if the configuration properties must be published along with the service. 
     * Any additional service properties specified directly are merged with these.
     * @return true if configuration must be published along with the service, false if not.
     */
    boolean propagate() default false;

    /**
     * The interface(s) to use when registering adapters. By default, directly implemented 
     * interfaces will be registered in the OSGi registry.
     */
    Class<?>[] service() default {};

    /**
     * Adapter Service properties. Notice that public factory configuration is also registered in service properties,
     * (only if propagate is true). Public factory configuration properties are those which don't starts with a dot (".").
     */
    Property[] properties() default {};

    /**
     * The label used to display the tab name (or section) where the properties are displayed. Example: "Printer Service".
     * @return The label used to display the tab name where the properties are displayed.
     */
    String heading() default "";

    /**
     * A human readable description of the PID this annotation is associated with. Example: "Configuration for the PrinterService bundle".
     * @return A human readable description of the PID this annotation is associated with.
     */
    String description() default "";

    /**
     * The list of properties types used to expose properties in web console. 
     * @return The list of properties types used to expose properties in web console. 
     */
    PropertyMetaData[] metadata() default {};
}
