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

import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import org.mockito.Mockito;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class FilterHandlerTest
    extends AbstractHandlerTest
{
    private Filter filter;

    @Before
    public void setUp()
    {
        super.setUp();
        this.filter = Mockito.mock(Filter.class);
    }

    protected AbstractHandler createHandler()
    {
        return createHandler("dummy", 0);
    }

    private FilterHandler createHandler(String pattern, int ranking)
    {
        return new FilterHandler(this.context, this.filter, pattern, ranking);
    }

    @Test
    public void testCompare()
    {
        FilterHandler h1 = createHandler("a", 0);
        FilterHandler h2 = createHandler("b", 10);

        Assert.assertEquals(10, h1.compareTo(h2));
        Assert.assertEquals(-10, h2.compareTo(h1));
    }

    @Test
    public void testMatches()
    {
        FilterHandler h1 = createHandler("/a/b", 0);
        FilterHandler h2 = createHandler("/a/b/.+", 0);
        FilterHandler h3 = createHandler("/", 0);
        FilterHandler h4 = createHandler("/.*", 0);

        Assert.assertFalse(h1.matches(null));
        Assert.assertFalse(h1.matches("/a"));
        Assert.assertTrue(h1.matches("/a/b"));
        Assert.assertFalse(h1.matches("/a/b/c"));
        Assert.assertFalse(h2.matches(null));
        Assert.assertFalse(h1.matches("/a"));
        Assert.assertTrue(h2.matches("/a/b/c"));
        Assert.assertFalse(h2.matches("/a/b/"));
        Assert.assertTrue(h3.matches(null));
        Assert.assertTrue(h3.matches("/"));
        Assert.assertFalse(h3.matches("/a/b/"));
        Assert.assertTrue(h4.matches(null));
        Assert.assertTrue(h4.matches("/"));
        Assert.assertTrue(h4.matches("/a/b/"));
    }

    @Test
    public void testInit()
        throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        h1.init();
        Mockito.verify(this.filter).init(Mockito.any(FilterConfig.class));
    }

    @Test
    public void testDestroy()
    {
        FilterHandler h1 = createHandler("/a", 0);
        h1.destroy();
        Mockito.verify(this.filter).destroy();
    }

    @Test
    public void testHandleNotFound()
        throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(req.getPathInfo()).thenReturn("/");
        h1.handle(req, res, chain);

        Mockito.verify(this.filter, Mockito.never()).doFilter(req, res, chain);
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    public void testHandleFound()
        throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        Mockito.when(this.context.handleSecurity(req, res)).thenReturn(true);

        Mockito.when(req.getPathInfo()).thenReturn("/a");
        h1.handle(req, res, chain);

        Mockito.verify(this.filter).doFilter(req, res, chain);
        Mockito.verify(chain, Mockito.never()).doFilter(req, res);
    }

    @Test
    public void testHandleFoundForbidden()
        throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        Mockito.when(this.context.handleSecurity(req, res)).thenReturn(false);

        Mockito.when(req.getPathInfo()).thenReturn("/a");
        h1.handle(req, res, chain);

        Mockito.verify(this.filter, Mockito.never()).doFilter(req, res, chain);
        Mockito.verify(chain, Mockito.never()).doFilter(req, res);
        Mockito.verify(res).sendError(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    public void testHandleNotFoundContextRoot()
        throws Exception
    {
        FilterHandler h1 = createHandler("/a", 0);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);

        Mockito.when(req.getPathInfo()).thenReturn(null);
        h1.handle(req, res, chain);

        Mockito.verify(this.filter, Mockito.never()).doFilter(req, res, chain);
        Mockito.verify(chain).doFilter(req, res);
    }

    @Test
    public void testHandleFoundContextRoot()
        throws Exception
    {
        FilterHandler h1 = createHandler("/", 0);
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        FilterChain chain = Mockito.mock(FilterChain.class);
        Mockito.when(this.context.handleSecurity(req, res)).thenReturn(true);

        Mockito.when(req.getPathInfo()).thenReturn(null);
        h1.handle(req, res, chain);

        Mockito.verify(this.filter).doFilter(req, res, chain);
        Mockito.verify(chain, Mockito.never()).doFilter(req, res);
    }
}
