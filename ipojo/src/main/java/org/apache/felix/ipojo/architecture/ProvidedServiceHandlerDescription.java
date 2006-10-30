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
package org.apache.felix.ipojo.architecture;

import java.util.Iterator;

import org.apache.felix.ipojo.handlers.providedservice.ProvidedService;
import org.apache.felix.ipojo.handlers.providedservice.ProvidedServiceHandler;

/**
 * Provided Service Handler Description.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandlerDescription extends HandlerDescription {

    /**
     * Provided Service Description list.
     */
    private ProvidedServiceDescription[] m_providedServices = new ProvidedServiceDescription[0];


    /**
     * Constructor.
     * @param isValid : the validity of the provided service handler.
     */
    public ProvidedServiceHandlerDescription(boolean isValid) {
        super(ProvidedServiceHandler.class.getName(), isValid);
    }

    /**
     * @return the provided service description list.
     */
    public ProvidedServiceDescription[] getProvidedServices() { return m_providedServices; }

    /**
     * Add a provided service.
     * @param pds : the provided service to add
     */
    public void addProvidedService(ProvidedServiceDescription pds) {
        //Verify that the provided service description is not already in the array.
        for (int i = 0; (i < m_providedServices.length); i++) {
            if (m_providedServices[i] == pds) {
                return; //NOTHING DO DO, the description is already in the array
            }
        }
        // The component Description is not in the array, add it
        ProvidedServiceDescription[] newPSD = new ProvidedServiceDescription[m_providedServices.length + 1];
        System.arraycopy(m_providedServices, 0, newPSD, 0, m_providedServices.length);
        newPSD[m_providedServices.length] = pds;
        m_providedServices = newPSD;
    }

    /**
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public String getHandlerInfo() {
        String info = "";
        for (int i = 0; i < m_providedServices.length; i++) {
            String state = "unregistered";
            if (m_providedServices[i].getState() == ProvidedService.REGISTERED) { state = "registered"; }
            String spec = "";
            for (int j = 0; j < m_providedServices[i].getServiceSpecification().length; j++) {
                spec += m_providedServices[i].getServiceSpecification()[j] + " ";
            }
            info += "\t Provided Service [" + spec + "] is " + state;
            Iterator it = m_providedServices[i].getProperties().keySet().iterator();
            while (it.hasNext()) {
                String k = (String) it.next();
                info += "\n\t\t Service Property : " + k + " = " + m_providedServices[i].getProperties().getProperty(k);
            }
        }
        return info;
    }



}
