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
import java.util.Dictionary;
import java.util.Properties;

import org.apache.felix.ipojo.ConfigurationException;
import org.apache.felix.ipojo.IPojoConfiguration;
import org.apache.felix.ipojo.InstanceManager;
import org.apache.felix.ipojo.PrimitiveHandler;
import org.apache.felix.ipojo.architecture.ComponentDescription;
import org.apache.felix.ipojo.architecture.HandlerDescription;
import org.apache.felix.ipojo.architecture.PropertyDescription;
import org.apache.felix.ipojo.handlers.dependency.Dependency;
import org.apache.felix.ipojo.handlers.dependency.DependencyHandler;
import org.apache.felix.ipojo.metadata.Element;
import org.apache.felix.ipojo.parser.FieldMetadata;
import org.apache.felix.ipojo.parser.ManifestMetadataParser;
import org.apache.felix.ipojo.parser.ManipulationMetadata;
import org.apache.felix.ipojo.parser.ParseException;
import org.apache.felix.ipojo.parser.ParseUtils;
import org.apache.felix.ipojo.util.Logger;

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

        ManipulationMetadata manipulation = new ManipulationMetadata(componentMetadata);

        m_providedServices = new ProvidedService[0];
        // Create the dependency according to the component metadata
        Element[] providedServices = componentMetadata.getElements("Provides");
        for (int i = 0; i < providedServices.length; i++) {
            // Create a ProvidedServiceMetadata object

            // First : create the serviceSpecification array
            String[] serviceSpecification = new String[0];
            if (providedServices[i].containsAttribute("interface")) {
                String serviceSpecificationStr = providedServices[i].getAttribute("interface");
                serviceSpecification = ParseUtils.parseArrays(serviceSpecificationStr);
            } else {
                serviceSpecification = manipulation.getInterfaces();
            }
            if (serviceSpecification.length == 0) {
                log(Logger.ERROR, "Cannot instantiate a provided service : no specifications found (no interfaces implemented by the pojo)");
                throw new ConfigurationException("Provides  : Cannot instantiate a provided service : no specifications found (no interfaces implemented by the pojo)", getInstanceManager().getFactory().getName());
            }

            // Get the factory policy
            int factory = ProvidedService.SINGLETON_FACTORY;
            if (providedServices[i].containsAttribute("factory") && "service".equals(providedServices[i].getAttribute("factory"))) {
                factory = ProvidedService.SERVICE_FACTORY;
            }

            // Then create the provided service
            ProvidedService ps = new ProvidedService(this, serviceSpecification, factory);

            Element[] props = providedServices[i].getElements("Property");
            Property[] properties = new Property[props.length];
            for (int j = 0; j < props.length; j++) {
                String name = null;
                if (props[j].containsAttribute("name")) {
                    name = props[j].getAttribute("name");
                }
                String value = null;
                if (props[j].containsAttribute("value")) {
                    value = props[j].getAttribute("value");
                }
                String type = null;
                if (props[j].containsAttribute("type")) {
                    type = props[j].getAttribute("type");
                }
                String field = null;
                if (props[j].containsAttribute("field")) {
                    field = props[j].getAttribute("field");
                }

                if (name != null && configuration.get(name) != null && configuration.get(name) instanceof String) {
                    value = (String) configuration.get(name);
                } else {
                    if (field != null && configuration.get(field) != null && configuration.get(field) instanceof String) {
                        value = (String) configuration.get(field);
                    }
                }

                Property prop = new Property(ps, name, field, type, value, manipulation);
                properties[j] = prop;
            }

            // Attached to properties to the provided service
            ps.setProperties(properties);

            if (checkProvidedService(ps, manipulation)) {
                addProvidedService(ps);
            } else {
                String itfs = "";
                for (int j = 0; j < serviceSpecification.length; j++) {
                    itfs = itfs + " " + serviceSpecification[j];
                }
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The provided service" + itfs + " is not valid");
                return;
            }

        }

        if (providedServices.length > 0) {
            FieldMetadata[] fields = new FieldMetadata[0];
            for (int i = 0; i < m_providedServices.length; i++) {
                ProvidedService ps = m_providedServices[i];
                for (int j = 0; j < ps.getProperties().length; j++) {
                    Property prop = ps.getProperties()[j];

                    // Check if the instance configuration has a value for this
                    // property
                    if (prop.getName() != null && configuration.get(prop.getName()) != null && !(configuration.get(prop.getName()) instanceof String)) {
                        prop.set(configuration.get(prop.getName()));
                    } else {
                        if (prop.getField() != null && configuration.get(prop.getField()) != null && !(configuration.get(prop.getField()) instanceof String)) {
                            prop.set(configuration.get(prop.getField()));
                        }
                    }
                    if (prop.getField() != null) {
                        FieldMetadata[] newFields = new FieldMetadata[fields.length + 1];
                        System.arraycopy(fields, 0, newFields, 0, fields.length);
                        newFields[fields.length] = manipulation.getField(prop.getField());
                        fields = newFields;
                    }
                }
            }
            getInstanceManager().register(this, fields, null);
        }
    }

    /**
     * Check the provided service given in argument in the sense that the
     * metadata are consistent.
     * 
     * @param ps : the provided service to check.
     * @param manipulation : component-type manipulation metadata.
     * @return true if the provided service is correct
     * @throws ConfigurationException : the checked provided service is not correct.
     */
    private boolean checkProvidedService(ProvidedService ps, ManipulationMetadata manipulation) throws ConfigurationException {
        for (int i = 0; i < ps.getServiceSpecification().length; i++) {
            // Check the implementation of the specification
            if (!manipulation.isInterfaceImplemented(ps.getServiceSpecification()[i])) {
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The service specification " + ps.getServiceSpecification()[i] + " is not implemented by the component class");
                throw new ConfigurationException("Provides  : The service specification " + ps.getServiceSpecification()[i] + " is not implemented by the component class", getInstanceManager().getFactory().getName());

            }

            // Check service level dependencies
            try {
                Class spec = getInstanceManager().getFactory().loadClass(ps.getServiceSpecification()[i]);
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
                    log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The specification field of the service specification " + ps.getServiceSpecification()[i] + " need to be a String");
                    throw new ConfigurationException("Provides  : The specification field of the service specification " + ps.getServiceSpecification()[i] + " need to be a String", getInstanceManager().getFactory().getName());
                }
            } catch (NoSuchFieldException e) {
                return true; // No specification field
            } catch (ClassNotFoundException e) {
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The service specification " + ps.getServiceSpecification()[i] + " cannot be load");
                throw new ConfigurationException("Provides  : The service specification " + ps.getServiceSpecification()[i] + " cannot be load", getInstanceManager().getFactory().getName());
            } catch (IllegalArgumentException e) {
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " is not accessible : " + e.getMessage());
                throw new ConfigurationException("Provides  : The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " is not accessible : " + e.getMessage(), getInstanceManager().getFactory().getName());
            } catch (IllegalAccessException e) {
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " is not accessible : " + e.getMessage());
                throw new ConfigurationException("Provides  : The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " is not accessible : " + e.getMessage(), getInstanceManager().getFactory().getName());
            } catch (ParseException e) {
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " does not contain a valid String : " + e.getMessage());
                throw new ConfigurationException("Provides  :  The field 'specification' of the service specification " + ps.getServiceSpecification()[i] + " does not contain a valid String : " + e.getMessage(), getInstanceManager().getFactory()
                        .getName());
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

        if (element.containsAttribute("id")) {
            // Look for dependency Id
            String id = element.getAttribute("id");
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
        boolean opt = false;
        if (elem.containsAttribute("optional") && elem.getAttribute("optional").equalsIgnoreCase("true")) {
            opt = true;
        }

        boolean agg = false;
        if (elem.containsAttribute("aggregate") && elem.getAttribute("aggregate").equalsIgnoreCase("true")) {
            agg = true;
        }

        if (dep == null && !opt) {
            log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The requirement " + elem.getAttribute("specification") + " is not present in the implementation and is declared as a mandatory service-level requirement");
            throw new ConfigurationException("Provides  :  The requirement " + elem.getAttribute("specification") + " is not present in the implementation and is declared as a mandatory service-level requirement", getInstanceManager().getFactory()
                    .getName());
        }

        if (dep != null && dep.isAggregate() && !agg) {
            log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement");
            throw new ConfigurationException("Provides  :  The requirement " + elem.getAttribute("specification") + " is aggregate in the implementation and is declared as a simple service-level requirement", getInstanceManager().getFactory()
                    .getName());
        }

        if (dep != null && elem.containsAttribute("filter")) {
            String filter = elem.getAttribute("filter");
            String filter2 = dep.getFilter();
            if (filter2 == null || !filter2.equalsIgnoreCase(filter)) {
                log(Logger.ERROR, "[" + getInstanceManager().getInstanceName() + "] The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement");
                throw new ConfigurationException("Provides  :  The specification requirement " + elem.getAttribute("specification") + " as not the same filter as declared in the service-level requirement", getInstanceManager().getFactory().getName());
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
    public void start() {
    }

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
     * @see org.apache.felix.ipojo.Handler#initializeComponentFactory(org.apache.felix.ipojo.architecture.ComponentDescription, org.apache.felix.ipojo.metadata.Element)
     */
    public void initializeComponentFactory(ComponentDescription cd, Element metadata) {
        // Change ComponentInfo
        Element[] provides = metadata.getElements("provides");
        ManipulationMetadata mm = new ManipulationMetadata(metadata);

        for (int i = 0; i < provides.length; i++) {
            String[] serviceSpecification = new String[0];
            if (provides[i].containsAttribute("interface")) {
                String serviceSpecificationStr = provides[i].getAttribute("interface");
                serviceSpecification = ParseUtils.parseArrays(serviceSpecificationStr);
            } else {
                serviceSpecification = mm.getInterfaces();
            }
            if (serviceSpecification.length == 0) {
                log(Logger.ERROR, "Cannot instantiate a provided service : no specifications found (no interfaces implemented by the pojo)");
                return;
            }

            for (int j = 0; j < serviceSpecification.length; j++) {
                cd.addProvidedServiceSpecification(serviceSpecification[j]);
            }

            Element[] props = provides[i].getElements("property");
            for (int j = 0; j < props.length; j++) {
                String name = null;
                if (props[j].containsAttribute("name")) {
                    name = props[j].getAttribute("name");
                }
                String value = null;
                if (props[j].containsAttribute("value")) {
                    value = props[j].getAttribute("value");
                }
                String type = null;
                if (props[j].containsAttribute("type")) {
                    type = props[j].getAttribute("type");
                }
                String field = null;
                if (props[j].containsAttribute("field")) {
                    field = props[j].getAttribute("field");
                }

                // Get property name :
                if (field != null && name == null) {
                    name = field;
                }

                // Check type if not already set
                if (type == null) {
                    if (field == null) {
                        System.err.println("The property " + name + " has neither type neither field.");
                        return;
                    }
                    FieldMetadata fm = mm.getField(field);
                    if (fm == null) {
                        System.err.println("A declared property was not found in the class : " + field);
                        return;
                    }
                    type = fm.getFieldType();
                }

                cd.addProperty(new PropertyDescription(name, type, value));
            }
        }
    }

}
