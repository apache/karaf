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
package org.apache.felix.dm.test.bundle.annotation.factoryconfadapter;

import java.util.Dictionary;

import org.apache.felix.dm.annotation.api.FactoryConfigurationAdapterService;
import org.apache.felix.dm.annotation.api.Property;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.Stop;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

/**
 * This service is instantiated when a factory configuration is created from ConfigAdmin
 */
@FactoryConfigurationAdapterService(factoryPid = "FactoryPidTest", properties = { @Property(name = "foo", value = "bar") }, propagate = true)
public class ServiceProvider implements ServiceInterface
{
    @ServiceDependency
    private Sequencer m_sequencer;
    
    private volatile boolean m_started;

    // Either initial config, or an updated config
    protected void updated(Dictionary conf)
    {
        if (m_started)
        {
            m_sequencer.step(3);
        }
    }

    @Start
    void start()
    {
        m_started = true;
        m_sequencer.step(1);
    }    

    // The ServiceClient is invoking our service
    public void doService()
    {
       m_sequencer.step();
    }

    @Stop
    void stop() 
    {
        m_sequencer.step(5);
    }
}
