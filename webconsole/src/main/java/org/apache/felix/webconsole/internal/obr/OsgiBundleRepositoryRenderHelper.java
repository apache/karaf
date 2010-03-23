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
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.json.JSONException;
import org.json.JSONObject;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.obr.Capability;
import org.osgi.service.obr.Repository;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Requirement;
import org.osgi.service.obr.Resolver;
import org.osgi.service.obr.Resource;


/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
public class OsgiBundleRepositoryRenderHelper extends AbstractBundleRepositoryRenderHelper
{
    public OsgiBundleRepositoryRenderHelper( final AbstractWebConsolePlugin logger, final BundleContext bundleContext )
    {
        super( logger, bundleContext, RepositoryAdmin.class.getName() );
    }


    String getData( final String filter, final boolean details, Bundle[] bundles )
    {
        RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
        if ( admin != null )
        {
            try
            {
                JSONObject json = new JSONObject();
                json.put( "status", admin != null );
                json.put( "details", details );

                final Repository repositories[] = admin.listRepositories();
                for ( int i = 0; repositories != null && i < repositories.length; i++ )
                {
                    json.append( "repositories", new JSONObject().put( "lastModified",
                        repositories[i].getLastModified() ).put( "name", repositories[i].getName() ).put( "url",
                        repositories[i].getURL() ) );
                }

                Resource[] resources = admin.discoverResources( filter );
                for ( int i = 0; resources != null && i < resources.length; i++ )
                {
                    json.append( "resources", toJSON( resources[i], bundles, details ) );
                }

                return json.toString();
            }
            catch ( JSONException e )
            {
                logger.log( "Failed to serialize repository to JSON object.", e );
            }
        }

        // fall back to no data
        return "{}";
    }


    private String parseRequirement( String req )
    {
        int p = req.indexOf( ':' );
        String filter;
        if ( p > 0 )
        {
            filter = req.substring( p + 1 );
        }
        else
        {
            filter = req;
        }
        if ( !filter.startsWith( "(" ) )
        {
            filter = "(" + filter + ")";
        }
        return filter;
    }


    void doAction( String action, String urlParam ) throws IOException, ServletException
    {
        RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
        Repository[] repos = admin.listRepositories();
        Repository repo = getRepository( repos, urlParam );

        URL uri = repo != null ? repo.getURL() : new URL( urlParam );

        if ( "delete".equals( action ) )
        {
            if ( !admin.removeRepository( uri ) )
            {
                throw new ServletException( "Failed to remove repository with URL " + uri );
            }
        }
        else if ( "add".equals( action ) || "refresh".equals( action ) )
        {
            try
            {
                admin.addRepository( uri );
            }
            catch ( IOException e )
            {
                throw e;
            }
            catch ( Exception e )
            {
                throw new ServletException( "Failed to " + action + " repository " + uri + ": " + e.toString() );
            }

        }
    }


    final void doDeploy( String[] bundles, boolean start, boolean optional )
    {
        // check whether we have to do something
        if ( bundles == null || bundles.length == 0 )
        {
            logger.log( "No resources to deploy" );
            return;
        }

        RepositoryAdmin repoAdmin = ( RepositoryAdmin ) getRepositoryAdmin();
        Resolver resolver = repoAdmin.resolver();

        // prepare the deployment
        for ( int i = 0; i < bundles.length; i++ )
        {
            String bundle = bundles[i];
            if ( bundle == null || bundle.equals( "-" ) )
            {
                continue;
            }

            String filter = "(id=" + bundle + ")";
            Resource[] resources = repoAdmin.discoverResources( filter );
            if ( resources != null && resources.length > 0 )
            {
                resolver.add( resources[0] );
            }
        }

        OsgiDeployer.deploy( resolver, logger, start );
    }


    private final Repository getRepository( Repository[] repos, String repositoryUrl )
    {
        if ( repositoryUrl == null || repositoryUrl.length() == 0 )
        {
            return null;
        }

        for ( int i = 0; i < repos.length; i++ )
        {
            if ( repositoryUrl.equals( repos[i].getURL().toString() ) )
            {
                return repos[i];
            }
        }

        return null;
    }


    private final JSONObject toJSON( Resource resource, Bundle[] bundles, boolean details ) throws JSONException
    {
        final String symbolicName = resource.getSymbolicName();
        final String version = resource.getVersion().toString();
        boolean installed = false;
        for ( int i = 0; symbolicName != null && !installed && bundles != null && i < bundles.length; i++ )
        {
            final String ver = ( String ) bundles[i].getHeaders( "" ).get( Constants.BUNDLE_VERSION );
            installed = symbolicName.equals( bundles[i].getSymbolicName() ) && version.equals( ver );
        }
        JSONObject json = new JSONObject( resource.getProperties() ) //
            .put( "id", resource.getId() ) //
            .put( "presentationname", resource.getPresentationName() ) //
            .put( "symbolicname", symbolicName ) //
            .put( "url", resource.getURL() ) //
            .put( "version", version ) //
            .put( "categories", resource.getCategories() ) //
            .put( "installed", installed );

        if ( details )
        {
            Capability[] caps = resource.getCapabilities();
            for ( int i = 0; caps != null && i < caps.length; i++ )
            {
                json.append( "capabilities", new JSONObject().put( "name", caps[i].getName() ).put( "properties",
                    new JSONObject( caps[i].getProperties() ) ) );
            }
            Requirement[] reqs = resource.getRequirements();
            for ( int i = 0; reqs != null && i < reqs.length; i++ )
            {
                json.append( "requirements", new JSONObject().put( "name", reqs[i].getName() ).put( "filter",
                    reqs[i].getFilter() ).put( "optional", reqs[i].isOptional() ) );
            }

            final RepositoryAdmin admin = ( RepositoryAdmin ) getRepositoryAdmin();
            Resolver resolver = admin.resolver();
            resolver.add( resource );
            resolver.resolve(); // (Resolver.NO_OPTIONAL_RESOURCES);
            Resource[] required = resolver.getRequiredResources();
            for ( int i = 0; required != null && i < required.length; i++ )
            {
                json.append( "required", toJSON( required[i], bundles, false ) );
            }
            Resource[] optional = resolver.getOptionalResources();
            for ( int i = 0; optional != null && i < optional.length; i++ )
            {
                json.append( "optional", toJSON( optional[i], bundles, false ) );
            }
            Requirement/*Reason*/[] unsatisfied = resolver.getUnsatisfiedRequirements();
            for ( int i = 0; unsatisfied != null && i < unsatisfied.length; i++ )
            {
                json.append( "unsatisfied", new JSONObject().put( "name", unsatisfied[i].getName() ).put( "filter",
                    unsatisfied[i].getFilter() ).put( "optional", unsatisfied[i].isOptional() ) );
            }
        }
        return json;
    }

}
