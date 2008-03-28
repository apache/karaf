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

import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Handler Description.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class HandlerDescription {

    /**
     * Handler Class Name (i.e namespace).
     */
    private String m_handlerName;

    /**
     * Is the handler valid.
     */
    private boolean m_isValid;

    /**
     * Constructor.
     * 
     * @param handler : handler.
     */
    public HandlerDescription(Handler handler) {
        m_handlerName = handler.getClass().getName();
        m_isValid = handler.isValid();
    }

    /**
     * Check if the handler is valid.
     * @return true if the handler is valid.
     */
    public boolean isValid() {
        return m_isValid;
    }

    /**
     * Get the handler name.
     * @return the handler name (i.e. namespace).
     */
    public String getHandlerName() {
        return m_handlerName;
    }

    /**
     * Get handler information.
     * @return the handler information.
     */
    public Element getHandlerInfo() {
        Element elem = new Element("Handler", "");
        elem.addAttribute(new Attribute("name", m_handlerName));
        if (m_isValid) {
            elem.addAttribute(new Attribute("state", "valid"));
        } else {
            elem.addAttribute(new Attribute("state", "invalid"));
        }
        return elem;
    }

}
