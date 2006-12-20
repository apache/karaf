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
package org.apache.felix.framework.util.manifestparser;

import java.util.Collections;
import java.util.Map;
import org.apache.felix.moduleloader.ICapability;

public class Capability implements ICapability
{
    private String m_namespace = null;
    private Map m_propMap = null;

    public Capability(String namespace, Map propMap)
    {
        m_namespace = namespace;
        m_propMap = Collections.unmodifiableMap(propMap);
    }

    public String getNamespace()
    {
        return m_namespace;
    }

    public Map getProperties()
    {
        return m_propMap;
    }

    public String[] getUses()
    {
        return null;
    }
}