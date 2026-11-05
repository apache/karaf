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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

import org.osgi.service.servlet.context.ServletContextHelper;

/**
 * The first filter of a whiteboard context with a servlet context helper: the request is only processed if the
 * helper accepts it.
 */
final class SecurityFilter implements Filter {

    static final String NAME = "karaf-whiteboard-security";

    private final ServletContextHelper helper;

    SecurityFilter(ServletContextHelper helper) {
        this.helper = helper;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        if (!helper.handleSecurity(httpRequest, httpResponse)) {
            if (!httpResponse.isCommitted()) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
            return;
        }
        try {
            chain.doFilter(new AuthenticatedRequest(httpRequest), response);
        } finally {
            helper.finishSecurity(httpRequest, httpResponse);
        }
    }

    /**
     * The remote user and the authentication type set by the helper as request attributes.
     */
    private static final class AuthenticatedRequest extends HttpServletRequestWrapper {

        AuthenticatedRequest(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getRemoteUser() {
            Object user = getAttribute(ServletContextHelper.REMOTE_USER);
            return user != null ? user.toString() : super.getRemoteUser();
        }

        @Override
        public String getAuthType() {
            Object type = getAttribute(ServletContextHelper.AUTHENTICATION_TYPE);
            return type != null ? type.toString() : super.getAuthType();
        }

    }

}
