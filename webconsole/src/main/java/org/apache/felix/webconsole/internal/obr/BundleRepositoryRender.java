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
package org.apache.felix.webconsole.internal.obr;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.bundlerepository.Capability;
import org.apache.felix.bundlerepository.Reason;
import org.apache.felix.bundlerepository.Requirement;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.apache.felix.bundlerepository.Repository;
import org.apache.felix.bundlerepository.RepositoryAdmin;
import org.apache.felix.bundlerepository.Resolver;
import org.apache.felix.bundlerepository.Resource;
import org.osgi.framework.InvalidSyntaxException;

/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
public class BundleRepositoryRender extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{
    private static final String LABEL = "obr";
    private static final String TITLE = "OSGi Repository";
    private static final String[] CSS = { "/res/ui/obr.css" };

    // Define a constant of that name to prevent NoClassDefFoundError in
    // updateFromOBR trying to load the class with RepositoryAdmin.class
    private static final String REPOSITORY_ADMIN_NAME = RepositoryAdmin.class.getName();

    // templates
    private final String TEMPLATE;

    /**
     *
     */
    public BundleRepositoryRender()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile("/templates/obr.html");
    }

    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        String query = request.getQueryString();
        if (query == null || query.length() == 0)
        {
            response.sendRedirect(LABEL + "?list=a");
            return;
        }
        // prepare variables
        DefaultVariableResolver vars = ((DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request));
        vars.put("__data__", getData(request));

        response.getWriter().print(TEMPLATE);
    }

    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        final RepositoryAdmin admin = getRepositoryAdmin();

        if (admin == null)
        {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                "RepositoryAdmin service is missing");
            return;
        }

        final String action = request.getParameter("action");
        final String deploy = request.getParameter("deploy");
        final String deploystart = request.getParameter("deploystart");
        final String optional = request.getParameter("optional");

        if (action != null)
        {
            doAction(action, request.getParameter("url"), admin);
            response.getWriter().print(getData(request));
            return;
        }

        if (deploy != null || deploystart != null)
        {
            doDeploy(request.getParameterValues("bundle"), deploystart != null, optional != null, admin);
            doGet(request, response);
            return;
        }

        super.doPost(request, response);
    }

    private final RepositoryAdmin getRepositoryAdmin()
    {
        try
        {
            return ( RepositoryAdmin ) super.getService( REPOSITORY_ADMIN_NAME );
        }
        catch (Throwable t)
        {
            log("Cannot create RepositoryAdmin service tracker", t);
            return null;
        }
    }

    private final String getData(HttpServletRequest request)
    {
        final Bundle[] bundles = getBundleContext().getBundles();
        try
        {
            RepositoryAdmin admin = getRepositoryAdmin();
            Resource[] resources = null;
            boolean details = request.getParameter("details") != null;
            if (admin != null)
            {
                String list = request.getParameter("list");
                String query = request.getParameter("query");
                if (list != null)
                {
                    String filter;
                    if ("-".equals(list))
                    {
                        StringBuffer sb = new StringBuffer("(!(|");
                        for (int c = 0; c < 26; c++)
                        {
                            sb.append("(presentationname=").append((char) ('a' + c))
                                    .append("*)(presentationname=")
                                    .append((char)('A' + c)).append("*)");
                        }
                        sb.append("))");
                        filter = sb.toString();
                    }
                    else
                    {
                        filter = "(|(presentationname=" + list.toLowerCase() + "*)(presentationname=" + list.toUpperCase() + "*))";
                    }
                    resources = admin.discoverResources(filter);
                }
                else if (query != null)
                {
                    if (query.indexOf('=') > 0)
                    {
                        resources = admin.discoverResources(new Requirement[] { parseRequirement(admin, query) });
                    }
                    else
                    {
                        resources = admin.discoverResources("(|(presentationame=*" + query + "*)(symbolicname=*" + query + "*))");
                    }
                }
                else
                {
                    StringBuffer sb = new StringBuffer("(&");
                    for (Enumeration e = request.getParameterNames(); e.hasMoreElements();)
                    {
                        String k = (String) e.nextElement();
                        String v = request.getParameter(k);
                        if (v != null && v.length() > 0
                                && !"details".equals(k) && !"deploy".equals(k)
                                && !"deploystart".equals(k) && !"bundle".equals(k)
                                && !"optional".equals(k))
                        {
                            sb.append("(").append(k).append("=").append(v).append(")");
                        }
                    }
                    sb.append(")");
                    resources = admin.discoverResources(sb.toString());

                }
            }
            JSONObject json = new JSONObject();
            json.put("status", admin != null);
            if (admin != null)
            {
                final Repository repositories[] = admin.listRepositories();
                for (int i = 0; repositories != null && i < repositories.length; i++)
                {
                    json.append("repositories", new JSONObject()
                            .put("lastModified", repositories[i].getLastModified())
                            .put("name", repositories[i].getName())
                            .put("url", repositories[i].getURI()));
                }
            }
            for (int i = 0; resources != null && i < resources.length; i++)
            {
                json.append("resources", toJSON(resources[i], bundles, details));
            }
            return json.toString();
        }
        catch (InvalidSyntaxException e)
        {
            log("Failed to parse filter.", e);
            return "";
        }
        catch (JSONException e)
        {
            log("Failed to serialize repository to JSON object.", e);
            return "";
        }
    }

    private Requirement parseRequirement(RepositoryAdmin admin, String req) throws InvalidSyntaxException {
        int p = req.indexOf(':');
        String name;
        String filter;
        if (p > 0) {
            name = req.substring(0, p);
            filter = req.substring(p + 1);
        } else {
            if (req.contains("package")) {
                name = "package";
            } else if (req.contains("service")) {
                name = "service";
            } else {
                name = "bundle";
            }
            filter = req;
        }
        if (!filter.startsWith("(")) {
            filter = "(" + filter + ")";
        }
        return admin.requirement(name, filter);
    }

    private final void doAction(String action, String urlParam, RepositoryAdmin admin)
        throws IOException, ServletException
    {
        Repository[] repos = admin.listRepositories();
        Repository repo = getRepository(repos, urlParam);

        String uri = repo != null ? repo.getURI() : urlParam;

        if ("delete".equals(action))
        {
            if (!admin.removeRepository(uri))
            {
                throw new ServletException("Failed to remove repository with URL " + uri);
            }
        }
        else if ("add".equals(action) || "refresh".equals(action))
        {
            try
            {
                admin.addRepository(uri);
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new ServletException("Failed to " + action + " repository " + uri
                    + ": " + e.toString());
            }

        }
    }

    private final void doDeploy(String[] bundles, boolean start, boolean optional, RepositoryAdmin repoAdmin)
    {
        try
        {
            // check whether we have to do something
            if (bundles == null || bundles.length == 0)
            {
                log("No resources to deploy");
                return;
            }

            Resolver resolver = repoAdmin.resolver();

            // prepare the deployment
            for (int i = 0; i < bundles.length; i++)
            {
                String bundle = bundles[i];
                if (bundle == null || bundle.equals("-"))
                {
                    continue;
                }

                String filter = "(id=" + bundle + ")";
                Resource[] resources = repoAdmin.discoverResources(filter);
                if (resources != null && resources.length > 0)
                {
                    resolver.add(resources[0]);
                }
            }

            DeployerThread dt = new DeployerThread( resolver, this, start, optional );
            dt.start();
        }
        catch (InvalidSyntaxException e)
        {
            throw new IllegalStateException(e);
        }
    }

    private final Repository getRepository(Repository[] repos, String repositoryUrl)
    {
        if (repositoryUrl == null || repositoryUrl.length() == 0)
        {
            return null;
        }

        for (int i = 0; i < repos.length; i++)
        {
            if (repositoryUrl.equals(repos[i].getURI()))
            {
                return repos[i];
            }
        }

        return null;
    }

    private final JSONObject toJSON(Resource resource, Bundle[] bundles, boolean details)
        throws JSONException
    {
        final String symbolicName = resource.getSymbolicName();
        final String version = resource.getVersion().toString();
        boolean installed = false;
        for (int i = 0; symbolicName != null && !installed && bundles != null
            && i < bundles.length; i++)
        {
            final String ver = (String) bundles[i].getHeaders("").get(
                Constants.BUNDLE_VERSION);
            installed = symbolicName.equals(bundles[i].getSymbolicName())
                && version.equals(ver);
        }
        JSONObject json = new JSONObject(resource.getProperties()) //
        .put("id", resource.getId()) //
        .put("presentationname", resource.getPresentationName()) //
        .put("symbolicname", symbolicName) //
        .put("url", resource.getURI()) //
        .put("version", version) //
        .put("categories", resource.getCategories()) //
        .put("installed", installed);

        if (details)
        {
            Capability[] caps = resource.getCapabilities();
            for (int i = 0; caps != null && i < caps.length; i++)
            {
                json.append("capabilities", new JSONObject().
                    put("name", caps[i].getName()).
                    put("properties", new JSONObject(caps[i].getProperties())));
            }
            Requirement[] reqs = resource.getRequirements();
            for (int i = 0; reqs != null && i < reqs.length; i++)
            {
                json.append("requirements", new JSONObject().
                    put("name", reqs[i].getName()).
                    put("filter", reqs[i].getFilter()).
                    put("optional", reqs[i].isOptional()));
            }

            final RepositoryAdmin admin = getRepositoryAdmin();
            Resolver resolver = admin.resolver();
            resolver.add(resource);
            resolver.resolve(Resolver.NO_OPTIONAL_RESOURCES);
            Resource[] required = resolver.getRequiredResources();
            for (int i = 0; required != null && i < required.length; i++)
            {
                json.append("required", toJSON(required[i], bundles, false));
            }
            Resource[] optional = resolver.getOptionalResources();
            for (int i = 0; optional != null && i < optional.length; i++)
            {
                json.append("optional", toJSON(optional[i], bundles, false));
            }
            Reason[] unsatisfied = resolver.getUnsatisfiedRequirements();
            for (int i = 0; unsatisfied != null && i < unsatisfied.length; i++)
            {
                json.append("unsatisfied", new JSONObject().
                    put("name", unsatisfied[i].getRequirement().getName()).
                    put("filter", unsatisfied[i].getRequirement().getFilter()).
                    put("optional", unsatisfied[i].getRequirement().isOptional()));
            }
        }
        return json;
    }

}
