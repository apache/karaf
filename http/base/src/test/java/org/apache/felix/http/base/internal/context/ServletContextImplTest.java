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

import org.junit.Before;
import org.junit.Test;
import org.junit.Assert;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletContext;
import java.net.URL;
import java.util.*;

public class ServletContextImplTest
{
    private Bundle bundle;
    private HttpContext httpContext;
    private ServletContextImpl context;

    @Before
    public void setUp()
    {
        this.bundle = Mockito.mock(Bundle.class);
        ServletContext globalContext = Mockito.mock(ServletContext.class);
        this.httpContext = Mockito.mock(HttpContext.class);
        this.context = new ServletContextImpl(this.bundle, globalContext, this.httpContext);
    }

    @Test
    public void testGetResource()
        throws Exception
    {
        URL url = getClass().getResource("resource.txt");
        Assert.assertNotNull(url);
        
        Mockito.when(this.httpContext.getResource("resource.txt")).thenReturn(url);
        Assert.assertNull(this.context.getResource("/notfound.txt"));
        Assert.assertEquals(url, this.context.getResource("/resource.txt"));
    }

    @Test
    public void testGetResourceAsStream()
        throws Exception
    {
        URL url = getClass().getResource("resource.txt");
        Assert.assertNotNull(url);

        Mockito.when(this.httpContext.getResource("resource.txt")).thenReturn(url);
        Assert.assertNull(this.context.getResourceAsStream("/notfound.txt"));
        Assert.assertNotNull(this.context.getResourceAsStream("/resource.txt"));
    }

    @Test
    public void testGetResourcePaths()
    {
        HashSet<String> paths = new HashSet<String>(Arrays.asList("/some/path/1", "/some/path/2"));
        Mockito.when(this.bundle.getEntryPaths("some/path")).thenReturn(Collections.enumeration(paths));

        Set set = this.context.getResourcePaths("/some/path");
        Assert.assertNotNull(set);
        Assert.assertEquals(2, set.size());
        Assert.assertTrue(set.contains("/some/path/1"));
        Assert.assertTrue(set.contains("/some/path/2"));
    }

    @Test
    public void testGetRealPath()
    {
        Assert.assertNull(this.context.getRealPath("path"));
    }

    @Test
    public void testGetInitParameter()
    {
        Assert.assertNull(this.context.getInitParameter("key1"));
    }

    @Test
    public void testGetInitParameterNames()
    {
        Enumeration e = this.context.getInitParameterNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetAttribute()
    {
        Assert.assertNull(this.context.getAttribute("key1"));

        this.context.setAttribute("key1", "value1");
        Assert.assertEquals("value1", this.context.getAttribute("key1"));

        this.context.removeAttribute("key1");
        Assert.assertNull(this.context.getAttribute("key1"));

        this.context.setAttribute("key1", null);
        Assert.assertNull(this.context.getAttribute("key1"));
    }

    @Test
    public void testGetAttributeNames()
    {
        Enumeration e = this.context.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());

        this.context.setAttribute("key1", "value1");
        e = this.context.getAttributeNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetServlet()
        throws Exception
    {
        Assert.assertNull(this.context.getServlet("test"));
    }

    @Test
    public void testGetServletNames()
    {
        Enumeration e = this.context.getServletNames();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetServlets()
    {
        Enumeration e = this.context.getServlets();
        Assert.assertNotNull(e);
        Assert.assertFalse(e.hasMoreElements());
    }

    @Test
    public void testGetMimeType()
    {
        Mockito.when(this.httpContext.getMimeType("file.xml")).thenReturn("some-other-format");
        Assert.assertEquals("some-other-format", this.context.getMimeType("file.xml"));
        Assert.assertEquals("text/plain", this.context.getMimeType("file.txt"));
    }

    @Test
    public void testHandleSecurity()
        throws Exception
    {
        HttpServletRequest req = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse res = Mockito.mock(HttpServletResponse.class);

        Mockito.when(this.httpContext.handleSecurity(req, res)).thenReturn(true);
        Assert.assertTrue(this.context.handleSecurity(req, res));

        Mockito.when(this.httpContext.handleSecurity(req, res)).thenReturn(false);
        Assert.assertFalse(this.context.handleSecurity(req, res));
    }
}
