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
package org.apache.felix.scrplugin.xml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

import org.apache.felix.scrplugin.Constants;
import org.apache.felix.scrplugin.SCRDescriptorException;
import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.om.Implementation;
import org.apache.felix.scrplugin.om.Interface;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.om.Service;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

/**
 * <code>ComponentDescriptorIO</code>
 *
 * is a helper class to read and write component descriptor files.
 *
 */
public class ComponentDescriptorIO {

    /** The namespace for R4.1 - Version 1.0 */
    public static final String NAMESPACE_URI_1_0 = "http://www.osgi.org/xmlns/scr/v1.0.0";

    /** The namespace for R4.2 - Version 1.1 */
    public static final String NAMESPACE_URI_1_1 = "http://www.osgi.org/xmlns/scr/v1.1.0";

    /** The namespace for R4.2+FELIX-1893 - Version 1.1-felix */
    public static final String NAMESPACE_URI_1_1_FELIX = "http://www.osgi.org/xmlns/scr/v1.1.0-felix";

    /** The inner namespace - used for all inner elements. */
    public static final String INNER_NAMESPACE_URI = "";

    /** The prefix used for the namespace. */
    private static final String PREFIX = "scr";

    /** The root element. */
    private static final String COMPONENTS = "components";

    /** The component element. */
    private static final String COMPONENT = "component";

    /** The qualified component element. */
    private static final String COMPONENT_QNAME = PREFIX + ':' + COMPONENT;

    /** The enabled attribute. */
    private static final String COMPONENT_ATTR_ENABLED = "enabled";

    /** Component: The policy attribute. */
    private static final String COMPONENT_ATTR_POLICY = "configuration-policy";

    /** Component: The factory attribute. */
    private static final String COMPONENT_ATTR_FACTORY = "factory";

    /** Component: The immediate attribute. */
    private static final String COMPONENT_ATTR_IMMEDIATE = "immediate";

    /** Component: The name attribute. */
    private static final String COMPONENT_ATTR_NAME = "name";

    /** Component: The activate attribute. */
    private static final String COMPONENT_ATTR_ACTIVATE = "activate";

    /** Component: The deactivate attribute. */
    private static final String COMPONENT_ATTR_DEACTIVATE = "deactivate";

    /** Component: The modified attribute. */
    private static final String COMPONENT_ATTR_MODIFIED = "modified";

    private static final String IMPLEMENTATION = "implementation";

    private static final String IMPLEMENTATION_QNAME = IMPLEMENTATION;

    private static final String SERVICE = "service";

    private static final String SERVICE_QNAME = SERVICE;

    private static final String PROPERTY = "property";

    private static final String PROPERTY_QNAME = PROPERTY;

    private static final String REFERENCE = "reference";

    private static final String REFERENCE_QNAME = REFERENCE;

    private static final String INTERFACE = "provide";

    private static final String INTERFACE_QNAME = INTERFACE;

    public static Components read(InputStream file)
    throws SCRDescriptorException {
        try {
            final XmlHandler xmlHandler = new XmlHandler();
            IOUtils.parse(file, xmlHandler);
            return xmlHandler.components;
        } catch (TransformerException e) {
            throw new SCRDescriptorException( "Unable to read xml", "[stream]", 0, e );
        }
    }

    /**
     * Write the component descriptors to the file.
     * @param components
     * @param file
     * @throws SCRDescriptorException
     */
    public static void write(Components components, File file, boolean isScrPrivateFile)
    throws SCRDescriptorException {
        try {
            generateXML(components, IOUtils.getSerializer(file), isScrPrivateFile);
        } catch (TransformerException e) {
            throw new SCRDescriptorException("Unable to write xml", file.toString(), 0, e);
        } catch (SAXException e) {
            throw new SCRDescriptorException("Unable to generate xml", file.toString(), 0, e);
        } catch (IOException e) {
            throw new SCRDescriptorException("Unable to write xml", file.toString(), 0, e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the components.
     * @param components
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Components components, ContentHandler contentHandler, boolean isScrPrivateFile)
    throws SAXException {
        // detect namespace to use
        final String namespace;
        if ( components.getSpecVersion() == Constants.VERSION_1_0 ) {
            namespace = NAMESPACE_URI_1_0;
        } else if ( components.getSpecVersion() == Constants.VERSION_1_1 ) {
            namespace = NAMESPACE_URI_1_1;
        } else {
            namespace = NAMESPACE_URI_1_1_FELIX;
        }
        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, namespace);

        // wrapper element to generate well formed xml
        contentHandler.startElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS, new AttributesImpl());
        IOUtils.newline(contentHandler);

        for(final Component component : components.getComponents()) {
            if ( component.isDs() ) {
                generateXML(namespace, component, contentHandler, isScrPrivateFile);
            }
        }
        // end wrapper element
        contentHandler.endElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS);
        IOUtils.newline(contentHandler);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    /**
     * Write the xml for a {@link Component}.
     * @param component
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(final String namespace,
                                      final Component component,
                                      final ContentHandler contentHandler,
                                      final boolean isScrPrivateFile)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, COMPONENT_ATTR_ENABLED, component.isEnabled());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_IMMEDIATE,component.isImmediate());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_NAME, component.getName());
        IOUtils.addAttribute(ai, COMPONENT_ATTR_FACTORY, component.getFactory());

        // attributes new in 1.1
        if ( NAMESPACE_URI_1_1.equals( namespace ) || NAMESPACE_URI_1_1_FELIX.equals( namespace ) ) {
            IOUtils.addAttribute(ai, COMPONENT_ATTR_POLICY, component.getConfigurationPolicy());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_ACTIVATE, component.getActivate());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_DEACTIVATE, component.getDeactivate());
            IOUtils.addAttribute(ai, COMPONENT_ATTR_MODIFIED, component.getModified());
        }

        IOUtils.indent(contentHandler, 1);
        contentHandler.startElement(namespace, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME, ai);
        IOUtils.newline(contentHandler);
        generateXML(component.getImplementation(), contentHandler);
        if ( component.getService() != null ) {
            generateXML(component.getService(), contentHandler);
        }
        if ( component.getProperties() != null ) {
            for(final Property property : component.getProperties()) {
                generateXML(property, contentHandler, isScrPrivateFile);
            }
        }
        if ( component.getReferences() != null ) {
            for(final Reference reference : component.getReferences()) {
                generateXML(namespace, reference, contentHandler, isScrPrivateFile);
            }
        }
        IOUtils.indent(contentHandler, 1);
        contentHandler.endElement(namespace, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Implementation}.
     * @param implementation
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Implementation implementation, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "class", implementation.getClassame());
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION, ComponentDescriptorIO.IMPLEMENTATION_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION, ComponentDescriptorIO.IMPLEMENTATION_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Service}.
     * @param service
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Service service, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "servicefactory", String.valueOf(service.isServicefactory()));
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME, ai);
        if ( service.getInterfaces() != null && service.getInterfaces().size() > 0 ) {
            IOUtils.newline(contentHandler);
            for(final Interface interf : service.getInterfaces()) {
                generateXML(interf, contentHandler);
            }
            IOUtils.indent(contentHandler, 2);
        }
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Interface}.
     * @param interf
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Interface interf, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "interface", interf.getInterfacename());
        IOUtils.indent(contentHandler, 3);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Property}.
     * @param property
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Property property, ContentHandler contentHandler, boolean isScrPrivateFile)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "name", property.getName());
        IOUtils.addAttribute(ai, "type", property.getType());
        IOUtils.addAttribute(ai, "value", property.getValue());
        // we have to write more information if this is our scr private file
        if ( isScrPrivateFile ) {
            IOUtils.addAttribute(ai, "private", String.valueOf(property.isPrivate()));
            if ( property.getLabel() != null ) {
                IOUtils.addAttribute(ai, "label", String.valueOf(property.getLabel()));
            }
            if ( property.getDescription() != null ) {
                IOUtils.addAttribute(ai, "description", String.valueOf(property.getDescription()));
            }
            if ( property.getCardinality() != null ) {
                IOUtils.addAttribute(ai, "cardinality", String.valueOf(property.getCardinality()));
            }
        }
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME, ai);
        if ( property.getMultiValue() != null && property.getMultiValue().length > 0 ) {
            // generate a new line first
            IOUtils.text(contentHandler, "\n");
            for(int i=0; i<property.getMultiValue().length; i++) {
                IOUtils.indent(contentHandler, 3);
                IOUtils.text(contentHandler, property.getMultiValue()[i]);
                IOUtils.newline(contentHandler);
            }
            IOUtils.indent(contentHandler, 2);
        }
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * Write the xml for a {@link Reference}.
     * @param reference
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(final String namespace,Reference reference, ContentHandler contentHandler, boolean isScrPrivateFile)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        IOUtils.addAttribute(ai, "name", reference.getName());
        IOUtils.addAttribute(ai, "interface", reference.getInterfacename());
        IOUtils.addAttribute(ai, "cardinality", reference.getCardinality());
        IOUtils.addAttribute(ai, "policy", reference.getPolicy());
        IOUtils.addAttribute(ai, "target", reference.getTarget());
        IOUtils.addAttribute(ai, "bind", reference.getBind());
        IOUtils.addAttribute(ai, "unbind", reference.getUnbind());

        // attributes new in 1.1-felix (FELIX-1893)
        if ( NAMESPACE_URI_1_1_FELIX.equals( namespace ) ) {
            IOUtils.addAttribute(ai, "updated", reference.getUpdated());
        }

        if ( isScrPrivateFile ) {
            IOUtils.addAttribute(ai, "checked", String.valueOf(reference.isChecked()));
        }
        IOUtils.indent(contentHandler, 2);
        contentHandler.startElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME, ai);
        contentHandler.endElement(INNER_NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME);
        IOUtils.newline(contentHandler);
    }

    /**
     * A content handler for parsing the component descriptions.
     *
     */
    protected static final class XmlHandler extends DefaultHandler {

        /** The components container. */
        protected final Components components = new Components();

        /** A reference to the current component. */
        protected Component currentComponent;

        /** The current service. */
        protected Service currentService;

        /** Pending property. */
        protected Property pendingProperty;

        /** Flag for detecting the first element. */
        protected boolean firstElement = true;

        /** Flag for elements inside a component element */
        protected boolean isComponent = false;

        /** Override namespace. */
        protected String overrideNamespace;

        public void startElement(String uri, String localName, String name, Attributes attributes)
        throws SAXException {
            // according to the spec, the elements should have the namespace,
            // except when the root element is the "component" element
            // So we check this for the first element, we receive.
            if ( this.firstElement ) {
                this.firstElement = false;
                if ( localName.equals(COMPONENT) && "".equals(uri) ) {
                    this.overrideNamespace = NAMESPACE_URI_1_0;
                }
            }

            if ( this.overrideNamespace != null && "".equals(uri) ) {
                uri = this.overrideNamespace;
            }

            // however the spec also states that the inner elements
            // of a component are unqualified, so they don't have
            // the namespace - we allow both: with or without namespace!
            if ( this.isComponent && "".equals(uri) )  {
                uri = NAMESPACE_URI_1_0;
            }

            // from here on, uri has the namespace regardless of the used xml format
            if ( NAMESPACE_URI_1_0.equals( uri ) || NAMESPACE_URI_1_1.equals( uri )
                || NAMESPACE_URI_1_1_FELIX.equals( uri ) ) {

                if ( NAMESPACE_URI_1_1.equals(uri) ) {
                    components.setSpecVersion(Constants.VERSION_1_1);
                } else if ( NAMESPACE_URI_1_1_FELIX.equals(uri) ) {
                    components.setSpecVersion(Constants.VERSION_1_1_FELIX);
                }

                if (localName.equals(COMPONENT)) {
                    this.isComponent = true;

                    this.currentComponent = new Component();
                    this.currentComponent.setName(attributes.getValue(COMPONENT_ATTR_NAME));

                    // enabled attribute is optional
                    if (attributes.getValue(COMPONENT_ATTR_ENABLED) != null) {
                        this.currentComponent.setEnabled(Boolean.valueOf(attributes.getValue(COMPONENT_ATTR_ENABLED)));
                    }

                    // immediate attribute is optional
                    if (attributes.getValue(COMPONENT_ATTR_IMMEDIATE) != null) {
                        this.currentComponent.setImmediate(Boolean.valueOf(attributes.getValue(COMPONENT_ATTR_IMMEDIATE)));
                    }

                    this.currentComponent.setFactory(attributes.getValue(COMPONENT_ATTR_FACTORY));

                    // check for version 1.1 attributes
                    if ( components.getSpecVersion() == Constants.VERSION_1_1 ) {
                        this.currentComponent.setConfigurationPolicy(attributes.getValue(COMPONENT_ATTR_POLICY));
                        this.currentComponent.setActivate(attributes.getValue(COMPONENT_ATTR_ACTIVATE));
                        this.currentComponent.setDeactivate(attributes.getValue(COMPONENT_ATTR_DEACTIVATE));
                        this.currentComponent.setModified(attributes.getValue(COMPONENT_ATTR_MODIFIED));
                    }
                } else if (localName.equals(IMPLEMENTATION)) {
                    // Set the implementation class name (mandatory)
                    final Implementation impl = new Implementation();
                    this.currentComponent.setImplementation(impl);
                    impl.setClassname(attributes.getValue("class"));

                } else if (localName.equals(PROPERTY)) {

                    // read the property, unless it is the service.pid
                    // property which must not be inherited
                    final String propName = attributes.getValue( "name" );
                    if ( !org.osgi.framework.Constants.SERVICE_PID.equals( propName ) )
                    {
                        final Property prop = new Property();

                        prop.setName( propName );
                        prop.setType( attributes.getValue( "type" ) );

                        if ( attributes.getValue( "value" ) != null )
                        {
                            prop.setValue( attributes.getValue( "value" ) );
                            this.currentComponent.addProperty( prop );
                        }
                        else
                        {
                            // hold the property pending as we have a multi value
                            this.pendingProperty = prop;
                        }
                        // check for abstract properties
                        prop.setLabel( attributes.getValue( "label" ) );
                        prop.setDescription( attributes.getValue( "description" ) );
                        prop.setCardinality( attributes.getValue( "cardinality" ) );
                        final String pValue = attributes.getValue( "private" );
                        if ( pValue != null )
                        {
                            prop.setPrivate( Boolean.valueOf( pValue ).booleanValue() );
                        }
                    }

                } else if (localName.equals("properties")) {

                    // TODO: implement the properties tag

                } else if (localName.equals(SERVICE)) {

                    this.currentService = new Service();

                    this.currentService.setServicefactory(attributes.getValue("servicefactory"));

                    this.currentComponent.setService(this.currentService);

                } else if (localName.equals(INTERFACE)) {
                    final Interface interf = new Interface();
                    this.currentService.addInterface(interf);
                    interf.setInterfacename(attributes.getValue("interface"));

                } else if (localName.equals(REFERENCE)) {
                    final Reference ref = new Reference();

                    ref.setName(attributes.getValue("name"));
                    ref.setInterfacename(attributes.getValue("interface"));
                    ref.setCardinality(attributes.getValue("cardinality"));
                    ref.setPolicy(attributes.getValue("policy"));
                    ref.setTarget(attributes.getValue("target"));
                    ref.setBind(attributes.getValue("bind"));
                    ref.setUnbind(attributes.getValue("unbind"));

                    if ( attributes.getValue("checked") != null ) {
                        ref.setChecked(Boolean.valueOf(attributes.getValue("checked")).booleanValue());
                    }

                    this.currentComponent.addReference(ref);
                }
            }
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        public void endElement(String uri, String localName, String name) throws SAXException {
            if ( this.overrideNamespace != null && "".equals(uri) ) {
                uri = this.overrideNamespace;
            }

            if ( this.isComponent && "".equals(uri) )  {
                uri = NAMESPACE_URI_1_0;
            }

            if ( NAMESPACE_URI_1_0.equals( uri ) || NAMESPACE_URI_1_1.equals( uri )
                || NAMESPACE_URI_1_1_FELIX.equals( uri ) )
            {
                if (localName.equals(COMPONENT) ) {
                    this.components.addComponent(this.currentComponent);
                    this.currentComponent = null;
                    this.isComponent = false;
                } else if (localName.equals(PROPERTY) && this.pendingProperty != null) {
                    // now split the value
                    final String text = this.pendingProperty.getValue();
                    if ( text != null ) {
                        final StringTokenizer st = new StringTokenizer(text);
                        final String[] values = new String[st.countTokens()];
                        int index = 0;
                        while ( st.hasMoreTokens() ) {
                            values[index] = st.nextToken();
                            index++;
                        }
                        this.pendingProperty.setMultiValue(values);
                    }
                    this.currentComponent.addProperty(this.pendingProperty);
                    this.pendingProperty = null;
                }
            }
        }

        /**
         * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
         */
        public void characters(char[] ch, int start, int length) throws SAXException {
            if ( this.pendingProperty != null ) {
                final String text = new String(ch, start, length);
                if ( this.pendingProperty.getValue() == null ) {
                    this.pendingProperty.setValue(text);
                } else {
                    this.pendingProperty.setValue(this.pendingProperty.getValue() + text);
                }
            }
        }
    }
}
