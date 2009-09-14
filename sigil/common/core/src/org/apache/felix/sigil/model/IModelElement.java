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

package org.apache.felix.sigil.model;


import java.util.Map;
import java.util.Set;


/**
 * IModelElement represent static information about a part of a model.
 * 
 * @author dave
 * 
 */
public interface IModelElement extends Cloneable
{
    /**
     * A brief human readable description of the element.
     * 
     * @return
     */
    String getElementDescription();


    /**
     * A set of key value pairs designed for use by a machine to classify a particular element.
     * 
     * @return
     */
    Map<Object, Object> getMeta();


    /**
     * Set meta data on this descriptor. Meta data is designed for use by a machine to classify or further enhance a
     * particular element.
     * 
     * @param meta
     */
    void setMeta( Map<Object, Object> meta );


    /**
     * Check to see if this element defines a complete set of properties. The definition of what constitutes a
     * complete set is up to the implementing class.
     * 
     * @throws InvalidModelException
     */
    void checkValid() throws InvalidModelException;


    /**
     * Find the parent element of this model element or null if no parent exists.
     * @return
     */
    IModelElement getParent();


    void setParent( IModelElement parent );


    /**
     * Finds the first ancestor up the hierarch of parents which is an instance of the specified
     * type.
     * 
     * @param type
     * @return
     */
    <T extends IModelElement> T getAncestor( Class<T> type );


    IModelElement clone();


    Set<String> getPropertyNames();


    void setProperty( String name, Object value ) throws NoSuchMethodException;


    void addProperty( String name, Object value ) throws NoSuchMethodException;


    void removeProperty( String name, Object value ) throws NoSuchMethodException;


    Object getProperty( String name ) throws NoSuchMethodException;


    Object getDefaultPropertyValue( String name );


    Set<String> getRequiredProperties();


    Class<?> getPropertyType( String name ) throws NoSuchMethodException;

}
