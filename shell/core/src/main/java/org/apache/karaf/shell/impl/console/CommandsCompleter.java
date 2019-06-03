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

import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.console.osgi.secured.SecuredCommand;
import org.apache.karaf.shell.impl.console.osgi.secured.SecuredSessionFactoryImpl;
import org.apache.karaf.shell.support.completers.ArgumentCommandLine;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

/**
 * Overall command line completer.
 */
public class CommandsCompleter extends org.apache.karaf.shell.support.completers.CommandsCompleter implements org.jline.reader.Completer {

    private final SessionFactory factory;
    private final Session session;
    private final Map<String, Completer> globalCompleters = new HashMap<>();
    private final Map<String, Completer> localCompleters = new HashMap<>();
    private final Completer aliasesCompleter = new SimpleCommandCompleter() {
        @Override
        protected Collection<String> getNames(Session session) {
            return getAliases(session);
        }
    };
    private final List<Command> commands = new ArrayList<>();

    public CommandsCompleter(SessionFactory factory, Session session) {
        this.factory = factory;
        this.session = session;
    }

    @Override
    public void complete(LineReader reader, ParsedLine line, List<org.jline.reader.Candidate> candidates) {
        CommandLine commandLine = new CommandLineImpl(line);
        List<Candidate> cands = new ArrayList<>();
        completeCandidates(session, commandLine, cands);
        // cleanup candidates to avoid to pollute depending of completion mode
        candidates.clear();
        for (Candidate cand : cands) {
            candidates.add(new org.jline.reader.Candidate(
                    cand.value(), cand.displ(), cand.group(),
                    cand.descr(), cand.suffix(), cand.key(),
                    cand.complete()));
        }
    }

    public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
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
            List<Completer> completers = new ArrayList<>();
            for (String name : allCompleters[1].keySet()) {
                if (name.startsWith(subShell + ":")) {
                    completers.add(allCompleters[1].get(name));
                }
            }
            if (!subShell.equals(Session.SCOPE_GLOBAL)) {
                completers.add(new StringsCompleter(Collections.singletonList("exit")));
            }
            completers.forEach(c -> c.completeCandidates(session, commandLine, candidates));
            return;
        }

        // FIRST mode
        if (Session.COMPLETION_MODE_FIRST.equalsIgnoreCase(completion)) {
            if (!subShell.isEmpty()) {
                List<Completer> completers = new ArrayList<>();
                for (String name : allCompleters[1].keySet()) {
                    if (name.startsWith(subShell + ":")) {
                        completers.add(allCompleters[1].get(name));
                    }
                }
                completers.forEach(c -> c.completeCandidates(session, commandLine, candidates));
                if (!candidates.isEmpty()) {
                    return;
                }
            }
            List<Completer> compl = new ArrayList<>();
            compl.add(aliasesCompleter);
            compl.addAll(allCompleters[0].values());
            compl.forEach(c -> c.completeCandidates(session, commandLine, candidates));
            return;
        }

        List<Completer> compl = new ArrayList<>();
        compl.add(aliasesCompleter);
        compl.addAll(allCompleters[0].values());
        compl.forEach(c -> c.completeCandidates(session, commandLine, candidates));
    }

    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        List<Candidate> cands = new ArrayList<>();
        completeCandidates(session, commandLine, cands);
        for (Candidate cand : cands) {
            candidates.add(cand.value());
        }
        return 0;
    }

    protected void sort(Map<String, Completer>[] completers, List<String> scopes) {
        ScopeComparator comparator = new ScopeComparator(scopes);
        for (int i = 0; i < completers.length; i++) {
            Map<String, Completer> map = new TreeMap<>(comparator);
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
            Map<String, Completer> global = new HashMap<>();
            Map<String, Completer> local = new HashMap<>();

            // add argument completers for each command
            for (Command command : commands) {
                String key = command.getScope() + ":" + command.getName();
                Completer cg = command.getCompleter(false);
                Completer cl = command.getCompleter(true);
                if (cg == null) {
                    if (Session.SCOPE_GLOBAL.equals(command.getScope())) {
                        cg = new FixedSimpleCommandCompleter(Collections.singletonList(command.getName()));
                    } else {
                        cg = new FixedSimpleCommandCompleter(Arrays.asList(key, command.getName()));
                    }
                }
                if (cl == null) {
                    cl = new FixedSimpleCommandCompleter(Collections.singletonList(command.getName()));
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
                    new HashMap<>(this.globalCompleters),
                    new HashMap<>(this.localCompleters)
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
        Set<String> aliases = new HashSet<>();
        for (String var : vars) {
            Object content = session.get(var);
            if (content != null && "org.apache.felix.gogo.runtime.Closure".equals(content.getClass().getName())) {

                //check both acl for alias and original cmd to determine if it should be visible
                int index = var.indexOf(":");
                if (index > 0 && (factory instanceof SecuredSessionFactoryImpl)) {
                    String scope = var.substring(0, index);
                    String command = var.substring(index + 1);
                    String originalCmd = content.toString();
                    index = originalCmd.indexOf(" ");
                    Object securityCmd = null;
                    if (index > 0) {
                        securityCmd = ((org.apache.felix.gogo.runtime.Closure)content).
                            get(originalCmd.substring(0, index));
                    }
                    if (securityCmd instanceof SecuredCommand) {
                        if (((SecuredSessionFactoryImpl)factory).isAliasVisible(scope, command)
                            && ((SecuredSessionFactoryImpl)factory).isVisible(((SecuredCommand)securityCmd).getScope(),
                                                                              ((SecuredCommand)securityCmd).getName())) {
                            aliases.add(var);
                        }
                    } else {
                        if (((SecuredSessionFactoryImpl)factory).isVisible(scope, command)) {
                            aliases.add(var);
                        }
                    }
                    
                } else {
                    aliases.add(var);
                }
            }
        }
        return aliases;
    }

    static abstract class SimpleCommandCompleter implements Completer {

        @Override
        public int complete(Session session, CommandLine commandLine, List<String> candidates) {
            String[] args = commandLine.getArguments();
            int argIndex = commandLine.getCursorArgumentIndex();
            StringsCompleter completer = new StringsCompleter(getNames(session));
            if (argIndex == 0) {
                int res = completer.complete(session, new ArgumentCommandLine(args[argIndex], commandLine.getArgumentPosition()), candidates);
                if (res > -1) {
                    res += commandLine.getBufferPosition() - commandLine.getArgumentPosition();
                }
                return res;
            } else if (!verifyCompleter(session, completer, args[0])) {
                return -1;
            }
            return 0;
        }

        protected abstract Collection<String> getNames(Session session);

        private boolean verifyCompleter(Session session, Completer completer, String argument) {
            List<String> candidates = new ArrayList<>();
            return completer.complete(session, new ArgumentCommandLine(argument, argument.length()), candidates) != -1 && !candidates.isEmpty();
        }

    }

    static class FixedSimpleCommandCompleter extends SimpleCommandCompleter {

        private final Collection<String> names;

        FixedSimpleCommandCompleter(Collection<String> names) {
            this.names = names;
        }

        @Override
        protected Collection<String> getNames(Session session) {
            return names;
        }
    }

    private static class CommandLineImpl implements CommandLine {
        private final ParsedLine line;

        public CommandLineImpl(ParsedLine line) {
            this.line = line;
        }

        @Override
        public int getCursorArgumentIndex() {
            return line.wordIndex();
        }

        @Override
        public String getCursorArgument() {
            return line.word();
        }

        @Override
        public int getArgumentPosition() {
            return line.wordCursor();
        }

        @Override
        public String[] getArguments() {
            return line.words().toArray(new String[line.words().size()]);
        }

        @Override
        public int getBufferPosition() {
            return line.cursor();
        }

        @Override
        public String getBuffer() {
            return line.line();
        }
    }

}

