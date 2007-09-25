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
import org.apache.felix.ipojo.ServiceContext;
import org.apache.felix.ipojo.architecture.ComponentDescription;
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
public class ExportHandler extends CompositeHandler {

    /**
     * Service Scope.
     */
    private ServiceContext m_scope;

    /**
     * Parent context.
     */
    private BundleContext m_context;

    /**
     * List of exporters.
     */
    private List m_exporters = new ArrayList();

    /**
     * Is the handler valid ?
     * (Lifecycle controller)
     */
    private boolean m_valid;

    /**
     * Initialize the component type.
     * @param cd : component type description to populate.
     * @param metadata : component type metadata.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) {
        // Update the component type description
        Element[] exp = metadata.getElements("exports");
        for (int i = 0; i < exp.length; i++) {
            if (exp[i].containsAttribute("specification")) { // Malformed exports
                String specification = exp[i].getAttribute("specification");
                cd.addProvidedServiceSpecification(specification);
            }
        }
    }

    /**
     * Configure the handler.
     * @param metadata : the metadata of the component
     * @param conf : the instance configuration
     * @throws ConfigurationException : if the specification attribute is missing in the metadata.
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf) throws ConfigurationException {
        m_context = getCompositeManager().getContext();
        m_scope = getCompositeManager().getServiceContext();

        Element[] exp = metadata.getElements("exports");

        for (int i = 0; i < exp.length; i++) {
            boolean optional = false;
            boolean aggregate = false;
            String specification = null;

            if (exp[i].containsAttribute("specification")) {
                specification = exp[i].getAttribute("specification");
                String filter = "(objectClass=" + specification + ")";
                if (exp[i].containsAttribute("optional") && exp[i].getAttribute("optional").equalsIgnoreCase("true")) {
                    optional = true;
                }
                if (exp[i].containsAttribute("aggregate") && exp[i].getAttribute("aggregate").equalsIgnoreCase("true")) {
                    aggregate = true;
                }
                if (exp[i].containsAttribute("filter")) {
                    String classnamefilter = "(objectClass=" + specification + ")";
                    filter = null;
                    if ("".equals(exp[i].getAttribute("filter"))) {
                        filter = classnamefilter;
                    } else {
                        filter = "(&" + classnamefilter + exp[i].getAttribute("filter") + ")";
                    }
                }
                ServiceExporter si = new ServiceExporter(specification, filter, aggregate, optional, m_scope, m_context, this);
                m_exporters.add(si);
            } else { // Malformed exports
                log(Logger.ERROR, "Malformed exports : the specification attribute is mandatory");
                throw new ConfigurationException("Malformed exports : the specification attribute is mandatory", getCompositeManager().getFactory().getName());
            }
        }
    }

    /**
     * Start the handler.
     * Start importers and exporters.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        for (int i = 0; i < m_exporters.size(); i++) {
            ServiceExporter se = (ServiceExporter) m_exporters.get(i);
            se.start();
        }

        isHandlerValid();

    }

    /**
     * Stop the handler.
     * Stop all importers and exporters.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_exporters.size(); i++) {
            ServiceExporter se = (ServiceExporter) m_exporters.get(i);
            se.stop();
        }
    }

    /**
     * Check the handler validity.
     * @return true if all importers and exporters are valid
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    private boolean isHandlerValid() {
        for (int i = 0; i < m_exporters.size(); i++) {
            ServiceExporter se = (ServiceExporter) m_exporters.get(i);
            if (!se.isSatisfied()) {
                m_valid = false;
                return false;
            }
        }

        m_valid = true;
        return true;
    }

    /**
     * Notify the handler that an exporter becomes invalid.
     * 
     * @param exporter : the implicated exporter.
     */
    protected void invalidating(ServiceExporter exporter) {
        // An export is no more valid
        if (m_valid) {
            m_valid = false;
        }

    }

    /**
     * Notify the handler that an exporter becomes valid.
     * 
     * @param exporter : the implicated exporter.
     */
    protected void validating(ServiceExporter exporter) {
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
        return new ExportDescription(this, m_exporters);
    }
}
