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
package org.apache.felix.ipojo.handlers.providedservice;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.IPojoConfiguration;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.metadata.Attribute;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.osgi.framework.Bundle;

/**
 * Composite Provided Service Handler.
 * This handler manage the service providing for a composition.
 * @author <a href="mailto:dev@felix.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandler extends PrimitiveHandler {

    /**
     * The list of the provided service.
     */
    private ProvidedService[] m_providedServices = new ProvidedService[0];

    /**
     * Add a provided service to the list .
     * 
     * @param ps : the provided service to add
     */
    private void addProvidedService(ProvidedService ps) {
        // Verify that the provided service is not already in the array.
        for (int i = 0; (m_providedServices != null) && (i < m_providedServices.length); i++) {
            if (m_providedServices[i] == ps) { return; }
        }

        if (m_providedServices.length > 0) {
            ProvidedService[] newPS = new ProvidedService[m_providedServices.length + 1];
            System.arraycopy(m_providedServices, 0, newPS, 0, m_providedServices.length);
            newPS[m_providedServices.length] = ps;
            m_providedServices = newPS;
        } else {
            m_providedServices = new ProvidedService[] { ps };
        }
    }

    /**
     * Get the array of provided service.
     * @return the list of the provided service.
     */
    public ProvidedService[] getProvidedService() {
        return m_providedServices;
    }

    /**
     * Configure the handler.
     * @param componentMetadata : the component type metadata
     * @param configuration : the instance configuration
     * @throws ConfigurationException : the metadata are not correct.
     * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.InstanceManager, org.apache.felix.ipojo.metadata.Element, java.util.Dictionary)
     */
    public void configure(Element componentMetadata, Dictionary configuration) throws ConfigurationException {
        m_providedServices = new ProvidedService[0];
        // Create the dependency according to the component metadata
        Element[] providedServices = componentMetadata.getElements("Provides");
        List fields = new ArrayList();
        for (int i = 0; i < providedServices.length; i++) {
            String[] serviceSpecifications = ParseUtils.parseArrays(providedServices[i].getAttribute("interface")); // Set by the initialize component factory.
            
            // Get the factory policy
            int factory = ProvidedService.SINGLETON_FACTORY;
            String fact = providedServices[i].getAttribute("factory");
            if (fact != null && "service".equalsIgnoreCase(fact)) {
                factory = ProvidedService.SERVICE_FACTORY;
            }

            // Then create the provided service
            ProvidedService ps = new ProvidedService(this, serviceSpecifications, factory);

            Element[] props = providedServices[i].getElements("Property");
            Property[] properties = new Property[props.length];
            for (int j = 0; j < props.length; j++) {
                String name = props[j].getAttribute("name");
                String value = props[j].getAttribute("value");
                String type = props[j].getAttribute("type");
                String field = props[j].getAttribute("field");

                if (name != null && configuration.get(name) != null && configuration.get(name) instanceof String) {
                    value = (String) configuration.get(name);
                } else {
                    if (field != null && configuration.get(field) != null && configuration.get(field) instanceof String) {
                        value = (String) configuration.get(field);
                    }
                }

                Property prop = new Property(ps, name, field, type, value);
                properties[j] = prop;
                
                // Check if the instance configuration has a value for this property
                Object o = configuration.get(prop.getName()); 
                if (o != null && ! (o instanceof String)) {
                    prop.set(o);
                } else {
                    if (field != null) {
                        o = configuration.get(field);
                        if (o != null && !(o instanceof String)) {
                            prop.set(configuration.get(field));
                        }
                    }
                }
                
                if (field != null) {
                    fields.add(new FieldMetadata(field, type));
                }
            }

            // Attached to properties to the provided service
            ps.setProperties(properties);

            if (checkProvidedService(ps)) {
                addProvidedService(ps);
            } else {
                String itfs = "";
                for (int j = 0; j < serviceSpecifications.length; j++) {
                    itfs = itfs + " " + serviceSpecifications[j];
                }
                throw new ConfigurationException("[" + getInstanceManager().getInstanceName() + "] The provided service" + itfs + " is not valid");                
            }

        }
        getInstanceManager().register(this, (FieldMetadata[]) fields.toArray(new FieldMetadata[fields.size()]), null);
    }
    
    /**
     * Collect interfaces implemented by the POJO.
     * @param specs : implemented interfaces.
     * @param parent : parent class.
     * @param bundle : Bundle object.
     * @return the set of implemented interfaces.
     * @throws ClassNotFoundException : occurs when an interface cannot be loaded.
     */
    private Set computeInterfaces(String[] specs, String parent, Bundle bundle) throws ClassNotFoundException {
        Set result = new HashSet();
        // First iterate on found specification in manipulation metadata
        for (int i = 0; i < specs.length; i++) {
            result.add(specs[i]);
            // Iterate on interfaces implemented by the current interface
            Class clazz = bundle.loadClass(specs[i]);
            collectInterfaces(clazz, result, bundle);
        }

        // Look for parent class.
        if (parent != null) {
            Class cl = bundle.loadClass(parent);
            collectInterfacesFromClass(cl, result, bundle);
        }

        return result;
    }
    
    /**
     * Look for inherited interfaces.
     * @param clazz : interface name to explore (class object)
     * @param l : set (accumulator)
     * @param b : bundle
     * @throws ClassNotFoundException : occurs when an interface cannot be loaded.
     */
    private void collectInterfaces(Class clazz, Set l, Bundle b) throws ClassNotFoundException {
        Class[] clazzes = clazz.getInterfaces();
        for (int i = 0; i < clazzes.length; i++) {
            l.add(clazzes[i].getName());
            collectInterfaces(clazzes[i], l, b);
        }
    }
    
    /**
     * Collect interfaces for the given class.
     * This method explores super class to.
     * @param cl : class object.
     * @param l : set of implemented interface (accumulator)
     * @param b : bundle.
     * @throws ClassNotFoundException : occurs if an interface cannot be load.
     */
    private void collectInterfacesFromClass(Class cl, Set l, Bundle b) throws ClassNotFoundException {
        Class[] clazzes = cl.getInterfaces();
        for (int i = 0; i < clazzes.length; i++) {
            l.add(clazzes[i].getName());
            collectInterfaces(clazzes[i], l, b);
        }
        // Iterate on parent classes
        Class sup = cl.getSuperclass();
        if (sup != null) {
            collectInterfacesFromClass(sup, l, b);
        }
    }

    /**
     * Check the provided service given in argument in the sense that the metadata are consistent.
     * @param ps : the provided service to check.
     * @return true if the provided service is correct
     * @throws ConfigurationException : the checked provided service is not correct.
     */
    private boolean checkProvidedService(ProvidedService ps) throws ConfigurationException {        
        for (int i = 0; i < ps.getServiceSpecification().length; i++) {
            String specName = ps.getServiceSpecification()[i];
            
            // Check service level dependencies
            try {
                Class spec = getInstanceManager().getFactory().loadClass(specName);
                Field specField = spec.getField("specification");
                Object o = specField.get(null);
                if (o instanceof String) {
                    Element specification = ManifestMetadataParser.parse((String) o);
                    Element[] deps = specification.getElements("requires");
                    for (int j = 0; j < deps.length; j++) {
                        Dependency d = getAttachedDependency(deps[j]);
                        if (d != null) {
                            // Fix service-level dependency flag
                            d.setServiceLevelDependency();
                        }
                        isDependencyCorrect(d, deps[j]);
                    }
                } else {
                    throw new ConfigurationException("Provides  : The specification field of the service specification " + ps.getServiceSpecification()[i] + " need to be a String");
                }
            } catch (NoSuchFieldException e) {
                return true; // No specification field
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("Provides  : The service specification " + ps.getServiceSpecification()[i] + " cannot be load");
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Provides  : The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " is not accessible : " + e.getMessage());
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Provides  : The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " is not accessible : " + e.getMessage());
            } catch (ParseException e) {
                throw new ConfigurationException("Provides  :  The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " does not contain a valid String : " + e.getMessage());
            }
        }

        return true;
    }

    /**
     * Look for the implementation (i.e. component) dependency for the given service-level requirement metadata.
     * @param element : the service-level requirement metadata
     * @return the Dependency object, null if not found or if the DependencyHandler is not plugged to the instance
     */
    private Dependency getAttachedDependency(Element element) {
        DependencyHandler dh = (DependencyHandler) getHandler(IPojoConfiguration.IPOJO_NAMESPACE + ":requires");
        if (dh == null) { return null; }

        String id = element.getAttribute("id");
        if (id != null) {
            // Look for dependency Id
            for (int i = 0; i < dh.getDependencies().length; i++) {
                if (dh.getDependencies()[i].getId().equals(id)) { return dh.getDependencies()[i]; }
            }
        }
        // If not found or no id, look for a dependency with the same specification
        String requirement = element.getAttribute("specification");
        for (int i = 0; i < dh.getDependencies().length; i++) {
            if (dh.getDependencies()[i].getSpecification().equals(requirement)) { return dh.getDependencies()[i]; }
        }

        return null;
    }

    /**
     * Check the correctness of the implementation dependency against the service level dependency.
     * @param dep : dependency to check
     * @param elem : service-level dependency metadata
     * @throws ConfigurationException  : the service level dependency and the implementation dependency does not match.
     */
    private void isDependencyCorrect(Dependency dep, Element elem) throws ConfigurationException {
        String optional = elem.getAttribute("optional");
        boolean opt = optional != null && optional.equalsIgnoreCase("true");

        String aggregate = elem.getAttribute("aggregate");
        boolean agg = aggregate != null && aggregate.equalsIgnoreCase("true");

        if (dep == null && !opt) {
            throw new ConfigurationException("Provides  :  The requirement " + elem.getAttribute("specification") + " is not present in the implementation and is declared as a mandatory service-level requirement");
        }

        if (dep != null && dep.isAggregate() && !agg) {
            throw new ConfigurationException("Provides  :  The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
        }

        String filter = elem.getAttribute("filter");
        if (dep != null && filter != null) {
            String filter2 = dep.getFilter();
            if (filter2 == null || !filter2.equalsIgnoreCase(filter)) {
                throw new ConfigurationException("Provides  :  The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
            }
        }
    }

    /**
     * Stop the provided service handler.
     * 
     * @see org.apache.felix.ipojo.Handler#stop()
     */
    public void stop() {
    }

    /**
     * Start the provided service handler.
     * 
     * @see org.apache.felix.ipojo.Handler#start()
     */
    public void start() { }

    /**
     * Setter Callback Method.
     * Check if the modified field is a property to update the value.
     * @param fieldName : field name
     * @param value : new value
     * @see org.apache.felix.ipojo.Handler#setterCallback(java.lang.String,
     * java.lang.Object)
     */
    public void setterCallback(String fieldName, Object value) {
        // Verify that the field name correspond to a dependency
        for (int i = 0; i < m_providedServices.length; i++) {
            ProvidedService ps = m_providedServices[i];
            boolean update = false;
            for (int j = 0; j < ps.getProperties().length; j++) {
                Property prop = ps.getProperties()[j];
                if (fieldName.equals(prop.getField())) {
                    // it is the associated property
                    prop.set(value);
                    update = true;
                }
            }
            if (update) {
                ps.update();
            }
        }
        // Else do nothing
    }

    /**
     * Getter Callback Method.
     * Check if the field is a property to push the stored value.
     * @param fieldName : field name
     * @param value : value pushed by the previous handler
     * @return the stored value or the previous value.
     * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String,
     * java.lang.Object)
     */
    public Object getterCallback(String fieldName, Object value) {
        for (int i = 0; i < m_providedServices.length; i++) {
            ProvidedService ps = m_providedServices[i];
            for (int j = 0; j < ps.getProperties().length; j++) {
                Property prop = ps.getProperties()[j];
                if (fieldName.equals(prop.getField())) {
                    // it is the associated property
                    return prop.get();
                }
            }
        }
        // Else it is not a property
        return value;
    }

    /**
     * Register the services if the new state is VALID. Unregister the services
     * if the new state is UNRESOLVED.
     * 
     * @param state : the new instance state.
     * @see org.apache.felix.ipojo.CompositeHandler#stateChanged(int)
     */
    public void stateChanged(int state) {
        // If the new state is INVALID => unregister all the services
        if (state == InstanceManager.INVALID) {
            for (int i = 0; i < m_providedServices.length; i++) {
                m_providedServices[i].unregisterService();
            }
            return;
        }

        // If the new state is VALID => register all the services
        if (state == InstanceManager.VALID) {
            for (int i = 0; i < m_providedServices.length; i++) {
                m_providedServices[i].registerService();
            }
            return;
        }
    }

    /**
     * Add properties to all provided services.
     * @param dict : dictionary of properties to add
     */
    public void addProperties(Dictionary dict) {
        for (int i = 0; i < m_providedServices.length; i++) {
            m_providedServices[i].addProperties(dict);
            m_providedServices[i].update();
        }
    }

    /**
     * Remove properties form all provided services.
     * 
     * @param dict : dictionary of properties to delete.
     */
    public void removeProperties(Dictionary dict) {
        for (int i = 0; i < m_providedServices.length; i++) {
            m_providedServices[i].deleteProperties(dict);
            m_providedServices[i].update();
        }
    }

    /**
     * Build the provided service description.
     * @return the handler description.
     * @see org.apache.felix.ipojo.Handler#getDescription()
     */
    public HandlerDescription getDescription() {
        ProvidedServiceHandlerDescription pshd = new ProvidedServiceHandlerDescription(this);

        for (int j = 0; j < getProvidedService().length; j++) {
            ProvidedService ps = getProvidedService()[j];
            ProvidedServiceDescription psd = new ProvidedServiceDescription(ps.getServiceSpecification(), ps.getState(), ps.getServiceReference());

            Properties props = new Properties();
            for (int k = 0; k < ps.getProperties().length; k++) {
                Property prop = ps.getProperties()[k];
                if (prop.getValue() != null) {
                    props.put(prop.getName(), prop.getValue().toString());
                }
            }
            psd.setProperty(props);
            pshd.addProvidedService(psd);
        }
        return pshd;
    }

    /**
     * Reconfigure provided service.
     * @param dict : the new instance configuration.
     * @see org.apache.felix.ipojo.Handler#reconfigure(java.util.Dictionary)
     */
    public void reconfigure(Dictionary dict) {
        for (int j = 0; j < getProvidedService().length; j++) {
            ProvidedService ps = getProvidedService()[j];
            Property[] props = ps.getProperties();
            boolean update = false;
            for (int k = 0; k < props.length; k++) {
                if (dict.get(props[k].getName()) != null) {
                    update = true;
                    if (dict.get(props[k].getName()) instanceof String) {
                        props[k].set((String) dict.get(props[k].getName()));
                    } else {
                        props[k].set(dict.get(props[k].getName()));
                    }
                }
            }
            if (update) {
                ps.update();
            }
        }
    }

    /**
     * Initialize the component type.
     * @param cd : component type description to populate.
     * @param metadata : component type metadata.
     * @throws ConfigurationException : occurs when the POJO does not implement any interfaces.
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) throws ConfigurationException {
        // Change ComponentInfo
        Element[] provides = metadata.getElements("provides");
        ManipulationMetadata manipulation = new ManipulationMetadata(metadata);

        for (int i = 0; i < provides.length; i++) {
         // First : create the serviceSpecification list
            String[] serviceSpecification = manipulation.getInterfaces();
            String parent = manipulation.getSuperClass();
            Set all = null;
            try {
                all = computeInterfaces(serviceSpecification, parent, cd.getBundleContext().getBundle());
            } catch (ClassNotFoundException e) {
                throw new ConfigurationException("A interface cannot be loaded : " + e.getMessage());
            }
            
            String serviceSpecificationStr = provides[i].getAttribute("interface");
            if (serviceSpecificationStr != null) {
                List itfs = ParseUtils.parseArraysAsList(serviceSpecificationStr);
                for (int j = 0; j < itfs.size(); j++) {
                    if (! all.contains(itfs.get(j))) {
                        throw new ConfigurationException("The specification " + itfs.get(j) + " is not implemented by " + cd.getClassName());
                    }
                }
                all = new HashSet(itfs);
            }
            
            if (all.size() == 0) {
                throw new ConfigurationException("Provides  : Cannot instantiate a provided service : no specifications found (no interfaces implemented by the pojo)");
            }

            String specs = null;
            Set set = new HashSet(all);
            Iterator it = set.iterator(); 
            while (it.hasNext()) {
                String sp = (String) it.next();
                cd.addProvidedServiceSpecification(sp);
                if (specs == null) {
                    specs = "{" + sp;
                } else {
                    specs += "," + sp;
                }
            }
            
            specs += "}";
            
            provides[i].addAttribute(new Attribute("interface", specs)); // Add interface attribute to avoid checking in the configure method

            Element[] props = provides[i].getElements("property");
            for (int j = 0; j < props.length; j++) {
                String name = props[j].getAttribute("name");
                String value = props[j].getAttribute("value");
                String type = props[j].getAttribute("type");
                String field = props[j].getAttribute("field");

                // Get property name :
                if (field != null && name == null) {
                    name = field;
                }

                // Check type if not already set
                if (type == null) {
                    if (field == null) {
                        throw new ConfigurationException("The property " + name + " has neither type neither field.");
                    }
                    FieldMetadata fm = manipulation.getField(field);
                    if (fm == null) {
                        throw new ConfigurationException("A declared property was not found in the class : " + field);
                    }
                    type = fm.getFieldType();
                    props[j].addAttribute(new Attribute("type", type));
                }

                cd.addProperty(new PropertyDescription(name, type, value));
            }
        }
    }

}
