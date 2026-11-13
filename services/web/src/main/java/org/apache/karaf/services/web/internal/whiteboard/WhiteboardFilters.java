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
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.catalina.Globals;

/**
 * The whiteboard filters of a context, in their ranking order. They are applied by the whiteboard servlets rather than
 * by Tomcat, so that they can be added to and removed from the running context.
 */
final class WhiteboardFilters {

    private volatile List<WhiteboardFilter> filters = List.of();
    private volatile boolean suspended;

    void set(List<WhiteboardFilter> filters) {
        this.filters = List.copyOf(filters);
    }

    /**
     * Rejects the requests until resumed: while a filter is replaced, when its properties change, the requests are not
     * served without it.
     */
    void suspend() {
        suspended = true;
    }

    void resume() {
        suspended = false;
    }

    /**
     * Applies the filters matching a request dispatched to a servlet, then the servlet.
     *
     * @param servletName the name the filters select the servlet with.
     */
    void doFilter(HttpServletRequest request, HttpServletResponse response, String servletName, FilterChain servlet)
            throws IOException, ServletException {
        if (suspended) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            return;
        }
        // the path Tomcat matches its filters with: the path the request is dispatched to, null for a named dispatch
        Object attribute = request.getAttribute(Globals.DISPATCHER_REQUEST_PATH_ATTR);
        String path = attribute == null ? null : attribute.toString();
        List<WhiteboardFilter> matching = new ArrayList<>();
        for (WhiteboardFilter filter : filters) {
            if (filter.matches(request.getDispatcherType(), path, servletName)) {
                matching.add(filter);
            }
        }
        if (matching.isEmpty()) {
            servlet.doFilter(request, response);
        } else {
            new Chain(matching, servlet).doFilter(request, response);
        }
    }

    private static final class Chain implements FilterChain {

        private final List<WhiteboardFilter> filters;
        private final FilterChain servlet;
        private int position;

        Chain(List<WhiteboardFilter> filters, FilterChain servlet) {
            this.filters = filters;
            this.servlet = servlet;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response) throws IOException, ServletException {
            if (position == filters.size()) {
                servlet.doFilter(request, response);
                return;
            }
            WhiteboardFilter filter = filters.get(position++);
            if (!filter.isAsyncSupported() && request.isAsyncSupported()) {
                // as Tomcat does for its own filters
                request.setAttribute(Globals.ASYNC_SUPPORTED_ATTR, Boolean.FALSE);
            }
            filter.doFilter(request, response, this);
        }

    }

}
