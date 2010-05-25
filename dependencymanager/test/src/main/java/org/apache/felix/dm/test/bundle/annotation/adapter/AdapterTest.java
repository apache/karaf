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
package org.apache.felix.dm.test.bundle.annotation.adapter;

import java.util.Map;

import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.Start;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@Service
public class AdapterTest
{
    @ServiceDependency
    Sequencer m_sequencer;
    private Map<String, String> m_serviceProperties;
    
    @ServiceDependency
    void bind(Map<String, String> serviceProperties, ServiceInterface3 service)
    {
        m_serviceProperties = serviceProperties;
        service.run3();
    }
    
    @Start
    void start() {
        // The adapter service must inherit from adaptee service properties ...
        if ("value1".equals(m_serviceProperties.get("param1")) // adaptee properties
            && "value2".equals(m_serviceProperties.get("param2"))) // adapter properties
        {
            m_sequencer.step(4);
        }        
    }
}
