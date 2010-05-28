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
package org.apache.felix.service.command;

/**
 * A converter is a service that can help create specific object types from a
 * string, and vice versa.
 * <p/>
 * The shell is capable of coercing arguments to the their proper type. However,
 * sometimes commands require extra help to do this conversion. This service can
 * implement a converter for a number of types.
 * <p/>
 * The command shell will rank these services in order of service.ranking and
 * will then call them until one of the converters succeeds.
 */
public interface Converter
{
    /**
     * This property is a string, or array of strings, and defines the classes
     * or interfaces that this converter recognizes. Recognized classes can be
     * converted from a string to a class and they can be printed in 3 different
     * modes.
     */
    String CONVERTER_CLASSES = "osgi.converter.classes";

    /**
     * Print the object in detail. This can contain multiple lines.
     */
    int INSPECT = 0;

    /**
     * Print the object as a row in a table. The columns should align for
     * multiple objects printed beneath each other. The print may run over
     * multiple lines but must not end in a CR.
     */
    int LINE = 1;

    /**
     * Print the value in a small format so that it is identifiable. This
     * printed format must be recognizable by the conversion method.
     */
    int PART = 2;

    /**
     * Convert an object to the desired type.
     * <p/>
     * Return null if the conversion can not be done. Otherwise return and
     * object that extends the desired type or implements it.
     *
     * @param desiredType The type that the returned object can be assigned to
     * @param in          The object that must be converted
     * @return An object that can be assigned to the desired type or null.
     * @throws Exception
     */
    Object convert(Class<?> desiredType, Object in) throws Exception;

    /**
     * Convert an objet to a CharSequence object in the requested format. The
     * format can be INSPECT, LINE, or PART. Other values must throw
     * IllegalArgumentException.
     *
     * @param target The object to be converted to a String
     * @param level  One of INSPECT, LINE, or PART.
     * @param escape Use this object to format sub ordinate objects.
     * @return A printed object of potentially multiple lines
     * @throws Exception
     */
    CharSequence format(Object target, int level, Converter escape) throws Exception;
}
