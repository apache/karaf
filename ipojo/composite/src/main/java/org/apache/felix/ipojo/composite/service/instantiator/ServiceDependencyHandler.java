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
package org.apache.felix.ipojo.composite.service.instantiator;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.composite.instance.InstanceHandler;
import org.apache.felix.ipojo.composite.util.SourceManager;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.DependencyStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Service Instantiator Class. This handler allows to instantiate service
 * instance inside the composition.
 * 
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ServiceDependencyHandler extends CompositeHandler implements DependencyStateListener {

    /**
     * List of instances to manage.
     */
    private List/* <SvcInstance> */m_instances = new ArrayList();
    
    /**
     * List of importers.
     */
    private List/* <ServiceImporter> */ m_importers = new ArrayList();
    
    /**
     * Flag indicating if the handler has already finished the start method.
     */
    private boolean m_isStarted;
    
    /**
     * The handler description.
     */
    private ServiceInstantiatorDescription m_description;

    /**
     * Source Managers.
     */
    private List m_sources;
    
    
    /**
     * Create a Service instance object form the given Element.
     * This method parse the given element and configure the service instance object.
     * @param service : the Element describing the service instance
     * @throws ConfigurationException : the service instance cannot be created correctly
     */
    private void createServiceInstance(Element service) throws ConfigurationException {
        String spec = service.getAttribute("specification");
        if (spec == null) {
            throw new ConfigurationException("Malformed service : the specification attribute is mandatory");
        }
        String filter = "(&(!(factory.name=" + getCompositeManager().getFactory().getComponentDescription().getName() + "))(factory.state=1))"; // Cannot reinstantiate yourself
        String givenFilter = service.getAttribute("filter");
        if (givenFilter != null) {
            filter = "(&" + filter + givenFilter + ")"; //NOPMD
        }
        
        Filter fil;
        try {
            fil = getCompositeManager().getGlobalContext().createFilter(filter);
        } catch (InvalidSyntaxException e) {
            throw new ConfigurationException("Malformed filter " + filter + " : " + e.getMessage());
        }
        
        Properties prop = new Properties();
        Element[] props = service.getElements("property");
        for (int k = 0; props != null && k < props.length; k++) {
            try {
                InstanceHandler.parseProperty(props[k], prop);
            } catch (ParseException e) {
                throw new ConfigurationException("An instance configuration is invalid : " + e.getMessage());
            }
        }
        
        String aggregate = service.getAttribute("aggregate");
        boolean agg = aggregate != null && aggregate.equalsIgnoreCase("true");
        
        String optional = service.getAttribute("optional");
        boolean opt = optional != null && optional.equalsIgnoreCase("true");
        
        int policy = DependencyModel.getPolicy(service);
        
        Comparator cmp = DependencyModel.getComparator(service, getCompositeManager().getGlobalContext());
        
        SvcInstance inst = new SvcInstance(this, spec, prop, agg, opt, fil, cmp, policy);
        m_instances.add(inst);
        
        String sources = service.getAttribute("context-source");
        if (sources != null) {
            SourceManager source = new SourceManager(sources, filter, inst, getCompositeManager());
            if (m_sources == null) {
                m_sources = new ArrayList(1);
            }
            m_sources.add(source);
        }
    }
    
    /**
     * Create a Service importer object from the given Element.
     * This method parse the given element and configure the service importer object.
     * @param imp : Element describing the import
     * @param confFilter : instance filter customization
     * @throws ConfigurationException : the service importer cannot be created correctly
     */
    private void createServiceImport(Element imp, Dictionary confFilter) throws ConfigurationException {
        boolean optional = false;
        boolean aggregate = false;
        String specification = imp.getAttribute("specification");

        if (specification == null) { 
            // Malformed import
            error("Malformed import: the specification attribute is mandatory");
            throw new ConfigurationException("Malformed import : the specification attribute is mandatory");
        } else {
            String opt = imp.getAttribute("optional");
            optional = opt != null && opt.equalsIgnoreCase("true");

            String agg = imp.getAttribute("aggregate");
            aggregate = agg != null && agg.equalsIgnoreCase("true");

            String original = "(&(objectClass=" + specification + ")(!(instance.name=" + getCompositeManager().getInstanceName() + ")))"; // Cannot import yourself
            String filter = original;
            String givenFilter = imp.getAttribute("filter");
            if (givenFilter != null) {
                filter = "(&" + filter + givenFilter + ")"; //NOPMD
            }

            String identitity = imp.getAttribute("id");

            String scope = imp.getAttribute("scope");
            BundleContext context = getCompositeManager().getGlobalContext(); // Get the default bundle context.
            if (scope != null) {
                if (scope.equalsIgnoreCase("global")) {
                    context = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.GLOBAL);
                } else if (scope.equalsIgnoreCase("composite")) {
                    context = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.LOCAL);
                } else if (scope.equalsIgnoreCase("composite+global")) {
                    context = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.LOCAL_AND_GLOBAL);
                }
            }

            // Configure instance filter if available
            if (confFilter != null && identitity != null && confFilter.get(identitity) != null) {
                filter = "(&" + original + (String) confFilter.get(identitity) + ")";
            }

            Filter fil = null;
            if (filter != null) {
                try {
                    fil = getCompositeManager().getGlobalContext().createFilter(filter);
                } catch (InvalidSyntaxException e) {
                    throw new ConfigurationException("A required filter " + filter + " is malformed : " + e.getMessage());
                }
            }

            Comparator cmp = DependencyModel.getComparator(imp, getCompositeManager().getGlobalContext());
            Class spec = DependencyModel.loadSpecification(specification, getCompositeManager().getGlobalContext());
            int policy = DependencyModel.getPolicy(imp);

            ServiceImporter importer = new ServiceImporter(spec, fil, aggregate, optional, cmp, policy, context, identitity, this);
            m_importers.add(importer);
            
            String sources = imp.getAttribute("context-source");
            if (sources != null) {
                SourceManager source = new SourceManager(sources, filter, importer, getCompositeManager());
                if (m_sources == null) {
                    m_sources = new ArrayList(1);
                }
                m_sources.add(source);
            }
            
        }
    }

    /**
     * Configure the handler.
     * @param metadata : the metadata of the component
     * @param conf : the instance configuration
     * @throws ConfigurationException : the specification attribute is missing
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary conf) throws ConfigurationException {
        Element[] services = metadata.getElements("subservice");
        // Get instance filters
        Dictionary confFilter = null;
        if (conf.get("requires.filters") != null) {
            confFilter = (Dictionary) conf.get("requires.filters");
        }
        
        for (int i = 0; i < services.length; i++) {
            String action = services[i].getAttribute("action");
            if (action == null) {
                throw new ConfigurationException("The action attribute must be set to 'instantiate' or 'import'");
            } else if ("instantiate".equalsIgnoreCase(action)) {
                createServiceInstance(services[i]);
            } else if ("import".equalsIgnoreCase(action)) {
                createServiceImport(services[i], confFilter);
            } else {
                throw new ConfigurationException("Unknown action : " + action);
            }
        }
        
        m_description = new ServiceInstantiatorDescription(this, m_instances, m_importers);
    }

    /**
     * Start the service instantiator handler.
     * Start all created service instance.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        for (int i = 0; m_sources != null && i < m_sources.size(); i++) {
            SourceManager source = (SourceManager) m_sources.get(i);
            source.start();
        }
        
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter imp = (ServiceImporter) m_importers.get(i);
            imp.start();
        }
        
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            inst.start();
        }

        isHandlerValid();
        m_isStarted = true;
    }

    /**
     * Check the handler validity.
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    private void isHandlerValid() {
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter imp = (ServiceImporter) m_importers.get(i);
            if (imp.getState() != DependencyModel.RESOLVED) {
                setValidity(false);
                return;
            }
        }
        
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            if (inst.getState() != DependencyModel.RESOLVED) {
                setValidity(false);
                return;
            }
        }
        
        setValidity(true);
    }

    /**
     * Handler stop method.
     * Stop all created service instance.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; m_sources != null && i < m_sources.size(); i++) {
            SourceManager source = (SourceManager) m_sources.get(i);
            source.stop();
        }
        
        for (int i = 0; i < m_instances.size(); i++) {
            SvcInstance inst = (SvcInstance) m_instances.get(i);
            inst.stop();
        }
        
        for (int i = 0; i < m_importers.size(); i++) {
            ServiceImporter imp = (ServiceImporter) m_importers.get(i);
            imp.stop();
        }

        m_isStarted = false;
    }
    
    /**
     * State change callback.
     * This method is used to freeze the set of used provider if the static binding policy is used.
     * @param newState : the new state of the underlying instance
     * @see org.apache.felix.ipojo.Handler#stateChanged(int)
     */
    public void stateChanged(int newState) {
        // If we are becoming valid and started, check if we need to freeze importers.
        if (m_isStarted && newState == ComponentInstance.VALID) { 
            for (int i = 0; i < m_importers.size(); i++) {
                ServiceImporter imp = (ServiceImporter) m_importers.get(i);
                if (imp.getBindingPolicy() == DependencyModel.STATIC_BINDING_POLICY) {
                    imp.freeze();
                }
            }
            
            for (int i = 0; i < m_instances.size(); i++) {
                SvcInstance imp = (SvcInstance) m_instances.get(i);
                if (imp.getBindingPolicy() == DependencyModel.STATIC_BINDING_POLICY) {
                    imp.freeze();
                }
            }
        }
    }

    /**
     * An service instance becomes valid.
     * @param dep : dependency becoming valid.
     */
    public void validate(DependencyModel dep) {
        if (!getValidity()) {
            isHandlerValid();
        }
    }

    /**
     * A service instance becomes invalid.
     * @param dep : dependency becoming valid.
     */
    public void invalidate(DependencyModel dep) {
        if (getValidity()) {
            isHandlerValid();
        }
    }

    /**
     * Get the service instantiator handler description.
     * @return the description
     * @see org.apache.felix.ipojo.CompositeHandler#getDescription()
     */
    public HandlerDescription getDescription() {
        return m_description;
    }
    
    public List getInstances() {
        return m_instances;
    }
    
    public List getRequirements() {
        return m_importers;
    }
}
