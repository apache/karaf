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
import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.*;
import org.osgi.framework.*;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
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
        final RequestInfo reqInfo = new RequestInfo(request);
        if ( reqInfo.bundle == null && reqInfo.bundleRequested ) {
            response.sendError(404);
            return;
        }
        if ( reqInfo.extension.equals("json")  )
        {
            this.renderJSON(response, reqInfo.bundle);

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }


    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        final RequestInfo reqInfo = new RequestInfo(req);
        if ( reqInfo.bundle == null && reqInfo.bundleRequested ) {
            resp.sendError(404);
            return;
        }

        boolean success = false;

        final String action = req.getParameter( "action" );

        Bundle bundle = getBundle( req.getPathInfo() );

        if ( bundle != null )
        {
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
            else if ( "refresh".equals( action ) )
            {
                // refresh bundle wiring
                refresh( bundle );
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
            getPackageAdmin().refreshPackages( null );
            success = true;
        }

        if ( success )
        {
            // let's wait a little bit to give the framework time
            // to process our request
            try {
                Thread.sleep(800);
            } catch (InterruptedException e) {
                // we ignore this
            }
            this.renderJSON(resp, null);
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
        try
        {
            final long bundleId = Long.parseLong( pathInfo );
            if ( bundleId >= 0 )
            {
                return getBundleContext().getBundle( bundleId );
            }
        }
        catch ( NumberFormatException nfe )
        {
            // check if this follows the pattern {symbolic-name}[:{version}]
            final int pos = pathInfo.indexOf(':');
            final String symbolicName;
            final String version;
            if ( pos == -1 ) {
                symbolicName = pathInfo;
                version = null;
            } else {
                symbolicName = pathInfo.substring(0, pos);
                version = pathInfo.substring(pos+1);
            }

            // search
            final Bundle[] bundles = getBundleContext().getBundles();
            for(int i=0; i<bundles.length; i++)
            {
                final Bundle bundle = bundles[i];
                // check symbolic name first
                if ( symbolicName.equals(bundle.getSymbolicName()) )
                {
                    if ( version == null || version.equals(bundle.getHeaders().get(Constants.BUNDLE_VERSION)) )
                    {
                        return bundle;
                    }
                }
            }
        }


        return null;
    }


    private void appendBundleInfoCount( final StringBuffer buf, String msg, int count )
    {
        buf.append(count);
        buf.append(" bundle");
        if ( count != 1 )
            buf.append( 's' );
        buf.append(' ');
        buf.append(msg);
    }

    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);
        final PrintWriter pw = response.getWriter();

        final String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        Util.script(pw, appRoot, "bundles.js");

        Util.startScript( pw );
        pw.println( "var imgRoot = '" + appRoot + "/res/imgs';");
        pw.println( "var startLevel = " + getStartLevel().getInitialBundleStartLevel() + ";");
        pw.println( "var drawDetails = " + reqInfo.bundleRequested + ";");
        Util.endScript( pw );

        Util.script(pw, appRoot, "bundles.js");

        pw.println( "<div id='plugin_content'/>");
        Util.startScript( pw );
        pw.print( "renderBundles(");
        writeJSON(pw, reqInfo.bundle);
        pw.println(");" );
        Util.endScript( pw );
    }

    private void renderJSON( final HttpServletResponse response, final Bundle bundle ) throws IOException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON(pw, bundle);
    }

    private void writeJSON( final PrintWriter pw, final Bundle bundle) throws IOException
    {
        final Bundle[] allBundles = this.getBundles();
        final String statusLine = this.getStatusLine(allBundles);
        final Bundle[] bundles = ( bundle != null ) ? new Bundle[]
            { bundle } : allBundles;
        Util.sort( bundles );

        final JSONWriter jw = new JSONWriter( pw );

        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            jw.key( "data" );

            jw.array();

            for ( int i = 0; i < bundles.length; i++ )
            {
                bundleInfo( jw, bundles[i], bundle != null );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }

    private String getStatusLine(final Bundle[] bundles)
    {
        int active = 0, installed = 0, resolved = 0;
        for ( int i = 0; i < bundles.length; i++ )
        {
            switch ( bundles[i].getState() )
            {
                case Bundle.ACTIVE:
                    active++;
                    break;
                case Bundle.INSTALLED:
                    installed++;
                    break;
                case Bundle.RESOLVED:
                    resolved++;
                    break;
            }
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Bundle information: ");
        appendBundleInfoCount(buffer, "in total", bundles.length);
        if ( active == bundles.length )
        {
            buffer.append(" - all ");
            appendBundleInfoCount(buffer, "active.", bundles.length);
        }
        else
        {
            if ( active != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "active", active);
            }
            if ( resolved != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "resolved", resolved);
            }
            if ( installed != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "installed", installed);
            }
            buffer.append('.');
        }
        return buffer.toString();
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

        if ( bundle.getBundleId() != 0 )
        {
            if ( hasStart(bundle) )
            {
                action( jw, hasStart( bundle ), "start", "Start", "start" );
            }
            else
            {
                action( jw, hasStop( bundle ), "stop", "Stop", "stop" );
            }
            action( jw, true, "refresh", "Refresh Package Imports", "refresh" );
            action( jw, hasUninstall( bundle ), "uninstall", "Uninstall", "delete" );
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


    private void action( JSONWriter jw, boolean enabled, String op, String opLabel, String image ) throws JSONException
    {
        jw.object();
        jw.key( "enabled" ).value( enabled );
        jw.key( "name" ).value( opLabel );
        jw.key( "link" ).value( op );
        jw.key( "image" ).value( image );
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


    private boolean hasUninstall( Bundle bundle )
    {
        return bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED
            || bundle.getState() == Bundle.ACTIVE;

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

            JSONArray val = new JSONArray();
            for ( int j = 0; j < exports.length; j++ )
            {
                ExportedPackage export = exports[j];
                collectExport( val, export.getName(), export.getVersion() );
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
            keyVal( jw, "Exported Packages", val );
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
            JSONArray val = new JSONArray();
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
                    collectImport( val, ep.getName(), ep.getVersion(), false, ep );
                }
            }
            else
            {
                // add description if there are no imports
                val.put( "None" );
            }

            keyVal( jw, "Imported Packages", val );
        }

        if ( !usingBundles.isEmpty() )
        {
            JSONArray val = new JSONArray();
            for ( Iterator ui = usingBundles.values().iterator(); ui.hasNext(); )
            {
                Bundle usingBundle = ( Bundle ) ui.next();
                val.put( getBundleDescriptor( usingBundle ) );
            }
            keyVal( jw, "Importing Bundles", val );
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

                JSONArray val = new JSONArray();
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    R4Export export = new R4Export( pkgs[i] );
                    collectExport( val, export.getName(), export.getVersion() );
                }
                keyVal( jw, "Exported Packages", val );
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
                JSONArray val = new JSONArray();
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

                        collectImport( val, r4Import.getName(), r4Import.getVersion(), r4Import.isOptional(), ep );
                    }
                }
                else
                {
                    // add description if there are no imports
                    val.put( "None" );
                }

                keyVal( jw, "Imported Packages", val );
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

            JSONArray val = new JSONArray();

            appendProperty( val, refs[i], Constants.OBJECTCLASS, "Types" );
            appendProperty( val, refs[i], Constants.SERVICE_PID, "PID" );
            appendProperty( val, refs[i], ConfigurationAdmin.SERVICE_FACTORYPID, "Factory PID" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_NAME, "Component Name" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_ID, "Component ID" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_FACTORY, "Component Factory" );
            appendProperty( val, refs[i], Constants.SERVICE_DESCRIPTION, "Description" );
            appendProperty( val, refs[i], Constants.SERVICE_VENDOR, "Vendor" );

            keyVal( jw, key, val);
        }
    }


    private void appendProperty( JSONArray array, ServiceReference ref, String name, String label )
    {
        StringBuffer dest = new StringBuffer();
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
            array.put(dest.toString());
        }
        else if ( value != null )
        {
            dest.append( label ).append( ": " ).append( value );
            array.put(dest.toString());
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


    private void collectExport( JSONArray array, String name, Version version )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );
        if ( bootDel )
        {
            val.append( "!! " );
        }

        val.append( name );
        val.append( ",version=" );
        val.append( version );

        if ( bootDel )
        {
            val.append( " -- Overwritten by Boot Delegation" );
        }

        array.put(val.toString());
    }


    private void collectImport( JSONArray array, String name, Version version, boolean optional, ExportedPackage export )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );

        String marker = null;
        val.append( name );
        val.append( ",version=" ).append( version );
        val.append( " from " );

        if ( export != null )
        {
            val.append( getBundleDescriptor( export.getExportingBundle() ) );

            if ( bootDel )
            {
                val.append( " -- Overwritten by Boot Delegation" );
                marker = "INFO";
            }
        }
        else
        {
            val.append( " -- Cannot be resolved" );
            marker = "ERROR";

            if ( optional )
            {
                val.append( " but is not required" );
            }

            if ( bootDel )
            {
                val.append( " and overwritten by Boot Delegation" );
            }
        }

        if ( marker != null ) {
            val.insert(0, ": ");
            val.insert(0, marker);
        }

        array.put(val);
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


    private void refresh( final Bundle bundle )
    {
        getPackageAdmin().refreshPackages( new Bundle[]
            { bundle } );
    }

    private final class RequestInfo
    {
        public final String extension;
        public final Bundle bundle;
        public final boolean bundleRequested;

        protected RequestInfo( final HttpServletRequest request )
        {
            String info = request.getPathInfo();
            // remove label and starting slash
            info = info.substring(getLabel().length() + 1);

            // get extension
            if ( info.endsWith(".json") )
            {
                extension = "json";
                info = info.substring(0, info.length() - 5);
            }
            else
            {
                extension = "html";
            }

            // we only accept direct requests to a bundle if they have a slash after the label
            String bundleInfo = null;
            if (info.startsWith("/") )
            {
                bundleInfo = info.substring(1);
            }
            if ( bundleInfo == null )
            {
                bundle = null;
                bundleRequested = false;
            }
            else
            {
                bundle = getBundle(bundleInfo);
                bundleRequested = true;
            }
            request.setAttribute(BundlesServlet.class.getName(), this);
        }

    }

    public static RequestInfo getRequestInfo(final HttpServletRequest request)
    {
        return (RequestInfo)request.getAttribute(BundlesServlet.class.getName());
    }
}
