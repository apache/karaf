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
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.Logger;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;

/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
public class BundleRepositoryRender extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{
    private static final String LABEL = "obr";
    private static final String TITLE = "OSGi Repository";
    private static final String[] CSS = null;

    // Define a constant of that name to prevent NoClassDefFoundError in
    // updateFromOBR trying to load the class with RepositoryAdmin.class
    private static final String REPOSITORY_ADMIN_NAME = "org.osgi.service.obr.RepositoryAdmin";

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
        // prepare variables
        DefaultVariableResolver vars = ((DefaultVariableResolver) WebConsoleUtil.getVariableResolver(request));
        vars.put("__data__", getData());

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

        if (action != null)
        {
            doAction(action, request.getParameter("url"), admin);
            response.getWriter().print(getData());
            return;
        }

        if (deploy != null || deploystart != null)
        {
            doDeploy(request.getParameterValues("bundle"), deploystart != null, admin);
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

    private final String getData()
    {
        final Bundle[] bundles = getBundleContext().getBundles();
        try
        {
            return toJSON(getRepositoryAdmin(), bundles).toString();
        }
        catch (JSONException e)
        {
            log("Failed to serialize repository to JSON object.", e);
            return "";
        }

    }

    private final void doAction(String action, String urlParam, RepositoryAdmin admin)
        throws IOException, ServletException
    {
        Repository[] repos = admin.listRepositories();
        Repository repo = getRepository(repos, urlParam);

        URL url = repo != null ? repo.getURL() : new URL(urlParam);

        if ("delete".equals(action))
        {
            if (!admin.removeRepository(url))
            {
                throw new ServletException("Failed to remove repository with URL " + url);
            }
        }
        else if ("add".equals(action) || "refresh".equals(action))
        {
            try
            {
                admin.addRepository(url);
            }
            catch (IOException e)
            {
                throw e;
            }
            catch (Exception e)
            {
                throw new ServletException("Failed to " + action + " repository " + url
                    + ": " + e.toString());
            }

        }
    }

    private final void doDeploy(String[] bundles, boolean start, RepositoryAdmin repoAdmin)
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

        DeployerThread dt = new DeployerThread(resolver, new Logger(getBundleContext()),
            start);
        dt.start();
    }

    private static final Repository getRepository(Repository[] repos, String repositoryUrl)
    {
        if (repositoryUrl == null || repositoryUrl.length() == 0)
        {
            return null;
        }

        for (int i = 0; i < repos.length; i++)
        {
            if (repositoryUrl.equals(repos[i].getURL().toString()))
            {
                return repos[i];
            }
        }

        return null;
    }

    private static final JSONObject toJSON(RepositoryAdmin admin, Bundle[] bundles)
        throws JSONException
    {
        JSONObject json = new JSONObject();
        json.put("status", admin != null);

        if (admin != null)
        {
            final Repository repositories[] = admin.listRepositories();
            for (int i = 0; repositories != null && i < repositories.length; i++)
            {
                json.append("repositories", toJSON(repositories[i], bundles));
            }
        }

        return json;

    }

    private static final JSONObject toJSON(Repository repo, Bundle[] bundles)
        throws JSONException
    {
        JSONObject json = new JSONObject() //
        .put("lastModified", repo.getLastModified()) //
        .put("name", repo.getName()) //
        .put("url", repo.getURL()); //

        Resource[] resources = repo.getResources();
        for (int i = 0; resources != null && i < resources.length; i++)
        {
            json.append("resources", toJSON(resources[i], bundles));
        }

        return json;
    }

    private static final JSONObject toJSON(Resource resource, Bundle[] bundles)
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
        .put("url", resource.getURL()) //
        .put("version", version) //
        .put("url", resource.getURL()) //
        .put("categories", resource.getCategories()) //
        .put("installed", installed);

        // TODO: do we need these ?
        // resource.getCapabilities()
        // resource.getRequirements()
        return json;
    }

}
