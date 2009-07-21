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

package org.apache.felix.sigil.ui.eclipse.ui.editors.project;


import java.io.File;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilPage;
import org.apache.felix.sigil.ui.eclipse.ui.form.SigilSection;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.forms.IManagedForm;


public abstract class AbstractResourceSection extends SigilSection implements IResourceChangeListener,
    ICheckStateListener
{

    protected static final Object[] EMPTY = new Object[]
        {};
    protected Tree tree;
    protected CheckboxTreeViewer viewer;
    private IWorkspace workspace;


    public AbstractResourceSection( SigilPage page, Composite parent, ISigilProjectModel project ) throws CoreException
    {
        super( page, parent, project );
    }


    @Override
    public void initialize( IManagedForm form )
    {
        super.initialize( form );
        viewer.setAllChecked( false );
        viewer.setGrayedElements( EMPTY );
        refreshSelections();
        viewer.refresh();
    }


    @Override
    public void refresh()
    {
        refreshSelections();
        super.refresh();
    }


    public void checkStateChanged( CheckStateChangedEvent event )
    {
        handleStateChanged( ( IResource ) event.getElement(), event.getChecked(), true, true );
    }


    public void resourceChanged( IResourceChangeEvent event )
    {
        if ( !getSection().getDisplay().isDisposed() )
        {
            getSection().getDisplay().asyncExec( new Runnable()
            {
                public void run()
                {
                    if ( getSection().isVisible() )
                    {
                        viewer.refresh();
                    }
                }
            } );
        }
    }


    protected void startWorkspaceListener( IWorkspace workspace )
    {
        this.workspace = workspace;
        workspace.addResourceChangeListener( this );
    }


    @Override
    public void dispose()
    {
        workspace.removeResourceChangeListener( this );
    }


    protected abstract void refreshSelections();


    protected abstract void syncResourceModel( IResource element, boolean checked );


    protected IResource findResource( IPath path )
    {
        IProject project2 = getProjectModel().getProject();
        File f = project2.getLocation().append( path ).toFile();

        if ( f.exists() )
        {
            try
            {
                if ( f.isFile() )
                {
                    return project2.getFile( path );
                }
                else
                {
                    return project2.getFolder( path );
                }
            }
            catch ( IllegalArgumentException e )
            {
                SigilCore.error( "Unknown path " + path );
                return null;
            }
        }
        else
        {
            SigilCore.error( "Unknown file " + f );
            return null;
        }
    }


    protected void handleStateChanged( IResource element, boolean checked, boolean recurse, boolean sync )
    {
        if ( element instanceof IContainer )
        {
            setParentsGrayChecked( element, checked );
            if ( recurse )
            {
                recursiveCheck( ( IContainer ) element, checked, sync );
            }
        }
        else
        {
            setParentsGrayChecked( element, checked );
        }

        if ( sync )
        {
            syncResourceModel( element, checked );
        }
    }


    private void recursiveCheck( IContainer element, final boolean checked, final boolean sync )
    {
        try
        {
            element.accept( new IResourceVisitor()
            {
                public boolean visit( IResource resource ) throws CoreException
                {
                    viewer.setChecked( resource, checked );
                    if ( sync )
                    {
                        syncResourceModel( resource, checked );
                    }
                    return true;
                }
            } );
        }
        catch ( CoreException e )
        {
            DebugPlugin.log( e.getStatus() );
        }
    }


    private void setParentsGrayChecked( IResource r, boolean checked )
    {
        while ( r.getParent() != null )
        {
            r = r.getParent();
            if ( ( viewer.getGrayed( r ) && viewer.getChecked( r ) ) == checked )
            {
                break;
            }
            if ( viewer.getChecked( r ) == checked )
            {
                viewer.setGrayed( r, !checked );
            }
            else
            {
                viewer.setGrayChecked( r, checked );
            }
        }
    }
}