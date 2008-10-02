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
package org.apache.felix.ipojo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

/**
 * iPOJO Internal event dispatcher.
 * This class provides an internal service event dispatcher in order to tackle the
 * event storm that can happen when starting large-scale applications.
 * @see Extender
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class EventDispatcher implements ServiceListener {
    
    private Map m_listeners;
    private BundleContext m_context;
    
    private static EventDispatcher m_dispatcher;
    public static EventDispatcher getDispatcher() {
        return m_dispatcher;
    }
    
    public EventDispatcher(BundleContext bc) {
        m_context = bc;
        m_listeners = new HashMap();
    }
    
    public void start() {
        // Only one thread can call the start method.
        m_context.addServiceListener(this);
        m_dispatcher = this; // Set the dispatcher.
    }
    
    public void stop() {
        synchronized(this) {
            m_dispatcher = null; 
            m_context.removeServiceListener(this);
            m_listeners.clear();
        }
    }

    public void serviceChanged(ServiceEvent event) {
        String[] itfs = (String[]) event.getServiceReference().getProperty(Constants.OBJECTCLASS);
        for (int s = 0; s < itfs.length; s++) {
            List list;
            synchronized (this) {
                List stored = (List) m_listeners.get(itfs[s]);
                if (stored == null) { 
                    return; // Nothing to do
                }
                // Creates a new list (stack confinement)
                list = new ArrayList(stored);
            }
            for (int i = 0; i < list.size(); i++) {
                ((ServiceListener) list.get(i)).serviceChanged(event);
            }
        }
    }
    
    public void addListener(String itf, ServiceListener listener) {
        synchronized (this) {
            List list = (List) m_listeners.get(itf);
            if (list == null) {
                list = new ArrayList(1);
                list.add(listener);
                m_listeners.put(itf, list);
            } else {
                list.add(listener);
            }
        }
    }
    
    public boolean removeListener(ServiceListener listener) {
        boolean removed = false;
        synchronized (this) {
            Set keys = m_listeners.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                String itf = (String) it.next();
                List list = (List) m_listeners.get(itf);
                removed = removed || list.remove(listener);
            }
        }
        return removed;
    }

}
