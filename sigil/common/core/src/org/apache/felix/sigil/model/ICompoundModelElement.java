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


import java.util.Set;


/**
 * Represents a model element that is made up of sub model elements.
 * 
 * @author dave
 *
 */
public interface ICompoundModelElement extends IModelElement
{
    /**
     * Calls the applicable set/add method of this model to add the element to this part of the model.
     * 
     * @param children
     * @return
     * @throws InvalidModelException
     */
    boolean addChild( IModelElement children ) throws InvalidModelException;


    /**
     * Calls the applicable set/remove method of this model to remove the element from this part of the model
     * @param children
     * @return
     */
    boolean removeChild( IModelElement children );


    /**
     * List all direct child elements of this model element.
     * @return
     */
    IModelElement[] children();


    /**
     * Visits child elements of this model element, recursing down to sub compound elements when found.
     * 
     * @param walker
     */
    void visit( IModelWalker walker );


    /**
     * Searches the model to find all child model elements which match the specified type
     * 
     * @param <T>
     * @param type
     * @return
     */
    <T extends IModelElement> T[] childrenOfType( Class<T> type );


    Set<Class<? extends IModelElement>> getRequiredChildren();


    Set<Class<? extends IModelElement>> getOptionalChildren();
}