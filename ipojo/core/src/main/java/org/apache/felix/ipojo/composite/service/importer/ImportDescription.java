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
package org.apache.felix.ipojo.composite.service.importer;

import java.util.List;

import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;

/**
 * Description of the Import Export Handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ImportDescription extends HandlerDescription {

    /**
     * List of exports.
     */
    private List m_imports;

    /**
     * Constructor.
     * 
     * @param h : composite handler
     * @param importers : list of managed imports
     */
    public ImportDescription(CompositeHandler h, List importers) {
        super(h);
        m_imports = importers;
    }

    /**
     * Build the ImportExport handler description.
     * @return the handler description
     * @see org.apache.felix.ipojo.architecture.HandlerDescription#getHandlerInfo()
     */
    public Element getHandlerInfo() {
        Element handler = super.getHandlerInfo();
        for (int i = 0; i < m_imports.size(); i++) {
            ServiceImporter imp = (ServiceImporter) m_imports.get(i);
            Element impo = new Element("Requires", "");
            impo.addAttribute(new Attribute("Specification", imp.getSpecification()));
            if (imp.getFilter() != null) {
                impo.addAttribute(new Attribute("Filter", imp.getFilter()));
            }
            if (imp.isSatisfied()) {
                impo.addAttribute(new Attribute("State", "resolved"));
                for (int j = 0; j < imp.getProviders().size(); j++) {
                    Element pr = new Element("Provider", "");
                    pr.addAttribute(new Attribute("name", (String) imp.getProviders().get(j)));
                    impo.addElement(pr);
                }
            } else {
                impo.addAttribute(new Attribute("State", "unresolved"));
            }
            handler.addElement(impo);
        }
        return handler;
    }

}
