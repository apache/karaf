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
package org.apache.felix.scr.annotations.sling;

import java.lang.annotation.*;

/**
 * Marks servlet classes as SCR component, and allows to add a
 * filter to Sling's processing.
 * This annotation generates to private properties for the
 * order and the scope.
 * By default it also generates a component and a service tag,
 * but this generation can be overriden.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface SlingFilter {

    /**
     * The order of the filter.
     * This value is used to sort the filters. Filters with a lower order
     * are executed before a filter with a higher order. If two filters
     * have the same order, they are executed in an undefined order.
     */
    int order();

    /**
     * The scope of a filter.
     * If the filter has request scope, it is run once for a request.
     * If the filter has component scope, it is run once for every included
     * component (rendering).
     */
    SlingFilterScope scope() default SlingFilterScope.REQUEST;

    /**
     * Whether to generate a default SCR component tag with. If
     * set to false, a {@link org.apache.felix.scr.annotations.Component}
     * annotation can be added manually with defined whatever configuration
     * needed.
     */
    boolean generateComponent() default true;

    /**
     * Whether to generate a default SCR service tag with
     * "interface=javax.servlet.Filter". If set to false, a
     * {@link org.apache.felix.scr.annotations.Service} annotation can be added
     * manually with defined whatever configuration needed.
     */
    boolean generateService() default true;

    /**
     * Defines the Component name also used as the PID for the Configuration
     * Admin Service. Default value: Fully qualified name of the Java class.
     */
    String name() default "";

    /**
     * Whether Metatype Service data is generated or not. If this parameter is
     * set to true Metatype Service data is generated in the
     * <code>metatype.xml</code> file for this component. Otherwise no Metatype
     * Service data is generated for this component.
     */
    boolean metatype() default false;

    /**
     * This is generally used as a title for the object described by the meta
     * type. This name may be localized by prepending a % sign to the name.
     * Default value: %&lt;name&gt;.name
     */
    String label() default "";

    /**
     * This is generally used as a description for the object described by the
     * meta type. This name may be localized by prepending a % sign to the name.
     * Default value: %&lt;name&gt;.description
     */
    String description() default "";
}
