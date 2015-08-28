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
package org.apache.karaf.shell.console.completer;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.jline.CommandSessionHolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Like the {@link org.apache.karaf.shell.console.completer.CommandsCompleter} but does not use OSGi but is
 * instead used from the non-OSGi {@link org.apache.karaf.shell.console.Main}
 */
public class CommandsCompleter implements Completer {

    public static final String COMMANDS = ".commands";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsCompleter.class);

    private CommandSession session;
    private final List<Completer> completers = new ArrayList<Completer>();
    private final Set<String> commands = new HashSet<String>();
    private CommandTracker tracker;

    public CommandsCompleter() {
        this(CommandSessionHolder.getSession());
    }

    public CommandsCompleter(CommandSession session) {
        this.session = session;
        try {
            tracker = new CommandTracker();
        } catch (Throwable t) {
            // Ignore in case we're not in OSGi
        }
    }

    public void dispose() {
        if (tracker != null) {
            tracker.dispose();
        }
    }
    
    public int complete(String buffer, int cursor, List<String> candidates) {
        if (session == null) {
            session = CommandSessionHolder.getSession();
        }
        checkData();
        int res;
        synchronized (this) {
            res = new AggregateCompleter(completers).complete(buffer, cursor, candidates);
        }
        Collections.sort(candidates);
        return res;
    }

    protected void checkData() {
        // Copy the set to avoid concurrent modification exceptions
        // TODO: fix that in gogo instead
        Set<String> names;
        boolean update;
        synchronized (this) {
            names = new HashSet<String>((Set<String>) session.get(COMMANDS));
            update = !names.equals(commands);
        }
        if (update) {
            // get command aliases
            Set<String> aliases = this.getAliases();
            Set<String> commands = new HashSet<String>();
            List<Completer> completers = new ArrayList<Completer>();
            completers.add(new StringsCompleter(aliases));

            // add argument completers for each command
            for (String command : names) {
                Function function = (Function) session.get(command);
                function = unProxy(function);
                if (function instanceof AbstractCommand) {
                    try {
                        completers.add(new ArgumentCompleter(session, (AbstractCommand) function, command));
                    } catch (Throwable t) {
                        LOGGER.debug("Unable to create completers for command '" + command + "'", t);
                    }
                }
                commands.add(command);
            }

            synchronized (this) {
                this.commands.clear();
                this.completers.clear();
                this.commands.addAll(commands);
                this.completers.addAll(completers);
            }
        }
    }

    /**
     * Get the aliases defined in the console session.
     *
     * @return the aliases set
     */
    private Set<String> getAliases() {
        Set<String> vars = (Set<String>) session.get(null);
        Set<String> aliases = new HashSet<String>();
        for (String var : vars) {
            Object content = session.get(var);
            if (content != null && "org.apache.felix.gogo.runtime.Closure".equals(content.getClass().getName()))  {
                aliases.add(var);
            }
        }
        return aliases;
    }

    protected Function unProxy(Function function) {
        try {
            if ("org.apache.felix.gogo.runtime.CommandProxy".equals(function.getClass().getName())) {
                Field contextField = function.getClass().getDeclaredField("context");
                Field referenceField = function.getClass().getDeclaredField("reference");
                contextField.setAccessible(true);
                referenceField.setAccessible(true);
                BundleContext context = (BundleContext) contextField.get(function);
                ServiceReference reference = (ServiceReference) referenceField.get(function);
                Object target = context != null ? context.getService(reference) : null;
                try {
                    if (target instanceof Function) {
                        function = (Function) target;
                    }
                } finally {
                    if (context != null) {
                        context.ungetService(reference);
                    }
                }
            }
        } catch (Throwable t) {
        }
        return function;
    }

    private class CommandTracker {
        private final ServiceListener listener;
        public CommandTracker() throws Exception {
            BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            if (context == null) {
                throw new IllegalStateException("Bundle is stopped");
            }
            listener = new ServiceListener() {
                public void serviceChanged(ServiceEvent event) {
                    synchronized (CommandsCompleter.this) {
                        commands.clear();
                    }
                }
            };
            context.addServiceListener(listener,
                    String.format("(&(%s=*)(%s=*))",
                            CommandProcessor.COMMAND_SCOPE,
                            CommandProcessor.COMMAND_FUNCTION));
        }
        
        public void dispose() {
            BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            context.removeServiceListener(listener);
        }
    }
}

