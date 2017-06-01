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
package org.apache.karaf.shell.impl.console.commands.help;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.utils.properties.InterpolationHelper;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.support.CommandException;
import org.apache.karaf.shell.support.completers.ArgumentCommandLine;
import org.apache.karaf.shell.support.completers.StringsCompleter;

import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_DEFAULT;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.COLOR_RED;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_BOLD;
import static org.apache.karaf.shell.support.ansi.SimpleAnsi.INTENSITY_NORMAL;

public class HelpCommand implements Command {

    public HelpCommand(SessionFactory factory) {
        Registry registry = factory.getRegistry();
        registry.register(this);
        registry.register(new SimpleHelpProvider());
        registry.register(new ShellHelpProvider());
        registry.register(new SingleCommandHelpProvider());
        registry.register(new CommandsHelpProvider());
        registry.register(new CommandListHelpProvider());
        registry.register(new BundleHelpProvider());
    }

    @Override
    public String getScope() {
        return Session.SCOPE_GLOBAL;
    }

    @Override
    public String getName() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Displays this help or help about a command";
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        if (arguments.contains("--help")) {
            printHelp(System.out);
            return null;
        }
        if (arguments.size() > 1) {
            String msg = COLOR_RED
                    + "Error executing command "
                    + INTENSITY_BOLD + getName() + INTENSITY_NORMAL
                    + COLOR_DEFAULT + ": " + "too many arguments specified";
            throw new CommandException(msg);
        }
        String path = arguments.isEmpty() ? null : arguments.get(0) == null ? null : arguments.get(0).toString();
        String help = getHelp(session, path);
        if (help != null) {
            try (BufferedReader reader = new BufferedReader(new StringReader(help))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
        return null;
    }

    @Override
    public Completer getCompleter(final boolean scoped) {
        return new Completer() {
            @Override
            public int complete(Session session, CommandLine commandLine, List<String> candidates) {
                String[] args = commandLine.getArguments();
                int argIndex = commandLine.getCursorArgumentIndex();
                StringsCompleter completer = new StringsCompleter(Collections.singletonList(getName()));
                if (argIndex == 0) {
                    return completer.complete(session, new ArgumentCommandLine(args[argIndex], commandLine.getArgumentPosition()), candidates);
                } else if (!verifyCompleter(session, completer, args[0])) {
                    return -1;
                }
                // TODO: use CommandNamesCompleter and better completion wrt parsing etc...
                completer = new StringsCompleter();
                for (Command command : session.getRegistry().getCommands()) {
                    if (!Session.SCOPE_GLOBAL.equals(command.getScope())) {
                        completer.getStrings().add(command.getScope() + ":" + command.getName());
                    }
                    completer.getStrings().add(command.getName());
                }
                completer.getStrings().add("--help");
                if (argIndex == 1) {
                    int res;
                    if (argIndex < args.length) {
                        res = completer.complete(session, new ArgumentCommandLine(args[argIndex], commandLine.getArgumentPosition()), candidates);
                    } else {
                        res = completer.complete(session, new ArgumentCommandLine("", 0), candidates);
                    }
                    return res + (commandLine.getBufferPosition() - commandLine.getArgumentPosition());
                } else if (!verifyCompleter(session, completer, args[1])) {
                    return -1;
                }
                return -1;
            }
            protected boolean verifyCompleter(Session session, Completer completer, String argument) {
                List<String> candidates = new ArrayList<>();
                return completer.complete(session, new ArgumentCommandLine(argument, argument.length()), candidates) != -1 && !candidates.isEmpty();
            }
        };
    }

    @Override
    public Parser getParser() {
        return null;
    }

    protected void printHelp(PrintStream out) {
        out.println(INTENSITY_BOLD + "DESCRIPTION" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + getName() + INTENSITY_NORMAL);
        out.println();
        out.print("\t");
        out.println(getDescription());
        out.println();
        out.println(INTENSITY_BOLD + "SYNTAX" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(getName() + " [options] [command]");
        out.println();
        out.println(INTENSITY_BOLD + "ARGUMENTS" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + "command" + INTENSITY_NORMAL);
        out.print("                ");
        out.println("Command to display help for");
        out.println();
        out.println(INTENSITY_BOLD + "OPTIONS" + INTENSITY_NORMAL);
        out.print("        ");
        out.println(INTENSITY_BOLD + "--help" + INTENSITY_NORMAL);
        out.print("                ");
        out.println("Display this help message");
        out.println();
    }

    public String getHelp(final Session session, String path) {
        if (path == null) {
            path = "%root%";
        }
        Map<String,String> props = new HashMap<>();
        props.put("data", "${" + path + "}");
        final List<HelpProvider> providers = session.getRegistry().getServices(HelpProvider.class);
        InterpolationHelper.performSubstitution(props, key -> {
            for (HelpProvider hp : providers) {
                String result = hp.getHelp(session, key);
                if (result != null) {
                    return removeNewLine(result);
                }
            }
            return null;
        });
        return props.get("data");
    }

    private String removeNewLine(String help) {
        if (help != null && help.endsWith("\n")) {
            help = help.substring(0, help.length() - 1);
        }
        return help;
    }

}
