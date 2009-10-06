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
package org.apache.felix.scr.annotations;

import java.lang.annotation.*;

/**
 * The <code>Component</code> annotation is the only required annotation. If
 * this annotation is not declared in a Java class, the class is not declared as
 * a component.
 * <p>
 * This annotation is used to declare the &lt;component&gt; element of the
 * component declaration. See section 112.4.3, Component Element, in the OSGi
 * Service Platform Service Compendium Specification for more information. The
 * required &lt;implementation&gt; element is automatically generated with the
 * fully qualified name of the class containing the <code>Component</code>
 * annotation.
 * </p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Component {

    /**
     * Defines the Component name also used as the PID for the Configuration
     * Admin Service. Default value: Fully qualified name of the Java class.
     */
    String name() default "";

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

    /**
     * Whether the component is enabled when the bundle starts.
     */
    boolean enabled() default true;

    /**
     * Whether the component is a factory component.
     */
    String factory() default "";

    /**
     * Whether the component is immediately activated.
     */
    boolean immediate() default false;

    /**
     * Whether any service, property and reference declarations from base
     * classes should be inherited by this class.
     */
    boolean inherit() default true;

    /**
     * Whether Metatype Service data is generated or not. If this parameter is
     * set to true Metatype Service data is generated in the
     * <code>metatype.xml</code> file for this component. Otherwise no Metatype
     * Service data is generated for this component.
     */
    boolean metatype() default false;

    /**
     * This marks an abstract service description which is not added to the
     * descriptor but intended for reuse through inheritance. This attribute
     * defaults to true for abstract classes and false for concrete classes.
     */
    boolean componentAbstract() default false;

    /**
     * Whether Declarative Services descriptor is generated or not. If this
     * parameter is not set or set to true the Declarative Services descriptor
     * is generated in the service descriptor file for this component. Otherwise
     * no Declarative Services descriptor is generated for this component.
     */
    boolean ds() default true;

    /**
     * The version of the Declarative Services specification against which the
     * component is written. Generally, the Maven SCR Plugin is able to
     * automatically detect which specification version a Component is written
     * against. There are some cases, though, where this is not easily or
     * reliably possible. In these cases use this attribute to force the
     * specification version.
     * <p>
     * Valid values currently are <code>1.0</code> and <code>1.1</code>. If
     * an unsupported value is declared, a descriptor failure results.
     *
     * @since 1.0.1
     */
    String specVersion() default "1.0";

    /**
     * Generated <code>service.pid</code> property by default, if none declared
     * explicitly.
     */
    boolean createPid() default true;

    /**
     * Set the metatype factory pid property (only for non factory components).
     * @since 1.0
     */
    boolean getConfigurationFactory() default false;

    /**
     * The configuration policy
     * @since 1.0
     */
    ConfigurationPolicy policy() default ConfigurationPolicy.OPTIONAL;
}
