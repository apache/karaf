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
package org.apache.felix.webconsole.internal.deppack;


import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;
import org.apache.felix.webconsole.internal.Util;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.service.deploymentadmin.DeploymentAdmin;
import org.osgi.service.deploymentadmin.DeploymentException;
import org.osgi.service.deploymentadmin.DeploymentPackage;


/**
 * DepPackServlet provides a plugin for managing deployment admin packages.
 */
public class DepPackServlet extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{

    private static final String LABEL = "deppack";
    private static final String TITLE = "%deployment.pluginTitle";
    private static final String CSS[] = { "/res/ui/deployment.css" };

    //
    private static final String ACTION_DEPLOY = "deploydp";
    private static final String ACTION_UNINSTALL = "uninstalldp";
    private static final String PARAMETER_PCK_FILE = "pckfile";

    private static final String DEPL_SERVICE = DeploymentAdmin.class.getName();

    // templates
    private final String TEMPLATE;

    /** Default constructor */
    public DepPackServlet()
    {
        super(LABEL, TITLE, CSS);

        // load templates
        TEMPLATE = readTemplateFile( "/templates/deployment.html" );
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        // get the uploaded data
        final String action = WebConsoleUtil.getParameter( req, Util.PARAM_ACTION );
        if ( ACTION_DEPLOY.equals( action ) )
        {
            Map params = ( Map ) req.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
            if ( params != null )
            {
                final FileItem pck = getFileItem( params, PARAMETER_PCK_FILE, false );
                final DeploymentAdmin admin = ( DeploymentAdmin ) this.getService( DEPL_SERVICE );
                if ( admin != null )
                {
                    try
                    {
                        admin.installDeploymentPackage( pck.getInputStream() );

                        final String uri = req.getRequestURI();
                        resp.sendRedirect( uri );
                        return;
                    }
                    catch ( DeploymentException e )
                    {
                        throw new ServletException( "Unable to deploy package.", e );
                    }
                }
            }
            throw new ServletException( "Upload file or deployment admin missing." );
        }
        else if ( ACTION_UNINSTALL.equals( action ) )
        {
            final String pckId = req.getPathInfo().substring( req.getPathInfo().lastIndexOf( '/' ) + 1 );
            if ( pckId != null && pckId.length() > 0 )
            {
                final DeploymentAdmin admin = ( DeploymentAdmin ) this.getService( DEPL_SERVICE );
                if ( admin != null )
                {
                    try
                    {
                        final DeploymentPackage pck = admin.getDeploymentPackage( pckId );
                        if ( pck != null )
                        {
                            pck.uninstall();
                        }
                    }
                    catch ( DeploymentException e )
                    {
                        throw new ServletException( "Unable to undeploy package.", e );
                    }
                }

            }

            final PrintWriter pw = resp.getWriter();
            pw.println( "{ \"reload\":true }" );
            return;
        }
        throw new ServletException( "Unknown action: " + action );
    }


    private static final FileItem getFileItem( Map params, String name, boolean isFormField )
    {
        FileItem[] items = ( FileItem[] ) params.get( name );
        if ( items != null )
        {
            for ( int i = 0; i < items.length; i++ )
            {
                if ( items[i].isFormField() == isFormField )
                {
                    return items[i];
                }
            }
        }

        // nothing found, fail
        return null;
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {

        final DeploymentAdmin admin = ( DeploymentAdmin ) this.getService( DEPL_SERVICE );

        StringWriter w = new StringWriter();
        PrintWriter w2 = new PrintWriter(w);
        JSONWriter jw = new JSONWriter( w2 );
        try
        {
            jw.object();
            if ( null == admin )
            {
                jw.key( "error" );
                jw.value( true );
            }
            else
            {
                final DeploymentPackage[] packages = admin.listDeploymentPackages();
                jw.key( "data" );

                jw.array();
                for ( int i = 0; i < packages.length; i++ )
                {
                    packageInfoJson( jw, packages[i] );
                }
                jw.endArray();

            }
            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", w.toString() );

        response.getWriter().print(TEMPLATE);
    }


    private static final void packageInfoJson( JSONWriter jw, DeploymentPackage pack ) throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( pack.getName() );
        jw.key( "name" );
        jw.value( pack.getName() );
        jw.key( "state" );
        jw.value( pack.getVersion() );

        jw.key( "actions" );
        jw.array();

        jw.object();
        jw.key( "enabled" );
        jw.value( true );
        jw.key( "name" );
        jw.value( "Uninstall" );
        jw.key( "link" );
        jw.value( ACTION_UNINSTALL );
        jw.endObject();

        jw.endArray();

        jw.key( "props" );
        jw.array();
        WebConsoleUtil.keyVal( jw, "Package Name", pack.getName() );
        WebConsoleUtil.keyVal( jw, "Version", pack.getVersion() );

        final StringBuffer buffer = new StringBuffer();
        for ( int i = 0; i < pack.getBundleInfos().length; i++ )
        {
            buffer.append( pack.getBundleInfos()[i].getSymbolicName() );
            buffer.append( " - " );
            buffer.append( pack.getBundleInfos()[i].getVersion() );
            buffer.append( "<br/>" );
        }
        WebConsoleUtil.keyVal( jw, "Bundles", buffer.toString() );

        jw.endArray();

        jw.endObject();
    }

}
