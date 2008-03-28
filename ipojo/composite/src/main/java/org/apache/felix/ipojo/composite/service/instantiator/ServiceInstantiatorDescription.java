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
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.DependencyModel;
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
     * List of exports.
     */
    private List m_imports;

    /**
     * Constructor.
     * 
     * @param handler : composite handler
     * @param insts : list of service instances
     * @param imps : list of service importers
     */
    public ServiceInstantiatorDescription(CompositeHandler handler, List insts, List imps) {
        super(handler);
        m_instances = insts;
        m_imports = imps;
    }

    /**
     * Build service instantiator handler description.
     * @return the handler description
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element services = super.getHandlerInfo();
        for (int i = 0; i < m_imports.size(); i++) {
            ServiceImporter imp = (ServiceImporter) m_imports.get(i);
            Element impo = new Element("Requires", "");
            impo.addAttribute(new Attribute("Specification", imp.getSpecification().getName()));
            if (imp.getFilter() != null) {
                impo.addAttribute(new Attribute("Filter", imp.getFilter()));
            }
            if (imp.getState() == DependencyModel.RESOLVED) {
                impo.addAttribute(new Attribute("State", "resolved"));
                for (int j = 0; j < imp.getProviders().size(); j++) {
                    Element prov = new Element("Provider", "");
                    prov.addAttribute(new Attribute("name", (String) imp.getProviders().get(j)));
                    impo.addElement(prov);
                }
            } else {
                impo.addAttribute(new Attribute("State", "unresolved"));
            }
            services.addElement(impo);
        }
        
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            Element service = new Element("Service", "");
            service.addAttribute(new Attribute("Specification", inst.getServiceSpecification()));
            String state = "unresolved";
            if (inst.getState() == DependencyModel.RESOLVED) {
                state = "resolved";
            }
            service.addAttribute(new Attribute("State", state));
            Map map = inst.getMatchingFactories();
            Set keys = map.keySet();
            Iterator iterator = keys.iterator();
            while (iterator.hasNext()) {
                ServiceReference ref = (ServiceReference) iterator.next();
                Object object = map.get(ref);
                if (object != null) {
                    Element fact = new Element("Factory", "");
                    fact.addAttribute(new Attribute("Name", ((ComponentInstance) object).getFactory().getName()));
                    service.addElement(fact);
                }
            }
            services.addElement(service);
        }
        return services;
    }

}
