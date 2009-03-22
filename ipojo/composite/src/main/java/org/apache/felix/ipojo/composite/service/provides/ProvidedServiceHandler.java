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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerFactory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.MissingHandlerException;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.UnacceptableConfiguration;
import org.apache.felix.ipojo.architecture.ComponentTypeDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.CompositeHandler;
import org.apache.felix.ipojo.composite.instance.InstanceHandler;
import org.apache.felix.ipojo.composite.service.instantiator.ServiceDependencyHandler;
import org.apache.felix.ipojo.composite.service.instantiator.ServiceImporter;
import org.apache.felix.ipojo.composite.service.instantiator.SvcInstance;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.util.DependencyModel;
import org.apache.felix.ipojo.util.DependencyStateListener;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 * Composite Provided Service Handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandler extends CompositeHandler implements DependencyStateListener {

    /**
     * External context.
     */
    private BundleContext m_context;
    
    /**
     * List of "available" services in the internal context.
     */
    private List m_services = new ArrayList();

    /**
     * List of exporters.
     */
    private List m_exporters = new ArrayList();

    /**
     * List of managed services.
     */
    private List m_managedServices = new ArrayList();

    /**
     * List of component type.
     */
    private List m_types;
    
    /**
     * Handler description.
     */
    private ProvidedServiceHandlerDescription m_description;

    /**
     * Initialize the component type.
     * @param desc : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentTypeDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentTypeDescription desc, Element metadata) throws ConfigurationException {
        Element[] provides = metadata.getElements("provides");
        for (int i = 0; i < provides.length; i++) {
            String action = provides[i].getAttribute("action");
            if (action == null) {
                throw new ConfigurationException("Invalid composition service providing : no specified action");
            } else if (action.equalsIgnoreCase("implement")) {
                String spec = provides[i].getAttribute("specification");
                if (spec == null) {
                    throw new ConfigurationException("Malformed provides : the specification attribute is mandatory");
                } else {
                    desc.addProvidedServiceSpecification(spec);
                }
            } else if (action.equalsIgnoreCase("export")) {
                String spec = provides[i].getAttribute("specification");
                if (spec == null) {
                    throw new ConfigurationException("Malformed exports - Missing the specification attribute");
                } else {
                    desc.addProvidedServiceSpecification(spec);
                }
            } else {
                throw new ConfigurationException("Invalid composition service providing : unknown action " + action);
            }
        }
    }

    /**
     * Configure the handler.
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @throws ConfigurationException  : the exporter cannot be created
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) throws ConfigurationException {
        m_context = getCompositeManager().getContext();

        Element[] provides = metadata.getElements("provides", "");
        for (int i = 0; i < provides.length; i++) {
            String action = provides[i].getAttribute("action");
            if (action.equalsIgnoreCase("implement")) {
                ProvidedService svc = new ProvidedService(this, provides[i], Integer.toString(i));
                m_managedServices.add(svc);
            } else if (action.equalsIgnoreCase("export")) {
                boolean optional = false;
                boolean aggregate = false;
                String specification = provides[i].getAttribute("specification");

                String filter = "(objectClass=" + specification + ")";

                String opt = provides[i].getAttribute("optional");
                optional = opt != null && opt.equalsIgnoreCase("true");

                String agg = provides[i].getAttribute("aggregate");
                aggregate = agg != null && agg.equalsIgnoreCase("true");

                String givenFilter = provides[i].getAttribute("filter");
                if (givenFilter != null) {
                    filter = "(&" + filter + givenFilter + ")"; //NOPMD
                }

                Filter fil = null;
                try {
                    fil = m_context.createFilter(filter);
                } catch (InvalidSyntaxException e) {
                    throw new ConfigurationException("An exporter filter is invalid " + filter + " : " + e.getMessage());
                }

                Comparator cmp = DependencyModel.getComparator(provides[i], m_context);
                int policy = DependencyModel.getPolicy(provides[i]);
                Class spec = DependencyModel.loadSpecification(specification, m_context);

                ServiceExporter imp = new ServiceExporter(spec, fil, aggregate, optional, cmp, policy, getCompositeManager().getServiceContext(), m_context, this, getCompositeManager());
                m_exporters.add(imp);
            } // Others case cannot happen. The test was already made during the factory initialization.
        }
        
        m_description = new ProvidedServiceHandlerDescription(this, m_managedServices, m_exporters);

    }

    /**
     * Start method.
     * Start all managed provided service.
     * @see org.apache.felix.ipojo.CompositeHandler#start()
     */
    public void start() {
        // Compute imports and instances
        computeAvailableServices();
        computeAvailableTypes();

        for (int i = 0; i < m_managedServices.size(); i++) {
            ProvidedService svc = (ProvidedService) m_managedServices.get(i);
            try {
                checkServiceSpecification(svc);
                svc.start();
            } catch (CompositionException e) {
                error("Cannot start the provided service handler", e);
                setValidity(false);
                return;
            }
        }

        for (int i = 0; i < m_exporters.size(); i++) {
            ServiceExporter exp = (ServiceExporter) m_exporters.get(i);
            exp.start();
        }

        isHandlerValid();
    }

    /**
     * Stop method.
     * Stop all managed provided service.
     * @see org.apache.felix.ipojo.CompositeHandler#stop()
     */
    public void stop() {
        for (int i = 0; i < m_managedServices.size(); i++) {
            ProvidedService svc = (ProvidedService) m_managedServices.get(i);
            svc.stop();
        }

        for (int i = 0; i < m_exporters.size(); i++) {
            ServiceExporter exp = (ServiceExporter) m_exporters.get(i);
            exp.stop();
        }
    }

    /**
     * Check the handler validity.
     * @see org.apache.felix.ipojo.CompositeHandler#isValid()
     */
    private void isHandlerValid() {
        for (int i = 0; i < m_exporters.size(); i++) {
            ServiceExporter exp = (ServiceExporter) m_exporters.get(i);
            if (exp.getState() != DependencyModel.RESOLVED) {
                setValidity(false);
                return;
            }
        }

        setValidity(true);
    }

    /**
     * Handler state changed.
     * @param state : the new instance state.
     * @see org.apache.felix.ipojo.CompositeHandler#stateChanged(int)
     */
    public void stateChanged(int state) {
        if (state == ComponentInstance.INVALID) {
            for (int i = 0; i < m_managedServices.size(); i++) {
                ProvidedService svc = (ProvidedService) m_managedServices.get(i);
                svc.unregister();
            }
            return;
        }

        // If the new state is VALID => register all the services
        if (state == ComponentInstance.VALID) {
            for (int i = 0; i < m_managedServices.size(); i++) {
                ProvidedService svc = (ProvidedService) m_managedServices.get(i);
                svc.register();
            }
            return;
        }
    }

    /**
     * Notify the handler that an exporter becomes invalid.
     * 
     * @param exporter : the implicated exporter.
     */
    public void invalidate(DependencyModel exporter) {
        // An export is no more valid
        if (getValidity()) {
            setValidity(false);
        }

    }

    /**
     * Notify the handler that an exporter becomes valid.
     * 
     * @param exporter : the implicated exporter.
     */
    public void validate(DependencyModel exporter) {
        // An import becomes valid
        if (!getValidity()) {
            isHandlerValid();
        }
    }

    /**
     * Build the list of available specification.
     * @return the list of available specification.
     */
    protected List getSpecifications() {
        return m_services;
    }

    /**
     * Build the list of available specifications.
     */
    private void computeAvailableServices() {
        // Get instantiated services :
        ServiceDependencyHandler handler = (ServiceDependencyHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":subservice");

        for (int i = 0; handler != null && i < handler.getInstances().size(); i++) {
            SvcInstance svc = (SvcInstance) handler.getInstances().get(i);
            String itf = svc.getServiceSpecification();
            boolean agg = svc.isAggregate();
            boolean opt = svc.isOptional();

            SpecificationMetadata specMeta = new SpecificationMetadata(itf, m_context, agg, opt, this);
            m_services.add(specMeta);
        }

        for (int i = 0; handler != null && i < handler.getRequirements().size(); i++) {
            ServiceImporter imp = (ServiceImporter) handler.getRequirements().get(i);
            String itf = imp.getSpecification().getName();
            boolean agg = imp.isAggregate();
            boolean opt = imp.isOptional();

            SpecificationMetadata specMeta = new SpecificationMetadata(itf, m_context, agg, opt, this);
            m_services.add(specMeta);
        }
    }

    /**
     * Check composite requirement against service specification requirement is available.
     * @param svc : the provided service to check
     * @throws CompositionException : occurs if the specification field of the service specification cannot be analyzed correctly.
     */
    private void checkServiceSpecification(ProvidedService svc) throws CompositionException {
        try {
            Class spec = m_context.getBundle().loadClass(svc.getSpecification());
            Field specField = spec.getField("specification");
            Object object = specField.get(null);
            if (object instanceof String) {
                Element specification = ManifestMetadataParser.parse((String) object);
                Element[] reqs = specification.getElements("requires");
                for (int j = 0; reqs != null && j < reqs.length; j++) {
                    ServiceImporter imp = getAttachedRequirement(reqs[j]);
                    if (imp != null) {
                        // Fix service-level dependency flag
                        imp.setServiceLevelDependency();
                    }
                    checkRequirement(imp, reqs[j]);
                }
            } else {
                error("[" + getCompositeManager().getInstanceName() + "] The specification field of the service specification " + svc.getSpecification() + " needs to be a String");
                throw new CompositionException("Service Specification checking failed : The specification field of the service specification " + svc.getSpecification() + " needs to be a String");
            }
        } catch (NoSuchFieldException e) {
            return; // No specification field
        } catch (ClassNotFoundException e) {
            error("[" + getCompositeManager().getInstanceName() + "] The service specification " + svc.getSpecification() + " cannot be load");
            throw new CompositionException("The service specification " + svc.getSpecification() + " cannot be load : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            error("[" + getCompositeManager().getInstanceName() + "] The field 'specification' of the service specification " + svc.getSpecification() + " is not accessible : " + e.getMessage());
            throw new CompositionException("The field 'specification' of the service specification " + svc.getSpecification() + " is not accessible : " + e.getMessage());
        } catch (IllegalAccessException e) {
            error("[" + getCompositeManager().getInstanceName() + "] The field 'specification' of the service specification " + svc.getSpecification() + " is not accessible : " + e.getMessage());
            throw new CompositionException("The field 'specification' of the service specification " + svc.getSpecification() + " is not accessible : " + e.getMessage());
        } catch (ParseException e) {
            error("[" + getCompositeManager().getInstanceName() + "] The field 'specification' of the service specification " + svc.getSpecification() + " does not contain a valid String : " + e.getMessage());
            throw new CompositionException("The field 'specification' of the service specification " + svc.getSpecification() + " does not contain a valid String : " + e.getMessage());
        }
    }

    /**
     * Look for the implementation (i.e. composite) requirement for the given service-level requirement metadata.
     * @param element : the service-level requirement metadata
     * @return the ServiceImporter object, null if not found or if the DependencyHandler is not plugged to the instance
     */
    private ServiceImporter getAttachedRequirement(Element element) {
        ServiceDependencyHandler handler = (ServiceDependencyHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":subservice");
        if (handler == null) { return null; }

        String identity = element.getAttribute("id");
        if (identity != null) {
            // Look for dependency Id
            for (int i = 0; i < handler.getRequirements().size(); i++) {
                ServiceImporter imp = (ServiceImporter) handler.getRequirements().get(i);
                if (imp.getId().equals(identity)) { return imp; }
            }
        }

        // If not found or no id, look for a dependency with the same specification
        String requirement = element.getAttribute("specification");
        for (int i = 0; i < handler.getRequirements().size(); i++) {
            ServiceImporter imp = (ServiceImporter) handler.getRequirements().get(i);
            if (imp.getId().equals(requirement) || imp.getSpecification().getName().equals(requirement)) { return imp; }
        }
        return null;
    }

    /**
     * Check the correctness of the composite requirement against the service level dependency.
     * @param imp : requirement to check
     * @param elem : service-level dependency metadata
     * @throws CompositionException : occurs if the requirement does not match with service-level specification requirement
     */
    private void checkRequirement(ServiceImporter imp, Element elem) throws CompositionException {
        String optional = elem.getAttribute("optional");
        boolean opt = optional != null && optional.equalsIgnoreCase("true");

        String aggregate = elem.getAttribute("aggregate");
        boolean agg = aggregate != null && aggregate.equalsIgnoreCase("true");

        if (imp == null) {
            // Add the missing requirement
            ServiceDependencyHandler handler = (ServiceDependencyHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":subservice");
            if (handler == null) {
                // Look for the ServiceDependencyHandler factory
                HandlerManager handlerManager = null;
                try {
                    ServiceReference[] refs = m_context.getServiceReferences(Factory.class.getName(), "(&(handler.name=subservice)(handler.namespace=" + HandlerFactory.IPOJO_NAMESPACE + ")(handler.type=composite))");
                    Factory factory = (Factory) m_context.getService(refs[0]);
                    handlerManager = (HandlerManager) factory.createComponentInstance(null, getCompositeManager().getServiceContext());
                } catch (InvalidSyntaxException e) {
                    // Should not happen
                } catch (UnacceptableConfiguration e) {
                    // Should not happen
                } catch (MissingHandlerException e) {
                    // Should not happen
                } catch (ConfigurationException e) {
                    // Should not happen
                }
                
                // Add the required handler 
                try {
                    handlerManager.init(getCompositeManager(), new Element("composite", ""), new Properties());
                } catch (ConfigurationException e) {
                    error("Internal error : cannot configure the Import Handler : " + e.getMessage());
                    throw new CompositionException("Internal error : cannot configure the Import Handler : " + e.getMessage());
                }
                handler = (ServiceDependencyHandler) handlerManager.getHandler();
                getCompositeManager().addCompositeHandler(handlerManager);
            }

            String spec = elem.getAttribute("specification");
            String filter = "(&(objectClass=" + spec + ")(!(instance.name=" + getCompositeManager().getInstanceName() + ")))"; // Cannot import yourself
            String givenFilter = elem.getAttribute("filter");
            if (givenFilter != null) {
                filter = "(&" + filter + givenFilter + ")"; //NOPMD
            }

            BundleContext context = new PolicyServiceContext(getCompositeManager().getGlobalContext(), getCompositeManager().getParentServiceContext(), PolicyServiceContext.GLOBAL);

            Filter fil = null;
            try {
                fil = getCompositeManager().getGlobalContext().createFilter(filter);
            } catch (InvalidSyntaxException e) {
                throw new CompositionException("A required filter " + filter + " is malformed : " + e.getMessage());
            }

            Class specToImport = null;
            try {
                specToImport = getCompositeManager().getGlobalContext().getBundle().loadClass(spec);
            } catch (ClassNotFoundException e) {
                throw new CompositionException("A required specification cannot be loaded : " + spec);
            }

            ServiceImporter importer = new ServiceImporter(specToImport, fil, agg, opt, null, DependencyModel.DYNAMIC_BINDING_POLICY, context, null, handler);

            handler.getRequirements().add(importer);
            SpecificationMetadata specMeta = new SpecificationMetadata(spec, m_context, agg, opt, this);
            m_services.add(specMeta); // Update the available types
            return;
        }

        if (imp.isAggregate() && !agg) {
            error("[" + getCompositeManager().getInstanceName() + "] The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
            throw new CompositionException("The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
        }

        String filter = elem.getAttribute("filter");
        if (filter != null) {
            String filter2 = imp.getFilter();
            if (filter2 == null || !filter2.equalsIgnoreCase(filter)) {
                error("[" + getCompositeManager().getInstanceName() + "] The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
                throw new CompositionException("The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
            }
        }
    }

    public HandlerDescription getDescription() {
        return m_description;
    }

    /**
     * Build available instance types.
     */
    private void computeAvailableTypes() {
        InstanceHandler handler = (InstanceHandler) getHandler(HandlerFactory.IPOJO_NAMESPACE + ":instance");
        if (handler == null) {
            m_types = new ArrayList();
        } else {
            m_types = handler.getUsedType();
        }
    }

    public List getInstanceType() {
        return m_types;
    }

}
