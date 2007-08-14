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

    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

    // A reference to the current component
    private ComponentMetadata m_currentComponent;

    // The current service
    private ServiceMetadata m_currentService;

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
    public void startElement(String uri, String localName, String qName, Properties attrib)
    throws ParseException {
        // we process elements in the default namespace and in the scr namespace only
        // TODO - To be 100% correct we should only listen to the scr namespace
        if ( "".equals(uri) || NAMESPACE_URI.equals(uri) ) {
        	try {

    	    	// 112.4.3 Component Element
    	        if (localName.equals("component")) {

    	        	// Create a new ComponentMetadata
    	        	this.m_currentComponent = new ComponentMetadata();

    	        	// name attribute is mandatory
    	        	this.m_currentComponent.setName(attrib.getProperty("name"));

    	        	// enabled attribute is optional
    	        	if(attrib.getProperty("enabled") != null) {
    	        		this.m_currentComponent.setEnabled(attrib.getProperty("enabled").equals("true"));
    	        	}

    	        	// immediate attribute is optional
    	        	if(attrib.getProperty("immediate") != null) {
    	        		this.m_currentComponent.setImmediate(attrib.getProperty("immediate").equals("true"));
    	        	}

    	        	// factory attribute is optional
    	        	if(attrib.getProperty("factory") != null) {
    	        		this.m_currentComponent.setFactoryIdentifier(attrib.getProperty("factory"));
    	        	}

    	        	// Add this component to the list
    	            this.m_components.add(this.m_currentComponent);
    	        }

    	        // 112.4.4 Implementation
    	        else if (localName.equals("implementation"))
    	        {
    	        	// Set the implementation class name (mandatory)
    	        	this.m_currentComponent.setImplementationClassName(attrib.getProperty("class"));
    	        }
    	        // 112.4.5 Properties and Property Elements
    	        else if (localName.equals("property")) {
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
    	            	this.m_currentComponent.addProperty(prop);
    	        	}
    	        	else {
    	        		// hold the metadata pending
                        this.m_pendingProperty = prop;
    	        	}
    	        	// TODO: treat the case where a properties file name is provided (p. 292)
    	        }
    	        else if(localName.equals("properties")) {
    	        	// TODO: implement the properties tag
    	        }
    	        // 112.4.6 Service Element
    	        else if (localName.equals("service")) {

    	        	this.m_currentService = new ServiceMetadata();

    	        	// servicefactory attribute is optional
    	        	if(attrib.getProperty("servicefactory") != null) {
    	        		this.m_currentService.setServiceFactory(attrib.getProperty("servicefactory").equals("true"));
    	        	}

    	            this.m_currentComponent.setService(this.m_currentService);
    	        }
    	        else if (localName.equals("provide")) {
    	            this.m_currentService.addProvide(attrib.getProperty("interface"));
    	        }

    	        // 112.4.7 Reference element
    	        else if (localName.equals("reference")) {
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

    	            this.m_currentComponent.addDependency(ref);
    	        }
        	}
        	catch(Exception ex) {
        		ex.printStackTrace();
        		throw new ParseException("Exception during parsing",ex);
        	}
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
    public void endElement(String uri, String localName, String qName) throws ParseException
    {
        // we process elements in the default namespace and in the scr namespace only
        // TODO - To be 100% correct we should only listen to the scr namespace
        if ( "".equals(uri) || NAMESPACE_URI.equals(uri) ) {
            if (localName.equals("component"))
            {
            	// When the closing tag for a component is found, the component is validated to check if
            	// the implementation class has been set
            	this.m_currentComponent.validate();
            } else if (localName.equals("property") && this.m_pendingProperty != null) {
                // 112.4.5 body expected to contain property value
                // if so, the m_pendingProperty field would be null
                // currently, we just ignore this situation
                this.m_pendingProperty = null;
            }
        }
    }

    /**
    * Called to retrieve the service descriptors
    *
    * @return   A list of service descriptors
    */
    List getComponentMetadataList()
    {
        return this.m_components;
    }

	public void characters( char[] ch, int offset, int length ) throws Exception
    {
        // 112.4.5 If the value attribute is not specified, the body must contain one or more values
        if ( this.m_pendingProperty != null )
        {
            this.m_pendingProperty.setValues( new String( ch, offset, length ) );
            this.m_currentComponent.addProperty( this.m_pendingProperty );
            this.m_pendingProperty = null;
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

