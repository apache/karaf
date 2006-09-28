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
package org.apache.felix.servicebinder.architecture;

import org.apache.felix.servicebinder.InstanceMetadata;

/**
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class InstanceChangeEvent
{
    /**
     * 
     * @uml.property name="m_ref"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private Instance m_ref;

    /**
     * 
     * @uml.property name="m_meta"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private InstanceMetadata m_meta;

    private int m_state;

    public InstanceChangeEvent(Instance ref,InstanceMetadata meta,int state)
    {
        m_ref = ref;
        m_meta = meta;
        m_state = state;
    }

    public Instance getInstance()
    {
        return m_ref;
    }

    public InstanceMetadata getInstanceMetadata()
    {
        return m_meta;
    }

    public int getState()
    {
        return m_state;
    }
}
