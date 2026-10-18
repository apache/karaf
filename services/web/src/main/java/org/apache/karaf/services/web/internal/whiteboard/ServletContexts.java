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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.Enumeration;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;

import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * The servlet context given to the whiteboard servlets and filters: the resources and the MIME types come from
 * the servlet context helper, the class loader is the one of the bundle registering the service, everything else
 * comes from the Tomcat context.
 */
final class ServletContexts {

    private ServletContexts() {
    }

    static ServletContext wrap(ServletContext context, ServletContextHelper helper, ClassLoader classLoader) {
        return (ServletContext) Proxy.newProxyInstance(ServletContext.class.getClassLoader(),
                new Class<?>[]{ServletContext.class}, (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getResource":
                            return helper.getResource((String) args[0]);
                        case "getResourceAsStream":
                            URL url = helper.getResource((String) args[0]);
                            return url == null ? null : url.openStream();
                        case "getResourcePaths":
                            return helper.getResourcePaths((String) args[0]);
                        case "getRealPath":
                            return helper.getRealPath((String) args[0]);
                        case "getMimeType":
                            String mimeType = helper.getMimeType((String) args[0]);
                            return mimeType != null ? mimeType : context.getMimeType((String) args[0]);
                        case "getClassLoader":
                            return classLoader;
                        case "equals":
                            return proxy == args[0];
                        case "hashCode":
                            return System.identityHashCode(proxy);
                        default:
                            try {
                                return method.invoke(context, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                    }
                });
    }

    static ServletConfig wrap(ServletConfig config, ServletContext context) {
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return config.getServletName();
            }

            @Override
            public ServletContext getServletContext() {
                return context;
            }

            @Override
            public String getInitParameter(String name) {
                return config.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return config.getInitParameterNames();
            }
        };
    }

    static FilterConfig wrap(FilterConfig config, ServletContext context) {
        return new FilterConfig() {
            @Override
            public String getFilterName() {
                return config.getFilterName();
            }

            @Override
            public ServletContext getServletContext() {
                return context;
            }

            @Override
            public String getInitParameter(String name) {
                return config.getInitParameter(name);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return config.getInitParameterNames();
            }
        };
    }

    /**
     * Runs an action with the given thread context class loader.
     */
    static <E extends Exception> void withClassLoader(ClassLoader classLoader, Action<E> action) throws E {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            action.run();
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    interface Action<E extends Exception> {
        void run() throws E;
    }

}
