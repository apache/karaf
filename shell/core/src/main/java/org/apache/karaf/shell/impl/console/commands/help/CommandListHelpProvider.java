/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.impl.console.commands.help;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.NameScoping;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;

public class CommandListHelpProvider implements HelpProvider {

    public String getHelp(Session session, String path) {
        String mode = "raw";
        if (path.indexOf('|') > 0) {
            if (path.startsWith("command-list|")) {
                path = path.substring("command-list|".length());
                if (path.indexOf('|') > 0) {
                    mode = path.substring(path.indexOf('|') + 1);
                    path = path.substring(0, path.indexOf('|'));
                }
            } else {
                return null;
            }
        } else {
            return null;
        }

        // Retrieve matching commands
        Set<Command> commands = getCommands(session, path);
        SortedMap<String,String> descriptions = new TreeMap<>();
        for (Command command : commands) {
            String subshell = (String) session.get(Session.SUBSHELL);
            String name = command.getScope() + ":" + command.getName();
            String description = command.getDescription();
            if (name.startsWith("*:")) {
                name = name.substring(2);
            }
            if (subshell != null && !subshell.trim().isEmpty() && name.startsWith(subshell + ":")) {
                name = name.substring(subshell.length() + 1);
            }
            descriptions.put(name, description);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (descriptions.isEmpty()) {
            return null;
        } else if (descriptions.size() == 1 && descriptions.containsKey(path)) {
            return null;
        } else {
            printMethodList(session, new PrintStream(baos), descriptions, Arrays.asList(mode.split(",")));
            return baos.toString();
        }
    }

    private Set<Command> getCommands(Session session, String path) {
        // TODO: this is not really clean

        List<Command> commands = session.getRegistry().getCommands();

        String subshell = (String) session.get(Session.SUBSHELL);
        String completionMode = (String) session.get(Session.COMPLETION_MODE);

        Set<Command> matchingCommands = new HashSet<>();
        for (Command command : commands) {

            String name = command.getScope() + ":" + command.getName();

            if (!name.startsWith(path)) {
                continue;
            }

            if (completionMode != null && completionMode.equalsIgnoreCase(Session.COMPLETION_MODE_SUBSHELL)) {
                // filter the help only for "global" commands
                if (subshell == null || subshell.trim().isEmpty()) {
                    if (!name.startsWith(Session.SCOPE_GLOBAL)) {
                        continue;
                    }
                }
            }

            if (completionMode != null && (completionMode.equalsIgnoreCase(Session.COMPLETION_MODE_SUBSHELL)
                                                || completionMode.equalsIgnoreCase(Session.COMPLETION_MODE_FIRST))) {
                // filter the help only for commands local to the subshell
                if (!name.startsWith(subshell)) {
                    continue;
                }
            }

            matchingCommands.add(command);
        }
        return matchingCommands;
    }

    protected void printMethodList(Session session, PrintStream out, SortedMap<String, String> commands, Collection<String> modes) {
        boolean list = false;
        boolean cyan = false;
        int indent = 0;
        for (String mode : modes) {
            if (mode.equals("list")) {
                list = true;
            } else if (mode.equals("cyan")) {
                cyan = true;
            } else if (mode.equals("indent")) {
                indent = 3;
            } else if (mode.startsWith("indent=")) {
                indent = Integer.parseInt(mode.substring("indent=".length())) - 1;
            }
        }

        Terminal term = session.getTerminal();
        int termWidth = term != null ? term.getWidth() : 80;
        ShellTable table = new ShellTable().noHeaders().separator(" ").size(termWidth - 1);
        Col col = new Col("Command").maxSize(64);
        if (indent > 0 || list) {
            table.column(new Col(""));
        }
        if (cyan) {
            col.cyan();
        } else {
            col.bold();
        }
        table.column(col);
        table.column(new Col("Description").wrap());
        for (Map.Entry<String,String> entry : commands.entrySet()) {
            String key = NameScoping.getCommandNameWithoutGlobalPrefix(session, entry.getKey());
            if (indent > 0 || list) {
                StringBuilder prefix = new StringBuilder();
                for (int i = 0; i < indent; i++) {
                    prefix.append(" ");
                }
                if (list) {
                    prefix.append(" *");
                }
                table.addRow().addContent(prefix.toString(), key, entry.getValue());
            } else {
                table.addRow().addContent(key, entry.getValue());
            }
        }
        table.print(out, true);
    }
    
}
