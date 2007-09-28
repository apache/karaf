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
package org.apache.felix.http.jetty;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import org.mortbay.http.HttpException;
import org.mortbay.http.HttpRequest;
import org.mortbay.http.HttpResponse;
import org.mortbay.http.handler.AbstractHttpHandler;
import org.mortbay.jetty.servlet.DummyServletHttpRequest;
import org.mortbay.jetty.servlet.DummyServletHttpResponse;
import org.mortbay.jetty.servlet.OsgiServletHandler;
import org.mortbay.jetty.servlet.ServletHttpRequest;
import org.mortbay.jetty.servlet.ServletHttpResponse;
import org.osgi.service.http.HttpContext;


public class OsgiResourceHandler extends AbstractHttpHandler
{
    protected HttpContext m_osgiHttpContext;
    protected String m_name;
    protected OsgiServletHandler m_dummyHandler;
    protected AccessControlContext m_acc;


    public OsgiResourceHandler( HttpContext osgiHttpContext, String name, AccessControlContext acc )
    {
        m_osgiHttpContext = osgiHttpContext;
        m_name = name;
        // needed for OSGi security handling
        m_dummyHandler = new OsgiServletHandler();
        m_acc = acc;
    }


    public void initialize( org.mortbay.http.HttpContext context )
    {
        super.initialize( context );
        m_dummyHandler.initialize( context );
    }


    public void handle( String pathInContext, String pathParams, HttpRequest request, HttpResponse response )
        throws HttpException, IOException
    {
        Activator.debug( "handle for name:" + m_name + "(path=" + pathInContext + ")" );

        ServletHttpRequest servletRequest = new DummyServletHttpRequest( m_dummyHandler, pathInContext, request );
        ServletHttpResponse servletResponse = new DummyServletHttpResponse( servletRequest, response );

        if ( !m_osgiHttpContext.handleSecurity( servletRequest, servletResponse ) )
        {
            // spec doesn't state specific processing here apart from
            // "send the response back to the client". We take this to mean
            // any response generated in the context, and so all we do here
            // is set handled to "true" to ensure any output is sent back
            request.setHandled( true );
            return;
        }

        // Create resource based name and see if we can resolve it
        String resName = m_name + pathInContext;
        Activator.debug( "** looking for: " + resName );
        URL url = m_osgiHttpContext.getResource( resName );

        if ( url == null )
        {
            return;
        }

        Activator.debug( "serving up:" + resName );

        // It doesn't state so in the OSGi spec, but can't see how anything
        // other than GET and variants would be supported
        String method = request.getMethod();
        if ( method.equals( HttpRequest.__GET ) || method.equals( HttpRequest.__POST )
            || method.equals( HttpRequest.__HEAD ) )
        {
            handleGet( request, response, url, resName );
        }
        else
        {
            try
            {
                response.sendError( HttpResponse.__501_Not_Implemented );
            }
            catch ( Exception e )
            {/*TODO: include error logging*/
            }
        }
    }


    public void handleGet( HttpRequest request, final HttpResponse response, final URL url, String resName )
        throws IOException
    {
        String encoding = m_osgiHttpContext.getMimeType( resName );

        if ( encoding == null )
        {
            encoding = getHttpContext().getMimeByExtension( resName );
        }

        if ( encoding == null )
        {
            encoding = getHttpContext().getMimeByExtension( ".default" );
        }

        //TODO: not sure why this is needed, but sometimes get "IllegalState"
        // errors if not included
        response.setAcceptTrailer( true );
        response.setContentType( encoding );

        //TODO: check other http fields e.g. ranges, timestamps etc.

        // make sure we access the resource inside the bundle's access control
        // context if supplied
        if ( System.getSecurityManager() != null )
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

        request.setHandled( true );
        //TODO: set other http fields e.g. __LastModified, __ContentLength
    }


    private void copyResourceBytes( URL url, HttpResponse response ) throws IOException
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
                System.err.println( "OsgiResourceHandler: " + ex );
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
}
