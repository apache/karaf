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
package org.apache.karaf.jaas.blueprint.config.impl;

import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.karaf.jaas.boot.ProxyLoginModule;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.karaf.jaas.config.KeystoreInstance;
import org.apache.karaf.jaas.config.impl.Config;
import org.apache.karaf.jaas.config.impl.Module;
import org.apache.karaf.jaas.config.impl.ResourceKeystoreInstance;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
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

    public URL getSchemaLocation(String namespace) {
        switch (namespace) {
            case "http://karaf.apache.org/xmlns/jaas/v1.0.0":
                return getClass().getResource("/org/apache/karaf/jaas/blueprint/config/karaf-jaas-1.0.0.xsd");
            case "http://karaf.apache.org/xmlns/jaas/v1.1.0":
                return getClass().getResource("/org/apache/karaf/jaas/blueprint/config/karaf-jaas-1.1.0.xsd");
            default:
                return null;
        }
    }

    public Set<Class> getManagedClasses() {
        return new HashSet<>(Arrays.asList(
                Config.class,
                ResourceKeystoreInstance.class
        ));
    }

    public Metadata parse(Element element, ParserContext context) {
        String name = element.getLocalName() != null ? element.getLocalName() : element.getNodeName();
        if ("config".equals(name)) {
            return parseConfig(element, context);
        } else if ("keystore".equals(name)) {
            return parseKeystore(element, context);
        }
        throw new ComponentDefinitionException("Bad xml syntax: unknown element '" + name + "'");
    }

    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        throw new ComponentDefinitionException("Bad xml syntax: node decoration is not supported");
    }

    public ComponentMetadata parseConfig(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
        bean.setRuntimeClass(Config.class);
        String name = element.getAttribute("name");
        bean.addProperty("bundleContext", createRef(context, "blueprintBundleContext"));
        bean.addProperty("name", createValue(context, name));
        String rank = element.getAttribute("rank");
        if (rank != null && rank.length() > 0) {
            bean.addProperty("rank", createValue(context, rank));
        }
        NodeList childElements = element.getElementsByTagNameNS(element.getNamespaceURI(), "module");
        if (childElements != null && childElements.getLength() > 0) {
            MutableCollectionMetadata children = context.createMetadata(MutableCollectionMetadata.class);
            for (int i = 0; i < childElements.getLength(); ++i) {
                Element childElement = (Element) childElements.item(i);
                MutableBeanMetadata md = context.createMetadata(MutableBeanMetadata.class);
                md.setRuntimeClass(Module.class);
                md.addProperty("className", createValue(context, childElement.getAttribute("className")));
                if (childElement.getAttribute("name") != null) {
                    md.addProperty("name", createValue(context, childElement.getAttribute("name")));
                }
                if (childElement.getAttribute("flags") != null) {
                    md.addProperty("flags", createValue(context, childElement.getAttribute("flags")));
                }
                String options = getTextValue(childElement);
                if (options != null && options.length() > 0) {
                    md.addProperty("options", createValue(context, options));
                }
                children.addValue(md);
            }
            bean.addProperty("modules", children);
        }
        // Publish Config
        MutableServiceMetadata service = context.createMetadata(MutableServiceMetadata.class);
        service.setId(name);
        service.setServiceComponent(bean);
        service.addInterface(JaasRealm.class.getName());
        service.addServiceProperty(createValue(context, ProxyLoginModule.PROPERTY_MODULE), createValue(context, name));
        return service;
    }

    public ComponentMetadata parseKeystore(Element element, ParserContext context) {
        MutableBeanMetadata bean = context.createMetadata(MutableBeanMetadata.class);
        bean.setRuntimeClass(ResourceKeystoreInstance.class);
        // Parse name
        String name = element.getAttribute("name");
        bean.addProperty("name", createValue(context, name));
        // Parse rank
        String rank = element.getAttribute("rank");
        if (rank != null && rank.length() > 0) {
            bean.addProperty("rank", createValue(context, rank));
        }
        // Parse path
        String path = element.getAttribute("path");
        if (path != null && path.length() > 0) {
            bean.addProperty("path", createValue(context, path));
        }
        // Parse keystorePassword
        String keystorePassword = element.getAttribute("keystorePassword");
        if (keystorePassword != null && keystorePassword.length() > 0) {
            bean.addProperty("keystorePassword", createValue(context, keystorePassword));
        }
        // Parse keyPasswords
        String keyPasswords = element.getAttribute("keyPasswords");
        if (keyPasswords != null && keyPasswords.length() > 0) {
            bean.addProperty("keyPasswords", createValue(context, keyPasswords));
        }
        // Publish Config
        MutableServiceMetadata service = context.createMetadata(MutableServiceMetadata.class);
        service.setId(name);
        service.setServiceComponent(bean);
        service.addInterface(KeystoreInstance.class.getName());
        return service;
    }

    private ValueMetadata createValue(ParserContext context, String value) {
        MutableValueMetadata v = context.createMetadata(MutableValueMetadata.class);
        v.setStringValue(value);
        return v;
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
