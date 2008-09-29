/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.webconsole.internal.core;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.bundlerepository.*;
import org.apache.felix.shell.impl.UpdateCommandImpl;
import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.obr.DeployerThread;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
import org.osgi.service.obr.*;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


/**
 * The <code>BundlesServlet</code> TODO
 */
public class BundlesServlet extends BaseWebConsolePlugin
{

    public static final String NAME = "bundles";

    public static final String LABEL = "Bundles";

    public static final String BUNDLE_ID = "bundleId";

    private static final String REPOSITORY_ADMIN_NAME = RepositoryAdmin.class.getName();

    // bootdelegation property entries. wildcards are converted to package
    // name prefixes. whether an entry is a wildcard or not is set as a flag
    // in the bootPkgWildcards array.
    // see #activate and #isBootDelegated
    private String[] bootPkgs;

    // a flag for each entry in bootPkgs indicating whether the respective
    // entry was declared as a wildcard or not
    // see #activate and #isBootDelegated
    private boolean[] bootPkgWildcards;


    public void activate( BundleContext bundleContext )
    {
        super.activate( bundleContext );

        // bootdelegation property parsing from Apache Felix R4SearchPolicyCore
        String bootDelegation = bundleContext.getProperty( Constants.FRAMEWORK_BOOTDELEGATION );
        bootDelegation = ( bootDelegation == null ) ? "java.*" : bootDelegation + ",java.*";
        StringTokenizer st = new StringTokenizer( bootDelegation, " ," );
        bootPkgs = new String[st.countTokens()];
        bootPkgWildcards = new boolean[bootPkgs.length];
        for ( int i = 0; i < bootPkgs.length; i++ )
        {
            bootDelegation = st.nextToken();
            if ( bootDelegation.endsWith( "*" ) )
            {
                bootPkgWildcards[i] = true;
                bootDelegation = bootDelegation.substring( 0, bootDelegation.length() - 1 );
            }
            bootPkgs[i] = bootDelegation;
        }
    }


    public String getLabel()
    {
        return NAME;
    }


    public String getTitle()
    {
        return LABEL;
    }


    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {

        String info = request.getPathInfo();
        if ( info.endsWith( ".json" ) )
        {
            info = info.substring( 0, info.length() - 5 );
            if ( getLabel().equals( info.substring( 1 ) ) )
            {
                // should return info on all bundles
            }
            else
            {
                Bundle bundle = getBundle( info );
                if ( bundle != null )
                {
                    // bundle properties

                    response.setContentType( "text/javascript" );
                    response.setCharacterEncoding( "UTF-8" );

                    PrintWriter pw = response.getWriter();
                    JSONWriter jw = new JSONWriter( pw );
                    try
                    {
                        performAction( jw, bundle );
                    }
                    catch ( JSONException je )
                    {
                        throw new IOException( je.toString() );
                    }
                }
            }

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }


    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        String action = req.getParameter( "action" );
        if ( "refreshPackages".equals( action ) )
        {
            getPackageAdmin().refreshPackages( null );
        }

        boolean success = false;
        Bundle bundle = getBundle( req.getPathInfo() );
        long bundleId = -1;

        if ( bundle != null )
        {
            bundleId = bundle.getBundleId();
            if ( action == null )
            {
                success = true;
            }
            else if ( "start".equals( action ) )
            {
                // start bundle
                success = true;
                try
                {
                    bundle.start();
                }
                catch ( BundleException be )
                {
                    getLog().log( LogService.LOG_ERROR, "Cannot start", be );
                }
            }
            else if ( "stop".equals( action ) )
            {
                // stop bundle
                success = true;
                try
                {
                    bundle.stop();
                }
                catch ( BundleException be )
                {
                    getLog().log( LogService.LOG_ERROR, "Cannot stop", be );
                }
            }
            else if ( "update".equals( action ) )
            {
                // update bundle
                update( bundle );
                success = true;
            }
            else if ( "uninstall".equals( action ) )
            {
                // uninstall bundle
                success = true;
                try
                {
                    bundle.uninstall();
                    bundle = null; // bundle has gone !
                }
                catch ( BundleException be )
                {
                    getLog().log( LogService.LOG_ERROR, "Cannot uninstall", be );
                }
            }
        }

        if ( "refreshPackages".equals( action ) )
        {
            success = true;
            getPackageAdmin().refreshPackages( null );

            // refresh completely
            bundle = null;
            bundleId = -1;
        }

        if ( success )
        {
            // redirect or 200
            resp.setStatus( HttpServletResponse.SC_OK );
            JSONWriter jw = new JSONWriter( resp.getWriter() );
            try
            {
                if ( bundle != null )
                {
                    bundleInfo( jw, bundle, true );
                }
                else if ( bundleId >= 0 )
                {
                    jw.object();
                    jw.key( "bundleId" );
                    jw.value( bundleId );
                    jw.endObject();
                }
                else
                {
                    jw.object();
                    jw.key( "reload" );
                    jw.value( true );
                    jw.endObject();
                }
            }
            catch ( JSONException je )
            {
                throw new IOException( je.toString() );
            }
        }
        else
        {
            super.doPost( req, resp );
        }
    }


    private Bundle getBundle( String pathInfo )
    {
        // only use last part of the pathInfo
        pathInfo = pathInfo.substring( pathInfo.lastIndexOf( '/' ) + 1 );

        // assume bundle Id
        long bundleId;
        try
        {
            bundleId = Long.parseLong( pathInfo );
        }
        catch ( NumberFormatException nfe )
        {
            bundleId = -1;
        }

        if ( bundleId >= 0 )
        {
            return getBundleContext().getBundle( bundleId );
        }

        return null;
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {

        PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/datatable.js' language='JavaScript'></script>" );
        pw.println( "<script src='" + appRoot + "/res/ui/bundles.js' language='JavaScript'></script>" );

        Util.startScript( pw );
        pw.println( "var bundleListData = " );
        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "startLevel" );
            jw.value( getStartLevel().getInitialBundleStartLevel() );

            jw.key( "numActions" );
            jw.value( 4 );

            Bundle bundle = getBundle( request.getPathInfo() );
            Bundle[] bundles = ( bundle != null ) ? new Bundle[]
                { bundle } : this.getBundles();
            boolean details = ( bundle != null );

            if ( bundles != null && bundles.length > 0 )
            {
                Util.sort( bundles );

                jw.key( "data" );

                jw.array();

                for ( int i = 0; i < bundles.length; i++ )
                {
                    bundleInfo( jw, bundles[i], details );
                }

                jw.endArray();

            }
            else
            {
                jw.key( "error" );
                jw.value( "No Bundles installed currently" );
            }

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

        pw.println( ";" );
        pw.println( "renderBundle( bundleListData );" );
        Util.endScript( pw );
    }


    private void bundleInfo( JSONWriter jw, Bundle bundle, boolean details ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( bundle.getBundleId() );
        jw.key( "name" );
        jw.value( Util.getName( bundle ) );
        jw.key( "state" );
        jw.value( toStateString( bundle.getState() ) );

        jw.key( "actions" );
        jw.array();

        if ( bundle.getBundleId() == 0 )
        {
            jw.value( false );
            jw.value( false );
            jw.value( false );
            jw.value( false );
        }
        else
        {
            action( jw, hasStart( bundle ), "start", "Start" );
            action( jw, hasStop( bundle ), "stop", "Stop" );
            action( jw, hasUpdate( bundle ), "update", "Update" );
            action( jw, hasUninstall( bundle ), "uninstall", "Uninstall" );
        }
        jw.endArray();

        if ( details )
        {
            bundleDetails( jw, bundle );
        }

        jw.endObject();
    }


    protected Bundle[] getBundles()
    {
        return getBundleContext().getBundles();
    }


    private String toStateString( int bundleState )
    {
        switch ( bundleState )
        {
            case Bundle.INSTALLED:
                return "Installed";
            case Bundle.RESOLVED:
                return "Resolved";
            case Bundle.STARTING:
                return "Starting";
            case Bundle.ACTIVE:
                return "Active";
            case Bundle.STOPPING:
                return "Stopping";
            case Bundle.UNINSTALLED:
                return "Uninstalled";
            default:
                return "Unknown: " + bundleState;
        }
    }


    private void action( JSONWriter jw, boolean enabled, String op, String opLabel ) throws JSONException
    {
        jw.object();
        jw.key( "enabled" );
        jw.value( enabled );
        jw.key( "name" );
        jw.value( opLabel );
        jw.key( "link" );
        jw.value( op );
        jw.endObject();
    }


    private boolean hasStart( Bundle bundle )
    {
        return bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED;
    }


    private boolean hasStop( Bundle bundle )
    {
        return bundle.getState() == Bundle.ACTIVE;
    }


    private boolean hasUpdate( Bundle bundle )
    {
        //        enabled = bundle.getState() != Bundle.UNINSTALLED && this.hasUpdates( bundle );

        // don't care for bundles with no symbolic name
        if ( bundle.getSymbolicName() == null )
        {
            return false;
        }

        // no updates if there is no installer service
        Object isObject = getService( REPOSITORY_ADMIN_NAME );
        if ( isObject == null )
        {
            return false;
        }

        Version bundleVersion = Version.parseVersion( ( String ) bundle.getHeaders().get( Constants.BUNDLE_VERSION ) );

        RepositoryAdmin repoAdmin = ( RepositoryAdmin ) isObject;
        Repository[] repositories = repoAdmin.listRepositories();
        for ( int i = 0; i < repositories.length; i++ )
        {
            Resource[] resources = repositories[i].getResources();
            for ( int j = 0; resources != null && j < resources.length; j++ )
            {
                Resource res = resources[j];
                if ( bundle.getSymbolicName().equals( res.getSymbolicName() ) )
                {
                    if ( res.getVersion().compareTo( bundleVersion ) > 0 )
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }


    private boolean hasUninstall( Bundle bundle )
    {
        return bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED
            || bundle.getState() == Bundle.ACTIVE;

    }


    private void performAction( JSONWriter jw, Bundle bundle ) throws JSONException
    {
        jw.object();
        jw.key( BUNDLE_ID );
        jw.value( bundle.getBundleId() );

        bundleDetails( jw, bundle );

        jw.endObject();
    }


    private void bundleDetails( JSONWriter jw, Bundle bundle ) throws JSONException
    {
        Dictionary headers = bundle.getHeaders();

        jw.key( "props" );
        jw.array();
        keyVal( jw, "Symbolic Name", bundle.getSymbolicName() );
        keyVal( jw, "Version", headers.get( Constants.BUNDLE_VERSION ) );
        keyVal( jw, "Location", bundle.getLocation() );
        keyVal( jw, "Last Modification", new Date( bundle.getLastModified() ) );

        String docUrl = ( String ) headers.get( Constants.BUNDLE_DOCURL );
        if ( docUrl != null )
        {
            docUrl = "<a href=\"" + docUrl + "\" target=\"_blank\">" + docUrl + "</a>";
            keyVal( jw, "Bundle Documentation", docUrl );
        }

        keyVal( jw, "Vendor", headers.get( Constants.BUNDLE_VENDOR ) );
        keyVal( jw, "Copyright", headers.get( Constants.BUNDLE_COPYRIGHT ) );
        keyVal( jw, "Description", headers.get( Constants.BUNDLE_DESCRIPTION ) );

        keyVal( jw, "Start Level", getStartLevel( bundle ) );
        
        keyVal( jw, "Bundle Classpath", headers.get( Constants.BUNDLE_CLASSPATH ) );
        
        if ( bundle.getState() == Bundle.INSTALLED )
        {
            listImportExportsUnresolved( jw, bundle );
        }
        else
        {
            listImportExport( jw, bundle );
        }

        listServices( jw, bundle );

        jw.endArray();
    }


    private Integer getStartLevel( Bundle bundle )
    {
        StartLevel sl = getStartLevel();
        return ( sl != null ) ? new Integer( sl.getBundleStartLevel( bundle ) ) : null;
    }


    private void listImportExport( JSONWriter jw, Bundle bundle ) throws JSONException
    {
        PackageAdmin packageAdmin = getPackageAdmin();
        if ( packageAdmin == null )
        {
            return;
        }

        Map usingBundles = new TreeMap();

        ExportedPackage[] exports = packageAdmin.getExportedPackages( bundle );
        if ( exports != null && exports.length > 0 )
        {
            // do alphabetical sort
            Arrays.sort( exports, new Comparator()
            {
                public int compare( ExportedPackage p1, ExportedPackage p2 )
                {
                    return p1.getName().compareTo( p2.getName() );
                }


                public int compare( Object o1, Object o2 )
                {
                    return compare( ( ExportedPackage ) o1, ( ExportedPackage ) o2 );
                }
            } );

            StringBuffer val = new StringBuffer();
            for ( int j = 0; j < exports.length; j++ )
            {
                ExportedPackage export = exports[j];
                printExport( val, export.getName(), export.getVersion() );
                Bundle[] ubList = export.getImportingBundles();
                if ( ubList != null )
                {
                    for ( int i = 0; i < ubList.length; i++ )
                    {
                        Bundle ub = ubList[i];
                        usingBundles.put( ub.getSymbolicName(), ub );
                    }
                }
            }
            keyVal( jw, "Exported Packages", val.toString() );
        }
        else
        {
            keyVal( jw, "Exported Packages", "None" );
        }

        exports = packageAdmin.getExportedPackages( ( Bundle ) null );
        if ( exports != null && exports.length > 0 )
        {
            // collect import packages first
            final List imports = new ArrayList();
            for ( int i = 0; i < exports.length; i++ )
            {
                final ExportedPackage ep = exports[i];
                final Bundle[] importers = ep.getImportingBundles();
                for ( int j = 0; importers != null && j < importers.length; j++ )
                {
                    if ( importers[j].getBundleId() == bundle.getBundleId() )
                    {
                        imports.add( ep );

                        break;
                    }
                }
            }
            // now sort
            StringBuffer val = new StringBuffer();
            if ( imports.size() > 0 )
            {
                final ExportedPackage[] packages = ( ExportedPackage[] ) imports.toArray( new ExportedPackage[imports
                    .size()] );
                Arrays.sort( packages, new Comparator()
                {
                    public int compare( ExportedPackage p1, ExportedPackage p2 )
                    {
                        return p1.getName().compareTo( p2.getName() );
                    }


                    public int compare( Object o1, Object o2 )
                    {
                        return compare( ( ExportedPackage ) o1, ( ExportedPackage ) o2 );
                    }
                } );
                // and finally print out
                for ( int i = 0; i < packages.length; i++ )
                {
                    ExportedPackage ep = packages[i];
                    printImport( val, ep.getName(), ep.getVersion(), false, ep );
                }
            }
            else
            {
                // add description if there are no imports
                val.append( "None" );
            }

            keyVal( jw, "Imported Packages", val.toString() );
        }

        if ( !usingBundles.isEmpty() )
        {
            StringBuffer val = new StringBuffer();
            for ( Iterator ui = usingBundles.values().iterator(); ui.hasNext(); )
            {
                Bundle usingBundle = ( Bundle ) ui.next();
                val.append( getBundleDescriptor( usingBundle ) );
                val.append( "<br />" );
            }
            keyVal( jw, "Importing Bundles", val.toString() );
        }
    }


    private void listImportExportsUnresolved( JSONWriter jw, Bundle bundle ) throws JSONException
    {
        Dictionary dict = bundle.getHeaders();

        String target = ( String ) dict.get( Constants.EXPORT_PACKAGE );
        if ( target != null )
        {
            R4Package[] pkgs = R4Package.parseImportOrExportHeader( target );
            if ( pkgs != null && pkgs.length > 0 )
            {
                // do alphabetical sort
                Arrays.sort( pkgs, new Comparator()
                {
                    public int compare( R4Package p1, R4Package p2 )
                    {
                        return p1.getName().compareTo( p2.getName() );
                    }


                    public int compare( Object o1, Object o2 )
                    {
                        return compare( ( R4Package ) o1, ( R4Package ) o2 );
                    }
                } );

                StringBuffer val = new StringBuffer();
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    R4Export export = new R4Export( pkgs[i] );
                    printExport( val, export.getName(), export.getVersion() );
                }
                keyVal( jw, "Exported Packages", val.toString() );
            }
            else
            {
                keyVal( jw, "Exported Packages", "None" );
            }
        }

        target = ( String ) dict.get( Constants.IMPORT_PACKAGE );
        if ( target != null )
        {
            R4Package[] pkgs = R4Package.parseImportOrExportHeader( target );
            if ( pkgs != null && pkgs.length > 0 )
            {
                Map imports = new TreeMap();
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    R4Package pkg = pkgs[i];
                    imports.put( pkg.getName(), new R4Import( pkg ) );
                }

                // collect import packages first
                final Map candidates = new HashMap();
                PackageAdmin packageAdmin = getPackageAdmin();
                if ( packageAdmin != null )
                {
                    ExportedPackage[] exports = packageAdmin.getExportedPackages( ( Bundle ) null );
                    if ( exports != null && exports.length > 0 )
                    {

                        for ( int i = 0; i < exports.length; i++ )
                        {
                            final ExportedPackage ep = exports[i];

                            R4Import imp = ( R4Import ) imports.get( ep.getName() );
                            if ( imp != null && imp.isSatisfied( toR4Export( ep ) ) )
                            {
                                candidates.put( ep.getName(), ep );
                            }
                        }
                    }
                }

                // now sort
                StringBuffer val = new StringBuffer();
                if ( imports.size() > 0 )
                {
                    for ( Iterator ii = imports.values().iterator(); ii.hasNext(); )
                    {
                        R4Import r4Import = ( R4Import ) ii.next();
                        ExportedPackage ep = ( ExportedPackage ) candidates.get( r4Import.getName() );

                        // if there is no matching export, check whether this
                        // bundle has the package, ignore the entry in this case
                        if ( ep == null )
                        {
                            String path = r4Import.getName().replace( '.', '/' );
                            if ( bundle.getResource( path ) != null )
                            {
                                continue;
                            }
                        }

                        printImport( val, r4Import.getName(), r4Import.getVersion(), r4Import.isOptional(), ep );
                    }
                }
                else
                {
                    // add description if there are no imports
                    val.append( "None" );
                }

                keyVal( jw, "Imported Packages", val.toString() );
            }
        }
    }


    private void listServices( JSONWriter jw, Bundle bundle ) throws JSONException
    {
        ServiceReference[] refs = bundle.getRegisteredServices();
        if ( refs == null || refs.length == 0 )
        {
            return;
        }

        for ( int i = 0; i < refs.length; i++ )
        {
            String key = "Service ID " + refs[i].getProperty( Constants.SERVICE_ID );

            StringBuffer val = new StringBuffer();

            appendProperty( val, refs[i], Constants.OBJECTCLASS, "Types" );
            appendProperty( val, refs[i], Constants.SERVICE_PID, "PID" );
            appendProperty( val, refs[i], ConfigurationAdmin.SERVICE_FACTORYPID, "Factory PID" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_NAME, "Component Name" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_ID, "Component ID" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_FACTORY, "Component Factory" );
            appendProperty( val, refs[i], Constants.SERVICE_DESCRIPTION, "Description" );
            appendProperty( val, refs[i], Constants.SERVICE_VENDOR, "Vendor" );

            keyVal( jw, key, val.toString() );
        }
    }


    private void appendProperty( StringBuffer dest, ServiceReference ref, String name, String label )
    {
        Object value = ref.getProperty( name );
        if ( value instanceof Object[] )
        {
            Object[] values = ( Object[] ) value;
            dest.append( label ).append( ": " );
            for ( int j = 0; j < values.length; j++ )
            {
                if ( j > 0 )
                    dest.append( ", " );
                dest.append( values[j] );
            }
            dest.append( "<br />" ); // assume HTML use of result
        }
        else if ( value != null )
        {
            dest.append( label ).append( ": " ).append( value ).append( "<br />" );
        }
    }


    private void keyVal( JSONWriter jw, String key, Object value ) throws JSONException
    {
        if ( key != null && value != null )
        {
            jw.object();
            jw.key( "key" );
            jw.value( key );
            jw.key( "value" );
            jw.value( value );
            jw.endObject();
        }
    }


    private void printExport( StringBuffer val, String name, Version version )
    {
        boolean bootDel = isBootDelegated( name );
        if ( bootDel )
        {
            val.append( "<span style=\"color: red\">!! " );
        }

        val.append( name );
        val.append( ",version=" );
        val.append( version );

        if ( bootDel )
        {
            val.append( " -- Overwritten by Boot Delegation</span>" );
        }

        val.append( "<br />" );
    }


    private void printImport( StringBuffer val, String name, Version version, boolean optional, ExportedPackage export )
    {
        boolean bootDel = isBootDelegated( name );
        boolean isSpan = bootDel || export == null;

        if ( isSpan )
        {
            val.append( "<span style=\"color: red\">!! " );
        }

        val.append( name );
        val.append( ",version=" ).append( version );
        val.append( " from " );

        if ( export != null )
        {
            val.append( getBundleDescriptor( export.getExportingBundle() ) );

            if ( bootDel )
            {
                val.append( " -- Overwritten by Boot Delegation" );
            }
        }
        else
        {
            val.append( " -- Cannot be resolved" );
            
            if ( optional )
            {
                val.append( " but is not required" );
            }

            if ( bootDel )
            {
                val.append( " and overwritten by Boot Delegation" );
            }
        }

        if ( isSpan )
        {
            val.append( "</span>" );
        }
        
        val.append( "<br />" );
    }


    // returns true if the package is listed in the bootdelegation property
    private boolean isBootDelegated( String pkgName )
    {

        // bootdelegation analysis from Apache Felix R4SearchPolicyCore

        // Only consider delegation if we have a package name, since
        // we don't want to promote the default package. The spec does
        // not take a stand on this issue.
        if ( pkgName.length() > 0 )
        {

            // Delegate any packages listed in the boot delegation
            // property to the parent class loader.
            for ( int i = 0; i < bootPkgs.length; i++ )
            {

                // A wildcarded boot delegation package will be in the form of
                // "foo.", so if the package is wildcarded do a startsWith() or
                // a regionMatches() to ignore the trailing "." to determine if
                // the request should be delegated to the parent class loader.
                // If the package is not wildcarded, then simply do an equals()
                // test to see if the request should be delegated to the parent
                // class loader.
                if ( ( bootPkgWildcards[i] && ( pkgName.startsWith( bootPkgs[i] ) || bootPkgs[i].regionMatches( 0,
                    pkgName, 0, pkgName.length() ) ) )
                    || ( !bootPkgWildcards[i] && bootPkgs[i].equals( pkgName ) ) )
                {
                    return true;
                }
            }
        }

        return false;
    }


    private R4Export toR4Export( ExportedPackage export )
    {
        R4Attribute version = new R4Attribute( Constants.VERSION_ATTRIBUTE, export.getVersion().toString(), false );
        return new R4Export( export.getName(), null, new R4Attribute[]
            { version } );
    }


    private String getBundleDescriptor( Bundle bundle )
    {
        StringBuffer val = new StringBuffer();
        if ( bundle.getSymbolicName() != null )
        {
            // list the bundle name if not null
            val.append( bundle.getSymbolicName() );
            val.append( " (" ).append( bundle.getBundleId() );
            val.append( ")" );
        }
        else if ( bundle.getLocation() != null )
        {
            // otherwise try the location
            val.append( bundle.getLocation() );
            val.append( " (" ).append( bundle.getBundleId() );
            val.append( ")" );
        }
        else
        {
            // fallback to just the bundle id
            // only append the bundle
            val.append( bundle.getBundleId() );
        }
        return val.toString();
    }


    private void update( final Bundle bundle )
    {
        final RepositoryAdmin repoAdmin = ( RepositoryAdmin ) getService( REPOSITORY_ADMIN_NAME );
        if ( repoAdmin != null && bundle.getSymbolicName() != null )
        {
            // current bundle version
            Version bundleVersion = Version
                .parseVersion( ( String ) bundle.getHeaders().get( Constants.BUNDLE_VERSION ) );

            // discover candidates for the update
            String filter = "(&(symbolicname=" + bundle.getSymbolicName() + ")(version>=" + bundleVersion + "))";
            Resource[] cand = repoAdmin.discoverResources( filter );

            // find the candidate with the highest version number
            Version base = bundleVersion;
            int idx = -1;
            for ( int i = 0; cand != null && i < cand.length; i++ )
            {
                if ( cand[i].getVersion().compareTo( base ) > 0 )
                {
                    base = cand[i].getVersion();
                    idx = i;
                }
            }

            // try to resolve and deploy the best candidate
            if ( idx >= 0 )
            {
                Resolver resolver = repoAdmin.resolver();
                resolver.add( cand[idx] );

                DeployerThread dt = new DeployerThread( resolver, getLog(), bundle.getState() == Bundle.ACTIVE,
                    "Update " + bundle.getSymbolicName() );
                dt.start();
            }
        }

    }
}
