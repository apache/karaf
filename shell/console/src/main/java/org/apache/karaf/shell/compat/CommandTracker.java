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
package org.apache.karaf.shell.compat;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

public class CommandTracker implements ServiceTrackerCustomizer<Object, Object> {

    SessionFactory sessionFactory;
    BundleContext context;
    ServiceTracker<?, ?> tracker;

    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void init() throws Exception {
        Filter filter = context.createFilter(String.format("(&(%s=*)(%s=*))",
                CommandProcessor.COMMAND_SCOPE, CommandProcessor.COMMAND_FUNCTION));
        this.tracker = new ServiceTracker<Object, Object>(context, filter, this);
        this.tracker.open();
    }

    public void destroy() {
        tracker.close();
    }

    @Override
    public Object addingService(final ServiceReference reference) {
        Object service = context.getService(reference);
        if (service instanceof CommandWithAction) {
            final CommandWithAction oldCommand = (CommandWithAction) service;
            final org.apache.karaf.shell.api.console.Command command = new org.apache.karaf.shell.api.console.Command() {
                @Override
                public String getScope() {
                    return reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
                }

                @Override
                public String getName() {
                    return reference.getProperty(CommandProcessor.COMMAND_FUNCTION).toString();
                }

                @Override
                public String getDescription() {
                    final Command cmd = oldCommand.getActionClass().getAnnotation(Command.class);
                    if (cmd != null) {
                        return cmd.description();
                    } else {
                        return getName();
                    }
                }

                @Override
                public Completer getCompleter(final boolean scoped) {
                    final ArgumentCompleter completer = new ArgumentCompleter(oldCommand, getScope(), getName(), scoped);
                    return new Completer() {
                        @Override
                        public int complete(Session session, CommandLine commandLine, List<String> candidates) {
                            return completer.complete(session, commandLine, candidates);
                        }
                    };
                }

                @Override
                public Parser getParser() {
                    return null;
                }

                @Override
                public Object execute(Session session, List<Object> arguments) throws Exception {
                    // TODO: remove not really nice cast
                    CommandSession commandSession = (CommandSession) session.get(".commandSession");
                    return oldCommand.execute(commandSession, arguments);
                }
            };
            sessionFactory.getRegistry().register(command);
            return command;
        } else if (service instanceof org.apache.felix.gogo.commands.CommandWithAction) {
            final org.apache.felix.gogo.commands.CommandWithAction oldCommand = (org.apache.felix.gogo.commands.CommandWithAction) service;
            final org.apache.karaf.shell.api.console.Command command = new org.apache.karaf.shell.api.console.Command() {
                @Override
                public String getScope() {
                    return reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
                }

                @Override
                public String getName() {
                    return reference.getProperty(CommandProcessor.COMMAND_FUNCTION).toString();
                }

                @Override
                public String getDescription() {
                    final org.apache.felix.gogo.commands.Command cmd = oldCommand.getActionClass().getAnnotation(org.apache.felix.gogo.commands.Command.class);
                    if (cmd != null) {
                        return cmd.description();
                    } else {
                        return getName();
                    }
                }

                @Override
                public Completer getCompleter(final boolean scoped) {
                    final OldArgumentCompleter completer = new OldArgumentCompleter(oldCommand, getScope(), getName(), scoped);
                    return new Completer() {
                        @Override
                        public int complete(Session session, CommandLine commandLine, List<String> candidates) {
                            return completer.complete(session, commandLine, candidates);
                        }
                    };
                }

                @Override
                public Parser getParser() {
                    return null;
                }

                @Override
                public Object execute(Session session, List<Object> arguments) throws Exception {
                    // TODO: remove not really nice cast
                    CommandSession commandSession = (CommandSession) session.get(".commandSession");
                    return oldCommand.execute(commandSession, arguments);
                }
            };
            sessionFactory.getRegistry().register(command);
            return command;
        } else {
            final String scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
            final Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);

            List<org.apache.karaf.shell.api.console.Command> commands = new ArrayList<>();

            if (function.getClass().isArray()) {
                for (final Object f : ((Object[]) function)) {
                    final Function target;

                    target = new CommandProxy(context, reference, f.toString());
                    org.apache.karaf.shell.api.console.Command command = new org.apache.karaf.shell.api.console.Command() {
                        @Override
                        public String getScope() {
                            return scope;
                        }

                        @Override
                        public String getName() {
                            return f.toString();
                        }

                        @Override
                        public String getDescription() {
                            Object property = reference.getProperty("osgi.command.description");
                            if (property != null) {
                                return property.toString();
                            } else {
                                return getName();
                            }
                        }

                        @Override
                        public Completer getCompleter(final boolean scoped) {
                            return null;
                        }

                        @Override
                        public Parser getParser() {
                            return null;
                        }

                        @Override
                        public Object execute(Session session, List<Object> arguments) throws Exception {
                            // TODO: remove not really nice cast
                            CommandSession commandSession = (CommandSession) session.get(".commandSession");
                            return target.execute(commandSession, arguments);
                        }
                    };
                    sessionFactory.getRegistry().register(command);
                    commands.add(command);
                }
            } else {
                final Function target = new CommandProxy(context, reference, function.toString());
                org.apache.karaf.shell.api.console.Command command = new org.apache.karaf.shell.api.console.Command() {
                    @Override
                    public String getScope() {
                        return scope;
                    }

                    @Override
                    public String getName() {
                        return function.toString();
                    }

                    @Override
                    public String getDescription() {
                        return reference.getProperty("osgi.command.description").toString();
                    }

                    @Override
                    public Completer getCompleter(final boolean scoped) {
                        return null;
                    }

                    @Override
                    public Parser getParser() {
                        return null;
                    }

                    @Override
                    public Object execute(Session session, List<Object> arguments) throws Exception {
                        // TODO: remove not really nice cast
                        CommandSession commandSession = (CommandSession) session.get(".commandSession");
                        return target.execute(commandSession, arguments);
                    }
                };
                sessionFactory.getRegistry().register(command);
                commands.add(command);
            }
            return commands;
        }
    }

    @Override
    public void modifiedService(ServiceReference reference, Object service) {
    }

    @Override
    public void removedService(ServiceReference reference, Object service) {
        if (service instanceof org.apache.karaf.shell.api.console.Command) {
            sessionFactory.getRegistry().unregister(service);
        }
        if (service instanceof List) {
            List<org.apache.karaf.shell.api.console.Command> commands = (List<org.apache.karaf.shell.api.console.Command>) service;
            for (org.apache.karaf.shell.api.console.Command command : commands) {
                sessionFactory.getRegistry().unregister(command);
            }
        }
        context.ungetService(reference);
    }
}
