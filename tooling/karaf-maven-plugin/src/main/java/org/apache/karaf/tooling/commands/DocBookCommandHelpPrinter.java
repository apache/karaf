/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.commands;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.impl.action.command.HelpOption;

/**
 * Prints documentation in docbook syntax
 */
public class DocBookCommandHelpPrinter extends AbstractCommandHelpPrinter {

    @Override
    public void printHelp(Action action, PrintStream out, boolean includeHelpOption) {
        Command command = action.getClass().getAnnotation(Command.class);
        Set<Option> options = new HashSet<>();
        List<Argument> arguments = new ArrayList<>();
        Map<Argument, Field> argFields = new HashMap<>();
        Map<Option, Field> optFields = new HashMap<>();
        for (Class<?> type = action.getClass(); type != null; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                Option option = field.getAnnotation(Option.class);
                if (option != null) {
                    options.add(option);
                }

                Argument argument = field.getAnnotation(Argument.class);
                if (argument != null) {
                    argument = replaceDefaultArgument(field, argument);
                    argFields.put(argument, field);
                    int index = argument.index();
                    while (arguments.size() <= index) {
                        arguments.add(null);
                    }
                    if (arguments.get(index) != null) {
                        throw new IllegalArgumentException("Duplicate argument index: " + index + " on Action " + action.getClass().getName());
                    }
                    arguments.set(index, argument);
                }
            }
        }
        if (includeHelpOption)
            options.add(HelpOption.HELP);

        out.println("<section>");
        out.println("  <title>" + command.scope() + ":" + command.name() + "</title>");
        out.println("  <section>");
        out.println("    <title>Description</title>");
        out.println("    <para>");
        out.println(command.description());
        out.println("    </para>");
        out.println("  </section>");

        StringBuilder syntax = new StringBuilder();
        syntax.append(String.format("%s:%s", command.scope(), command.name()));
        if (options.size() > 0) {
            syntax.append(" [options]");
        }
        if (arguments.size() > 0) {
            syntax.append(' ');
            for (Argument argument : arguments) {
                syntax.append(String.format(argument.required() ? "%s " : "[%s] ", argument.name()));
            }
        }
        out.println("  <section>");
        out.println("    <title>Syntax</title>");
        out.println("    <para>");
        out.println(syntax.toString());
        out.println("    </para>");
        out.println("  </section>");

        if (arguments.size() > 0) {
            out.println("  <section>");
            out.println("    <title>Arguments</title>");
            out.println("    <informaltable>");
            for (Argument argument : arguments) {
                out.println("    <tr>");
                out.println("      <td>" + argument.name() + "</td>");
                String description = argument.description();
                if (!argument.required()) {
                    if (argument.valueToShowInHelp() != null && argument.valueToShowInHelp().length() != 0) {
                        if (Argument.DEFAULT_STRING.equals(argument.valueToShowInHelp())) {
                            Object o = getDefaultValue(action, argFields.get(argument));
                            String defaultValue = getDefaultValueString(o);
                            if (defaultValue != null) {
                                description += " (defaults to " + o.toString() + ")";
                            }
                        }
                    }
                }
                out.println("      <td>" + description + "</td>");
                out.println("    </tr>");
            }

            out.println("    </informaltable>");
            out.println("  </section>");
        }

        if (options.size() > 0) {
            out.println("  <section>");
            out.println("    <title>Options</title>");
            out.println("    <informaltable>");

            for (Option option : options) {
                StringBuilder opt = new StringBuilder(option.name());
                String description = option.description();
                for (String alias : option.aliases()) {
                    opt.append(", ").append(alias);
                }
                Object o = getDefaultValue(action, optFields.get(option));
                String defaultValue = getDefaultValueString(o);
                if (defaultValue != null) {
                    description += " (defaults to " + o.toString() + ")";
                }
                out.println("    <tr>");
                out.println("      <td>" + opt + "</td>");
                out.println("      <td>" + description + "</td>");
                out.println("    </tr>");
            }

            out.println("    </informaltable>");
            out.println("  </section>");
        }

        if (command.detailedDescription().length() > 0) {
            out.println("  <section>");
            out.println("    <title>Details</title>");
            out.println("    <para>");
            out.println(command.detailedDescription());
            out.println("    </para>");
            out.println("  </section>");
        }
        out.println("</section>");
    }

    @Override
    public void printOverview(Map<String, Set<String>> commands, PrintStream writer) {
        writer.println("<chapter id='commands' xmlns:xi=\"http://www.w3.org/2001/XInclude\">");
        writer.println("  <title>Commands</title>");
        writer.println("  <toc></toc>");
        for (String key : commands.keySet()) {
            writer.println("  <section id='commands-" + key + "'>");
            writer.println("    <title>" + key + "</title>");
            for (String cmd : commands.get(key)) {
                writer.println("    <xi:include href='" + key + "-" + cmd + ".xml' parse='xml'/>");
            }
            writer.println("  </section>");
        }
        writer.println("</chapter>");
    }

}
