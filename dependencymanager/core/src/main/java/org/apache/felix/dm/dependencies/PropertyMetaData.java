/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0 Unless
 * required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.dm.dependencies;

/**
 * This interface defines meta data regarding a given configuration property.
 */
public interface PropertyMetaData
{
    /**
     * The label used to display the property. Example: "Log Level".
     * @return The label used to display the property (may be localized)
     */
    PropertyMetaData setHeading(String heading);

    /**
     * The key of a ConfigurationAdmin property. Example: "printer.logLevel"
     * @return The Configuration Admin property name
     */
    PropertyMetaData setId(String id);

    /**
     * Returns the property primitive type. If must be either one of the following types:<p>
     * <ul>
     *    <li>String.class</li>
     *    <li>Long.class</li>
     *    <li>Integer.class</li>
     *    <li>Character.class</li>
     *    <li>Byte.class</li>
     *    <li>Double.class</li>
     *    <li>Float.class</li>
     *    <li>Boolean.class</li>
     * </ul>
     */
    PropertyMetaData setType(Class type);

    /**
     * Returns a default for this property. The object must be of the appropriate type as defined by the cardinality and getType(). 
     * The return type is a list of String  objects that can be converted to the appropriate type. The cardinality of the return 
     * array must follow the absolute cardinality of this type. E.g. if the cardinality = 0, the array must contain 1 element. 
     * If the cardinality is 1, it must contain 0 or 1 elements. If it is -5, it must contain from 0 to max 5 elements. Note that 
     * the special case of a 0 cardinality, meaning a single value, does not allow arrays or vectors of 0 elements. 
     */
    PropertyMetaData setDefaults(String[] defaults);

    /**
     * Returns the property description. The description may be localized and must describe the semantics of this type and any 
     * constraints. Example: "Select the log level for the Printer Service".
     * @return a localizable description of the property.
     */
    PropertyMetaData setDescription(String description);

    /**
     * Return the cardinality of this property. The OSGi environment handles multi valued properties in arrays ([]) or in Vector objects. 
     * The return value is defined as follows:<p>
     *
     * <ul>
     * <li> x = Integer.MIN_VALUE    no limit, but use Vector</li>
     * <li> x < 0                    -x = max occurrences, store in Vector</li>
     * <li> x > 0                     x = max occurrences, store in array []</li>
     * <li> x = Integer.MAX_VALUE    no limit, but use array []</li>
     * <li> x = 0                     1 occurrence required</li>
     * </ul>
     */
    PropertyMetaData setCardinality(int cardinality);

    /**
     * Tells if this property is required or not.
     */
    PropertyMetaData setRequired(boolean required);

    /**
     * Return a list of valid options for this property (the labels may be localized).
     * @return the list of valid options for this property.
     */
    PropertyMetaData addOption(String optionLabel, String optionValue);
}