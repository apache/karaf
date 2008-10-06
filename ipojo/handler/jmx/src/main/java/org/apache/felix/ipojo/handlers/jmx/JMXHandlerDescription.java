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
package org.apache.felix.ipojo.handlers.jmx;

import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Description of the JMX handler.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class JMXHandlerDescription extends HandlerDescription {

    /**
     * The referenced handler.
     */
    private MBeanHandler m_handler;

    /**
     * Constructs a new handler description for the given handler.
     * 
     * @param handler the handler to describe
     */
    public JMXHandlerDescription(MBeanHandler handler) {
        super(handler);
        m_handler = handler;
    }

    /**
     * Gets handler information.
     * 
     * @return the handler information.
     */
    public Element getHandlerInfo() {
        Element elem = super.getHandlerInfo();
        elem.addAttribute(new Attribute("registered", Boolean
            .toString(m_handler.isRegistered())));
        elem.addAttribute(new Attribute("objectName", m_handler
            .getUsedObjectName()));
        if (m_handler.isUsesMOSGi()) {
            String foundStr = null;

            if (m_handler.isMOSGiExists()) {
                foundStr = "found";
            } else {
                foundStr = "not_found";
            }
            elem.addAttribute(new Attribute("mosgi", foundStr));
        }

        return elem;
    }
}
