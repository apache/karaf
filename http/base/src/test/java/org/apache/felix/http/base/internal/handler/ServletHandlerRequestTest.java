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
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;
import javax.servlet.http.HttpServletRequest;

public class ServletHandlerRequestTest
{
    private HttpServletRequest req1;
    private HttpServletRequest req2;

    @Before
    public void setUp()
    {
        HttpServletRequest superReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(superReq.getContextPath()).thenReturn("/mycontext");
        Mockito.when(superReq.getRequestURI()).thenReturn("/mycontext/request/to/resource");
        this.req1 = new ServletHandlerRequest(superReq, "/");

        superReq = Mockito.mock(HttpServletRequest.class);
        Mockito.when(superReq.getContextPath()).thenReturn("/mycontext");
        Mockito.when(superReq.getRequestURI()).thenReturn("/mycontext/myservlet/request/to/resource;jsession=123");
        this.req2 = new ServletHandlerRequest(superReq, "/myservlet");
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
}
