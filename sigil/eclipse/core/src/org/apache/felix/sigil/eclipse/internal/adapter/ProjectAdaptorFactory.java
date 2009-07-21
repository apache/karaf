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

package org.apache.felix.sigil.eclipse.internal.adapter;


import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterFactory;


/**
 * @author savage
 *
 */
public class ProjectAdaptorFactory implements IAdapterFactory
{

    private Class<?>[] types = new Class<?>[]
        { ISigilProjectModel.class };


    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapter(java.lang.Object, java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    public Object getAdapter( Object adaptableObject, Class adapterType )
    {
        Object adapted = null;

        IProject project = ( IProject ) adaptableObject;

        if ( ISigilProjectModel.class.equals( adapterType ) )
        {
            adapted = adaptProject( project );
        }

        return adapted;
    }


    private Object adaptProject( IProject project )
    {
        try
        {
            if ( SigilCore.isSigilProject( project ) )
            {
                return SigilCore.create( project );
            }
        }
        catch ( CoreException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    /* (non-Javadoc)
     * @see org.eclipse.core.runtime.IAdapterFactory#getAdapterList()
     */
    @SuppressWarnings("unchecked")
    public Class[] getAdapterList()
    {
        return types;
    }
}
