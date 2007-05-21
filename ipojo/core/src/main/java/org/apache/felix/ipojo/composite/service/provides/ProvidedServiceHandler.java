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
import java.util.Dictionary;
import java.util.List;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.CompositeManager;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * Composite Provided Service Handler.
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandler extends CompositeHandler {

    /**
     * Reference on the instance.
     */
    private CompositeManager m_manager;

    /**
     * External context.
     */
    private BundleContext m_context;

    /**
     * List of "available" services in the internal context.
     */
    private List m_services = new ArrayList();

    /**
     * List of managed services.
     */
    private ArrayList m_managedServices = new ArrayList();

    /**
     * Handler validity. False if
     */
    private boolean m_valid = false;

    /**
     * List of component type.
     */
    private ArrayList m_types;

    /**
     * Configure the handler.
     * 
     * @param im : the instance manager
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(CompositeManager im, Element metadata, Dictionary configuration) {
        m_manager = im;
        m_context = im.getContext();

        // Get composition metadata
        Element[] provides = metadata.getElements("provides", "");
        if (provides.length == 0) {
            return;
        }

        // Compute imports and instances
        computeAvailableServices(metadata);
        computeAvailableTypes(metadata);

        for (int i = 0; i < provides.length; i++) {
            ProvidedService ps = new ProvidedService(this, provides[i], "" + i);
            m_managedServices.add(ps);
            im.getComponentDescription().addProvidedServiceSpecification(ps.getSpecification());
        }

        im.register(this);
    }

    /**
     * Start metod.
     * Start all managed provided service.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        for (int i = 0; i < m_managedServices.size(); i++) {
            ProvidedService ps = (ProvidedService) m_managedServices.get(i);
            try {
                ps.start();
            } catch (CompositionException e) {
                m_manager.getFactory().getLogger().log(Logger.ERROR, "Cannot start the provided service handler", e);
                m_valid = false;
                return;
            }
        }
        m_valid  = true;
    }
    
    /**
     * Check the handler validity.
     * @return true if the handler is valid.
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    public boolean isValid() {
        return m_valid;
    }

    /**
     * Stop method.
     * Stop all managedprovided service.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_managedServices.size(); i++) {
            ProvidedService ps = (ProvidedService) m_managedServices.get(i);
            ps.stop();
        }
    }

    /**
     * Handler state changed.
     * @param state : the new instance state.
     * @see org.apache.felix.ipojo.CompositeHandler#stateChanged(int)
     */
    public void stateChanged(int state) {
        if (state == ComponentInstance.INVALID) {
            for (int i = 0; i < m_managedServices.size(); i++) {
                ProvidedService ps = (ProvidedService) m_managedServices.get(i);
                ps.unregister();
            }
            return;
        }

        // If the new state is VALID => regiter all the services
        if (state == ComponentInstance.VALID) {
            for (int i = 0; i < m_managedServices.size(); i++) {
                ProvidedService ps = (ProvidedService) m_managedServices.get(i);
                ps.register();
            }
            return;
        }
    }

    protected CompositeManager getManager() {
        return m_manager;
    }

    /**
     * Build the list of available specification.
     * @return the list of available specification.
     */
    protected List getSpecifications() {
        return m_services;
    }

    /**
     * Build available specification.
     * 
     * @param metadata : composite metadata
     */
    private void computeAvailableServices(Element metadata) {
        // Get instantiated services :
        Element[] services = metadata.getElements("service", "");
        for (int i = 0; i < services.length; i++) {
            String itf = services[i].getAttribute("specification");
            boolean agg = false;
            boolean opt = false;
            if (services[i].containsAttribute("aggregate") && services[i].getAttribute("aggregate").equalsIgnoreCase("true")) {
                agg = true;
            }
            if (services[i].containsAttribute("optional") && services[i].getAttribute("optional").equalsIgnoreCase("true")) {
                opt = true;
            }
            SpecificationMetadata sm = new SpecificationMetadata(itf, m_context, agg, opt, this);
            m_services.add(sm);
        }

        Element[] imports = metadata.getElements("import", "");
        for (int i = 0; i < imports.length; i++) {
            String itf = imports[i].getAttribute("specification");
            boolean agg = false;
            boolean opt = false;
            if (imports[i].containsAttribute("aggregate") && imports[i].getAttribute("aggregate").equalsIgnoreCase("true")) {
                agg = true;
            }
            if (imports[i].containsAttribute("optional") && imports[i].getAttribute("optional").equalsIgnoreCase("true")) {
                opt = true;
            }
            SpecificationMetadata sm = new SpecificationMetadata(itf, m_context, agg, opt, this);
            m_services.add(sm);
        }
    }

    public HandlerDescription getDescription() {
        return new ProvidedServiceHandlerDescription(true, m_managedServices);
    }
    
    /**
     * Build available instance type.
     * 
     * @param metadata : composite metadata
     */
    private void computeAvailableTypes(Element metadata) {
        m_types = new ArrayList();
        Element[] instances = metadata.getElements("instance", "");
        for (int i = 0; i < instances.length; i++) {
            String itf = instances[i].getAttribute("component");
            m_types.add(itf);
        }
    }

    public List getInstanceType() {
        return m_types;
    }

}
