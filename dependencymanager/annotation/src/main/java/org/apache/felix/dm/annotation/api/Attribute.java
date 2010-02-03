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
 * This Annotation has to be specified within the MetaType annotation, and declares an Attribute Definition
 * which complies to the MetaType specification.
 */
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.ANNOTATION_TYPE)
public @interface Attribute
{
    /**
     * Unique identity for this attribute. Attributes share a global namespace in the registry. E.g. an attribute cn or commonName 
     * must always be a String  and the semantics are always a name of some object. They share this aspect with LDAP/X.500 attributes. 
     * In these standards the OSI Object Identifier (OID) is used to uniquely identify an attribute. If such an OID exists, (which can 
     * be requested at several standard organisations and many companies already have a node in the tree) it can be returned here. Otherwise,
     * a unique id should be returned which can be a Java class name (reverse domain name) or generated with a GUID algorithm.
     * Note that all LDAP defined attributes already have an OID. It is strongly advised to define the attributes from existing LDAP schemes 
     * which will give the OID. Many such schemes exist ranging from postal addresses to DHCP parameters.
     */
    String id();

    /**
     * Return the type for this attribute. If must be either one of the following types:<p>
     * <ul>
     *    <li>String.class</li>
     *    <li>Long.class</li>
     *    <li>Integer.class</li>
     *    <li>Char.class</li>
     *    <li>Byte.class</li>
     *    <li>Double.class</li>
     *    <li>Float.class</li>
     *    <li>Boolean.class</li>
     * </ul>
     */
    Class<?> type() default String.class;

    /**
     * Return a default for this attribute. The object must be of the appropriate type as defined by the cardinality and getType(). 
     * The return type is a list of String  objects that can be converted to the appropriate type. The cardinality of the return 
     * array must follow the absolute cardinality of this type. E.g. if the cardinality = 0, the array must contain 1 element. 
     * If the cardinality is 1, it must contain 0 or 1 elements. If it is -5, it must contain from 0 to max 5 elements. Note that 
     * the special case of a 0 cardinality, meaning a single value, does not allow arrays or vectors of 0 elements. 
     */
    String[] defaults() default {};

    /**
     * Returns the name of the attribute. This name may be localized.
     */
    String name();

    /**
     * Returns a description of this attribute. The description may be localized and must describe the semantics of this type and any 
     * constraints.
     * @return The localized description of the definition.
     */
    String description();

    /**
     * Return the cardinality of this attribute. The OSGi environment handles multi valued attributes in arrays ([]) or in Vector objects. 
     * The return value is defined as follows:<p>
     *
     * <ul>
     * <li> x = Integer.MIN_VALUE    no limit, but use Vector</li>
     * <li> x < 0                    -x = max occurrences, store in Vector</li>
     * <li> x > 0                     x = max occurrences, store in array []</li>
     * <li> x = Integer.MAX_VALUE    no limit, but use array []</li>
     * <li> x = 0                     1 occurrence required</li>
     */
    int cardinality() default 0;

    /**
     * Tells if this attribute is required or not.
     */
    boolean required() default true;

    /**
     * Return a list of option that this attribute can take.
     * The Options are defined using the <code>Property</code> annotation, where the name attributes is used to
     * reference the option label, and the value attribute is used to reference the option value.
     */
    Property[] options() default {};
}
