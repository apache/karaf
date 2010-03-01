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

package org.apache.felix.sigil.search;


import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
import org.apache.felix.sigil.eclipse.model.util.JavaHelper;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.repository.IBundleRepository;
import org.apache.felix.sigil.repository.IRepositoryChangeListener;
import org.apache.felix.sigil.repository.IRepositoryVisitor;
import org.apache.felix.sigil.repository.RepositoryChangeEvent;
import org.apache.felix.sigil.search.index.Index;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;


/**
 * The activator class controls the plug-in life cycle
 */
public class SigilSearch extends AbstractUIPlugin
{

    // The plug-in ID
    public static final String PLUGIN_ID = "org.apache.felix.sigil.eclipse.search";

    private static final String CLASS_EXTENSION = ".class";

    // The shared instance
    private static SigilSearch plugin;
    private static Index index;


    /**
     * The constructor
     */
    public SigilSearch()
    {
    }


    public static List<ISearchResult> findProviders( String fullyQualifiedName, ISigilProjectModel sigil,
        IProgressMonitor monitor )
    {
        listen( sigil );
        return index.findProviders( fullyQualifiedName, monitor );
    }


    public static List<ISearchResult> findProviders( Pattern namePattern, ISigilProjectModel sigil,
        IProgressMonitor monitor )
    {
        listen( sigil );
        return index.findProviders( namePattern, monitor );
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    public void start( BundleContext context ) throws Exception
    {
        super.start( context );
        plugin = this;
    }


    /*
     * (non-Javadoc)
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    public void stop( BundleContext context ) throws Exception
    {
        plugin = null;
        super.stop( context );
    }


    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static SigilSearch getDefault()
    {
        return plugin;
    }


    private static void listen( ISigilProjectModel sigil )
    {
        synchronized ( plugin )
        {
            if ( index == null )
            {
                index = new Index();
                for ( IBundleRepository rep : SigilCore.getRepositoryManager( sigil ).getRepositories() )
                {
                    index( index, rep );
                }

                SigilCore.getRepositoryManager( sigil ).addRepositoryChangeListener( new IRepositoryChangeListener()
                {
                    public void repositoryChanged( RepositoryChangeEvent event )
                    {
                        index( index, event.getRepository() );
                    }
                } );
            }
        }
    }


    private static void index( final Index index, final IBundleRepository rep )
    {
        index.delete( rep );
        rep.accept( new IRepositoryVisitor()
        {
            public boolean visit( ISigilBundle bundle )
            {
                ISigilProjectModel p = bundle.getAncestor( ISigilProjectModel.class );
                if ( p == null )
                {
                    if ( bundle.isSynchronized() )
                    {
                        IPath loc = bundle.getLocation();
                        if ( loc == null ) {
                            SigilCore.error("Location is null for " + bundle);
                        }
                        else {
                            if ( loc.isAbsolute() )
                            {
                                indexJar( rep, bundle, loc );
                            }
                        }
                    }
                }
                else
                {
                    indexProject( rep, p );
                }
                return true;
            }
        } );
    }


    private static void indexProject( IBundleRepository rep, ISigilProjectModel sigil )
    {
        try
        {
            for ( ICompilationUnit unit : JavaHelper.findCompilationUnits( sigil ) )
            {
                IPackageFragment p = ( IPackageFragment ) unit.getParent();
                ISigilBundle b = sigil.getBundle();
                IPackageExport export = b.findExport( p.getElementName() );
                index.addEntry( unit, rep, b, export != null );
            }
        }
        catch ( JavaModelException e )
        {
            SigilCore.error( "Failed to index project", e);
        }
    }


    private static void indexJar( IBundleRepository rep, ISigilBundle bundle, IPath loc )
    {
        JarFile jar = null;
        try
        {
            jar = new JarFile( loc.toOSString() );
            for ( Map.Entry<JarEntry, IPackageExport> export : findExportedClasses( bundle, jar ).entrySet() )
            {
                JarEntry entry = export.getKey();
                InputStream in = null;
                try
                {
                    in = jar.getInputStream( entry );
                    ClassParser parser = new ClassParser( in, entry.getName() );
                    JavaClass c = parser.parse();
                    index.addEntry( c, rep, bundle, true );
                }
                finally
                {
                    if ( in != null )
                    {
                        in.close();
                    }
                }
            }
        }
        catch ( IOException e )
        {
            SigilCore.error( "Failed to read jar " + loc, e );
        }
        finally
        {
            if ( jar != null )
            {
                try
                {
                    jar.close();
                }
                catch ( IOException e )
                {
                    SigilCore.error( "Failed to close jar " + loc, e );
                }
            }
        }
    }


    private static Map<JarEntry, IPackageExport> findExportedClasses( ISigilBundle bundle, JarFile jar )
    {
        HashMap<JarEntry, IPackageExport> found = new HashMap<JarEntry, IPackageExport>();

        IPackageExport[] exports = bundle.getBundleInfo().childrenOfType( IPackageExport.class );
        if ( exports.length > 0 )
        {
            Arrays.sort( exports, new Comparator<IPackageExport>()
            {
                public int compare( IPackageExport o1, IPackageExport o2 )
                {
                    return -1 * o1.compareTo( o2 );
                }
            } );
            for ( Enumeration<JarEntry> e = jar.entries(); e.hasMoreElements(); )
            {
                JarEntry entry = e.nextElement();
                String className = toClassName( entry );
                if ( className != null )
                {
                    IPackageExport ex = findExport( className, exports );

                    if ( found != null )
                    {
                        found.put( entry, ex );
                    }
                }
            }
        }

        return found;
    }


    private static IPackageExport findExport( String className, IPackageExport[] exports )
    {
        for ( IPackageExport e : exports )
        {
            if ( className.startsWith( e.getPackageName() ) )
            {
                return e;
            }
        }
        return null;
    }


    private static String toClassName( JarEntry entry )
    {
        String name = entry.getName();
        if ( name.endsWith( CLASS_EXTENSION ) )
        {
            name = name.substring( 0, name.length() - CLASS_EXTENSION.length() );
            name = name.replace( '/', '.' );
            return name;
        }
        else
        {
            return null;
        }
    }
}
