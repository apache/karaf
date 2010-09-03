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

import jline.Terminal;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.basic.DefaultActionPreparator;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.console.Main;
import org.fusesource.jansi.Ansi;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Like the {@link org.apache.karaf.shell.console.completer.CommandsCompleter} but does not use OSGi but is
 * instead used from the non-OSGi {@link org.apache.karaf.shell.console.Main}
 */
public class SimpleCommandsCompleter implements Completer {

    private final Main main;
    private final Map<Command, Completer> completers = new ConcurrentHashMap<Command, Completer>();
    private final Map<String, Function> functions = Collections.synchronizedMap(new TreeMap<String, Function>());
    private final Map<String, Command> commands = Collections.synchronizedMap(new TreeMap<String, Command>());

    public SimpleCommandsCompleter(Main main) {
        this.main = main;
    }


    public void addCommand(Command command, Function function) {
        Set<String> names = getNames(command);
        if (names != null) {
            List<Completer> cl = new ArrayList<Completer>();
            cl.add(new StringsCompleter(names));
            if (function instanceof CompletableFunction) {
                List<Completer> fcl = ((CompletableFunction) function).getCompleters();
                if (fcl != null) {
                    for (Completer c : fcl) {
                        cl.add(c == null ? NullCompleter.INSTANCE : c);
                    }
                } else {
                    cl.add(NullCompleter.INSTANCE);
                }
            } else {
                cl.add(NullCompleter.INSTANCE);
            }
            ArgumentCompleter c = new ArgumentCompleter(cl);
            completers.put(command, c);
            for (String name : names) {
                functions.put(name, function);
                commands.put(name, command);
            }
        }
    }

    public void removeCommand(Command cmd) {
        if (cmd != null) {
            completers.remove(cmd);
        }
    }

    private Set<String> getNames(Command command) {
        Set<String> names = new HashSet<String>();
        Object scope = command.scope();
        String function = command.name();
        if (function != null) {
            if (scope == null || scope.equals(main.getApplication()) || scope.equals("*")) {
                names.add(function);
                return names;
            } else {
                names.add(scope + ":" + function);
                return names;
            }
        }
        return null;
    }

    public int complete(String buffer, int cursor, List<String> candidates) {
        int res = new AggregateCompleter(completers.values()).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

    /**
     * Prints the usage for all commands
     */
    public Object printUsage(CommandSession session) {
        Terminal term = (Terminal) session.get(".jline.terminal");
        PrintStream out = System.out;

        if (commands.size() > 0) {
            out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("COMMANDS").a(Ansi.Attribute.RESET));
            for (Command command : commands.values()) {
                out.print("        ");
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(command.name()).a(Ansi.Attribute.RESET));
                DefaultActionPreparator.printFormatted("                ", command.description(), term != null ? term.getTerminalWidth() : 80, out);
            }
            out.println();
        }
        return null;
    }

    /**
     * Prints the usage for a single command
     */
    public Object printCommandUsage(CommandSession session, String command) throws Exception {
        // lets just call the command if it exists using the help option
        Function function = functions.get(command);
        if (function == null) {
            throw new IllegalArgumentException("No such command: " + command);
        }
        return function.execute(session, Arrays.asList(new Object[]{"--help"}));
    }
}

