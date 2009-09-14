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
import org.osgi.framework.Bundle;
import org.osgi.service.http.HttpContext;
import org.mockito.Mockito;

import javax.servlet.ServletContext;

public class ServletContextManagerTest
{
    private ServletContextManager manager;

    @Before
    public void setUp()
    {
        Bundle bundle = Mockito.mock(Bundle.class);
        ServletContext globalContext = Mockito.mock(ServletContext.class);
        this.manager = new ServletContextManager(bundle, globalContext);
    }

    @Test
    public void testGetServletContext()
    {
        HttpContext httpCtx = Mockito.mock(HttpContext.class);
        ServletContext result1 = this.manager.getServletContext(httpCtx);
        ServletContext result2 = this.manager.getServletContext(httpCtx);

        Assert.assertNotNull(result1);
        Assert.assertNotNull(result2);
        Assert.assertSame(result1, result2);

        httpCtx = Mockito.mock(HttpContext.class);
        result2 = this.manager.getServletContext(httpCtx);

        Assert.assertNotNull(result2);
        Assert.assertNotSame(result1, result2);
    }
    
}
