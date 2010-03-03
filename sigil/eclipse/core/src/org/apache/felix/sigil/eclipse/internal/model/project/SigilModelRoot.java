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

package org.apache.felix.sigil.eclipse.internal.model.project;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilModelRoot;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ICapabilityModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.IRequirementModelElement;
import org.apache.felix.sigil.model.eclipse.ILibrary;
import org.apache.felix.sigil.model.eclipse.ILibraryImport;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.repository.IBundleResolver;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.apache.felix.sigil.repository.ResolutionException;
import org.apache.felix.sigil.repository.ResolutionMonitorAdapter;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;


public class SigilModelRoot implements ISigilModelRoot
{
    public List<ISigilProjectModel> getProjects()
    {
        IProject[] all = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        ArrayList<ISigilProjectModel> projects = new ArrayList<ISigilProjectModel>( all.length );
        for ( IProject p : all )
        {
            try
            {
                if ( p.isOpen() && p.hasNature( SigilCore.NATURE_ID ) )
                {
                    ISigilProjectModel n = SigilCore.create( p );
                    projects.add( n );
                }
            }
            catch ( CoreException e )
            {
                SigilCore.error( "Failed to build model element", e );
            }
        }

        return projects;
    }


    public Set<ISigilProjectModel> resolveDependentProjects( Collection<ICapabilityModelElement> caps, IProgressMonitor monitor )
    {
        final HashSet<ISigilProjectModel> dependents = new HashSet<ISigilProjectModel>();

        for ( final ISigilProjectModel n : getProjects() )
        {
            for (final ICapabilityModelElement cap : caps ) {
                final ISigilProjectModel sigil = cap.getAncestor(ISigilProjectModel.class);
                
                n.visit(new IModelWalker()
                {
                    public boolean visit(IModelElement element)
                    {
                        if ( element instanceof IRequirementModelElement ) {
                            IRequirementModelElement req = (IRequirementModelElement) element;
                            if ( req.accepts(cap) ) {
                                dependents.add(n);
                                return false;
                            }
                        }
                        else if ( element instanceof ILibraryImport ) {
                            ILibraryImport l = (ILibraryImport) element;
                            ILibrary lib = SigilCore.getRepositoryManager( sigil ).resolveLibrary( l );

                            if ( lib != null )
                            {
                                for ( IPackageImport i : lib.getImports() )
                                {
                                    if ( i.accepts(cap))
                                    {
                                        dependents.add( n );
                                    }
                                }
                            }
                            else
                            {
                                SigilCore.error( "No library found for " + l );
                            }
                        }
                        return true;
                    }
                });
            }
//            if ( !sigil.equals( n ) )
//            {
//                for ( IPackageExport pe : sigil.getBundle().getBundleInfo().getExports() )
//                {
//                    for ( IPackageImport i : n.getBundle().getBundleInfo().getImports() )
//                    {
//                        if ( pe.getPackageName().equals( i.getPackageName() )
//                            && i.getVersions().contains( pe.getVersion() ) )
//                        {
//                            dependents.add( n );
//                        }
//                    }
//
//                    for ( ILibraryImport l : n.getBundle().getBundleInfo().getLibraryImports() )
//                    {
//                        ILibrary lib = SigilCore.getRepositoryManager( sigil ).resolveLibrary( l );
//
//                        if ( lib != null )
//                        {
//                            for ( IPackageImport i : lib.getImports() )
//                            {
//                                if ( pe.getPackageName().equals( i.getPackageName() )
//                                    && i.getVersions().contains( pe.getVersion() ) )
//                                {
//                                    dependents.add( n );
//                                }
//                            }
//                        }
//                        else
//                        {
//                            SigilCore.error( "No library found for " + l );
//                        }
//                    }
//                }
//
//                for ( IRequiredBundle r : n.getBundle().getBundleInfo().getRequiredBundles() )
//                {
//                    if ( sigil.getSymbolicName().equals( r.getSymbolicName() )
//                        && r.getVersions().contains( sigil.getVersion() ) )
//                    {
//                        dependents.add( n );
//                    }
//                }
//            }
        }

        return dependents;
    }


    public Collection<ISigilBundle> resolveBundles( ISigilProjectModel sigil, IModelElement element,
        boolean includeOptional, IProgressMonitor monitor ) throws CoreException
    {
        int options = ResolutionConfig.INCLUDE_DEPENDENTS;
        if ( includeOptional )
        {
            options |= ResolutionConfig.INCLUDE_OPTIONAL;
        }

        ResolutionConfig config = new ResolutionConfig( options );
        try
        {
            IBundleResolver resolver = SigilCore.getRepositoryManager( sigil ).getBundleResolver();
            IResolution resolution = resolver.resolve( element, config, new ResolutionMonitorAdapter( monitor ) );
            resolution.synchronize( monitor );
            return resolution.getBundles();
        }
        catch ( ResolutionException e )
        {
            throw SigilCore.newCoreException( e.getMessage(), e );
        }
    }
}
