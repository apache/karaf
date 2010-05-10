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

import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.annotation.api.dependency.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@Service(factory = "MyServiceFactory", factoryConfigure = "configure", properties = { @Property(name = "foo", value = "bar") })
public class MyService implements MyServiceInterface
{
    @ServiceDependency
    Sequencer m_sequencer;

    @SuppressWarnings("unchecked")
    Dictionary m_configuration;

    public void configure(Dictionary<?, ?> configuration)
    {
        if (m_configuration == null)
        {
            m_configuration = configuration;
        }
        else
        {
            m_sequencer.step(5);
        }
    }

    @Start
    public void start()
    {
        m_sequencer.step(2);
    }

    @Stop
    public void stop()
    {
        m_sequencer.step(10);
    }

    public void added(String instanceId)
    {
        if (instanceId.equals(m_configuration.get("instance.id")))
        {
            m_sequencer.step(4);
        }
    }

    public void changed(String modified)
    {
        if (modified.equals(m_configuration.get("instance.modified")))
        {
            m_sequencer.step(7);
        }
    }

    public void removed()
    {
        m_sequencer.step(9);
    }
}
