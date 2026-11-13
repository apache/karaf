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
package org.apache.karaf.services.web.internal.whiteboard;

import java.io.IOException;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.karaf.services.web.internal.ContextClassLoader;
import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * A whiteboard servlet or resource deployed in Tomcat. A request is checked by the servlet context helper of the
 * bundle of the servlet, then goes through the whiteboard filters, then reaches the servlet. The servlet sees the
 * servlet context of its bundle, and runs with the class loader of its bundle as thread context class loader.
 */
final class WhiteboardServlet implements Servlet {

    private final Servlet servlet;
    private final String name;
    private final ServletContextHelper helper;
    private final ServletContext servletContext;
    private final ClassLoader classLoader;
    private final WhiteboardFilters filters;

    /**
     * @param name the name the filters select the servlet with.
     */
    WhiteboardServlet(Servlet servlet, String name, ServletContextHelper helper, ServletContext servletContext,
                      ClassLoader classLoader, WhiteboardFilters filters) {
        this.servlet = servlet;
        this.name = name;
        this.helper = helper;
        this.servletContext = servletContext;
        this.classLoader = classLoader;
        this.filters = filters;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
            servlet.init(ServletContexts.wrap(config, servletContext));
        }
    }

    @Override
    public ServletConfig getServletConfig() {
        return servlet.getServletConfig();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        HttpServletRequest httpRequest = new WhiteboardRequest((HttpServletRequest) request, servletContext);
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        // the security is checked when the request comes in, not when it is dispatched in the context
        if (request.getDispatcherType() != DispatcherType.REQUEST) {
            filters.doFilter(httpRequest, httpResponse, name, this::invoke);
            return;
        }
        if (!helper.handleSecurity(httpRequest, httpResponse)) {
            // unless the helper has set the status, a 401 with a challenge or a redirect for instance
            if (!httpResponse.isCommitted() && httpResponse.getStatus() == HttpServletResponse.SC_OK) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }
        try {
            filters.doFilter(httpRequest, httpResponse, name, this::invoke);
        } finally {
            helper.finishSecurity(httpRequest, httpResponse);
        }
    }

    private void invoke(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
            servlet.service(request, response);
        }
    }

    @Override
    public String getServletInfo() {
        return servlet.getServletInfo();
    }

    @Override
    public void destroy() {
        try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
            servlet.destroy();
        }
    }

    /**
     * The request seen by the filters and the servlet: its servlet context is the one of the bundle of the servlet,
     * and its remote user and authentication type are the ones set by the servlet context helper as attributes.
     */
    private static final class WhiteboardRequest extends HttpServletRequestWrapper {

        private final ServletContext servletContext;

        WhiteboardRequest(HttpServletRequest request, ServletContext servletContext) {
            super(request);
            this.servletContext = servletContext;
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getRemoteUser() {
            Object user = getAttribute(ServletContextHelper.REMOTE_USER);
            return user != null ? user.toString() : super.getRemoteUser();
        }

        @Override
        public String getAuthType() {
            Object type = getAttribute(ServletContextHelper.AUTHENTICATION_TYPE);
            return type != null ? type.toString() : super.getAuthType();
        }

    }

}
