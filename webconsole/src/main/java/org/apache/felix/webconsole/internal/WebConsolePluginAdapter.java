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
package org.apache.felix.webconsole.internal;


import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.apache.felix.webconsole.WebConsoleConstants;
import org.osgi.framework.ServiceReference;


/**
 * The <code>WebConsolePluginAdapter</code> is an adapter to the
 * {@link AbstractWebConsolePlugin} for regular servlets registered with the
 * {@link org.apache.felix.webconsole.WebConsoleConstants#PLUGIN_TITLE}
 * service attribute.
 */
public class WebConsolePluginAdapter extends AbstractWebConsolePlugin
{

    /** serial UID */
    private static final long serialVersionUID = 1L;

    // the plugin label (aka address)
    private final String label;

    // the actual plugin to forward rendering requests to
    private final Servlet plugin;

    // the CSS references (null if none)
    private final String[] cssReferences;

    /**
     * Creates a new wrapper for a Web Console Plugin
     *
     * @param label the label
     * @param title the title
     * @param plugin the plugin itself
     * @param serviceReference reference to the plugin
     */
    public WebConsolePluginAdapter( String label, Servlet plugin, ServiceReference serviceReference )
    {
        this.label = label;
        this.plugin = plugin;
        this.cssReferences = toStringArray( serviceReference.getProperty( WebConsoleConstants.PLUGIN_CSS_REFERENCES ) );

        // activate this abstract plugin (mainly to set the bundle context)
        activate( serviceReference.getBundle().getBundleContext() );
    }


    //---------- AbstractWebConsolePlugin API

    /**
     * Returns the label of this plugin page as defined in the constructor.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getLabel()
     */
    public String getLabel()
    {
        return label;
    }


    /**
     * Returns the title of this plugin page as defined by the
     * {@link WebConsoleConstants#PLUGIN_TITLE} service registration attribute
     * which is exposed as the servlet name in the servlet configuration.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getTitle()
     */
    public String getTitle()
    {
        // return the servlet name from the configuration but don't call
        // the base class implementation, which calls getTitle()
        return getServletConfig().getServletName();
    }


    /**
     * Returns the CSS references from the
     * {@link WebConsoleConstants#PLUGIN_CSS_REFERENCES felix.webconsole.css}
     * service registration property of the plugin.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getCssReferences()
     */
    protected String[] getCssReferences()
    {
        return cssReferences;
    }


    /**
     * Call the plugin servlet's service method to render the content of this
     * page.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#renderContent(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    protected void renderContent( HttpServletRequest req, HttpServletResponse res ) throws ServletException,
        IOException
    {
        plugin.service( req, res );
    }


    /**
     * Returns the registered plugin class to be able to call the
     * <code>getResource()</code> method on that object for this plugin to
     * provide additional resources.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#getResourceProvider()
     */
    protected Object getResourceProvider()
    {
        return plugin;
    }


    //---------- Servlet API overwrite

    /**
     * Initializes this servlet as well as the plugin servlet.
     *
     * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
     */
    public void init( ServletConfig config ) throws ServletException
    {
        // no need to activate the plugin, this has already been done
        // when the instance was setup
        try
        {
            // base classe initialization
            super.init( config );

            // plugin initialization
            plugin.init( config );
        }
        catch ( ServletException se )
        {
            // if init fails, the plugin will not be destroyed and thus
            // the plugin not deactivated. Do it here
            deactivate();

            // rethrow the exception
            throw se;
        }
    }


    /**
     * Detects whether this request is intended to have the headers and
     * footers of this plugin be rendered or not. The decision is taken based
     * on whether and what extension the request URI has: If the request URI
     * has no extension or the the extension is <code>.html</code>, the request
     * is assumed to be rendered with header and footer. Otherwise the
     * headers and footers are omitted and the
     * {@link #renderContent(HttpServletRequest, HttpServletResponse)}
     * method is called without any decorations and without setting any
     * response headers.
     *
     * @see org.apache.felix.webconsole.AbstractWebConsolePlugin#isHtmlRequest(javax.servlet.http.HttpServletRequest)
     */
    protected boolean isHtmlRequest( final HttpServletRequest request )
    {
        final String requestUri = request.getRequestURI();
        return requestUri.endsWith( ".html" ) || requestUri.lastIndexOf( '.' ) < 0;
    }


    /**
     * Directly refer to the plugin's service method unless the request method
     * is <code>GET</code> in which case we defer the call into the service method
     * until the abstract web console plugin calls the
     * {@link #renderContent(HttpServletRequest, HttpServletResponse)}
     * method.
     *
     * @see javax.servlet.http.HttpServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    public void service( ServletRequest req, ServletResponse resp ) throws ServletException, IOException
    {
        if ( ( req instanceof HttpServletRequest ) && ( ( HttpServletRequest ) req ).getMethod().equals( "GET" ) )
        {
            // handle the GET request here and call into plugin on renderContent
            super.service( req, resp );
        }
        else
        {
            // not a GET request, have the plugin handle it directly
            plugin.service( req, resp );
        }
    }


    /**
     * Destroys this servlet as well as the plugin servlet.
     *
     * @see javax.servlet.GenericServlet#destroy()
     */
    public void destroy()
    {
        try
        {
            plugin.destroy();
            super.destroy();
        }
        finally
        {
            deactivate();
        }
    }


    //---------- internal

    private String[] toStringArray( final Object value )
    {
        if ( value instanceof String )
        {
            return new String[]
                { ( String ) value };
        }
        else if ( value != null )
        {
            final Collection cssListColl;
            if ( value.getClass().isArray() )
            {
                cssListColl = Arrays.asList( ( Object[] ) value );
            }
            else if ( value instanceof Collection )
            {
                cssListColl = ( Collection ) value;
            }
            else
            {
                cssListColl = null;
            }

            if ( cssListColl != null && !cssListColl.isEmpty() )
            {
                String[] entries = new String[cssListColl.size()];
                int i = 0;
                for ( Iterator cli = cssListColl.iterator(); cli.hasNext(); i++ )
                {
                    entries[i] = String.valueOf( cli.next() );
                }
                return entries;
            }
        }

        return null;
    }
}
