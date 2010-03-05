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


import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.job.ResolveProjectsJob;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.ui.eclipse.ui.SigilUI;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IFormPart;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.editor.IFormPage;


public class SigilProjectEditorPart extends FormEditor implements IResourceChangeListener
{

    private final Set<IModelElement> unresolvedElements = Collections.synchronizedSet( new HashSet<IModelElement>() );
    private ISigilProjectModel project;
    private ISigilProjectModel tempProject;
    private volatile boolean saving = false;
    private int dependenciesPageIndex;

    private Image errorImage = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);

    private PropertiesForm textPage;


    public IProject getProject()
    {
        IFileEditorInput fileInput = ( IFileEditorInput ) getEditorInput();
        return fileInput.getFile().getProject();
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave( IProgressMonitor monitor )
    {
        monitor.beginTask( "Saving", IProgressMonitor.UNKNOWN );
        try
        {
            saving = true;
            new ProgressMonitorDialog( getSite().getShell() ).run( true, true, new IRunnableWithProgress()
            {
                public void run( IProgressMonitor monitor ) throws InvocationTargetException,
                    InterruptedException
                {
                    try
                    {
                        if ( doInternalSave(monitor) ) {
                            project.setBundle( null );
                            tempProject.setBundle(null);
                            project.rebuildDependencies(monitor);
                            refreshAllPages();
                        }

                        monitor.done();
                    }
                    catch ( CoreException e )
                    {
                        throw new InvocationTargetException( e );
                    }
                }
            } );
        }
        catch ( InvocationTargetException e )
        {
            SigilCore.error( "Failed to save " + project, e.getTargetException() );
        }
        catch ( InterruptedException e )
        {
            monitor.setCanceled( true );
            return;
        }
        finally
        {
            saving = false;
        }
        monitor.done();
    }


    private boolean doInternalSave(final IProgressMonitor monitor) throws CoreException
    {
        if ( textPage.isDirty() )
        {
            SigilUI.runInUISync( new Runnable()
            {
                public void run()
                {
                    textPage.doSave( monitor );
                }
            } );
            return true;
        }
        else if ( isDirty() )
        {
            commitPages( true );
            tempProject.save( monitor, false );
            SigilUI.runInUISync( new Runnable()
            {
                public void run()
                {
                    textPage.setInput(getEditorInput());
                    editorDirtyStateChanged();
                }
            } );
            return true;
        }
        
        // ok no changes
        return false;
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.forms.editor.FormEditor#addPages()
     */
    @Override
    protected void addPages()
    {
        try
        {
            addPage( new OverviewForm( this, tempProject ) );
            addPage( new ContentsForm( this, tempProject ) );
            dependenciesPageIndex = addPage( new DependenciesForm( this, tempProject, unresolvedElements ) );
            addPage( new ExportsForm( this, tempProject ) );
            textPage = new PropertiesForm( this, tempProject );
            addPage( textPage, getEditorInput() );
            setPartName( project.getSymbolicName() );

            refreshTabImages();
        }
        catch ( PartInitException e )
        {
            SigilCore.error( "Failed to build " + this, e );
        }
    }


    protected void refreshTabImages()
    {
        if ( unresolvedElements.isEmpty() )
        {
            setPageImage( dependenciesPageIndex, null );
        }
        else
        {
            setPageImage( dependenciesPageIndex, errorImage );
        }
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs()
    {
        // save as not allowed
    }


    /* (non-Javadoc)
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed()
    {
        return false;
    }


    @Override
    public void dispose()
    {
        ResourcesPlugin.getWorkspace().removeResourceChangeListener( this );
        super.dispose();
    }


    public void resourceChanged( IResourceChangeEvent event )
    {
        try
        {
            switch (event.getType()) {
                case IResourceChangeEvent.PRE_REFRESH:
                    handleRefresh(event);
                    break;
                case IResourceChangeEvent.POST_BUILD:
                    handleBuild(event);
                    break;
                case IResourceChangeEvent.POST_CHANGE:
                    handleChange(event);
                    break;
            }
        }
        catch ( CoreException e )
        {
            ErrorDialog.openError( getSite().getShell(), "Error", null, e.getStatus() );
        }
    }


    private void handleBuild(IResourceChangeEvent event) throws CoreException
    {
        refreshView();
    }


    private void handleRefresh(IResourceChangeEvent event)
    {
        ResolveProjectsJob job = new ResolveProjectsJob(project);
        job.schedule();        
    }


    private void handleChange(IResourceChangeEvent event) throws CoreException
    {
        IResourceDelta delta = event.getDelta();
        final IFile editorFile = ( ( IFileEditorInput ) getEditorInput() ).getFile();
        delta.accept( new IResourceDeltaVisitor()
        {
            public boolean visit( IResourceDelta delta ) throws CoreException
            {
                int kind = delta.getKind();
                IResource resource = delta.getResource();
                if ( resource instanceof IProject )
                {
                    int flags = delta.getFlags();
                    return handleProjectChange(editorFile, (IProject) resource, kind, flags);
                }

                if ( resource instanceof IFile )
                {
                    handleFileChange(editorFile, (IFile) resource, kind);
                    // Recurse no more
                    return false;
                }

                return true;
            }
        } );
    }


    protected void handleFileChange(IFile editorFile, IFile affectedFile, int kind)
    {
        if ( affectedFile.equals( editorFile ) )
        {
            switch ( kind )
            {
                case IResourceDelta.REMOVED:
                    close( false );
                    break;
                case IResourceDelta.CHANGED:
                    if ( !saving )
                    {
                        reload();
                    }
                    SigilUI.runInUISync( new Runnable()
                    {
                        public void run()
                        {
                            setPartName( project.getSymbolicName() );
                        }
                    } );
                    break;
            }
        }
    }


    private boolean handleProjectChange(IResource editorFile, IProject project, int kind, int flags) throws CoreException
    {
        if ( !editorFile.getProject().equals( project ) )
        {
            return false;
        }
        if ( kind == IResourceDelta.CHANGED )
        {
            int mask = flags & (IResourceDelta.MARKERS);
            if ( mask > 0 ) {
                refreshView();
            }
        }
        return true;
    }


    private void refreshView() throws CoreException
    {
        loadUnresolvedDependencies();
        refreshAllPages();
    }


    protected void refreshAllPages()
    {
        Runnable op = new Runnable()
        {
            public void run()
            {
                for ( Iterator<?> iter = pages.iterator(); iter.hasNext(); )
                {
                    IFormPage page = ( IFormPage ) iter.next();
                    if ( page != null )
                    {
                        IManagedForm managedForm = page.getManagedForm();
                        if ( managedForm != null )
                        {
                            managedForm.refresh();
                            IFormPart[] parts = managedForm.getParts();
                            for ( IFormPart part : parts )
                            {
                                part.refresh();
                            }
                        }
                    }
                }
                firePropertyChange( IEditorPart.PROP_DIRTY );
                setPartName( project.getSymbolicName() );
                refreshTabImages();
            }
        };
        getSite().getShell().getDisplay().syncExec( op );
    }


    private void reload()
    {
        tempProject.setBundle(null);
        project.setBundle( null );
        refreshAllPages();
        ResolveProjectsJob job = new ResolveProjectsJob(project);
        job.schedule();        
    }


    @Override
    public void init( IEditorSite site, IEditorInput input ) throws PartInitException
    {
        super.init( site, input );

        try
        {
            this.project = SigilCore.create( getProject() );
            this.tempProject = (ISigilProjectModel) project.clone();
        }
        catch ( CoreException e )
        {
            throw new PartInitException( "Error creating Sigil project", e );
        }

        ResourcesPlugin.getWorkspace().addResourceChangeListener( this, IResourceChangeEvent.POST_CHANGE | IResourceChangeEvent.PRE_REFRESH );
                
        if ( input instanceof IFileEditorInput )
        {
            try
            {
                loadUnresolvedDependencies();
            }
            catch ( CoreException e )
            {
                throw new PartInitException( "Error retrieving dependency markers", e );
            }
        }
    }


    private void loadUnresolvedDependencies() throws CoreException
    {
        ModelElementFactory factory = ModelElementFactory.getInstance();
        IMarker[] markers = getProject()
            .findMarkers( SigilCore.MARKER_UNRESOLVED_DEPENDENCY, true, IResource.DEPTH_ONE );
        unresolvedElements.clear();

        for ( IMarker marker : markers )
        {
            String elementName = ( String ) marker.getAttribute( "element" );
            String versionRangeStr = ( String ) marker.getAttribute( "versionRange" );
            if ( elementName != null && versionRangeStr != null )
            {
                if ( marker.getType().equals( SigilCore.MARKER_UNRESOLVED_IMPORT_PACKAGE ) )
                {
                    IPackageImport pkgImport = factory.newModelElement( IPackageImport.class );
                    pkgImport.setPackageName( elementName );
                    pkgImport.setVersions( VersionRange.parseVersionRange( versionRangeStr ) );
                    unresolvedElements.add( pkgImport );
                }
                else if ( marker.getType().equals( SigilCore.MARKER_UNRESOLVED_REQUIRE_BUNDLE ) )
                {
                    IRequiredBundle req = factory.newModelElement( IRequiredBundle.class );
                    req.setSymbolicName( elementName );
                    req.setVersions( VersionRange.parseVersionRange( versionRangeStr ) );
                    unresolvedElements.add( req );
                }
            }
        }
    }
}
