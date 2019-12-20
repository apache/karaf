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
package org.apache.karaf.shell.console.commands;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.aries.blueprint.ParserContext;
import org.apache.aries.blueprint.PassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableBeanMetadata;
import org.apache.aries.blueprint.mutable.MutableCollectionMetadata;
import org.apache.aries.blueprint.mutable.MutableIdRefMetadata;
import org.apache.aries.blueprint.mutable.MutablePassThroughMetadata;
import org.apache.aries.blueprint.mutable.MutableRefMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.karaf.shell.console.SubShellAction;
import org.apache.karaf.shell.commands.Command;
import org.osgi.framework.Bundle;
import org.osgi.service.blueprint.container.ComponentDefinitionException;
import org.osgi.service.blueprint.reflect.BeanArgument;
import org.osgi.service.blueprint.reflect.BeanMetadata;
import org.osgi.service.blueprint.reflect.BeanProperty;
import org.osgi.service.blueprint.reflect.ComponentMetadata;
import org.osgi.service.blueprint.reflect.IdRefMetadata;
import org.osgi.service.blueprint.reflect.MapMetadata;
import org.osgi.service.blueprint.reflect.Metadata;
import org.osgi.service.blueprint.reflect.NullMetadata;
import org.osgi.service.blueprint.reflect.RefMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


@Deprecated
public class NamespaceHandler implements org.apache.aries.blueprint.NamespaceHandler {

    public static final String ID = "id";
    public static final String ACTION = "action";
    public static final String ACTION_ID = "actionId";
    public static final String COMMAND_BUNDLE = "command-bundle";
    public static final String SCAN = "scan";
    public static final String NAME = "name";
    public static final String COMMAND = "command";
    public static final String COMPLETERS = "completers";
    public static final String OPTIONAL_COMPLETERS = "optional-completers";
    public static final String OPTIONAL_COMPLETERS_PROPERTY = "optionalCompleters";
    public static final String BEAN = "bean";
    public static final String REF = "ref";
    public static final String NULL = "null";
    public static final String MAP = "map";
    public static final String BLUEPRINT_CONTAINER = "blueprintContainer";
    public static final String BLUEPRINT_CONVERTER = "blueprintConverter";

    public static final String SHELL_NAMESPACE_1_0_0 = "http://karaf.apache.org/xmlns/shell/v1.0.0";
    public static final String SHELL_NAMESPACE_1_1_0 = "http://karaf.apache.org/xmlns/shell/v1.1.0";

    private int nameCounter = 0;

    public URL getSchemaLocation(String namespace) {
        if(SHELL_NAMESPACE_1_0_0.equals(namespace)) {
            return getClass().getResource("karaf-shell-1.0.0.xsd");
        } else if(SHELL_NAMESPACE_1_1_0.equals(namespace)) {
            return getClass().getResource("karaf-shell-1.1.0.xsd");
        }
        return null;
    }

	public Set<Class> getManagedClasses() {
		return new HashSet<>(Collections.singletonList(
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
            registerConverters(context);
            return null;
        } else {
            throw new IllegalStateException("Unexpected element " + element.getNodeName());
        }
    }

    private Bundle getBundle(ParserContext context) {
        PassThroughMetadata ptm = (PassThroughMetadata) context.getComponentDefinitionRegistry().getComponentDefinition("blueprintBundle");
        return (Bundle) ptm.getObject();
    }

    private void parseChildElement(Element element, ParserContext context) {
        if (nodeNameEquals(element, COMMAND)) {
            parseCommand(element, context);
        }
    }

    private void registerConverters(ParserContext context) {
        String converterName = "." + NumberToStringConverter.class.getName();
        if (!context.getComponentDefinitionRegistry().containsComponentDefinition(converterName)) {
            MutablePassThroughMetadata cnv = context.createMetadata(MutablePassThroughMetadata.class);
            cnv.setId(converterName);
            cnv.setObject(new NumberToStringConverter());
            context.getComponentDefinitionRegistry().registerTypeConverter(cnv);
        }
    }

    private void parseCommand(Element element, ParserContext context) {
        MutableBeanMetadata command = context.createMetadata(MutableBeanMetadata.class);
        command.setRuntimeClass(BlueprintCommand.class);
        command.addProperty(BLUEPRINT_CONTAINER, createRef(context, BLUEPRINT_CONTAINER));
        command.addProperty(BLUEPRINT_CONVERTER, createRef(context, BLUEPRINT_CONVERTER));

        NodeList children = element.getChildNodes();
        MutableBeanMetadata action = null;
        for (int i = 0; i < children.getLength(); i++) {
            Node child  = children.item(i);
            if (child instanceof Element) {
                Element childElement = (Element) child;
                if (nodeNameEquals(childElement, ACTION)) {
                    action = parseAction(context, command, childElement);
                    action.setId(getName());
                    context.getComponentDefinitionRegistry().registerComponentDefinition(action);
                    command.addProperty(ACTION_ID, createIdRef(context, action.getId()));
                } else if (nodeNameEquals(childElement, COMPLETERS)) {
                    command.addProperty(COMPLETERS, parseCompleters(context, command, childElement));
                } else if (nodeNameEquals(childElement, OPTIONAL_COMPLETERS)) {
                    command.addProperty(OPTIONAL_COMPLETERS_PROPERTY, parseOptionalCompleters(context, command, childElement));
                }
                else {
                    throw new ComponentDefinitionException("Bad xml syntax: unknown element '" + childElement.getNodeName() + "'");
                }
            }
        }

        MutableServiceMetadata commandService = context.createMetadata(MutableServiceMetadata.class);
        commandService.setActivation(MutableServiceMetadata.ACTIVATION_LAZY);
        commandService.setId(getName());
        commandService.setAutoExport(ServiceMetadata.AUTO_EXPORT_INTERFACES);
        commandService.setServiceComponent(command);

        String scope;
        String function;
        if (SHELL_NAMESPACE_1_0_0.equals(element.getNamespaceURI())) {
            String location = element.getAttribute(NAME);
            location = location.replace('/', ':');
            if (location.lastIndexOf(':') >= 0) {
                scope = location.substring(0, location.lastIndexOf(':'));
                function = location.substring(location.lastIndexOf(':') + 1);
            } else {
                scope = "";
                function = location;
            }
        } else {
            try {
                Class actionClass = getBundle(context).loadClass(action.getClassName());
                scope = getScope(actionClass);
                function = getName(actionClass);
            } catch (Throwable e) {
                throw new ComponentDefinitionException("Unable to introspect action " + action.getClassName(), e);
            }
        }
        commandService.addServiceProperty(createStringValue(context, "osgi.command.scope"),
                createStringValue(context, scope));
        commandService.addServiceProperty(createStringValue(context, "osgi.command.function"),
                createStringValue(context, function));

        context.getComponentDefinitionRegistry().registerComponentDefinition(commandService);

        String subShellName = null;
        if (scope != null && !scope.isEmpty()) {
            // if it's shell 1.0.0 schema and scope is contained in the descriptor itself
            subShellName = ".subshell." + scope;
        }

        if (subShellName == null || !context.getComponentDefinitionRegistry().containsComponentDefinition(subShellName)) {
            // if the scope is unknown or if the scope has not been defined before
            createSubShell(context, scope, subShellName);
        }
    }

    private void createSubShell(ParserContext context, String scope, String subShellName) {
        if (context.getComponentDefinitionRegistry().containsComponentDefinition(subShellName)) {
            return;
        }
        MutableBeanMetadata subShellAction = context.createMetadata(MutableBeanMetadata.class);
        subShellAction.setRuntimeClass(SubShellAction.class);
        subShellAction.setActivation(MutableBeanMetadata.ACTIVATION_LAZY);
        subShellAction.setScope(MutableBeanMetadata.SCOPE_PROTOTYPE);
        subShellAction.setId(getName());
        subShellAction.addProperty("subShell", createStringValue(context, scope));
        context.getComponentDefinitionRegistry().registerComponentDefinition(subShellAction);
        // generate the sub-shell command
        MutableBeanMetadata subShellCommand = context.createMetadata(MutableBeanMetadata.class);
        subShellCommand.setId(getName());
        subShellCommand.setRuntimeClass(BlueprintCommand.class);
        subShellCommand.addProperty(BLUEPRINT_CONTAINER, createRef(context, BLUEPRINT_CONTAINER));
        subShellCommand.addProperty(BLUEPRINT_CONVERTER, createRef(context, BLUEPRINT_CONVERTER));
        subShellCommand.addProperty(ACTION_ID, createIdRef(context, subShellAction.getId()));
        context.getComponentDefinitionRegistry().registerComponentDefinition(subShellCommand);
        // generate the sub-shell OSGi service
        MutableServiceMetadata subShellCommandService = context.createMetadata(MutableServiceMetadata.class);
        subShellCommandService.setActivation(MutableServiceMetadata.ACTIVATION_LAZY);
        subShellCommandService.setId(subShellName);
        subShellCommandService.setAutoExport(ServiceMetadata.AUTO_EXPORT_INTERFACES);
        subShellCommandService.setServiceComponent(subShellCommand);
        subShellCommandService.addServiceProperty(createStringValue(context, "osgi.command.scope"), createStringValue(context, "*"));
        subShellCommandService.addServiceProperty(createStringValue(context, "osgi.command.function"), createStringValue(context, scope));
        context.getComponentDefinitionRegistry().registerComponentDefinition(subShellCommandService);
    }

    private MutableBeanMetadata getInvocationValue(ParserContext context, String method, String className) {
        MutableBeanMetadata scope = context.createMetadata(MutableBeanMetadata.class);
        scope.setRuntimeClass(NamespaceHandler.class);
        scope.setFactoryMethod(method);
        scope.addArgument(createStringValue(context, className), null, 0);
        return scope;
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

    private Metadata parseOptionalCompleters(ParserContext context, ComponentMetadata enclosingComponent, Element element) {
        Metadata metadata = context.parseElement(MapMetadata.class, context.getEnclosingComponent(), element);
        return metadata;
    }

    private Metadata parseCompleters(ParserContext context, ComponentMetadata enclosingComponent, Element element) {
        MutableCollectionMetadata collection = context.createMetadata(MutableCollectionMetadata.class);
        collection.setCollectionClass(List.class);
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
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

    public static String getScope(Class<?> action) {
        Command command = action.getAnnotation(Command.class);
        if (command != null) {
            return command.scope();
        }
        org.apache.felix.gogo.commands.Command command2 = action.getAnnotation(org.apache.felix.gogo.commands.Command.class);
        if (command2 != null) {
            return command2.scope();
        }
        return null;
    }

    public static String getName(Class<?> action) {
        Command command = action.getAnnotation(Command.class);
        if (command != null) {
            return command.name();
        }
        org.apache.felix.gogo.commands.Command command2 = action.getAnnotation(org.apache.felix.gogo.commands.Command.class);
        if (command2 != null) {
            return command2.name();
        }
        return null;
    }
}
