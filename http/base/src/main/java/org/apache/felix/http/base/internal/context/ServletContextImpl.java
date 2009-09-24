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
package org.apache.felix.http.base.internal.context;

import org.osgi.service.http.HttpContext;
import org.osgi.framework.Bundle;
import org.apache.felix.http.base.internal.util.MimeTypes;
import org.apache.felix.http.base.internal.logger.SystemLogger;

import javax.servlet.ServletContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.URL;
import java.io.InputStream;
import java.io.IOException;

public final class ServletContextImpl
    implements ExtServletContext
{
    private final Bundle bundle;
    private final ServletContext context;
    private final HttpContext httpContext;
    private final Map<String, Object> attributes;

    public ServletContextImpl(Bundle bundle, ServletContext context, HttpContext httpContext)
    {
        this.bundle = bundle;
        this.context = context;
        this.httpContext = httpContext;
        this.attributes = new ConcurrentHashMap<String, Object>();
    }

    public String getContextPath()
    {
        return this.context.getContextPath();
    }

    public ServletContext getContext(String uri)
    {
        return this.context.getContext(uri);
    }

    public int getMajorVersion()
    {
        return this.context.getMajorVersion();
    }

    public int getMinorVersion()
    {
        return this.context.getMinorVersion();
    }

    public Set getResourcePaths(String path)
    {
        Enumeration paths = this.bundle.getEntryPaths(normalizePath(path));
        if ((paths == null) || !paths.hasMoreElements()) {
            return null;
        }

        Set<String> set = new HashSet<String>();
        while (paths.hasMoreElements()) {
            set.add((String)paths.nextElement());
        }

        return set;
    }

    public URL getResource(String path)
    {
        return this.httpContext.getResource(normalizePath(path));
    }

    public InputStream getResourceAsStream(String path)
    {
        URL res = getResource(path);
        if (res != null) {
            try {
                return res.openStream();
            } catch (IOException e) {
                // Do nothing
            }
        }

        return null;
    }

    private String normalizePath(String path)
    {
        if (path == null) {
            return null;
        }

        String normalizedPath = path.trim().replaceAll("/+", "/");
        if (normalizedPath.startsWith("/") && (normalizedPath.length() > 1)) {
            normalizedPath = normalizedPath.substring(1);
        }

        return normalizedPath;
    }

    public RequestDispatcher getRequestDispatcher(String uri)
    {
        return null;
    }

    public RequestDispatcher getNamedDispatcher(String name)
    {
        return null;
    }

    public String getInitParameter(String name)
    {
        return null;
    }

    public Enumeration getInitParameterNames()
    {
        return Collections.enumeration(Collections.emptyList());
    }

    public Object getAttribute(String name)
    {
        return this.attributes.get(name);
    }

    public Enumeration getAttributeNames()
    {
        return Collections.enumeration(this.attributes.keySet());
    }

    public void setAttribute(String name, Object value)
    {
        this.attributes.put(name, value);
    }

    public void removeAttribute(String name)
    {
        this.attributes.remove(name);
    }

    @SuppressWarnings("deprecation")
    public Servlet getServlet(String name)
        throws ServletException
    {
        return null;
    }

    @SuppressWarnings("deprecation")
    public Enumeration getServlets()
    {
        return Collections.enumeration(Collections.emptyList());
    }

    @SuppressWarnings("deprecation")
    public Enumeration getServletNames()
    {
        return Collections.enumeration(Collections.emptyList());
    }

    public void log(String message)
    {
        SystemLogger.info(message);
    }

    public void log(Exception cause, String message)
    {
        SystemLogger.error(message, cause);
    }

    public void log(String message, Throwable cause)
    {
        SystemLogger.error(message, cause);
    }

    public String getServletContextName()
    {
        return this.context.getServletContextName();
    }

    public String getRealPath(String name)
    {
        return null;
    }

    public String getServerInfo()
    {
        return this.context.getServerInfo();
    }

    public String getMimeType(String file)
    {
        String type = this.httpContext.getMimeType(file);
        if (type != null) {
            return type;
        }

        return MimeTypes.get().getByFile(file);
    }

    public boolean handleSecurity(HttpServletRequest req, HttpServletResponse res)
        throws IOException
    {
        return this.httpContext.handleSecurity(req, res);
    }
}
