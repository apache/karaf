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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks servlet classes as felix SCR component, and allows to configure sling
 * resource resolving mapping.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface SlingServlet {

    /**
     * Whether to generate a default SCR component tag with "immediate=true". If
     * set to false, a {@link org.apache.felix.scr.annotations.Component}
     * annotation can be added manually with defined whatever configuration
     * needed.
     */
    boolean generateComponent() default true;

    /**
     * Whether to generate a default SCR service tag with with
     * "interface=javax.servlet.Servlet". If set to false, a
     * {@link org.apache.felix.scr.annotations.Service} annotation can be added
     * manually with defined whatever configuration needed.
     */
    boolean generateService() default true;

    /**
     * The name of the service registration property of a Servlet registered as
     * a service providing the absolute paths under which the servlet is
     * accessible as a Resource (value is "sling.servlet.paths")
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types.
     */
    String[] paths() default {};

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the resource type(s) supported by the servlet (value
     * is "sling.servlet.resourceTypes").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #paths} property is set. Otherwise this property must be set or
     * the servlet is ignored.
     */
    String[] resourceTypes() default {};

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request URL selectors supported by the servlet
     * (value is "sling.servlet.selectors"). The selectors must be configured as
     * they would be specified in the URL that is as a list of dot-separated
     * strings such as <em>print.a4</em>.
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #paths} property is set. Otherwise this property is optional and
     * ignored if not set.
     */
    String[] selectors() default {};

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request URL extensions supported by the servlet
     * for GET requests (value is "sling.servlet.extensions").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #paths} property is set. Otherwise this property or the
     * {@link #methods} is optional and ignored if not set.
     */
    String[] extensions() default {};

    /**
     * The name of the service registration property of a Servlet registered as
     * a service containing the request methods supported by the servlet (value
     * is "sling.servlet.methods").
     * <p>
     * The type of this property is a String or String[] (array of strings)
     * denoting the resource types. This property is ignored if the
     * {@link #paths} property is set. Otherwise this property or the
     * {@link #extensions} is optional and ignored if not set.
     */
    String[] methods() default {};

}
