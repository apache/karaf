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
package org.apache.felix.karaf.gshell.core;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Properties;

import org.apache.geronimo.gshell.command.Alias;
import org.apache.geronimo.gshell.command.Command;
import org.apache.geronimo.gshell.command.Link;
import org.apache.geronimo.gshell.registry.AliasRegistry;
import org.apache.geronimo.gshell.registry.CommandRegistry;
import org.apache.geronimo.gshell.wisdom.command.LinkCommand;
import org.apache.geronimo.gshell.wisdom.registry.CommandLocationImpl;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.osgi.context.BundleContextAware;

public class CommandBundle implements BundleContextAware, InitializingBean, DisposableBean, ApplicationContextAware {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private CommandRegistry commandRegistry;

    private AliasRegistry aliasRegistry;

    private BundleContext bundleContext;

    private List<Command> commands;

    private List<Link> links;

    private List<Alias> aliases;

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

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        assert links != null;

        this.links = links;
    }

    public List<Alias> getAliases() {
        return aliases;
    }

    public void setAliases(List<Alias> aliases) {
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
                for (Link link : links) {
                    log.debug("Registering link: {}", link.getName());
                    LinkCommand cmd = new LinkCommand(commandRegistry, link.getTarget());
                    cmd.setLocation(new CommandLocationImpl(link.getName()));
                    commandRegistry.registerCommand(cmd);
                }
            }
            if (aliases != null) {
                for (Alias alias : aliases) {
                    log.debug("Registering alias: {}", alias.getName());
                    aliasRegistry.registerAlias(alias.getName(), alias.getAlias());
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
                for (Link link : links) {
                    log.debug("Registering link: {}", link.getName());
                    registrations.add(bundleContext.registerService(Link.class.getName(), link, new Properties()));
                }
            }
            if (aliases != null) {
                for (Alias alias : aliases) {
                    log.debug("Registering alias: {}", alias.getName());
                    Dictionary props = new Properties();
                    registrations.add(bundleContext.registerService(Alias.class.getName(), alias, new Properties()));
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
