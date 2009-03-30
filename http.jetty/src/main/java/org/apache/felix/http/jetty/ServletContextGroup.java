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
import java.net.URL;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.mortbay.jetty.servlet.OsgiResourceHolder;
import org.mortbay.jetty.servlet.OsgiServletHandler;
import org.mortbay.jetty.servlet.OsgiServletHolder;
import org.mortbay.jetty.servlet.ServletHolder;
import org.osgi.service.http.HttpContext;


public class ServletContextGroup implements ServletContext
{
    /** global pool of all OSGi HttpContext that have been created */
    private static Map m_contextMap = new HashMap();

    /** global set of all servlet instances that have been registered */
    private static Set m_servletSet = new HashSet();

    private OsgiServletHandler m_hdlr = null;
    private HttpContext m_osgiHttpContext = null;
    private Hashtable m_attributes = null;


    static void initializeStatics()
    {
        m_contextMap.clear();
        m_servletSet.clear();
    }


    static ServletContextGroup getServletContextGroup( OsgiServletHandler hdlr, HttpContext osgiHttpContext )
    {

        ServletContextGroup grp = ( ServletContextGroup ) m_contextMap.get( osgiHttpContext );
        if ( grp == null )
        {
            grp = new ServletContextGroup( hdlr, osgiHttpContext );
            m_contextMap.put( osgiHttpContext, grp );
        }

        return grp;
    }


    static boolean isServletRegistered( Servlet servlet )
    {
        return m_servletSet.contains( servlet );
    }


    private ServletContextGroup( OsgiServletHandler hdlr, HttpContext osgiHttpContext )
    {
        init( hdlr, osgiHttpContext );
    }


    private void init( OsgiServletHandler hdlr, HttpContext osgiHttpContext )
    {
        m_hdlr = hdlr;
        m_osgiHttpContext = osgiHttpContext;

        m_attributes = new Hashtable();

        m_contextMap.put( m_osgiHttpContext, this );
    }


    private void destroy()
    {
        m_contextMap.remove( m_osgiHttpContext );
    }


    public HttpContext getOsgiHttpContext()
    {
        return m_osgiHttpContext;
    }


    void addServlet( Servlet servlet, String alias, Dictionary params )
    {
        String wAlias = aliasWildcard( alias );
        ServletHolder holder = new OsgiServletHolder( m_hdlr, servlet, wAlias, this, params );
        m_hdlr.addOsgiServletHolder( wAlias, holder );
        Activator.debug( " adding servlet instance: " + servlet );
        m_servletSet.add( servlet );
    }


    void addResource( String alias, String path )
    {
        String wAlias = aliasWildcard( alias );
        ServletHolder holder = new OsgiResourceHolder( m_hdlr, alias, path, this );
        m_hdlr.addOsgiServletHolder( wAlias, holder );
        Activator.debug( " adding resources for " + wAlias + " at: " + path );
    }


    void remove( String alias, boolean destroy )
    {
        String wAlias = aliasWildcard( alias );
        ServletHolder holder = m_hdlr.removeOsgiServletHolder( wAlias );

        if ( holder != null )
        {
            try
            {
                Servlet servlet = holder.getServlet();
                if ( servlet != null )
                {
                    Activator.debug( " removing servlet instance: " + servlet );
                    m_servletSet.remove( servlet );

                    if ( destroy )
                    {
                        servlet.destroy();
                    }

                    if ( m_servletSet.isEmpty() )
                    {
                        destroy();
                    }
                }
            }
            catch ( ServletException se )
            {
                // may only be thrown if servlet in holder is null
            }
        }
    }


    private String aliasWildcard( String alias )
    {
        // add wilcard filter at the end of the alias to allow servlet to
        // get requests which include sub-paths
        return "/".equals( alias ) ? "/*" : alias + "/*";
    }


    // ServletContext interface for OSGi servlets

    public ServletContext getContext( String contextName )
    {
        return m_hdlr.getServletContext().getContext( contextName );
    }


    public int getMajorVersion()
    {
        return m_hdlr.getServletContext().getMajorVersion();
    }


    public int getMinorVersion()
    {
        return m_hdlr.getServletContext().getMinorVersion();
    }


    public String getContextPath()
    {
        return m_hdlr.getServletContext().getContextPath();
    }


    public String getMimeType( String file )
    {
        String type = m_osgiHttpContext.getMimeType( file );
        if ( type != null )
        {
            return type;
        }

        return m_hdlr.getServletContext().getMimeType( file );
    }


    public String getRealPath( String path )
    {
        // resources are contained in the bundle and thus are not
        // available as normal files in the platform filesystem
        return null;
    }


    public URL getResource( String path )
    {
        return m_osgiHttpContext.getResource( path );
    }


    public InputStream getResourceAsStream( String path )
    {
        URL res = getResource( path );
        if ( res != null )
        {
            try
            {
                return res.openStream();
            }
            catch ( IOException ignore )
            {
                // might want to log, but actually don't care here
            }
        }

        return null;
    }


    public Set getResourcePaths( String path )
    {
        // This is not implemented yet, might want to access the bundle entries
        return null;
    }


    public RequestDispatcher getRequestDispatcher( String uri )
    {
        return m_hdlr.getServletContext().getRequestDispatcher( uri );
    }


    public RequestDispatcher getNamedDispatcher( String name )
    {
        if ( getMinorVersion() >= 2 )
        {
            return m_hdlr.getServletContext().getNamedDispatcher( name );
        }

        return null;
    }


    public String getServerInfo()
    {
        return m_hdlr.getServletContext().getServerInfo();
    }


    public String getServletContextName()
    {
        if ( getMinorVersion() >= 3 )
        {
            return m_hdlr.getServletContext().getServletContextName();
        }

        return null;
    }


    public Servlet getServlet( String servletName ) throws ServletException
    {
        return m_hdlr.getServletContext().getServlet( servletName );
    }


    public Enumeration getServletNames()
    {
        return m_hdlr.getServletContext().getServletNames();
    }


    public String getInitParameter( String name )
    {
        if ( getMinorVersion() >= 2 )
        {
            return m_hdlr.getServletContext().getInitParameter( name );
        }

        return null;
    }


    public Enumeration getInitParameterNames()
    {
        if ( getMinorVersion() >= 2 )
        {
            return m_hdlr.getServletContext().getInitParameterNames();
        }

        return Collections.enumeration( Collections.EMPTY_LIST );
    }


    /* (non-Javadoc)
     * @see javax.servlet.ServletContext#getServlets()
     */
    public Enumeration getServlets()
    {
        return m_hdlr.getServletContext().getServlets();
    }


    public void log( Exception exception, String message )
    {
        m_hdlr.getServletContext().log( exception, message );
    }


    public void log( String message, Throwable throwable )
    {
        m_hdlr.getServletContext().log( message, throwable );
    }


    public void log( String message )
    {
        m_hdlr.getServletContext().log( message );
    }


    public void setAttribute( String name, Object value )
    {
        m_attributes.put( name, value );
    }


    public Object getAttribute( String name )
    {
        return m_attributes.get( name );
    }


    public Enumeration getAttributeNames()
    {
        return m_attributes.keys();
    }


    public void removeAttribute( String name )
    {
        m_attributes.remove( name );
    }
}