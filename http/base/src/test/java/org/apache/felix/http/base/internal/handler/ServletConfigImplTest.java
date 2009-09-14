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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import javax.servlet.ServletContext;
import java.util.HashMap;
import java.util.Enumeration;

public class ServletConfigImplTest
{
    private ServletContext context;
    private ServletConfigImpl config;

    @Before
    public void setUp()
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("key1", "value1");

        this.context = Mockito.mock(ServletContext.class);
        this.config = new ServletConfigImpl("myservlet", this.context, params);
    }

    @Test
    public void testGetServletName()
    {
        Assert.assertSame("myservlet", this.config.getServletName());
    }

    @Test
    public void testGetServletContext()
    {
        Assert.assertSame(this.context, this.config.getServletContext());
    }

    @Test
    public void testGetInitParameter()
    {
        Assert.assertNull(this.config.getInitParameter("key2"));
        Assert.assertEquals("value1", this.config.getInitParameter("key1"));
    }

    @Test
    public void testGetInitParameterNames()
    {
        Enumeration e = this.config.getInitParameterNames();
        Assert.assertNotNull(e);
        Assert.assertTrue(e.hasMoreElements());
        Assert.assertEquals("key1", e.nextElement());
        Assert.assertFalse(e.hasMoreElements());
    }
}