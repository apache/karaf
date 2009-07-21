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

package org.apache.felix.sigil.ui.eclipse.classpath;


import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.job.*;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;


public class SigilClasspathContainerInitializer extends ClasspathContainerInitializer
{

    public SigilClasspathContainerInitializer()
    {
        // TODO Auto-generated constructor stub
    }


    @Override
    public boolean canUpdateClasspathContainer( IPath containerPath, IJavaProject project )
    {
        return true;
    }


    @Override
    public void requestClasspathContainerUpdate( IPath containerPath, IJavaProject project,
        IClasspathContainer containerSuggestion ) throws CoreException
    {
        ISigilProjectModel sigil = SigilCore.create( project.getProject() );

        IClasspathContainer sigilContainer = new SigilClassPathContainer( sigil );

        IJavaProject[] affectedProjects = new IJavaProject[]
            { project };

        IClasspathContainer[] respectiveContainers = new IClasspathContainer[]
            { sigilContainer };

        IProgressMonitor monitor = ThreadProgressMonitor.getProgressMonitor();

        if ( monitor == null )
        {
            monitor = Job.getJobManager().createProgressGroup();
        }

        JavaCore.setClasspathContainer( containerPath, affectedProjects, respectiveContainers, monitor );
    }


    @Override
    public void initialize( IPath containerPath, IJavaProject project ) throws CoreException
    {
        ISigilProjectModel sigil = SigilCore.create( project.getProject() );

        IClasspathContainer sigilContainer = new SigilClassPathContainer( sigil );

        IJavaProject[] affectedProjects = new IJavaProject[]
            { project };

        IClasspathContainer[] respectiveContainers = new IClasspathContainer[]
            { sigilContainer };

        IProgressMonitor monitor = Job.getJobManager().createProgressGroup();

        JavaCore.setClasspathContainer( containerPath, affectedProjects, respectiveContainers, monitor );
    }

}
