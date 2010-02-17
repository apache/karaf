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

package org.apache.felix.sigil.eclipse.job;


import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IBundleResolver;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.apache.felix.sigil.repository.ResolutionException;
import org.apache.felix.sigil.repository.ResolutionMonitorAdapter;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;


public class ResolveProjectsJob extends Job
{

    private final IWorkspace workspace;
    private final IProject project;


    public ResolveProjectsJob( IWorkspace workspace )
    {
        super( "Resolving Sigil projects" );
        this.workspace = workspace;
        this.project = null;
        setRule( ResourcesPlugin.getWorkspace().getRoot() );
    }


    public ResolveProjectsJob( IProject project )
    {
        super( "Resolving Sigil project" );
        this.workspace = null;
        this.project = project;
        setRule( project.getFile( SigilCore.SIGIL_PROJECT_FILE ) );
    }


    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        MultiStatus status = new MultiStatus( SigilCore.PLUGIN_ID, 0, "Error resolving Sigil projects", null );

        Collection<ISigilProjectModel> sigilProjects = null;

        if ( workspace != null )
        {
            sigilProjects = SigilCore.getRoot().getProjects();
        }
        else if ( project != null )
        {
            try
            {
                ISigilProjectModel sigilProject = SigilCore.create( project );
                sigilProjects = Collections.singleton( sigilProject );
            }
            catch ( CoreException e )
            {
                status.add( e.getStatus() );
            }
        }

        if ( sigilProjects != null )
        {
            for ( ISigilProjectModel sigilProject : sigilProjects )
            {
                try
                {
                    // Delete existing dependency markers on project
                    sigilProject.getProject().deleteMarkers( SigilCore.MARKER_UNRESOLVED_DEPENDENCY, true,
                        IResource.DEPTH_ONE );

                    IRepositoryManager repository = SigilCore.getRepositoryManager( sigilProject );
                    ResolutionMonitorAdapter resolutionMonitor = new ResolutionMonitorAdapter( monitor );

                    IBundleResolver resolver = repository.getBundleResolver();
                    ResolutionConfig config = new ResolutionConfig( ResolutionConfig.IGNORE_ERRORS );

                    // Execute resolver
                    IResolution resolution = resolver.resolve( sigilProject, config, resolutionMonitor );

                    // Find missing imports
                    Collection<IPackageImport> imports = sigilProject.getBundle().getBundleInfo().getImports();
                    for ( IPackageImport pkgImport : imports )
                    {
                        if ( resolution.getProvider( pkgImport ) == null )
                        {
                            markMissingImport( pkgImport, sigilProject.getProject() );
                        }
                    }

                    // Find missing required bundles
                    Collection<IRequiredBundle> requiredBundles = sigilProject.getBundle().getBundleInfo()
                        .getRequiredBundles();
                    for ( IRequiredBundle requiredBundle : requiredBundles )
                    {
                        if ( resolution.getProvider( requiredBundle ) == null )
                        {
                            markMissingRequiredBundle( requiredBundle, sigilProject.getProject() );
                        }
                    }
                }
                catch ( ResolutionException e )
                {
                    status.add( new Status( IStatus.ERROR, SigilCore.PLUGIN_ID, 0, "Error resolving project "
                        + sigilProject.getProject().getName(), e ) );
                }
                catch ( CoreException e )
                {
                    status.add( e.getStatus() );
                }
            }
        }

        return status;
    }


    private static void markMissingImport( IPackageImport pkgImport, IProject project ) throws CoreException
    {
        IMarker marker = project.getProject().createMarker( SigilCore.MARKER_UNRESOLVED_IMPORT_PACKAGE );
        marker.setAttribute( "element", pkgImport.getPackageName() );
        marker.setAttribute( "versionRange", pkgImport.getVersions().toString() );
        marker.setAttribute( IMarker.MESSAGE, "Cannot resolve imported package \"" + pkgImport.getPackageName()
            + "\" with version range " + pkgImport.getVersions() );
        marker.setAttribute( IMarker.SEVERITY, pkgImport.isOptional() ? IMarker.SEVERITY_WARNING
            : IMarker.SEVERITY_ERROR );
        marker.setAttribute( IMarker.PRIORITY, IMarker.PRIORITY_HIGH );
    }


    private static void markMissingRequiredBundle( IRequiredBundle req, IProject project ) throws CoreException
    {
        IMarker marker = project.getProject().createMarker( SigilCore.MARKER_UNRESOLVED_REQUIRE_BUNDLE );
        marker.setAttribute( "element", req.getSymbolicName() );
        marker.setAttribute( "versionRange", req.getVersions().toString() );
        marker.setAttribute( IMarker.MESSAGE, "Cannot resolve required bundle \"" + req.getSymbolicName()
            + "\" with version range " + req.getVersions() );
        marker.setAttribute( IMarker.SEVERITY, req.isOptional() ? IMarker.SEVERITY_WARNING : IMarker.SEVERITY_ERROR );
        marker.setAttribute( IMarker.PRIORITY, IMarker.PRIORITY_HIGH );
    }

}
