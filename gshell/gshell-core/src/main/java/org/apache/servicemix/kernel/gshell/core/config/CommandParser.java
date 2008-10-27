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
package org.apache.servicemix.kernel.gshell.core.config;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.ArrayList;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.geronimo.gshell.wisdom.command.ConfigurableCommandCompleter;
import org.apache.geronimo.gshell.wisdom.registry.CommandLocationImpl;
import org.apache.servicemix.kernel.gshell.core.CommandBundle;

public class CommandParser extends AbstractBeanDefinitionParser {

    public static final String ID = ID_ATTRIBUTE;

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

    public static final String TYPE = "type";

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

    @Override
    protected boolean shouldGenerateId() {
        return true;
    }

    @Override
    protected boolean shouldGenerateIdAsFallback() {
        return true;
    }

    protected AbstractBeanDefinition parseInternal(final Element element, final ParserContext context) {
        assert element != null;
        assert context != null;

        Builder builder = new Builder(context);
        BeanDefinitionBuilder plugin = builder.parseCommandBundle(element);
        return plugin.getBeanDefinition();
    }

    /**
     * Helper to deal with command type.
     */
    private enum CommandType
    {
        STATELESS,
        STATEFUL;

        public static CommandType parse(final String text) {
            assert text != null;

            return valueOf(text.toUpperCase());
        }

        public String getTemplateName() {
            return name().toLowerCase() + COMMAND_TEMPLATE_SUFFIX;
        }

        public void wire(final BeanDefinitionBuilder command, final BeanDefinitionHolder action) {
            assert command != null;
            assert action != null;

            switch (this) {
                case STATELESS:
                    command.addPropertyReference(ACTION, action.getBeanName());
                    break;

                case STATEFUL:
                    command.addPropertyValue(ACTION_ID, action.getBeanName());
                    break;
            }
        }
    }

    /**
     * Helper to build plugin related bean definitions.
     */
    private class Builder
    {
        private final Logger log = LoggerFactory.getLogger(getClass());

        private ParserContext context;

        public Builder(final ParserContext context) {
            assert context != null;

            this.context = context;
        }

        private String resolveId(final Element element, final BeanDefinition def) throws BeanDefinitionStoreException {
            assert element != null;
            assert def != null;

            if (shouldGenerateId()) {
                return context.getReaderContext().generateBeanName(def);
            }

            String id = element.getAttribute(ID_ATTRIBUTE);

            if (!StringUtils.hasText(id) && shouldGenerateIdAsFallback()) {
                id = context.getReaderContext().generateBeanName(def);
            }

            return id;
        }

        @SuppressWarnings({"unchecked"})
        private List<Element> getChildElements(final Element element, final String name) {
            assert element != null;
            assert name != null;

            return DomUtils.getChildElementsByTagName(element, name);
        }

        @SuppressWarnings({"unchecked"})
        private List<Element> getChildElements(final Element element, final String[] names) {
            assert element != null;
            assert names != null;

            return DomUtils.getChildElementsByTagName(element, names);
        }

        @SuppressWarnings({"unchecked"})
        private Element getChildElement(final Element element, final String name) {
            assert element != null;
            assert name != null;

            List<Element> elements = DomUtils.getChildElementsByTagName(element, name);
            if (elements != null && !elements.isEmpty()) {
                return elements.get(0);
            }
            return null;
        }

        private BeanDefinitionParserDelegate createBeanDefinitionParserDelegate(final Element element) {
            assert element != null;

            BeanDefinitionParserDelegate parser = new BeanDefinitionParserDelegate(context.getReaderContext());
            parser.initDefaults(element.getOwnerDocument().getDocumentElement());
            return parser;
        }

        private BeanDefinitionHolder parseBeanDefinitionElement(final Element element) {
            assert element != null;

            BeanDefinitionParserDelegate parser = createBeanDefinitionParserDelegate(element);
            return parser.parseBeanDefinitionElement(element);
        }

        private void parseAndApplyDescription(final Element element, final BeanDefinition def) {
            assert element != null;
            assert def != null;

            Element desc = getChildElement(element, DESCRIPTION);
            if (desc != null) {
                if (def instanceof AbstractBeanDefinition) {
                    ((AbstractBeanDefinition)def).setDescription(desc.getTextContent());
                }
            }
        }

        private void parseAndApplyDescription(final Element element, final BeanDefinitionBuilder builder) {
            assert element != null;
            assert builder != null;

            parseAndApplyDescription(element, builder.getRawBeanDefinition());
        }

        private BeanDefinitionHolder register(final BeanDefinitionHolder holder) {
            assert holder != null;

            registerBeanDefinition(holder, context.getRegistry());
            return holder;
        }

        private BeanDefinitionHolder register(final BeanDefinition def, final String id) {
            assert def != null;
            assert id != null;

            BeanDefinitionHolder holder = new BeanDefinitionHolder(def, id);
            return register(holder);
        }

        //
        // <gshell:command-bundle>
        //

        private BeanDefinitionBuilder parseCommandBundle(final Element element) {
            assert element != null;

            log.trace("Parse command bundle; element; {}", element);

            BeanDefinitionBuilder bundle = BeanDefinitionBuilder.rootBeanDefinition(CommandBundle.class);
            parseAndApplyDescription(element, bundle);

            //
            // TODO: Figure out how we can save the order of <gshell:command> and <gshell:link> so that 'help' displays them in the order they are defined
            //

            ManagedList commands = new ManagedList();

            commands.addAll(parseCommands(element));
            bundle.addPropertyValue(COMMANDS, commands);

            ManagedMap links = new ManagedMap();
            links.putAll(parseLinks(element));
            bundle.addPropertyValue(LINKS, links);

            ManagedMap aliases = new ManagedMap();
            aliases.putAll(parseAliases(element));
            bundle.addPropertyValue(ALIASES, aliases);

            return bundle;
        }

        //
        // <gshell:command>
        //

        private List<BeanDefinition> parseCommands(final Element element) {
            assert element != null;

            log.trace("Parse commands; element; {}", element);

            List<BeanDefinition> commands = new ArrayList<BeanDefinition>();

            List<Element> children = getChildElements(element, COMMAND);

            for (Element child : children) {
                BeanDefinitionBuilder command = parseCommand(child);
                commands.add(command.getBeanDefinition());
            }

            return commands;
        }

        private BeanDefinitionBuilder parseCommand(final Element element) {
            assert element != null;

            log.trace("Parse command; element; {}", element);

            CommandType type = CommandType.parse(element.getAttribute(TYPE));
            BeanDefinitionBuilder command = BeanDefinitionBuilder.childBeanDefinition(type.getTemplateName());
            parseAndApplyDescription(element, command);

            Element child;

            // Required children elements

            String name = element.getAttribute(NAME);
            BeanDefinition def = new GenericBeanDefinition();
            def.setBeanClassName(CommandLocationImpl.class.getName());
            def.getConstructorArgumentValues().addGenericArgumentValue(name);
            command.addPropertyValue(LOCATION, def);

            child = getChildElement(element, ACTION);
            BeanDefinitionHolder action = parseCommandAction(child);
            type.wire(command, action);

            // Optional children elements

            child = getChildElement(element, DOCUMENTER);
            if (child != null) {
                BeanDefinitionHolder holder = parseBeanDefinitionElement(child);
                command.addPropertyValue(DOCUMENTER, holder.getBeanDefinition());
            }

            child = getChildElement(element, COMPLETER);
            if (child != null) {
                BeanDefinitionHolder holder = parseBeanDefinitionElement(child);
                command.addPropertyValue(COMPLETER, holder.getBeanDefinition());
            }

            child = getChildElement(element, COMPLETERS);
            if (child != null) {
                BeanDefinitionBuilder completer = parseCommandCompleters(child);
                command.addPropertyValue(COMPLETER, completer.getBeanDefinition());
            }

            child = getChildElement(element, MESSAGE_SOURCE);
            if (child != null) {
                BeanDefinitionHolder holder = parseBeanDefinitionElement(child);
                command.addPropertyValue(MESSAGES, holder.getBeanDefinition());
            }

            //String id = resolveId(element, command.getBeanDefinition());
            //BeanDefinitionHolder holder = register(command.getBeanDefinition(), id);

            return command;
        }

        //
        // <gshell:completers>
        //

        private BeanDefinitionBuilder parseCommandCompleters(final Element element) {
            assert element != null;

            BeanDefinitionBuilder completer = BeanDefinitionBuilder.rootBeanDefinition(ConfigurableCommandCompleter.class);

            ManagedList completers = new ManagedList();

            List<Element> children = getChildElements(element, new String[] {BEAN, REF, NULL});

            for (Element child : children) {
                if (DomUtils.nodeNameEquals(child, BEAN)) {
                    BeanDefinitionHolder holder = parseBeanDefinitionElement(child);
                    // noinspection unchecked
                    completers.add(holder.getBeanDefinition());
                }
                else if (DomUtils.nodeNameEquals(child, REF)) {
                    BeanDefinitionParserDelegate parser = createBeanDefinitionParserDelegate(child);
                    RuntimeBeanReference ref = (RuntimeBeanReference) parser.parsePropertySubElement(child, completer.getRawBeanDefinition());
                    // noinspection unchecked
                    completers.add(ref);
                }
                else if (DomUtils.nodeNameEquals(child, NULL)) {
                    // noinspection unchecked
                    completers.add(null);
                }
            }

            completer.addConstructorArgValue(completers);

            return completer;
        }

        //
        // <gshell:action>
        //

        private BeanDefinitionHolder parseCommandAction(final Element element) {
            assert element != null;

            log.trace("Parse command action; element; {}", element);

            // Construct the action
            BeanDefinition action = parseBeanDefinitionElement(element).getBeanDefinition();

            // All actions are configured as prototypes
            action.setScope(PROTOTYPE);

            // Generate id and register the bean
            String id = resolveId(element, action);
            return register(action, id);
        }

        //
        // <gshell:link>
        //

        private Map<String,String> parseLinks(final Element element) {
            assert element != null;

            log.trace("Parse links; element; {}", element);

            Map<String,String> links = new LinkedHashMap<String,String>();

            List<Element> children = getChildElements(element, LINK);

            for (Element child : children) {
                String name = child.getAttribute(NAME);
                String target = child.getAttribute(TARGET);

                links.put(name, target);
            }

            return links;
        }

        //
        // <gshell:alias>
        //

        private Map<String,String> parseAliases(final Element element) {
            assert element != null;

            log.trace("Parse aliases; element; {}", element);

            Map<String,String> aliases = new LinkedHashMap<String,String>();

            List<Element> children = getChildElements(element, ALIAS);

            for (Element child : children) {
                String name = child.getAttribute(NAME);
                String alias = child.getAttribute(ALIAS);

                aliases.put(name, alias);
            }

            return aliases;
        }
    }
}
