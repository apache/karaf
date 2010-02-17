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

package org.apache.felix.sigil.config;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.felix.sigil.common.osgi.VersionRange;
import org.apache.felix.sigil.common.osgi.VersionTable;
import org.apache.felix.sigil.config.IBldProject.IBldBundle;
import org.apache.felix.sigil.core.BldCore;
import org.apache.felix.sigil.core.internal.model.eclipse.SigilBundle;
import org.apache.felix.sigil.core.internal.model.osgi.BundleModelElement;
import org.apache.felix.sigil.model.eclipse.ISigilBundle;
import org.apache.felix.sigil.model.osgi.IBundleModelElement;
import org.apache.felix.sigil.model.osgi.IPackageExport;
import org.apache.felix.sigil.model.osgi.IPackageImport;
import org.apache.felix.sigil.model.osgi.IRequiredBundle;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import aQute.lib.osgi.Constants;


public class BldConverter
{
    private static final String classpathFormat = "<classpathentry kind=\"%s\" path=\"%s\"/>";
    private BldConfig config;
    private Properties packageDefaults;
    private TreeSet<String> packageWildDefaults;


    public BldConverter( BldConfig config )
    {
        this.config = config;
    }


    /**
     * converts to an ISigilBundle.
     * @param id
     * @param bundle
     * @return
     */
    public ISigilBundle getBundle( String id, IBldBundle bundle )
    {

        ISigilBundle sigilBundle = new SigilBundle();
        IBundleModelElement info = new BundleModelElement();
        sigilBundle.setBundleInfo( info );

        // exports
        // FIXME: UI doesn't understand export wildcard packages
        for ( IPackageExport export : bundle.getExports() )
        {
            IPackageExport clone = ( IPackageExport ) export.clone();
            clone.setParent( null );
            info.addExport( clone );
        }

        // imports
        for ( IPackageImport import1 : bundle.getImports() )
        {
            IPackageImport clone = ( IPackageImport ) import1.clone();
            clone.setParent( null );
            info.addImport( clone );
        }

        // requires
        for ( IRequiredBundle require : bundle.getRequires() )
        {
            IRequiredBundle clone = ( IRequiredBundle ) require.clone();
            clone.setParent( null );
            info.addRequiredBundle( clone );
        }

        // fragment
        IRequiredBundle fragment = bundle.getFragmentHost();
        if ( fragment != null )
        {
            info.setFragmentHost( fragment );
        }

        // contents
        for ( String pkg : bundle.getContents() )
        {
            sigilBundle.addPackage( pkg );
        }

        // sources
        for ( String source : config.getList( null, BldConfig.L_SRC_CONTENTS ) )
        {
            sigilBundle.addClasspathEntry( String.format( classpathFormat, "src", source ) );
        }

        // libs
        Map<String, Map<String, String>> libs = bundle.getLibs();

        for ( String path : libs.keySet() )
        {
            Map<String, String> attr = libs.get( path );
            String kind = attr.get( BldAttr.KIND_ATTRIBUTE );
            
            if ( "classpath".equals( kind ) )
            {
                sigilBundle.addClasspathEntry( String.format( classpathFormat, "lib", path ) );
            }
            else
            {
                BldCore.error( "Can't convert -libs kind=" + kind );
            }
        }

        // resources
        // FIXME: UI doesn't support -resources: path1=path2
        Map<String, String> resources = bundle.getResources();
        for ( String resource : resources.keySet() )
        {
            String fsPath = resources.get( resource );
            if ( !"".equals( fsPath ) )
            {
                BldCore.error( "FIXME: can't convert resource: " + resource + "=" + fsPath );
            }
            sigilBundle.addSourcePath( new Path( resource ) );
        }

        ////////////////////
        // simple headers

        info.setSymbolicName( bundle.getSymbolicName() );

        info.setVersion( VersionTable.getVersion( bundle.getVersion() ) );

        String activator = bundle.getActivator();
        if ( activator != null )
            info.setActivator( activator );

        Properties headers = config.getProps( id, BldConfig.P_HEADER );
        String header;

        header = headers.getProperty( "CATEGORY" );
        if ( header != null )
            info.setCategory( header );

        header = headers.getProperty( Constants.BUNDLE_CONTACTADDRESS );
        if ( header != null )
            info.setContactAddress( header );

        header = headers.getProperty( Constants.BUNDLE_COPYRIGHT );
        if ( header != null )
            info.setCopyright( header );

        header = headers.getProperty( Constants.BUNDLE_DESCRIPTION );
        if ( header != null )
            info.setDescription( header );

        header = headers.getProperty( Constants.BUNDLE_VENDOR );
        if ( header != null )
            info.setVendor( header );

        header = headers.getProperty( Constants.BUNDLE_NAME );
        if ( header != null )
            info.setName( header );

        header = headers.getProperty( Constants.BUNDLE_DOCURL );
        if ( header != null )
            info.setDocURI( URI.create( header ) );

        header = headers.getProperty( Constants.BUNDLE_LICENSE );
        if ( header != null )
            info.setDocURI( URI.create( header ) );

        return sigilBundle;
    }


    private VersionRange defaultVersion( VersionRange current, String defaultRange )
    {
        if ( current.equals( VersionRange.ANY_VERSION )
            || current.equals( VersionRange.parseVersionRange( defaultRange ) ) )
        {
            return null;
        }
        return current;
    }


    // FIXME - copied from BldProject
    private String getDefaultPackageVersion( String name )
    {
        if ( packageDefaults == null )
        {
            packageDefaults = config.getProps( null, BldConfig.P_PACKAGE_VERSION );
            packageWildDefaults = new TreeSet<String>();

            for ( Object key : packageDefaults.keySet() )
            {
                String pkg = ( String ) key;
                if ( pkg.endsWith( "*" ) )
                {
                    packageWildDefaults.add( pkg.substring( 0, pkg.length() - 1 ) );
                }
            }
        }

        String version = packageDefaults.getProperty( name );

        if ( version == null )
        {
            for ( String pkg : packageWildDefaults )
            {
                if ( name.startsWith( pkg ) )
                {
                    version = packageDefaults.getProperty( pkg + "*" );
                    // break; -- don't break, as we want the longest match
                }
            }
        }

        return version;
    }


    /**
     * converts from an ISigilBundle.
     * 
     * @param id
     * @param bundle
     */
    public void setBundle( String id, ISigilBundle bundle )
    {
        IBundleModelElement info = bundle.getBundleInfo();
        String bundleVersion = config.getString( id, BldConfig.S_VERSION );
        Map<String, Map<String, String>> exports = new TreeMap<String, Map<String, String>>();
        
        setSimpleHeaders(id, info);
        setExports(id, bundleVersion, info, exports);
        setImports(id, bundleVersion, info, exports);
        setRequires(id, bundleVersion, info);
        setFragments(id, info);
        setContents(id, info, bundle);
        setLibraries(id, info, bundle);
        setResources(id, info, bundle);

        if ( info.getSourceLocation() != null )
        {
            BldCore.error( "SourceLocation conversion not yet implemented." );
        }

        if ( !info.getLibraryImports().isEmpty() )
        {
            BldCore.error( "LibraryImports conversion not yet implemented." );
        }
    }


    /**
     * @param id
     * @param info
     */
    private void setSimpleHeaders( String id, IBundleModelElement info )
    {
        List<String> ids = config.getList( null, BldConfig.C_BUNDLES );
        String idBsn = id != null ? id : ids.get( 0 );
        String oldBsn = config.getString( id, BldConfig.S_SYM_NAME );
        String bsn = info.getSymbolicName();

        if ( !bsn.equals( idBsn ) || oldBsn != null )
            config.setString( id, BldConfig.S_SYM_NAME, bsn );

        String version = info.getVersion().toString();
        if ( version != null )
            config.setString( id, BldConfig.S_VERSION, version );

        String activator = info.getActivator();
        if ( activator != null )
            config.setString( id, BldConfig.S_ACTIVATOR, activator );

        Properties headers = config.getProps( null, BldConfig.P_HEADER );

        setHeader( headers, id, "CATEGORY", info.getCategory() );
        setHeader( headers, id, Constants.BUNDLE_CONTACTADDRESS, info.getContactAddress() );
        setHeader( headers, id, Constants.BUNDLE_COPYRIGHT, info.getCopyright() );
        setHeader( headers, id, Constants.BUNDLE_DESCRIPTION, info.getDescription() );
        setHeader( headers, id, Constants.BUNDLE_VENDOR, info.getVendor() );
        setHeader( headers, id, Constants.BUNDLE_NAME, info.getName() );

        if ( info.getDocURI() != null )
            config.setProp( id, BldConfig.P_HEADER, Constants.BUNDLE_DOCURL, info.getDocURI().toString() );

        if ( info.getLicenseURI() != null )
            config.setProp( id, BldConfig.P_HEADER, Constants.BUNDLE_LICENSE, info.getLicenseURI().toString() );
    }


    /**
     * @param id
     * @param info
     * @param bundle 
     */
    private void setResources( String id, IBundleModelElement info, ISigilBundle bundle )
    {
        // resources
        ArrayList<String> resources = new ArrayList<String>();
        for ( IPath ipath : bundle.getSourcePaths() )
        {
            resources.add( ipath.toString() );
        }

        if ( !resources.isEmpty() || !config.getList( id, BldConfig.L_RESOURCES ).isEmpty() )
        {
            Collections.sort( resources );
            config.setList( id, BldConfig.L_RESOURCES, resources );
        }
    }


    /**
     * @param id 
     * @param info
     * @param bundle 
     */
    private void setLibraries( String id, IBundleModelElement info, ISigilBundle bundle )
    {
        // libs
        Map<String, Map<String, String>> libs = new TreeMap<String, Map<String, String>>();
        List<String> sources = new ArrayList<String>();

        // classpathEntries map to -libs or -sources
        for ( String entry : bundle.getClasspathEntrys() )
        {
            // <classpathentry kind="lib" path="lib/dependee.jar"/>
            // <classpathentry kind="src" path="src"/>
            final String regex = ".* kind=\"([^\"]+)\" path=\"([^\"]+)\".*";
            Pattern pattern = Pattern.compile( regex );
            Matcher matcher = pattern.matcher( entry );
            if ( matcher.matches() )
            {
                String kind = matcher.group( 1 );
                String path = matcher.group( 2 );
                if ( kind.equals( "lib" ) )
                {
                    Map<String, String> map2 = new TreeMap<String, String>();
                    map2.put( BldAttr.KIND_ATTRIBUTE, "classpath" );
                    libs.put( path, map2 );
                }
                else if ( kind.equals( "src" ) )
                {
                    sources.add( path );
                }
                else
                {
                    BldCore.error( "unknown classpathentry kind=" + kind );
                }
            }
            else
            {
                BldCore.error( "can't match classpathEntry in: " + entry );
            }
        }

        if ( !libs.isEmpty() || !config.getMap( id, BldConfig.M_LIBS ).isEmpty() )
        {
            config.setMap( id, BldConfig.M_LIBS, libs );
        }

        if ( !sources.isEmpty() || !config.getList( id, BldConfig.L_SRC_CONTENTS ).isEmpty() )
        {
            config.setList( id, BldConfig.L_SRC_CONTENTS, sources );
        }

    }


    /**
     * @param id 
     * @param info
     * @param bundle 
     */
    private void setContents( String id, IBundleModelElement info, ISigilBundle bundle )
    {
        // contents
        List<String> contents = new ArrayList<String>();
        for ( String pkg : bundle.getPackages() )
        {
            contents.add( pkg );
        }
        if ( !contents.isEmpty() || !config.getList( id, BldConfig.L_CONTENTS ).isEmpty() )
        {
            config.setList( id, BldConfig.L_CONTENTS, contents );
        }
    }


    /**
     * @param id
     * @param info
     */
    private void setFragments( String id, IBundleModelElement info )
    {
        Properties defaultBundles = config.getProps( null, BldConfig.P_BUNDLE_VERSION );
        Map<String, Map<String, String>> fragments = new TreeMap<String, Map<String, String>>();
        IRequiredBundle fragment = info.getFragmentHost();
        if ( fragment != null )
        {
            Map<String, String> map2 = new TreeMap<String, String>();
            String name = fragment.getSymbolicName();
            VersionRange versions = defaultVersion( fragment.getVersions(), defaultBundles.getProperty( name ) );
            if ( versions != null )
                map2.put( BldAttr.VERSION_ATTRIBUTE, versions.toString() );
            fragments.put( name, map2 );
        }
        if ( !fragments.isEmpty() || !config.getMap( id, BldConfig.M_FRAGMENT ).isEmpty() )
        {
            config.setMap( id, BldConfig.M_FRAGMENT, fragments );
        }
    }


    /**
     * @param id
     * @param bundleVersion
     * @param info
     */
    private void setRequires( String id, String bundleVersion, IBundleModelElement info )
    {
        // requires
        Properties defaultBundles = config.getProps( null, BldConfig.P_BUNDLE_VERSION );
        Map<String, Map<String, String>> requires = new TreeMap<String, Map<String, String>>();

        for ( IRequiredBundle require : info.getRequiredBundles() )
        {
            Map<String, String> map2 = new TreeMap<String, String>();
            String name = require.getSymbolicName();
            VersionRange versions = defaultVersion( require.getVersions(), defaultBundles.getProperty( name ) );
            if ( versions != null )
                map2.put( BldAttr.VERSION_ATTRIBUTE, versions.toString() );
            requires.put( name, map2 );
        }
        if ( !requires.isEmpty() || !config.getMap( id, BldConfig.M_REQUIRES ).isEmpty() )
        {
            config.setMap( id, BldConfig.M_REQUIRES, requires );
        }
    }


    /**
     * @param bundleVersion 
     * @param info
     * @param exports 
     */
    private void setImports( String id, String bundleVersion, IBundleModelElement info, Map<String, Map<String, String>> exports )
    {
        // imports
        Map<String, Map<String, String>> imports = new TreeMap<String, Map<String, String>>();

        // FIXME: default version logic is wrong here
        //    if the version to be saved is the same as the default version,
        //    then we should _remove_ the version from the value being saved,
        //    since config.getMap() does not apply default versions.
        for ( IPackageImport import1 : info.getImports() )
        {
            Map<String, String> map2 = new TreeMap<String, String>();
            String name = import1.getPackageName();
            VersionRange versions = defaultVersion( import1.getVersions(), getDefaultPackageVersion( name ) );

            boolean isDependency = import1.isDependency();
            Map<String, String> selfImport = exports.get( name );

            if ( selfImport != null )
            {
                // avoid saving self-import attributes, e.g.
                // org.cauldron.newton.example.fractal.engine;resolve=auto;version=1.0.0
                isDependency = true;

                if ( versions != null )
                {
                    String exportVersion = selfImport.get( BldAttr.VERSION_ATTRIBUTE );
                    if ( exportVersion == null )
                        exportVersion = bundleVersion;

                    if ( exportVersion.equals( versions.toString() ) )
                    {
                        versions = null;
                    }
                }
            }

            if ( versions != null )
            {
                map2.put( BldAttr.VERSION_ATTRIBUTE, versions.toString() );
            }

            if ( import1.isOptional() )
            {
                map2.put( BldAttr.RESOLUTION_ATTRIBUTE, BldAttr.RESOLUTION_OPTIONAL );
            }

            String resolve = BldProject.getResolve( import1, isDependency );
            if ( resolve != null )
                map2.put( BldAttr.RESOLVE_ATTRIBUTE, resolve );

            imports.put( name, map2 );
        }
        if ( !imports.isEmpty() || !config.getMap( id, BldConfig.M_IMPORTS ).isEmpty() )
        {
            config.setMap( id, BldConfig.M_IMPORTS, imports );
        }
    }


    /**
     * @param id 
     * @param info 
     * @param bundleVersion 
     * @param exports 
     * 
     */
    private void setExports(String id, String bundleVersion, IBundleModelElement info, Map<String, Map<String, String>> exports)
    {
        for ( IPackageExport export : info.getExports() )
        {
            Map<String, String> map2 = new TreeMap<String, String>();
            String version = export.getVersion().toString();
            if ( !version.equals( bundleVersion ) )
                map2.put( BldAttr.VERSION_ATTRIBUTE, version );
            exports.put( export.getPackageName(), map2 );
        }

        if ( !exports.isEmpty() || !config.getMap( id, BldConfig.M_EXPORTS ).isEmpty() )
        {
            config.setMap( id, BldConfig.M_EXPORTS, exports );
        }
    }


    private void setHeader( Properties headers, String id, String key, String value )
    {
        if ( value == null )
            value = "";
        if ( !value.equals( headers.getProperty( key, "" ) ) )
            config.setProp( id, BldConfig.P_HEADER, key, value );
    }
}
