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
package org.apache.felix.ipojo.handler.wbp;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.osgi.framework.InvalidSyntaxException;

/**
 * White board pattern handler.
 * This handler aims to automate white board patterns by invoking callback when needed.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class WhiteBoardPatternHandler extends PrimitiveHandler {
    
    /**
     * Handler namespace.
     */
    public static final String NAMESPACE = "org.apache.felix.ipojo.white-board-pattern";
    
    /**
     * White board pattern to manage. By default just one. 
     */
    private List m_managers = new ArrayList(1);

    /**
     * Configure method. It will parse metadata to analyze org.apache.felix.ipojo.white-board-pattern:wbp element. 
     * @param elem : component type description
     * @param dict : instance description
     * @throws ConfigurationException : occurs when the description is not valid.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element elem, Dictionary dict) throws ConfigurationException {
        Element[] elems = elem.getElements("wbp", NAMESPACE);
        for (int i = 0; i < elems.length; i++) {
            String filter = elems[i].getAttribute("filter");
            String onArrival = elems[i].getAttribute("onArrival");
            String onDeparture = elems[i].getAttribute("onDeparture");
            String onModification = elems[i].getAttribute("onModification");
            
            if (filter == null) {
                throw new ConfigurationException("The white board pattern element requires a filter attribute");
            }
            if (onArrival == null || onDeparture == null) {
                throw new ConfigurationException("The white board pattern element requires the onArrival and onDeparture attributes");
            }
            
            try {
                WhiteBoardManager wbm = new WhiteBoardManager(this, getInstanceManager().getContext().createFilter(filter), onArrival, onDeparture, onModification);
                m_managers.add(wbm);
            } catch (InvalidSyntaxException e) {
                throw new ConfigurationException("The filter " + filter + " is invalid : " + e);
            }
        }
        
    }

    /**
     * Start method : start managers.
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() {
        for (int i = 0; i < m_managers.size(); i++) {
            ((WhiteBoardManager) m_managers.get(i)).start();
        }
    }

    /**
     * Stop method : stop managers.
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_managers.size(); i++) {
            ((WhiteBoardManager) m_managers.get(i)).stop();
        } 
    }

}
