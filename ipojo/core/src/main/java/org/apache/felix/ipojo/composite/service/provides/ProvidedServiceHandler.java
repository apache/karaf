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

import org.apache.felix.ipojo.ComponentInstance;
import org.apache.felix.ipojo.CompositeHandler;
import org.apache.felix.ipojo.CompositeManager;
import org.apache.felix.ipojo.PolicyServiceContext;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.composite.instance.InstanceHandler;
import org.apache.felix.ipojo.composite.service.importer.ImportExportHandler;
import org.apache.felix.ipojo.composite.service.importer.ServiceImporter;
import org.apache.felix.ipojo.composite.service.instantiator.ServiceInstantiatorHandler;
import org.apache.felix.ipojo.composite.service.instantiator.SvcInstance;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.util.Logger;
import org.osgi.framework.BundleContext;

/**
 * Composite Provided Service Handler.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
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
    private List m_managedServices = new ArrayList();

    /**
     * Handler validity. False if
     */
    private boolean m_valid = false;

    /**
     * List of component type.
     */
    private List m_types;

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
        
        for (int i = 0; i < provides.length; i++) {
            ProvidedService ps = new ProvidedService(this, provides[i], "" + i);
            m_managedServices.add(ps);
            // Check requirements against the service specification
            if (!checkServiceSpecification(ps)) {
                return;
            }
            im.getComponentDescription().addProvidedServiceSpecification(ps.getSpecification());
        }

        // Compute imports and instances
        computeAvailableServices();
        computeAvailableTypes();

        
        im.register(this);
    }

    /**
     * Start method.
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
     * Build the list of available specifications.
     */
    private void computeAvailableServices() {
        // Get instantiated services :
        ImportExportHandler ih = (ImportExportHandler) m_manager.getCompositeHandler(ImportExportHandler.class.getName());
        ServiceInstantiatorHandler sh = (ServiceInstantiatorHandler) m_manager.getCompositeHandler(ServiceInstantiatorHandler.class.getName());
        
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
     * @return true if the composite is a correct implementation of the service
     */
    private boolean checkServiceSpecification(ProvidedService ps) {
        try {
            Class spec = m_manager.getFactory().loadClass(ps.getSpecification());
            Field specField = spec.getField("specification");     
            Object o = specField.get(null);
            if (!(o instanceof String)) {
                m_manager.getFactory().getLogger().log(Logger.ERROR, "[" + m_manager.getInstanceName() + "] The specification field of the service specification " + ps.getSpecification() + " need to be a String");                                                                                                                                                                    
                return false;
            } else {
                Element specification = ManifestMetadataParser.parse((String) o);
                Element[] reqs = specification.getElements("requires");
                for (int j = 0; j < reqs.length; j++) {
                    ServiceImporter imp = getAttachedRequirement(reqs[j]);
                    if (imp != null) {
                        // Fix service-level dependency flag
                        imp.setServiceLevelDependency();
                    }
                    if (!isRequirementCorrect(imp, reqs[j])) {
                        return false;
                    }
                }
            }
        } catch (NoSuchFieldException e) {
            return true;  // No specification field
        } catch (ClassNotFoundException e) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[" + m_manager.getInstanceName() + "] The service specification " + ps.getSpecification() + " cannot be load");                                                                                                                                                                    
            return false;
        } catch (IllegalArgumentException e) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[" + m_manager.getInstanceName() + "] The field 'specification' of the service specification " + ps.getSpecification() + " is not accessible : " + e.getMessage());                                                                                                                                                                    
            return false;
        } catch (IllegalAccessException e) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[" + m_manager.getInstanceName() + "] The field 'specification' of the service specification " + ps.getSpecification() + " is not accessible : " + e.getMessage());                                                                                                                                                                    
            return false;
        } catch (ParseException e) {
            m_manager.getFactory().getLogger().log(Logger.ERROR, "[" + m_manager.getInstanceName() + "] The field 'specification' of the service specification " + ps.getSpecification() + " does not contain a valid String : " + e.getMessage());                                                                                                                                                                    
            return false;
        }
        return true;
    }
    
    /**
     * Look for the implementation (i.e. composite) requirement for the given service-level requirement metadata.
     * @param element : the service-level requirement metadata
     * @return the ServiceImporter object, null if not found or if the DependencyHandler is not plugged to the instance
     */
    private ServiceImporter getAttachedRequirement(Element element) {
        ImportExportHandler ih = (ImportExportHandler) m_manager.getCompositeHandler(ImportExportHandler.class.getName());
        if (ih == null) { 
            return null;
        }
        
        if (element.containsAttribute("id")) {
            // Look for dependency Id
            String id = element.getAttribute("id");
            for (int i = 0; i < ih.getRequirements().size(); i++) {
                ServiceImporter imp = (ServiceImporter) ih.getRequirements().get(i);
                if (imp.getId().equals(id)) {
                    return imp; 
                }
            }
        }
        
        // If not found or no id, look for a dependency with the same specification
        String requirement = element.getAttribute("specification");
        for (int i = 0; i < ih.getRequirements().size(); i++) {
            ServiceImporter imp = (ServiceImporter) ih.getRequirements().get(i);
            if (imp.getSpecification().equals(requirement)) {
                return imp; 
            }
        }
        
        return null;
    }
    
    /**
     * Check the correctness of the composite requirement against the service level dependency.
     * @param imp : requirement to check
     * @param elem : service-level dependency metadata
     * @return true if the dependency is correct, false otherwise
     */
    private boolean isRequirementCorrect(ServiceImporter imp, Element elem) {
        boolean opt = false;
        if (elem.containsAttribute("optional") && elem.getAttribute("optional").equalsIgnoreCase("true")) {
            opt = true;
        }
        
        boolean agg = false;
        if (elem.containsAttribute("aggregate") && elem.getAttribute("aggregate").equalsIgnoreCase("true")) {
            agg = true;
        }

        if (imp == null) {
            //TODO do we need to add the requirement for optional service-level dependency ?
            // Add the missing requirement
            ImportExportHandler ih = (ImportExportHandler) m_manager.getCompositeHandler(ImportExportHandler.class.getName());
            if (ih == null) {
                // Add the importer handler 
                ih = new ImportExportHandler();
                ih.configure(m_manager, new Element("composite", "") , null); // Enter fake info in the configure method.
                m_manager.register(ih);
            }
            String spec = elem.getAttribute("specification");
            String filter = "(&(objectClass=" + spec + ")(!(service.pid=" + m_manager.getInstanceName() + ")))"; // Cannot import yourself
            if (elem.containsAttribute("filter")) {
                if (!elem.getAttribute("filter").equals("")) {
                    filter = "(&" + filter + elem.getAttribute("filter") + ")";
                }
            }
            ServiceImporter si = new ServiceImporter(spec, filter, agg, opt, m_manager.getContext(), m_manager.getServiceContext(), PolicyServiceContext.LOCAL, null, ih);
            ih.getRequirements().add(si);
            return true;
        }
        
        if (imp.isAggregate() && !agg) {
            getManager().getFactory().getLogger().log(Logger.ERROR, "[" + getManager().getInstanceName() + "] The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
            return false;
        }
      
        if (elem.containsAttribute("filter")) {
            String filter = elem.getAttribute("filter");
            String filter2 = imp.getFilter();
            if (filter2 == null || !filter2.equalsIgnoreCase(filter)) {
                getManager().getFactory().getLogger().log(Logger.ERROR, "[" + getManager().getInstanceName() + "] The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
                return false;
            }
        }
        
        return true;
    }

    public HandlerDescription getDescription() {
        return new ProvidedServiceHandlerDescription(true, m_managedServices);
    }
    
    /**
     * Build available instance types.
     */
    private void computeAvailableTypes() {
        InstanceHandler ih = (InstanceHandler) m_manager.getCompositeHandler(InstanceHandler.class.getName());       
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
