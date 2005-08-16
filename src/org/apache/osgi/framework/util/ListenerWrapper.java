/*
 *   Copyright 2005 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.osgi.framework.util;

import java.util.EventListener;

import org.osgi.framework.Bundle;

public class ListenerWrapper
{
    // The bundle associated with the listener.
    private Bundle m_bundle = null;
    // Listener class.
    private Class m_class = null;
    // The original listener.
    private EventListener m_listener = null;

    public ListenerWrapper(Bundle bundle, Class clazz, EventListener l)
    {
        m_bundle = bundle;
        m_class = clazz;
        m_listener = l;
    }

    public Bundle getBundle()
    {
        return m_bundle;
    }

    protected Class getListenerClass()
    {
        return m_class;
    }

    protected EventListener getListener()
    {
        return m_listener;
    }

    public boolean equals(Object obj)
    {
        if (obj instanceof ListenerWrapper)
        {
            return (((ListenerWrapper) obj).m_listener == m_listener);
        }
        else if (obj instanceof EventListener)
        {
            return (obj == m_listener);
        }
        return false;
    }
}