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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
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
import org.apache.aries.blueprint.mutable.MutableReferenceMetadata;
import org.apache.aries.blueprint.mutable.MutableServiceMetadata;
import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.apache.felix.gogo.commands.Action;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.SubShellAction;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.inject.Destroy;
import org.apache.karaf.shell.inject.Init;
import org.apache.karaf.shell.inject.Reference;
import org.apache.karaf.shell.inject.Service;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.wiring.BundleWiring;
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
import org.osgi.service.blueprint.reflect.ReferenceMetadata;
import org.osgi.service.blueprint.reflect.ServiceMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


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
    public static final String SHELL_NAMESPACE_1_2_0 = "http://karaf.apache.org/xmlns/shell/v1.2.0";

    private int nameCounter = 0;

    public URL getSchemaLocation(String namespace) {
        if(SHELL_NAMESPACE_1_0_0.equals(namespace)) {
            return getClass().getResource("karaf-shell-1.0.0.xsd");
        } else if(SHELL_NAMESPACE_1_1_0.equals(namespace)) {
            return getClass().getResource("karaf-shell-1.1.0.xsd");
        } else if(SHELL_NAMESPACE_1_2_0.equals(namespace)) {
            return getClass().getResource("karaf-shell-1.2.0.xsd");
        }
        return getClass().getResource("karaf-shell-1.2.0.xsd");
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
            NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node child = attrs.item(i);
                if (child instanceof Attr) {
                    Attr childAttr = (Attr) child;
                    parseChildAttr(childAttr, context);
                }
            }
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

    private void parseChildAttr(Attr attr, ParserContext context) {
        if (nodeNameEquals(attr, SCAN)) {
            scan(attr, context);
        }
    }

    private void scan(Attr attr, ParserContext context) {
        try {
            Bundle bundle = getBundle(context);
            BundleWiring wiring = bundle.adapt(BundleWiring.class);
            for (String pkg : attr.getValue().split(" ")) {
                String name = pkg;
                int options = BundleWiring.LISTRESOURCES_LOCAL;
                name = name.replace('.', '/');
                if (name.endsWith("*")) {
                    options |= BundleWiring.LISTRESOURCES_RECURSE;
                    name = name.substring(0, name.length() - 1);
                }
                if (!name.startsWith("/")) {
                    name = "/" + name;
                }
                if (name.endsWith("/")) {
                    name = name.substring(0, name.length() - 1);
                }
                Collection<String> classes = wiring.listResources(name, "*.class", options);
                for (String className : classes) {
                    className = className.replace('/', '.').replace(".class", "");
                    inspectClass(context, bundle.loadClass(className));
                }
            }
        } catch (ComponentDefinitionException e) {
            throw e;
        } catch (Exception e) {
            throw new ComponentDefinitionException("Unable to scan commands", e);
        }
    }

    private void inspectClass(ParserContext context, Class<?> clazz) throws Exception {
        Service reg = clazz.getAnnotation(Service.class);
        if (reg == null) {
            return;
        }
        if (Action.class.isAssignableFrom(clazz)) {
            final Command cmd = clazz.getAnnotation(Command.class);
            if (cmd == null) {
                throw new IllegalArgumentException("Command " + clazz.getName() + " is not annotated with @Command");
            }
            String scope = cmd.scope();
            String function = cmd.name();
            // Create action
            MutableBeanMetadata action = context.createMetadata(MutableBeanMetadata.class);
            action.setId(getName());
            action.setActivation(MutableBeanMetadata.ACTIVATION_LAZY);
            action.setScope(MutableBeanMetadata.SCOPE_PROTOTYPE);
            action.setRuntimeClass(clazz);
            for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        if (field.getType() == BundleContext.class) {
                            action.addProperty(field.getName(), createRef(context, "blueprintBundleContext"));
                        } else {
                            action.addProperty(field.getName(), createRef(context, createServiceRef(context, field.getType()).getId()));
                        }
                    }
                }
                for (Method method : cl.getDeclaredMethods()) {
                    if (method.getAnnotation(Init.class) != null) {
                        if (action.getInitMethod() == null) {
                            action.setInitMethod(method.getName());
                        }
                    }
                    if (method.getAnnotation(Destroy.class) != null) {
                        if (action.getDestroyMethod() == null) {
                            action.setDestroyMethod(method.getName());
                        }
                    }
                }
            }
            context.getComponentDefinitionRegistry().registerComponentDefinition(action);
            // Create command
            MutableBeanMetadata command = context.createMetadata(MutableBeanMetadata.class);
            command.setRuntimeClass(BlueprintCommand.class);
            command.addProperty(BLUEPRINT_CONTAINER, createRef(context, BLUEPRINT_CONTAINER));
            command.addProperty(BLUEPRINT_CONVERTER, createRef(context, BLUEPRINT_CONVERTER));
            command.addProperty(ACTION_ID, createIdRef(context, action.getId()));
            // Create command service
            MutableServiceMetadata commandService = context.createMetadata(MutableServiceMetadata.class);
            commandService.setActivation(MutableServiceMetadata.ACTIVATION_LAZY);
            commandService.setId(getName());
            commandService.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
            commandService.setServiceComponent(command);
            commandService.addServiceProperty(createStringValue(context, "osgi.command.scope"),
                    createStringValue(context, scope));
            commandService.addServiceProperty(createStringValue(context, "osgi.command.function"),
                    createStringValue(context, function));
            context.getComponentDefinitionRegistry().registerComponentDefinition(commandService);

            // create the sub-shell action
            createSubShell(context, scope);
        }
        if (Completer.class.isAssignableFrom(clazz)) {
            MutableBeanMetadata completer = context.createMetadata(MutableBeanMetadata.class);
            completer.setId(getName());
            completer.setActivation(MutableBeanMetadata.ACTIVATION_LAZY);
            completer.setScope(MutableBeanMetadata.SCOPE_SINGLETON);
            completer.setRuntimeClass(clazz);
            // Create completer
            for (Class<?> cl = clazz; cl != Object.class; cl = cl.getSuperclass()) {
                for (Field field : cl.getDeclaredFields()) {
                    if (field.getAnnotation(Reference.class) != null) {
                        if (field.getType() == BundleContext.class) {
                            completer.addProperty(field.getName(), createRef(context, "blueprintBundleContext"));
                        } else {
                            completer.addProperty(field.getName(), createRef(context, createServiceRef(context, field.getType()).getId()));
                        }
                    }
                }
                for (Method method : cl.getDeclaredMethods()) {
                    if (method.getAnnotation(Init.class) != null) {
                        if (completer.getInitMethod() == null) {
                            completer.setInitMethod(method.getName());
                        }
                    }
                    if (method.getAnnotation(Destroy.class) != null) {
                        if (completer.getDestroyMethod() == null) {
                            completer.setDestroyMethod(method.getName());
                        }
                    }
                }
            }
            context.getComponentDefinitionRegistry().registerComponentDefinition(completer);
            // Create completer service
            MutableServiceMetadata completerService = context.createMetadata(MutableServiceMetadata.class);
            completerService.setActivation(MutableServiceMetadata.ACTIVATION_LAZY);
            completerService.setId(getName());
            completerService.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
            completerService.setServiceComponent(createRef(context, completer.getId()));
            context.getComponentDefinitionRegistry().registerComponentDefinition(completerService);
        }
    }

    private ComponentMetadata createServiceRef(ParserContext context, Class<?> cls) {
        String id = ".serviceref." + cls.getName();
        ComponentMetadata metadata = context.getComponentDefinitionRegistry().getComponentDefinition(id);
        if (metadata == null) {
            MutableReferenceMetadata m = context.createMetadata(MutableReferenceMetadata.class);
            m.setRuntimeInterface(cls);
            m.setInterface(cls.getName());
            m.setActivation(ReferenceMetadata.ACTIVATION_EAGER);
            m.setAvailability(ReferenceMetadata.AVAILABILITY_MANDATORY);
            m.setId(id);
            context.getComponentDefinitionRegistry().registerComponentDefinition(m);
            return m;
        } else {
            return metadata;
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
        commandService.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
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

        // create the sub-shell action
        createSubShell(context, scope);
    }

    private void createSubShell(ParserContext context, String scope) {
        String subShellName = ".subshell." + scope;
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
        subShellCommandService.setAutoExport(ServiceMetadata.AUTO_EXPORT_ALL_CLASSES);
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
        Metadata metadata = context.parseElement(MapMetadata.class, context.getEnclosingComponent(), (Element) element);
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
