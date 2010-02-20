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

import org.apache.felix.dm.annotation.api.AdapterService;
import org.apache.felix.dm.annotation.api.Param;
import org.apache.felix.dm.annotation.api.ServiceDependency;
import org.apache.felix.dm.test.bundle.annotation.sequencer.Sequencer;

@AdapterService(adapteeService = ServiceInterface.class, adapterProperties={@Param(name="param2", value="value2")})
public class ServiceProviderAdapter implements ServiceInterface3
{
    // This is the adapted service
    protected ServiceInterface m_adaptee;
    
    @ServiceDependency
    protected Sequencer m_sequencer;

    public void run3()
    {
        m_adaptee.run();
        m_sequencer.step(3);
    }
}
