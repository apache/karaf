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
package org.apache.felix.dm.impl;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.felix.dm.DependencyManager;
import org.apache.felix.dm.resources.Resource;
import org.apache.felix.dm.service.Service;
import org.osgi.framework.ServiceReference;

public abstract class AbstractDecorator {
    protected volatile DependencyManager m_manager;
    private final Map m_services = new HashMap();
    
    public abstract Service createService(Object[] properties);

    // callbacks for resources
    public void added(Resource resource) {
        Service newService = createService(new Object[] { resource });
        m_services.put(resource, newService);
        m_manager.add(newService);
    }

    public void removed(Resource resource) {
        Service newService = (Service) m_services.remove(resource);
        if (newService == null) {
            System.out.println("Service should not be null here, dumping stack.");
            Thread.dumpStack();
        }
        else {
            m_manager.remove(newService);
        }
    }
    
    // callbacks for services
    public void added(ServiceReference ref, Object service) {
        Service newService = createService(new Object[] { ref, service });
        m_services.put(ref, newService);
        m_manager.add(newService);
    }
    
    public void removed(ServiceReference ref, Object service) {
        Service newService = (Service) m_services.remove(ref);
        if (newService == null) {
            System.out.println("Service should not be null here, dumping stack.");
            Thread.dumpStack();
        }
        else {
            m_manager.remove(newService);
        }
    }
    
    public void stop() { 
        Iterator i = m_services.values().iterator();
        while (i.hasNext()) {
            m_manager.remove((Service) i.next());
        }
        m_services.clear();
    }
}
