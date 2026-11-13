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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.tomcat.util.http.RequestUtil;
import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * Serves the whiteboard resources: the request path, relative to the resource pattern, is appended to the
 * resource prefix, and the resource is read from the servlet context helper.
 */
final class ResourceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    // the number of resources whose type is kept, a helper possibly returning a resource for any name
    private static final int MAX_DIRECTORIES = 10000;

    private final String prefix;
    private final transient ServletContextHelper helper;
    // whether the resources found are directories
    private final transient Map<String, Boolean> directories = new ConcurrentHashMap<>();

    /**
     * @param prefix the prefix of the resources, the empty string for the root of the resources.
     */
    ResourceServlet(String prefix, ServletContextHelper helper) {
        this.prefix = prefix;
        this.helper = helper;
    }

    String getPrefix() {
        return prefix;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        serve(request, response, true);
    }

    @Override
    protected void doHead(HttpServletRequest request, HttpServletResponse response) throws IOException {
        serve(request, response, false);
    }

    private void serve(HttpServletRequest request, HttpServletResponse response, boolean body) throws IOException {
        String path = resourcePath(request);
        URL url = path == null || path.endsWith("/") ? null : helper.getResource(path);
        if (url == null || isDirectory(path, url)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        URLConnection connection = url.openConnection();
        long lastModified = connection.getLastModified();
        if (lastModified > 0) {
            long modifiedSince = -1;
            try {
                modifiedSince = request.getDateHeader("If-Modified-Since");
            } catch (IllegalArgumentException e) {
                // invalid header, ignored
            }
            if (modifiedSince >= 0 && lastModified / 1000 <= modifiedSince / 1000) {
                response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                connection.getInputStream().close();
                return;
            }
            response.setDateHeader("Last-Modified", lastModified);
        }
        String mimeType = helper.getMimeType(path);
        if (mimeType == null) {
            mimeType = getServletContext().getMimeType(path);
        }
        if (mimeType != null) {
            response.setContentType(mimeType);
        }
        long length = connection.getContentLengthLong();
        if (length >= 0) {
            response.setContentLengthLong(length);
        }
        try (InputStream is = connection.getInputStream()) {
            if (body) {
                write(is, response, mimeType);
            }
        }
    }

    /**
     * Copies a resource to the response, or a text resource to its writer when the writer is used, by an including
     * servlet for instance, as the Tomcat default servlet does.
     */
    private static void write(InputStream is, HttpServletResponse response, String mimeType) throws IOException {
        OutputStream os;
        try {
            os = response.getOutputStream();
        } catch (IllegalStateException e) {
            if (!isText(mimeType)) {
                throw e;
            }
            new InputStreamReader(is, StandardCharsets.UTF_8).transferTo(response.getWriter());
            return;
        }
        is.transferTo(os);
    }

    private static boolean isText(String mimeType) {
        return mimeType == null || mimeType.startsWith("text") || mimeType.endsWith("xml")
                || mimeType.contains("/javascript") || mimeType.endsWith("json");
    }

    /**
     * The directories are not served: the entry of a bundle directory is also returned for its path without the
     * trailing slash, so they are detected by listing their content. Listing the entries of a bundle goes through all
     * of them, so this is done once per resource.
     */
    private boolean isDirectory(String path, URL url) {
        if (url.getPath().endsWith("/")) {
            return true;
        }
        Boolean directory = directories.get(path);
        if (directory == null) {
            Set<String> children = helper.getResourcePaths(path + "/");
            directory = children != null && !children.isEmpty();
            if (directories.size() < MAX_DIRECTORIES) {
                directories.put(path, directory);
            }
        }
        return directory;
    }

    /**
     * @return the name of the resource, or {@code null} if the request path goes out of the prefix.
     */
    String resourcePath(HttpServletRequest request) {
        // when included, the paths of the request are the ones of the including servlet
        boolean include = request.getDispatcherType() == DispatcherType.INCLUDE
                && request.getAttribute(RequestDispatcher.INCLUDE_MAPPING) instanceof HttpServletMapping;
        HttpServletMapping mapping = include
                ? (HttpServletMapping) request.getAttribute(RequestDispatcher.INCLUDE_MAPPING)
                : request.getHttpServletMapping();
        String servletPath = include
                ? (String) request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH) : request.getServletPath();
        String pathInfo = include
                ? (String) request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO) : request.getPathInfo();
        String relative;
        switch (mapping.getMappingMatch()) {
            case EXACT:
            case CONTEXT_ROOT:
                relative = "";
                break;
            case PATH:
                relative = pathInfo == null ? "" : pathInfo;
                break;
            default:
                relative = (servletPath == null ? "" : servletPath) + (pathInfo == null ? "" : pathInfo);
        }
        if (relative.isEmpty()) {
            return prefix;
        }
        String normalized = RequestUtil.normalize(relative);
        if (normalized == null) {
            return null;
        }
        return (prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix) + normalized;
    }

}
