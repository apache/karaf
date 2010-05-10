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

import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.adapter.ResourceAdapterService;
import org.apache.felix.dm.annotation.api.dependency.ServiceDependency;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * Our ServiceInterface provider, which service is activated by a ResourceAdapter.
 */
@ResourceAdapterService(
    filter = "(&(path=/test)(name=test1.txt)(repository=TestRepository))", 
    properties = {@Property(name="foo", value="bar")},
    propagate = true)
public class ServiceProvider implements ServiceInterface
{
    // Injected by reflection
    Resource m_resource;
        
    @ServiceDependency(filter="(test=adapter)")
    Sequencer m_sequencer;
    
    public void run()
    {
        Assert.assertNotNull("Resource has not been injected in the adapter", m_resource);
        Assert.assertEquals("ServiceProvider did not get expected resource", "test1.txt", m_resource.getName());
        Assert.assertEquals("ServiceProvider did not get expected resource", "/test", m_resource.getPath());
        Assert.assertEquals("ServiceProvider did not get expected resource", "TestRepository", m_resource.getRepository());
        m_sequencer.step(2);
    }
}
