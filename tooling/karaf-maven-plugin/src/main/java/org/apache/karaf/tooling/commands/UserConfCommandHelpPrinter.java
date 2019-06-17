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
 * Prints documentation in wiki syntax
 */
public class UserConfCommandHelpPrinter extends AbstractCommandHelpPrinter {

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

        out.println("h1. " + command.scope() + ":" + command.name());
        out.println();

        out.println("h2. Description");
        out.println(command.description());
        out.println();

        StringBuilder syntax = new StringBuilder();
        syntax.append(String.format("%s:%s", command.scope(), command.name()));
        if (options.size() > 0) {
            syntax.append(" \\[options\\]");
        }
        if (arguments.size() > 0) {
            syntax.append(' ');
            for (Argument argument : arguments) {
                syntax.append(String.format(argument.required() ? "%s " : "\\[%s\\] ", argument.name()));
            }
        }
        out.println("h2. Syntax");
        out.println(syntax.toString());
        out.println();

        if (arguments.size() > 0) {
            out.println("h2. Arguments");
            out.println("|| Name || Description ||");
            for (Argument argument : arguments) {
                String description = argument.description();
                if (!argument.required()) {
                    Object o = getDefaultValue(action, argFields.get(argument));
                    String defaultValue = getDefaultValueString(o);
                    if (defaultValue != null) {
                        description += " (defaults to " + o.toString() + ")";
                    }
                }
                out.println("| " + argument.name() + " | " + description + " |");
            }
            out.println();
        }
        if (options.size() > 0) {
            out.println("h2. Options");
            out.println("|| Name || Description ||");
            for (Option option : options) {
                StringBuilder opt = new StringBuilder(option.name());
                String desc = option.description();
                for (String alias : option.aliases()) {
                    opt.append(", ").append(alias);
                }
                Object o = getDefaultValue(action, optFields.get(option));
                String defaultValue = getDefaultValueString(o);
                if (defaultValue != null) {
                    desc += " (defaults to " + defaultValue + ")";
                }
                out.println("| " + opt + " | " + desc + " |");
            }
            out.println();
        }
        if (command.detailedDescription().length() > 0) {
            out.println("h2. Details");
            out.println(command.detailedDescription());
        }
        out.println();
    }

    @Override
    public void printOverview(Map<String, Set<String>> commands, PrintStream writer) {
        writer.println("h1. Commands");
        writer.println();
        for (String key : commands.keySet()) {
            writer.println("h2. " + key);
            writer.println();
            for (String cmd : commands.get(key)) {
                writer.println("* [" + key + ":" + cmd + "|" + key + "-" + cmd + "]");
            }
            writer.println();
        }
    }

}
