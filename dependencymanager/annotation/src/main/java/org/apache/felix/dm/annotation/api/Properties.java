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
 * Describes configuration properties MetaData informations. This annotation is used to generate
 * a MetaType xml file under OSGI-INF/metatype/metatype.xml and is used to configure
 * Service Component properties through web console.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface Properties
{
    /**
     * Sets the PID that is associated with this annotation. By default, the PID is the full java class name.
     * @return The PID that is associated with this annotation.
     */
    String pid() default "";

    /**
     * The label used to display the tab name (or section) where the properties are displayed. Example: "Printer Service".
     * @return The label used to display the tab name where the properties are displayed.
     */
    String heading();

    /**
     * A human readable description of the PID this annotation is associated with. Example: "Configuration for the PrinterService bundle".
     * @return A human readable description of the PID this annotation is associated with.
     */
    String description();

    /**
     * The list of properties types used to expose properties in web console. 
     * @return The list of properties types used to expose properties in web console. 
     */
    Property[] properties();
}
