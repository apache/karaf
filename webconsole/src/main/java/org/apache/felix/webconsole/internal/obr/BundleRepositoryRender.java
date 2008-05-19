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


import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.felix.webconsole.Render;
import org.apache.felix.webconsole.internal.Util;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Version;


public abstract class BundleRepositoryRender extends AbstractObrPlugin implements Render
{

    public static final String NAME = "bundlerepo";

    public static final String LABEL = "OSGi Repository";

    public static final String PARAM_REPO_ID = "repositoryId";

    public static final String PARAM_REPO_URL = "repositoryURL";

    private static final String REPOSITORY_PROPERTY = "obr.repository.url";

    private String[] repoURLs;


    public void setBundleContext( BundleContext bundleContext )
    {
        super.setBundleContext( bundleContext );

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


    public String getName()
    {
        return NAME;
    }


    public String getLabel()
    {
        return LABEL;
    }


    /*
        public void render(HttpServletRequest request, HttpServletResponse response)
                throws IOException {

            PrintWriter pw = response.getWriter();
            this.header(pw);

            Iterator<?> repos;
            BundleRepositoryAdmin repoAdmin = getBundleRepositoryAdmin();
            if (repoAdmin != null) {
                repos = repoAdmin.getRepositories();
            } else {
                repos = Collections.emptyList().iterator();
            }

            Set<String> activeURLs = new HashSet<String>();
            if (!repos.hasNext()) {
                pw.println("<tr class='content'>");
                pw.println("<td class='content' colspan='4'>No Active Repositories</td>");
                pw.println("</tr>");
            } else {
                while (repos.hasNext()) {
                    Repository repo = (Repository) repos.next();

                    activeURLs.add(repo.getURL().toString());

                    pw.println("<tr class='content'>");
                    pw.println("<td class='content'>" + repo.getName() + "</td>");
                    pw.println("<td class='content'>" + repo.getURL() + "</td>");
                    pw.println("<td class='content'>"
                        + new Date(repo.getLastModified()) + "</td>");
                    pw.println("<td class='content'>");
                    pw.println("<form>");
                    pw.println("<input type='hidden' name='" + Util.PARAM_ACTION
                        + "' value='" + RefreshRepoAction.NAME + "'>");
                    pw.println("<input type='hidden' name='"
                        + RefreshRepoAction.PARAM_REPO + "' value='"
                        + repo.getURL() + "'>");
                    pw.println("<input class='submit' type='submit' value='Refresh'>");
                    pw.println("</form>");
                    pw.println("</td>");
                    pw.println("</tr>");
                }
            }

            // list any repositories configured but not active
            for (int i = 0; i < this.repoURLs.length; i++) {
                if (!activeURLs.contains(this.repoURLs[i])) {
                    pw.println("<tr class='content'>");
                    pw.println("<td class='content'>-</td>");
                    pw.println("<td class='content'>" + this.repoURLs[i] + "</td>");
                    pw.println("<td class='content'>[inactive, click Refresh to activate]</td>");
                    pw.println("<td class='content'>");
                    pw.println("<form>");
                    pw.println("<input type='hidden' name='" + Util.PARAM_ACTION
                        + "' value='" + RefreshRepoAction.NAME + "'>");
                    pw.println("<input type='hidden' name='"
                        + RefreshRepoAction.PARAM_REPO + "' value='"
                        + this.repoURLs[i] + "'>");
                    pw.println("<input class='submit' type='submit' value='Refresh'>");
                    pw.println("</form>");
                    pw.println("</td>");
                    pw.println("</tr>");
                }
            }

            this.footer(pw);

            this.listResources(pw);
        }
    */
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


    private void resourcesHeader( PrintWriter pw, boolean doForm )
    {

        if ( doForm )
        {
            pw.println( "<form method='post'>" );
            pw.println( "<input type='hidden' name='" + Util.PARAM_ACTION + "' value='" + InstallFromRepoAction.NAME
                + "'>" );
        }

        pw.println( "<table class='content' cellpadding='0' cellspacing='0' width='100%'>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content container' colspan='3'>Available Resources</th>" );
        pw.println( "</tr>" );
        pw.println( "<tr class='content'>" );
        pw.println( "<th class='content'>Deploy</th>" );
        pw.println( "<th class='content'>Name</th>" );
        pw.println( "<th class='content'>Version</th>" );
        pw.println( "</tr>" );
    }


    /*
        private void listResources(PrintWriter pw) {
            InstallerService is = getInstallerService();
            if (is == null) {
                return;
            }

            Map<String, Version> bundles = this.getBundles();

            Iterator<?> resources = is.getBundleRepositoryAdmin().getResources();
            SortedSet<Resource> resSet = new TreeSet<Resource>(
                new Comparator<Resource>() {
                    public int compare(Resource o1, Resource o2) {
                        if (o1 == o2 || o1.equals(o2)) {
                            return 0;
                        }

                        if (o1.getPresentationName().equals(
                            o2.getPresentationName())) {
                            return o1.getVersion().compareTo(o2.getVersion());
                        }

                        return o1.getPresentationName().compareTo(
                            o2.getPresentationName());
                    }
                });

            while (resources.hasNext()) {
                Resource res = (Resource) resources.next();
                Version ver = bundles.get(res.getSymbolicName());
                if (ver == null || ver.compareTo(res.getVersion()) < 0) {
                    resSet.add(res);
                }
            }

            this.resourcesHeader(pw, !resSet.isEmpty());

            for (Resource resource : resSet) {
                this.printResource(pw, resource);
            }

            this.resourcesFooter(pw, !resSet.isEmpty());
        }

        private void printResource(PrintWriter pw, Resource res) {
            pw.println("<tr class='content'>");
            pw.println("<td class='content' valign='top' align='center'><input class='checkradio' type='checkbox' name='bundle' value='"
                + res.getSymbolicName() + "," + res.getVersion() + "'></td>");

            // check whether the resource is an assembly (category name)
            String style = "";
            String[] cat = res.getCategories();
            for (int i = 0; cat != null && i < cat.length; i++) {
                if ("assembly".equals(cat[i])) {
                    style = "style='font-weight:bold'";
                }
            }
            pw.println("<td class='content' " + style + ">"
                + res.getPresentationName() + " (" + res.getSymbolicName()
                + ")</td>");
            pw.println("<td class='content' " + style + " valign='top'>"
                + res.getVersion() + "</td>");

            pw.println("</tr>");
        }
    */
    private void resourcesButtons( PrintWriter pw )
    {
        pw.println( "<tr class='content'>" );
        pw.println( "<td class='content'>&nbsp;</td>" );
        pw.println( "<td class='content' colspan='2'>" );
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

}
