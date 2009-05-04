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
package org.apache.felix.karaf.gshell.core.config;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.w3c.dom.Element;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.osgi.service.exporter.support.OsgiServiceFactoryBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.geronimo.gshell.wisdom.registry.CommandLocationImpl;
import org.apache.geronimo.gshell.wisdom.command.ConfigurableCommandCompleter;
import org.apache.geronimo.gshell.wisdom.command.LinkImpl;
import org.apache.geronimo.gshell.wisdom.command.AliasImpl;
import org.apache.geronimo.gshell.wisdom.command.StatefulCommand;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.Link;
import org.apache.geronimo.gshell.command.Alias;
import org.apache.felix.karaf.gshell.core.OsgiCommandRegistry;
import org.apache.felix.karaf.gshell.core.BeanContainerAwareProcessor;

public class NamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser(CommandParser.COMMAND_BUNDLE, new CommandParser());
    }

    public static class CommandParser extends AbstractBeanDefinitionParser {

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

            new Builder(context).parseCommandBundle(element);
            // We need to return a valid bean
            BeanDefinitionBuilder dummy = BeanDefinitionBuilder.rootBeanDefinition(String.class);
            return dummy.getBeanDefinition();
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

            private void parseCommandBundle(final Element element) {
                assert element != null;

                log.trace("Parse command bundle; element; {}", element);

                if (!context.getRegistry().containsBeanDefinition("$beanContainerAwareProcessor")) {
                    BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(BeanContainerAwareProcessor.class);
                    context.getRegistry().registerBeanDefinition("$beanContainerAwareProcessor", builder.getBeanDefinition());
                }

                parseCommands(element);
                parseLinks(element);
                parseAliases(element);
            }

            //
            // <gshell:command>
            //

            private void parseCommands(final Element element) {
                assert element != null;

                log.trace("Parse commands; element; {}", element);

                List<Element> children = getChildElements(element, COMMAND);
                for (Element child : children) {
                    parseCommand(child);
                }
            }

            private void parseCommand(final Element element) {
                assert element != null;

                log.trace("Parse command; element; {}", element);

                BeanDefinitionBuilder command = BeanDefinitionBuilder.genericBeanDefinition(StatefulCommand.class);
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
                command.addPropertyValue(ACTION_ID, action.getBeanName());

                // Optional children elements

                child = getChildElement(element, DOCUMENTER);
                if (child != null) {
                    BeanDefinitionHolder holder = parseBeanDefinitionElement(child);
                    command.addPropertyValue(DOCUMENTER, holder.getBeanDefinition());
                } else {
                    BeanDefinition documenter = new GenericBeanDefinition();
                    documenter.setBeanClassName("org.apache.geronimo.gshell.wisdom.command.MessageSourceCommandDocumenter");
                    command.addPropertyValue(DOCUMENTER, documenter);
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
                } else {
                    BeanDefinition msgSource = new GenericBeanDefinition();
                    msgSource.setBeanClassName("org.apache.geronimo.gshell.wisdom.command.CommandMessageSource");
                    command.addPropertyValue(MESSAGES, msgSource);
                }

                //String id = resolveId(element, command.getBeanDefinition());
                //BeanDefinitionHolder holder = register(command.getBeanDefinition(), id);

                BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceFactoryBean.class);
                bd.addPropertyReference("bundleContext", "bundleContext");
                bd.addPropertyValue("target", command.getBeanDefinition());
                bd.addPropertyValue("interfaces", new Class[] { Command.class });
                Map<String,String> props = new HashMap<String,String>();
                props.put(OsgiCommandRegistry.NAME, name);
                bd.addPropertyValue("serviceProperties", props);
                BeanDefinition defSvc = bd.getBeanDefinition();
                String id = context.getReaderContext().generateBeanName(defSvc);
                BeanDefinitionHolder holder = new BeanDefinitionHolder(defSvc, id);
                registerBeanDefinition(holder, context.getRegistry());
                if (shouldFireEvents()) {
                    BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
                    postProcessComponentDefinition(componentDefinition);
                    context.registerComponent(componentDefinition);
                }
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

            private void parseLinks(final Element element) {
                assert element != null;

                log.trace("Parse links; element; {}", element);

                List<Element> children = getChildElements(element, LINK);

                for (Element child : children) {
                    String name = child.getAttribute(NAME);
                    String target = child.getAttribute(TARGET);

                    BeanDefinitionBuilder link = BeanDefinitionBuilder.genericBeanDefinition(LinkImpl.class);
                    link.addConstructorArgValue(name);
                    link.addConstructorArgValue(target);

                    BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceFactoryBean.class);
                    bd.addPropertyReference("bundleContext", "bundleContext");
                    bd.addPropertyValue("target", link.getBeanDefinition());
                    bd.addPropertyValue("interfaces", new Class[] { Link.class });
                    BeanDefinition defSvc = bd.getBeanDefinition();
                    String id = context.getReaderContext().generateBeanName(defSvc);
                    BeanDefinitionHolder holder = new BeanDefinitionHolder(defSvc, id);
                    registerBeanDefinition(holder, context.getRegistry());
                    if (shouldFireEvents()) {
                        BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
                        postProcessComponentDefinition(componentDefinition);
                        context.registerComponent(componentDefinition);
                    }

                }
            }

            //
            // <gshell:alias>
            //

            private void parseAliases(final Element element) {
                assert element != null;

                log.trace("Parse aliases; element; {}", element);

                List<Element> children = getChildElements(element, ALIAS);

                for (Element child : children) {
                    String name = child.getAttribute(NAME);
                    String aliasValue = child.getAttribute(ALIAS);

                    BeanDefinitionBuilder alias = BeanDefinitionBuilder.genericBeanDefinition(AliasImpl.class);
                    alias.addConstructorArgValue(name);
                    alias.addConstructorArgValue(aliasValue);

                    BeanDefinitionBuilder bd = BeanDefinitionBuilder.genericBeanDefinition(OsgiServiceFactoryBean.class);
                    bd.addPropertyReference("bundleContext", "bundleContext");
                    bd.addPropertyValue("target", alias.getBeanDefinition());
                    bd.addPropertyValue("interfaces", new Class[] { Alias.class });
                    BeanDefinition defSvc = bd.getBeanDefinition();
                    String id = context.getReaderContext().generateBeanName(defSvc);
                    BeanDefinitionHolder holder = new BeanDefinitionHolder(defSvc, id);
                    registerBeanDefinition(holder, context.getRegistry());
                    if (shouldFireEvents()) {
                        BeanComponentDefinition componentDefinition = new BeanComponentDefinition(holder);
                        postProcessComponentDefinition(componentDefinition);
                        context.registerComponent(componentDefinition);
                    }

                }
            }
        }
    }
}
