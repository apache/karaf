package org.apache.felix.sigil.eclipse.internal.resources;

import java.util.LinkedList;

import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.model.ICapabilityModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

public class ProjectResourceListener implements IResourceChangeListener
{
    public static final int EVENT_MASKS = IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE;

    public void resourceChanged( IResourceChangeEvent event )
    {
        try
        {
            switch ( event.getType() ) {
                case IResourceChangeEvent.PRE_DELETE:
                    handlePreDelete(event);
                    break;
                case IResourceChangeEvent.POST_CHANGE:
                    handlePostChange(event);
                    break;
            }
        }
        catch (CoreException e)
        {
            SigilCore.error( "Failed to process resource change", e );
        }
    }

    private LinkedList<ICapabilityModelElement> capabilities = new LinkedList<ICapabilityModelElement>();
    
    private void handlePostChange(IResourceChangeEvent event) throws CoreException
    {
        IResourceDelta delta = event.getDelta();
        if ( delta != null )
        {
            delta.accept( new IResourceDeltaVisitor()
            {
                public boolean visit( IResourceDelta delta ) throws CoreException
                {
                    IResource resource = delta.getResource();
                    if ( resource instanceof IProject )
                    {
                        IProject project = ( IProject ) resource;
                        if ( SigilCore.isSigilProject( project ) )
                        {
                            switch ( delta.getKind() )
                            {
                                case IResourceDelta.REMOVED:
                                case IResourceDelta.ADDED:
                                    readCapabilities(project);
                                    break;
                            }
                        }
                        // Recurse no more
                        return false;
                    }
                    return true;
                }
            } );
            
            if( capabilities.size() > 0 ) {
                final LinkedList<ICapabilityModelElement> changes = new LinkedList<ICapabilityModelElement>(capabilities);
                capabilities.clear();
                
                Job job = new Job("Rebuild project dependencies")
                {
                    
                    @Override
                    protected IStatus run(IProgressMonitor monitor)
                    {
                        SigilCore.rebuildBundleDependencies(null, changes, monitor);
                        return Status.OK_STATUS;
                    }
                };
                job.setRule(ResourcesPlugin.getWorkspace().getRoot());
                job.schedule();
            }
        }
    }

    protected void handlePreDelete(IResourceChangeEvent event) throws CoreException
    {
        IResource resource = event.getResource();
        if ( resource instanceof IProject )
        {
            IProject project = ( IProject ) resource;
            if ( SigilCore.isSigilProject( project ) )
            {
                readCapabilities(project);
            }
        }
    }

    private void readCapabilities(IProject project) throws CoreException
    {
        ISigilProjectModel sigil = SigilCore.create(project);
        sigil.visit(new IModelWalker()
        { 
            public boolean visit(IModelElement element)
            {
                if ( element instanceof ICapabilityModelElement ) {
                    capabilities.add((ICapabilityModelElement) element);
                }
                return true;
            }
        });
    }


}
