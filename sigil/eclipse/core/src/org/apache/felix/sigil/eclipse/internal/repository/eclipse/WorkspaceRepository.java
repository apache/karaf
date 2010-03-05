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


import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.repository.AbstractBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;

public class WorkspaceRepository extends AbstractBundleRepository implements IResourceChangeListener
{

    private static final int UPDATE_MASK = IResourceDelta.CONTENT | IResourceDelta.OPEN;
    
    static final int EVENT_MASKS = IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_REFRESH;

    public WorkspaceRepository( String id )
    {
        super( id );
    }    
      
    
    @Override
    public void accept( IRepositoryVisitor visitor, int options )
    {
        List<ISigilProjectModel> models = SigilCore.getRoot().getProjects();
        for ( ISigilProjectModel project : models )
        {
            ISigilBundle b = project.getBundle();
            if ( b == null )
            {
                SigilCore.error( "No bundle found for project " + project.getProject().getName() );
            }
            else
            {
                if ( (options & ResolutionConfig.COMPILE_TIME) != 0 ) {
                    b = compileTimeFilter(project, b);
                }
                visitor.visit( b );
            }
        }
    }


    private ISigilBundle compileTimeFilter(ISigilProjectModel project, ISigilBundle bundle)
    {
        bundle = (ISigilBundle) bundle.clone();

        Collection<String> packages = findPackages(project);
        
        for ( IPackageExport pe : bundle.getBundleInfo().getExports() ) {
            final String packagePath = pe.getPackageName().replace('.', '/');
            if ( !packages.contains(packagePath) ) {
                bundle.getBundleInfo().removeExport(pe);
            }
        }
        
        return bundle;
    }



    private Collection<String> findPackages(ISigilProjectModel project)
    {
        final IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
        final IContentType javaContentType = contentTypeManager.getContentType("org.eclipse.jdt.core.javaSource");
        final HashSet<String> packages = new HashSet<String>();
        
        try
        {
            project.getProject().accept(new IResourceVisitor()
            {
                public boolean visit(IResource resource) throws CoreException
                {
                    if ( resource instanceof IFile ) {
                        IFile f = (IFile) resource;
                        IContentType ct = contentTypeManager.findContentTypeFor(f.getName());
                        if ( ct != null && ct.isKindOf(javaContentType) ) {
                            IPath p = f.getProjectRelativePath();
                            p = p.removeLastSegments(1);
                            p = p.removeFirstSegments(1);
                            packages.add( p.toString() );
                        }
                    }
                    
                    return true;
                }
            });
        }
        catch (CoreException e)
        {
            SigilCore.error( "Failed to read packages for " + project.getProject().getName() );
        }
        
        return packages;
     }



    public void refresh()
    {
        // no action
        // TODO this method is used to prompt repository to update caches - 
        // however caches are complex to maintain in this workspace as the bundle is actively being developed...
        // potential performance improvement in future?
    }

    public void resourceChanged( IResourceChangeEvent event )
    {
        try
        {
            switch (event.getType()) {
                case IResourceChangeEvent.PRE_DELETE:
                    handleDelete(event);
                    break;
                case IResourceChangeEvent.PRE_REFRESH:
                    handleRefresh(event);
                    break;
                case IResourceChangeEvent.POST_CHANGE:
                    handleChange(event);
                    break;
            }
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Workspace repository update failed", e );
        }
    }

    private HashSet<IProject> deleted = new HashSet<IProject>();

    private void handleDelete(IResourceChangeEvent event)
    {
        if ( event.getResource() instanceof IProject ) {
            IProject project = (IProject) event.getResource();
            if ( isSigilProject(project) )
            {
                deleted.add(project);
            }
        }
    }


    private void handleRefresh(IResourceChangeEvent event)
    {
        SigilCore.log("Refreshing workspace repository");
        notifyChange();
    }


    private void handleChange(IResourceChangeEvent event) throws CoreException
    {
        final boolean[] notify = new boolean[1];
        
        event.getDelta().accept( new IResourceDeltaVisitor()
        {
            public boolean visit( IResourceDelta delta ) throws CoreException
            {
                boolean checkMembers = true;

                IResource resource = delta.getResource();
                if ( resource instanceof IProject )
                {
                    IProject project = ( IProject ) resource;
                    if ( isSigilProject(project) )
                    {
                        switch ( delta.getKind() )
                        {
                            case IResourceDelta.CHANGED:
                                int flag = delta.getFlags();
                                if ( ( flag & UPDATE_MASK ) == 0 )
                                {
                                    break;
                                }
                                // else 
                                // fall through on purpose
                            case IResourceDelta.ADDED: // fall through on purpose
                            case IResourceDelta.REMOVED: // fall through on purpose
                                notify[0] = true;
                                break;
                        }
                        checkMembers = true;
                    }
                    else
                    {
                        checkMembers = false;
                    }
                }
                else if ( resource.getName().equals( SigilCore.SIGIL_PROJECT_FILE ) )
                {
                    switch ( delta.getKind() )
                    {
                        case IResourceDelta.CHANGED:
                        case IResourceDelta.ADDED:
                        case IResourceDelta.REMOVED:
                            notify[0] = true;
                    }
                    checkMembers = false;
                }
                return checkMembers && !notify[0];
            }
        } );
        
        if (notify[0]) {
            notifyChange();
        }
    }


    private boolean isSigilProject(IProject project)
    {
        return SigilCore.isSigilProject( project ) || deleted.remove(project);
    }

}
