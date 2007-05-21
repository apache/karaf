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
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Provided Service Handler Description for composite.
 * 
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandlerDescription extends HandlerDescription {

    /**
     * Provided Service Description list.
     */
    private List m_providedServices = new ArrayList();

    /**
     * Constructor.
     * 
     * @param isValid : the validity of the provided service handler.
     * @param ps : The list of Provided Service.
     */
    public ProvidedServiceHandlerDescription(boolean isValid, List ps) {
        super(ProvidedServiceHandler.class.getName(), isValid);
        m_providedServices = ps;
    }

    /**
     * Get the handler description.
     * @return the provided service handler description
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element services = super.getHandlerInfo();
        for (int i = 0; i < m_providedServices.size(); i++) {
            ProvidedService ps = (ProvidedService) m_providedServices.get(i);
            Element service = new Element("service", "");
            String state = "unregistered";
            if (ps.getState()) {
                state = "registered";
            }
            String spec = "[" + ps.getSpecification() + "]";
            service.addAttribute(new Attribute("Specification", spec));
            service.addAttribute(new Attribute("State", state));
            services.addElement(service);
        }
        return services;
    }

}
