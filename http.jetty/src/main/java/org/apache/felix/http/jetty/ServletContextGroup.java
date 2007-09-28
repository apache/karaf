/*
 * $Url: $
 * $Id: $
 *
 * Copyright 1997-2005 Day Management AG
 * Barfuesserplatz 6, 4001 Basel, Switzerland
 * All Rights Reserved.
 *
 * This software is the confidential and proprietary information of
 * Day Management AG, ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms of the license agreement you entered into
 * with Day.
 */
package org.apache.felix.http.jetty;


import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
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


    void removeServlet( String alias, boolean destroy )
    {
        String wAlias = aliasWildcard( alias );
        OsgiServletHolder holder = m_hdlr.removeOsgiServletHolder( wAlias );
        Servlet servlet = holder.getOsgiServlet();
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
        return m_hdlr.getServletContext().getRealPath( path );
    }


    public RequestDispatcher getRequestDispatcher( String uri )
    {
        return m_hdlr.getServletContext().getRequestDispatcher( uri );
    }


    public URL getResource( String path ) throws MalformedURLException
    {
        return m_hdlr.getServletContext().getResource( path );
    }


    public InputStream getResourceAsStream( String path )
    {
        return m_hdlr.getServletContext().getResourceAsStream( path );
    }


    public String getServerInfo()
    {
        return m_hdlr.getServletContext().getServerInfo();
    }


    public Servlet getServlet( String servletName ) throws ServletException
    {
        return m_hdlr.getServletContext().getServlet( servletName );
    }


    public Enumeration getServletNames()
    {
        return m_hdlr.getServletContext().getServletNames();
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