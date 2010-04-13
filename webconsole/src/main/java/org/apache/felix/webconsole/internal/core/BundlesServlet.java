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


import java.io.File;
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
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.framework.util.VersionRange;
import org.apache.felix.utils.manifest.Clause;
import org.apache.felix.utils.manifest.Parser;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.ConfigurationPrinter;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.apache.felix.webconsole.WebConsoleUtil;
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
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentConstants;
import org.osgi.service.log.LogService;
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
    private static final String TITLE = "%bundles.pluginTitle";
    private static final String CSS[] = { "/res/ui/bundles.css" };

    // an LDAP filter, that is used to search manifest headers, see FELIX-1441
    private static final String FILTER_PARAM = "filter";

    private static final String FIELD_STARTLEVEL = "bundlestartlevel";

    private static final String FIELD_START = "bundlestart";

    private static final String FIELD_BUNDLEFILE = "bundlefile";

    // set to ask for PackageAdmin.refreshPackages() after install/update
    private static final String FIELD_REFRESH_PACKAGES = "refreshPackages";

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

    /** Default constructor */
    public BundlesServlet()
    {
        super(NAME, TITLE, CSS);

        // load templates
        TEMPLATE_MAIN = readTemplateFile( "/templates/bundles.html" );
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
            writeJSON( w, null, null, null, true, Locale.ENGLISH, null, null );
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
            try
            {
                this.renderJSON(response, reqInfo.bundle, pluginRoot, servicesRoot, request.getLocale(), request.getParameter(FILTER_PARAM), null );
            }
            catch (InvalidSyntaxException e)
            {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid LDAP filter specified");
            }

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
        boolean success = false;
        BundleException bundleException = null;
        final String action = WebConsoleUtil.getParameter( req, "action" );
        if ( "refreshPackages".equals( action ) )
        {
            getPackageAdmin().refreshPackages( null );
            success = true;
        }
        else if ( "install".equals( action ) )
        {
            installBundles( req );

            if (req.getRequestURI().endsWith( "/install" )) {
                // just send 200/OK, no content
                resp.setContentLength( 0 );
            } else {
                // redirect to URL
                resp.sendRedirect( req.getRequestURI() );
            }

            return;
        }
        else
        {
            final RequestInfo reqInfo = new RequestInfo( req );
            if ( reqInfo.bundle == null && reqInfo.bundleRequested )
            {
                resp.sendError( 404 );
                return;
            }

            final Bundle bundle = reqInfo.bundle;
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
                        bundleException = be;
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
                        bundleException = be;
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
                    }
                    catch ( BundleException be )
                    {
                        bundleException = be;
                        log( "Cannot uninstall", be );
                    }
                }

                // let's wait a little bit to give the framework time
                // to process our request
                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    // we ignore this
                }

                // write the state only
                resp.getWriter().print("{fragment:" + isFragmentBundle(bundle) //
                    + ",stateRaw:" + bundle.getState() + "}");
                return;
            }
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
            try
            {
                this.renderJSON( resp, null, pluginRoot, servicesRoot, req.getLocale(), req.getParameter(FILTER_PARAM), bundleException );
            }
            catch (InvalidSyntaxException e)
            {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid LDAP filter specified");
            }
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
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException, ServletException
    {
        // get request info from request attribute
        final RequestInfo reqInfo = getRequestInfo(request);

        final int startLevel = getStartLevel().getInitialBundleStartLevel();

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "startLevel", String.valueOf(startLevel));
        vars.put( "drawDetails", reqInfo.bundleRequested ? Boolean.TRUE : Boolean.FALSE );
        vars.put( "currentBundle", (reqInfo.bundleRequested && reqInfo.bundle != null ? String.valueOf(reqInfo.bundle.getBundleId()) : "null"));

        final String pluginRoot = ( String ) request.getAttribute( WebConsoleConstants.ATTR_PLUGIN_ROOT );
        final String servicesRoot = getServicesRoot ( request );
        StringWriter w = new StringWriter();
        try
        {
            writeJSON(w, reqInfo.bundle, pluginRoot, servicesRoot, request.getLocale(), request.getParameter(FILTER_PARAM), null );
        }
        catch (InvalidSyntaxException e)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Invalid LDAP filter specified");
            return;
        }
        vars.put( "__bundles__", w.toString());

        response.getWriter().print(TEMPLATE_MAIN);
    }

    private void renderJSON( final HttpServletResponse response, final Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale, final String filter, final BundleException be )
        throws IOException, InvalidSyntaxException
    {
        response.setContentType( "application/json" );
        response.setCharacterEncoding( "UTF-8" );

        final PrintWriter pw = response.getWriter();
        writeJSON(pw, bundle, pluginRoot, servicesRoot, locale, filter, be);
    }


    private void writeJSON( final Writer pw, final Bundle bundle, final String pluginRoot, final String servicesRoot, final Locale locale, final String filter, final BundleException be )
        throws IOException, InvalidSyntaxException
    {
        writeJSON( pw, bundle, pluginRoot, servicesRoot, false, locale, filter, be );
    }


    private void writeJSON( final Writer pw, final Bundle bundle, final String pluginRoot,
        final String servicesRoot, final boolean fullDetails, final Locale locale, final String filter, final BundleException be ) throws IOException, InvalidSyntaxException
    {
        final Bundle[] allBundles = this.getBundles();
        final Object[] status = getStatusLine(allBundles);
        final String statusLine = (String) status[5];
        // filter bundles by headers
        final Bundle[] bundles;
        if (bundle != null)
        {
            bundles = new Bundle[] { bundle };
        }
        else if (filter != null)
        {
            Filter f = getBundleContext().createFilter(filter);
            ArrayList list = new ArrayList(allBundles.length);
            final String localeString = locale.toString();
            for (int i = 0, size = allBundles.length; i < size; i++)
            {
                if (f.match(allBundles[i].getHeaders(localeString)))
                {
                    list.add(allBundles[i]);
                }
            }
            bundles = new Bundle[list.size()];
            list.toArray(bundles);
        }
        else
        {
            bundles = allBundles;
        }

        Util.sort( bundles, locale );

        final JSONWriter jw = new JSONWriter( pw );

        try
        {
            jw.object();

            if (null != be)
            {
                final StringWriter s = new StringWriter();
                final Throwable t = be.getNestedException() != null ? be.getNestedException() : be;
                t.printStackTrace( new PrintWriter(s) );
                jw.key( "error" );
                jw.value( s.toString() );
            }

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
        jw.key( "fragment" );
        jw.value( isFragmentBundle(bundle) );
        jw.key( "stateRaw" );
        jw.value( bundle.getState() );
        jw.key( "state" );
        jw.value( toStateString( bundle ) );
        jw.key( "version" );
        jw.value( Util.getHeaderValue(bundle, Constants.BUNDLE_VERSION) );
        jw.key( "symbolicName" );
        jw.value( Util.getHeaderValue(bundle, Constants.BUNDLE_SYMBOLICNAME) );
        jw.key( "category" );
        jw.value( Util.getHeaderValue(bundle, Constants.BUNDLE_CATEGORY) );

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

    private final boolean isFragmentBundle( Bundle bundle)
    {
        return getPackageAdmin().getBundleType( bundle ) == PackageAdmin.BUNDLE_TYPE_FRAGMENT;
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

        if ( bundle.getState() != Bundle.UNINSTALLED )
        {
            listServices( jw, bundle, servicesRoot );
        }

        listHeaders( jw, bundle );

        jw.endArray();
    }


    private final Integer getStartLevel( Bundle bundle )
    {
        if ( bundle.getState() != Bundle.UNINSTALLED )
        {
            StartLevel sl = getStartLevel();
            if ( sl != null )
            {
                return new Integer( sl.getBundleStartLevel( bundle ) );
            }
        }

        // bundle has been uninstalled or StartLevel service is not available
        return null;
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
            Clause[] pkgs = Parser.parseHeader( target );
            if ( pkgs != null && pkgs.length > 0 )
            {
                // do alphabetical sort
                Arrays.sort( pkgs, new Comparator()
                {
                    public int compare( Clause p1, Clause p2 )
                    {
                        return p1.getName().compareTo( p2.getName() );
                    }


                    public int compare( Object o1, Object o2 )
                    {
                        return compare( ( Clause) o1, ( Clause ) o2 );
                    }
                } );

                JSONArray val = new JSONArray();
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    Clause export = new Clause( pkgs[i].getName(), pkgs[i].getDirectives(), pkgs[i].getAttributes() );
                    collectExport( val, export.getName(), export.getAttribute( Constants.VERSION_ATTRIBUTE ) );
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
            Clause[] pkgs = Parser.parseHeader( target );
            if ( pkgs != null && pkgs.length > 0 )
            {
                Map imports = new TreeMap();
                for ( int i = 0; i < pkgs.length; i++ )
                {
                    Clause pkg = pkgs[i];
                    imports.put( pkg.getName(), new Clause( pkg.getName(), pkg.getDirectives(), pkg.getAttributes() ) );
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

                            Clause imp = ( Clause ) imports.get( ep.getName() );
                            if ( imp != null && isSatisfied( imp, ep ) )
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
                        Clause r4Import = ( Clause ) ii.next();
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

                        collectImport( val, r4Import.getName(), r4Import.getAttribute( Constants.VERSION_ATTRIBUTE ),
                            Constants.RESOLUTION_OPTIONAL.equals( r4Import
                                .getDirective( Constants.RESOLUTION_DIRECTIVE ) ), ep, pluginRoot );
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

        Dictionary headers = bundle.getHeaders(""); // don't localize at all - raw headers
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
        collectExport( array, name, ( version == null ) ? null : version.toString() );
    }


    private void collectExport( JSONArray array, String name, String version )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );
        if ( bootDel )
        {
            val.append( "!! " );
        }

        val.append( name );

        if ( version != null )
        {
            val.append( ",version=" ).append( version );
        }

        if ( bootDel )
        {
            val.append( " -- Overwritten by Boot Delegation" );
        }

        array.put(val.toString());
    }


    private void collectImport( JSONArray array, String name, Version version, boolean optional,
        ExportedPackage export, final String pluginRoot )
    {
        collectImport( array, name, ( version == null ) ? null : version.toString(), optional, export, pluginRoot );
    }


    private void collectImport( JSONArray array, String name, String version, boolean optional, ExportedPackage export,
        final String pluginRoot )
    {
        StringBuffer val = new StringBuffer();
        boolean bootDel = isBootDelegated( name );

        String marker = null;
        val.append( name );

        if ( version != null )
        {
            val.append( ",version=" ).append( version );
        }

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


    private boolean isSatisfied( Clause imported, ExportedPackage exported )
    {
        if ( imported.getName().equals( exported.getName() ) )
        {
            String versionAttr = imported.getAttribute( Constants.VERSION_ATTRIBUTE );
            if ( versionAttr == null )
            {
                // no specific version required, this export surely satisfies it
                return true;
            }

            VersionRange required = VersionRange.parse( versionAttr );
            return required.isInRange( exported.getVersion() );
        }

        // no this export does not satisfy the import
        return false;
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
        UpdateHelper t = new UpdateHelper( this, bundle, false );
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


    //---------- Bundle Installation handler (former InstallAction)

    private void installBundles( HttpServletRequest request ) throws IOException
    {

        // get the uploaded data
        final Map params = ( Map ) request.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
        if ( params == null )
        {
            return;
        }

        final FileItem startItem = getParameter( params, FIELD_START );
        final FileItem startLevelItem = getParameter( params, FIELD_STARTLEVEL );
        final FileItem[] bundleItems = getFileItems( params, FIELD_BUNDLEFILE );
        final FileItem refreshPackagesItem = getParameter( params, FIELD_REFRESH_PACKAGES );

        // don't care any more if no bundle item
        if ( bundleItems.length == 0 )
        {
            return;
        }

        // default values
        // it exists
        int startLevel = -1;
        String bundleLocation = "inputstream:";

        // convert the start level value
        if ( startLevelItem != null )
        {
            try
            {
                startLevel = Integer.parseInt( startLevelItem.getString() );
            }
            catch ( NumberFormatException nfe )
            {
                log( LogService.LOG_INFO, "Cannot parse start level parameter " + startLevelItem
                    + " to a number, not setting start level" );
            }
        }

        for ( int i = 0; i < bundleItems.length; i++ )
        {
            final FileItem bundleItem = bundleItems[i];
            // write the bundle data to a temporary file to ease processing
            File tmpFile = null;
            try
            {
                // copy the data to a file for better processing
                tmpFile = File.createTempFile( "install", ".tmp" );
                bundleItem.write( tmpFile );
            }
            catch ( Exception e )
            {
                log( LogService.LOG_ERROR, "Problem accessing uploaded bundle file: " + bundleItem.getName(), e );

                // remove the tmporary file
                if ( tmpFile != null )
                {
                    tmpFile.delete();
                    tmpFile = null;
                }
            }

            // install or update the bundle now
            if ( tmpFile != null )
            {
                // start, refreshPackages just needs to exist, don't care for value
                boolean start = startItem != null;
                boolean refreshPackages = refreshPackagesItem != null;

                bundleLocation = "inputstream:" + bundleItem.getName();
                installBundle( bundleLocation, tmpFile, startLevel, start, refreshPackages );
            }
        }
    }


    private FileItem getParameter( Map params, String name )
    {
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( items[i].isFormField() )
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }


    private FileItem[] getFileItems( Map params, String name )
    {
        final List files = new ArrayList();
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( !items[i].isFormField() && items[i].getSize() > 0 )
                {
                    files.add( items[i] );
                }
            }
        }

        return ( FileItem[] ) files.toArray( new FileItem[files.size()] );
    }


    private void installBundle( String location, File bundleFile, int startLevel, boolean start, boolean refreshPackages )
        throws IOException
    {
        if ( bundleFile != null )
        {

            // try to get the bundle name, fail if none
            String symbolicName = getSymbolicName( bundleFile );
            if ( symbolicName == null )
            {
                bundleFile.delete();
                throw new IOException( Constants.BUNDLE_SYMBOLICNAME + " header missing, cannot install bundle" );
            }

            // check for existing bundle first
            Bundle updateBundle = null;
            if ( Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals( symbolicName ) )
            {
                updateBundle = getBundleContext().getBundle( 0 );
            }
            else
            {
                Bundle[] bundles = getBundleContext().getBundles();
                for ( int i = 0; i < bundles.length; i++ )
                {
                    if ( ( bundles[i].getLocation() != null && bundles[i].getLocation().equals( location ) )
                        || ( bundles[i].getSymbolicName() != null && bundles[i].getSymbolicName().equals( symbolicName ) ) )
                    {
                        updateBundle = bundles[i];
                        break;
                    }
                }
            }

            if ( updateBundle != null )
            {

                updateBackground( updateBundle, bundleFile, refreshPackages );

            }
            else
            {

                installBackground( bundleFile, location, startLevel, start, refreshPackages );

            }
        }
    }


    private String getSymbolicName( File bundleFile )
    {
        JarFile jar = null;
        try
        {
            jar = new JarFile( bundleFile );
            Manifest m = jar.getManifest();
            if ( m != null )
            {
                return m.getMainAttributes().getValue( Constants.BUNDLE_SYMBOLICNAME );
            }
        }
        catch ( IOException ioe )
        {
            log( LogService.LOG_WARNING, "Cannot extract symbolic name of bundle file " + bundleFile, ioe );
        }
        finally
        {
            if ( jar != null )
            {
                try
                {
                    jar.close();
                }
                catch ( IOException ioe )
                {
                    // ignore
                }
            }
        }

        // fall back to "not found"
        return null;
    }


    private void installBackground( final File bundleFile, final String location, final int startlevel,
        final boolean doStart, final boolean refreshPackages )
    {

        InstallHelper t = new InstallHelper( this, getBundleContext(), bundleFile, location, startlevel, doStart,
            refreshPackages );
        t.start();
    }


    private void updateBackground( final Bundle bundle, final File bundleFile, final boolean refreshPackages )
    {
        UpdateHelper t = new UpdateHelper( this, bundle, bundleFile, refreshPackages );
        t.start();
    }
}
