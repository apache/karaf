/*
* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements.  See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership.  The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License.  You may obtain a copy of the License at
*
*   http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.apache.felix.dm.test.bundle.annotation.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import junit.framework.Assert;

import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.resources.ResourceHandler;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

@Service
public class ResourceProvider
{
    private volatile BundleContext m_context;
    private final Map m_handlers = new HashMap();
    private StaticResource[] m_resources = {
            new StaticResource("test1.txt", "/test", "TestRepository")
            {
                public InputStream openStream() throws IOException
                {
                    return null;
                };
            }, new StaticResource("test2.txt", "/test", "TestRepository")
            {
                public InputStream openStream() throws IOException
                {
                    return null;
                };
            }, new StaticResource("README.doc", "/", "TestRepository")
            {
                public InputStream openStream() throws IOException
                {
                    Assert.fail("resource should not have matched the filter");
                    return null;
                };
            } };

    @ServiceDependency(removed = "remove", required=false)
    public void add(ServiceReference ref, ResourceHandler handler)
    {
        String filterString = (String) ref.getProperty("filter");
        Filter filter;
        try
        {
            filter = m_context.createFilter(filterString);
        }
        catch (InvalidSyntaxException e)
        {
            Assert.fail("Could not create filter for resource handler: " + e);
            return;
        }
        synchronized (m_handlers)
        {
            m_handlers.put(handler, filter);
        }
        for (int i = 0; i < m_resources.length; i++)
        {
            if (filter.match(m_resources[i].getProperties()))
            {
                handler.added(m_resources[i]);
            }
        }
    }

    public void remove(ServiceReference ref, ResourceHandler handler)
    {
        Filter filter;
        synchronized (m_handlers)
        {
            filter = (Filter) m_handlers.remove(handler);
        }
        removeResources(handler, filter);
    }

    private void removeResources(ResourceHandler handler, Filter filter)
    {
        for (int i = 0; i < m_resources.length; i++)
        {
            if (filter.match(m_resources[i].getProperties()))
            {
                handler.removed(m_resources[i]);
            }
        }
    }

    @Destroy
    public void destroy()
    {
        Entry[] handlers;
        synchronized (m_handlers)
        {
            handlers = (Entry[]) m_handlers.entrySet().toArray(new Entry[m_handlers.size()]);
        }
        for (int i = 0; i < handlers.length; i++)
        {
            removeResources((ResourceHandler) handlers[i].getKey(), (Filter) handlers[i].getValue());
        }
    }
}
