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

package org.apache.felix.sigil.eclipse.internal.repository.eclipse;


import java.util.ArrayList;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.repository.AbstractBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;


public class WorkspaceRepository extends AbstractBundleRepository implements IResourceChangeListener
{

    private static final int UPDATE_MASK = IResourceDelta.CONTENT | IResourceDelta.DESCRIPTION | IResourceDelta.OPEN;
    private ISigilBundle[] bundles;


    public WorkspaceRepository( String id )
    {
        super( id );
    }


    @Override
    public void accept( IRepositoryVisitor visitor, int options )
    {
        synchronized ( this )
        {
            if ( bundles == null )
            {
                List<ISigilProjectModel> models = SigilCore.getRoot().getProjects();
                ArrayList<ISigilBundle> tmp = new ArrayList<ISigilBundle>( models.size() );
                for ( ISigilProjectModel n : models )
                {
                    ISigilBundle b = n.getBundle();
                    if ( b == null )
                    {
                        SigilCore.error( "No bundle found for project " + n.getProject().getName() );
                    }
                    else
                    {
                        tmp.add( b );
                    }
                }
                bundles = tmp.toArray( new ISigilBundle[tmp.size()] );
            }
        }

        for ( ISigilBundle b : bundles )
        {
            visitor.visit( b );
        }
    }


    public void refresh()
    {
        synchronized ( this )
        {
            bundles = null;
        }
    }


    @Override
    protected void notifyChange()
    {
        refresh();
        super.notifyChange();
    }


    public void resourceChanged( IResourceChangeEvent event )
    {
        try
        {
            event.getDelta().accept( new IResourceDeltaVisitor()
            {
                public boolean visit( IResourceDelta delta ) throws CoreException
                {
                    boolean result;

                    IResource resource = delta.getResource();
                    if ( resource instanceof IWorkspaceRoot )
                    {
                        result = true;
                    }
                    else if ( resource instanceof IProject )
                    {
                        IProject project = ( IProject ) resource;
                        if ( SigilCore.isSigilProject( project ) )
                        {
                            switch ( delta.getKind() )
                            {
                                case IResourceDelta.CHANGED:
                                    if ( ( delta.getFlags() & UPDATE_MASK ) == 0 )
                                    {
                                        break;
                                    }
                                    // else 
                                    // fall through on purpose
                                case IResourceDelta.ADDED: // fall through on purpose
                                case IResourceDelta.REMOVED: // fall through on purpose
                                    notifyChange();
                                    break;
                            }
                            result = true;
                        }
                        else
                        {
                            result = false;
                        }
                    }
                    else if ( resource.getName().equals( SigilCore.SIGIL_PROJECT_FILE ) )
                    {
                        switch ( delta.getKind() )
                        {
                            case IResourceDelta.CHANGED:
                            case IResourceDelta.ADDED:
                            case IResourceDelta.REMOVED:
                                notifyChange();
                        }
                        result = false;
                    }
                    else
                    {
                        result = false;
                    }
                    return result;
                }
            } );
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Workspace repository update failed", e );
        }
    }

}
