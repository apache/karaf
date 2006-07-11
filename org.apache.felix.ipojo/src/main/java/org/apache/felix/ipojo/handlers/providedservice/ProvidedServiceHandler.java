/*
 *   Copyright 2006 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.felix.ipojo.handlers.providedservice;

import java.util.Dictionary;
import java.util.logging.Level;

import org.apache.felix.ipojo.ComponentManager;
import org.apache.felix.ipojo.Handler;
import org.apache.felix.ipojo.Activator;
import org.apache.felix.ipojo.metadata.Element;

/**
 * @author <a href="mailto:felix-dev@incubator.apache.org">Felix Project Team</a>
 */
public class ProvidedServiceHandler implements Handler {


	/**
	 * The list of the provided service.
	 */
	private ProvidedService[] m_providedServices = new ProvidedService[0];

	/**
	 * The component manager.
	 */
	private ComponentManager m_componentManager;

	private void addProvidedService(ProvidedService ps) {
	        //  Verify that the provided service is not already in the array.
	        for (int i = 0; (m_providedServices != null) && (i < m_providedServices.length); i++) {
	            if (m_providedServices[i] == ps) { return; }
	        }

	        if (m_providedServices.length > 0) {
	            ProvidedService[] newPS = new ProvidedService[m_providedServices.length + 1];
	            System.arraycopy(m_providedServices, 0, newPS, 0, m_providedServices.length);
	            newPS[m_providedServices.length] = ps;
	            m_providedServices = newPS;
	        }
	        else { m_providedServices = new ProvidedService[] {ps}; }
	}

	/**
	 * @return the component manager.
	 */
	public ComponentManager getComponentManager() { return m_componentManager; }

	/**
	 * @return the list of the provided service.
	 */
	public ProvidedService[] getProvidedService() { return m_providedServices; }

	/**
	 * @see org.apache.felix.ipojo.Handler#configure(org.apache.felix.ipojo.ComponentManager, org.apache.felix.ipojo.metadata.Element)
	 */
	public void configure(ComponentManager cm, Element componentMetadata) {
		// Fix the component managert & clean the provided service list
		m_componentManager = cm;
		m_providedServices = new ProvidedService[0];
		// Create the dependency according to the component metadata
		Element[] providedServices = componentMetadata.getElements("Provides");
		for (int i = 0; i < providedServices.length; i++) {
			// Create a ProvidedServiceMetadata object

            // First : create the serviceSpecification array
            String[] serviceSpecification = new String[0];
            if (providedServices[i].containsAttribute("interface")) {
                String serviceSpecificationStr = providedServices[i].getAttribute("interface");
                //Get serviceSpecification if exist in the metadata
                String[] spec = serviceSpecificationStr.split(",");
                serviceSpecification = new String[spec.length];
                for (int j = 0; j < spec.length; j++) { serviceSpecification[j] = spec[j].trim(); }
            } else {
            	Element manipulation = m_componentManager.getComponentMetatada().getMetadata().getElements("Manipulation")[0];
            	serviceSpecification = new String[manipulation.getElements("Interface").length];
            	for (int j = 0; j < manipulation.getElements("Interface").length; j++) {
            		serviceSpecification[j] = manipulation.getElements("Interface")[j].getAttribute("name");
            	}
            }

            // Get the factory policy
			int factory = ProvidedServiceMetadata.SINGLETON_FACTORY;
			if (providedServices[i].containsAttribute("factory") && providedServices[i].getAttribute("factory").equals("service")) { factory = ProvidedService.SERVICE_FACTORY; }

			// Then create the provided service metadata
			ProvidedServiceMetadata psm = new ProvidedServiceMetadata(serviceSpecification, factory);

			// Create properties
			Element[] dynamicProps = providedServices[i].getElements("DynamicProperty");
			Element[] staticProps = providedServices[i].getElements("StaticProperty");
			Element[] props = providedServices[i].getElements("Property");
			for (int j = 0; j < dynamicProps.length; j++) {
				Activator.getLogger().log(Level.WARNING, "[" + m_componentManager.getComponentMetatada().getClassName() + "] Please use property instead of dynamic property");
				String name = null;
				if (dynamicProps[j].containsAttribute("name")) { name = dynamicProps[j].getAttribute("name"); }
				String field = dynamicProps[j].getAttribute("field");
				String value = null;
				if (dynamicProps[j].containsAttribute("value")) { value = dynamicProps[j].getAttribute("value"); }
				String type = null;
				if (dynamicProps[j].containsAttribute("type")) { type = dynamicProps[j].getAttribute("type"); }
				PropertyMetadata pm = new PropertyMetadata(name, field, type, value);
				psm.addProperty(pm);
			}
			for (int j = 0; j < staticProps.length; j++) {
				Activator.getLogger().log(Level.WARNING, "[" + m_componentManager.getComponentMetatada().getClassName() + "] Please use property instead of static property");
				String name = staticProps[j].getAttribute("name");
				String value = staticProps[j].getAttribute("value");
				String type = staticProps[j].getAttribute("type");
				PropertyMetadata pm = new PropertyMetadata(name, null, type, value);
				psm.addProperty(pm);
			}
			for (int j = 0; j < props.length; j++) {
				String name = null;
				if (props[j].containsAttribute("name")) { name = props[j].getAttribute("name"); }
				String value = null;
				if (props[j].containsAttribute("value")) { value = props[j].getAttribute("value"); }
				String type = null;
				if (props[j].containsAttribute("type")) { type = props[j].getAttribute("type"); }
				String field = null;
				if (props[j].containsAttribute("field")) { field = props[j].getAttribute("field"); }
				PropertyMetadata pm = new PropertyMetadata(name, field, type, value);
				psm.addProperty(pm);
			}

			// Create the provided service object
			ProvidedService ps = new ProvidedService(this, psm);
			if (checkProvidedService(ps)) { addProvidedService(ps); }
			else {
                String itfs = "";
                for (int j = 0; j < serviceSpecification.length; j++) {
                    itfs = itfs + " " + serviceSpecification[j];
                }
				Activator.getLogger().log(Level.SEVERE, "[" + m_componentManager.getComponentMetatada().getClassName() + "] The provided service" + itfs + " is not valid, it will be removed");
				ps = null;
			}
		}

		if (providedServices.length > 0) { m_componentManager.register(this); }
	}

	private boolean containsInterface(String s) {
		Element manipulation = m_componentManager.getComponentMetatada().getMetadata().getElements("Manipulation")[0];
		for (int i = 0; i < manipulation.getElements("Interface").length; i++) {
			if (manipulation.getElements("Interface")[i].getAttribute("name").equals(s)) {
				return true;
			}
		}
		return false;
	}

	private boolean checkProvidedService(ProvidedService ps) {

        for (int i = 0; i < ps.getMetadata().getServiceSpecification().length; i++) {
            if (!containsInterface(ps.getMetadata().getServiceSpecification()[i])) {
                Activator.getLogger().log(Level.SEVERE, "[" + m_componentManager.getComponentMetatada().getClassName() + "] The service specification " + ps.getMetadata().getServiceSpecification()[i] + " is not implemented by the component class");
                return false;
            }
        }

		// Fix internal property type
		for (int i = 0; i < ps.getProperties().length; i++) {
			Property prop = ps.getProperties()[i];
			String field = prop.getMetadata().getField();

			if (field == null) {
				// Static dependency -> Nothing to check
				return true;
			} else {
				Element manipulation = getComponentManager().getComponentMetatada().getMetadata().getElements("Manipulation")[0];
	        	String type = null;
	        	for (int j = 0; j < manipulation.getElements("Field").length; j++) {
	        		if (field.equals(manipulation.getElements("Field")[j].getAttribute("name"))) {
	        			type = manipulation.getElements("Field")[j].getAttribute("type");
	        			break;
	        		}
	        	}
				if (type == null) {
					Activator.getLogger().log(Level.SEVERE, "[" + m_componentManager.getComponentMetatada().getClassName() + "] A declared property was not found in the class : " + prop.getMetadata().getField());
					return false;
				}

				if (type != null) {
					if (type.endsWith("[]")) {
						// TODO array property
						Activator.getLogger().log(Level.SEVERE, "[" + m_componentManager.getComponentMetatada().getClassName() + "] An array property was found in the class [Not implemented yet] : " + prop.getMetadata().getField());
						return false;
					}

					if (prop.getMetadata().getType() == null) { prop.getMetadata().setType(type); }

					if (!prop.getMetadata().getType().equals(type)) {
						Activator.getLogger().log(Level.WARNING, "[" + m_componentManager.getComponentMetatada().getClassName() + "] The field type [" + type + "] and the declared type [" + prop.getMetadata().getType() + "] are not the same for " + prop.getMetadata().getField());
						prop.getMetadata().setType(type);
					}
				}
				else {
					Activator.getLogger().log(Level.WARNING, "[" + m_componentManager.getComponentMetatada().getClassName() + "] The declared property " + prop.getMetadata().getField() + "  does not exist in the code");
				}
			}
		}
		return true;
	}

	/**
	 * Stop the provided service handler : unregister all provided services.
	 * @see org.apache.felix.ipojo.Handler#stop()
	 */
	public void stop() {
		for (int i = 0; i < m_providedServices.length; i++) {
			m_providedServices[i].unregisterService();
		}
	}

	/**
	 * Start the provided service handler : register the service if the component is resolved.
	 * Else do nothing and whait for a component state change event
	 * @see org.apache.felix.ipojo.Handler#start()
	 */
	public void start() {
		Activator.getLogger().log(Level.INFO, "[" + m_componentManager.getComponentMetatada().getClassName() + "] Start the provided service handler");
			for (int i = 0; (m_componentManager.getState() == ComponentManager.VALID) && i < m_providedServices.length; i++) {
				m_providedServices[i].registerService();
			}
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#setterCallback(java.lang.String, java.lang.Object)
	 */
	public void setterCallback(String fieldName, Object value) {
		// Verify that the field name coreespond to a dependency
		for (int i = 0; i < m_providedServices.length; i++) {
			ProvidedService ps = m_providedServices[i];
			for (int j = 0; j < ps.getProperties().length; j++) {
				Property prop = ps.getProperties()[j];
				if (fieldName.equals(prop.getMetadata().getField())) {
					// it is the associated property
					prop.set(value);
				}
			}
		}
		//Else do nothing
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#getterCallback(java.lang.String, java.lang.Object)
	 */
	public Object getterCallback(String fieldName, Object value) {
		for (int i = 0; i < m_providedServices.length; i++) {
			ProvidedService ps = m_providedServices[i];
			for (int j = 0; j < ps.getProperties().length; j++) {
				Property prop = ps.getProperties()[j];
				if (fieldName.equals(prop.getMetadata().getField())) {
					// it is the associated property
					return prop.get();
				}
			}
		}
		// Else it is not a property
		return value;
	}

	/**
	 * @see org.apache.felix.ipojo.Handler#isValid()
	 */
	public boolean isValid() {
		// The provided service handler does not need to manipulate the field
		// Return always true
		return true;
	}

	/**
	 * Register the services if the new state is VALID.
	 * Unregister the services if the new state is UNRESOLVED.
	 * @see org.apache.felix.ipojo.Handler#stateChanged(int)
	 */
	public void stateChanged(int state) {
		// If the new state is UNRESOLVED => unregister all the services
		if (state == ComponentManager.INVALID) {
			stop();
			return;
		}

		// If the new state is VALID => regiter all the services
		if (state == ComponentManager.VALID) {
			start();
			return;
		}

	}

	/**
	 * Add properties to all provided services.
	 * @param dict : dictionary fo properties to add
	 */
	public void addProperties(Dictionary dict) {
		for (int i = 0; i < m_providedServices.length; i++) {
			m_providedServices[i].addProperties(dict);
		}
	}

	/**
	 * Remove properties form all provided services.
	 * @param dict : dictionary of properties to delete.
	 */
	public void removeProperties(Dictionary dict) {
		for (int i = 0; i < m_providedServices.length; i++) {
			m_providedServices[i].deleteProperties(dict);
		}
	}

}
