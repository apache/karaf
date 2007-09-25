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
package org.apache.felix.ipojo.composite.service.instantiator;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.ServiceReference;

/**
 * Description of the Service Creator Handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceInstantiatorDescription extends HandlerDescription {

    /**
     * List of managed service instances.
     */
    private List m_instances;

    /**
     * Constructor.
     * 
     * @param h : composite handler
     * @param insts : list of service instance
     */
    public ServiceInstantiatorDescription(CompositeHandler h, List insts) {
        super(h);
        m_instances = insts;
    }

    /**
     * Build service instantiator handler description.
     * @return the handler description
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element services = super.getHandlerInfo();
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            Element service = new Element("Service", "");
            service.addAttribute(new Attribute("Specification", inst.getSpecification()));
            String state = "unresolved";
            if (inst.isSatisfied()) {
                state = "resolved";
            }
            service.addAttribute(new Attribute("State", state));
            Map map = inst.getUsedReferences();
            Set keys = map.keySet();
            Iterator it = keys.iterator();
            while (it.hasNext()) {
                ServiceReference ref = (ServiceReference) it.next();
                Object o = map.get(ref);
                if (o != null) {
                    Element fact = new Element("Factory", "");
                    fact.addAttribute(new Attribute("Name", ((ComponentInstance) o).getFactory().getName()));
                    service.addElement(fact);
                }
            }
            services.addElement(service);
        }
        return services;
    }

}
