/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.commands;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.basic.AbstractCommand;
import org.apache.karaf.shell.commands.converter.DefaultConverter;
import org.apache.karaf.shell.commands.converter.GenericType;
import org.apache.karaf.shell.commands.converter.ReifiedType;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;

/**
 *
 */
public class Commands {

    protected final List<CommandBuilder> commandBuilders = new ArrayList<CommandBuilder>();
    protected final List<CompleterBuilder> completerBuilders = new ArrayList<CompleterBuilder>();

    protected DefaultConverter converter;

    protected BundleContext context;

    public BundleContext getContext() {
        if (context == null) {
            context = FrameworkUtil.getBundle(getClass()).getBundleContext();
        }
        return context;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public DefaultConverter getConverter() {
        if (converter == null) {
            converter = new DefaultConverter(getContext().getBundle());
        }
        return converter;
    }

    public void setConverter(DefaultConverter converter) {
        this.converter = converter;
    }

    public void register() {
        for (CompleterBuilder builder : completerBuilders) {
            builder.register();
        }
        for (CommandBuilder builder : commandBuilders) {
            builder.register();
        }
    }

    public void unregister() {
        for (CommandBuilder commandBuilder : commandBuilders) {
            commandBuilder.unregister();
        }
        for (CompleterBuilder builder : completerBuilders) {
            builder.unregister();
        }
    }

    public CommandBuilder command() {
        CommandBuilder commandBuilder = new CommandBuilder();
        commandBuilders.add(commandBuilder);
        return commandBuilder;
    }

    public CommandBuilder command(Class<? extends Action> actionClass) {
        return command().action(actionClass);
    }

    public CompleterBuilder completer(Completer completer) {
        CompleterBuilder completerBuilder = new CompleterBuilder(completer);
        completerBuilders.add(completerBuilder);
        return completerBuilder;
    }

    public class CompleterBuilder {
        protected Completer completer;
        protected ServiceRegistration registration;

        public CompleterBuilder(Completer completer) {
            this.completer = completer;
        }

        public void register() {
            if (registration == null) {
                Hashtable<String, String> props = new Hashtable<String, String>();
                String[] classes = {
                        Completer.class.getName(),
                        completer.getClass().getName()
                };
                registration = getContext().registerService(classes, completer, props);
            }
        }

        protected void unregister() {
            if (registration != null) {
                registration.unregister();
            }
        }

    }

    public class CommandBuilder {
        protected Class<? extends Action> clazz;
        protected final List<Object> arguments = new ArrayList<Object>();
        protected final List<Object> properties = new ArrayList<Object>();
        protected ServiceRegistration registration;
        protected final List<Completer> argCompleters = new ArrayList<Completer>();
        protected final Map<String, Completer> optionCompleters = new HashMap<String, Completer>();
        protected final Map<String, String> serviceProperties = new HashMap<String, String>();

        public CommandBuilder action(Class<? extends Action> actionClass) {
            this.clazz = actionClass;
            return this;
        }

        public CommandBuilder arguments(Object... arguments) {
            this.arguments.addAll(Arrays.asList(arguments));
            return this;
        }

        public CommandBuilder properties(Object... properties) {
            this.properties.addAll(Arrays.asList(properties));
            return this;
        }

        public CommandBuilder argCompleter(Completer completer) {
            this.argCompleters.add(completer);
            return this;
        }

        public CommandBuilder optionCompleter(String option, Completer completer) {
            this.optionCompleters.put(option, completer);
            return this;
        }

        public CommandBuilder serviceProp(String key, String val) {
            this.serviceProperties.put(key, val);
            return this;
        }

        public void register() {
            if (registration == null) {
                Command cmd = getCommand();
                Hashtable<String, String> props = new Hashtable<String, String>();
                props.put(CommandProcessor.COMMAND_SCOPE, cmd.scope());
                props.put(CommandProcessor.COMMAND_FUNCTION, cmd.name());
                props.putAll(serviceProperties);
                String[] classes = {
                        Function.class.getName(),
                        CompletableFunction.class.getName(),
                        CommandWithAction.class.getName(),
                        AbstractCommand.class.getName()
                };
                registration = getContext().registerService(classes, new CommandWrapper(this), props);
            }
        }

        protected void unregister() {
            if (registration != null) {
                registration.unregister();
            }
        }

        private Action createNewAction() {
            try {
                Action action;
                // Instantiate action
                Map<Constructor, List<Object>> matches = findMatchingConstructors(clazz, arguments);
                if (matches.size() == 1) {
                    Map.Entry<Constructor, List<Object>> match = matches.entrySet().iterator().next();
                    action = (Action) match.getKey().newInstance(match.getValue().toArray());
                } else if (matches.size() == 0) {
                    throw new IllegalArgumentException("Unable to find a matching constructor on class " + clazz.getName() + " for arguments " + arguments + " when instanciating command " + getName());
                } else {
                    throw new IllegalArgumentException("Multiple matching constructors found on class " + clazz + " for arguments " + arguments + " when instanciating bean " + getName() + ": " + matches.keySet());
                }
                // Inject action
                for (Object prop : properties) {
                    Method setter = null;
                    for (Method method : clazz.getMethods()) {
                        if (method.getName().startsWith("set") && method.getReturnType() == void.class
                                && method.getParameterTypes().length == 1
                                && method.getParameterTypes()[0].isInstance(prop)) {
                            if (setter == null) {
                                setter = method;
                            } else {
                                throw new IllegalArgumentException("Property " + prop + " matches multiple setters on class " + clazz.getName());
                            }
                        }
                    }
                    if (setter == null) {
                        throw new IllegalArgumentException("Property " + prop + " has no matching setter on class " + clazz.getName());
                    }
                    setter.invoke(action, prop);
                }
                return action;
            } catch (Exception e) {
                throw new RuntimeException("Unable to create action", e);
            }
        }

        private String getName() {
            Command cmd = getCommand();
            return cmd.scope() + ":" + cmd.name();
        }

        private Command getCommand() {
            Command cmd = clazz.getAnnotation(Command.class);
            if (cmd == null)
            {
                throw new IllegalArgumentException("Action class is not annotated with @Command");
            }
            return cmd;
        }

    }

    public static class CommandWrapper extends AbstractCommand implements CompletableFunction {
        private final CommandBuilder builder;

        public CommandWrapper(CommandBuilder builder) {
            this.builder = builder;
        }

        @Override
        public org.apache.felix.gogo.commands.Action createNewAction() {
            return builder.createNewAction();
        }

        @Override
        public List<Completer> getCompleters() {
            return builder.argCompleters;
        }

        @Override
        public Map<String, Completer> getOptionalCompleters() {
            return builder.optionCompleters;
        }
    }

    private Map<Constructor, List<Object>> findMatchingConstructors(Class type, List<Object> args) {
        Map<Constructor, List<Object>> matches = new HashMap<Constructor, List<Object>>();
        // Get constructors
        List<Constructor> constructors = new ArrayList<Constructor>(Arrays.asList(type.getConstructors()));
        // Discard any signature with wrong cardinality
        for (Iterator<Constructor> it = constructors.iterator(); it.hasNext();) {
            if (it.next().getParameterTypes().length != args.size()) {
                it.remove();
            }
        }
        // Find a direct match with assignment
        if (matches.size() != 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                boolean found = true;
                List<Object> match = new ArrayList<Object>();
                for (int i = 0; i < args.size(); i++) {
                    ReifiedType argType = new GenericType(cns.getGenericParameterTypes()[i]);
                    //If the arg is an Unwrappered bean then we need to do the assignment check against the
                    //unwrappered bean itself.
                    Object arg = args.get(i);
                    Object argToTest = arg;
                    if (!DefaultConverter.isAssignable(argToTest, argType)) {
                        found = false;
                        break;
                    }
                    try {
                        match.add(getConverter().convert(arg, cns.getGenericParameterTypes()[i]));
                    } catch (Throwable t) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        // Find a direct match with conversion
        if (matches.size() != 1) {
            Map<Constructor, List<Object>> nmatches = new HashMap<Constructor, List<Object>>();
            for (Constructor cns : constructors) {
                boolean found = true;
                List<Object> match = new ArrayList<Object>();
                for (int i = 0; i < args.size(); i++) {
                    ReifiedType argType = new GenericType(cns.getGenericParameterTypes()[i]);
                    try {
                        Object val = getConverter().convert(args.get(i), argType);
                        match.add(val);
                    } catch (Throwable t) {
                        found = false;
                        break;
                    }
                }
                if (found) {
                    nmatches.put(cns, match);
                }
            }
            if (nmatches.size() > 0) {
                matches = nmatches;
            }
        }
        return matches;
    }

}
