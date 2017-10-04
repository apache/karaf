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
package org.apache.karaf.shell.impl.console.osgi;

import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.runtime.Reflective;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Descriptor;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.api.console.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CommandTracker extends ServiceTracker<Object, List<Command>> {

    final SessionFactory sessionFactory;

    public CommandTracker(SessionFactory sessionFactory, BundleContext context) throws InvalidSyntaxException{
        super(context, createFilter(context), null);
        this.sessionFactory = sessionFactory;
    }

    static private Filter createFilter(BundleContext context) throws InvalidSyntaxException {
        return context.createFilter(String.format("(&(%s=*)(%s=*)(!(%s=%s))(!(%s=%s)))",
                CommandProcessor.COMMAND_SCOPE,
                CommandProcessor.COMMAND_FUNCTION,
                Constants.OBJECTCLASS,
                "org.apache.felix.gogo.commands.CommandWithAction",
                Constants.OBJECTCLASS,
                "org.apache.karaf.shell.commands.CommandWithAction"));
    }

    @Override
    public List<Command> addingService(final ServiceReference<Object> reference) {
        final String scope = reference.getProperty(CommandProcessor.COMMAND_SCOPE).toString();
        final Object function = reference.getProperty(CommandProcessor.COMMAND_FUNCTION);

        final List<String> names = new ArrayList<>();
        if (function.getClass().isArray()) {
            for (final Object f : ((Object[]) function)) {
                names.add(f.toString());
            }
        } else {
            names.add(function.toString());
        }

        List<Command> commands = new ArrayList<>();
        for (String name : names) {
            final Function target = new CommandProxy(context, reference, name);
            Command command = new Command() {
                @Override
                public String getScope() {
                    return scope;
                }

                @Override
                public String getName() {
                    return name;
                }

                @Override
                public String getDescription() {
                    for (Method method : getMethods()) {
                        Descriptor d = method.getAnnotation(Descriptor.class);
                        if (d != null) {
                            return d.value();
                        }
                    }
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

                private List<Method> getMethods() {
                    Object target = context.getService(reference);
                    List<Method> methods = new ArrayList<>();
                    String func = name.substring(name.indexOf(':') + 1).toLowerCase();
                    List<String> funcs = new ArrayList<>();
                    funcs.add("is" + func);
                    funcs.add("get" + func);
                    funcs.add("set" + func);
                    if (Reflective.KEYWORDS.contains(func)) {
                        funcs.add("_" + func);
                    } else {
                        funcs.add(func);
                    }
                    for (Method method : target.getClass().getMethods()) {
                        if (funcs.contains(method.getName().toLowerCase())) {
                            methods.add(method);
                        }
                    }
                    context.ungetService(reference);
                    return methods;
                }
            };
            sessionFactory.getRegistry().register(command);
            commands.add(command);
        }
        return commands;
    }

    @Override
    public void removedService(ServiceReference<Object> reference, List<Command> commands) {
        for (Command command : commands) {
            sessionFactory.getRegistry().unregister(command);
        }
    }
}
