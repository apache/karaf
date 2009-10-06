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

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import java.io.IOException;

public final class ServletHandler
    extends AbstractHandler implements Comparable<ServletHandler>
{
    private final String alias;
    private final Servlet servlet;

    public ServletHandler(ExtServletContext context, Servlet servlet, String alias)
    {
        super(context);
        this.alias = alias;
        this.servlet = servlet;
    }

    public String getAlias()
    {
        return this.alias;
    }

    public Servlet getServlet()
    {
        return this.servlet;
    }

    public void init()
        throws ServletException
    {
        String name = "servlet_" + getId();
        ServletConfig config = new ServletConfigImpl(name, getContext(), getInitParams());
        this.servlet.init(config);
    }

    public void destroy()
    {
        this.servlet.destroy();
    }

    public boolean matches(String uri)
    {
        if (this.alias.equals("/")) {
            return uri.startsWith(this.alias);
        } else {
            return uri.equals(this.alias) || uri.startsWith(this.alias + "/");
        }
    }

    public boolean handle(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        final boolean matches = matches(req.getPathInfo());
        if (matches) {
            doHandle(req, res);
        }

        return matches;
    }

    private void doHandle(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException
    {
        if (!getContext().handleSecurity(req, res)) {
            if (!res.isCommitted()) {
                res.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } else {
            this.servlet.service(new ServletHandlerRequest(req, this.alias), res);
        }
    }

    public int compareTo(ServletHandler other)
    {
        return other.alias.length() - this.alias.length();
    }    
}
