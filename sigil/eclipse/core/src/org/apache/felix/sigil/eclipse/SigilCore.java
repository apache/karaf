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

package org.apache.felix.sigil.eclipse;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.eclipse.install.IOSGiInstallManager;
import org.apache.felix.sigil.eclipse.internal.install.OSGiInstallManager;
import org.apache.felix.sigil.eclipse.internal.model.project.SigilModelRoot;
import org.apache.felix.sigil.eclipse.internal.model.project.SigilProject;
import org.apache.felix.sigil.eclipse.internal.model.repository.RepositoryConfiguration;
import org.apache.felix.sigil.eclipse.internal.repository.eclipse.GlobalRepositoryManager;
import org.apache.felix.sigil.eclipse.internal.repository.eclipse.RepositoryMap;
import org.apache.felix.sigil.eclipse.internal.repository.eclipse.SigilRepositoryManager;
import org.apache.felix.sigil.eclipse.internal.resources.ProjectResourceListener;
import org.apache.felix.sigil.eclipse.internal.resources.SigilProjectManager;
import org.apache.felix.sigil.eclipse.model.project.ISigilModelRoot;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.repository.IRepositoryConfiguration;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.model.ICapabilityModelElement;
import org.apache.felix.sigil.model.ModelElementFactory;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryManager;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.apache.felix.sigil.repository.ResolutionConfig;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The activator class controls the plug-in life cycle
 */
public class SigilCore extends AbstractUIPlugin
{

    private static final String BASE = "org.apache.felix.sigil";
    // The plug-in ID
    public static final String PLUGIN_ID = BASE + ".eclipse.core";
    public static final String NATURE_ID = BASE + ".sigilnature";
    public static final String PREFERENCES_ID = BASE + ".ui.preferences.SigilPreferencePage";
    public static final String OSGI_INSTALLS_PREFERENCES_ID = BASE + ".ui.preferences.osgiInstalls";
    public static final String EXCLUDED_RESOURCES_PREFERENCES_ID = BASE + ".ui.preferences.excludedResources";
    public static final String REPOSITORIES_PREFERENCES_ID = BASE + ".ui.preferences.repositoriesPreferencePage";
    public static final String SIGIL_PROJECT_FILE = IBldProject.PROJECT_FILE;
    public static final String BUILDER_ID = PLUGIN_ID + ".sigilBuilder";
    public static final String CLASSPATH_CONTAINER_PATH = BASE + ".classpathContainer";

    public static final String OSGI_INSTALLS = BASE + ".osgi.installs";
    public static final String OSGI_DEFAULT_INSTALL_ID = BASE + ".osgi.default.install.id";
    public static final String OSGI_INSTALL_PREFIX = BASE + ".osgi.install.";
    public static final String OSGI_SOURCE_LOCATION = BASE + ".osgi.source.location";
    public static final String OSGI_INSTALL_CHECK_PREFERENCE = BASE + ".osgi.install.check";
    public static final String LIBRARY_KEYS_PREF = BASE + ".library.keys";
    public static final String PREFERENCES_REBUILD_PROJECTS = BASE + ".rebuild.projects";
    public static final String QUALIFY_VERSIONS = BASE + ".qualify.versions";

    public static final String DEFAULT_VERSION_LOWER_BOUND = BASE + ".versionLowerBound";
    public static final String DEFAULT_VERSION_UPPER_BOUND = BASE + ".versionUpperBound";

    public static final String DEFAULT_EXCLUDED_RESOURCES = BASE + ".excludedResources";
    public static final String PREFERENCES_NOASK_OSGI_INSTALL = BASE + ".noAskOSGIHome";
    public static final String PREFERENCES_ADD_IMPORT_FOR_EXPORT = BASE + ".addImportForExport";
    public static final String INCLUDE_OPTIONAL_DEPENDENCIES = BASE + ".includeOptionalDependencies";

    public static final String INSTALL_BUILDER_EXTENSION_POINT_ID = BASE + ".installbuilder";
    public static final String REPOSITORY_PROVIDER_EXTENSION_POINT_ID = BASE + ".repositoryprovider";

    public static final String MARKER_UNRESOLVED_DEPENDENCY = BASE + ".unresolvedDependencyMarker";
    public static final String MARKER_UNRESOLVED_IMPORT_PACKAGE = BASE + ".unresolvedDependencyMarker.importPackage";
    public static final String MARKER_UNRESOLVED_REQUIRE_BUNDLE = BASE + ".unresolvedDependencyMarker.requireBundle";
    public static final String REPOSITORY_SET = PLUGIN_ID + ".repository.set";
    public static final String PREFERENCES_INCLUDE_OPTIONAL = PLUGIN_ID + ".include.optional";

    private static final Object NULL = new Object();

    // The shared instance
    private static SigilCore plugin;

    private ServiceTracker descriptorTracker;
    private ServiceTracker registryTracker;
    private ServiceTracker serializerTracker;
    private static IRepositoryConfiguration repositoryConfig;
    private static SigilProjectManager projectManager;
    private static OSGiInstallManager installs;
    private static ISigilModelRoot modelRoot;
    private static HashMap<Object, SigilRepositoryManager> repositoryManagers = new HashMap<Object, SigilRepositoryManager>();
    private static GlobalRepositoryManager globalRepositoryManager;
    private static RepositoryMap repositoryMap;

    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static SigilCore getDefault()
    {
        return plugin;
    }


    public static CoreException newCoreException( String msg, Throwable t )
    {
        return new CoreException( makeStatus( msg, t, IStatus.ERROR ) );
    }


    public static void log( String msg )
    {
        DebugPlugin.log( makeStatus( msg, null, IStatus.INFO ) );
    }


    public static void error( String msg )
    {
        DebugPlugin.log( makeStatus( msg, null, IStatus.ERROR ) );
    }


    public static void error( String msg, Throwable t )
    {
        DebugPlugin.log( makeStatus( msg, t, IStatus.ERROR ) );
    }


    public static void warn( String msg )
    {
        DebugPlugin.log( makeStatus( msg, null, IStatus.WARNING ) );
    }


    public static void warn( String msg, Throwable t )
    {
        DebugPlugin.log( makeStatus( msg, t, IStatus.WARNING ) );
    }


    private static IStatus makeStatus( String msg, Throwable t, int status )
    {
        if ( t instanceof CoreException )
        {
            CoreException c = ( CoreException ) t;
            return c.getStatus();
        }
        else
        {
            return new Status( status, SigilCore.PLUGIN_ID, status, msg, t );
        }
    }


    public static boolean isSigilProject( IProject resource )
    {
        if ( resource == null )
            return false;

        if ( resource.isAccessible() && resource instanceof IProject )
        {
            IProject project = ( IProject ) resource;
            try
            {
                return project.hasNature( NATURE_ID );
            }
            catch ( CoreException e )
            {
                error( e.getMessage(), e );
                return false;
            }
        }
        else
        {
            return false;
        }
    }


    public static boolean hasProjectNature( IProject project ) throws CoreException
    {
        return project.getNature( NATURE_ID ) != null;
    }


    public static ResourceBundle getResourceBundle()
    {
        return ResourceBundle.getBundle( "resources." + SigilCore.class.getName(), Locale.getDefault(), SigilCore.class
            .getClassLoader() );
    }


    public static ISigilProjectModel create( IProject project ) throws CoreException
    {
        return projectManager.getSigilProject(project);
    }


    /**
     * The constructor
     */
    public SigilCore()
    {
        plugin = this;
    }


    public void earlyStartup()
    {
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext
     * )
     */
    public void start( final BundleContext context ) throws Exception
    {
        super.start( context );

        modelRoot = new SigilModelRoot();

        repositoryConfig = new RepositoryConfiguration();

        installs = new OSGiInstallManager();

        repositoryMap = new RepositoryMap();
        globalRepositoryManager = new GlobalRepositoryManager(repositoryMap);
        globalRepositoryManager.initialise();
        
        projectManager = new SigilProjectManager();

        registerModelElements( context );
        registerResourceListeners();
    }


    /*
     * (non-Javadoc)
     * 
     * @see
     * org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext
     * )
     */
    public void stop( BundleContext context ) throws Exception
    {
        if ( descriptorTracker != null )
        {
            descriptorTracker.close();
            descriptorTracker = null;
        }

        if ( registryTracker != null )
        {
            registryTracker.close();
            registryTracker = null;
        }

        if ( serializerTracker != null )
        {
            serializerTracker.close();
            serializerTracker = null;
        }

        for ( SigilRepositoryManager m : repositoryManagers.values() )
        {
            m.destroy();
        }

        repositoryManagers.clear();

        globalRepositoryManager.destroy();
        globalRepositoryManager = null;

        plugin = null;

        super.stop( context );
    }


    public static boolean isBundledPath( String bp ) throws CoreException
    {
        boolean bundle = JavaHelper.isCachedBundle( bp );

        if ( !bundle )
        {
            bundle = isProjectPath( bp );

            if ( !bundle )
            {
                for ( IBundleRepository r : getGlobalRepositoryManager().getRepositories() )
                {
                    bundle = isBundlePath( bp, r );
                    if ( bundle )
                        break;
                }
            }
        }

        return bundle;
    }


    private static boolean isBundlePath( final String bp, IBundleRepository r )
    {
        final AtomicBoolean flag = new AtomicBoolean();

        IRepositoryVisitor visitor = new IRepositoryVisitor()
        {
            public boolean visit( ISigilBundle b )
            {
                IPath path = b.getLocation();
                if ( path != null && path.toOSString().equals( bp ) )
                {
                    flag.set( true );
                    return false;
                }
                else
                {
                    return true;
                }
            }

        };

        r.accept( visitor, ResolutionConfig.INDEXED_ONLY | ResolutionConfig.LOCAL_ONLY );

        return flag.get();
    }


    private static boolean isProjectPath( String bp ) throws CoreException
    {
        for ( ISigilProjectModel p : SigilCore.getRoot().getProjects() )
        {
            IPath path = p.findOutputLocation();

            if ( path.toOSString().equals( bp ) )
            {
                return true;
            }
        }

        return false;
    }


    private void registerResourceListeners()
    {
        Job job = new Job( "Initialising sigil resource listeners" )
        {
            @Override
            protected IStatus run( IProgressMonitor monitor )
            {
                ResourcesPlugin.getWorkspace().addResourceChangeListener( new ProjectResourceListener(projectManager),
                    ProjectResourceListener.EVENT_MASKS );
                return Status.OK_STATUS;
            }
        };
        job.setSystem( true );
        job.schedule();
    }


    private void registerModelElements( BundleContext context )
    {
        // trick to get eclipse to lazy load BldCore for model elements
        BldCore.getLicenseManager();
        ModelElementFactory.getInstance().register( ISigilProjectModel.class, SigilProject.class, "project", "sigil", null );
    }


    public static IOSGiInstallManager getInstallManager()
    {
        return installs;
    }


    public static ISigilModelRoot getRoot()
    {
        return modelRoot;
    }


    public static IRepositoryManager getGlobalRepositoryManager()
    {
        return globalRepositoryManager;
    }


    public static IRepositoryManager getRepositoryManager( String set )
    {
        SigilRepositoryManager manager = null;

        if ( set == null )
        {
            manager = repositoryManagers.get( NULL );
            if ( manager == null )
            {
                manager = new SigilRepositoryManager( null, repositoryMap );
                manager.initialise();
                repositoryManagers.put( NULL, manager );
            }
        }
        else
        {
            manager = repositoryManagers.get( set );

            if ( manager == null )
            {
                manager = new SigilRepositoryManager( set, repositoryMap );
                manager.initialise();
                repositoryManagers.put( set, manager );
            }
        }

        return manager;
    }


    public static IRepositoryManager getRepositoryManager( ISigilProjectModel model )
    {
        return getRepositoryManager( loadProjectRepositorySet( model ) );
    }


    private static String loadProjectRepositorySet( ISigilProjectModel model )
    {
        if ( model == null )
        {
            return null;
        }

        return model.getPreferences().get( REPOSITORY_SET, null );
    }


    public static IRepositoryConfiguration getRepositoryConfiguration()
    {
        return repositoryConfig;
    }


    public static void rebuildAllBundleDependencies( IProgressMonitor monitor )
    {
        Collection<ISigilProjectModel> projects = getRoot().getProjects();
        if ( !projects.isEmpty() )
        {
            SubMonitor progress = SubMonitor.convert( monitor, projects.size() * 20 );
            for ( ISigilProjectModel p : projects )
            {
                rebuild( p, progress );
            }
        }

        monitor.done();
    }


    public static void rebuildBundleDependencies( ISigilProjectModel project, Collection<ICapabilityModelElement> caps, IProgressMonitor monitor )
    {
        Set<ISigilProjectModel> affected = SigilCore.getRoot().resolveDependentProjects( caps, monitor );

        if ( project != null ) {
            affected.remove( project );
        }

        SubMonitor progress = SubMonitor.convert( monitor, affected.size() * 20 );
        for ( ISigilProjectModel dependent : affected )
        {
            //dependent.flushDependencyState();
            rebuild( dependent, progress );
        }
    }


    public static void rebuild( ISigilProjectModel dependent, SubMonitor progress )
    {
        try
        {
            dependent.resetClasspath( progress.newChild( 10 ) );
            dependent.getProject().build( IncrementalProjectBuilder.FULL_BUILD, progress.newChild( 10 ) );
        }
        catch ( CoreException e )
        {
            SigilCore.error( "Failed to rebuild " + dependent, e );
        }
    }


    public IPath findDefaultBundleLocation( ISigilProjectModel m ) throws CoreException
    {
        IPath loc = m.getProject().getLocation();
        loc = loc.append( m.getJavaModel().getOutputLocation().removeFirstSegments( 1 ) );
        loc = loc.removeLastSegments( 1 ).append( "lib" );
        return loc.append( m.getSymbolicName() + ".jar" );
    }


    public static void makeSigilProject( IProject project, IProgressMonitor monitor ) throws CoreException
    {
        IProjectDescription description = project.getDescription();

        String[] natures = description.getNatureIds();
        String[] newNatures = new String[natures.length + 1];
        System.arraycopy( natures, 0, newNatures, 0, natures.length );
        newNatures[natures.length] = SigilCore.NATURE_ID;
        description.setNatureIds( newNatures );

        ICommand sigilBuild = description.newCommand();
        sigilBuild.setBuilderName( SigilCore.BUILDER_ID );

        ICommand javaBuild = description.newCommand();
        javaBuild.setBuilderName( JavaCore.BUILDER_ID );

        description.setBuildSpec( new ICommand[]
            { javaBuild, sigilBuild } );

        project.setDescription( description, new SubProgressMonitor( monitor, 2 ) );

        IJavaProject java = JavaCore.create( project );
        if ( java.exists() )
        {
            IClasspathEntry[] cp = java.getRawClasspath();
            // XXX fix for http://jira.codecauldron.org/browse/SIGIL-304
            if ( !isSigilOnClasspath( cp ) )
            {
                ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>( Arrays.asList( cp ) );
                entries.add( JavaCore.newContainerEntry( new Path( SigilCore.CLASSPATH_CONTAINER_PATH ) ) );
                java.setRawClasspath( entries.toArray( new IClasspathEntry[entries.size()] ), monitor );
            }
        }
    }


    /**
     * @param cp
     * @return
     */
    private static boolean isSigilOnClasspath( IClasspathEntry[] cp )
    {
        for ( IClasspathEntry e : cp )
        {
            if ( e.getEntryKind() == IClasspathEntry.CPE_CONTAINER
                && e.getPath().segment( 0 ).equals( SigilCore.CLASSPATH_CONTAINER_PATH ) )
            {
                return true;
            }
        }
        return false;
    }


    public static Image loadImage( URL url ) throws IOException
    {
        ImageRegistry registry = getDefault().getImageRegistry();

        String key = url.toExternalForm();
        Image img = registry.get( key );

        if ( img == null )
        {
            img = openImage( url );
            registry.put( key, img );
        }

        return img;
    }


    private static Image openImage( URL url ) throws IOException
    {
        Display display = Display.getCurrent();
        if ( display == null )
        {
            display = Display.getDefault();
        }

        InputStream in = null;
        try
        {
            in = url.openStream();
            return new Image( display, in );
        }
        finally
        {
            if ( in != null )
            {
                try
                {
                    in.close();
                }
                catch ( IOException e )
                {
                    error( "Failed to close stream", e );
                }
            }
        }
    }
}
