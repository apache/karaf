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
package org.apache.felix.scr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.naming.ConfigurationException;

import org.apache.felix.scr.parser.KXml2SAXParser;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * The BundleComponentActivator is helper class to load and unload Components of
 * a single bundle. It will read information from the metadata.xml file
 * descriptors and create the corresponding managers.
 */
class BundleComponentActivator
{	
    // global component registration
    private ComponentRegistry m_componentRegistry;
    
	// The bundle context owning the registered component
    private BundleContext m_context = null;
    
    // This is a list of component instance managers that belong to a particular bundle
    private List m_managers = new ArrayList();

    // The Configuration Admin tracker providing configuration for components
    private ServiceTracker m_configurationAdmin;

    /**
     * Called upon starting of the bundle. This method invokes initialize() which
     * parses the metadata and creates the instance managers
     *
     * @param componentRegistry The <code>ComponentRegistry</code> used to
     *      register components with to ensure uniqueness of component names
     *      and to ensure configuration updates.
     * @param   context  The bundle context owning the components
     * 
     * @throws ComponentException if any error occurrs initializing this class
     */
    BundleComponentActivator(ComponentRegistry componentRegistry, BundleContext context) throws ComponentException
    {
        // The global "Component" registry
        this.m_componentRegistry = componentRegistry;
        
    	// Stores the context
        m_context = context;
        
        // have the Configuration Admin Service handy (if available)
        m_configurationAdmin = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        m_configurationAdmin.open();
        
        // Get the Metadata-Location value from the manifest
        String descriptorLocations =
            (String) m_context.getBundle().getHeaders().get("Service-Component");
        if (descriptorLocations == null)
        {
            throw new ComponentException("Service-Component entry not found in the manifest");
        }

        initialize(descriptorLocations);
    }

    /**
     * Gets the MetaData location, parses the meta data and requests the processing
     * of binder instances
     * 
     * @param descriptorLocations A comma separated list of locations of
     *      component descriptors. This must not be <code>null</code>.
     *      
     * @throws IllegalStateException If the bundle has already been uninstalled.
     */
    private void initialize(String descriptorLocations) {

        // 112.4.1: The value of the the header is a comma separated list of XML entries within the Bundle 
		StringTokenizer st = new StringTokenizer(descriptorLocations, ", ");
        
		while (st.hasMoreTokens()) {
			String descriptorLocation = st.nextToken();
			
            URL descriptorURL = m_context.getBundle().getResource(descriptorLocation);
            if (descriptorURL == null)
            {
                // 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
                // fragments, SCR must log an error message with the Log Service, if present, and continue.
                Activator.error( "Component descriptor entry '" + descriptorLocation + "' not found", null );
                continue;
            }

            InputStream stream = null;
			try {
				stream = descriptorURL.openStream();

				BufferedReader in = new BufferedReader(new InputStreamReader(stream)); 
	            XmlHandler handler = new XmlHandler(); 
	            KXml2SAXParser parser;

	            parser = new KXml2SAXParser(in); 

		        parser.parseXML(handler);

		        // 112.4.2 Component descriptors may contain a single, root component element
		        // or one or more component elements embedded in a larger document
		        Iterator i = handler.getComponentMetadataList().iterator();
		        while (i.hasNext()) {
                    ComponentMetadata metadata = (ComponentMetadata) i.next();
                	try
                    {
                        // validate the component metadata
    		            validate(metadata);
    		        	
    	                // Request creation of the component manager
    	                ComponentManager manager;
                        
                        if (metadata.isFactory()) {
                            // 112.2.4 SCR must register a Component Factory service on behalf ot the component
                            // as soon as the component factory is satisfied
                            manager = new ComponentFactoryImpl(this, metadata, m_componentRegistry);
                        } else {
                            manager = ManagerFactory.createManager( this, metadata, m_componentRegistry
                            .createComponentId() );
                        }
                		
                        // register the component after validation
                        m_componentRegistry.registerComponent( metadata.getName(), manager );
                        
                        // enable the component
                		if(metadata.isEnabled())
                        {
		                	manager.enable();
		                }

                		// register the manager
                		m_managers.add(manager);
                    }
                    catch (Exception e)
                    {
						// There is a problem with this particular component, we'll log the error
	                	// and proceed to the next one
                        Activator.exception("Cannot register Component", metadata, e);
					} 
		        }
			}
			catch ( IOException ex )
            {
				// 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
				// fragments, SCR must log an error message with the Log Service, if present, and continue.
				
				Activator.exception("Problem reading descriptor entry '"
                    + descriptorLocation + "'", null, ex);
			}
			catch (Exception ex)
            {
                Activator.exception("General problem with descriptor entry '"
                    + descriptorLocation + "'", null, ex);
			}
            finally
            {
                if ( stream != null )
                {
                    try
                    {
                        stream.close();
                    }
                    catch ( IOException ignore )
                    {
                    }
                }
            }
		}
    }

    /**
    * Dispose of this component activator instance and all the component
    * managers.
    */
    void dispose()
    {
        if (m_context == null) {
            return;
        }

        Activator.trace("BundleComponentActivator : Bundle ["
            + m_context.getBundle().getBundleId() + "] will destroy "
            + m_managers.size() + " instances", null);

        while (m_managers.size() !=0 )
        {
            ComponentManager manager = (ComponentManager) m_managers.get(0);
            try
            {
                m_managers.remove(manager);
                manager.dispose();
            }
            catch(Exception e)
            {
                Activator.exception("BundleComponentActivator : Exception invalidating",
                    manager.getComponentMetadata(), e);
            }
            finally
            {
                m_componentRegistry.unregisterComponent( manager.getComponentMetadata().getName() );
            }
            
        }

        // close the Configuration Admin tracker
        if (m_configurationAdmin != null) {
            m_configurationAdmin.close();
        }
        
        Activator.trace("BundleComponentActivator : Bundle ["
            + m_context.getBundle().getBundleId() + "] STOPPED", null);

        m_context = null;
    }

   /**
    * Returns the list of instance references currently associated to this activator
    *
    * @return the list of instance references
    */
    protected List getInstanceReferences()
    {
        return m_managers;
    }

    /**
    * Returns the BundleContext
    *
    * @return the BundleContext
    */
    protected BundleContext getBundleContext()
    {
        return m_context;
    }

    /**
     * Returns the <code>ConfigurationAdmin</code> service used to retrieve
     * configuration data for components managed by this activator or
     * <code>null</code> if no Configuration Admin Service is available in the
     * framework.
     */
    protected ConfigurationAdmin getConfigurationAdmin() {
        return (ConfigurationAdmin) m_configurationAdmin.getService();
    }
    
    /**
     * Implements the <code>ComponentContext.enableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * then starting a thread to actually enable all components found.
     * <p>
     * If no component matching the given name is found the thread is not
     * started and the method does nothing. 
     * 
     * @param name The name of the component to enable or <code>null</code> to
     *      enable all components.
     */
    void enableComponent(String name)
    {
        final ComponentManager[] cm = getSelectedComponents(name);
        if (cm == null)
        {
            return;
        }
        
        Thread enabler = new Thread("Component Enabling") 
        {
            public void run()
            {
                for (int i=0; i < cm.length; i++)
                {
                    try
                    {
                        cm[i].enable();
                    }
                    catch (Throwable t) 
                    {
                        Activator.exception( "Cannot enable component", cm[i].getComponentMetadata(), t );
                    }
                }
            }
        };
        enabler.start();
    }
    
    /**
     * Implements the <code>ComponentContext.disableComponent(String)</code>
     * method by first finding the component(s) for the <code>name</code> and
     * then starting a thread to actually disable all components found.
     * <p>
     * If no component matching the given name is found the thread is not
     * started and the method does nothing. 
     * 
     * @param name The name of the component to disable or <code>null</code> to
     *      disable all components.
     */
    void disableComponent(String name)
    {
        final ComponentManager[] cm = getSelectedComponents(name);
        if (cm == null)
        {
            return;
        }
        
        Thread disabler = new Thread("Component Disabling")
        {
            public void run()
            {
                for (int i=0; i < cm.length; i++)
                {
                    try
                    {
                        cm[i].disable();
                    }
                    catch (Throwable t)
                    {
                        Activator.exception("Cannot disable component",
                            cm[i].getComponentMetadata(), t);
                    }
                }
            }
        };
        disabler.start();
    }
    
    /**
     * Returns an array of {@link ComponentManager} instances which match the
     * <code>name</code>. If the <code>name</code> is <code>null</code> an
     * array of all currently known component managers is returned. Otherwise
     * an array containing a single component manager matching the name is
     * returned if one is registered. Finally, if no component manager with the
     * given name is registered, <code>null</code> is returned.
     *  
     * @param name The name of the component manager to return or
     *      <code>null</code> to return an array of all component managers.
     *      
     * @return An array containing one or more component managers according
     *      to the <code>name</code> parameter or <code>null</code> if no
     *      component manager with the given name is currently registered.
     */
    private ComponentManager[] getSelectedComponents(String name) {
        // if all components are selected
        if (name == null)
        {
            return (ComponentManager[]) m_managers.toArray(new ComponentManager[m_managers.size()]);
        }
        
        if ( m_componentRegistry.getComponent( name ) != null )
        {
            // otherwise just find it
            Iterator it = m_managers.iterator();
            while (it.hasNext())
            {
                ComponentManager cm = (ComponentManager) it.next();
                if (name.equals(cm.getComponentMetadata().getName())) {
                    return new ComponentManager[]{ cm  };
                }
            }
        }
        
        // if the component is not known
        return null;
    }

    /**
     * This method is used to validate that the component. This method verifies multiple things:
     * 
     * 1.- That the name attribute is set and is globally unique
     * 2.- That an implementation class name has been set 
     * 3.- That a delayed component provides a service and is not specified to be a factory
     * - That the serviceFactory attribute for the provided service is not true if the component is a factory or immediate
     * 
     * If the component is valid, its name is registered
     * 
     * @throws A ComponentException if something is not right
     */
    void validate(ComponentMetadata component) throws ComponentException
    {

        m_componentRegistry.checkComponentName( component.getName() );
    	
        try
        {
            component.validate();
        }
        catch ( ComponentException ce )
        {
            // remove the reservation before leaving
            m_componentRegistry.unregisterComponent( component.getName() );
            throw ce;
        }

        Activator.trace( "Validated and registered component", component );
    }
}
