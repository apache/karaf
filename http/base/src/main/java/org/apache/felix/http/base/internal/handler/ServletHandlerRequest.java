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
package org.apache.felix.http.base.internal.handler;

import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;

import org.osgi.service.http.HttpContext;

final class ServletHandlerRequest
    extends HttpServletRequestWrapper
{
    private final String alias;
    private String contextPath;
    private String pathInfo;
    private boolean pathInfoCalculated = false;

    public ServletHandlerRequest(HttpServletRequest req, String alias)
    {
        super(req);
        this.alias = alias;
    }

    @Override
    public String getAuthType()
    {
        String authType = (String) getAttribute(HttpContext.AUTHENTICATION_TYPE);
        if (authType != null) {
            return authType;
        }

        return super.getAuthType();
    }

    @Override
    public String getContextPath()
    {
        /*
         * FELIX-2030 Calculate the context path for the Http Service
         * registered servlets from the container context and servlet paths
         */
        if (contextPath == null) {
            final String context = super.getContextPath();
            final String servlet = super.getServletPath();
            if (context.length() == 0) {
                contextPath = servlet;
            } else if (servlet.length() == 0) {
                contextPath = context;
            } else {
                contextPath = context + servlet;
            }
        }

        return contextPath;
    }

    @Override
    public String getPathInfo()
    {
        if (!this.pathInfoCalculated) {
            this.pathInfo = calculatePathInfo();
            this.pathInfoCalculated = true;
        }

        return this.pathInfo;
    }

    @Override
    public String getPathTranslated()
    {
        final String info = getPathInfo();
        return (null == info) ? null : getRealPath(info);
    }

    @Override
    public String getRemoteUser()
    {
        String remoteUser = (String) getAttribute(HttpContext.REMOTE_USER);
        if (remoteUser != null) {
            return remoteUser;
        }

        return super.getRemoteUser();
    }

    @Override
    public String getServletPath()
    {
        if ("/".equals(this.alias)) {
            return "";
        }
        return this.alias;
    }

    private String calculatePathInfo()
    {
        /*
         * The pathInfo from the servlet container is
         *       servletAlias + pathInfo
         * where pathInfo is either an empty string (in which case the
         * client directly requested the servlet) or starts with a slash
         * (in which case the client requested a child of the servlet).
         *
         * Note, the servlet container pathInfo may also be null if the
         * servlet is registered as the root servlet
         */

        String pathInfo = super.getPathInfo();
        if (pathInfo != null) {

            // cut off alias of this servlet (if not the root servlet)
            if (!"/".equals(alias)) {
                pathInfo = pathInfo.substring(alias.length());
            }

            // ensure empty string is coerced to null
            if (pathInfo.length() == 0) {
                pathInfo = null;
            }

        }

        return pathInfo;
    }
}
