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
import java.util.*;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.mortbay.http.HttpServer;
import org.mortbay.jetty.servlet.*;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;

public class HttpServiceImpl implements HttpService
{
    /** global namesspace of all aliases that have been registered */
    private static Map      m_aliasNamespace = null;
    /** global pool of all OSGi HttpContext that have been created */
    private static Map      m_contextMap = null;
    /** global set of all servlet instances that have been registered */
    private static Set      m_servletSet = null;

    /** local list of aliases registered by the bundle holding this service */
    private Set m_localAliasSet = null;

    /** Bundle which "got" this service instance from the service factory */
    private Bundle m_bundle = null;
    /** Instance of Jetty server which provides underlying http server */
    private HttpServer m_server = null;

    public HttpServiceImpl(Bundle bundle, HttpServer server)
    {
        m_bundle = bundle;
        m_server = server;
        m_localAliasSet = new HashSet();

        if (m_aliasNamespace == null)
        {
            m_aliasNamespace = new HashMap();
        }

        if (m_contextMap == null)
        {
            m_contextMap = new HashMap();
        }

        if (m_servletSet == null)
        {
            m_servletSet = new HashSet();
        }
    }

    /**
     * Initializes static variables.
    **/
    public static void initializeStatics()
    {
        if (m_aliasNamespace != null)
        {
            m_aliasNamespace.clear();
        }
        if (m_contextMap != null)
        {
            m_contextMap.clear();
        }
        if (m_servletSet != null)
        {
            m_servletSet.clear();
        }
    }

    public org.osgi.service.http.HttpContext createDefaultHttpContext()
    {
        return new DefaultContextImpl(m_bundle);
    }

    public void registerServlet(String alias, Servlet servlet,
        Dictionary params, org.osgi.service.http.HttpContext osgiHttpContext)
        throws ServletException, NamespaceException
    {
        Activator.debug("http register servlet :" + m_bundle + ", alias: " + alias);

        if (!aliasValid(alias))
        {
            throw new IllegalArgumentException("malformed alias");
        }

        if (m_servletSet.contains(servlet))
        {
            throw new ServletException("servlet already registered");
        }

        // add alias with null details, and record servlet instance details
        addAlias(alias, null);

        //make sure alias is unique, and create
        ServletContextGroup grp = null;

        if (osgiHttpContext == null)
        {
            osgiHttpContext = createDefaultHttpContext();
        }

        // servlets using same context must get same handler to ensure
        // they share a common ServletContext
        Activator.debug("looking for context: " + osgiHttpContext);
        grp = (ServletContextGroup) m_contextMap.get(osgiHttpContext);
        if (grp == null)
        {
            grp = new ServletContextGroup(
                    servlet.getClass().getClassLoader(), osgiHttpContext);
        }

        grp.addServlet(servlet, alias, params);

        // update alias namespace with reference to group object for later
        // unregistering
        updateAlias(alias, grp);

        // maybe should remove alias/servlet entries if exceptions?
    }

    public void registerResources(String alias, String name,
        org.osgi.service.http.HttpContext osgiHttpContext)
        throws NamespaceException
    {
        Activator.debug("** http register resource :" + m_bundle + ", alias: " + alias);

        if (!aliasValid(alias))
        {
            throw new IllegalArgumentException("malformed alias");
        }

        // add alias with null details
        addAlias(alias, null);

        //make sure alias is unique, and create
        org.mortbay.http.HttpContext hdlrContext = null;

        if (osgiHttpContext == null)
        {
            osgiHttpContext = createDefaultHttpContext();
        }

        hdlrContext = m_server.addContext(alias);

        // update alias namespace with reference to context object for later
        // unregistering
        updateAlias(alias, hdlrContext);

        // create resource handler, observing any access controls
        AccessControlContext acc = null;
        if (System.getSecurityManager() != null)
        {
            acc = AccessController.getContext();
        }
        OsgiResourceHandler hdlr = new OsgiResourceHandler(osgiHttpContext,
                name, acc);

        hdlrContext.addHandler(hdlr);
        try
        {
            hdlrContext.start();
        }
        catch (Exception e)
        {
            System.err.println("Oscar exception adding resource: " + e);
            e.printStackTrace(System.err);
            // maybe we should remove alias here?
        }
    }

    public void unregister(String alias)
    {
        doUnregister(alias, true);
    }

    protected void unregisterAll()
    {
        // note that this is a forced unregister, so we shouldn't call destroy
        // on any servlets
        // unregister each alias for the bundle - copy list since it will
        // change
        String[] all = (String[]) m_localAliasSet.toArray(new String[0]);
        for (int ix = 0; ix < all.length; ix++)
        {
            doUnregister(all[ix], false);
        }
    }

    protected void doUnregister(String alias, boolean forced)
    {
        Object obj = removeAlias(alias);

        if (obj instanceof org.mortbay.http.HttpContext)
        {
            Activator.debug("** http unregister resource :" + m_bundle + ", alias: " + alias);

            org.mortbay.http.HttpContext ctxt = (org.mortbay.http.HttpContext) obj;
            try
            {
                ctxt.stop();
                m_server.removeContext(ctxt);
            }
            catch(Exception e)
            {
                System.err.println("Oscar exception removing resource: " + e);
                e.printStackTrace();
            }
        }
        else if (obj instanceof ServletContextGroup)
        {
            Activator.debug("** http unregister servlet :" + m_bundle + ", alias: " + alias + ",forced:" + forced);

            ServletContextGroup grp = (ServletContextGroup) obj;
            grp.removeServlet(alias, forced);
        }
        else
        {
            // oops - this shouldn't happen !
        }
    }

    protected void addAlias(String alias, Object obj)
            throws NamespaceException
    {
        synchronized (m_aliasNamespace)
        {
            if (m_aliasNamespace.containsKey(alias))
            {
                throw new NamespaceException("alias already registered");
            }

            m_aliasNamespace.put(alias, obj);
            m_localAliasSet.add(alias);
        }
    }

    protected Object removeAlias(String alias)
    {
        synchronized (m_aliasNamespace)
        {
            // remove alias, don't worry if doesn't exist
            Object obj = m_aliasNamespace.remove(alias);
            m_localAliasSet.remove(alias);
            return obj;
        }
    }

    protected void updateAlias(String alias, Object obj)
    {
        synchronized (m_aliasNamespace)
        {
            // only update if already present
            if (m_aliasNamespace.containsKey(alias))
            {
                m_aliasNamespace.put(alias, obj);
            }
        }
    }

    protected boolean aliasValid(String alias)
    {
       if (!alias.equals("/") &&
            (!alias.startsWith("/") || alias.endsWith("/")))
        {
            return false;
        }

        return true;
    }

    private class ServletContextGroup
    {
        private OsgiServletHttpContext m_hdlrContext = null;
        private OsgiServletHandler m_hdlr = null;
        private org.osgi.service.http.HttpContext m_osgiHttpContext = null;
        private int m_servletCount = 0;

        private ServletContextGroup(ClassLoader loader,
                org.osgi.service.http.HttpContext osgiHttpContext)
        {
            init(loader, osgiHttpContext);
        }

        private void init(ClassLoader loader,
                org.osgi.service.http.HttpContext osgiHttpContext)
        {
            m_osgiHttpContext = osgiHttpContext;
            m_hdlrContext = new OsgiServletHttpContext(m_osgiHttpContext);
            m_hdlrContext.setContextPath("/");
            //TODO: was in original code, but seems we shouldn't serve
            //      resources in servlet context
            //m_hdlrContext.setServingResources(true);
            m_hdlrContext.setClassLoader(loader);
            Activator.debug(" adding handler context : " + m_hdlrContext);
            m_server.addContext(m_hdlrContext);

            m_hdlr = new OsgiServletHandler(m_osgiHttpContext);
            m_hdlrContext.addHandler(m_hdlr);

            try
            {
                m_hdlrContext.start();
            }
            catch (Exception e)
            {
                // make sure we unwind the adding process
                System.err.println("Oscar exception adding servlet: " + e);
                e.printStackTrace(System.err);
            }

            m_contextMap.put(m_osgiHttpContext, this);
        }

        private void destroy()
        {
            Activator.debug(" removing handler context : " + m_hdlrContext);
            m_server.removeContext(m_hdlrContext);
            m_contextMap.remove(m_osgiHttpContext);
        }

        private void addServlet(Servlet servlet, String alias,
                Dictionary params)
        {
            String wAlias = aliasWildcard(alias);
            ServletHolder holder = new OsgiServletHolder(m_hdlr, servlet, wAlias, params);
            m_hdlr.addOsgiServletHolder(wAlias, holder);
            Activator.debug(" adding servlet instance: " + servlet);
            m_servletSet.add(servlet);
            m_servletCount++;
        }

        private void removeServlet(String alias, boolean destroy)
        {
            String wAlias = aliasWildcard(alias);
            OsgiServletHolder holder = m_hdlr.removeOsgiServletHolder(wAlias);
            Servlet servlet = holder.getOsgiServlet();
            Activator.debug(" removing servlet instance: " + servlet);
            m_servletSet.remove(servlet);

            if (destroy)
            {
                servlet.destroy();
            }

            if (--m_servletCount == 0)
            {
                destroy();
            }
        }
        
        private String aliasWildcard(String alias)
        {
            // add wilcard filter at the end of the alias to allow servlet to
            // get requests which include sub-paths
            return "/".equals(alias) ? "/*" : alias + "/*";
        } 
    }
}