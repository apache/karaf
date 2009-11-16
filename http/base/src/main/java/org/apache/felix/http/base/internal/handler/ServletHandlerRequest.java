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
        final int servletPathLength = getServletPath().length();
        final String contextPath = getContextPath();
        
        String pathInfo = getRequestURI();
        pathInfo = pathInfo.substring(contextPath.length());
        pathInfo = pathInfo.replaceAll("[/]{2,}", "/");
        pathInfo = pathInfo.substring(servletPathLength);

        int scPos = pathInfo.indexOf(';');
        if (scPos > 0) {
            pathInfo = pathInfo.substring(0, scPos);
        }

        if ("".equals(pathInfo) && servletPathLength != 0) {
            pathInfo = null;
        }

        if (pathInfo != null && !pathInfo.startsWith("/")) {
            pathInfo = "/" + pathInfo;
        }

        return pathInfo;
    }
}
