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

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.hooks.service.ListenerHook;

public class ListenerHookInfoImpl implements ListenerHook.ListenerInfo
{
    private final BundleContext m_context;
    private final ServiceListener m_listener;
    private final String m_filter;
    private boolean m_removed;

    public ListenerHookInfoImpl(BundleContext context, ServiceListener listener, String filter, boolean removed)
    {
        m_context = context;
        m_listener = listener;
        m_filter = filter;
        m_removed = removed;
    }

    public BundleContext getBundleContext()
    {
        return m_context;
    }

    public String getFilter()
    {
        return m_filter;
    }

    public boolean isRemoved()
    {
        return m_removed;
    }

    public boolean equals(Object obj) 
    {
        if (obj == this) 
        {
            return true;
        }
        
        if (!(obj instanceof ListenerHookInfoImpl)) 
        {
            return false;
        }
        
        ListenerHookInfoImpl other = (ListenerHookInfoImpl) obj;
        return other.m_listener == m_listener &&
            (m_filter == null ? other.m_filter == null : m_filter.equals(other.m_filter));
    }

    public int hashCode() 
    {
        int rc = 17;
        
        rc = 37 * rc + m_listener.hashCode();
        if (m_filter != null)
        {
            rc = 37 * rc + m_filter.hashCode();
        }
        return rc;
    }
}