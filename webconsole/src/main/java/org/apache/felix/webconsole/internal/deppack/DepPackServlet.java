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
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.internal.BaseWebConsolePlugin;
import org.apache.felix.webconsole.internal.Util;
import org.apache.felix.webconsole.internal.servlet.OsgiManager;
import org.json.JSONException;
import org.json.JSONWriter;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.deploymentadmin.*;


/**
 * @scr.component metatype="false"
 * @scr.service interface="javax.servlet.Servlet"
 * @scr.property name="felix.webconsole.label" valueRef="LABEL"
 */
public class DepPackServlet extends BaseWebConsolePlugin
{

    public static final String LABEL = "deppack";

    public static final String TITLE = "Deployment Packages";

    private static final String ACTION_DEPLOY = "deploydp";

    private static final String ACTION_UNINSTALL = "uninstalldp";

    private static final String PARAMETER_PCK_FILE = "pckfile";

    public String getLabel()
    {
        return LABEL;
    }


    public String getTitle()
    {
        return TITLE;
    }


    protected void activate(ComponentContext context) {
        this.activate(context.getBundleContext());
    }

    protected void deactivate(ComponentContext context) {
        this.deactivate();
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException
    {
        // get the uploaded data
        final String action = getParameter(req, Util.PARAM_ACTION);
        if ( ACTION_DEPLOY.equals(action))
        {
            Map params = ( Map ) req.getAttribute( AbstractWebConsolePlugin.ATTR_FILEUPLOAD );
            if ( params != null )
            {
                final FileItem pck = getFileItem( params, PARAMETER_PCK_FILE, false );
                final DeploymentAdmin admin = (DeploymentAdmin) this.getService(DeploymentAdmin.class.getName());
                if ( admin != null )
                {
                    try
                    {
                        admin.installDeploymentPackage(pck.getInputStream());

                        final String uri = req.getRequestURI();
                        resp.sendRedirect( uri );
                        return;
                    }
                    catch (DeploymentException e)
                    {
                        throw new ServletException("Unable to deploy package.", e);
                    }
                }
            }
            throw new ServletException("Upload file or deployment admin missing.");
        }
        else if (ACTION_UNINSTALL.equals(action))
        {
            final String pckId = req.getPathInfo().substring( req.getPathInfo().lastIndexOf( '/' ) + 1 );
            if ( pckId != null && pckId.length() > 0 )
            {
                final DeploymentAdmin admin = (DeploymentAdmin) this.getService(DeploymentAdmin.class.getName());
                if ( admin != null )
                {
                    try
                    {
                        final DeploymentPackage pck = admin.getDeploymentPackage(pckId);
                        if ( pck != null )
                        {
                            pck.uninstall();
                        }
                    }
                    catch (DeploymentException e)
                    {
                        throw new ServletException("Unable to undeploy package.", e);
                    }
                }

            }

            final PrintWriter pw = resp.getWriter();
            pw.println("{ \"reload\":true }");
            return;
        }
        throw new ServletException("Unknown action: " + action);
    }

    private FileItem getFileItem( Map params, String name, boolean isFormField )
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

    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {

        PrintWriter pw = response.getWriter();

        String appRoot = ( String ) request.getAttribute( OsgiManager.ATTR_APP_ROOT );
        pw.println( "<script src='" + appRoot + "/res/ui/packages.js' language='JavaScript'></script>" );

        pw.println("<h1>Deployment Admin</h1>");
        final DeploymentAdmin admin = (DeploymentAdmin) this.getService(DeploymentAdmin.class.getName());
        if ( admin == null ) {
            pw.println("<p><em>Deployment Admin is not installed.</em></p>");
            return;
        }
        final DeploymentPackage[] packages = admin.listDeploymentPackages();

        Util.startScript( pw );
        pw.println( "var packageListData = " );
        JSONWriter jw = new JSONWriter( pw );
        try
        {
            jw.object();

            jw.key( "data" );

            jw.array();

            for(int i=0; i<packages.length; i++)
            {
                packageInfoJson( jw, packages[i] );
            }

            jw.endArray();

            jw.endObject();

        }
        catch ( JSONException je )
        {
            throw new IOException( je.toString() );
        }

        pw.println( ";" );
        pw.println( "renderPackage( packageListData );" );
        Util.endScript( pw );
    }

    private void packageInfoJson( JSONWriter jw, DeploymentPackage pack)
    throws JSONException
    {
        jw.object();
        jw.key( "id" );
        jw.value( pack.getName() );
        jw.key( "name" );
        jw.value( pack.getName());
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
        keyVal( jw, "Package Name", pack.getName() );
        keyVal( jw, "Version", pack.getVersion() );

        final StringBuffer buffer = new StringBuffer();
        for(int i=0; i<pack.getBundleInfos().length; i++) {
            buffer.append(pack.getBundleInfos()[i].getSymbolicName() );
            buffer.append(" - " );
            buffer.append(pack.getBundleInfos()[i].getVersion() );
            buffer.append("<br/>");
        }
        keyVal(jw, "Bundles", buffer.toString());

        jw.endArray();

        jw.endObject();
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
}
