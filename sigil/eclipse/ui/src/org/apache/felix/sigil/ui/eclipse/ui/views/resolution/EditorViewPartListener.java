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

package org.apache.felix.sigil.ui.eclipse.ui.views.resolution;


import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPartReference;


public class EditorViewPartListener implements IPartListener2
{

    private BundleResolverView bundleResolverView;


    public EditorViewPartListener( BundleResolverView bundleResolverView )
    {
        this.bundleResolverView = bundleResolverView;
    }


    public void partActivated( IWorkbenchPartReference partRef )
    {
        checkRef( partRef );
    }


    public void partBroughtToTop( IWorkbenchPartReference partRef )
    {
        // no action
    }


    public void partClosed( IWorkbenchPartReference partRef )
    {
        // no action
    }


    public void partDeactivated( IWorkbenchPartReference partRef )
    {
        // no action
    }


    public void partHidden( IWorkbenchPartReference partRef )
    {
        // no action
    }


    public void partInputChanged( IWorkbenchPartReference partRef )
    {
        // no action
    }


    public void partOpened( IWorkbenchPartReference partRef )
    {
        // no action
    }


    public void partVisible( IWorkbenchPartReference partRef )
    {
        // no action
    }


    private void checkRef( IWorkbenchPartReference partRef )
    {
        IEditorPart editor = partRef.getPage().getActiveEditor();
        if ( editor != null )
        {
            IEditorInput input = editor.getEditorInput();
            if ( input instanceof IFileEditorInput )
            {
                IFileEditorInput f = ( IFileEditorInput ) input;
                IProject project = f.getFile().getProject();
                try
                {
                    ISigilProjectModel model = SigilCore.create( project );
                    if ( model != null )
                    {
                        bundleResolverView.setInput( model );
                    }
                }
                catch ( CoreException e )
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    }

}
