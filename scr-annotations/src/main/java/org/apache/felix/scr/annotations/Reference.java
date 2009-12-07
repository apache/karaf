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
 * The <code>Reference</code> annotation defines references to other services
 * made available to the component by the Service Component Runtime.
 * <p>
 * This annotation may be declared for a Java Class or any Java Field to which
 * it might apply. Depending on where the annotation is declared, the parameters
 * may have different default values.
 * <p>
 * This annotation is used to declare &lt;reference&gt; elements of the
 * component declaration. See section 112.4.7, Reference Element, in the OSGi
 * Service Platform Service Compendium Specification for more information.
 */
@Target( { ElementType.TYPE, ElementType.FIELD })
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Reference {

    /**
     * The local name of the reference. If the annotation is declared on class
     * level, this parameter is required. If the tag is declared for a field,
     * the default value for the name parameter is the name of the field.
     */
    String name() default "";

    /**
     * The name of the service interface. This name is used by the Service
     * Component Runtime to access the service on behalf of the component. If
     * the annotation is declared on class level, this parameter is required. If
     * the annotation is declared for a field, the default value for the
     * interface parameter is the type of the field.
     */
    Class<?> referenceInterface() default AutoDetect.class;

    /**
     * The cardinality of the service reference. This must be one of 0..1, 1..1,
     * 0..n, and 1..n.
     */
    ReferenceCardinality cardinality() default ReferenceCardinality.MANDATORY_UNARY;

    /**
     * The dynamicity policy of the reference. If dynamic the service will be
     * made available to the component as it comes and goes. If static the
     * component will be deactivated and re-activated if the service comes
     * and/or goes away.
     */
    ReferencePolicy policy() default ReferencePolicy.STATIC;

    /**
     * A service target filter to select specific services to be made available.
     * In order to be able to overwrite the value of this value by a
     * configuration property, this parameter must be declared. If the parameter
     * is not declared, the respective declaration attribute will not be
     * generated.
     */
    String target() default "";

    /**
     * The name of the method to be called when the service is to be bound to
     * the component. The default value is the name created by appending the
     * reference name to the string bind. The method must be declared
     * <code>public</code> or <code>protected</code> and take single argument
     * which is declared with the service interface type.
     */
    String bind() default "";

    /**
     * The name of the method to be called when the service is to be unbound
     * from the component. The default value is the name created by appending
     * the reference name to the string unbind. The method must be declared
     * <code>public</code> or <code>protected</code> and take single argument
     * which is declared with the service interface type.
     */
    String unbind() default "";

    /**
     * The name of the method to be called when the bound service updates its
     * service registration properties. By default this is not set.
     */
    String updated() default "";

    /**
     * The reference strategy for the reference. This can either be
     * {@link ReferenceStrategy#EVENT} in which case the bind and unbind
     * methods are used or it can be {@link ReferenceStrategy#LOOKUP}
     * in which case the reference is looked up through the
     * component context.
     */
    ReferenceStrategy strategy() default ReferenceStrategy.EVENT;
}
