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

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletHandlerTest
    extends AbstractHandlerTest
{
    private Servlet servlet;

    @Before
    public void setUp()
    {
        super.setUp();
        this.servlet = Mockito.mock(Servlet.class);
    }

    protected AbstractHandler createHandler()
    {
        return createHandler("/dummy");
    }
    
    private ServletHandler createHandler(String alias)
    {
        return new ServletHandler(this.context, this.servlet, alias);
    }
    
    @Test
    public void testCompare()
    {
        ServletHandler h1 = createHandler("/a");
        ServletHandler h2 = createHandler("/a/b");

        Assert.assertEquals(2, h1.compareTo(h2));
        Assert.assertEquals(-2, h2.compareTo(h1));
    }

    @Test
    public void testMatches()
    {
        ServletHandler h1 = createHandler("/a/b");

        Assert.assertFalse(h1.matches("/a/"));
        Assert.assertTrue(h1.matches("/a/b"));
        Assert.assertTrue(h1.matches("/a/b/"));
        Assert.assertTrue(h1.matches("/a/b/c"));
    }

    @Test
    public void testInit()
        throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        h1.init();
        Mockito.verify(this.servlet).init(Mockito.any(ServletConfig.class));
    }

    @Test
    public void testDestroy()
    {
        ServletHandler h1 = createHandler("/a");
        h1.destroy();
        Mockito.verify(this.servlet).destroy();
    }

    @Test
    public void testHandleNotFound()
        throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);

        Mockito.when(req.getPathInfo()).thenReturn("/");
        boolean result = h1.handle(req, res);

        Assert.assertFalse(result);
        Mockito.verify(this.servlet, Mockito.never()).service(req, res);
    }

    @Test
    public void testHandleFound()
        throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        Mockito.when(this.context.handleSecurity(req, res)).thenReturn(true);

        Mockito.when(req.getPathInfo()).thenReturn("/a/b");
        boolean result = h1.handle(req, res);

        Assert.assertTrue(result);
        Mockito.verify(this.servlet).service(Mockito.any(HttpServletRequest.class),
                Mockito.any(HttpServletResponse.class));
    }

    @Test
    public void testHandleFoundForbidden()
        throws Exception
    {
        ServletHandler h1 = createHandler("/a");
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);
        Mockito.when(this.context.handleSecurity(req, res)).thenReturn(false);

        Mockito.when(req.getPathInfo()).thenReturn("/a/b");
        boolean result = h1.handle(req, res);

        Assert.assertTrue(result);
        Mockito.verify(this.servlet, Mockito.never()).service(req, res);
        Mockito.verify(res).sendError(HttpServletResponse.SC_FORBIDDEN);
    }
}
