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
package org.apache.karaf.shell.commands.meta;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.commands.Action;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.HelpOption;
import org.apache.karaf.shell.commands.Option;
import org.apache.karaf.shell.console.Completer;
import org.apache.karaf.shell.util.IndentFormatter;
import org.apache.karaf.shell.util.ShellUtil;
import org.fusesource.jansi.Ansi;

public class ActionMetaData {

    private final Class<? extends Action> actionClass;
    private final Command command;
    private final Map<Option, Field> options;
    private final Map<Argument, Field> arguments;
    List<Argument> orderedArguments;
    private final Completer[] completers;

    public ActionMetaData(Class<? extends Action> actionClass, Command command, Map<Option, Field> options, Map<Argument, Field> args,
            List<Argument> orderedArguments, Completer[] completers) {
        super();
        this.actionClass = actionClass;
        this.command = command;
        this.options = options;
        this.arguments = args;
        this.orderedArguments = orderedArguments;
        this.completers = completers;
    }

    public Class<? extends Action> getActionClass() {
        return actionClass;
    }
    
    public Command getCommand() {
        return command;
    }

    public Map<Option, Field> getOptions() {
        return options;
    }

    public Map<Argument, Field> getArguments() {
        return arguments;
    }

    public Completer[] getCompleters() {
        return completers;
    }

    public List<Argument> getOrderedArguments() {
        return orderedArguments;
    }

    public void printUsage(Action action, PrintStream out, boolean globalScope, int termWidth) {
        if (command != null) {
            List<Argument> argumentsSet = new ArrayList<Argument>(arguments.keySet());
            Collections.sort(argumentsSet, new Comparator<Argument>() {
                public int compare(Argument o1, Argument o2) {
                    return Integer.valueOf(o1.index()).compareTo(Integer.valueOf(o2.index()));
                }
            });
            Set<Option> optionsSet = new HashSet<Option>(options.keySet());
            optionsSet.add(HelpOption.HELP);
            if (command != null && (command.description() != null || command.name() != null)) {
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("DESCRIPTION").a(Ansi.Attribute.RESET));
                out.print("        ");
                if (command.name() != null) {
                    if (globalScope) {
                        out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(command.name()).a(Ansi.Attribute.RESET));
                    } else {
                        out.println(Ansi.ansi().a(command.scope()).a(":").a(Ansi.Attribute.INTENSITY_BOLD).a(command.name()).a(Ansi.Attribute.RESET));
                    }
                    out.println();
                }
                out.print("\t");
                out.println(command.description());
                out.println();
            }
            StringBuffer syntax = new StringBuffer();
            if (command != null) {
                if (globalScope) {
                    syntax.append(command.name());
                } else {
                    syntax.append(String.format("%s:%s", command.scope(), command.name()));
                }
            }
            if (options.size() > 0) {
                syntax.append(" [options]");
            }
            if (arguments.size() > 0) {
                syntax.append(' ');
                for (Argument argument : argumentsSet) {
                    if (!argument.required()) {
                        syntax.append(String.format("[%s] ", argument.name()));
                    } else {
                        syntax.append(String.format("%s ", argument.name()));
                    }
                }
            }

            out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("SYNTAX").a(Ansi.Attribute.RESET));
            out.print("        ");
            out.println(syntax.toString());
            out.println();
            if (arguments.size() > 0) {
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("ARGUMENTS").a(Ansi.Attribute.RESET));
                for (Argument argument : argumentsSet) {
                    out.print("        ");
                    out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(argument.name()).a(Ansi.Attribute.RESET));
                    IndentFormatter.printFormatted("                ", argument.description(), termWidth, out);
                    if (!argument.required()) {
                        if (argument.valueToShowInHelp() != null && argument.valueToShowInHelp().length() != 0) {
                            try {
                                if (Argument.DEFAULT_STRING.equals(argument.valueToShowInHelp())) {
                                    arguments.get(argument).setAccessible(true);
                                    Object o = arguments.get(argument).get(action);
                                    printObjectDefaultsTo(out, o);
                                } else {
                                    printDefaultsTo(out, argument.valueToShowInHelp());
                                }
                            } catch (Throwable t) {
                                // Ignore
                            }
                        }
                    }
                }
                out.println();
            }
            if (options.size() > 0) {
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("OPTIONS").a(Ansi.Attribute.RESET));
                for (Option option : optionsSet) {
                    String opt = option.name();
                    for (String alias : option.aliases()) {
                        opt += ", " + alias;
                    }
                    out.print("        ");
                    out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a(opt).a(Ansi.Attribute.RESET));
                    IndentFormatter.printFormatted("                ", option.description(), termWidth, out);
                    if (option.valueToShowInHelp() != null && option.valueToShowInHelp().length() != 0) {
                        try {
                            if (Option.DEFAULT_STRING.equals(option.valueToShowInHelp())) {
                                options.get(option).setAccessible(true);
                                Object o = options.get(option).get(action);
                                printObjectDefaultsTo(out, o);
                            } else {
                                printDefaultsTo(out, option.valueToShowInHelp());
                            }
                        } catch (Throwable t) {
                            // Ignore
                        }
                    }
                }
                out.println();
            }
            if (command.detailedDescription().length() > 0) {
                out.println(Ansi.ansi().a(Ansi.Attribute.INTENSITY_BOLD).a("DETAILS").a(Ansi.Attribute.RESET));
                String desc = loadDescription(actionClass, command.detailedDescription());
                IndentFormatter.printFormatted("        ", desc, termWidth, out);
            }
        }
    }
    
    private String loadDescription(Class clazz, String desc) {
        if (desc.startsWith("classpath:")) {
            desc = ShellUtil.loadClassPathResource(clazz, desc.substring("classpath:".length()));
        }
        return desc;
    }

    private void printObjectDefaultsTo(PrintStream out, Object o) {
        if (o != null
                && (!(o instanceof Boolean) || ((Boolean) o))
                && (!(o instanceof Number) || ((Number) o).doubleValue() != 0.0)) {
            printDefaultsTo(out, o.toString());
        }
    }

    private void printDefaultsTo(PrintStream out, String value) {
        out.print("                (defaults to ");
        out.print(value);
        out.println(")");
    }

}
