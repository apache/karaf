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
package org.apache.servicemix.kernel.gshell.core;

import java.util.Map;
import java.util.Dictionary;
import java.util.Properties;
import java.util.List;
import java.util.ArrayList;

import org.springframework.osgi.context.BundleContextAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.registry.AliasRegistry;
import org.apache.geronimo.gshell.wisdom.command.CommandSupport;
import org.apache.geronimo.gshell.wisdom.command.LinkCommand;
import org.apache.geronimo.gshell.wisdom.registry.CommandLocationImpl;
import org.apache.geronimo.gshell.spring.BeanContainerAware;
import org.apache.geronimo.gshell.spring.BeanContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandBundle implements BundleContextAware, InitializingBean, DisposableBean, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private CommandRegistry commandRegistry;

    private AliasRegistry aliasRegistry;

    private BundleContext bundleContext;

    private List<Command> commands;

    private Map<String,String> links;

    private Map<String,String> aliases;

    private ApplicationContext applicationContext;

    private List<ServiceRegistration> registrations = new ArrayList<ServiceRegistration>();

    public CommandBundle() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(final List<Command> commands) {
        assert commands != null;

        this.commands = commands;
    }

    public Map<String, String> getLinks() {
        return links;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public Map<String, String> getAliases() {
        return aliases;
    }

    public void setAliases(final Map<String, String> aliases) {
        assert aliases != null;

        this.aliases = aliases;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    public void afterPropertiesSet() throws Exception {
        log.debug("Initializing command bundle");
        if (commandRegistry == null) {
            String[] names = applicationContext.getBeanNamesForType(CommandRegistry.class);
            if (names.length == 1) {
                commandRegistry = (CommandRegistry) applicationContext.getBean(names[0], CommandRegistry.class);
            }
        }
        if (aliasRegistry == null) {
            String[] names = applicationContext.getBeanNamesForType(AliasRegistry.class);
            if (names.length == 1) {
                aliasRegistry = (AliasRegistry) applicationContext.getBean(names[0], AliasRegistry.class);
            }
        }
        if (commandRegistry != null && aliasRegistry != null) {
            log.debug("Command bundle is using the auto wired command/alias registry");
            if (commands != null) {
                for (Command command : commands) {
                    log.debug("Registering command: {}", command.getLocation());
                    commandRegistry.registerCommand(command);
                }
            }
            if (links != null) {
                for (String name : links.keySet()) {
                    log.debug("Registering link: {}", name);
                    LinkCommand link = new LinkCommand(commandRegistry, links.get(name));
                    link.setLocation(new CommandLocationImpl(name));
                    commandRegistry.registerCommand(link);
                }
            }
            if (aliases != null) {
                for (String name : aliases.keySet()) {
                    log.debug("Registering alias: {}", name);
                    aliasRegistry.registerAlias(name, aliases.get(name));
                }
            }
        } else if (bundleContext != null) {
            log.debug("Command bundle is using the OSGi registry");
            if (commands != null) {
                for (Command command : commands) {
                    log.debug("Registering command: {}", command.getLocation());
                    Dictionary props = new Properties();
                    props.put(OsgiCommandRegistry.NAME, command.getLocation().getFullPath());
                    registrations.add(bundleContext.registerService(Command.class.getName(), command, props));
                }
            }
            if (links != null) {
                for (String name : links.keySet()) {
                    log.debug("Registering link: {}", name);
                    Dictionary props = new Properties();
                    props.put(OsgiCommandRegistry.NAME, name);
                    props.put(OsgiCommandRegistry.TARGET, links.get(name));
                    registrations.add(bundleContext.registerService(Link.class.getName(), new Link() {}, props));
                }
            }
            if (aliases != null) {
                for (String name : aliases.keySet()) {
                    log.debug("Registering alias: {}", name);
                    Dictionary props = new Properties();
                    props.put(OsgiAliasRegistry.NAME, name);
                    props.put(OsgiAliasRegistry.ALIAS, aliases.get(name));
                    registrations.add(bundleContext.registerService(Alias.class.getName(), new Alias() {}, props));
                }
            }
        } else {
            throw new Exception("Command bundle should be wired to the command/alias registry or be used in an OSGi context");
        }
    }

    public void destroy() {
        log.debug("Destroying command bundle");
        for (ServiceRegistration reg : registrations) {
            reg.unregister();
        }
    }

}
