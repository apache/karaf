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


import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.OsgiServletHandler;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;


public class HttpServiceImpl implements HttpService
{
    /** global namesspace of all aliases that have been registered */
    private static Map m_aliasNamespace = null;

    /** local list of aliases registered by the bundle holding this service */
    private Set m_localAliasSet = null;

    /** Bundle which "got" this service instance from the service factory */
    private Bundle m_bundle = null;
    private HttpServer m_server = null;
    private OsgiServletHandler m_serverServletHandler = null;


    public HttpServiceImpl( Bundle bundle, HttpServer server, OsgiServletHandler serverServletHandler )
    {
        m_bundle = bundle;
        m_server = server;
        m_serverServletHandler = serverServletHandler;
        m_localAliasSet = new HashSet();

        if ( m_aliasNamespace == null )
        {
            m_aliasNamespace = new HashMap();
        }
    }


    /**
     * Initializes static variables.
    **/
    public static void initializeStatics()
    {
        if ( m_aliasNamespace != null )
        {
            m_aliasNamespace.clear();
        }
        ServletContextGroup.initializeStatics();
    }


    public HttpContext createDefaultHttpContext()
    {
        return new DefaultContextImpl( m_bundle );
    }


    public void registerServlet( String alias, Servlet servlet, Dictionary params, HttpContext osgiHttpContext )
        throws ServletException, NamespaceException
    {
        Activator.debug( "http register servlet :" + m_bundle + ", alias: " + alias );

        if ( !aliasValid( alias ) )
        {
            throw new IllegalArgumentException( "malformed alias" );
        }

        if ( ServletContextGroup.isServletRegistered( servlet ) )
        {
            throw new ServletException( "servlet already registered" );
        }

        // add alias with null details, and record servlet instance details
        addAlias( alias, null );

        //make sure alias is unique, and create
        ServletContextGroup grp = null;

        if ( osgiHttpContext == null )
        {
            osgiHttpContext = createDefaultHttpContext();
        }

        // servlets using same context must get same handler to ensure
        // they share a common ServletContext
        Activator.debug( "looking for context: " + osgiHttpContext );
        grp = ServletContextGroup.getServletContextGroup( m_serverServletHandler, osgiHttpContext );

        grp.addServlet( servlet, alias, params );

        // update alias namespace with reference to group object for later
        // unregistering
        updateAlias( alias, grp );

        // maybe should remove alias/servlet entries if exceptions?
    }


    public void registerResources( String alias, String name, HttpContext osgiHttpContext ) throws NamespaceException
    {
        Activator.debug( "** http register resource :" + m_bundle + ", alias: " + alias );

        if ( !aliasValid( alias ) )
        {
            throw new IllegalArgumentException( "malformed alias" );
        }

        // add alias with null details
        addAlias( alias, null );

        //make sure alias is unique, and create
        org.mortbay.http.HttpContext hdlrContext = null;

        if ( osgiHttpContext == null )
        {
            osgiHttpContext = createDefaultHttpContext();
        }

        hdlrContext = m_server.addContext( alias );

        // update alias namespace with reference to context object for later
        // unregistering
        updateAlias( alias, hdlrContext );

        // create resource handler, observing any access controls
        AccessControlContext acc = null;
        if ( System.getSecurityManager() != null )
        {
            acc = AccessController.getContext();
        }
        OsgiResourceHandler hdlr = new OsgiResourceHandler( osgiHttpContext, name, acc );

        hdlrContext.addHandler( hdlr );
        try
        {
            hdlrContext.start();
        }
        catch ( Exception e )
        {
            System.err.println( "Oscar exception adding resource: " + e );
            e.printStackTrace( System.err );
            // maybe we should remove alias here?
        }
    }


    public void unregister( String alias )
    {
        doUnregister( alias, true );
    }


    protected void unregisterAll()
    {
        // note that this is a forced unregister, so we shouldn't call destroy
        // on any servlets
        // unregister each alias for the bundle - copy list since it will
        // change
        String[] all = ( String[] ) m_localAliasSet.toArray( new String[0] );
        for ( int ix = 0; ix < all.length; ix++ )
        {
            doUnregister( all[ix], false );
        }
    }


    protected void doUnregister( String alias, boolean forced )
    {
        Object obj = removeAlias( alias );

        if ( obj instanceof org.mortbay.http.HttpContext )
        {
            Activator.debug( "** http unregister resource :" + m_bundle + ", alias: " + alias );

            org.mortbay.http.HttpContext ctxt = ( org.mortbay.http.HttpContext ) obj;
            try
            {
                ctxt.stop();
                m_server.removeContext( ctxt );
            }
            catch ( Exception e )
            {
                System.err.println( "Oscar exception removing resource: " + e );
                e.printStackTrace();
            }
        }
        else if ( obj instanceof ServletContextGroup )
        {
            Activator.debug( "** http unregister servlet :" + m_bundle + ", alias: " + alias + ",forced:" + forced );

            ServletContextGroup grp = ( ServletContextGroup ) obj;
            grp.removeServlet( alias, forced );
        }
        else
        {
            // oops - this shouldn't happen !
        }
    }


    protected void addAlias( String alias, Object obj ) throws NamespaceException
    {
        synchronized ( m_aliasNamespace )
        {
            if ( m_aliasNamespace.containsKey( alias ) )
            {
                throw new NamespaceException( "alias already registered" );
            }

            m_aliasNamespace.put( alias, obj );
            m_localAliasSet.add( alias );
        }
    }


    protected Object removeAlias( String alias )
    {
        synchronized ( m_aliasNamespace )
        {
            // remove alias, don't worry if doesn't exist
            Object obj = m_aliasNamespace.remove( alias );
            m_localAliasSet.remove( alias );
            return obj;
        }
    }


    protected void updateAlias( String alias, Object obj )
    {
        synchronized ( m_aliasNamespace )
        {
            // only update if already present
            if ( m_aliasNamespace.containsKey( alias ) )
            {
                m_aliasNamespace.put( alias, obj );
            }
        }
    }


    protected boolean aliasValid( String alias )
    {
        if ( !alias.equals( "/" ) && ( !alias.startsWith( "/" ) || alias.endsWith( "/" ) ) )
        {
            return false;
        }

        return true;
    }
}