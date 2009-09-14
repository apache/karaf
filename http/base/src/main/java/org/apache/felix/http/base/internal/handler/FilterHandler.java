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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import java.io.IOException;

public final class FilterHandler
    extends AbstractHandler implements Comparable<FilterHandler>
{
    private final Filter filter;
    private final String pattern;
    private final int ranking;

    public FilterHandler(ExtServletContext context, Filter filter, String pattern, int ranking)
    {
        super(context);
        this.filter = filter;
        this.pattern = pattern;
        this.ranking = ranking;
    }

    public Filter getFilter()
    {
        return this.filter;
    }
    
    public void init()
        throws ServletException
    {
        String name = "filter_" + getId();
        FilterConfig config = new FilterConfigImpl(name, getContext(), getInitParams());
        this.filter.init(config);
    }

    public void destroy()
    {
        this.filter.destroy();
    }

    public boolean matches(String uri)
    {
        return uri.matches(this.pattern);
    }

    public void handle(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException
    {
        final boolean matches = matches(req.getPathInfo());
        if (matches) {
            doHandle(req, res, chain);
        } else {
            chain.doFilter(req, res);
        }
    }

    private void doHandle(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
        throws ServletException, IOException
    {
        if (!getContext().handleSecurity(req, res)) {
            res.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else {
            this.filter.doFilter(req, res, chain);
        }
    }

    public int compareTo(FilterHandler other)
    {
        return other.ranking - this.ranking;
    }
}
