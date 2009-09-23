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
package org.apache.felix.webconsole.internal.obr;


import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;


public class BundleRepositoryRender extends BaseWebConsolePlugin
{

    public static final String LABEL = "bundlerepo";

    public static final String TITLE = "OSGi Repository";

    public static final String PARAM_REPO_ID = "repositoryId";

    public static final String PARAM_REPO_URL = "repositoryURL";

    private static final String REPOSITORY_PROPERTY = "obr.repository.url";

    private static final String ALL_CATEGORIES_OPTION = "*";

    private static final String PAR_CATEGORIES = "category";

    private String[] repoURLs;


    public void activate( BundleContext bundleContext )
    {
        super.activate( bundleContext );

        String urlStr = bundleContext.getProperty( REPOSITORY_PROPERTY );
        List urlList = new ArrayList();

        if ( urlStr != null )
        {
            StringTokenizer st = new StringTokenizer( urlStr );
            while ( st.hasMoreTokens() )
            {
                urlList.add( st.nextToken() );
            }
        }

        this.repoURLs = ( String[] ) urlList.toArray( new String[urlList.size()] );
    }


    public String getLabel()
    {
        return LABEL;
    }


    public String getTitle()
    {
        return TITLE;
    }


    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {

        PrintWriter pw = response.getWriter();
        this.header( pw );

        RepositoryAdmin repoAdmin = getRepositoryAdmin();
        if ( repoAdmin == null )
        {
            pw.println( "<tr class='content'>" );
            pw.println( "<td class='content' colspan='4'>RepositoryAdmin Service not available</td>" );
            pw.println( "</tr>" );

            footer( pw );

            return;
        }

        Repository[] repos = repoAdmin.listRepositories();
        Set activeURLs = new HashSet();
        if ( repos == null || repos.length == 0 )
        {
            pw.println( "<tr class='content'>" );
            pw.println( "<td class='content' colspan='4'>No Active Repositories</td>" );
            pw.println( "</tr>" );
        }
        else
        {
            for ( int i = 0; i < repos.length; i++ )
            {
                Repository repo = repos[i];

                activeURLs.add( repo.getURL().toString() );

                pw.println( "<tr class='content'>" );
                pw.println( "<td class='content'>" + repo.getName() + "</td>" );

                pw.print( "<td class='content'>" );
                pw.print( "<a href='" + repo.getURL() + "' target='_blank' title='Show Repository " + repo.getURL()
                    + "'>" + repo.getURL() + "</a>" );
                pw.println( "</td>" );

                pw.println( "<td class='content'>" + new Date( repo.getLastModified() ) + "</td>" );
                pw.println( "<td class='content'>" );
                pw.println( "<form>" );
                pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + RefreshRepoAction.NAME
                    + "'>" );
                pw.println( "<input type='hidden' name='" + RefreshRepoAction.PARAM_REPO + "' value='" + repo.getURL()
                    + "'>" );
                pw.println( "<input class='submit' type='submit' value='Refresh'>" );
                pw.println( "<input class='submit' type='submit' name='remove' value='Remove'>" );
                pw.println( "</form>" );
                pw.println( "</td>" );
                pw.println( "</tr>" );
            }
        }

        // list any repositories configured but not active
        for ( int i = 0; i < this.repoURLs.length; i++ )
        {
            if ( !activeURLs.contains( this.repoURLs[i] ) )
            {
                pw.println( "<tr class='content'>" );
                pw.println( "<td class='content'>-</td>" );
                pw.println( "<td class='content'>" + this.repoURLs[i] + "</td>" );
                pw.println( "<td class='content'>[inactive, click Refresh to activate]</td>" );
                pw.println( "<td class='content'>" );
                pw.println( "<form>" );
                pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + RefreshRepoAction.NAME
                    + "'>" );
                pw.println( "<input type='hidden' name='" + RefreshRepoAction.PARAM_REPO + "' value='"
                    + this.repoURLs[i] + "'>" );
                pw.println( "<input class='submit' type='submit' value='Refresh'>" );
                pw.println( "</form>" );
                pw.println( "</td>" );
                pw.println( "</tr>" );
            }
        }

        // entry of a new repository
        pw.println( "<form>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>&nbsp;</td>" );
        pw.println( "<td class='content' colspan='2'>" );
        pw.println( "  <input class='input' type='text' name='" + RefreshRepoAction.PARAM_REPO
            + "' value='' size='80'>" );
        pw.println( "</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + RefreshRepoAction.NAME + "'>" );
        pw.println( "<input class='submit' type='submit' value='Add'>" );
        pw.println( "</td>" );
        pw.println( "</tr>" );
        pw.println( "</form>" );

        this.footer( pw );

        this.listResources( pw, repos, request.getParameter( PAR_CATEGORIES ) );
    }


    private void header( PrintWriter pw )
    {
        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content container' colspan='4'>Bundle Repositories</th>" );
        pw.println( "</tr>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content'>Name</th>" );
        pw.println( "<th class='content'>URL</th>" );
        pw.println( "<th class='content'>Last Modification Time</th>" );
        pw.println( "<th class='content'>&nbsp;</th>" );
        pw.println( "</tr>" );
    }


    private void footer( PrintWriter pw )
    {
        pw.println( "</table>" );
    }


    private void listResources( PrintWriter pw, Repository[] repos, String category )
    {

        // assume no category if the all option
        if ( ALL_CATEGORIES_OPTION.equals( category ) )
        {
            category = null;
        }

        Map bundles = this.getBundles();

        Map resSet = new HashMap();
        SortedSet categories = new TreeSet();
        SortedSet labels = new TreeSet();

        for ( int i = 0; i < repos.length; i++ )
        {
            Resource[] resources = repos[i].getResources();
            for ( int j = 0; resources != null && j < resources.length; j++ )
            {
                Resource res = resources[j];

                // get categories and check whether we should actually
                // ignore this resource
                boolean useResource = false;
                String[] cats = res.getCategories();
                for ( int ci = 0; cats != null && ci < cats.length; ci++ )
                {
                    String cat = cats[ci];
                    categories.add( cat );
                    useResource |= ( category == null || cat.equals( category ) );
                }

                if ( useResource )
                {
                    String symbolicName = res.getSymbolicName();
                    Version version = res.getVersion();
                    Version installedVersion = ( Version ) bundles.get( symbolicName );
                    if ( installedVersion == null || installedVersion.compareTo( version ) < 0 )
                    {
                        Collection versions = ( Collection ) resSet.get( symbolicName );
                        if ( versions == null )
                        {
                            // order versions, hence use a TreeSet
                            versions = new TreeSet();
                            resSet.put( symbolicName, versions );
                        }
                        versions.add( version );

                        labels.add( res.getPresentationName() + "§" + symbolicName );
                    }
                }
            }
        }

        boolean doForm = !resSet.isEmpty();
        this.resourcesHeader( pw, doForm, category, categories );

        for ( Iterator ri = labels.iterator(); ri.hasNext(); )
        {
            String label = (String) ri.next();
            String[] parts = label.split( "§" );
            Collection versions = (Collection) resSet.remove(parts[1]);
            if (versions != null) {
                this.printResource( pw, parts[1], parts[0], versions );
            }
        }

        this.resourcesFooter( pw, doForm );
    }


    private void resourcesHeader( PrintWriter pw, boolean doForm, String currentCategory, Collection categories )
    {

        if ( doForm )
        {
            pw.println( "<form method='post'>" );
            pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + InstallFromRepoAction.NAME
                + "'>" );
        }

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content container'>Available Resources</th>" );

        if ( categories != null && !categories.isEmpty() )
        {
            pw.println( "<th class='content container' style='text-align:right'>Limit to Bundle Category:</th>" );
            pw.println( "<th class='content container'>" );
            Util.startScript( pw );
            pw.println( "function reloadWithCat(field) {" );
            pw.println( "  var query = '?" + PAR_CATEGORIES + "=' + field.value;" );
            pw
                .println( "  var dest = document.location.protocol + '//' + document.location.host + document.location.pathname + query;" );
            pw.println( "  document.location = dest;" );
            pw.println( "}" );
            Util.endScript( pw );
            pw.println( "<select class='select' name='__ignoreoption__' onChange='reloadWithCat(this);'>" );
            pw.print( "<option value='" + ALL_CATEGORIES_OPTION + "'>all</option>" );
            for ( Iterator ci = categories.iterator(); ci.hasNext(); )
            {
                String category = ( String ) ci.next();
                pw.print( "<option value='" + category + "'" );
                if ( category.equals( currentCategory ) )
                {
                    pw.print( " selected" );
                }
                pw.print( '>' );
                pw.print( category );
                pw.println( "</option>" );
            }
            pw.println( "</select>" );
            pw.println( "</th>" );
        }
        else
        {
            pw.println( "<th class='content container'>&nbsp;</th>" );
        }

        pw.println( "</tr>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content'>Deploy Version</th>" );
        pw.println( "<th class='content' colspan='2'>Name</th>" );
        pw.println( "</tr>" );
    }


    private void printResource( PrintWriter pw, String symbolicName, String presentationName, Collection versions )
    {
        pw.println( "<tr class='content'>" );

        pw.println( "<td class='content' valign='top' align='center'>" );
        pw.println( "<select class='select' name='bundle'>" );
        pw.print( "<option value='" + AbstractObrPlugin.DONT_INSTALL_OPTION + "'>Select Version...</option>" );
        for ( Iterator vi = versions.iterator(); vi.hasNext(); )
        {
            Version version = ( Version ) vi.next();
            pw.print( "<option value='" + symbolicName + "," + version + "'>" );
            pw.print( version );
            pw.println( "</option>" );
        }
        pw.println( "</select>" );
        pw.println( "</td>" );

        pw.println( "<td class='content'  colspan='2'>" + presentationName + " (" + symbolicName + ")</td>" );

        pw.println( "</tr>" );
    }


    private void resourcesButtons( PrintWriter pw )
    {
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>&nbsp;</td>" );
        pw.println( "<td class='content'>" );
        pw.println( "<input class='submit' style='width:auto' type='submit' name='deploy' value='Deploy Selected'>" );
        pw.println( "&nbsp;&nbsp;&nbsp;" );
        pw
            .println( "<input class='submit' style='width:auto' type='submit' name='deploystart' value='Deploy and Start Selected'>" );
        pw.println( "</td></tr>" );
    }


    private void resourcesFooter( PrintWriter pw, boolean doForm )
    {
        if ( doForm )
        {
            this.resourcesButtons( pw );
        }
        pw.println( "</table></form>" );
    }


    private Map getBundles()
    {
        Map bundles = new HashMap();

        Bundle[] installed = getBundleContext().getBundles();
        for ( int i = 0; i < installed.length; i++ )
        {
            String ver = ( String ) installed[i].getHeaders().get( Constants.BUNDLE_VERSION );
            Version bundleVersion = Version.parseVersion( ver );

            // assume one bundle instance per symbolic name !!
            // only add if there is a symbolic name !
            if ( installed[i].getSymbolicName() != null )
            {
                bundles.put( installed[i].getSymbolicName(), bundleVersion );
            }
        }

        return bundles;
    }


    protected RepositoryAdmin getRepositoryAdmin()
    {
        return ( RepositoryAdmin ) getService( "org.osgi.service.obr.RepositoryAdmin" );
    }

}
