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
package org.apache.felix.dm.test.bundle.annotation.factory;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import junit.framework.Assert;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.dependency.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@SuppressWarnings("unchecked")
@Service
public class MyServiceFactory
{
    @ServiceDependency(filter = "(dm.factory.name=MyServiceFactory)")
    Set<Dictionary> m_myServiceFactory;

    @ServiceDependency
    Sequencer m_sequencer;

    Hashtable m_conf;

    @Start
    void start()
    {
        m_sequencer.step(1);
        // create a service instance with this configuration
        m_conf = new Hashtable();
        m_conf.put("instance.id", "instance");
        m_conf.put(".private.param", "private");
        Assert.assertTrue(m_myServiceFactory.add(m_conf));
    }

    @ServiceDependency(required = false, changed = "update", removed = "removed")
    void bind(Map serviceProperties, MyServiceInterface service)
    {
        m_sequencer.step(3);
        Assert.assertEquals("bar", serviceProperties.get("foo"));
        Assert.assertNull(serviceProperties.get(".private.param"));
        service.added((String) serviceProperties.get("instance.id"));

        // update the service instance
        m_conf.put("instance.modified", "true");
        Assert.assertFalse(m_myServiceFactory.add(m_conf));
    }

    void update(Map serviceProperties, MyServiceInterface service)
    {
        m_sequencer.step(6);
        service.changed((String) serviceProperties.get("instance.modified"));

        // remove instance
        Assert.assertTrue(m_myServiceFactory.remove(m_conf));
    }

    void removed(MyServiceInterface service)
    {
        m_sequencer.step(8);
        service.removed();
    }
}
