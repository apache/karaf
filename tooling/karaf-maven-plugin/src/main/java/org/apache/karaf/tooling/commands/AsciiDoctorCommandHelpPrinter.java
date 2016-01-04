/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.commands;

import org.apache.karaf.shell.commands.*;
import org.apache.karaf.shell.commands.meta.ActionMetaData;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Prints documentation in asciidoc syntax
 */
public class AsciiDoctorCommandHelpPrinter implements CommandHelpPrinter {

    @Override
    public void printHelp(Action action, ActionMetaData actionMeta, PrintStream out, boolean includeHelpOption) {

        Map<Option, Field> optionsMap = actionMeta.getOptions();
        Map<Argument, Field> argsMap = actionMeta.getArguments();
        Command command = action.getClass().getAnnotation(Command.class);
        List<Argument> arguments = new ArrayList<Argument>(argsMap.keySet());
        Collections.sort(arguments, new Comparator<Argument>() {
            public int compare(Argument o1, Argument o2) {
                return Integer.valueOf(o1.index()).compareTo(Integer.valueOf(o2.index()));
            }
        });
        Set<Option> options = new HashSet<Option>(optionsMap.keySet());
        if (includeHelpOption)
            options.add(HelpOption.HELP);

        out.println("= " + command.scope() + ":" + command.name());
        out.println();

        out.println("== Description");
        out.println(command.description());
        out.println();

        StringBuffer syntax = new StringBuffer();
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
        out.println("== Syntax");
        out.println();
        out.println(syntax.toString());
        out.println();

        if (arguments.size() > 0) {
            out.println("== Arguments");
            out.println();
            out.println("|===");
            out.println("|Name |Description");
            for (Argument argument : arguments) {
                String description = argument.description();
                if (!argument.required()) {
                    Object o = actionMeta.getDefaultValue(action, argument);
                    String defaultValue = actionMeta.getDefaultValueString(o);
                    if (defaultValue != null) {
                        description += " (defaults to " + o.toString() + ")";
                    }
                }
                out.println();
                out.println("| " + argument.name());
                out.println("| " + description);
            }
            out.println("|===");
            out.println();
        }

        if (options.size() > 0) {
            out.println("== Options");
            out.println();
            out.println("|===");
            out.println("|Name |Description");

            for (Option option : options) {
                String opt = option.name();
                String description = option.description();
                for (String alias : option.aliases()) {
                    opt += ", " + alias;
                }
                Object o = actionMeta.getDefaultValue(action, option);
                String defaultValue = actionMeta.getDefaultValueString(o);
                if (defaultValue != null) {
                    description += " (defaults to " + o.toString() + ")";
                }

                out.println();
                out.println("|" + opt);
                out.println("|" + description);
            }
            out.println("|===");
            out.println();
        }

        String desc = actionMeta.getDetailedDescription();
        if (desc != null) {
            out.println("== Details");
            out.println();
            out.println(desc);
            out.println();
        }
        out.println();
    }

    @Override
    public void printOverview(Map<String, Set<String>> commands, PrintStream writer) {
        writer.println("= Commands");
        writer.println();
        for (String key : commands.keySet()) {
            writer.println("== " + key);
            writer.println();
            for (String cmd : commands.get(key)) {
                writer.println("* link:" + key + "-" + cmd + "[" + key + ":" + cmd + "]");
            }
        }
        writer.println();
    }

}
