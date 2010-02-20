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

import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.Service;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@Service(properties = { @Param(name = "param1", value = "value1") })
public class ServiceProvider implements ServiceInterface
{
    @ServiceDependency
    protected Sequencer m_sequencer;

    @ServiceDependency
    protected ServiceInterface2 m_serviceInterface2;

    public void run()
    {
        m_sequencer.step(1);
        m_serviceInterface2.run2();
    }
}
