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

import org.mockito.Mockito;
import org.osgi.service.http.HttpContext;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import javax.servlet.http.HttpServletRequest;

public class ServletHandlerRequestTest
{
    private HttpServletRequest superReq1;
    private HttpServletRequest superReq2;
    
    private HttpServletRequest req1;
    private HttpServletRequest req2;

    @Before
    public void setUp()
    {
        this.superReq1 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(this.superReq1.getContextPath()).thenReturn("/mycontext");
        Mockito.when(this.superReq1.getRequestURI()).thenReturn("/mycontext/request/to/resource");
        Mockito.when(this.superReq1.getAttribute(HttpContext.AUTHENTICATION_TYPE)).thenReturn(HttpServletRequest.BASIC_AUTH);
        Mockito.when(this.superReq1.getAttribute(HttpContext.REMOTE_USER)).thenReturn("felix");
        this.req1 = new ServletHandlerRequest(this.superReq1, "/");

        this.superReq2 = Mockito.mock(HttpServletRequest.class);
        Mockito.when(this.superReq2.getContextPath()).thenReturn("/mycontext");
        Mockito.when(this.superReq2.getRequestURI()).thenReturn("/mycontext/myservlet/request/to/resource;jsession=123");
        Mockito.when(this.superReq2.getAttribute(HttpContext.AUTHENTICATION_TYPE)).thenReturn(null);
        Mockito.when(this.superReq2.getAuthType()).thenReturn(HttpServletRequest.DIGEST_AUTH);
        Mockito.when(this.superReq2.getAttribute(HttpContext.REMOTE_USER)).thenReturn(null);
        Mockito.when(this.superReq2.getRemoteUser()).thenReturn("sling");
        this.req2 = new ServletHandlerRequest(this.superReq2, "/myservlet");
    }

    @Test
    public void testPathInfo()
    {
        Assert.assertEquals("/request/to/resource", this.req1.getPathInfo());
        Assert.assertEquals("/request/to/resource", this.req2.getPathInfo());
    }
    
    @Test
    public void testServletPath()
    {
        Assert.assertEquals("", this.req1.getServletPath());
        Assert.assertEquals("/myservlet", this.req2.getServletPath());
    }
    
    @Test
    public void testGetAuthType()
    {
        Assert.assertEquals(HttpServletRequest.BASIC_AUTH, this.req1.getAuthType());
        Mockito.verify(this.superReq1).getAttribute(HttpContext.AUTHENTICATION_TYPE);
        Mockito.verifyNoMoreInteractions(this.superReq1);
        
        Assert.assertEquals(HttpServletRequest.DIGEST_AUTH, this.req2.getAuthType());
        Mockito.verify(this.superReq2).getAttribute(HttpContext.AUTHENTICATION_TYPE);
        Mockito.verify(this.superReq2).getAuthType();
        Mockito.verifyNoMoreInteractions(this.superReq2);   
    }
    
    @Test
    public void testGetRemoteUser()
    {
        Assert.assertEquals("felix", this.req1.getRemoteUser());
        Mockito.verify(this.superReq1).getAttribute(HttpContext.REMOTE_USER);
        Mockito.verifyNoMoreInteractions(this.superReq1);
        
        Assert.assertEquals("sling", this.req2.getRemoteUser());
        Mockito.verify(this.superReq2).getAttribute(HttpContext.REMOTE_USER);
        Mockito.verify(this.superReq2).getRemoteUser();
        Mockito.verifyNoMoreInteractions(this.superReq2);   
    }
}
