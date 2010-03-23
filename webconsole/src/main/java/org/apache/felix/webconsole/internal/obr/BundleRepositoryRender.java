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

import org.apache.felix.webconsole.DefaultVariableResolver;
import org.apache.felix.webconsole.SimpleWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.apache.felix.webconsole.internal.OsgiManagerPlugin;


/**
 * This class provides a plugin for rendering the available OSGi Bundle Repositories
 * and the resources they provide.
 */
public class BundleRepositoryRender extends SimpleWebConsolePlugin implements OsgiManagerPlugin
{
    private static final String LABEL = "obr";
    private static final String TITLE = "OSGi Repository";
    private static final String[] CSS =
        { "/res/ui/obr.css" };

    // templates
    private final String TEMPLATE;

    private AbstractBundleRepositoryRenderHelper helper;


    /**
     *
     */
    public BundleRepositoryRender()
    {
        super( LABEL, TITLE, CSS );

        // load templates
        TEMPLATE = readTemplateFile( "/templates/obr.html" );
    }


    public void deactivate()
    {
        if ( helper != null )
        {
            helper.dispose();
            helper = null;
        }

        super.deactivate();
    }


    /**
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest request, HttpServletResponse response ) throws IOException
    {
        // prepare variables
        DefaultVariableResolver vars = ( ( DefaultVariableResolver ) WebConsoleUtil.getVariableResolver( request ) );
        vars.put( "__data__", getData( request ) );

        response.getWriter().print( TEMPLATE );
    }


    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException,
        IOException
    {
        if ( !hasRepositoryAdmin() )
        {
            response.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "RepositoryAdmin service is missing" );
            return;
        }

        final String action = request.getParameter( "action" );
        final String deploy = request.getParameter( "deploy" );
        final String deploystart = request.getParameter( "deploystart" );
        final String optional = request.getParameter( "optional" );

        if ( action != null )
        {
            doAction( action, request.getParameter( "url" ) );
            response.getWriter().print( getData( request ) );
            return;
        }

        if ( deploy != null || deploystart != null )
        {
            doDeploy( request.getParameterValues( "bundle" ), deploystart != null, optional != null );
            doGet( request, response );
            return;
        }

        super.doPost( request, response );
    }


    AbstractBundleRepositoryRenderHelper getHelper()
    {
        if ( helper == null )
        {
            try
            {
                helper = new FelixBundleRepositoryRenderHelper( this, getBundleContext() );
            }
            catch ( Throwable felixt )
            {
                // ClassNotFoundException, ClassDefNotFoundError

                try
                {
                    helper = new OsgiBundleRepositoryRenderHelper( this, getBundleContext() );
                }
                catch ( Throwable osgit )
                {
                    // ClassNotFoundException, ClassDefNotFoundError
                }
            }
        }

        return helper;
    }


    private boolean hasRepositoryAdmin()
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        return helper != null && helper.hasRepositoryAdmin();
    }


    private String getData( final HttpServletRequest request )
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        if ( helper == null || !helper.hasRepositoryAdmin() )
        {
            return "{}";
        }

        RequestInfo info = new RequestInfo( request );

        final String filter;
        String list = info.getList();
        if ( list != null )
        {
            if ( "-".equals( list ) )
            {
                StringBuffer sb = new StringBuffer( "(!(|" );
                for ( int c = 0; c < 26; c++ )
                {
                    sb.append( "(presentationname=" ).append( ( char ) ( 'a' + c ) ).append( "*)(presentationname=" )
                        .append( ( char ) ( 'A' + c ) ).append( "*)" );
                }
                sb.append( "))" );
                filter = sb.toString();
            }
            else
            {
                filter = "(|(presentationname=" + list.toLowerCase() + "*)(presentationname=" + list.toUpperCase()
                    + "*))";
            }
        }
        else
        {
            String query = info.getQuery();
            if ( query != null )
            {
                if ( query.indexOf( '=' ) > 0 )
                {
                    if ( query.startsWith( "(" ) )
                    {
                        filter = query;
                    }
                    else
                    {
                        filter = "(" + query + ")";
                    }
                }
                else
                {
                    filter = "(|(presentationame=*" + query + "*)(symbolicname=*" + query + "*))";
                }
            }
            else
            {
                StringBuffer sb = new StringBuffer( "(&" );
                for ( Enumeration e = request.getParameterNames(); e.hasMoreElements(); )
                {
                    String k = ( String ) e.nextElement();
                    String v = request.getParameter( k );
                    if ( v != null && v.length() > 0 && !"details".equals( k ) && !"deploy".equals( k )
                        && !"deploystart".equals( k ) && !"bundle".equals( k ) && !"optional".equals( k ) )
                    {
                        sb.append( "(" ).append( k ).append( "=" ).append( v ).append( ")" );
                    }
                }
                sb.append( ")" );
                filter = sb.toString();
            }
        }

        return helper.getData( filter, info.showDetails(), getBundleContext().getBundles() );
    }


    private void doAction( String action, String urlParam ) throws IOException, ServletException
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        if ( helper != null )
        {
            helper.doAction( action, urlParam );
        }
    }


    private void doDeploy( String[] bundles, boolean start, boolean optional )
    {
        AbstractBundleRepositoryRenderHelper helper = getHelper();
        if ( helper != null )
        {
            helper.doDeploy( bundles, start, optional );
        }
    }

    private static class RequestInfo {

        private final HttpServletRequest request;

        private boolean details;
        private String query;
        private String list;


        RequestInfo( final HttpServletRequest request )
        {
            this.request = request;
        }


        boolean showDetails()
        {
            getQuery();
            return details;
        }


        String getQuery()
        {
            if ( query == null )
            {
                String query = WebConsoleUtil.urlDecode( request.getParameter( "query" ) );
                boolean details = false;
                if ( query == null && request.getPathInfo().length() > 5 )
                {
                    // cut off "/obr/" prefix (might want to use getTitle ??)
                    String path = request.getPathInfo().substring( 5 );
                    int slash = path.indexOf( '/' );
                    if ( slash < 0 )
                    {
                        // symbolic name only, version ??
                        query = "(symbolicname=" + path + ")";
                    }
                    else
                    {
                        query = "(&(symbolicname=" + path.substring( 0, slash ) + ")(version="
                            + path.substring( slash + 1 ) + "))";
                        details = true;
                    }
                }

                this.query = query;
                this.details = details;
            }
            return query;
        }


        String getList()
        {
            if ( list == null )
            {
                list = WebConsoleUtil.urlDecode( request.getParameter( "list" ) );
                if ( list == null && !request.getParameterNames().hasMoreElements() && getQuery() == null )
                {
                    list = "a";
                }
            }
            return list;
        }
    }
}
