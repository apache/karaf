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

import junit.framework.Assert;

import org.apache.felix.dm.annotation.api.Destroy;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ResourceDependency;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * A Component which has a resource dependency.
 */
@Service
public class ResourceConsumer
{
    @ServiceDependency(filter = "(test=resource)")
    Sequencer m_sequencer;
    
    private int m_resourcesSeen;

    @ResourceDependency(required = false, filter = "(&(path=/test)(name=*.txt)(repository=TestRepository))")
    public void add(Resource resource)
    {
        if (match(resource, "test1.txt", "/test", "TestRepository"))
        {
            m_resourcesSeen++;
            return;
        }

        if (match(resource, "test2.txt", "/test", "TestRepository"))
        {
            m_resourcesSeen++;
            return;
        }

        Assert.fail("Got unexpected resource: " + resource.getName() + "/" + resource.getPath()
            + "/" + resource.getRepository());
    }

    private boolean match(Resource resource, String name, String path, String repo)
    {
        return name.equals(resource.getName()) && path.equals(resource.getPath())
            && repo.equals(resource.getRepository());
    }

    @Destroy
    private void destroy()
    {
        Assert.assertEquals(2, m_resourcesSeen);
        m_sequencer.step(1);
    }
}
