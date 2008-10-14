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
import java.io.File;
import java.net.URL;
import java.net.URLConnection;
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
    private String m_path;


    public OsgiResourceHolder( ServletHandler handler, String name, String path, ServletContextGroup servletContextGroup )
    {
        super();

        setServletHandler( handler );
        setName( name );

        m_servletContextGroup = servletContextGroup;
        m_osgiHttpContext = servletContextGroup.getOsgiHttpContext();
        m_path = path;
        if (m_path == null)
        {
            m_path = "";
        }
        
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
        
        // get the relative path (assume empty path if there is no path info)
        // (FELIX-503)
        String target = request.getPathInfo();
        if (target == null) 
        {
            target = "";
        }
        
        if (!target.startsWith("/"))
        {
            target += "/" + target;
        }

        Activator.debug( "handle for name:" + m_path + " (path=" + target + ")" );

        if ( !m_osgiHttpContext.handleSecurity( request, response ) )
        {
            return;
        }

        // Create resource based name and see if we can resolve it
        String resName = m_path + target;
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

        long lastModified  = getLastModified(url);
        
        if (lastModified != 0)
        {
            response.setDateHeader("Last-Modified", lastModified);
        }

        if (!resourceModified(lastModified, request.getDateHeader("If-Modified-Since")))
        {
            response.setStatus(response.SC_NOT_MODIFIED);
        }
        else
        {
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
    
    
    /**
     * Gets the last modified value for file modification detection.
     * Aids in "conditional get" and intermediate proxy/node cacheing.
     *
     * Approach used follows that used by Sun for JNLP handling to workaround an 
     * apparent issue where file URLs do not correctly return a last modified time.
     * 
     */
    protected long getLastModified (URL resUrl)
    {
        long lastModified = 0;
        
        try 
        {
            // Get last modified time
            URLConnection conn = resUrl.openConnection();
            lastModified = conn.getLastModified();
        } 
        catch (Exception e) 
        {
            // do nothing
        }

        if (lastModified == 0) 
        {
            // Arguably a bug in the JRE will not set the lastModified for file URLs, and
            // always return 0. This is a workaround for that problem.
            String filepath = resUrl.getPath();
            
            if (filepath != null) 
            {
                File f = new File(filepath);
                if (f.exists()) 
                {
                    lastModified = f.lastModified();
                }
            }
        }
        
        Activator.debug( "url: " + resUrl  + ", lastModified:" + lastModified);
        
        return lastModified;
    }
    
    
    protected boolean resourceModified(long resTimestamp, long modSince)
    {
        boolean retval = false;
        
        // Have to normalise timestamps as HTTP times have last 3 digits as zero
        modSince /= 1000;
        resTimestamp /= 1000;
        
        // Timestamp check to see if modified - resTimestamp 0 check is for 
        // safety in case we didn't manage to get a timestamp for the resource
        if (resTimestamp == 0 || modSince == -1 || resTimestamp > modSince)
        {
            retval = true;
        }
        
        return retval;
    }
    
}
