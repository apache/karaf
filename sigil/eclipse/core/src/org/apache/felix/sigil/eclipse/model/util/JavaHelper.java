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

package org.apache.felix.sigil.eclipse.model.util;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.regex.Pattern;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionRangeBoundingRule;
import org.apache.felix.sigil.eclipse.SigilCore;
import org.apache.felix.sigil.eclipse.model.project.ISigilProjectModel;
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
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentDescription;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.content.IContentTypeManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IAccessRule;
import org.eclipse.jdt.core.IAnnotation;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathAttribute;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IImportDeclaration;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IParent;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.ITypeHierarchy;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.preference.IPreferenceStore;
import org.osgi.framework.Version;


/**
 * @author dave
 *
 */
public class JavaHelper
{

    public static final IAccessRule DENY_RULE = JavaCore.newAccessRule( new Path( "**" ), IAccessRule.K_NON_ACCESSIBLE
        | IAccessRule.IGNORE_IF_BETTER );

    public static final IAccessRule ALLOW_ALL_RULE = JavaCore
        .newAccessRule( new Path( "**" ), IAccessRule.K_ACCESSIBLE );

    private static Map<String, Collection<IClasspathEntry>> entryCache = new HashMap<String, Collection<IClasspathEntry>>();


    public static Collection<IClasspathEntry> findClasspathEntries( ISigilBundle bundle )
    {
        LinkedList<IClasspathEntry> cp = new LinkedList<IClasspathEntry>();

        ISigilProjectModel sp = bundle.getAncestor( ISigilProjectModel.class );

        if ( sp != null )
        {
            IJavaProject p = sp.getJavaModel();

            for ( String enc : bundle.getClasspathEntrys() )
            {
                IClasspathEntry e = p.decodeClasspathEntry( enc );
                if ( e != null )
                {
                    cp.add( e );
                }
            }
        }

        return cp;
    }


    public static Collection<ICompilationUnit> findCompilationUnits( ISigilProjectModel project )
        throws JavaModelException
    {
        LinkedList<ICompilationUnit> ret = new LinkedList<ICompilationUnit>();

        IJavaProject java = project.getJavaModel();
        for ( IClasspathEntry cp : findClasspathEntries( project.getBundle() ) )
        {
            IPackageFragmentRoot[] roots = java.findPackageFragmentRoots( cp );
            for ( IPackageFragmentRoot rt : roots )
            {
                for ( IJavaElement j : rt.getChildren() )
                {
                    IPackageFragment p = ( IPackageFragment ) j;
                    ICompilationUnit[] units = p.getCompilationUnits();
                    for ( ICompilationUnit u : units )
                    {
                        ret.add( u );
                    }
                }
            }
        }

        return ret;
    }


    /**
     * @param project 
     * @param packageName
     * @return
     */
    public static Collection<IPackageExport> findExportsForPackage( ISigilProjectModel project, final String packageName )
    {
        final LinkedList<IPackageExport> results = new LinkedList<IPackageExport>();

        SigilCore.getRepositoryManager( project ).visit( new IModelWalker()
        {
            public boolean visit( IModelElement element )
            {
                if ( element instanceof IPackageExport )
                {
                    IPackageExport e = ( IPackageExport ) element;
                    if ( e.getPackageName().equals( packageName ) )
                    {
                        results.add( e );
                    }
                }
                return true;
            }
        } );

        return results;
    }


    public static String[] findUses( String packageName, ISigilProjectModel projectModel ) throws CoreException
    {
        ArrayList<String> uses = new ArrayList<String>();

        for ( final String dependency : findPackageDependencies( packageName, projectModel ) )
        {
            if ( !dependency.equals( packageName ) )
            {
                final boolean[] found = new boolean[1];

                projectModel.visit( new IModelWalker()
                {

                    public boolean visit( IModelElement element )
                    {
                        if ( element instanceof IPackageImport )
                        {
                            IPackageImport pi = ( IPackageImport ) element;
                            if ( pi.getPackageName().equals( dependency ) )
                            {
                                found[0] = true;
                            }
                        }
                        return !found[0];
                    }
                } );

                if ( found[0] )
                {
                    uses.add( dependency );
                }
            }
        }

        return uses.toArray( new String[uses.size()] );
    }


    private static String[] findPackageDependencies( String packageName, ISigilProjectModel projectModel )
        throws CoreException
    {
        HashSet<String> imports = new HashSet<String>();

        IPackageFragment p = ( IPackageFragment ) projectModel.getJavaModel().findElement(
            new Path( packageName.replace( '.', '/' ) ) );

        if ( p == null )
        {
            throw SigilCore.newCoreException( "Unknown package " + packageName, null );
        }
        for ( ICompilationUnit cu : p.getCompilationUnits() )
        {
            scanImports( cu, imports );
        }
        for ( IClassFile cf : p.getClassFiles() )
        {
            scanImports( cf, imports );
        }

        return imports.toArray( new String[imports.size()] );
    }


    /**
     * @param project
     * @param monitor
     * @return
     */
    public static List<IPackageImport> findRequiredImports( ISigilProjectModel project, IProgressMonitor monitor )
    {
        LinkedList<IPackageImport> imports = new LinkedList<IPackageImport>();

        for ( String packageName : findJavaImports( project, monitor ) )
        {
            if ( !ProfileManager.isBootDelegate( project, packageName ) )
            { // these must come from boot classloader
                try
                {
                    if ( !project.isInClasspath( packageName, monitor ) )
                    {
                        Collection<IPackageExport> exports = findExportsForPackage( project, packageName );
                        if ( !exports.isEmpty() )
                        {
                            imports.add( select( exports ) );
                        }
                    }
                }
                catch ( CoreException e )
                {
                    SigilCore.error( "Failed to check classpath", e );
                }
            }
        }

        return imports;
    }


    /**
     * @param project
     * @param monitor
     * @return
     */
    public static Collection<IModelElement> findUnusedReferences( final ISigilProjectModel project,
        final IProgressMonitor monitor )
    {
        final LinkedList<IModelElement> unused = new LinkedList<IModelElement>();

        final Set<String> packages = findJavaImports( project, monitor );

        project.visit( new IModelWalker()
        {
            public boolean visit( IModelElement element )
            {
                if ( element instanceof IPackageImport )
                {
                    IPackageImport pi = ( IPackageImport ) element;
                    if ( !packages.contains( pi.getPackageName() ) )
                    {
                        unused.add( pi );
                    }
                }
                else if ( element instanceof IRequiredBundle )
                {
                    IRequiredBundle rb = ( IRequiredBundle ) element;
                    IRepositoryManager manager = SigilCore.getRepositoryManager( project );
                    ResolutionConfig config = new ResolutionConfig( ResolutionConfig.INCLUDE_OPTIONAL
                        | ResolutionConfig.IGNORE_ERRORS );
                    try
                    {
                        IResolution r = manager.getBundleResolver().resolve( rb, config,
                            new ResolutionMonitorAdapter( monitor ) );
                        ISigilBundle bundle = r.getProvider( rb );
                        boolean found = false;
                        for ( IPackageExport pe : bundle.getBundleInfo().getExports() )
                        {
                            if ( packages.contains( pe.getPackageName() ) )
                            {
                                found = true;
                                break;
                            }
                        }

                        if ( !found )
                        {
                            unused.add( rb );
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

        return unused;
    }


    public static Collection<IClasspathEntry> resolveClasspathEntrys( ISigilProjectModel sigil,
        IProgressMonitor monitor ) throws CoreException
    {
        if ( monitor == null )
        {
            monitor = Job.getJobManager().createProgressGroup();
            monitor.beginTask( "Resolving classpath for " + sigil.getSymbolicName(), 2 );
        }

        ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();

        ResolutionConfig config = new ResolutionConfig( ResolutionConfig.INCLUDE_OPTIONAL
            | ResolutionConfig.IGNORE_ERRORS | ResolutionConfig.INDEXED_ONLY | ResolutionConfig.LOCAL_ONLY );

        IResolution resolution;
        try
        {
            resolution = SigilCore.getRepositoryManager( sigil ).getBundleResolver().resolve( sigil, config,
                new ResolutionMonitorAdapter( monitor ) );
        }
        catch ( ResolutionException e )
        {
            throw SigilCore.newCoreException( "Failed to resolve dependencies", e );
        }

        monitor.worked( 1 );

        Set<ISigilBundle> bundles = resolution.getBundles();
        for ( ISigilBundle bundle : bundles )
        {
            if ( !sigil.getSymbolicName().equals(bundle.getBundleInfo().getSymbolicName()) )
            { // discard self reference...
                List<IModelElement> matched = resolution.getMatchedRequirements( bundle );
                for ( IClasspathEntry cpe : buildClassPathEntry( sigil, bundle, bundles, matched, monitor ) )
                {
                    entries.add( cpe );
                }
            }
        }

        Collections.sort( entries, new Comparator<IClasspathEntry>()
        {
            public int compare( IClasspathEntry o1, IClasspathEntry o2 )
            {
                return o1.toString().compareTo( o2.toString() );
            }
        } );

        monitor.worked( 1 );
        monitor.done();

        return entries;
    }


    private static Collection<IClasspathEntry> buildClassPathEntry( ISigilProjectModel project, ISigilBundle provider,
        Set<ISigilBundle> all, List<IModelElement> requirements, IProgressMonitor monitor ) throws CoreException
    {
        IAccessRule[] rules = buildAccessRules( project, provider, all, requirements );

        ISigilProjectModel other = provider.getAncestor( ISigilProjectModel.class );

        try
        {
            if ( other == null )
            {
                provider.synchronize( monitor );
                return newBundleEntry( provider, rules, null, false );
            }
            else
            {
                return newProjectEntry( other, rules, null, false );
            }
        }
        catch ( IOException e )
        {
            throw SigilCore.newCoreException( "Failed to synchronize " + provider, e );
        }
    }


    private static IAccessRule[] buildExportRules( ISigilBundle bundle, Set<ISigilBundle> all,
        List<IModelElement> requirements )
    {
        Set<IPackageExport> ex = mergeExports( bundle, all, requirements );

        IAccessRule[] rules = new IAccessRule[ex.size() + 1];

        Iterator<IPackageExport> iter = ex.iterator();
        for ( int i = 0; i < rules.length - 1; i++ )
        {
            IPackageExport p = iter.next();
            rules[i] = JavaCore.newAccessRule( new Path( p.getPackageName().replace( '.', '/' ) ).append( "*" ),
                IAccessRule.K_ACCESSIBLE );
        }

        rules[rules.length - 1] = DENY_RULE;

        return rules;
    }


    private static Set<IPackageExport> mergeExports( ISigilBundle bundle, Set<ISigilBundle> all,
        List<IModelElement> requirements )
    {
        IBundleModelElement headers = bundle.getBundleInfo();
        // FIXME treeset as PackageExport does not implement equals/hashCode
        TreeSet<IPackageExport> exports = new TreeSet<IPackageExport>( headers.getExports() );
        IRequiredBundle host = headers.getFragmentHost();
        if ( host != null )
        {
            for ( ISigilBundle b : all )
            {
                if ( host.accepts( b.getBundleInfo() ) )
                {
                    exports.addAll( b.getBundleInfo().getExports() );
                    break;
                }
            }
        }
        return exports;
    }


    private static Collection<IClasspathEntry> newProjectEntry( ISigilProjectModel n, IAccessRule[] rules,
        IClasspathAttribute[] attributes, boolean export ) throws CoreException
    {
        //		if (rules == null) {
        //			rules = JavaHelper.buildExportRules(n.getBundle());
        //		}

        if ( attributes == null )
        {
            attributes = new IClasspathAttribute[]
                {};
        }

        ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>();
        entries.add( JavaCore.newProjectEntry( n.getProject().getFullPath(), rules, false, attributes, export ) );
        for ( IClasspathEntry e : n.getJavaModel().getRawClasspath() )
        {
            switch ( e.getEntryKind() )
            {
                case IClasspathEntry.CPE_LIBRARY:
                    entries.add( JavaCore.newLibraryEntry( e.getPath(), e.getSourceAttachmentPath(), e
                        .getSourceAttachmentRootPath(), rules, attributes, export ) );
                    break;
                case IClasspathEntry.CPE_VARIABLE:
                    IPath path = JavaCore.getResolvedVariablePath( e.getPath() );
                    if ( path != null )
                    {
                        entries.add( JavaCore.newLibraryEntry( path, e.getSourceAttachmentPath(), e
                            .getSourceAttachmentRootPath(), rules, attributes, export ) );
                    }
                    break;
            }
        }

        return entries;
    }


    private static Collection<IClasspathEntry> newBundleEntry( ISigilBundle bundle, IAccessRule[] rules,
        IClasspathAttribute[] attributes, boolean exported ) throws CoreException
    {
        String name = bundle.getBundleInfo().getSymbolicName();

        //		if (rules == null) {
        //			rules = JavaHelper.buildExportRules(bundle);
        //		}

        if ( attributes == null )
        {
            attributes = new IClasspathAttribute[]
                {};
        }

        if ( bundle.getBundleInfo().getVersion() != null )
        {
            name += "_version_" + bundle.getBundleInfo().getVersion();
        }

        String cacheName = name + rules.toString();

        Collection<IClasspathEntry> entries = null;

        synchronized ( entryCache )
        {
            entries = entryCache.get( cacheName );

            if ( entries == null )
            {
                IPath path = bundle.getLocation();

                if ( path == null )
                {
                    SigilCore.error( "Found null path for " + bundle.getBundleInfo().getSymbolicName() );
                    entries = Collections.emptyList();
                }
                else
                {
                    entries = buildEntries( path, name, bundle, rules, attributes, exported );
                }

                entryCache.put( cacheName, entries );
            }
        }

        return entries;
    }

    private static IPath bundleCache = SigilCore.getDefault().getStateLocation().append( "bundle-cache" );


    public static boolean isCachedBundle( String bp )
    {
        return bp.startsWith( bundleCache.toOSString() );
    }


    private static Collection<IClasspathEntry> buildEntries( IPath path, String name, ISigilBundle bundle,
        IAccessRule[] rules, IClasspathAttribute[] attributes, boolean exported ) throws CoreException
    {
        if ( path.toFile().isDirectory() )
        {
            throw SigilCore.newCoreException( "Bundle location cannot be a directory", null );
        }
        else
        {
            // ok it's a jar could contain libs etc
            try
            {
                IPath cache = bundleCache.append( name );
                Collection<String> classpath = bundle.getBundleInfo().getClasspaths();
                ArrayList<IClasspathEntry> entries = new ArrayList<IClasspathEntry>( classpath.size() );
                IPath source = bundle.getSourcePathLocation();

                if ( source != null && !source.toFile().exists() )
                {
                    source = null;
                }

                if ( !classpath.isEmpty() )
                {
                    unpack( cache, bundle, classpath );
                    for ( String cp : classpath )
                    {
                        IPath p = ".".equals( cp ) ? path : cache.append( cp );
                        if ( p.toFile().exists() )
                        {
                            IClasspathEntry e = JavaCore.newLibraryEntry( p, source, bundle.getSourceRootPath(), rules,
                                attributes, exported );
                            entries.add( e );
                        }
                    }
                }
                else
                { // default classpath is .
                    IClasspathEntry e = JavaCore.newLibraryEntry( path, source, bundle.getSourceRootPath(), rules,
                        attributes, exported );
                    entries.add( e );
                }
                return entries;
            }
            catch ( IOException e )
            {
                throw SigilCore.newCoreException( "Failed to unpack bundle", e );
            }
        }
    }

    private static HashMap<IPath, Collection<String>> unpacked = new HashMap<IPath, Collection<String>>();


    private static synchronized void unpack( IPath cache, ISigilBundle bundle, Collection<String> classpath )
        throws IOException
    {
        Collection<String> check = unpacked.get( cache );

        if ( check == null || !check.equals( classpath ) )
        {
            if ( classpath.size() == 1 && classpath.contains( "." ) )
            {
                unpacked.put( cache, classpath );
            }
            else
            {
                // trim . from path to avoid check later in inClasspath
                check = new HashSet<String>( classpath );
                check.remove( "." );

                File dir = createEmptyDir( cache );
                FileInputStream fin = null;
                try
                {
                    fin = new FileInputStream( bundle.getLocation().toFile() );
                    JarInputStream in = new JarInputStream( fin );
                    JarEntry entry;
                    while ( ( entry = in.getNextJarEntry() ) != null )
                    {
                        if ( inClasspath( check, entry ) )
                        {
                            File f = new File( dir, entry.getName() );
                            if ( entry.isDirectory() )
                            {
                                createDir( f );
                            }
                            else
                            {
                                try
                                {
                                    File p = f.getParentFile();
                                    createDir( p );
                                    streamTo( in, f );
                                }
                                catch ( RuntimeException e )
                                {
                                    SigilCore.error( "Failed to unpack " + entry, e );
                                }
                            }
                        }
                    }
                    unpacked.put( cache, classpath );
                }
                finally
                {
                    if ( fin != null )
                    {
                        fin.close();
                    }
                }
            }
        }
    }


    /**
     * @param classpath
     * @param entry
     * @return
     */
    private static boolean inClasspath( Collection<String> classpath, JarEntry entry )
    {
        for ( String s : classpath )
        {
            if ( entry.getName().startsWith( s ) )
            {
                return true;
            }
        }
        return false;
    }


    private static void createDir( File p ) throws IOException
    {
        if ( !p.exists() )
        {
            if ( !p.mkdirs() )
                throw new IOException( "Failed to create directory " + p );
        }
    }


    private static void streamTo( InputStream in, File f ) throws IOException
    {
        FileOutputStream fos = new FileOutputStream( f );
        try
        {
            byte[] buf = new byte[1024];
            for ( ;; )
            {
                int r = in.read( buf );

                if ( r == -1 )
                    break;

                fos.write( buf, 0, r );
            }

            fos.flush();
        }
        finally
        {
            try
            {
                fos.close();
            }
            catch ( IOException e )
            {
                SigilCore.error( "Failed to close stream", e );
            }
        }
    }


    private static File createEmptyDir( IPath cache )
    {
        File dir = cache.toFile();
        if ( dir.exists() )
        {
            deleteAll( dir );
        }

        dir.mkdirs();
        return dir;
    }


    private static void deleteAll( File file )
    {
        File[] sub = file.listFiles();
        if ( sub != null )
        {
            for ( File f : sub )
            {
                deleteAll( f );
            }
        }
        file.delete();
    }


    private static IAccessRule[] buildAccessRules( ISigilProjectModel project, ISigilBundle bundle,
        Set<ISigilBundle> all, List<IModelElement> requirements ) throws JavaModelException
    {
        ArrayList<IAccessRule> rules = new ArrayList<IAccessRule>();

        for ( IModelElement e : requirements )
        {
            if ( e instanceof IRequiredBundle )
            {
                IRequiredBundle host = project.getBundle().getBundleInfo().getFragmentHost();
                if ( host != null )
                {
                    if ( host.equals( e ) )
                    {
                        return new IAccessRule[]
                            { ALLOW_ALL_RULE };
                    }
                    else
                    {
                        return buildExportRules( bundle, all, requirements );
                    }
                }
                else
                {
                    return buildExportRules( bundle, all, requirements );
                }
            }
            else if ( e instanceof IPackageImport )
            {
                IPackageImport pi = ( IPackageImport ) e;
                String pckg = pi.getPackageName();
                HashSet<String> pckgs = new HashSet<String>();
                pckgs.add( pckg );
                //findIndirectReferences(pckgs, pckg, project.getJavaModel(), project.getJavaModel());

                for ( String p : pckgs )
                {
                    rules.add( newPackageAccess( p ) );
                }
            }
        }

        rules.add( DENY_RULE );

        return rules.toArray( new IAccessRule[rules.size()] );
    }


    /*
     * Searches for C (and D, E, etc) in case:
     * A extends B extends C where A, B and C are in different packages and A is in this bundle
     * and B and C are in one or more external bundles
     */
    private static void findIndirectReferences( Set<String> indirect, String pckg, IParent parent, IJavaProject p )
        throws JavaModelException
    {
        for ( IJavaElement e : parent.getChildren() )
        {
            boolean skip = false;
            switch ( e.getElementType() )
            {
                case IJavaElement.PACKAGE_FRAGMENT_ROOT:
                    IPackageFragmentRoot rt = ( IPackageFragmentRoot ) e;
                    IClasspathEntry ce = rt.getRawClasspathEntry();
                    IPath path = ce.getPath();
                    skip = "org.eclipse.jdt.launching.JRE_CONTAINER".equals( path.toString() );
                    break;
                case IJavaElement.CLASS_FILE:
                    IClassFile cf = ( IClassFile ) e;
                    if ( cf.getElementName().startsWith( pckg ) )
                    {
                        findIndirectReferences( indirect, findPackage( cf.getType().getSuperclassName() ), p, p );
                    }
                    break;
                case IJavaElement.COMPILATION_UNIT:
                    ICompilationUnit cu = ( ICompilationUnit ) e;
                    break;
            }

            if ( !skip && e instanceof IParent )
            {
                IParent newParent = ( IParent ) e;
                findIndirectReferences( indirect, pckg, newParent, p );
            }
        }
    }


    private static IAccessRule newPackageAccess( String packageName )
    {
        return JavaCore.newAccessRule( new Path( packageName.replace( '.', '/' ) ).append( "*" ),
            IAccessRule.K_ACCESSIBLE );
    }


    private static Set<String> findJavaImports( ISigilProjectModel project, IProgressMonitor monitor )
    {
        Set<String> imports = new HashSet<String>();

        findJavaModelImports( project, imports, monitor );
        findTextImports( project, imports, monitor );

        return imports;
    }


    private static void findTextImports( ISigilProjectModel project, Set<String> imports, IProgressMonitor monitor )
    {
        IContentTypeManager contentTypeManager = Platform.getContentTypeManager();
        IContentType txt = contentTypeManager.getContentType( "org.eclipse.core.runtime.text" );
        for ( IPath p : project.getBundle().getSourcePaths() )
        {
            IFile f = project.getProject().getFile( p );
            if ( f.exists() )
            {
                try
                {
                    IContentDescription desc = f.getContentDescription();
                    if ( desc != null )
                    {
                        IContentType type = desc.getContentType();
                        if ( type != null )
                        {
                            if ( type.isKindOf( txt ) )
                            {
                                parseText( f, imports );
                            }
                        }
                    }
                }
                catch ( CoreException e )
                {
                    SigilCore.error( "Failed to parse text file " + f, e );
                }
            }
        }
    }


    private static void findJavaModelImports( ISigilProjectModel project, Set<String> imports, IProgressMonitor monitor )
    {
        try
        {
            for ( IPackageFragment root : project.getJavaModel().getPackageFragments() )
            {
                IPackageFragmentRoot rt = ( IPackageFragmentRoot ) root
                    .getAncestor( IJavaElement.PACKAGE_FRAGMENT_ROOT );

                if ( isInClassPath( project, rt ) )
                {
                    for ( ICompilationUnit cu : root.getCompilationUnits() )
                    {
                        scanImports( cu, imports );
                    }

                    for ( IClassFile cf : root.getClassFiles() )
                    {
                        scanImports( cf, imports );
                    }
                }
            }
        }
        catch ( JavaModelException e )
        {
            SigilCore.error( "Failed to parse java model", e );
        }
    }

    // matches word.word.word.word.Word
    private static final Pattern JAVA_CLASS_PATTERN = Pattern.compile( "((\\w*\\.\\w*)+?)\\.[A-Z]\\w*" );


    private static void parseText( IFile f, Set<String> imports ) throws CoreException
    {
        for ( String result : Grep.grep( JAVA_CLASS_PATTERN, f ) )
        {
            findImport( result, imports );
        }
    }


    private static boolean isInClassPath( ISigilProjectModel project, IPackageFragmentRoot rt )
        throws JavaModelException
    {
        String path = encode( project, rt.getRawClasspathEntry() );
        return project.getBundle().getClasspathEntrys().contains( path );
    }


    private static String encode( ISigilProjectModel project, IClasspathEntry cp )
    {
        return project.getJavaModel().encodeClasspathEntry( cp ).trim();
    }


    private static void scanImports( IParent parent, Set<String> imports ) throws JavaModelException
    {
        for ( IJavaElement e : parent.getChildren() )
        {
            switch ( e.getElementType() )
            {
                case IJavaElement.TYPE:
                    handleType( ( IType ) e, imports );
                    break;
                case IJavaElement.IMPORT_DECLARATION:
                    handleImport( ( IImportDeclaration ) e, imports );
                    break;
                case IJavaElement.FIELD:
                    handleField( ( IField ) e, imports );
                    break;
                case IJavaElement.LOCAL_VARIABLE:
                    handleLocalVariable( ( ILocalVariable ) e, imports );
                    break;
                case IJavaElement.ANNOTATION:
                    handleAnnotation( ( IAnnotation ) e, imports );
                    break;
                case IJavaElement.METHOD:
                    handleMethod( ( IMethod ) e, imports );
                    break;
                default:
                    // no action
                    break;
            }

            if ( e instanceof IParent )
            {
                scanImports( ( IParent ) e, imports );
            }
        }
    }


    private static void handleType( IType e, Set<String> imports ) throws JavaModelException
    {
        findImportFromType( e.getSuperclassTypeSignature(), imports );
        for ( String sig : e.getSuperInterfaceTypeSignatures() )
        {
            findImportFromType( sig, imports );
        }
        //findImportsForSuperTypes(e, imports);
    }


    /*private static void findImportsForSuperTypes(IType e, Set<String> imports) throws JavaModelException {
    	IJavaProject project = (IJavaProject) e.getAncestor(IJavaModel.JAVA_PROJECT);
    	LinkedList<String> types = new LinkedList<String>();
    	types.add( decodeSignature(e.getSuperclassTypeSignature()) );
    	for ( String sig : e.getSuperInterfaceTypeSignatures() ) {
    		types.add( decodeSignature(sig) );
    	}
    	
    	for ( IPackageFragmentRoot root : project.getPackageFragmentRoots() ) {
    		// only need to search binary files for inheritance as source will automatically be searched
    		if ( root.getKind() == IPackageFragmentRoot.K_BINARY ) {
    			for ( String t : types ) {
    				String pac = findPackage(t);
    				if ( pac != null ) {
    					IPackageFragment fragment = root.getPackageFragment(pac);
    					if ( fragment != null ) {
    						IClassFile c = fragment.getClassFile(findClass(t));
    						if ( c != null ) {
    							findImportsForSuperTypes(c.getType(), imports);
    						}
    					}
    				}
    			}
    		}
    	}
    } */

    private static void handleMethod( IMethod e, Set<String> imports ) throws JavaModelException
    {
        findImportFromType( e.getReturnType(), imports );

        for ( String param : e.getParameterTypes() )
        {
            findImportFromType( param, imports );
        }

        for ( String ex : e.getExceptionTypes() )
        {
            findImportFromType( ex, imports );
        }
    }


    private static void handleAnnotation( IAnnotation e, Set<String> imports )
    {
        findImport( e.getElementName(), imports );
    }


    private static void handleLocalVariable( ILocalVariable e, Set<String> imports )
    {
        findImportFromType( e.getTypeSignature(), imports );
    }


    private static void handleField( IField e, Set<String> imports ) throws IllegalArgumentException,
        JavaModelException
    {
        findImportFromType( Signature.getElementType( e.getTypeSignature() ), imports );
    }


    private static void handleImport( IImportDeclaration id, Set<String> imports )
    {
        findImport( id.getElementName(), imports );
    }


    private static void findImportFromType( String type, Set<String> imports )
    {
        String element = decodeSignature( type );
        findImport( element, imports );
    }


    private static String decodeSignature( String type )
    {
        return decodeSignature( type, false );
    }


    private static String decodeSignature( String type, boolean resolve )
    {
        if ( type == null )
        {
            return null;
        }

        if ( type.length() > 0 )
        {
            switch ( type.charAt( 0 ) )
            {
                case Signature.C_ARRAY:
                    return decodeSignature( type.substring( 1 ) );
                case Signature.C_UNRESOLVED:
                    return resolve ? resolve( type.substring( 1, type.length() - 1 ) ) : null;
                case Signature.C_RESOLVED:
                    return type.substring( 1 );
            }
        }
        return type;
    }


    private static String resolve( String substring )
    {
        // TODO Auto-generated method stub
        return null;
    }


    private static void findImport( String clazz, Set<String> imports )
    {
        String packageName = findPackage( clazz );
        if ( packageName != null )
        {
            imports.add( packageName );
        }
    }


    private static String findPackage( String clazz )
    {
        if ( clazz == null )
            return null;
        int pos = clazz.lastIndexOf( '.' );
        return pos == -1 ? null : clazz.substring( 0, pos );
    }


    private static String findClass( String clazz )
    {
        if ( clazz == null )
            return null;
        int pos = clazz.lastIndexOf( '.' );
        return pos == -1 ? null : clazz.substring( pos + 1 );
    }


    private static IPackageImport select( Collection<IPackageExport> proposals )
    {
        IPackageExport pe = null;

        for ( IPackageExport check : proposals )
        {
            if ( pe == null || check.getVersion().compareTo( pe.getVersion() ) > 0 )
            {
                pe = check;
            }
        }

        String packageName = pe.getPackageName();

        IPreferenceStore store = SigilCore.getDefault().getPreferenceStore();
        VersionRangeBoundingRule lowerBoundRule = VersionRangeBoundingRule.valueOf( store
            .getString( SigilCore.DEFAULT_VERSION_LOWER_BOUND ) );
        VersionRangeBoundingRule upperBoundRule = VersionRangeBoundingRule.valueOf( store
            .getString( SigilCore.DEFAULT_VERSION_UPPER_BOUND ) );

        Version version = pe.getVersion();
        VersionRange versions = VersionRange.newInstance( version, lowerBoundRule, upperBoundRule );

        IPackageImport pi = ModelElementFactory.getInstance().newModelElement( IPackageImport.class );
        pi.setPackageName( packageName );
        pi.setVersions( versions );

        return pi;
    }


    public static Iterable<IJavaElement> findTypes( IParent parent, int... type ) throws JavaModelException
    {
        LinkedList<IJavaElement> found = new LinkedList<IJavaElement>();
        scanForElement( parent, type, found, false );
        return found;
    }


    public static IJavaElement findType( IParent parent, int... type ) throws JavaModelException
    {
        LinkedList<IJavaElement> found = new LinkedList<IJavaElement>();
        scanForElement( parent, type, found, true );
        return found.isEmpty() ? null : found.getFirst();
    }


    private static void scanForElement( IParent parent, int[] type, List<IJavaElement> roots, boolean thereCanBeOnlyOne )
        throws JavaModelException
    {
        for ( IJavaElement e : parent.getChildren() )
        {
            if ( isType( type, e ) )
            {
                roots.add( e );
                if ( thereCanBeOnlyOne )
                {
                    break;
                }
            }
            else if ( e instanceof IParent )
            {
                scanForElement( ( IParent ) e, type, roots, thereCanBeOnlyOne );
            }
        }
    }


    private static boolean isType( int[] type, IJavaElement e )
    {
        for ( int i : type )
        {
            if ( i == e.getElementType() )
            {
                return true;
            }
        }
        return false;
    }


    public static boolean isAssignableTo( String ifaceOrParentClass, IType type ) throws JavaModelException
    {
        if ( ifaceOrParentClass == null )
            return true;

        ITypeHierarchy h = type.newSupertypeHierarchy( null );

        for ( IType superType : h.getAllClasses() )
        {
            String name = superType.getFullyQualifiedName();
            if ( name.equals( ifaceOrParentClass ) )
            {
                return true;
            }
        }
        for ( IType ifaceType : h.getAllInterfaces() )
        {
            String name = ifaceType.getFullyQualifiedName();
            if ( name.equals( ifaceOrParentClass ) )
            {
                return true;
            }
        }

        return false;
    }


    private static IType findType( ITypeRoot root ) throws JavaModelException
    {
        // TODO Auto-generated method stub
        for ( IJavaElement child : root.getChildren() )
        {
            if ( child.getElementType() == IJavaElement.TYPE )
            {
                return ( IType ) child;
            }
        }

        throw new JavaModelException( new IllegalStateException( "Missing type for " + root ), IStatus.ERROR );
    }
}