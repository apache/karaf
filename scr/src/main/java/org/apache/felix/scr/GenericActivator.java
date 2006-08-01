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
package org.apache.felix.scr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import java.io.InputStream;

import org.apache.felix.scr.parser.KXml2SAXParser;
import org.apache.felix.scr.parser.ParseException;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentException;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * The GenericActivator is the main startup class. It will read information from the metadata.xml file
 * descriptors and create the corresponding managers.
 *
 */
public class GenericActivator implements BundleActivator
{	
	// The bundle context
    private BundleContext m_context = null;
    
    // This is a list of component instance managers that belong to a particular bundle
    private List m_managers = new ArrayList();

    // Flag that sets tracing messages
    private static boolean m_trace = true;
    
    // Flag that sets error messages
    private static boolean m_error = true;

    // A string containing the version number
    private static String m_version = "1.0.0 (12012006)";

    // Registry of component names
    static Set m_componentNames = new HashSet();

    // Static initializations based on system properties
    static {
        // Get system properties to see if traces or errors need to be displayed
        String result = System.getProperty("ds.showtrace");
        if(result != null && result.equals("true"))
        {
            m_trace = true;
        }
        result = System.getProperty("ds.showerrors");
        if(result != null && result.equals("false"))
        {
            m_error = false;
        }
        result = System.getProperty("ds.showversion");
        if(result != null && result.equals("true"))
        {
            System.out.println("[ Version = "+m_version+" ]\n");
        }        
    }

    /**
    * Called upon starting of the bundle. This method invokes initialize() which
    * parses the metadata and creates the instance managers
    *
    * @param   context  The bundle context passed by the framework
    * @exception   Exception any exception thrown from initialize
    */
    public void start(BundleContext context) throws Exception
    {
    	// Stores the context
        m_context = context;
        
        try
        {
            initialize();
        }
        catch (Exception e)
        {
            GenericActivator.error("GenericActivator : in bundle ["
                + context.getBundle().getBundleId() + "] : " + e);
            e.printStackTrace();
            throw e;
        }
    }

    /**
    * Gets the MetaData location, parses the meta data and requests the processing
    * of binder instances
    *
    * @throws FileNotFoundException if the metadata file is not found
    * @throws ParseException if the metadata file is not found
    */
    private void initialize() throws ParseException {
        // Get the Metadata-Location value from the manifest
        String descriptorLocations = (String) m_context.getBundle().getHeaders().get("Service-Component");

        if (descriptorLocations == null)
        {
            throw new ComponentException("Service-Component entry not found in the manifest");
        }

        // 112.4.1: The value of the the header is a comma separated list of XML entries within the Bundle 
		StringTokenizer st = new StringTokenizer(descriptorLocations, ", ");
        
		while (st.hasMoreTokens()) {
			String descriptorLocation = st.nextToken();
			
			try {
				InputStream stream = m_context.getBundle().getResource(descriptorLocation).openStream();

				BufferedReader in = new BufferedReader(new InputStreamReader(stream)); 
	            XmlHandler handler = new XmlHandler(); 
	            KXml2SAXParser parser;

	            parser = new KXml2SAXParser(in); 

		        parser.parseXML(handler);

		        // 112.4.2 Component descriptors may contain a single, root component element
		        // or one or more component elements embedded in a larger document
		        Iterator i = handler.getComponentMetadataList().iterator();
		        while (i.hasNext()) {
                	try {
    		            ComponentMetadata metadata = (ComponentMetadata) i.next();
    		            
    		            validate(metadata);
    		        	
    	                // Request creation of the component manager
    	                ComponentManager manager = ManagerFactory.createManager(this,metadata);
                		
                		if(metadata.isFactory()) {
                			// 112.2.4 SCR must register a Component Factory service on behalf ot the component
                			// as soon as the component factory is satisfied
                		}
                		else if(metadata.isEnabled()) {
		                	// enable the component
		                	manager.enable();
		                	m_managers.add(manager);
		                }
	                } catch (Exception e) {
						// There is a problem with this particular component, we'll log the error
	                	// and proceed to the next one
	                	// TODO: log the error
						e.printStackTrace();
					} 
		        }
		        
		        stream.close();

			}
			catch ( IOException ex ) {
				// 112.4.1 If an XML document specified by the header cannot be located in the bundle and its attached
				// fragments, SCR must log an error message with the Log Service, if present, and continue.
				
				error("Component descriptor entry '" + descriptorLocation	+ "' not found");
			}
			catch (Exception ex) {
				ex.printStackTrace();
			}
		}
    }

    /**
    * Stop method that destroys all the instance managers
    *
    * @param   context The Bundle Context passed by the framework
    * @exception Exception any exception thrown during destruction of the instance managers
    */
    public void stop(BundleContext context) throws java.lang.Exception
    {
        //GenericActivator.trace("GenericActivator : Bundle ["+context.getBundle().getBundleId()+"] will destroy "+m_managers.size()+" instances");

        while (m_managers.size() !=0 )
        {
            ComponentManager manager = (ComponentManager)m_managers.get(0);
            try {
                manager.dispose();
                m_managers.remove(manager);
            }
            catch(Exception e) {
                GenericActivator.error("GenericActivator : Exception during invalidate : "+e);
                e.printStackTrace();
            }
            finally {
            	m_componentNames.remove(manager.getComponentMetadata().getName());
            }
            
        }

        m_context = null;

        //GenericActivator.trace("GenericActivator : Bundle ["+context.getBundle().getBundleId()+"] STOPPED");
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
     * Method to display traces
     *
     * @param message a string to be displayed
     * @param metadata ComponentMetadata associated to the message (can be null)
    **/
    static void trace(String message, ComponentMetadata metadata)
    {
        if(m_trace)
        {
            System.out.print("--- ");
            if(metadata != null) {
            	System.out.print("[");
            	System.out.print(metadata.getName());
            	System.out.print("] ");
            	System.out.println(message);
            }
        }
    }

    /**
     * Method to display errors
     *
     * @param s a string to be displayed
    **/
    static void error(String s) {
        if(m_error) {
            System.err.println("### "+s);
        }
    }

    /**
     * Method to display exceptions
     *
     * @param ex an exception
    **/   
    static void exception(String message, ComponentMetadata metadata, Throwable ex) {
    	 if(m_error) {
    		 System.out.println("--- Exception in component "+metadata.getName()+" : "+message+" ---");
             ex.printStackTrace();
         }   	
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
    void validate(ComponentMetadata component) throws ComponentException {

    	if(m_componentNames.contains(component.getName()))
    	{
    		throw new ComponentException("The component name '"+component.getName()+"' has already been registered.");
    	}
    	
    	m_componentNames.add(component.getName());
    
    	component.validate();
    	
    	trace("Validated and registered component",component);
    }
}
