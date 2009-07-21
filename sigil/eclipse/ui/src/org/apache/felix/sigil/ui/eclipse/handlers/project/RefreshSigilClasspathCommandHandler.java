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

package org.apache.felix.sigil.ui.eclipse.handlers.project;


import java.lang.reflect.InvocationTargetException;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.ui.eclipse.handlers.IResourceCommandHandler;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.actions.WorkspaceModifyOperation;


public class RefreshSigilClasspathCommandHandler implements IResourceCommandHandler
{

    public Object execute( IResource[] resources, ExecutionEvent event ) throws ExecutionException
    {
        try
        {
            for ( IResource res : resources )
            {
                IProject p = ( IProject ) res;
                final ISigilProjectModel model = SigilCore.create( p );

                WorkspaceModifyOperation op = new WorkspaceModifyOperation()
                {
                    @Override
                    protected void execute( IProgressMonitor monitor ) throws CoreException, InvocationTargetException,
                        InterruptedException
                    {
                        model.resetClasspath( monitor );
                    }
                };

                SigilUI.runWorkspaceOperation( op, null );
            }
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to create sigil project for refresh action", e );
        }
        return null;
    }

}
