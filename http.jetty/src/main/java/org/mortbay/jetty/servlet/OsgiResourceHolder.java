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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.http.jetty.Activator;
import org.apache.felix.http.jetty.ServletContextGroup;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.Request;
import org.osgi.service.http.HttpContext;
import org.osgi.service.log.LogService;


public class OsgiResourceHolder extends ServletHolder
{
    private ServletContextGroup m_servletContextGroup;
    private HttpContext m_osgiHttpContext;
    private AccessControlContext m_acc;


    public OsgiResourceHolder( ServletHandler handler, String name, ServletContextGroup servletContextGroup )
    {
        super();

        setServletHandler( handler );
        setName( name );

        m_servletContextGroup = servletContextGroup;
        m_osgiHttpContext = servletContextGroup.getOsgiHttpContext();

        if ( System.getSecurityManager() != null )
        {
            m_acc = AccessController.getContext();
        }
    }


    public synchronized Servlet getServlet()
    {
        return null;
    }


    // override "Holder" method to prevent instantiation
    public synchronized Object newInstance()
    {
        return null;
    }


    public void handle( ServletRequest sRequest, ServletResponse sResponse ) throws ServletException, IOException
    {
        HttpServletRequest request = ( HttpServletRequest ) sRequest;
        HttpServletResponse response = ( HttpServletResponse ) sResponse;
        String target = request.getPathInfo();

        Activator.debug( "handle for name:" + getName() + "(path=" + target + ")" );

        if ( !m_osgiHttpContext.handleSecurity( request, response ) )
        {
            return;
        }

        // Create resource based name and see if we can resolve it
        String resName = getName() + target;
        Activator.debug( "** looking for: " + resName );
        URL url = m_osgiHttpContext.getResource( resName );

        if ( url == null )
        {
            Request base_request = sRequest instanceof Request ? ( Request ) sRequest : HttpConnection
                .getCurrentConnection().getRequest();
            base_request.setHandled( false );
            return;
        }

        Activator.debug( "serving up:" + resName );

        String method = request.getMethod();
        if ( method.equals( HttpMethods.GET ) || method.equals( HttpMethods.POST ) || method.equals( HttpMethods.HEAD ) )
        {
            handleGet( request, response, url, resName );
        }
        else
        {
            try
            {
                response.sendError( HttpServletResponse.SC_NOT_IMPLEMENTED );
            }
            catch ( Exception e )
            {/*TODO: include error logging*/
            }
        }
    }


    public void handleGet( HttpServletRequest request, final HttpServletResponse response, final URL url, String resName )
        throws IOException
    {
        String encoding = m_osgiHttpContext.getMimeType( resName );

        if ( encoding == null )
        {
            encoding = m_servletContextGroup.getMimeType( resName );
        }

        if ( encoding == null )
        {
            encoding = m_servletContextGroup.getMimeType( ".default" );
        }

        //TODO: not sure why this is needed, but sometimes get "IllegalState"
        // errors if not included
        response.setContentType( encoding );

        //TODO: check other http fields e.g. ranges, timestamps etc.

        // make sure we access the resource inside the bundle's access control
        // context if supplied
        if ( m_acc != null )
        {
            try
            {
                AccessController.doPrivileged( new PrivilegedExceptionAction()
                {
                    public Object run() throws Exception
                    {
                        copyResourceBytes( url, response );
                        return null;
                    }
                }, m_acc );
            }
            catch ( PrivilegedActionException ex )
            {
                IOException ioe = ( IOException ) ex.getException();
                throw ioe;
            }
        }
        else
        {
            copyResourceBytes( url, response );
        }

        //TODO: set other http fields e.g. __LastModified, __ContentLength
    }


    private void copyResourceBytes( URL url, HttpServletResponse response ) throws IOException
    {
        OutputStream os = null;
        InputStream is = null;

        try
        {
            os = response.getOutputStream();
            is = url.openStream();

            int len = 0;
            byte[] buf = new byte[1024];
            int n = 0;

            while ( ( n = is.read( buf, 0, buf.length ) ) >= 0 )
            {
                os.write( buf, 0, n );
                len += n;
            }

            try
            {
                response.setContentLength( len );
            }
            catch ( IllegalStateException ex )
            {
                Activator.log( LogService.LOG_ERROR, "OsgiResourceHandler", ex );
            }
        }
        finally
        {
            if ( is != null )
            {
                is.close();
            }
            if ( os != null )
            {
                os.close();
            }
        }
    }


    // override "Holder" method to prevent attempt to load
    // the servlet class.
    public void doStart() throws Exception
    {
    }


    // override "Holder" method to prevent destroy, which is only called
    // when a bundle manually unregisters
    public void doStop()
    {
    }

}
