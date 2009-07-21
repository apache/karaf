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

package org.apache.felix.sigil.repository;


import java.util.Collection;

import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.model.eclipse.ILibraryImport;
import org.eclipse.core.runtime.CoreException;


public interface IRepositoryManager
{
    void addRepositoryChangeListener( IRepositoryChangeListener listener );


    void removeRepositoryChangeListener( IRepositoryChangeListener listener );


    Collection<IBundleRepository> getRepositories();


    Collection<IBundleRepository> getRepositories( int level );


    void addLibrary( ILibrary library );


    void removeLibrary( ILibrary library );


    Collection<ILibrary> getLibraries();


    ILibrary resolveLibrary( final ILibraryImport l );


    IBundleResolver getBundleResolver();


    int[] getPriorityLevels();


    void visit( IModelWalker modelWalker );
}