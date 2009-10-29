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
package org.apache.felix.scr.impl.metadata;


import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.felix.scr.impl.helper.Logger;
import org.apache.felix.scr.impl.parser.KXml2SAXHandler;
import org.apache.felix.scr.impl.parser.ParseException;
import org.osgi.framework.Bundle;
import org.osgi.service.log.LogService;


/**
 *
 *
 */
public class XmlHandler implements KXml2SAXHandler
{

    // Empty Namespace URI maps to DS 1.0
    public static final String NAMESPACE_URI_EMPTY = "";

    // Namespace URI of DS 1.0
    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

    // Namespace URI of DS 1.1
    public static final String NAMESPACE_URI_1_1 = "http://www.osgi.org/xmlns/scr/v1.1.0";

    // namespace code for non-DS namespace
    public static final int DS_VERSION_NONE = -1;

    // namespace code for the DS 1.0 specification
    public static final int DS_VERSION_1_0 = 0;

    // namespace code for the DS 1.0 specification
    public static final int DS_VERSION_1_1 = 1;

    // mapping of namespace URI to namespace code
    private static final Map NAMESPACE_CODE_MAP;

    // the bundle containing the XML resource being parsed
    private final Bundle m_bundle;

    // logger for any messages
    private final Logger m_logger;

    // A reference to the current component
    private ComponentMetadata m_currentComponent;

    // The current service
    private ServiceMetadata m_currentService;

    // A list of component descriptors contained in the file
    private List m_components = new ArrayList();

    // PropertyMetaData whose value attribute is missing, hence has element data
    private PropertyMetadata m_pendingProperty;

    /** Flag for detecting the first element. */
    protected boolean firstElement = true;

    /** Override namespace. */
    protected String overrideNamespace;

    /** Flag for elements inside a component element */
    protected boolean isComponent = false;

    static
    {
        NAMESPACE_CODE_MAP = new HashMap();
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_EMPTY, new Integer( DS_VERSION_1_0 ) );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI, new Integer( DS_VERSION_1_0 ) );
        NAMESPACE_CODE_MAP.put( NAMESPACE_URI_1_1, new Integer( DS_VERSION_1_1 ) );
    }


    // creates an instance with the bundle owning the component descriptor
    // file parsed by this instance
    public XmlHandler( Bundle bundle, Logger logger )
    {
        m_bundle = bundle;
        m_logger = logger;
    }


    /**
    * Called to retrieve the service descriptors
    *
    * @return   A list of service descriptors
    */
    public List getComponentMetadataList()
    {
        return m_components;
    }


    /**
     * Method called when a tag opens
     *
     * @param   uri
     * @param   localName
     * @param   attrib
     * @exception   ParseException
    **/
    public void startElement( String uri, String localName, Properties attrib ) throws ParseException
    {
        // according to the spec, the elements should have the namespace,
        // except when the root element is the "component" element
        // So we check this for the first element, we receive.
        if ( firstElement )
        {
            firstElement = false;
            if ( localName.equals( "component" ) && "".equals( uri ) )
            {
                overrideNamespace = NAMESPACE_URI;
            }
        }

        if ( overrideNamespace != null && "".equals( uri ) )
        {
            uri = overrideNamespace;
        }

        // FELIX-695: however the spec also states that the inner elements
        // of a component are unqualified, so they don't have
        // the namespace - we allow both: with or without namespace!
        if ( this.isComponent && "".equals(uri) )
        {
            uri = NAMESPACE_URI;
        }

        // get the namespace code for the namespace uri
        Integer namespaceCode = (Integer) NAMESPACE_CODE_MAP.get( uri );
        // from now on uri points to the namespace
        if ( namespaceCode != null )
        {
            try
            {

                // 112.4.3 Component Element
                if ( localName.equals( "component" ) )
                {
                    this.isComponent = true;

                    // Create a new ComponentMetadata
                    m_currentComponent = new ComponentMetadata( namespaceCode.intValue() );

                    // name attribute is optional (since DS 1.1)
                    if ( attrib.getProperty( "name" ) != null )
                    {
                        m_currentComponent.setName( attrib.getProperty( "name" ) );
                    }

                    // enabled attribute is optional
                    if ( attrib.getProperty( "enabled" ) != null )
                    {
                        m_currentComponent.setEnabled( attrib.getProperty( "enabled" ).equals( "true" ) );
                    }

                    // immediate attribute is optional
                    if ( attrib.getProperty( "immediate" ) != null )
                    {
                        m_currentComponent.setImmediate( attrib.getProperty( "immediate" ).equals( "true" ) );
                    }

                    // factory attribute is optional
                    if ( attrib.getProperty( "factory" ) != null )
                    {
                        m_currentComponent.setFactoryIdentifier( attrib.getProperty( "factory" ) );
                    }

                    // configuration-policy is optional (since DS 1.1)
                    if ( attrib.getProperty( "configuration-policy" ) != null )
                    {
                        m_currentComponent.setConfigurationPolicy( attrib.getProperty( "configuration-policy" ) );
                    }

                    // activate attribute is optional (since DS 1.1)
                    if ( attrib.getProperty( "activate" ) != null )
                    {
                        m_currentComponent.setActivate( attrib.getProperty( "activate" ) );
                    }

                    // deactivate attribute is optional (since DS 1.1)
                    if ( attrib.getProperty( "deactivate" ) != null )
                    {
                        m_currentComponent.setDeactivate( attrib.getProperty( "deactivate" ) );
                    }

                    // modified attribute is optional (since DS 1.1)
                    if ( attrib.getProperty( "modified" ) != null )
                    {
                        m_currentComponent.setModified( attrib.getProperty( "modified" ) );
                    }

                    // Add this component to the list
                    m_components.add( m_currentComponent );
                }

                // 112.4.4 Implementation
                else if ( localName.equals( "implementation" ) )
                {
                    // Set the implementation class name (mandatory)
                    m_currentComponent.setImplementationClassName( attrib.getProperty( "class" ) );
                }
                // 112.4.5 [...] Property Elements
                else if ( localName.equals( "property" ) )
                {
                    PropertyMetadata prop = new PropertyMetadata();

                    // name attribute is mandatory
                    prop.setName( attrib.getProperty( "name" ) );

                    // type attribute is optional
                    if ( attrib.getProperty( "type" ) != null )
                    {
                        prop.setType( attrib.getProperty( "type" ) );
                    }

                    // 112.4.5: If the value attribute is specified, the body of the element is ignored.
                    if ( attrib.getProperty( "value" ) != null )
                    {
                        prop.setValue( attrib.getProperty( "value" ) );
                        m_currentComponent.addProperty( prop );
                    }
                    else
                    {
                        // hold the metadata pending
                        m_pendingProperty = prop;
                    }
                }
                // 112.4.5 Properties [...] Elements
                else if ( localName.equals( "properties" ) )
                {
                    readPropertiesEntry( attrib.getProperty( "entry" ) );
                }
                // 112.4.6 Service Element
                else if ( localName.equals( "service" ) )
                {

                    m_currentService = new ServiceMetadata();

                    // servicefactory attribute is optional
                    if ( attrib.getProperty( "servicefactory" ) != null )
                    {
                        m_currentService.setServiceFactory( attrib.getProperty( "servicefactory" ).equals( "true" ) );
                    }

                    m_currentComponent.setService( m_currentService );
                }
                else if ( localName.equals( "provide" ) )
                {
                    m_currentService.addProvide( attrib.getProperty( "interface" ) );
                }

                // 112.4.7 Reference element
                else if ( localName.equals( "reference" ) )
                {
                    ReferenceMetadata ref = new ReferenceMetadata();

                    // name attribute is optional (since DS 1.1)
                    if ( attrib.getProperty( "name" ) != null )
                    {
                        ref.setName( attrib.getProperty( "name" ) );
                    }

                    ref.setInterface( attrib.getProperty( "interface" ) );

                    // Cardinality
                    if ( attrib.getProperty( "cardinality" ) != null )
                    {
                        ref.setCardinality( attrib.getProperty( "cardinality" ) );
                    }

                    if ( attrib.getProperty( "policy" ) != null )
                    {
                        ref.setPolicy( attrib.getProperty( "policy" ) );
                    }

                    //if
                    ref.setTarget( attrib.getProperty( "target" ) );
                    ref.setBind( attrib.getProperty( "bind" ) );
                    ref.setUnbind( attrib.getProperty( "unbind" ) );

                    m_currentComponent.addDependency( ref );
                }

                // unexpected element (except the root element "components"
                // used by the Maven SCR Plugin, which is just silently ignored)
                else if ( !localName.equals( "components" ) )
                {
                    m_logger.log( LogService.LOG_DEBUG, "Ignoring unsupported element {0} (bundle {1})", new Object[]
                        { localName, m_bundle.getLocation() }, null, null );
                }
            }
            catch ( Exception ex )
            {
                ex.printStackTrace();
                throw new ParseException( "Exception during parsing", ex );
            }
        }

        // unexpected namespace (except the root element "components"
        // used by the Maven SCR Plugin, which is just silently ignored)
        else if ( !localName.equals( "components" ) )
        {
            m_logger.log( LogService.LOG_DEBUG, "Ignoring unsupported element '{'{0}'}'{1} (bundle {2})", new Object[]
                { uri, localName, m_bundle.getLocation() }, null, null );
        }
    }


    /**
    * Method called when a tag closes
    *
    * @param   uri
    * @param   localName
    */
    public void endElement( String uri, String localName )
    {
        if ( overrideNamespace != null && "".equals( uri ) )
        {
            uri = overrideNamespace;
        }

        if ( this.isComponent && "".equals(uri) )
        {
            uri = NAMESPACE_URI;
        }

        if ( NAMESPACE_URI.equals( uri ) )
        {
            if ( localName.equals( "component" ) )
            {
                this.isComponent = false;
            }
            else if ( localName.equals( "property" ) && m_pendingProperty != null )
            {
                // 112.4.5 body expected to contain property value
                // if so, the m_pendingProperty field would be null
                // currently, we just ignore this situation
                m_pendingProperty = null;
            }
        }
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#characters(java.lang.String)
     */
    public void characters( String text )
    {
        // 112.4.5 If the value attribute is not specified, the body must contain one or more values
        if ( m_pendingProperty != null )
        {
            m_pendingProperty.setValues( text );
            m_currentComponent.addProperty( m_pendingProperty );
            m_pendingProperty = null;
        }
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#processingInstruction(java.lang.String, java.lang.String)
     */
    public void processingInstruction( String target, String data )
    {
        // Not used
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#setLineNumber(int)
     */
    public void setLineNumber( int lineNumber )
    {
        // Not used
    }


    /**
     * @see org.apache.felix.scr.impl.parser.KXml2SAXHandler#setColumnNumber(int)
     */
    public void setColumnNumber( int columnNumber )
    {
        // Not used
    }


    /**
     * Reads the name property file from the bundle owning this descriptor. All
     * properties read from the properties file are added to the current
     * component's property meta data list.
     *
     * @param entryName The name of the bundle entry containing the propertes
     *      to be added. This must not be <code>null</code>.
     *
     * @throws ParseException If the entry name is <code>null</code> or no
     *      entry with the given name exists in the bundle or an error occurrs
     *      reading the properties file.
     */
    private void readPropertiesEntry( String entryName ) throws ParseException
    {
        if ( entryName == null )
        {
            throw new ParseException( "Missing entry attribute of properties element", null );
        }

        URL entryURL = m_bundle.getEntry( entryName );
        if ( entryURL == null )
        {
            throw new ParseException( "Missing bundle entry " + entryName, null );
        }

        Properties props = new Properties();
        InputStream entryStream = null;
        try
        {
            entryStream = entryURL.openStream();
            props.load( entryStream );
        }
        catch ( IOException ioe )
        {
            throw new ParseException( "Failed to read properties entry " + entryName, ioe );
        }
        finally
        {
            if ( entryStream != null )
            {
                try
                {
                    entryStream.close();
                }
                catch ( IOException ignore )
                {
                    // don't care
                }
            }
        }

        // create PropertyMetadata for the properties from the file
        for ( Iterator pi = props.entrySet().iterator(); pi.hasNext(); )
        {
            Map.Entry pEntry = ( Map.Entry ) pi.next();
            PropertyMetadata prop = new PropertyMetadata();
            prop.setName( String.valueOf( pEntry.getKey() ) );
            prop.setValue( String.valueOf( pEntry.getValue() ) );
            m_currentComponent.addProperty( prop );
        }
    }
}
