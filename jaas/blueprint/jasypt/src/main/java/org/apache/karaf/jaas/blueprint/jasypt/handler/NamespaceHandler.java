/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.apache.karaf.jaas.blueprint.jasypt.handler;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.ext.PlaceholdersUtils;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.CollectionMetadata;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class NamespaceHandler implements org.apache.aries.blueprint.NamespaceHandler {

    public static final String ID_ATTRIBUTE = "id";
    public static final String PLACEHOLDER_PREFIX_ATTRIBUTE = "placeholder-prefix";
    public static final String PLACEHOLDER_SUFFIX_ATTRIBUTE = "placeholder-suffix";
    public static final String PROPERTY_PLACEHOLDER_ELEMENT = "property-placeholder";
    public static final String ENCRYPTOR_REF_ATTRIBUTE = "encryptor-ref";
    public static final String ENCRYPTOR_ELEMENT = "encryptor";
    public static final String JASYPT_NAMESPACE_1_0 = "http://karaf.apache.org/xmlns/jasypt/v1.0.0";

    private int idCounter;

    @Override
    public URL getSchemaLocation(String namespace) {
        switch (namespace) {
            case "http://karaf.apache.org/xmlns/jasypt/v1.0.0":
                return getClass().getResource("/org/apache/karaf/jaas/blueprint/jasypt/handler/karaf-jasypt-1.0.0.xsd");
            default:
                return null;
        }
    }

    @Override
    public Set<Class> getManagedClasses() {
        return new HashSet<>(Collections.singletonList(
            EncryptablePropertyPlaceholder.class
        ));
    }

    @Override
    public Metadata parse(Element element, ParserContext context) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        if (PROPERTY_PLACEHOLDER_ELEMENT.equals(name)) {
            return parsePropertyPlaceholder(element, context);
        }
        throw new ComponentDefinitionException("Bad xml syntax: unknown element '" + name + "'");
    }

    @Override
    public ComponentMetadata decorate(Node node, ComponentMetadata componentMetadata, ParserContext parserContext) {
        throw new ComponentDefinitionException("Bad xml syntax: node decoration is not supported");
    }

    public ComponentMetadata parsePropertyPlaceholder(Element element, ParserContext context) {
        MutableBeanMetadata metadata = context.createMetadata(MutableBeanMetadata.class);
        metadata.setProcessor(true);
        metadata.setId(getId(context, element));
        metadata.setScope(BeanMetadata.SCOPE_SINGLETON);
        metadata.setRuntimeClass(EncryptablePropertyPlaceholder.class);
        metadata.setInitMethod("init");
        String prefix = element.hasAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                                    ? element.getAttribute(PLACEHOLDER_PREFIX_ATTRIBUTE)
                                    : "ENC(";
        metadata.addProperty("placeholderPrefix", createValue(context, prefix));
        String suffix = element.hasAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                                    ? element.getAttribute(PLACEHOLDER_SUFFIX_ATTRIBUTE)
                                    : ")";
        metadata.addProperty("placeholderSuffix", createValue(context, suffix));
        String encryptorRef = element.hasAttribute("encryptor-ref")
                                    ? element.getAttribute("encryptor-ref")
                                    : null;
        if (encryptorRef != null) {
            metadata.addProperty("encryptor", createRef(context, encryptorRef));
        }
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node node = nl.item(i);
            if (node instanceof Element) {
                Element e = (Element) node;
                if (JASYPT_NAMESPACE_1_0.equals(e.getNamespaceURI())) {
                    String name = e.getLocalName() != null ? e.getLocalName() : e.getNodeName();
                    if (ENCRYPTOR_ELEMENT.equals(name)) {
                        if (encryptorRef != null) {
                            throw new ComponentDefinitionException("Only one of " + ENCRYPTOR_REF_ATTRIBUTE + " attribute or " + ENCRYPTOR_ELEMENT + " element is allowed");
                        }
                        BeanMetadata encryptor = context.parseElement(BeanMetadata.class, metadata, e);
                        metadata.addProperty("encryptor", encryptor);
                    }
                }
            }
        }
        PlaceholdersUtils.validatePlaceholder(metadata, context.getComponentDefinitionRegistry());
        return metadata;
    }

    public String getId(ParserContext context, Element element) {
        if (element.hasAttribute(ID_ATTRIBUTE)) {
            return element.getAttribute(ID_ATTRIBUTE);
        } else {
            return generateId(context);
        }
    }

    private String generateId(ParserContext context) {
        String id;
        do {
            id = ".jaas-" + ++idCounter;
        } while (context.getComponentDefinitionRegistry().containsComponentDefinition(id));
        return id;
    }

    private static ValueMetadata createValue(ParserContext context, String value) {
        return createValue(context, value, null);
    }

    private static ValueMetadata createValue(ParserContext context, String value, String type) {
        MutableValueMetadata m = context.createMetadata(MutableValueMetadata.class);
        m.setStringValue(value);
        m.setType(type);
        return m;
    }

    private static CollectionMetadata createList(ParserContext context, List<String> list) {
        MutableCollectionMetadata m = context.createMetadata(MutableCollectionMetadata.class);
        m.setCollectionClass(List.class);
        m.setValueType(String.class.getName());
        for (String v : list) {
            m.addValue(createValue(context, v, String.class.getName()));
        }
        return m;
    }

    private RefMetadata createRef(ParserContext context, String value) {
        MutableRefMetadata r = context.createMetadata(MutableRefMetadata.class);
        r.setComponentId(value);
        return r;
    }

    private static String getTextValue(Element element) {
        StringBuilder value = new StringBuilder();
        NodeList nl = element.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if ((item instanceof CharacterData && !(item instanceof Comment)) || item instanceof EntityReference) {
                value.append(item.getNodeValue());
            }
        }
        return value.toString();
    }
}
