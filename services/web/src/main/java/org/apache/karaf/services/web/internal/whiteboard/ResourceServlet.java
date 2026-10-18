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
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

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

    private final String prefix;
    private final transient ServletContextHelper helper;

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
        URL url = path == null || isDirectory(path) ? null : helper.getResource(path);
        if (url == null) {
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
                is.transferTo(response.getOutputStream());
            }
        }
    }

    /**
     * The directories are not served: the entry of a bundle directory is also returned for its path without the
     * trailing slash, so they are detected by listing their content.
     */
    private boolean isDirectory(String path) {
        if (path.endsWith("/")) {
            return true;
        }
        Set<String> children = helper.getResourcePaths(path + "/");
        return children != null && !children.isEmpty();
    }

    /**
     * @return the name of the resource, or {@code null} if the request path goes out of the prefix.
     */
    String resourcePath(HttpServletRequest request) {
        HttpServletMapping mapping = request.getHttpServletMapping();
        String relative;
        switch (mapping.getMappingMatch()) {
            case EXACT:
            case CONTEXT_ROOT:
                relative = "";
                break;
            case PATH:
                relative = request.getPathInfo() == null ? "" : request.getPathInfo();
                break;
            default:
                relative = request.getServletPath()
                        + (request.getPathInfo() == null ? "" : request.getPathInfo());
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
