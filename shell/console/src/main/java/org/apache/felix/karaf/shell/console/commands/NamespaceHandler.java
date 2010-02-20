/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.felix.karaf.shell.console.commands;

import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.HashSet;
import java.util.Set;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableIdRefMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.felix.karaf.shell.console.CompletableFunction;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.command.Function;


public class NamespaceHandler implements org.apache.aries.blueprint.NamespaceHandler {

    public static final String ID = "id";
    public static final String DESCRIPTION = "description";
    public static final String PLUGIN_TEMPLATE = "pluginTemplate";
    public static final String ACTION = "action";
    public static final String ACTION_ID = "actionId";
    public static final String COMMAND_TEMPLATE_SUFFIX = "CommandTemplate";
    public static final String COMMAND_BUNDLE = "command-bundle";
    public static final String NAME = "name";
    public static final String LOCATION = "location";
    public static final String COMMANDS = "commands";
    public static final String COMMAND = "command";
    public static final String DOCUMENTER = "documenter";
    public static final String COMPLETER = "completer";
    public static final String COMPLETERS = "completers";
    public static final String BEAN = "bean";
    public static final String REF = "ref";
    public static final String NULL = "null";
    public static final String MESSAGE_SOURCE = "message-source";
    public static final String MESSAGES = "messages";
    public static final String PROTOTYPE = "prototype";
    public static final String ALIAS = "alias";
    public static final String ALIASES = "aliases";
    public static final String LINK = "link";
    public static final String LINKS = "links";
    public static final String TARGET = "target";
    public static final String BLUEPRINT_CONTAINER = "blueprintContainer";
    public static final String BLUEPRINT_CONVERTER = "blueprintConverter";

    private int nameCounter = 0;

    public URL getSchemaLocation(String namespace) {
        return getClass().getResource("karaf-shell.xsd");
    }

	public Set<Class> getManagedClasses() {
		return new HashSet<Class>(Arrays.asList(
			BlueprintCommand.class
		));
	}

    public ComponentMetadata decorate(Node node, ComponentMetadata component, ParserContext context) {
        throw new ComponentDefinitionException("Bad xml syntax: node decoration is not supported");
    }

    public Metadata parse(Element element, ParserContext context) {
        if (nodeNameEquals(element, COMMAND_BUNDLE)) {
            NodeList children = element.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child  = children.item(i);
                if (child instanceof Element) {
                    Element childElement = (Element) child;
                    parseChildElement(childElement, context);
                }
            }
            return null;
        } else {
            throw new IllegalStateException("Unexpected element " + element.getNodeName());
        }
    }

    private void parseChildElement(Element element, ParserContext context) {
        if (nodeNameEquals(element, COMMAND)) {
            parseCommand(element, context);
        } else if (nodeNameEquals(element, LINK)) {
//            parseLink(element, context);
        } else if (nodeNameEquals(element, ALIAS)) {
//            parseAlias(element, context);
        }
    }

    private void parseCommand(Element element, ParserContext context) {
        MutableBeanMetadata command = context.createMetadata(MutableBeanMetadata.class);
        command.setRuntimeClass(BlueprintCommand.class);
        command.addProperty(BLUEPRINT_CONTAINER, createRef(context, BLUEPRINT_CONTAINER));
        command.addProperty(BLUEPRINT_CONVERTER, createRef(context, BLUEPRINT_CONVERTER));
//        MutableBeanMetadata documenter = context.createMetadata(MutableBeanMetadata.class);
//        documenter.setRuntimeClass(MessageSourceCommandDocumenter.class);
//        command.addProperty(DOCUMENTER, documenter);
//        MutableBeanMetadata messages = context.createMetadata(MutableBeanMetadata.class);
//        messages.setRuntimeClass(CommandMessageSource.class);
//        command.addProperty(MESSAGES, messages);
//        MutableBeanMetadata location = context.createMetadata(MutableBeanMetadata.class);
//        location.setRuntimeClass(CommandLocationImpl.class);
//        location.addArgument(createStringValue(context, element.getAttribute(NAME)), String.class.getName(), 0);
//        command.addProperty(LOCATION, location);

        String location = element.getAttribute(NAME);
        location = location.replace('/', ':');
        String scope;
        String function;
        if (location.lastIndexOf(':') >= 0) {
            scope = location.substring(0, location.lastIndexOf(':'));
            function = location.substring(location.lastIndexOf(':') + 1);
        } else {
            scope = "";
            function = location;
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child  = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                if (nodeNameEquals(childElement, ACTION)) {
                    MutableBeanMetadata action = parseAction(context, command, childElement);
                    action.setId(getName());
                    context.getComponentDefinitionRegistry().registerComponentDefinition(action);
                    command.addProperty(ACTION_ID, createIdRef(context, action.getId()));
                } else if (nodeNameEquals(childElement, COMPLETERS)) {
                    command.addProperty(COMPLETERS, parseCompleters(context, command, childElement));
                } else {
                    throw new ComponentDefinitionException("Bad xml syntax: unknown element '" + childElement.getNodeName() + "'");
                }
            }
        }

        MutableServiceMetadata commandService = context.createMetadata(MutableServiceMetadata.class);
        commandService.setActivation(MutableServiceMetadata.ACTIVATION_LAZY);
        commandService.setId(getName());
        commandService.addInterface(Function.class.getName());
        commandService.addInterface(CompletableFunction.class.getName());
        commandService.setServiceComponent(command);
        commandService.addServiceProperty(createStringValue(context, "osgi.command.scope"),
                                          createStringValue(context, scope));
        commandService.addServiceProperty(createStringValue(context, "osgi.command.function"),
                                          createStringValue(context, function));
        context.getComponentDefinitionRegistry().registerComponentDefinition(commandService);
    }

    private MutableBeanMetadata parseAction(ParserContext context, ComponentMetadata enclosingComponent, Element element) {
        MutableBeanMetadata action = context.createMetadata(MutableBeanMetadata.class);
        action.setActivation(MutableBeanMetadata.ACTIVATION_LAZY);
        action.setScope(MutableBeanMetadata.SCOPE_PROTOTYPE);
        action.setClassName(element.getAttribute("class"));
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child  = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                if (nodeNameEquals(childElement, "argument")) {
                    action.addArgument(context.parseElement(BeanArgument.class, enclosingComponent, childElement));
                } else if (nodeNameEquals(childElement, "property")) {
                    action.addProperty(context.parseElement(BeanProperty.class, enclosingComponent, childElement));
                }
            }
        }
        return action;
    }

    private Metadata parseCompleters(ParserContext context, ComponentMetadata enclosingComponent, Element element) {
        MutableCollectionMetadata collection = context.createMetadata(MutableCollectionMetadata.class);
        collection.setCollectionClass(List.class);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child  = children.item(i);
            if (child instanceof Element) {
                Metadata metadata;
                if (nodeNameEquals(child, REF)) {
                    metadata = context.parseElement(RefMetadata.class, context.getEnclosingComponent(), (Element) child);
                } else if (nodeNameEquals(child, NULL)) {
                    metadata = context.parseElement(NullMetadata.class, context.getEnclosingComponent(), (Element) child);
                } else if (nodeNameEquals(child, BEAN)) {
                    metadata = context.parseElement(BeanMetadata.class, enclosingComponent, (Element) child);
                } else {
                    throw new IllegalStateException("Unexpected element " + child.getNodeName());
                }
                collection.addValue(metadata);
            }
        }
        return collection;
    }

//    private void parseLink(Element element, ParserContext context) {
//        MutableBeanMetadata link = context.createMetadata(MutableBeanMetadata.class);
//        link.setRuntimeClass(LinkImpl.class);
//        link.addArgument(createStringValue(context, element.getAttribute(NAME)), String.class.getName(), 0);
//        link.addArgument(createStringValue(context, element.getAttribute(TARGET)), String.class.getName(), 0);
//
//        MutableServiceMetadata linkService = context.createMetadata(MutableServiceMetadata.class);
//        linkService.setId(getName());
//        linkService.addInterface(Link.class.getName());
//        linkService.setServiceComponent(link);
//        context.getComponentDefinitionRegistry().registerComponentDefinition(linkService);
//    }
//
//    private void parseAlias(Element element, ParserContext context) {
//        MutableBeanMetadata alias = context.createMetadata(MutableBeanMetadata.class);
//        alias.setRuntimeClass(AliasImpl.class);
//        alias.addArgument(createStringValue(context, element.getAttribute(NAME)), String.class.getName(), 0);
//        alias.addArgument(createStringValue(context, element.getAttribute(ALIAS)), String.class.getName(), 0);
//
//        MutableServiceMetadata aliasService = context.createMetadata(MutableServiceMetadata.class);
//        aliasService.setId(getName());
//        aliasService.addInterface(Alias.class.getName());
//        aliasService.setServiceComponent(alias);
//        context.getComponentDefinitionRegistry().registerComponentDefinition(aliasService);
//    }
//
    private ValueMetadata createStringValue(ParserContext context, String str) {
        MutableValueMetadata value = context.createMetadata(MutableValueMetadata.class);
        value.setStringValue(str);
        return value;
    }

    private RefMetadata createRef(ParserContext context, String id) {
        MutableRefMetadata idref = context.createMetadata(MutableRefMetadata.class);
        idref.setComponentId(id);
        return idref;
    }

    private IdRefMetadata createIdRef(ParserContext context, String id) {
        MutableIdRefMetadata idref = context.createMetadata(MutableIdRefMetadata.class);
        idref.setComponentId(id);
        return idref;
    }

    public synchronized String getName() {
        return "shell-" + ++nameCounter;
    }

    private static boolean nodeNameEquals(Node node, String name) {
        return (name.equals(node.getNodeName()) || name.equals(node.getLocalName()));
    }
}
