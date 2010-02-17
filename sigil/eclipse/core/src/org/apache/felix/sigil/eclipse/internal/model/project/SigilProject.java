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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;

import org.apache.felix.sigil.config.BldFactory;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.job.ThreadProgressMonitor;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.model.AbstractCompoundModelElement;
import org.apache.felix.sigil.model.IModelElement;
import org.apache.felix.sigil.model.IModelWalker;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.apache.felix.sigil.repository.IResolution;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.apache.felix.sigil.repository.ResolutionException;
import org.apache.felix.sigil.repository.ResolutionMonitorAdapter;
import org.apache.felix.sigil.utils.GlobCompiler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.osgi.framework.Version;
import org.osgi.service.prefs.Preferences;


/**
 * @author dave
 *
 */
public class SigilProject extends AbstractCompoundModelElement implements ISigilProjectModel
{

    private static final long serialVersionUID = 1L;

    private IFile bldProjectFile;
    private IProject project;
    private IBldProject bldProject;

    private ISigilBundle bundle;

    private IEclipsePreferences preferences;


    public SigilProject()
    {
        super( "Sigil Project" );
    }


    public SigilProject( IProject project ) throws CoreException
    {
        this();
        this.project = project;
        bldProjectFile = project.getFile( new Path( SigilCore.SIGIL_PROJECT_FILE ) );
    }
    

    public void save( IProgressMonitor monitor ) throws CoreException
    {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );

        bldProjectFile.setContents( buildContents(), IFile.KEEP_HISTORY, progress.newChild( 10 ) );
        
        rebuildDependencies(progress.newChild(90));
    }

    public void rebuildDependencies(IProgressMonitor monitor) throws CoreException {
        SubMonitor progress = SubMonitor.convert( monitor, 100 );
        
        calculateUses();

        IRepositoryManager manager = SigilCore.getRepositoryManager( this );
        ResolutionConfig config = new ResolutionConfig( ResolutionConfig.INCLUDE_OPTIONAL );

        try
        {
            IResolution res = manager.getBundleResolver().resolve( this, config,
                new ResolutionMonitorAdapter( progress.newChild( 10 ) ) );
            if ( !res.isSynchronized() )
            {
                res.synchronize( progress.newChild( 60 ) );
            }
        }
        catch ( ResolutionException e )
        {
            throw SigilCore.newCoreException( "Failed to synchronize dependencies", e );
        }

        progress.setWorkRemaining( 30 );

        SigilCore.rebuildBundleDependencies( this, progress.newChild( 30 ) );
    }

    /**
     * Returns the project custom preference pool.
     * Project preferences may include custom encoding.
     * @return IEclipsePreferences or <code>null</code> if the project
     * 	does not have a java nature.
     */
    public Preferences getPreferences()
    {
        synchronized ( this )
        {
            if ( preferences == null )
            {
                preferences = loadPreferences();
            }

            return preferences;
        }
    }


    /**
     * @return
     */
    private synchronized IEclipsePreferences loadPreferences()
    {
        IScopeContext context = new ProjectScope( getProject() );
        final IEclipsePreferences eclipsePreferences = context.getNode( SigilCore.PLUGIN_ID );

        // Listen to node removal from parent in order to reset cache
        INodeChangeListener nodeListener = new IEclipsePreferences.INodeChangeListener()
        {
            public void added( IEclipsePreferences.NodeChangeEvent event )
            {
                // do nothing
            }


            public void removed( IEclipsePreferences.NodeChangeEvent event )
            {
                if ( event.getChild() == eclipsePreferences )
                {
                    synchronized ( SigilProject.this )
                    {
                        preferences = null;
                    }
                    ( ( IEclipsePreferences ) eclipsePreferences.parent() ).removeNodeChangeListener( this );
                }
            }
        };

        ( ( IEclipsePreferences ) eclipsePreferences.parent() ).addNodeChangeListener( nodeListener );

        return eclipsePreferences;
    }


    public Collection<IClasspathEntry> findExternalClasspath( IProgressMonitor monitor ) throws CoreException
    {
        return JavaHelper.resolveClasspathEntrys( this, monitor );
    }


    private void calculateUses()
    {
        visit( new IModelWalker()
        {
            public boolean visit( IModelElement element )
            {
                if ( element instanceof IPackageExport )
                {
                    IPackageExport pe = ( IPackageExport ) element;
                    try
                    {
                        pe.setUses( Arrays.asList( JavaHelper.findUses( pe.getPackageName(), SigilProject.this ) ) );
                    }
                    catch ( CoreException e )
                    {
                        SigilCore.error( "Failed to build uses list for " + pe, e );
                    }
                }
                return true;
            }
        } );
    }


    public Collection<ISigilProjectModel> findDependentProjects( IProgressMonitor monitor )
    {
        return SigilCore.getRoot().resolveDependentProjects( this, monitor );
    }


    public Version getVersion()
    {
        ISigilBundle bundle = getBundle();
        return bundle == null ? null : bundle.getBundleInfo() == null ? null : bundle.getBundleInfo().getVersion();
    }


    public String getSymbolicName()
    {
        ISigilBundle bundle = getBundle();
        return bundle == null ? null : bundle.getBundleInfo() == null ? null : bundle.getBundleInfo().getSymbolicName();
    }


    public IProject getProject()
    {
        return project;
    }


    public ISigilBundle getBundle()
    {
        ISigilBundle b = null;
        
        synchronized ( bldProjectFile )
        {
            if ( bundle == null )
            {
                try
                {
                    if ( bldProjectFile.getLocation().toFile().exists() )
                    {
                        bundle = parseContents( bldProjectFile );
                    }
                    else
                    {
                        bundle = setupDefaults();
                        NullProgressMonitor npm = new NullProgressMonitor();
                        bldProjectFile.create( buildContents(), true /* force */, npm );
                        project.refreshLocal( IResource.DEPTH_INFINITE, npm );
                    }
                }
                catch ( CoreException e )
                {
                    SigilCore.error( "Failed to build bundle", e );
                }
            }
            
            b = bundle;
        }
        
        return b;
    }


    public void setBundle( ISigilBundle bundle )
    {
        synchronized( bldProjectFile ) {
            this.bundle = bundle;
        }
    }


    public IJavaProject getJavaModel()
    {
        return JavaCore.create( project );
    }


    @Override
    public boolean equals( Object obj )
    {
        if ( obj == null )
            return false;

        if ( obj == this )
            return true;

        try
        {
            SigilProject p = ( SigilProject ) obj;
            return getSymbolicName().equals( p.getSymbolicName() )
                && ( getVersion() == null ? p.getVersion() == null : getVersion().equals( p.getVersion() ) );
        }
        catch ( ClassCastException e )
        {
            return false;
        }
    }


    @Override
    public int hashCode()
    {
        int hc = getSymbolicName().hashCode();
        if ( getVersion() != null ) {
            hc *= getVersion().hashCode();
        }
        hc *= 7;
        return hc;
    }


    @Override
    public String toString()
    {
        return "SigilProject[" + getSymbolicName() + ":" + getVersion() + "]";
    }


    public void resetClasspath( IProgressMonitor monitor ) throws CoreException
    {
        Path containerPath = new Path( SigilCore.CLASSPATH_CONTAINER_PATH );
        IJavaProject java = getJavaModel();
        ClasspathContainerInitializer init = JavaCore
            .getClasspathContainerInitializer( SigilCore.CLASSPATH_CONTAINER_PATH );
        ThreadProgressMonitor.setProgressMonitor( monitor );
        try
        {
            init.requestClasspathContainerUpdate( containerPath, java, null );
        }
        finally
        {
            ThreadProgressMonitor.setProgressMonitor( null );
        }
    }


    public IPath findBundleLocation() throws CoreException
    {
        IPath p = getBundle().getLocation();
        if ( p == null )
        {
            p = SigilCore.getDefault().findDefaultBundleLocation( this );
        }
        return p;
    }


    public IModelElement findImport( final String packageName, final IProgressMonitor monitor )
    {
        final IModelElement[] found = new IModelElement[1];

        visit( new IModelWalker()
        {
            public boolean visit( IModelElement element )
            {
                if ( element instanceof IPackageImport )
                {
                    IPackageImport pi = ( IPackageImport ) element;
                    if ( pi.getPackageName().equals( packageName ) )
                    {
                        found[0] = pi;
                        return false;
                    }
                }
                else if ( element instanceof IRequiredBundle )
                {
                    IRequiredBundle rb = ( IRequiredBundle ) element;
                    try
                    {
                        IRepositoryManager manager = SigilCore.getRepositoryManager( SigilProject.this );
                        ResolutionConfig config = new ResolutionConfig( ResolutionConfig.IGNORE_ERRORS );
                        IResolution res = manager.getBundleResolver().resolve( rb, config,
                            new ResolutionMonitorAdapter( monitor ) );
                        ISigilBundle b = res.getProvider( rb );
                        for ( IPackageExport pe : b.getBundleInfo().getExports() )
                        {
                            if ( pe.getPackageName().equals( packageName ) )
                            {
                                found[0] = rb;
                                return false;
                            }
                        }
                    }
                    catch ( ResolutionException e )
                    {
                        SigilCore.error( "Failed to resolve " + rb, e );
                    }
                }
                return true;
            }

        } );

        return found[0];
    }


    public boolean isInClasspath( String packageName, IProgressMonitor monitor ) throws CoreException
    {
        if ( findImport( packageName, monitor ) != null )
        {
            return true;
        }

        for ( String path : getBundle().getClasspathEntrys() )
        {
            IClasspathEntry cp = getJavaModel().decodeClasspathEntry( path );
            for ( IPackageFragmentRoot root : getJavaModel().findPackageFragmentRoots( cp ) )
            {
                if ( findPackage( packageName, root ) )
                {
                    return true;
                }
            }
        }
        return false;
    }


    public boolean isInClasspath( ISigilBundle bundle )
    {
        for ( String path : getBundle().getClasspathEntrys() )
        {
            IClasspathEntry cp = getJavaModel().decodeClasspathEntry( path );
            switch ( cp.getEntryKind() )
            {
                case IClasspathEntry.CPE_PROJECT:
                    ISigilProjectModel p = bundle.getAncestor( ISigilProjectModel.class );
                    return p != null && cp.getPath().equals( p.getProject().getFullPath() );
                case IClasspathEntry.CPE_LIBRARY:
                    return cp.getPath().equals( bundle.getLocation() );
            }
        }

        return false;
    }


    private boolean findPackage( String packageName, IParent parent ) throws JavaModelException
    {
        for ( IJavaElement e : parent.getChildren() )
        {
            if ( e.getElementType() == IJavaElement.PACKAGE_FRAGMENT )
            {
                return e.getElementName().equals( packageName );
            }

            if ( e instanceof IParent )
            {
                if ( findPackage( packageName, ( IParent ) e ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    private ISigilBundle setupDefaults()
    {
        ISigilBundle bundle = ModelElementFactory.getInstance().newModelElement( ISigilBundle.class );
        IBundleModelElement info = ModelElementFactory.getInstance().newModelElement( IBundleModelElement.class );
        info.setSymbolicName( project.getName() );
        bundle.setBundleInfo( info );
        bundle.setParent( this );
        return bundle;
    }


    private ISigilBundle parseContents( IFile projectFile ) throws CoreException
    {
        /*if ( !projectFile.isSynchronized(IResource.DEPTH_ONE) ) {
        	projectFile.refreshLocal(IResource.DEPTH_ONE, new NullProgressMonitor());
        }*/

        if ( projectFile.getName().equals( SigilCore.SIGIL_PROJECT_FILE ) )
        {
            return parseBldContents( projectFile.getLocationURI() );
        }
        else
        {
            throw SigilCore.newCoreException( "Unexpected project file: " + projectFile.getName(), null );
        }
    }


    private ISigilBundle parseBldContents( URI uri ) throws CoreException
    {
        try
        {
            bldProject = BldFactory.getProject( uri, true );
            ISigilBundle bundle = bldProject.getDefaultBundle();
            
            if ( bundle == null ) {
                throw SigilCore.newCoreException("No default bundle", null);
            }
            
            bundle.setParent( this );
            return bundle;
        }
        catch ( IOException e )
        {
            throw SigilCore.newCoreException( "Failed to parse " + uri, e );
        }
    }


    private InputStream buildContents() throws CoreException
    {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try
        {
            if ( bldProject == null )
            {
                bldProject = BldFactory.newProject( bldProjectFile.getLocationURI(), null );
            }
            bldProject.setDefaultBundle( getBundle() );
            bldProject.saveTo( buf );
        }
        catch ( IOException e )
        {
            throw SigilCore.newCoreException( "Failed to save project file", e );
        }
        return new ByteArrayInputStream( buf.toByteArray() );
    }


    public String getName()
    {
        return getProject().getName();
    }


    public IPath findOutputLocation() throws CoreException
    {
        return getProject().getLocation().append( getJavaModel().getOutputLocation().removeFirstSegments( 1 ) );
    }


    public IBldProject getBldProject() throws CoreException
    {
        try
        {
            return BldFactory.getProject( project.getFile( IBldProject.PROJECT_FILE ).getLocationURI() );
        }
        catch ( IOException e )
        {
            throw SigilCore.newCoreException( "Failed to get project file: ", e );
        }
    }


    public boolean isInBundleClasspath( IPackageFragment root ) throws JavaModelException
    {
        if ( getBundle().getClasspathEntrys().isEmpty() ) {
            for ( String p : getBundle().getPackages() ) {
                SigilCore.log("Checking " + p + "->" + root.getElementName() );
                Matcher m = GlobCompiler.compile(p).matcher(root.getElementName());
                if ( m.matches() ) {
                    return true;
                }
            }
            return false;
        }
        else {
            IPackageFragmentRoot parent = (IPackageFragmentRoot) root.getParent();
            String enc = getJavaModel().encodeClasspathEntry( parent.getRawClasspathEntry() );
            return getBundle().getClasspathEntrys().contains( enc.trim() );
        }
    }
}
