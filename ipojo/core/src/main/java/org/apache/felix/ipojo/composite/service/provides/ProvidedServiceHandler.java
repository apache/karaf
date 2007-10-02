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
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.Factory;
import org.apache.felix.ipojo.HandlerManager;
import org.apache.felix.ipojo.IPojoConfiguration;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.instance.InstanceHandler;
import org.apache.felix.ipojo.composite.service.importer.ImportHandler;
import org.apache.felix.ipojo.composite.service.importer.ServiceImporter;
import org.apache.felix.ipojo.composite.service.instantiator.ServiceInstantiatorHandler;
import org.apache.felix.ipojo.composite.service.instantiator.SvcInstance;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * Composite Provided Service Handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandler extends CompositeHandler {

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
    private List m_managedServices = new ArrayList();

    /**
     * Handler validity.
     * (Lifecycle controller)
     */
    private boolean m_valid = false;

    /**
     * List of component type.
     */
    private List m_types;

    /**
     * Initialize the component type.
     * @param cd : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : metadata are incorrect.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) throws ConfigurationException {
        Element[] provides = metadata.getElements("provides", "");
        for (int i = 0; i < provides.length; i++) {
            if (provides[i].containsAttribute("specification")) {
                String spec = provides[i].getAttribute("specification");
                cd.addProvidedServiceSpecification(spec);
            } else {
                throw new ConfigurationException("Malformed provides : the specification attribute is mandatory", getCompositeManager().getFactory().getName());
            }
        }
    }

    /**
     * Configure the handler.
     * @param metadata : the metadata of the component
     * @param configuration : the instance configuration
     * @see org.apache.felix.ipojo.CompositeHandler#configure(org.apache.felix.ipojo.CompositeManager,
     * org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element metadata, Dictionary configuration) {
        m_context = getCompositeManager().getContext();

        // Get composition metadata
        Element[] provides = metadata.getElements("provides", "");
        if (provides.length == 0) { return; }

        for (int i = 0; i < provides.length; i++) {
            ProvidedService ps = new ProvidedService(this, provides[i], "" + i);
            m_managedServices.add(ps);
        }
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
            ProvidedService ps = (ProvidedService) m_managedServices.get(i);
            try {
                checkServiceSpecification(ps);
                ps.start();
            } catch (CompositionException e) {
                log(Logger.ERROR, "Cannot start the provided service handler", e);
                if (m_valid) { m_valid = false; }
                return;
            }
        }
        m_valid = true;
    }

    /**
     * Stop method.
     * Stop all managed provided service.
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

        // If the new state is VALID => register all the services
        if (state == ComponentInstance.VALID) {
            for (int i = 0; i < m_managedServices.size(); i++) {
                ProvidedService ps = (ProvidedService) m_managedServices.get(i);
                ps.register();
            }
            return;
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
        ImportHandler ih = (ImportHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":requires");
        ServiceInstantiatorHandler sh = (ServiceInstantiatorHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":service");

        for (int i = 0; sh != null && i < sh.getInstances().size(); i++) {
            SvcInstance svc = (SvcInstance) sh.getInstances().get(i);
            String itf = svc.getSpecification();
            boolean agg = svc.isAggregate();
            boolean opt = svc.isOptional();

            SpecificationMetadata sm = new SpecificationMetadata(itf, m_context, agg, opt, this);
            m_services.add(sm);
        }

        for (int i = 0; ih != null && i < ih.getRequirements().size(); i++) {
            ServiceImporter si = (ServiceImporter) ih.getRequirements().get(i);
            String itf = si.getSpecification();
            boolean agg = si.isAggregate();
            boolean opt = si.isOptional();

            SpecificationMetadata sm = new SpecificationMetadata(itf, m_context, agg, opt, this);
            m_services.add(sm);
        }
    }

    /**
     * Check composite requirement against service specification requirement is available.
     * @param ps : the provided service to check
     * @throws CompositionException : occurs if the specification field of the service specification cannot be analyzed correctly.
     */
    private void checkServiceSpecification(ProvidedService ps) throws CompositionException {
        try {
            Class spec = m_context.getBundle().loadClass(ps.getSpecification());
            Field specField = spec.getField("specification");
            Object o = specField.get(null);
            if (o instanceof String) {
                Element specification = ManifestMetadataParser.parse((String) o);
                Element[] reqs = specification.getElements("requires");
                for (int j = 0; j < reqs.length; j++) {
                    ServiceImporter imp = getAttachedRequirement(reqs[j]);
                    if (imp != null) {
                        // Fix service-level dependency flag
                        imp.setServiceLevelDependency();
                    }
                    checkRequirement(imp, reqs[j]);
                }
            } else {
                log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The specification field of the service specification " + ps.getSpecification() + " need to be a String");
                throw new CompositionException("Service Specification checking failed : The specification field of the service specification " + ps.getSpecification() + " need to be a String");
            }
        } catch (NoSuchFieldException e) {
            return; // No specification field
        } catch (ClassNotFoundException e) {
            log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The service specification " + ps.getSpecification() + " cannot be load");
            throw new CompositionException("The service specification " + ps.getSpecification() + " cannot be load : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The field 'specification' of the service specification " + ps.getSpecification() + " is not accessible : " + e.getMessage());
            throw new CompositionException("The field 'specification' of the service specification " + ps.getSpecification() + " is not accessible : " + e.getMessage());
        } catch (IllegalAccessException e) {
            log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The field 'specification' of the service specification " + ps.getSpecification() + " is not accessible : " + e.getMessage());
            throw new CompositionException("The field 'specification' of the service specification " + ps.getSpecification() + " is not accessible : " + e.getMessage());
        } catch (ParseException e) {
            log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The field 'specification' of the service specification " + ps.getSpecification() + " does not contain a valid String : " + e.getMessage());
            throw new CompositionException("The field 'specification' of the service specification " + ps.getSpecification() + " does not contain a valid String : " + e.getMessage());
        }
    }

    /**
     * Look for the implementation (i.e. composite) requirement for the given service-level requirement metadata.
     * @param element : the service-level requirement metadata
     * @return the ServiceImporter object, null if not found or if the DependencyHandler is not plugged to the instance
     */
    private ServiceImporter getAttachedRequirement(Element element) {
        ImportHandler ih = (ImportHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":requires");
        if (ih == null) { return null; }

        if (element.containsAttribute("id")) {
            // Look for dependency Id
            String id = element.getAttribute("id");
            for (int i = 0; i < ih.getRequirements().size(); i++) {
                ServiceImporter imp = (ServiceImporter) ih.getRequirements().get(i);
                if (imp.getId().equals(id)) { return imp; }
            }
        }

        // If not found or no id, look for a dependency with the same specification
        String requirement = element.getAttribute("specification");
        for (int i = 0; i < ih.getRequirements().size(); i++) {
            ServiceImporter imp = (ServiceImporter) ih.getRequirements().get(i);
            if (imp.getSpecification().equals(requirement)) { return imp; }
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
        boolean opt = false;
        if (elem.containsAttribute("optional") && elem.getAttribute("optional").equalsIgnoreCase("true")) {
            opt = true;
        }

        boolean agg = false;
        if (elem.containsAttribute("aggregate") && elem.getAttribute("aggregate").equalsIgnoreCase("true")) {
            agg = true;
        }

        if (imp == null) {
            // Add the missing requirement
            ImportHandler ih = (ImportHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":requires");
            if (ih == null) {
                // Look for the import handler factory
                HandlerManager ci = null;
                try {
                    ServiceReference[] refs = m_context.getServiceReferences(Factory.class.getName(), "(&(handler.name=requires)(handler.namespace=" + IPojoConfiguration.IPOJO_NAMESPACE + ")(handler.type=composite))");
                    Factory factory = (Factory) m_context.getService(refs[0]);
                    ci = (HandlerManager) factory.createComponentInstance(null, getCompositeManager().getServiceContext());
                } catch (Exception e) {
                    e.printStackTrace(); // Should not happen
                }
                // Add the required handler 
                try {
                    ci.init(getCompositeManager(), new Element("composite", ""), new Properties());
                } catch (ConfigurationException e) {
                    log(Logger.ERROR, "Internal error : cannot configure the Import Handler : " + e.getMessage());
                    throw new CompositionException("Internal error : cannot configure the Import Handler : " + e.getMessage());
                }
                ih = (ImportHandler) ci.getHandler();
                getCompositeManager().addCompositeHandler(ci);
            }
            String spec = elem.getAttribute("specification");
            String filter = "(&(objectClass=" + spec + ")(!(instance.name=" + getCompositeManager().getInstanceName() + ")))"; // Cannot import yourself
            if (elem.containsAttribute("filter")) {
                if (!"".equals(elem.getAttribute("filter"))) {
                    filter = "(&" + filter + elem.getAttribute("filter") + ")";
                }
            }
            
            ServiceImporter si = new ServiceImporter(spec, filter, agg, opt, getCompositeManager().getContext(), getCompositeManager().getServiceContext(), PolicyServiceContext.LOCAL, null, ih);
            ih.getRequirements().add(si);
            SpecificationMetadata sm = new SpecificationMetadata(spec, m_context, agg, opt, this);
            m_services.add(sm); // Update the available types
            return;
        }

        if (imp.isAggregate() && !agg) {
            log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
            throw new CompositionException("The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
        }

        if (elem.containsAttribute("filter")) {
            String filter = elem.getAttribute("filter");
            String filter2 = imp.getFilter();
            if (filter2 == null || !filter2.equalsIgnoreCase(filter)) {
                log(Logger.ERROR, "[" + getCompositeManager().getInstanceName() + "] The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
                throw new CompositionException("The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
            }
        }
    }

    public HandlerDescription getDescription() {
        return new ProvidedServiceHandlerDescription(this, m_managedServices);
    }

    /**
     * Build available instance types.
     */
    private void computeAvailableTypes() {
        InstanceHandler ih = (InstanceHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":instance");
        if (ih == null) {
            m_types = new ArrayList();
        } else {
            m_types = ih.getUsedType();
        }
    }

    public List getInstanceType() {
        return m_types;
    }

}
