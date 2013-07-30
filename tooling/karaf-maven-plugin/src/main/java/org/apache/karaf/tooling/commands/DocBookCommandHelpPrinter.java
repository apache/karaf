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
import org.apache.karaf.shell.commands.meta.ActionMetaData;

/**
 * Prints documentation in docbook syntax
 */
public class DocBookCommandHelpPrinter implements CommandHelpPrinter {

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

        out.println("<section>");
        out.println("  <title>" + command.scope() + ":" + command.name() + "</title>");
        out.println("  <section>");
        out.println("    <title>Description</title>");
        out.println("    <para>");
        out.println(command.description());
        out.println("    </para>");
        out.println("  </section>");

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
                    Object o = actionMeta.getDefaultValue(action, argument);
                    String defaultValue = actionMeta.getDefaultValueString(o);
                    if (defaultValue != null) {
                        description += " (defaults to " + o.toString() + ")";
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
                out.println("    <tr>");
                out.println("      <td>" + opt + "</td>");
                out.println("      <td>" + description + "</td>");
                out.println("    </tr>");
            }

            out.println("    </informaltable>");
            out.println("  </section>");
        }

        String desc = actionMeta.getDetailedDescription();
        if (desc != null) {
            out.println("  <section>");
            out.println("    <title>Details</title>");
            out.println("    <para>");
            out.println(desc);
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
