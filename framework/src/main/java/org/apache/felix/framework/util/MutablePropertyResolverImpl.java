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
package org.apache.felix.framework.util;

import java.util.Map;

public class MutablePropertyResolverImpl implements MutablePropertyResolver
{
    private Map m_props = null;
    
    public MutablePropertyResolverImpl(Map props)
    {
        m_props = props;
    }

    public synchronized String put(String key, String value)
    {
        return (String) m_props.put(key, value);
    }

    public synchronized String get(String key)
    {
        return (String) m_props.get(key);
    }

    public synchronized String[] getKeys()
    {
        return (String[]) m_props.keySet().toArray(new String[0]);
    }
}