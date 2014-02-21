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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.SessionProperties;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Like the {@link org.apache.karaf.shell.console.completer.CommandsCompleter} but does not use OSGi but is
 * instead used from the non-OSGi {@link org.apache.karaf.shell.console.impl.Main}
 */
public class CommandsCompleter implements Completer {

    public static final String COMMANDS = ".commands";

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsCompleter.class);

    private CommandSession session;
    private final Map<String, Completer> globalCompleters = new HashMap<String, Completer>();
    private final Map<String, Completer> localCompleters = new HashMap<String, Completer>();
    private final Set<String> commands = new HashSet<String>();

    public CommandsCompleter() {
        this(CommandSessionHolder.getSession());
    }

    public CommandsCompleter(CommandSession session) {
        this.session = session;
        try {
            new CommandTracker();
        } catch (Throwable t) {
            // Ignore in case we're not in OSGi
        }
    }


    public int complete(String buffer, int cursor, List<String> candidates) {
        if (session == null) {
            session = CommandSessionHolder.getSession();
        }

        List<String> scopes = getCurrentScopes();
        Map<String, Completer>[] allCompleters = checkData();
        sort(allCompleters, scopes);

        String subShell = getCurrentSubShell();
        String completion = getCompletionType();

        // SUBSHELL mode
        if ("SUBSHELL".equalsIgnoreCase(completion)) {
            if (subShell.isEmpty()) {
                subShell = "*";
            }
            List<Completer> completers = new ArrayList<Completer>();
            for (String name : allCompleters[1].keySet()) {
                if (name.startsWith(subShell)) {
                    completers.add(allCompleters[1].get(name));
                }
            }
            if (!subShell.equals("*")) {
                completers.add(new StringsCompleter(new String[] { "exit" }));
            }
            int res = new AggregateCompleter(completers).complete(buffer, cursor, candidates);
            Collections.sort(candidates);
            return res;
        }

        if ("FIRST".equalsIgnoreCase(completion)) {
            if (!subShell.isEmpty()) {
                List<Completer> completers = new ArrayList<Completer>();
                for (String name : allCompleters[1].keySet()) {
                    if (name.startsWith(subShell)) {
                        completers.add(allCompleters[1].get(name));
                    }
                }
                int res = new AggregateCompleter(completers).complete(buffer, cursor, candidates);
                if (!candidates.isEmpty()) {
                    Collections.sort(candidates);
                    return res;
                }
            }
            List<Completer> compl = new ArrayList<Completer>();
            compl.add(new StringsCompleter(getAliases()));
            compl.addAll(allCompleters[0].values());
            int res = new AggregateCompleter(compl).complete(buffer, cursor, candidates);
            Collections.sort(candidates);
            return res;
        }

        List<Completer> compl = new ArrayList<Completer>();
        compl.add(new StringsCompleter(getAliases()));
        compl.addAll(allCompleters[0].values());
        int res = new AggregateCompleter(compl).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

    protected void sort(Map<String, Completer>[] completers, List<String> scopes) {
        ScopeComparator comparator = new ScopeComparator(scopes);
        for (int i = 0; i < completers.length; i++) {
            Map<String, Completer> map = new TreeMap<String, Completer>(comparator);
            map.putAll(completers[i]);
            completers[i] = map;
        }
    }

    protected static class ScopeComparator implements Comparator<String> {
        private final List<String> scopes;
        public ScopeComparator(List<String> scopes) {
            this.scopes = scopes;
        }
        @Override
        public int compare(String o1, String o2) {
            String[] p1 = o1.split(":");
            String[] p2 = o2.split(":");
            int p = 0;
            while (p < p1.length && p < p2.length) {
                int i1 = scopes.indexOf(p1[p]);
                int i2 = scopes.indexOf(p2[p]);
                if (i1 < 0) {
                    if (i2 < 0) {
                        int c = p1[p].compareTo(p2[p]);
                        if (c != 0) {
                            return c;
                        } else {
                            p++;
                        }
                    } else {
                        return +1;
                    }
                } else if (i2 < 0) {
                    return -1;
                } else if (i1 < i2) {
                    return -1;
                } else if (i1 > i2) {
                    return +1;
                } else {
                    p++;
                }
            }
            return 0;
        }
    }

    protected List<String> getCurrentScopes() {
        String scopes = (String) session.get("SCOPE");
        return Arrays.asList(scopes.split(":"));
    }

    protected String getCurrentSubShell() {
        String s = (String) session.get("SUBSHELL");
        if (s == null) {
            s = "";
        }
        return s;
    }

    protected String getCompletionType() {
        String completion = (String) session.get(SessionProperties.COMPLETION_MODE);
        if (completion == null) {
            completion = "GLOBAL";
        }
        return completion;
    }

    protected String stripScope(String name) {
        int index = name.indexOf(":");
        return index > 0 ? name.substring(index + 1) : name;
    }

    @SuppressWarnings({
        "unchecked", "deprecation"
    })
    protected Map<String, Completer>[] checkData() {
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
            Set<String> commands = new HashSet<String>();
            Map<String, Completer> global = new HashMap<String, Completer>();
            Map<String, Completer> local = new HashMap<String, Completer>();

            // add argument completers for each command
            for (String command : names) {
                String rawCommand = stripScope(command);
                Function function = (Function) session.get(command);
                function = unProxy(function);
                if (function instanceof CommandWithAction) {
                    try {
                        global.put(command, new ArgumentCompleter(session, (CommandWithAction) function, command));
                        local.put(command, new ArgumentCompleter(session, (CommandWithAction) function, rawCommand));
                    } catch (Throwable t) {
                        LOGGER.debug("Unable to create completers for command '" + command + "'", t);
                    }
                }
                else if (function instanceof org.apache.felix.gogo.commands.CommandWithAction) {
                    try {
                        global.put(command, new OldArgumentCompleter(session, (org.apache.felix.gogo.commands.CommandWithAction) function, command));
                        local.put(command, new OldArgumentCompleter(session, (org.apache.felix.gogo.commands.CommandWithAction) function, rawCommand));
                    } catch (Throwable t) {
                        LOGGER.debug("Unable to create completers for command '" + command + "'", t);
                    }
                }
                commands.add(command);
            }

            synchronized (this) {
                this.commands.clear();
                this.globalCompleters.clear();
                this.localCompleters.clear();
                this.commands.addAll(commands);
                this.globalCompleters.putAll(global);
                this.localCompleters.putAll(local);
            }
        }
        synchronized (this) {
            return new Map[] {
                    new HashMap<String, Completer>(this.globalCompleters),
                    new HashMap<String, Completer>(this.localCompleters)
            };
        }
    }

    /**
     * Get the aliases defined in the console session.
     *
     * @return the aliases set
     */
    @SuppressWarnings("unchecked")
    private Set<String> getAliases() {
        Set<String> vars = ((Set<String>) session.get(null));
        Set<String> aliases = new HashSet<String>();
        for (String var : vars) {
            Object content = session.get(var);
            if (content != null && "org.apache.felix.gogo.runtime.Closure".equals(content.getClass().getName())) {
                aliases.add(var);
            }
        }
        return aliases;
    }

    public static Function unProxy(Function function) {
        if (function == null || function.getClass() != CommandProxy.class) {
            return function;
        }
        CommandProxy proxy = (CommandProxy)function;
        Object target = proxy.getTarget();
        try {
            return target instanceof Function ? (Function)target : function;
        } finally {
            proxy.ungetTarget();
        }
    }

    private class CommandTracker {
        public CommandTracker() throws Exception {
            BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            if (context == null) {
                throw new IllegalStateException("Bundle is stopped");
            }
            ServiceListener listener = new ServiceListener() {
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
    }

}

