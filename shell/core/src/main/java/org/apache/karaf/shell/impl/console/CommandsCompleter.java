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
package org.apache.karaf.shell.impl.console;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.support.completers.AggregateCompleter;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Overall command line completer.
 */
public class CommandsCompleter extends org.apache.karaf.shell.support.completers.CommandsCompleter {

    private final SessionFactory factory;
    private final Map<String, Completer> globalCompleters = new HashMap<String, Completer>();
    private final Map<String, Completer> localCompleters = new HashMap<String, Completer>();
    private final List<Command> commands = new ArrayList<Command>();

    public CommandsCompleter(SessionFactory factory) {
        this.factory = factory;
    }

    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        Map<String, Completer>[] allCompleters = checkData();

        List<String> scopes = getCurrentScopes(session);
        sort(allCompleters, scopes);

        String subShell = getCurrentSubShell(session);
        String completion = getCompletionType(session);

        // SUBSHELL mode
        if (Session.COMPLETION_MODE_SUBSHELL.equalsIgnoreCase(completion)) {
            if (subShell.isEmpty()) {
                subShell = Session.SCOPE_GLOBAL;
            }
            List<Completer> completers = new ArrayList<Completer>();
            for (String name : allCompleters[1].keySet()) {
                if (name.startsWith(subShell)) {
                    completers.add(allCompleters[1].get(name));
                }
            }
            if (!subShell.equals(Session.SCOPE_GLOBAL)) {
                completers.add(new StringsCompleter(new String[] { "exit" }));
            }
            int res = new AggregateCompleter(completers).complete(session, commandLine, candidates);
            Collections.sort(candidates);
            return res;
        }

        if (Session.COMPLETION_MODE_FIRST.equalsIgnoreCase(completion)) {
            if (!subShell.isEmpty()) {
                List<Completer> completers = new ArrayList<Completer>();
                for (String name : allCompleters[1].keySet()) {
                    if (name.startsWith(subShell)) {
                        completers.add(allCompleters[1].get(name));
                    }
                }
                int res = new AggregateCompleter(completers).complete(session, commandLine, candidates);
                if (!candidates.isEmpty()) {
                    Collections.sort(candidates);
                    return res;
                }
            }
            List<Completer> compl = new ArrayList<Completer>();
            compl.add(new StringsCompleter(getAliases(session)));
            compl.addAll(allCompleters[0].values());
            int res = new AggregateCompleter(compl).complete(session, commandLine, candidates);
            Collections.sort(candidates);
            return res;
        }

        List<Completer> compl = new ArrayList<Completer>();
        compl.add(new StringsCompleter(getAliases(session)));
        compl.addAll(allCompleters[0].values());
        int res = new AggregateCompleter(compl).complete(session, commandLine, candidates);
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

    protected List<String> getCurrentScopes(Session session) {
        String scopes = (String) session.get(Session.SCOPE);
        return Arrays.asList(scopes.split(":"));
    }

    protected String getCurrentSubShell(Session session) {
        String s = (String) session.get(Session.SUBSHELL);
        if (s == null) {
            s = "";
        }
        return s;
    }

    protected String getCompletionType(Session session) {
        String completion = (String) session.get(Session.COMPLETION_MODE);
        if (completion == null) {
            completion = Session.COMPLETION_MODE_GLOBAL;
        }
        return completion;
    }

    protected String stripScope(String name) {
        int index = name.indexOf(":");
        return index > 0 ? name.substring(index + 1) : name;
    }

    @SuppressWarnings("unchecked")
    protected Map<String, Completer>[] checkData() {
        // Copy the set to avoid concurrent modification exceptions
        // TODO: fix that in gogo instead
        Collection<Command> commands;
        boolean update;
        synchronized (this) {
            commands = factory.getRegistry().getCommands();
            update = !commands.equals(this.commands);
        }
        if (update) {
            // get command aliases
            Map<String, Completer> global = new HashMap<String, Completer>();
            Map<String, Completer> local = new HashMap<String, Completer>();

            // add argument completers for each command
            for (Command command : commands) {
                String key = command.getScope() + ":" + command.getName();
                Completer cg = command.getCompleter(false);
                Completer cl = command.getCompleter(true);
                if (cg == null) {
                    if (Session.SCOPE_GLOBAL.equals(command.getScope())) {
                        cg = new StringsCompleter(new String[] { command.getName() });
                    } else {
                        cg = new StringsCompleter(new String[] { key, command.getName() });
                    }
                }
                if (cl == null) {
                    cl = new StringsCompleter(new String[] { command.getName() });
                }
                global.put(key, cg);
                local.put(key, cl);
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
    private Set<String> getAliases(Session session) {
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

}

