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
package org.apache.felix.ipojo.composite.service.provides;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.DependencyModel;

/**
 * Provided Service Handler Description for composite.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandlerDescription extends HandlerDescription {

    /**
     * Provided Service Description list.
     */
    private List m_services = new ArrayList();
    
    /**
     * List of exports.
     */
    private List m_exports;

    /**
     * Constructor.
     * 
     * @param handler : composite handler.
     * @param services : The list of Provided Service.
     * @param exporters : list of managed exports
     */
    public ProvidedServiceHandlerDescription(CompositeHandler handler, List services, List exporters) {
        super(handler);
        m_services = services;
        m_exports = exporters;
    }

    /**
     * Get the handler description.
     * @return the provided service handler description
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element services = super.getHandlerInfo();
        for (int i = 0; i < m_services.size(); i++) {
            ProvidedService svc = (ProvidedService) m_services.get(i);
            Element service = new Element("service", "");
            String state = "unregistered";
            if (svc.isRegistered()) {
                state = "registered";
            }
            String spec = "[" + svc.getSpecification() + "]";
            service.addAttribute(new Attribute("Specification", spec));
            service.addAttribute(new Attribute("State", state));
            services.addElement(service);
        }
        
        for (int i = 0; i < m_exports.size(); i++) {
            ServiceExporter exp = (ServiceExporter) m_exports.get(i);
            Element expo = new Element("Exports", "");
            expo.addAttribute(new Attribute("Specification", exp.getSpecification().getName()));
            expo.addAttribute(new Attribute("Filter", exp.getFilter()));
            if (exp.getState() == DependencyModel.RESOLVED) {
                expo.addAttribute(new Attribute("State", "resolved"));
            } else {
                expo.addAttribute(new Attribute("State", "unresolved"));
            }
            services.addElement(expo);
        }
        
        return services;
    }

}
