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
import org.junit.Assert;
import org.mockito.Mockito;
import org.apache.felix.http.base.internal.context.ExtServletContext;
import java.util.Hashtable;

public abstract class AbstractHandlerTest
{
    protected ExtServletContext context;

    protected abstract AbstractHandler createHandler();

    public void setUp()
    {
        this.context = Mockito.mock(ExtServletContext.class);
    }
    
    @Test
    public void testId()
    {
        AbstractHandler h1 = createHandler();
        AbstractHandler h2 = createHandler();

        Assert.assertNotNull(h1.getId());
        Assert.assertNotNull(h2.getId());
        Assert.assertFalse(h1.getId().equals(h2.getId()));
    }

    @Test
    public void testInitParams()
    {
        AbstractHandler handler = createHandler();
        Assert.assertEquals(0, handler.getInitParams().size());
        
        Hashtable<String, String> map = new Hashtable<String, String>();
        map.put("key1", "value1");

        handler.setInitParams(map);
        Assert.assertEquals(1, handler.getInitParams().size());
        Assert.assertEquals("value1", handler.getInitParams().get("key1"));
    }
}
