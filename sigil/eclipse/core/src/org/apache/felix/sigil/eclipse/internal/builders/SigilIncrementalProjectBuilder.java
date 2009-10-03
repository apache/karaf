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

package org.apache.felix.sigil.eclipse.internal.builders;


import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.sigil.bnd.BundleBuilder;
import org.apache.felix.sigil.config.IBldProject;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsoleStream;


public class SigilIncrementalProjectBuilder extends IncrementalProjectBuilder
{

    @Override
    protected IProject[] build( int kind, @SuppressWarnings("unchecked") Map args, IProgressMonitor monitor )
        throws CoreException
    {
        IProject project = getProject();

        if ( checkOk( project ) )
        {
            switch ( kind )
            {
                case CLEAN_BUILD:
                case FULL_BUILD:
                    fullBuild( project, monitor );
                    break;
                case AUTO_BUILD:
                case INCREMENTAL_BUILD:
                    autoBuild( project, monitor );
                    break;
            }
        }

        return null;
    }


    /**
     * @param install
     * @param project
     * @param monitor
     * @throws CoreException 
     */
    private void autoBuild( IProject project, IProgressMonitor monitor ) throws CoreException
    {
        IResourceDelta delta = getDelta( project );
        final boolean[] changed = new boolean[1];
        ISigilProjectModel sigil = SigilCore.create( project );
        final IPath bldRoot = sigil.findBundleLocation().removeLastSegments( 1 );

        delta.accept( new IResourceDeltaVisitor()
        {
            public boolean visit( IResourceDelta delta ) throws CoreException
            {
                if ( !changed[0] )
                {
                    IResource res = delta.getResource();
                    if ( res.getType() == IResource.FILE )
                    {
                        changed[0] = !bldRoot.isPrefixOf( res.getLocation() );
                    }
                }
                return !changed[0];
            }
        } );

        if ( changed[0] )
        {
            doBuild( project, monitor );
        }
    }


    /**
     * @param install
     * @param project
     * @param monitor
     * @throws CoreException 
     */
    private void fullBuild( IProject project, IProgressMonitor monitor ) throws CoreException
    {
        doBuild( project, monitor );
    }


    private boolean checkOk( IProject project ) throws CoreException
    {
        IMarker[] markers = project.findMarkers( IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true,
            IResource.DEPTH_INFINITE );

        for ( IMarker m : markers )
        {
            Integer s = ( Integer ) m.getAttribute( IMarker.SEVERITY );
            if ( s != null && s.equals( IMarker.SEVERITY_ERROR ) )
            {
                SigilCore.log( "Skipping " + project.getName() + " build due to unresolved errors" );
                return false;
            }
        }

        return true;
    }


    private void doBuild( IProject project, IProgressMonitor monitor ) throws CoreException
    {
        ISigilProjectModel sigil = SigilCore.create( project );
        IBldProject bld = sigil.getBldProject();

        File[] classpath = buildClasspath( sigil, monitor );

        String destPattern = buildDestPattern( sigil );

        Properties env = new Properties();

        BundleBuilder bb = new BundleBuilder( bld, classpath, destPattern, env );

        for ( IBldProject.IBldBundle bundle : bld.getBundles() )
        {
            String id = bundle.getId();
            loginfo( "creating bundle: " + id );
            int nWarn = 0;
            int nErr = 0;
            String msg = "";

            try
            {
                boolean modified = bb.createBundle( bundle, false, new BundleBuilder.Log()
                {
                    public void warn( String msg )
                    {
                        logwarn( msg );
                    }


                    public void verbose( String msg )
                    {
                        loginfo( msg );
                    }
                } );
                nWarn = bb.warnings().size();
                if ( !modified )
                {
                    msg = " (not modified)";
                }
            }
            catch ( Exception e )
            {
                List<String> errors = bb.errors();
                if ( errors != null )
                {
                    nErr = errors.size();
                    for ( String err : errors )
                    {
                        logerror( err );
                    }
                }
                // FELIX-1690 - error is already logged no need to throw error as this
                // results in noisy error dialog box
                //throw SigilCore.newCoreException( "Failed to create: " + id + ": " + e, e );
            }
            finally
            {
                loginfo( id + ": " + count( nErr, "error" ) + ", " + count( nWarn, "warning" ) + msg );
            }
        }
    }


    private static void loginfo( String message )
    {
        BuildConsole console = findConsole();
        MessageConsoleStream stream = console.getMessageStream();
        stream.println( "INFO: " + message );
    }


    private static void logwarn( String message )
    {
        BuildConsole console = findConsole();
        MessageConsoleStream stream = console.getMessageStream();
        stream.println( "WARN: " + message );
    }


    private static void logerror( String message )
    {
        BuildConsole console = findConsole();
        MessageConsoleStream stream = console.getMessageStream();
        stream.println( "ERROR: " + message );
    }


    private static BuildConsole findConsole()
    {
        BuildConsole console = null;

        IConsoleManager manager = ConsolePlugin.getDefault().getConsoleManager();

        for ( IConsole c : manager.getConsoles() )
        {
            if ( c instanceof BuildConsole )
            {
                console = ( BuildConsole ) c;
                break;
            }
        }

        if ( console == null )
        {
            console = new BuildConsole();
            manager.addConsoles( new IConsole[]
                { console } );
        }

        return console;
    }


    private String buildDestPattern( ISigilProjectModel sigil ) throws CoreException
    {
        IPath loc = sigil.findBundleLocation().removeLastSegments( 1 );

        loc.toFile().mkdirs();

        return loc.toOSString() + File.separator + "[name].jar";
    }


    private File[] buildClasspath( ISigilProjectModel sigil, IProgressMonitor monitor ) throws CoreException
    {
        ArrayList<File> files = new ArrayList<File>();

        buildLocalClasspath( sigil, files );
        buildExternalClasspath( sigil, files, monitor );

        return files.toArray( new File[files.size()] );
    }


    private void buildExternalClasspath( ISigilProjectModel sigil, ArrayList<File> files, IProgressMonitor monitor )
        throws CoreException
    {
        Collection<IClasspathEntry> entries = sigil.findExternalClasspath( monitor );
        files.ensureCapacity( files.size() + entries.size() );

        for ( IClasspathEntry cp : entries )
        {
            convert( cp, sigil, files );
        }
    }


    private void buildLocalClasspath( ISigilProjectModel sigil, ArrayList<File> files ) throws CoreException
    {
        Collection<IClasspathEntry> entries = JavaHelper.findClasspathEntries( sigil.getBundle() );
        files.ensureCapacity( files.size() + entries.size() );
        for ( IClasspathEntry cp : entries )
        {
            convert( cp, sigil, files );
        }
    }


    private void convert( IClasspathEntry cp, ISigilProjectModel sigil, ArrayList<File> files ) throws CoreException
    {
        switch ( cp.getEntryKind() )
        {
            case IClasspathEntry.CPE_PROJECT:
            {
                IProject p = findProject( cp.getPath() );
                ISigilProjectModel project = SigilCore.create( p );
                for ( String scp : project.getBundle().getClasspathEntrys() )
                {
                    IClasspathEntry jcp = project.getJavaModel().decodeClasspathEntry( scp );
                    convert( jcp, project, files );
                }
                break;
            }
            case IClasspathEntry.CPE_SOURCE:
            {
                IPath path = cp.getOutputLocation() == null ? sigil.getJavaModel().getOutputLocation() : cp
                    .getOutputLocation();
                IFolder buildFolder = sigil.getProject().getFolder( path.removeFirstSegments( 1 ) );
                if ( buildFolder.exists() )
                {
                    files.add( buildFolder.getLocation().toFile() );
                }
                break;
            }
            case IClasspathEntry.CPE_LIBRARY:
            {
                IPath p = cp.getPath();

                IProject project = sigil.getProject().getWorkspace().getRoot().getProject( p.segment( 0 ) );
                if ( project.exists() )
                {
                    p = project.getLocation().append( p.removeFirstSegments( 1 ) );
                }

                files.add( p.toFile() );
                break;
            }
            case IClasspathEntry.CPE_VARIABLE:
                cp = JavaCore.getResolvedClasspathEntry( cp );
                if ( cp != null )
                {
                    IPath p = cp.getPath();
                    files.add( p.toFile() );
                }
                break;
        }
    }


    private IProject findProject( IPath path ) throws CoreException
    {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        for ( IProject p : root.getProjects() )
        {
            IPath projectPath = p.getFullPath();
            if ( projectPath.equals( path ) )
            {
                return p;
            }
        }

        throw SigilCore.newCoreException( "No such project " + path, null );
    }


    private String count( int count, String msg )
    {
        return count + " " + msg + ( count == 1 ? "" : "s" );
    }
}
