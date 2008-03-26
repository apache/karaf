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
package org.mortbay.jetty.servlet;


import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.jetty.ServletContextGroup;
import org.osgi.service.http.HttpContext;


public class OsgiServletHolder extends ServletHolder
{
    private Servlet m_servlet;
    private ServletContextGroup m_servletContextGroup;
    private ServletConfig m_config;


    public OsgiServletHolder( ServletHandler handler, Servlet servlet, String name,
        ServletContextGroup servletContextGroup, Dictionary params )
    {
        super(servlet);
        setServletHandler( handler );
        setName( name );
        
        m_servlet = servlet;
        m_servletContextGroup = servletContextGroup;

        // Seemed safer to copy params into parent holder, rather than override
        // the getInitxxx methods.
        if ( params != null )
        {
            Enumeration e = params.keys();
            while ( e.hasMoreElements() )
            {
                Object key = e.nextElement();
                setInitParameter( String.valueOf( key ), String.valueOf( params.get( key ) ) );
            }
        }
    }


    public synchronized Servlet getServlet()
    {
        return m_servlet;
    }


    public Servlet getOsgiServlet()
    {
        return m_servlet;
    }


    // override "Holder" method to prevent instantiation
    public synchronized Object newInstance()
    {
        return getOsgiServlet();
    }


    public void handle( ServletRequest request, ServletResponse response ) throws ServletException,
        UnavailableException, IOException
    {
        if ( m_servletContextGroup.getOsgiHttpContext().handleSecurity( ( HttpServletRequest ) request,
            ( HttpServletResponse ) response ) )
        {
            // wrap request to get auth type and remote user
            request = new HttpServiceHttpServletRequest( ( HttpServletRequest ) request );

            // service request
            super.handle( request, response );
        }
        else
        {
            //TODO: any other error/auth handling we should do in here?

            // response.flushBuffer() if available
            try
            {
                response.getClass().getDeclaredMethod( "flushBuffer", null ).invoke( response, null );
            }
            catch ( Exception ex )
            {
                // else ignore
                ex.printStackTrace();
            }
        }
    }


    // override "Holder" method to prevent attempt to load
    // the servlet class.
    public void doStart() throws Exception
    {
        _class = m_servlet.getClass();

        m_config = new Config()
        {
            public ServletContext getServletContext()
            {
                return m_servletContextGroup;
            }
        };

        m_servlet.init( m_config );
    }


    // override "Holder" method to prevent destroy, which is only called
    // when a bundle manually unregisters
    public void doStop()
    {
    }

    // Simple wrapper class returning the authentication type and remote user
    // from the respective request attribute as specificed by the HttpService
    // spec (step 4 in Section 102.7, Authentication)
    private static class HttpServiceHttpServletRequest extends HttpServletRequestWrapper {

        HttpServiceHttpServletRequest(HttpServletRequest request) {
            super(request);
        }

        public String getAuthType()
        {
            // use the auth type attribute; should not be null, but
            // better check and use wrapped result if missing
            Object name = getAttribute( HttpContext.AUTHENTICATION_TYPE );
            if (name != null) {
                return name.toString();
            }

            return super.getAuthType();
        }

        public String getRemoteUser()
        {
            // use the remote user attribute; should not be null, but
            // better check and use wrapped result if missing
            Object name = getAttribute( HttpContext.REMOTE_USER );
            if (name != null) {
                return name.toString();
            }

            return super.getRemoteUser();
        }
    }
}
