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
package org.apache.felix.scrplugin.tags;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * <code>JavaClassDescription.java</code>...
 * Description of a java class
 *
 */
public interface JavaClassDescription {

    JavaClassDescription[] EMPTY_RESULT = new JavaClassDescription[0];

    /**
     * Get the java class tag with the name.
     * @param name
     * @return the tag or null.
     */
    JavaTag getTagByName(String name);

    /**
     * Get all class tags with this name.
     * @param name
     * @param inherited If true, parent classes are searched as well.
     * @return An array of tags or the empty array.
     * @throws MojoExecutionException
     */
    JavaTag[] getTagsByName(String name, boolean inherited)
    throws MojoExecutionException;

    /**
     * Get the description for the parent class.
     * @return The description or <code>null</code> if this class is the
     *         Object class.
     * @throws MojoExecutionException
     */
    JavaClassDescription getSuperClass() throws MojoExecutionException;

    /**
     * Get the name of the described class.
     * @return The name of the described class.
     */
    String getName();

    /**
     * Get the fields of this class
     * @return All fields or an empty array
     */
    JavaField[] getFields();

    /**
     * Get the field with the name.
     * @param name The name of the field
     * @return The field with the name or null.
     * @throws MojoExecutionException
     */
    JavaField getFieldByName(String name) throws MojoExecutionException;

    JavaField getExternalFieldByName(String name) throws MojoExecutionException;

    /**
     * Returns an array of the implemented interfaces of this class.
     * @return An array containing the interfaces or an empty array
     *         if this class does not implement any interface.
     * @throws MojoExecutionException
     */
    JavaClassDescription[] getImplementedInterfaces() throws MojoExecutionException;

    /**
     * Search for a method with the given signature.
     * @param name
     * @param parameters
     * @return A descriptor for the method or <code>null</code>
     * @throws MojoExecutionException
     */
    JavaMethod getMethodBySignature(String name, String[] parameters)
    throws MojoExecutionException;

    /**
     * Is this class public?
     * @return True if this class is public.
     */
    boolean isPublic();

    /**
     * Is this class abstract?
     * @return True if this class is abstract.
     */
    boolean isAbstract();

    /**
     * Is this class an interface?
     * @return True if this is an interface.
     */
    boolean isInterface();

    /**
     * Return all methods of this class
     * @return An array of methods or an empty array.
     */
    JavaMethod[] getMethods();

    /**
     * Is this class of the type?
     * @param type
     * @return True if this class is of the type.
     * @throws MojoExecutionException
     */
    boolean isA(String type) throws MojoExecutionException;

    /**
     * Search for the class.
     * If the referenced name is not fully qualified, the imports
     * of the class are searched.
     * @param referencedName
     * @return The java class description or null
     * @throws MojoExecutionException
     */
    JavaClassDescription getReferencedClass(String referencedName)
    throws MojoExecutionException;
}
