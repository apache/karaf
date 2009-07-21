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

package org.apache.felix.sigil.ui.eclipse.actions;


import java.util.Collection;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.apache.felix.sigil.ui.eclipse.ui.util.ResourceReviewDialog;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.progress.IProgressService;


public class PruneProjectDependenciesAction extends DisplayAction
{

    private ISigilProjectModel project;


    public PruneProjectDependenciesAction( ISigilProjectModel project )
    {
        this.project = project;
    }


    @Override
    public void run()
    {
        final Shell shell = findDisplay().getActiveShell();

        Job job = new Job( "Resolving imports" )
        {

            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                Collection<IModelElement> unused = JavaHelper.findUnusedReferences( project, monitor );

                if ( unused.isEmpty() )
                {
                    info( shell, "No unused references found" );
                }
                else
                {
                    final ResourceReviewDialog<IModelElement> dialog = new ResourceReviewDialog<IModelElement>( shell,
                        "Review Unused Imports", unused );

                    shell.getDisplay().asyncExec( new Runnable()
                    {
                        public void run()
                        {
                            if ( dialog.open() == Window.OK )
                            {
                                WorkspaceModifyOperation op = new WorkspaceModifyOperation()
                                {
                                    @Override
                                    protected void execute( IProgressMonitor monitor ) throws CoreException
                                    {
                                        for ( IModelElement e : dialog.getResources() )
                                        {
                                            if ( !project.getBundle().getBundleInfo().removeChild( e ) )
                                            {
                                                SigilCore.error( "Failed to remove " + e );
                                            }
                                        }

                                        project.save( monitor );
                                    }
                                };

                                SigilUI.runWorkspaceOperation( op, shell );
                            }
                        }
                    } );
                }

                return Status.OK_STATUS;
            }
        };

        job.schedule();

        IProgressService p = PlatformUI.getWorkbench().getProgressService();
        p.showInDialog( shell, job );
    }
}
