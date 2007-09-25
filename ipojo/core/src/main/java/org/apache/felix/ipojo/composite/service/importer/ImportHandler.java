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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * This handler manages the import and the export of services from /
 * to the parent context.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ImportHandler extends CompositeHandler {

    /**
     * Service Scope.
     */
    private ServiceContext m_scope;

    /**
     * Parent context.
     */
    private BundleContext m_context;

    /**
     * List of importers.
     */
    private List m_importers = new ArrayList();

    /**
     * Is the handler valid ?
     * (Lifecycle controller)
     */
    private boolean m_valid;
    

    /**
     * Configure the handler.
     * 
     * @param metadata : the metadata of the component
     * @param conf : the instance configuration
     * @throws ConfigurationException : the specification attribute is missing. 
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf) throws ConfigurationException {
        m_context = getCompositeManager().getContext();
        m_scope = getCompositeManager().getServiceContext();

        Element[] imp = metadata.getElements("requires");

        for (int i = 0; i < imp.length; i++) {
            boolean optional = false;
            boolean aggregate = false;
            String specification = null;

            if (imp[i].containsAttribute("specification")) {
                specification = imp[i].getAttribute("specification");
                String filter = "(&(objectClass=" + specification + ")(!(instance.name=" + getCompositeManager().getInstanceName() + ")))"; // Cannot import yourself
                if (imp[i].containsAttribute("optional") && imp[i].getAttribute("optional").equalsIgnoreCase("true")) {
                    optional = true;
                }
                if (imp[i].containsAttribute("aggregate") && imp[i].getAttribute("aggregate").equalsIgnoreCase("true")) {
                    aggregate = true;
                }
                if (imp[i].containsAttribute("filter")) {
                    if (!"".equals(imp[i].getAttribute("filter"))) {
                        filter = "(&" + filter + imp[i].getAttribute("filter") + ")";
                    }
                }
                
                String id = null;
                if (imp[i].containsAttribute("id")) {
                    id = imp[i].getAttribute("id");
                }
                
                int scopePolicy = -1;
                if (imp[i].containsAttribute("scope")) {
                    if (imp[i].getAttribute("scope").equalsIgnoreCase("global")) {
                        scopePolicy = PolicyServiceContext.GLOBAL;
                    } else if (imp[i].getAttribute("scope").equalsIgnoreCase("composite")) {
                        scopePolicy = PolicyServiceContext.LOCAL;
                    } else if (imp[i].getAttribute("scope").equalsIgnoreCase("composite+global")) {
                        scopePolicy = PolicyServiceContext.LOCAL_AND_GLOBAL;
                    }                
                }
                ServiceImporter si = new ServiceImporter(specification, filter, aggregate, optional, m_context, m_scope, scopePolicy, id, this);
                m_importers.add(si);
            } else { // Malformed import
                log(Logger.ERROR, "Malformed imports : the specification attribute is mandatory");
                throw new ConfigurationException("Malformed imports : the specification attribute is mandatory", getCompositeManager().getFactory().getName());
            }
        }
    }

    /**
     * Start the handler.
     * Start importers and exporters.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter si = (ServiceImporter) m_importers.get(i);
            si.start();
        }
        isHandlerValid();
    }

    /**
     * Stop the handler.
     * Stop all importers and exporters.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter si = (ServiceImporter) m_importers.get(i);
            si.stop();
        }
    }

    /**
     * Check the handler validity.
     * @return true if all importers and exporters are valid
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    public boolean isHandlerValid() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter si = (ServiceImporter) m_importers.get(i);
            if (!si.isSatisfied()) {
                m_valid = false;
                return false;
            }
        }
        m_valid = true;
        return true;
    }

    /**
     * Notify the handler that an importer is no more valid.
     * 
     * @param importer : the implicated importer.
     */
    protected void invalidating(ServiceImporter importer) {
        // An import is no more valid
        if (m_valid) {
            m_valid = false;
        }
    }

    /**
     * Notify the handler that an importer becomes valid.
     * 
     * @param importer : the implicated importer.
     */
    protected void validating(ServiceImporter importer) {
        // An import becomes valid
        if (!m_valid) {
            isHandlerValid();
        }
    }

    /**
     * Get the import / export handler description.
     * @return the handler description
     * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
     */
    public HandlerDescription getDescription() {
        return new ImportDescription(this, m_importers);
    }
    
    public List getRequirements() {
        return m_importers;
    }
}
