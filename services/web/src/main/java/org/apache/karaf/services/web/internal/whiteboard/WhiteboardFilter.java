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
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.apache.karaf.services.web.internal.ContextClassLoader;

/**
 * A whiteboard filter, and the requests it applies to: the ones dispatched to a path matching one of its patterns or
 * regular expressions, or to one of its servlets. It runs with the class loader of its bundle as thread context class
 * loader.
 */
final class WhiteboardFilter {

    private final Filter filter;
    private final ClassLoader classLoader;
    private final List<Pattern> regexes;
    private final String[] patterns;
    private final String[] servletNames;
    private final Set<DispatcherType> dispatcherTypes;
    private final boolean asyncSupported;

    WhiteboardFilter(Filter filter, ClassLoader classLoader, List<Pattern> regexes, String[] patterns,
                     String[] servletNames, Set<DispatcherType> dispatcherTypes, boolean asyncSupported) {
        this.filter = filter;
        this.classLoader = classLoader;
        this.regexes = regexes;
        this.patterns = patterns;
        this.servletNames = servletNames;
        this.dispatcherTypes = dispatcherTypes;
        this.asyncSupported = asyncSupported;
    }

    void init(FilterConfig config) throws ServletException {
        try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
            filter.init(config);
        }
    }

    boolean isAsyncSupported() {
        return asyncSupported;
    }

    /**
     * @param path        the path the request is dispatched to in the context, {@code null} for a named dispatch.
     * @param servletName the name of the servlet the request is dispatched to.
     */
    boolean matches(DispatcherType type, String path, String servletName) {
        if (!dispatcherTypes.contains(type)) {
            return false;
        }
        if (path != null) {
            for (String pattern : patterns) {
                if (matches(pattern, path)) {
                    return true;
                }
            }
            for (Pattern regex : regexes) {
                if (regex.matcher(path).matches()) {
                    return true;
                }
            }
        }
        for (String name : servletNames) {
            if (name.equals(servletName)) {
                return true;
            }
        }
        return false;
    }

    void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
            filter.doFilter(request, response, chain);
        }
    }

    void destroy() {
        try (ContextClassLoader ignored = ContextClassLoader.use(classLoader)) {
            filter.destroy();
        }
    }

    /**
     * Matches a path in the context with a URL pattern, as Tomcat does to select the filters of a request.
     */
    static boolean matches(String pattern, String path) {
        if (pattern.equals(path) || pattern.equals("/*")) {
            return true;
        }
        if (pattern.endsWith("/*")) {
            String prefix = pattern.substring(0, pattern.length() - 2);
            return path.equals(prefix) || path.startsWith(prefix + "/");
        }
        if (pattern.startsWith("*.")) {
            int slash = path.lastIndexOf('/');
            int period = path.lastIndexOf('.');
            return period > slash && pattern.length() > 2 && path.substring(period + 1).equals(pattern.substring(2));
        }
        return false;
    }

    /**
     * Validates a URL pattern, as Tomcat does for the filters it maps.
     */
    static boolean isValid(String pattern) {
        if (pattern.indexOf('\n') >= 0 || pattern.indexOf('\r') >= 0) {
            return false;
        }
        if (pattern.startsWith("*.")) {
            return pattern.indexOf('/') < 0;
        }
        return pattern.isEmpty() || pattern.startsWith("/") && !pattern.contains("*.");
    }

}
