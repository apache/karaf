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
import java.util.List;
import java.util.regex.Pattern;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * A whiteboard filter, running with the class loader of its bundle as thread context class loader, and seeing
 * the resources of its servlet context helper.
 * <p>
 * A filter with regular expressions is mapped to all the requests, and only applies to the requests whose path
 * in the context matches one of them.
 */
final class WhiteboardFilter implements Filter {

    private final Filter filter;
    private final ServletContextHelper helper;
    private final ClassLoader classLoader;
    private final List<Pattern> regexes;

    WhiteboardFilter(Filter filter, ServletContextHelper helper, ClassLoader classLoader, List<Pattern> regexes) {
        this.filter = filter;
        this.helper = helper;
        this.classLoader = classLoader;
        this.regexes = regexes;
    }

    @Override
    public void init(FilterConfig config) throws ServletException {
        FilterConfig wrapped = ServletContexts.wrap(config,
                ServletContexts.wrap(config.getServletContext(), helper, classLoader));
        ServletContexts.withClassLoader(classLoader, () -> filter.init(wrapped));
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (!regexes.isEmpty() && !matches(request)) {
            chain.doFilter(request, response);
            return;
        }
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            filter.doFilter(request, response, chain);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    private boolean matches(ServletRequest request) {
        if (!(request instanceof HttpServletRequest)) {
            return false;
        }
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getServletPath()
                + (httpRequest.getPathInfo() == null ? "" : httpRequest.getPathInfo());
        for (Pattern regex : regexes) {
            if (regex.matcher(path).matches()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        ServletContexts.withClassLoader(classLoader, filter::destroy);
    }

}
