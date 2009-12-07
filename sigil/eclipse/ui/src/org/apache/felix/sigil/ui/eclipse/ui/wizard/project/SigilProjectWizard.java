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

package org.apache.felix.sigil.ui.eclipse.ui.wizard.project;


import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.ui.eclipse.ui.wizard.SigilNewResourceWizard;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;


/**
 * @author dave
 *
 */
public class SigilProjectWizard extends SigilNewResourceWizard implements IExecutableExtension
{

    private SigilProjectWizardFirstPage firstPage;
    private SigilProjectWizardSecondPage secondPage;

    private String name;

    public static final IPath SIGIL_PROJECT_PATH = new Path( SigilCore.SIGIL_PROJECT_FILE );
    private IConfigurationElement config;


    public void init( IWorkbench workbench, IStructuredSelection currentSelection )
    {
        super.init( workbench, currentSelection );

        firstPage = new SigilProjectWizardFirstPage();
        firstPage.setInitialProjectName( name );
        secondPage = new SigilProjectWizardSecondPage( firstPage );

        addPage( firstPage );
        addPage( secondPage );
    }


    private void finishPage( IProgressMonitor monitor ) throws CoreException, InterruptedException
    {
        secondPage.performFinish( monitor );

        IProject newProject = firstPage.getProjectHandle();

        if ( newProject != null && newProject.exists() )
        {
            IFile file = newProject.getFile( SigilProjectWizard.SIGIL_PROJECT_PATH );

            selectRevealAndShow( file );

            // don't do this check for now - see FELIX-1924
//            new Job( "Check OSGi Install" )
//            {
//                @Override
//                protected IStatus run( IProgressMonitor monitor )
//                {
//                    // prompt for osgi home if not already set.
//                    SigilCore.getInstallManager().getDefaultInstall();
//                    return Status.OK_STATUS;
//                }
//            }.schedule();
        }
    }


    /* (non-Javadoc)
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish()
    {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();

        IWorkspaceRunnable op = new IWorkspaceRunnable()
        {
            public void run( IProgressMonitor monitor ) throws CoreException
            {
                try
                {
                    finishPage( monitor );
                }
                catch ( InterruptedException e )
                {
                    throw new OperationCanceledException( e.getMessage() );
                }
            }
        };

        try
        {
            workspace.run( op, Job.getJobManager().createProgressGroup() );
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to complete project wizard", e );
            return false;
        }

        BasicNewProjectResourceWizard.updatePerspective( config );
        return true;
    }


    public void setName( String name )
    {
        this.name = name;
    }


    public String getName()
    {
        return name;
    }


    @Override
    public boolean performCancel()
    {
        secondPage.performCancel();
        return super.performCancel();
    }


    public void setInitializationData( IConfigurationElement config, String propertyName, Object data )
        throws CoreException
    {
        this.config = config;
    }
}
