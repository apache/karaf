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

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.jobs.Job;


public class ResolveProjectsJob extends Job
{
    final Collection<ISigilProjectModel> sigilProjects;
    
    public ResolveProjectsJob( IWorkspace workspace )
    {
        super( "Resolving Sigil projects" );
        setRule( workspace.getRoot() );
        sigilProjects = SigilCore.getRoot().getProjects();
    }


    public ResolveProjectsJob(ISigilProjectModel project)
    {
        super( "Resolving Sigil project" );
        setRule( project.getProject().getWorkspace().getRoot() );
        sigilProjects = Collections.singleton(project);
    }


    @Override
    protected IStatus run( IProgressMonitor monitor )
    {
        MultiStatus status = new MultiStatus( SigilCore.PLUGIN_ID, 0, "Error resolving Sigil projects", null );

        for ( ISigilProjectModel sigilProject : sigilProjects )
        {
            try
            {
                sigilProject.rebuildDependencies(monitor);
            }
            catch ( CoreException e )
            {
                status.add( e.getStatus() );
            }
        }

        return status;
    }
}
