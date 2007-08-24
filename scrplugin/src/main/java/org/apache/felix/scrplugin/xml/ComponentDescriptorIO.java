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
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.xml.transform.TransformerException;

import org.apache.felix.scrplugin.om.Component;
import org.apache.felix.scrplugin.om.Components;
import org.apache.felix.scrplugin.om.Implementation;
import org.apache.felix.scrplugin.om.Interface;
import org.apache.felix.scrplugin.om.Property;
import org.apache.felix.scrplugin.om.Reference;
import org.apache.felix.scrplugin.om.Service;
import org.apache.maven.plugin.MojoExecutionException;
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

    public static final String NAMESPACE_URI = "http://www.osgi.org/xmlns/scr/v1.0.0";

    private static final String PREFIX = "scr";

    private static final String COMPONENTS = "components";

    private static final String COMPONENT = "component";

    private static final String COMPONENT_QNAME = PREFIX + ':' + COMPONENT;

    private static final String IMPLEMENTATION = "implementation";

    private static final String IMPLEMENTATION_QNAME = PREFIX + ':' + IMPLEMENTATION;

    private static final String SERVICE = "service";

    private static final String SERVICE_QNAME = PREFIX + ':' + SERVICE;

    private static final String PROPERTY = "property";

    private static final String PROPERTY_QNAME = PREFIX + ':' + PROPERTY;

    private static final String REFERENCE = "reference";

    private static final String REFERENCE_QNAME = PREFIX + ':' + REFERENCE;

    private static final String INTERFACE = "provide";

    private static final String INTERFACE_QNAME = PREFIX + ':' + INTERFACE;

    public static Components read(File file)
    throws MojoExecutionException {
        try {
            final XmlHandler xmlHandler = new XmlHandler();
            IOUtils.parse(file, xmlHandler);
            return xmlHandler.components;
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to read xml.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to read xml from " + file, e);
        }
    }

    /**
     * Write the component descriptors to the file.
     * @param components
     * @param file
     * @throws MojoExecutionException
     */
    public static void write(Components components, File file)
    throws MojoExecutionException {
        try {
            generateXML(components, IOUtils.getSerializer(file));
        } catch (TransformerException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        } catch (SAXException e) {
            throw new MojoExecutionException("Unable to generate xml for " + file, e);
        } catch (IOException e) {
            throw new MojoExecutionException("Unable to write xml to " + file, e);
        }
    }

    /**
     * Generate the xml top level element and start streaming
     * the components.
     * @param components
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Components components, ContentHandler contentHandler)
    throws SAXException {
        contentHandler.startDocument();
        contentHandler.startPrefixMapping(PREFIX, NAMESPACE_URI);

        // wrapper element to generate well formed xml
        contentHandler.startElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS, new AttributesImpl());

        final Iterator i = components.getComponents().iterator();
        while ( i.hasNext() ) {
            final Component component = (Component)i.next();
            generateXML(component, contentHandler);
        }
        // end wrapper element
        contentHandler.endElement("", ComponentDescriptorIO.COMPONENTS, ComponentDescriptorIO.COMPONENTS);
        contentHandler.endPrefixMapping(PREFIX);
        contentHandler.endDocument();
    }

    /**
     * Write the xml for a {@link Component}.
     * @param component
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Component component, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "enabled", component.isEnabled());
        addAttribute(ai, "immediate",component.isImmediate());
        addAttribute(ai, "name", component.getName());
        addAttribute(ai, "factory", component.getFactory());

        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME, ai);
        generateXML(component.getImplementation(), contentHandler);
        if ( component.getService() != null ) {
            generateXML(component.getService(), contentHandler);
        }
        if ( component.getProperties() != null ) {
            final Iterator i = component.getProperties().iterator();
            while ( i.hasNext() ) {
                final Property property = (Property)i.next();
                generateXML(property, contentHandler);
            }
        }
        if ( component.getReferences() != null ) {
            final Iterator i = component.getReferences().iterator();
            while ( i.hasNext() ) {
                final Reference reference = (Reference)i.next();
                generateXML(reference, contentHandler);
            }
        }
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.COMPONENT, ComponentDescriptorIO.COMPONENT_QNAME);
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
        addAttribute(ai, "class", implementation.getClassame());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION, ComponentDescriptorIO.IMPLEMENTATION_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.IMPLEMENTATION, ComponentDescriptorIO.IMPLEMENTATION_QNAME);
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
        addAttribute(ai, "servicefactory", service.getServicefactory());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME, ai);
        if ( service.getInterfaces() != null ) {
            final Iterator i = service.getInterfaces().iterator();
            while ( i.hasNext() ) {
                final Interface interf = (Interface)i.next();
                generateXML(interf, contentHandler);
            }
        }
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.SERVICE, ComponentDescriptorIO.SERVICE_QNAME);
    }

    /**
     * Write the xml for a {@link Interface}.
     * @param interface
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Interface interf, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "interface", interf.getInterfacename());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.INTERFACE, ComponentDescriptorIO.INTERFACE_QNAME);
    }

    /**
     * Write the xml for a {@link Property}.
     * @param property
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Property property, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "name", property.getName());
        addAttribute(ai, "type", property.getType());
        addAttribute(ai, "value", property.getValue());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME, ai);
        if ( property.getMultiValue() != null && property.getMultiValue().length > 0 ) {
            for(int i=0; i<property.getMultiValue().length; i++) {
                text(contentHandler, "    ");
                text(contentHandler, property.getMultiValue()[i]);
                text(contentHandler, "\n");
            }
        }
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.PROPERTY, ComponentDescriptorIO.PROPERTY_QNAME);
    }

    /**
     * Write the xml for a {@link Reference}.
     * @param reference
     * @param contentHandler
     * @throws SAXException
     */
    protected static void generateXML(Reference reference, ContentHandler contentHandler)
    throws SAXException {
        final AttributesImpl ai = new AttributesImpl();
        addAttribute(ai, "name", reference.getName());
        addAttribute(ai, "interface", reference.getInterfacename());
        addAttribute(ai, "cardinality", reference.getCardinality());
        addAttribute(ai, "policy", reference.getPolicy());
        addAttribute(ai, "target", reference.getTarget());
        addAttribute(ai, "bind", reference.getBind());
        addAttribute(ai, "unbind", reference.getUnbind());
        contentHandler.startElement(NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME, ai);
        contentHandler.endElement(NAMESPACE_URI, ComponentDescriptorIO.REFERENCE, ComponentDescriptorIO.REFERENCE_QNAME);
    }

    /**
     * Helper method to add an attribute.
     * This implementation adds a new attribute with the given name
     * and value. Before adding the value is checked for non-null.
     * @param ai    The attributes impl receiving the additional attribute.
     * @param name  The name of the attribute.
     * @param value The value of the attribute.
     */
    protected static void addAttribute(AttributesImpl ai, String name, Object value) {
        if ( value != null ) {
            ai.addAttribute("", name, name, "CDATA", value.toString());
        }
    }

    /**
     * Helper method writing out a string.
     * @param ch
     * @param text
     * @throws SAXException
     */
    protected static void text(ContentHandler ch, String text)
    throws SAXException {
        if ( text != null ) {
            final char[] c = text.toCharArray();
            ch.characters(c, 0, c.length);
        }
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
                    this.overrideNamespace = NAMESPACE_URI;
                }
            }

            if ( this.overrideNamespace != null && "".equals(uri) ) {
                uri = this.overrideNamespace;
            }

            if ( NAMESPACE_URI.equals(uri) ) {

                if (localName.equals(COMPONENT)) {

                    this.currentComponent = new Component();
                    this.currentComponent.setName(attributes.getValue("name"));

                    // enabled attribute is optional
                    if (attributes.getValue("enabled") != null) {
                        this.currentComponent.setEnabled(Boolean.valueOf(attributes.getValue("enabled")));
                    }

                    // immediate attribute is optional
                    if (attributes.getValue("immediate") != null) {
                        this.currentComponent.setImmediate(Boolean.valueOf(attributes.getValue("immediate")));
                    }

                    this.currentComponent.setFactory(attributes.getValue("factory"));

                } else if (localName.equals(IMPLEMENTATION)) {
                    // Set the implementation class name (mandatory)
                    final Implementation impl = new Implementation();
                    this.currentComponent.setImplementation(impl);
                    impl.setClassname(attributes.getValue("class"));

                } else if (localName.equals(PROPERTY)) {
                    final Property prop = new Property();

                    prop.setName(attributes.getValue("name"));
                    prop.setType(attributes.getValue("type"));

                    if ( attributes.getValue("value") != null) {
                        prop.setValue(attributes.getValue("value"));
                        this.currentComponent.addProperty(prop);
                    } else {
                        // hold the property pending as we have a multi value
                        this.pendingProperty = prop;
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

            if ( NAMESPACE_URI.equals(uri) ) {
                if (localName.equals("component") ) {
                    this.components.addComponent(this.currentComponent);
                    this.currentComponent = null;
                } else if (localName.equals("property") && this.pendingProperty != null) {
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
                if ( this.pendingProperty.getValue() != null ) {
                    this.pendingProperty.setValue(text);
                } else {
                    this.pendingProperty.setValue(this.pendingProperty.getValue() + text);
                }
            }
        }
    }
}
