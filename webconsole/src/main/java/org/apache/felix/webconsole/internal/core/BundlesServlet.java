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
import java.io.StringWriter;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.bundlerepository.impl.R4Attribute;
import org.apache.felix.bundlerepository.impl.R4Export;
import org.apache.felix.bundlerepository.impl.R4Import;
import org.apache.felix.bundlerepository.impl.R4Package;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.Logger;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.packageadmin.ExportedPackage;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;


/**
 * The <code>BundlesServlet</code> provides the bundles plugins, used to display
 * the list of bundles, installed on the framework. It also adds ability to control
 * the lifecycle of the bundles, like start, stop, uninstall, install.
 */
public class BundlesServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin, ConfigurationPrinter
{

    /** the label of the bundles plugin - used by other plugins to reference to plugin details */
    public static final String NAME = "bundles";
    private static final String TITLE = "Bundles";
    private static final String CSS[] = { "/res/ui/bundles.css" };

    // bootdelegation property entries. wildcards are converted to package
    // name prefixes. whether an entry is a wildcard or not is set as a flag
    // in the bootPkgWildcards array.
    // see #activate and #isBootDelegated
    private String[] bootPkgs;

    // a flag for each entry in bootPkgs indicating whether the respective
    // entry was declared as a wildcard or not
    // see #activate and #isBootDelegated
    private boolean[] bootPkgWildcards;

    private ServiceRegistration configurationPrinter;

    // templates
    private final String TEMPLATE_MAIN;
    private final String TEMPLATE_UPLOAD;

    /** Default constructor */
    public BundlesServlet()
    {
        super(NAME, TITLE, CSS);

        // load templates
        TEMPLATE_MAIN = readTemplateFile( "/templates/bundles.html" );
        TEMPLATE_UPLOAD = readTemplateFile( "/templates/bundles_upload.html" );
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#activate(org.osgi.framework.BundleContext)
     */
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

        configurationPrinter = bundleContext.registerService( ConfigurationPrinter.SERVICE, this, null );
    }


    /**
     * @see org.apache.felix.webconsole.SimpleWebConsolePlugin#deactivate()
     */
    public void deactivate()
    {
        if ( configurationPrinter != null )
        {
            configurationPrinter.unregister();
            configurationPrinter = null;
        }

        super.deactivate();
    }


    //---------- ConfigurationPrinter

    /**
     * @see org.apache.felix.webconsole.ConfigurationPrinter#printConfiguration(java.io.PrintWriter)
     */
    public void printConfiguration( PrintWriter pw )
    {
        try
        {
            StringWriter w = new StringWriter();
            writeJSON( w, null, null, null, true, Locale.ENGLISH );
            String jsonString = w.toString();
            JSONObject json = new JSONObject( jsonString );

            pw.println( "Status: " + json.get( "status" ) );
            pw.println();

            JSONArray data = json.getJSONArray( "data" );
            for ( int i = 0; i < data.length(); i++ )
            {
                if ( !data.isNull( i ) )
                {
                    JSONObject bundle = data.getJSONObject( i );

                    pw.println( MessageFormat.format( "Bundle {0} - {1} {2} (state: {3})", new Object[]
                        { bundle.get( "id" ), bundle.get( "name" ), bundle.get( "version" ), bundle.get( "state" ) } ) );

                    JSONArray props = bundle.getJSONArray( "props" );
                    for ( int pi = 0; pi < props.length(); pi++ )
                    {
                        if ( !props.isNull( pi ) )
                        {
                            JSONObject entry = props.getJSONObject( pi );

                            pw.print( "    " + entry.get( "key" ) + ": " );

                            Object entryValue = entry.get( "value" );
                            if ( entryValue instanceof JSONArray )
                            {
                                pw.println();
                                JSONArray entryArray = ( JSONArray ) entryValue;
                                for ( int ei = 0; ei < entryArray.length(); ei++ )
                                {
                                    if ( !entryArray.isNull( ei ) )
                                    {
                                        pw.println( "        " + entryArray.get( ei ) );
                                    }
                                }
                            }
                            else
                            {
                                pw.println( entryValue );
                            }
                        }
                    }

                    pw.println();
                }
            }
        }
        catch ( Exception e )
        {
            log( "Problem rendering Bundle details for configuration status", e );
        }
    }


    //---------- BaseWebConsolePlugin

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        final RequestInfo reqInfo = new RequestInfo(request);
        if ( "upload".equals(reqInfo.pathInfo) )
        {
            super.doGet(request, response);
            return;
        }
        if ( reqInfo.bundle == null && reqInfo.bundleRequested )
        {
            response.sendError(404);
            return;
        }
        if ( reqInfo.extension.equals("json")  )
        {
            final String pluginRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
            final String servicesRoot = getServicesRoot( request );
            this.renderJSON(response, reqInfo.bundle, pluginRoot, servicesRoot, request.getLocale());

            // nothing more to do
            return;
        }

        super.doGet( request, response );
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
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
                    log( "Cannot start", be );
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
                    log( "Cannot stop", be );
                }
            }
            else if ( "refresh".equals( action ) )
            {
                // refresh bundle wiring
                refresh( bundle );
                success = true;
            }
            else if ( "update".equals( action ) )
            {
                // update the bundle
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
                    log( "Cannot uninstall", be );
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
            final String pluginRoot = ( String ) req.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
            final String servicesRoot = getServicesRoot( req );
            this.renderJSON(resp, null, pluginRoot, servicesRoot, req.getLocale() );
        }
        else
        {
            super.doPost( req, resp );
        }
    }

    private String getServicesRoot(HttpServletRequest request)
    {
        return ( ( String ) request.getAttribute( WebConsoleConstants.ATTR_APP_ROOT ) ) +
            "/" + ServicesServlet.LABEL + "/";
    }

    Bundle getBundle( String pathInfo )
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

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);

        final int startLevel = getStartLevel().getInitialBundleStartLevel();

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "startLevel", String.valueOf(startLevel));
        vars.put( "drawDetails", reqInfo.bundleRequested ? Boolean.TRUE : Boolean.FALSE );
        vars.put( "currentBundle", (reqInfo.bundleRequested && reqInfo.bundle != null ? String.valueOf(reqInfo.bundle.getBundleId()) : "null"));

        if ( "upload".equals(reqInfo.pathInfo) )
        {
            response.getWriter().print(TEMPLATE_UPLOAD);
        }
        else
        {
            final String pluginRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
            final String servicesRoot = getServicesRoot ( request );
            StringWriter w = new StringWriter();
            writeJSON(w, reqInfo.bundle, pluginRoot, servicesRoot, request.getLocale() );
            vars.put( "__bundles__", w.toString());

            response.getWriter().print(TEMPLATE_MAIN);
        }
    }

    private void renderJSON( final HttpServletResponse response, final Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale )
        throws IOException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON(pw, bundle, pluginRoot, servicesRoot, locale);
    }


    private void writeJSON( final Writer pw, final Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale )
        throws IOException
    {
        writeJSON( pw, bundle, pluginRoot, servicesRoot, false, locale );
    }


    private void writeJSON( final Writer pw, final Bundle bundle, final String pluginRoot,
        final String servicesRoot, final boolean fullDetails, final Locale locale ) throws IOException
    {
        final Bundle[] allBundles = this.getBundles();
        final Object[] status = getStatusLine(allBundles);
        final String statusLine = (String) status[5];
        final Bundle[] bundles = ( bundle != null ) ? new Bundle[]
            { bundle } : allBundles;
        Util.sort( bundles, locale );

        final JSONWriter jw = new JSONWriter( pw );

        try
        {
            jw.object();

            jw.key( "status" );
            jw.value( statusLine );

            // add raw status
            jw.key( "s" );
            jw.array();
            for ( int i = 0; i < 5; i++ ) jw.value(status[i]);
            jw.endArray();

            jw.key( "data" );

            jw.array();

            for ( int i = 0; i < bundles.length; i++ )
            {
                bundleInfo( jw, bundles[i], fullDetails || bundle != null, pluginRoot, servicesRoot, locale );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

    }

    private Object[] getStatusLine(final Bundle[] bundles)
    {
        Object[] ret = new Object[6];
        int active = 0, installed = 0, resolved = 0, fragments = 0;
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
                    if ( isFragmentBundle( bundles[i] ) )
                    {
                        fragments++;
                    }
                    else
                    {
                        resolved++;
                    }
                    break;
            }
        }
        final StringBuffer buffer = new StringBuffer();
        buffer.append("Bundle information: ");
        appendBundleInfoCount(buffer, "in total", bundles.length);
        if ( active == bundles.length || active + fragments == bundles.length )
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
            if ( fragments != 0 )
            {
                buffer.append(", ");
                appendBundleInfoCount(buffer, "active fragments", fragments);
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
        ret[0] = new Integer(bundles.length);
        ret[1] = new Integer(active);
        ret[2] = new Integer(fragments);
        ret[3] = new Integer(resolved);
        ret[4] = new Integer(installed);
        ret[5] = buffer.toString();
        return ret;
    }

    private void bundleInfo( JSONWriter jw, Bundle bundle, boolean details, final String pluginRoot, final String servicesRoot, final Locale locale )
        throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( bundle.getBundleId() );
        jw.key( "name" );
        jw.value( Util.getName( bundle, locale ) );
        jw.key( "state" );
        jw.value( toStateString( bundle ) );
        jw.key( "version" );
        jw.value( Util.getHeaderValue(bundle, Constants.BUNDLE_VERSION) );
        jw.key( "symbolicName" );
        jw.value( Util.getHeaderValue(bundle, Constants.BUNDLE_SYMBOLICNAME) );

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
            action( jw, true, "update", "Update", "update" );
            action( jw, hasUninstall( bundle ), "uninstall", "Uninstall", "delete" );
        }
        jw.endArray();

        if ( details )
        {
            bundleDetails( jw, bundle, pluginRoot, servicesRoot, locale );
        }

        jw.endObject();
    }


    private final Bundle[] getBundles()
    {
        return getBundleContext().getBundles();
    }


    private String toStateString( final Bundle bundle )
    {
        switch ( bundle.getState() )
        {
            case Bundle.INSTALLED:
                return "Installed";
            case Bundle.RESOLVED:
                if ( isFragmentBundle(bundle) )
                {
                    return "Fragment";
                }
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
                return "Unknown: " + bundle.getState();
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

    private final boolean isFragmentBundle( Bundle bundle)
    {
        return getPackageAdmin().getBundleType( bundle ) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
    }

    private final boolean hasStart( Bundle bundle )
    {
        if ( isFragmentBundle(bundle) )
        {
            return false;
        }
        return bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED;
    }


    private final boolean hasStop( Bundle bundle )
    {
        if ( isFragmentBundle(bundle) )
        {
            return false;
        }
        return bundle.getState() == Bundle.ACTIVE;
    }


    private static final boolean hasUninstall( Bundle bundle )
    {
        return bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED
            || bundle.getState() == Bundle.ACTIVE;

    }


    private final void bundleDetails( JSONWriter jw, Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale)
        throws JSONException
    {
        Dictionary headers = bundle.getHeaders( locale == null ? null : locale.toString() );

        jw.key( "props" );
        jw.array();
        WebConsoleUtil.keyVal( jw, "Symbolic Name", bundle.getSymbolicName() );
        WebConsoleUtil.keyVal( jw, "Version", headers.get( Constants.BUNDLE_VERSION ) );
        WebConsoleUtil.keyVal( jw, "Bundle Location", bundle.getLocation() );
        WebConsoleUtil.keyVal( jw, "Last Modification", new Date( bundle.getLastModified() ) );

        String docUrl = ( String ) headers.get( Constants.BUNDLE_DOCURL );
        if ( docUrl != null )
        {
            WebConsoleUtil.keyVal( jw, "Bundle Documentation", docUrl );
        }

        WebConsoleUtil.keyVal( jw, "Vendor", headers.get( Constants.BUNDLE_VENDOR ) );
        WebConsoleUtil.keyVal( jw, "Copyright", headers.get( Constants.BUNDLE_COPYRIGHT ) );
        WebConsoleUtil.keyVal( jw, "Description", headers.get( Constants.BUNDLE_DESCRIPTION ) );

        WebConsoleUtil.keyVal( jw, "Start Level", getStartLevel( bundle ) );

        WebConsoleUtil.keyVal( jw, "Bundle Classpath", headers.get( Constants.BUNDLE_CLASSPATH ) );

        listFragmentInfo( jw, bundle, pluginRoot );

        if ( bundle.getState() == Bundle.INSTALLED )
        {
            listImportExportsUnresolved( jw, bundle, pluginRoot );
        }
        else
        {
            listImportExport( jw, bundle, pluginRoot );
        }

        listServices( jw, bundle, servicesRoot );

        listHeaders( jw, bundle );

        jw.endArray();
    }


    private final Integer getStartLevel( Bundle bundle )
    {
        StartLevel sl = getStartLevel();
        return ( sl != null ) ? new Integer( sl.getBundleStartLevel( bundle ) ) : null;
    }


    private void listImportExport( JSONWriter jw, Bundle bundle, final String pluginRoot ) throws JSONException
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
                        String name = ub.getSymbolicName();
                        if (name == null) name = ub.getLocation();
                        usingBundles.put( name, ub );
                    }
                }
            }
            WebConsoleUtil.keyVal( jw, "Exported Packages", val );
        }
        else
        {
            WebConsoleUtil.keyVal( jw, "Exported Packages", "---" );
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
                    collectImport( val, ep.getName(), ep.getVersion(), false, ep, pluginRoot );
                }
            }
            else
            {
                // add description if there are no imports
                val.put( "None" );
            }

            WebConsoleUtil.keyVal( jw, "Imported Packages", val );
        }

        if ( !usingBundles.isEmpty() )
        {
            JSONArray val = new JSONArray();
            for ( Iterator ui = usingBundles.values().iterator(); ui.hasNext(); )
            {
                Bundle usingBundle = ( Bundle ) ui.next();
                val.put( getBundleDescriptor( usingBundle, pluginRoot ) );
            }
            WebConsoleUtil.keyVal( jw, "Importing Bundles", val );
        }
    }


    private void listImportExportsUnresolved( JSONWriter jw, Bundle bundle, final String pluginRoot ) throws JSONException
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
                WebConsoleUtil.keyVal( jw, "Exported Packages", val );
            }
            else
            {
                WebConsoleUtil.keyVal( jw, "Exported Packages", "---" );
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
                            if ( bundle.getEntry( path ) != null )
                            {
                                continue;
                            }
                        }

                        collectImport( val, r4Import.getName(), r4Import.getVersion(), r4Import.isOptional(), ep, pluginRoot );
                    }
                }
                else
                {
                    // add description if there are no imports
                    val.put( "---" );
                }

                WebConsoleUtil.keyVal( jw, "Imported Packages", val );
            }
        }
    }

    private String getServiceID(ServiceReference ref, final String servicesRoot) {
        String id = ref.getProperty( Constants.SERVICE_ID ).toString();
        StringBuffer val = new StringBuffer();

        if ( servicesRoot != null ) {
            val.append( "<a href='" ).append( servicesRoot ).append( id ).append( "'>" );
            val.append( id );
            val.append( "</a>" );
            return val.toString();
        } else {
            return id;
        }
    }


    private void listServices( JSONWriter jw, Bundle bundle, final String servicesRoot ) throws JSONException
    {
        ServiceReference[] refs = bundle.getRegisteredServices();
        if ( refs == null || refs.length == 0 )
        {
            return;
        }

        for ( int i = 0; i < refs.length; i++ )
        {


            String key = "Service ID " + getServiceID( refs[i], servicesRoot );

            JSONArray val = new JSONArray();

            appendProperty( val, refs[i], Constants.OBJECTCLASS, "Types" );
            appendProperty( val, refs[i], Constants.SERVICE_PID, "Service PID" );
            appendProperty( val, refs[i], "org.apache.felix.karaf.features.configKey", "Feature PID" );
            appendProperty( val, refs[i], ConfigurationAdmin.SERVICE_FACTORYPID, "Factory PID" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_NAME, "Component Name" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_ID, "Component ID" );
            appendProperty( val, refs[i], ComponentConstants.COMPONENT_FACTORY, "Component Factory" );
            appendProperty( val, refs[i], Constants.SERVICE_DESCRIPTION, "Description" );
            appendProperty( val, refs[i], Constants.SERVICE_VENDOR, "Vendor" );

            WebConsoleUtil.keyVal( jw, key, val);
        }
    }


    private void listHeaders( JSONWriter jw, Bundle bundle ) throws JSONException
    {
        JSONArray val = new JSONArray();

        Dictionary headers = bundle.getHeaders();
        Enumeration he = headers.keys();
        while ( he.hasMoreElements() )
        {
            Object header = he.nextElement();
            String value = String.valueOf(headers.get( header ));
            // Package headers may be long, support line breaking by
            // ensuring blanks after comma and semicolon.
            value = enableLineWrapping(value);
            val.put( header + ": " + value );
        }

        WebConsoleUtil.keyVal( jw, "Manifest Headers", val );
    }

    private static final String enableLineWrapping(final String value)
    {
        StringBuffer sb = new StringBuffer(value.length() * 2 / 3);
        synchronized (sb)
        { // faster
            for (int i = 0; i < value.length(); i++)
            {
                final char ch = value.charAt( i );
                sb.append( ch );
                if ( ch == ';' || ch == ',' )
                {
                    sb.append( ' ' );
                }
            }
            return sb.toString();
        }
    }

    private void listFragmentInfo( final JSONWriter jw, final Bundle bundle, final String pluginRoot )
        throws JSONException
    {

        if ( isFragmentBundle( bundle ) )
        {
            Bundle[] hostBundles = getPackageAdmin().getHosts( bundle );
            if ( hostBundles != null )
            {
                JSONArray val = new JSONArray();
                for ( int i = 0; i < hostBundles.length; i++ )
                {
                    val.put( getBundleDescriptor( hostBundles[i], pluginRoot ) );
                }
                WebConsoleUtil.keyVal( jw, "Host Bundles", val );
            }
        }
        else
        {
            Bundle[] fragmentBundles = getPackageAdmin().getFragments( bundle );
            if ( fragmentBundles != null )
            {
                JSONArray val = new JSONArray();
                for ( int i = 0; i < fragmentBundles.length; i++ )
                {
                    val.put( getBundleDescriptor( fragmentBundles[i], pluginRoot ) );
                }
                WebConsoleUtil.keyVal( jw, "Fragments Attached", val );
            }
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


    private void collectImport( JSONArray array, String name, Version version, boolean optional,
            ExportedPackage export, final String pluginRoot )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );

        String marker = null;
        val.append( name );
        val.append( ",version=" ).append( version );

        if ( export != null )
        {
            val.append( " from " );
            val.append( getBundleDescriptor( export.getExportingBundle(), pluginRoot ) );

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


    private String getBundleDescriptor( Bundle bundle, final String pluginRoot )
    {
        StringBuffer val = new StringBuffer();

        if ( pluginRoot != null )
        {
            val.append( "<a href='" ).append( pluginRoot ).append( '/' ).append( bundle.getBundleId() ).append( "'>" );
        }

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
        if ( pluginRoot != null )
        {
            val.append( "</a>" );
        }
        return val.toString();
    }


    private void refresh( final Bundle bundle )
    {
        getPackageAdmin().refreshPackages( new Bundle[]
            { bundle } );
    }


    private void update( final Bundle bundle )
    {
        UpdateHelper t = new UpdateHelper( bundle, false )
        {
            protected Logger getLog()
            {
                return BundlesServlet.this.getLog();
            }


            protected Object getService( String serviceName )
            {
                return BundlesServlet.this.getService( serviceName );
            }
        };

        t.start();
    }

    private final class RequestInfo
    {
        public final String extension;
        public final Bundle bundle;
        public final boolean bundleRequested;
        public final String pathInfo;

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
            if ( bundleInfo == null || bundleInfo.length() == 0 )
            {
                bundle = null;
                bundleRequested = false;
                pathInfo = null;
            }
            else
            {
                bundle = getBundle(bundleInfo);
                bundleRequested = true;
                pathInfo = bundleInfo;
            }
            request.setAttribute(BundlesServlet.class.getName(), this);
        }

    }

    static final RequestInfo getRequestInfo(final HttpServletRequest request)
    {
        return (RequestInfo)request.getAttribute( BundlesServlet.class.getName() );
    }

    private final PackageAdmin getPackageAdmin()
    {
        return ( PackageAdmin ) getService( PackageAdmin.class.getName() );
    }

    private final StartLevel getStartLevel()
    {
        return ( StartLevel ) getService( StartLevel.class.getName() );
    }

    // TODO: may remove later, when BaseWebConsolePlugin is made to extend SimpleWebConsolePlugin
    private Logger log;
    Logger getLog()
    {
        if ( log == null )
        {
            log = new Logger( getBundleContext() );
        }

        return log;
    }
}
