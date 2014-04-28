/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.features.internal.repository;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.karaf.features.internal.resolver.CapabilityImpl;
import org.apache.karaf.features.internal.resolver.RequirementImpl;
import org.apache.karaf.features.internal.resolver.ResourceImpl;
import org.osgi.framework.Version;
import org.osgi.resource.Resource;

import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

/**
 * Repository XML xml based on StaX
 */
public final class StaxParser {

    public static final String REPOSITORY = "repository";
    public static final String REPO_NAME = "name";
    public static final String INCREMENT = "increment";
    public static final String REFERRAL = "referral";
    public static final String DEPTH = "depth";
    public static final String URL = "url";
    public static final String RESOURCE = "resource";
    public static final String CAPABILITY = "capability";
    public static final String REQUIREMENT = "requirement";
    public static final String NAMESPACE = "namespace";
    public static final String ATTRIBUTE = "attribute";
    public static final String DIRECTIVE = "directive";
    public static final String NAME = "name";
    public static final String VALUE = "value";
    public static final String TYPE = "type";

    static XMLInputFactory factory;

    private StaxParser() {
    }

    public static class Referral {
        String url;
        int depth = Integer.MAX_VALUE;
    }

    public static class XmlRepository {
        String name;
        long increment;
        List<Referral> referrals = new ArrayList<>();
        List<Resource> resources = new ArrayList<>();
    }

    public static XmlRepository parse(InputStream is) throws XMLStreamException {
        return parse(is, null);
    }

    public static XmlRepository parse(InputStream is, XmlRepository previous) throws XMLStreamException {
        XMLStreamReader reader = getFactory().createXMLStreamReader(is);
        int event = reader.nextTag();
        if (event != START_ELEMENT || !REPOSITORY.equals(reader.getLocalName())) {
            throw new IllegalStateException("Expected element 'repository' at the root of the document");
        }
        XmlRepository repo = new XmlRepository();
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++) {
            String attrName = reader.getAttributeLocalName(i);
            String attrValue = reader.getAttributeValue(i);
            switch (attrName) {
            case REPO_NAME:
                repo.name = attrName;
                break;
            case INCREMENT:
                repo.increment = Integer.parseInt(attrValue);
                break;
            default:
                throw new IllegalStateException("Unexpected attribute '" + attrName + "'");
            }
        }
        if (previous != null && repo.increment == previous.increment) {
            return previous;
        }
        while ((event = reader.nextTag()) == START_ELEMENT) {
            String element = reader.getLocalName();
            switch (element) {
            case REFERRAL:
                Referral referral = new Referral();
                for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++) {
                    String attrName = reader.getAttributeLocalName(i);
                    String attrValue = reader.getAttributeValue(i);
                    switch (attrName) {
                    case DEPTH:
                        referral.depth = Integer.parseInt(attrValue);
                        break;
                    case URL:
                        referral.url = attrValue;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected attribute '" + attrName + "'");
                    }
                }
                if (referral.url == null) {
                    throw new IllegalStateException("Expected attribute '" + URL + "'");
                }
                repo.referrals.add(referral);
                sanityCheckEndElement(reader, reader.nextTag(), REFERRAL);
                break;
            case RESOURCE:
                repo.resources.add(parseResource(reader));
                break;
            default:
                throw new IllegalStateException("Unsupported element '" + element + "'. Expected 'referral' or 'resource'");
            }
        }
        // Sanity check
        sanityCheckEndElement(reader, event, REPOSITORY);
        return repo;
    }

    private static void sanityCheckEndElement(XMLStreamReader reader, int event, String element) {
        if (event != END_ELEMENT || !element.equals(reader.getLocalName())) {
            throw new IllegalStateException("Unexpected state while finishing element " + element);
        }
    }

    private static ResourceImpl parseResource(XMLStreamReader reader) {
        try {
            if (reader.getAttributeCount() > 0) {
                throw new IllegalStateException("Unexpected attribute '" + reader.getAttributeLocalName(0) + "'");
            }
            ResourceImpl resource = new ResourceImpl();
            int event;
            while ((event = reader.nextTag()) == START_ELEMENT) {
                String element = reader.getLocalName();
                switch (element) {
                case CAPABILITY:
                    resource.addCapability(parseCapability(reader, resource));
                    break;
                case REQUIREMENT:
                    resource.addRequirement(parseRequirement(reader, resource));
                    break;
                default:
                    while ((event = reader.next()) != END_ELEMENT) {
                        switch (event) {
                        case START_ELEMENT:
                            throw new IllegalStateException("Unexpected element '" + reader.getLocalName() + "' inside 'resource' element");
                        case CHARACTERS:
                            throw new IllegalStateException("Unexpected text inside 'resource' element");
                        default:
                            break;
                        }
                    }
                    break;
                }
            }
            // Sanity check
            sanityCheckEndElement(reader, event, RESOURCE);
            return resource;
        } catch (Exception e) {
            Location loc = reader.getLocation();
            if (loc != null) {
                throw new IllegalStateException("Error while parsing resource at line " + loc.getLineNumber() + " and column " + loc.getColumnNumber(), e);
            } else {
                throw new IllegalStateException("Error while parsing resource", e);
            }
        }
    }

    private static CapabilityImpl parseCapability(XMLStreamReader reader, ResourceImpl resource) throws XMLStreamException {
        String[] namespace = new String[1];
        Map<String, String> directives = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        parseClause(reader, namespace, directives, attributes);
        sanityCheckEndElement(reader, reader.getEventType(), CAPABILITY);
        return new CapabilityImpl(resource, namespace[0], directives, attributes);
    }

    private static RequirementImpl parseRequirement(XMLStreamReader reader, ResourceImpl resource) throws XMLStreamException {
        String[] namespace = new String[1];
        Map<String, String> directives = new HashMap<>();
        Map<String, Object> attributes = new HashMap<>();
        parseClause(reader, namespace, directives, attributes);
        sanityCheckEndElement(reader, reader.getEventType(), REQUIREMENT);
        return new RequirementImpl(resource, namespace[0], directives, attributes);
    }

    private static void parseClause(XMLStreamReader reader, String[] namespace, Map<String, String> directives, Map<String, Object> attributes) throws XMLStreamException {
        namespace[0] = null;
        for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++) {
            String name = reader.getAttributeLocalName(i);
            String value = reader.getAttributeValue(i);
            if (NAMESPACE.equals(name)) {
                namespace[0] = value;
            } else {
                throw new IllegalStateException("Unexpected attribute: '" + name + "'. Expected 'namespace'");
            }
        }
        if (namespace[0] == null) {
            throw new IllegalStateException("Expected attribute 'namespace'");
        }
        while (reader.nextTag() == START_ELEMENT) {
            String element = reader.getLocalName();
            switch (element) {
            case DIRECTIVE: {
                String name = null;
                String value = null;
                for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++) {
                    String attName = reader.getAttributeLocalName(i);
                    String attValue = reader.getAttributeValue(i);
                    switch (attName) {
                    case NAME:
                        name = attValue;
                        break;
                    case VALUE:
                        value = attValue;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected attribute: '" + attName + "'. Expected 'name', or 'value'.");
                    }
                }
                if (name == null || value == null) {
                    throw new IllegalStateException("Expected attribute 'name' and 'value'");
                }
                directives.put(name, value);
                sanityCheckEndElement(reader, reader.nextTag(), DIRECTIVE);
                break;
            }
            case ATTRIBUTE: {
                String name = null;
                String value = null;
                String type = "String";
                for (int i = 0, nb = reader.getAttributeCount(); i < nb; i++) {
                    String attName = reader.getAttributeLocalName(i);
                    String attValue = reader.getAttributeValue(i);
                    switch (attName) {
                    case NAME:
                        name = attValue;
                        break;
                    case VALUE:
                        value = attValue;
                        break;
                    case TYPE:
                        type = attValue;
                        break;
                    default:
                        throw new IllegalStateException("Unexpected attribute: '" + attName + "'. Expected 'name', 'value' or 'type'.");
                    }
                }
                if (name == null || value == null) {
                    throw new IllegalStateException("Expected attribute 'name' and 'value'");
                }
                attributes.put(name, parseAttribute(value, type));
                sanityCheckEndElement(reader, reader.nextTag(), ATTRIBUTE);
                break;
            }
            default:
                throw new IllegalStateException("Unexpected element: '" + element + ". Expected 'directive' or 'attribute'");
            }
        }
    }

    private static Object parseAttribute(String value, String type) {
        if ("String".equals(type)) {
            return value;
        } else if ("Version".equals(type)) {
            return Version.parseVersion(value);
        } else if ("Long".equals(type)) {
            return Long.parseLong(value.trim());
        } else if ("Double".equals(type)) {
            return Double.parseDouble(value.trim());
        } else if (type.startsWith("List<") && type.endsWith(">")) {
            type = type.substring("List<".length(), type.length() - 1);
            List<Object> list = new ArrayList<>();
            for (String s : value.split(",")) {
                list.add(parseAttribute(s.trim(), type));
            }
            return list;
        } else {
            throw new IllegalStateException("Unexpected type: '" + type + "'");
        }
    }

    private static synchronized XMLInputFactory getFactory() {
        if (StaxParser.factory == null) {
            XMLInputFactory factory = XMLInputFactory.newInstance();
            factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, true);
            StaxParser.factory = factory;
        }
        return StaxParser.factory;
    }

}