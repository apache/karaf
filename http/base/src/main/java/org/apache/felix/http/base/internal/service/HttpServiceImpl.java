/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.felix.http.base.internal.service;

import org.apache.felix.http.api.ExtHttpService;
import org.apache.felix.http.base.internal.context.ServletContextManager;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import org.apache.felix.http.base.internal.handler.HandlerRegistry;
import org.apache.felix.http.base.internal.handler.FilterHandler;
import org.apache.felix.http.base.internal.handler.ServletHandler;
import org.apache.felix.http.base.internal.logger.SystemLogger;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.NamespaceException;
import org.osgi.framework.Bundle;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import java.util.Dictionary;
import java.util.HashSet;

public final class HttpServiceImpl
    implements ExtHttpService
{
    private final Bundle bundle;
    private final HandlerRegistry handlerRegistry;
    private final HashSet<Servlet> localServlets;
    private final HashSet<Filter> localFilters;
    private final ServletContextManager contextManager;

    public HttpServiceImpl(Bundle bundle, ServletContext context, HandlerRegistry handlerRegistry)
    {
        this.bundle = bundle;
        this.handlerRegistry = handlerRegistry;
        this.localServlets = new HashSet<Servlet>();
        this.localFilters = new HashSet<Filter>();
        this.contextManager = new ServletContextManager(this.bundle, context);
    }

    private ExtServletContext getServletContext(HttpContext context)
    {
        if (context == null) {
            context = createDefaultHttpContext();
        }

        return this.contextManager.getServletContext(context);
    }

    public void registerFilter(Filter filter, String pattern, Dictionary initParams, int ranking, HttpContext context)
        throws ServletException
    {
        FilterHandler handler = new FilterHandler(getServletContext(context), filter, pattern, ranking);
        handler.setInitParams(initParams);
        this.handlerRegistry.addFilter(handler);
        this.localFilters.add(filter);
    }

    public void unregisterFilter(Filter filter)
    {
        if (filter != null) {
            this.handlerRegistry.removeFilter(filter);
            this.localFilters.remove(filter);
        }
    }

    public void unregisterServlet(Servlet servlet)
    {
        if (servlet != null) {
            this.handlerRegistry.removeServlet(servlet);
            this.localServlets.remove(servlet);
        }
    }

    public void registerServlet(String alias, Servlet servlet, Dictionary initParams, HttpContext context)
        throws ServletException, NamespaceException
    {
        if (!isAliasValid(alias)) {
            throw new IllegalArgumentException( "Malformed servlet alias [" + alias + "]");
        }
        
        ServletHandler handler = new ServletHandler(getServletContext(context), servlet, alias);
        handler.setInitParams(initParams);
        this.handlerRegistry.addServlet(handler);
        this.localServlets.add(servlet);
    }

    public void registerResources(String alias, String name, HttpContext context)
        throws NamespaceException
    {
        if (!isNameValid(name)) {
            throw new IllegalArgumentException( "Malformed resource name [" + name + "]");
        }
        
        try {
            Servlet servlet = new ResourceServlet(name);
            registerServlet(alias, servlet, null, context);
        } catch (ServletException e) {
            SystemLogger.error("Failed to register resources", e);
        }
    }

    public void unregister(String alias)
    {
        unregisterServlet(this.handlerRegistry.getServletByAlias(alias));
    }

    public HttpContext createDefaultHttpContext()
    {
        return new DefaultHttpContext(this.bundle);
    }

    public void unregisterAll()
    {
        HashSet<Servlet> servlets = new HashSet<Servlet>(this.localServlets);
        for (Servlet servlet : servlets) {
            unregisterServlet(servlet);
        }

        HashSet<Filter> filters = new HashSet<Filter>(this.localFilters);
        for (Filter fiter : filters) {
            unregisterFilter(fiter);
        }
    }

    private boolean isNameValid(String name)
    {
        if (name == null) {
            return false;
        }

        if (name.endsWith( "/" )) {
            return false;
        }

        return true;
    }

    private boolean isAliasValid(String alias)
    {
        if (alias == null) {
            return false;
        }

        if (!alias.equals("/") && ( !alias.startsWith("/") || alias.endsWith("/"))) {
            return false;
        }

        return true;
    }
}
