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

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * A whiteboard servlet, running with the class loader of its bundle as thread context class loader, and seeing
 * the resources of its servlet context helper.
 */
final class WhiteboardServlet implements Servlet {

    private final Servlet servlet;
    private final ServletContextHelper helper;
    private final ClassLoader classLoader;

    WhiteboardServlet(Servlet servlet, ServletContextHelper helper, ClassLoader classLoader) {
        this.servlet = servlet;
        this.helper = helper;
        this.classLoader = classLoader;
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        ServletConfig wrapped = ServletContexts.wrap(config,
                ServletContexts.wrap(config.getServletContext(), helper, classLoader));
        ServletContexts.withClassLoader(classLoader, () -> servlet.init(wrapped));
    }

    @Override
    public ServletConfig getServletConfig() {
        return servlet.getServletConfig();
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(classLoader);
        try {
            servlet.service(request, response);
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Override
    public String getServletInfo() {
        return servlet.getServletInfo();
    }

    @Override
    public void destroy() {
        ServletContexts.withClassLoader(classLoader, servlet::destroy);
    }

}
