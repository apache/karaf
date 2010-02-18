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
package org.apache.felix.dm.test.bundle.annotation.multiple;

import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@Service(properties = { @Param(name = "foo", value = "bar") }, factory = Factory.class, factoryMethod = "createServiceProvider")
public class ServiceProvider implements ServiceInterface
{
    @ServiceDependency
    Sequencer m_sequencer;

    ServiceProvider2 m_serviceProvider2;

    @ServiceDependency(removed = "unbind")
    void bind(ServiceProvider2 provider2)
    {
        m_serviceProvider2 = provider2;
    }

    void unbind(ServiceProvider2 provider2)
    {
        m_sequencer.step(10);
    }

    @Start
    void start()
    {
        if (m_serviceProvider2 != null)
        {
            m_sequencer.step(4);
        }
        m_sequencer.step(5);
    }

    @Stop
    void stop()
    {
        m_sequencer.step(9);
    }

    public void doService()
    {
        m_sequencer.step(7);
    }
}
