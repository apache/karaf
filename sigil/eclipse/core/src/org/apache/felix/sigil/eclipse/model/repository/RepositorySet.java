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

package org.apache.felix.sigil.eclipse.model.repository;


import java.util.ArrayList;
import java.util.Collection;


public class RepositorySet implements IRepositorySet
{

    private static final IRepositoryModel[] EMPTY = new IRepositoryModel[0];

    private IRepositoryModel[] reps;


    public RepositorySet()
    {
        this( EMPTY );
    }


    public RepositorySet( Collection<IRepositoryModel> reps )
    {
        this( reps.toArray( new IRepositoryModel[reps.size()] ) );
    }


    public RepositorySet( IRepositoryModel[] repositories )
    {
        this.reps = repositories;
    }


    public void setRepository( IRepositoryModel id, int position )
    {
        ArrayList<IRepositoryModel> tmp = new ArrayList<IRepositoryModel>( reps.length + 1 );
        tmp.remove( id );
        tmp.add( position, id );
        reps = tmp.toArray( new IRepositoryModel[tmp.size()] );
    }


    public IRepositoryModel[] getRepositories()
    {
        return reps;
    }


    public void removeRepository( IRepositoryModel id )
    {
        ArrayList<IRepositoryModel> tmp = new ArrayList<IRepositoryModel>( reps.length + 1 );
        tmp.remove( id );
        reps = tmp.toArray( new IRepositoryModel[tmp.size()] );
    }


    public void setRepositories( IRepositoryModel[] repositories )
    {
        reps = repositories == null ? EMPTY : repositories;
    }
}
