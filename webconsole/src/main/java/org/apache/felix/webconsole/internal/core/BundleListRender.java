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
import java.util.Arrays;
import java.util.Comparator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.Render;
import org.apache.felix.webconsole.internal.BaseManagementPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.util.tracker.ServiceTracker;


/**
 * The <code>BundleListRender</code> TODO
 */
public class BundleListRender extends BaseManagementPlugin implements Render
{

    public static final String NAME = "list";

    public static final String LABEL = "Bundles";

    public static final String BUNDLE_ID = "bundleId";

    private static final String INSTALLER_SERVICE_NAME = "org.apache.sling.osgi.assembly.installer.InstallerService";

    // track the optional installer service manually
    private ServiceTracker installerService;


    public void setBundleContext( BundleContext bundleContext )
    {
        super.setBundleContext( bundleContext );

        installerService = new ServiceTracker( bundleContext, INSTALLER_SERVICE_NAME, null );
        installerService.open();
    }


    // protected void deactivate(ComponentContext context) {
    // if (installerService != null) {
    // installerService.close();
    // installerService = null;
    // }
    // }

    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    public void render( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        PrintWriter pw = response.getWriter();

        this.header( pw );

        this.installForm( pw );
        pw.println( "<tr class='content'>" );
        pw.println( "<td colspan='7' class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );

        this.tableHeader( pw );

        Bundle[] bundles = this.getBundles();
        if ( bundles == null || bundles.length == 0 )
        {
            pw.println( "<tr class='content'>" );
            pw.println( "<td class='content' colspan='6'>No " + this.getLabel() + " installed currently</td>" );
            pw.println( "</tr>" );
        }
        else
        {

            sort( bundles );

            long previousBundle = -1;
            for ( int i = 0; i < bundles.length; i++ )
            {

                if ( previousBundle >= 0 )
                {
                    // prepare for injected table information row
                    pw.println( "<tr id='bundle" + previousBundle + "'></tr>" );
                }

                this.bundle( pw, bundles[i] );

                previousBundle = bundles[i].getBundleId();
            }

            if ( previousBundle >= 0 )
            {
                // prepare for injected table information row
                pw.println( "<tr id='bundle" + previousBundle + "'></tr>" );
            }
        }

        pw.println( "<tr class='content'>" );
        pw.println( "<td colspan='7' class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );

        this.installForm( pw );

        this.footer( pw );
    }


    protected Bundle[] getBundles()
    {
        return getBundleContext().getBundles();
    }


    private void header( PrintWriter pw )
    {
        Util.startScript( pw );
        pw.println( "function showDetails(bundleId) {" );
        pw.println( "    var span = document.getElementById('bundle' + bundleId);" );
        pw.println( "    if (!span) {" );
        pw.println( "        return;" );
        pw.println( "    }" );
        pw.println( "    if (span.innerHTML) {" );
        pw.println( "        span.innerHTML = '';" );
        pw.println( "        return;" );
        pw.println( "    }" );
        pw.println( "    var parm = '?" + Util.PARAM_ACTION + "=" + AjaxBundleDetailsAction.NAME + "&" + BUNDLE_ID
            + "=' + bundleId;" );
        pw.println( "    sendRequest('GET', parm, displayBundleDetails);" );
        pw.println( "}" );
        pw.println( "function displayBundleDetails(obj) {" );
        pw.println( "    var span = document.getElementById('bundle' + obj." + BUNDLE_ID + ");" );
        pw.println( "    if (!span) {" );
        pw.println( "        return;" );
        pw.println( "    }" );
        pw
            .println( "    var innerHtml = '<td class=\"content\">&nbsp;</td><td class=\"content\" colspan=\"6\"><table broder=\"0\">';" );
        pw.println( "    var props = obj.props;" );
        pw.println( "    for (var i=0; i < props.length; i++) {" );
        pw
            .println( "        innerHtml += '<tr><td valign=\"top\" noWrap>' + props[i].key + '</td><td valign=\"top\">' + props[i].value + '</td></tr>';" );
        pw.println( "    }" );
        pw.println( "    innerHtml += '</table></td>';" );
        pw.println( "    span.innerHTML = innerHtml;" );
        pw.println( "}" );
        Util.endScript( pw );

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
    }


    private void tableHeader( PrintWriter pw )
    {
        // pw.println("<tr class='content'>");
        // pw.println("<th class='content container' colspan='7'>Installed " +
        // getLabel() + "</th>");
        // pw.println("</tr>");

        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content'>ID</th>" );
        pw.println( "<th class='content' width='100%'>Name</th>" );
        pw.println( "<th class='content'>Status</th>" );
        pw.println( "<th class='content' colspan='4'>Actions</th>" );
        pw.println( "</tr>" );
    }


    private void footer( PrintWriter pw )
    {
        pw.println( "</table>" );
    }


    private void bundle( PrintWriter pw, Bundle bundle )
    {
        String name = getName( bundle );

        pw.println( "<tr>" );
        pw.println( "<td class='content right'>" + bundle.getBundleId() + "</td>" );
        pw.println( "<td class='content'><a href='javascript:showDetails(" + bundle.getBundleId() + ")'>" + name
            + "</a></td>" );
        pw.println( "<td class='content center'>" + this.toStateString( bundle.getState() ) + "</td>" );

        // no buttons for system bundle
        if ( bundle.getBundleId() == 0 )
        {
            pw.println( "<td class='content' colspan='4'>&nbsp;</td>" );
        }
        else
        {
            boolean enabled = bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED;
            this.actionForm( pw, enabled, bundle.getBundleId(), StartAction.NAME, StartAction.LABEL );

            enabled = bundle.getState() == Bundle.ACTIVE;
            this.actionForm( pw, enabled, bundle.getBundleId(), StopAction.NAME, StopAction.LABEL );

            enabled = bundle.getState() != Bundle.UNINSTALLED && this.hasUpdates( bundle );
            this.actionForm( pw, enabled, bundle.getBundleId(), UpdateAction.NAME, UpdateAction.LABEL );

            enabled = bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED
                || bundle.getState() == Bundle.ACTIVE;
            this.actionForm( pw, enabled, bundle.getBundleId(), UninstallAction.NAME, UninstallAction.LABEL );
        }

        pw.println( "</tr>" );
    }


    private void actionForm( PrintWriter pw, boolean enabled, long bundleId, String action, String actionLabel )
    {
        pw.println( "<form name='form" + bundleId + "' method='post'>" );
        pw.println( "<td class='content' align='right'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + action + "' />" );
        pw.println( "<input type='hidden' name='" + BUNDLE_ID + "' value='" + bundleId + "' />" );
        pw.println( "<input class='submit' type='submit' value='" + actionLabel + "'" + ( enabled ? "" : "disabled" )
            + " />" );
        pw.println( "</td>" );
        pw.println( "</form>" );
    }


    private void installForm( PrintWriter pw )
    {
        int startLevel = getStartLevel().getInitialBundleStartLevel();

        pw.println( "<form method='post' enctype='multipart/form-data'>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>&nbsp;</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + InstallAction.NAME + "' />" );
        pw.println( "<input class='input' type='file' name='" + InstallAction.FIELD_BUNDLEFILE + "'>" );
        pw.println( " - Start <input class='checkradio' type='checkbox' name='" + InstallAction.FIELD_START
            + "' value='start'>" );
        pw.println( " - Start Level <input class='input' type='input' name='" + InstallAction.FIELD_STARTLEVEL
            + "' value='" + startLevel + "' width='4'>" );
        pw.println( "</td>" );
        pw.println( "<td class='content' align='right' colspan='5' noWrap>" );
        pw.println( "<input class='submit' style='width:auto' type='submit' value='" + InstallAction.LABEL + "'>" );
        pw.println( "&nbsp;" );
        pw.println( "<input class='submit' style='width:auto' type='submit' value='" + RefreshPackagesAction.LABEL
            + "' onClick='this.form[\"" + Util.PARAM_ACTION + "\"].value=\"" + RefreshPackagesAction.NAME
            + "\"; return true;'>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );
        pw.println( "</form>" );
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


    private boolean hasUpdates( Bundle bundle )
    {

        // no updates if there is no installer service
        Object isObject = installerService.getService();
        if ( isObject == null )
        {
            return false;
        }

        // don't care for bundles with no symbolic name
        if ( bundle.getSymbolicName() == null )
        {
            return false;
        }
        /*
                Version bundleVersion = Version.parseVersion((String) bundle.getHeaders().get(
                    Constants.BUNDLE_VERSION));

                BundleRepositoryAdmin repoAdmin = ((InstallerService) isObject).getBundleRepositoryAdmin();
                for (Iterator<Resource> ri = repoAdmin.getResources(); ri.hasNext();) {
                    Resource res = ri.next();
                    if (bundle.getSymbolicName().equals(res.getSymbolicName())) {
                        if (res.getVersion().compareTo(bundleVersion) > 0) {
                            return true;
                        }
                    }
                }
        */

        return false;
    }


    private void sort( Bundle[] bundles )
    {
        Arrays.sort( bundles, BUNDLE_NAME_COMPARATOR );
    }


    private static String getName( Bundle bundle )
    {
        String name = ( String ) bundle.getHeaders().get( Constants.BUNDLE_NAME );
        if ( name == null || name.length() == 0 )
        {
            name = bundle.getSymbolicName();
            if ( name == null )
            {
                name = bundle.getLocation();
                if ( name == null )
                {
                    name = String.valueOf( bundle.getBundleId() );
                }
            }
        }
        return name;
    }

    // ---------- inner classes ------------------------------------------------

    private static final Comparator BUNDLE_NAME_COMPARATOR = new Comparator()
    {
        public int compare( Object o1, Object o2 )
        {
            return compare( ( Bundle ) o1, ( Bundle ) o2 );
        }


        public int compare( Bundle b1, Bundle b2 )
        {

            // the same bundles
            if ( b1 == b2 || b1.getBundleId() == b2.getBundleId() )
            {
                return 0;
            }

            // special case for system bundle, which always is first
            if ( b1.getBundleId() == 0 )
            {
                return -1;
            }
            else if ( b2.getBundleId() == 0 )
            {
                return 1;
            }

            // compare the symbolic names
            int snComp = getName( b1 ).compareToIgnoreCase( getName( b2 ) );
            if ( snComp != 0 )
            {
                return snComp;
            }

            // same names, compare versions
            Version v1 = Version.parseVersion( ( String ) b1.getHeaders().get( Constants.BUNDLE_VERSION ) );
            Version v2 = Version.parseVersion( ( String ) b2.getHeaders().get( Constants.BUNDLE_VERSION ) );
            int vComp = v1.compareTo( v2 );
            if ( vComp != 0 )
            {
                return vComp;
            }

            // same version ? Not really, but then, we compare by bundle id
            if ( b1.getBundleId() < b2.getBundleId() )
            {
                return -1;
            }

            // b1 id must be > b2 id because equality is already checked
            return 1;
        }
    };
}
