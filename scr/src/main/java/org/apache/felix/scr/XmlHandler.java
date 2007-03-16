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

import java.util.Properties;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.parser.KXml2SAXHandler;
import org.apache.felix.scr.parser.ParseException;

/**
 * 
 * 
 */
public class XmlHandler implements KXml2SAXHandler {

    // A reference to the current component
    private ComponentMetadata m_currentComponent = null;

    // The current service
    private ServiceMetadata m_currentService = null;
    
    // A list of component descriptors contained in the file
    private List m_components = new ArrayList();

    // PropertyMetaData whose value attribute is missing, hence has element data
    private PropertyMetadata m_pendingProperty;
    
    /**
     * Method called when a tag opens
     *
     * @param   uri
     * @param   localName
     * @param   qName
     * @param   attrib
     * @exception   ParseException
    **/
    public void startElement(String uri,String localName,String qName,Properties attrib) throws ParseException
    {
    	try {
    		
	    	// 112.4.3 Component Element
	        if (qName.equals("component")) {

	        	// Create a new ComponentMetadata
	        	m_currentComponent = new ComponentMetadata();

	        	// name attribute is mandatory
	        	m_currentComponent.setName(attrib.getProperty("name"));
	        	
	        	// enabled attribute is optional
	        	if(attrib.getProperty("enabled") != null) {
	        		m_currentComponent.setEnabled(attrib.getProperty("enabled").equals("true"));
	        	}

	        	// immediate attribute is optional
	        	if(attrib.getProperty("immediate") != null) {
	        		m_currentComponent.setImmediate(attrib.getProperty("immediate").equals("true"));
	        	}
	        	
	        	// factory attribute is optional
	        	if(attrib.getProperty("factory") != null) {
	        		m_currentComponent.setFactoryIdentifier(attrib.getProperty("factory"));
	        	}
		        	
	        	// Add this component to the list
	            m_components.add(m_currentComponent);
	        }
	        
	        // 112.4.4 Implementation
	        else if (qName.equals("implementation"))
	        {
	        	// Set the implementation class name (mandatory)
	        	m_currentComponent.setImplementationClassName(attrib.getProperty("class"));
	        }
	        // 112.4.5 Properties and Property Elements
	        else if (qName.equals("property")) {
                PropertyMetadata prop = new PropertyMetadata();
                
                // name attribute is mandatory
                prop.setName(attrib.getProperty("name"));
                
                // type attribute is optional
                if(attrib.getProperty("type") != null) {
                    prop.setType(attrib.getProperty("type"));
                }

                // 112.4.5: If the value attribute is specified, the body of the element is ignored.
	        	if( attrib.getProperty("value") != null) {
        			prop.setValue(attrib.getProperty("value"));
	            	m_currentComponent.addProperty(prop);
	        	}
	        	else {
	        		// hold the metadata pending
                    m_pendingProperty = prop;
	        	}
	        	// TODO: treat the case where a properties file name is provided (p. 292)
	        }
	        else if(qName.equals("properties")) {
	        	// TODO: implement the properties tag
	        }
	        // 112.4.6 Service Element
	        else if (qName.equals("service")) {
	        	
	        	m_currentService = new ServiceMetadata();
	        	
	        	// servicefactory attribute is optional
	        	if(attrib.getProperty("servicefactory") != null) {
	        		m_currentService.setServiceFactory(attrib.getProperty("servicefactory").equals("true"));
	        	}      	
	        	
	            m_currentComponent.setService(m_currentService);
	        }
	        else if (qName.equals("provide")) {
	            m_currentService.addProvide(attrib.getProperty("interface"));
	        }
	        
	        // 112.4.7 Reference element
	        else if (qName.equals("reference")) {
	            ReferenceMetadata ref=new ReferenceMetadata ();
	            ref.setName(attrib.getProperty("name"));
	            ref.setInterface(attrib.getProperty("interface"));
	            
	            // Cardinality 
	            if(attrib.getProperty("cardinality")!= null) {
	            	ref.setCardinality(attrib.getProperty("cardinality"));
	            }
	            
	            if(attrib.getProperty("policy") != null) {
	            	ref.setPolicy(attrib.getProperty("policy"));
	            }
	            
	            //if
	            ref.setTarget(attrib.getProperty("target"));
	            ref.setBind(attrib.getProperty("bind"));
	            ref.setUnbind(attrib.getProperty("unbind"));
	
	            m_currentComponent.addDependency(ref);
	        }
    	}
    	catch(Exception ex) {
    		ex.printStackTrace();
    		throw new ParseException("Exception during parsing",ex);
    	}
    }

    /**
    * Method called when a tag closes
    *
    * @param   uri
    * @param   localName
    * @param   qName
    * @exception   ParseException
    */
    public void endElement(java.lang.String uri,java.lang.String localName,java.lang.String qName) throws ParseException
    {
        if (qName.equals("component"))
        {
        	// When the closing tag for a component is found, the component is validated to check if 
        	// the implementation class has been set
        	m_currentComponent.validate();
        } else if (qName.equals("property") && m_pendingProperty != null) {
            // 112.4.5 body expected to contain property value
            // if so, the m_pendingProperty field would be null
            // currently, we just ignore this situation
            m_pendingProperty = null;
        }
    }

    /**
    * Called to retrieve the service descriptors
    *
    * @return   A list of service descriptors
    */
    List getComponentMetadataList()
    {
        return m_components;
    }

	public void characters( char[] ch, int offset, int length ) throws Exception
    {
        // 112.4.5 If the value attribute is not specified, the body must contain one or more values
        if ( m_pendingProperty != null )
        {
            m_pendingProperty.setValues( new String( ch, offset, length ) );
            m_currentComponent.addProperty( m_pendingProperty );
            m_pendingProperty = null;
        }
    }

	public void processingInstruction(String target, String data) throws Exception {
		// Not used
		
	}

	public void setLineNumber(int lineNumber) {
		// Not used
		
	}

	public void setColumnNumber(int columnNumber) {
		// Not used
		
	}
}

