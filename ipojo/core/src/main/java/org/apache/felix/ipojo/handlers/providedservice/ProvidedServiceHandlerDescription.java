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
package org.apache.felix.ipojo.handlers.providedservice;

import java.util.Iterator;
import java.util.Properties;

import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.Constants;

/**
 * Provided Service Handler Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandlerDescription extends HandlerDescription {

    /**
     * Provided Service Description list.
     */
    private ProvidedServiceDescription[] m_providedServices = new ProvidedServiceDescription[0];
   
    /**
     * Constructor.
     * @param handler the handler.
     * @param pss the list of provided service
     */
    public ProvidedServiceHandlerDescription(ProvidedServiceHandler handler, ProvidedService[] pss) {
        super(handler);
        m_providedServices = new ProvidedServiceDescription[pss.length];
        for (int i = 0; i < pss.length; i++) {
            m_providedServices[i] = new ProvidedServiceDescription(pss[i]);
        }
    }

    /**
     * Get the provided service descriptions.
     * @return the provided service description list.
     */
    public ProvidedServiceDescription[] getProvidedServices() {
        return m_providedServices;
    }

    /**
     * Build the provided service handler description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element services = super.getHandlerInfo();
        for (int i = 0; i < m_providedServices.length; i++) {
            Element service = new Element("provides", null);
            StringBuffer spec = new StringBuffer("[");
            for (int j = 0; j < m_providedServices[i].getServiceSpecifications().length; j++) {
                if (j == 0) {
                    spec.append(m_providedServices[i].getServiceSpecifications()[j]);
                } else {
                    spec.append(',');
                    spec.append(m_providedServices[i].getServiceSpecifications()[j]);
                }
            }
            spec.append(']');
            service.addAttribute(new Attribute("specifications", spec.toString()));

            if (m_providedServices[i].getState() == ProvidedService.REGISTERED) {
                service.addAttribute(new Attribute("state", "registered"));
                service.addAttribute(new Attribute("service.id", m_providedServices[i].getServiceReference()
                        .getProperty(Constants.SERVICE_ID).toString()));
            } else {
                service.addAttribute(new Attribute("state", "unregistered"));
            }
            
            // Service Properties.
            Properties props = m_providedServices[i].getProperties();
            Iterator iterator = props.keySet().iterator();
            while (iterator.hasNext()) {
                Element prop = new Element("property", null);
                String name = (String) iterator.next();
                prop.addAttribute(new Attribute("name", name));
                Object obj = props.get(name);
                if (obj != null) {
                    prop.addAttribute(new Attribute("value", obj.toString()));
                } else {
                    prop.addAttribute(new Attribute("value", "null"));
                }
                service.addElement(prop);
            }
            
            // Service Controller.
            if (m_providedServices[i].getController() != null) {
                Element controller = new Element("controller", null);
                controller.addAttribute(new Attribute("value", m_providedServices[i].getController()));
                service.addElement(controller);
            }
            
            services.addElement(service);
        }
        return services;
    }

}
