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
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.support.NameScoping;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.apache.karaf.shell.support.table.Col;
import org.apache.karaf.shell.support.table.ShellTable;

public class CommandListHelpProvider implements HelpProvider {

    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("command-list|")) {
                path = path.substring("command-list|".length());
            } else {
                return null;
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SortedMap<String, String> commands = getCommandDescriptions(session, path);
        if (commands.isEmpty()) {
            return null;
        } else if (commands.size() == 1 && commands.containsKey(path)) {
            return null;
        } else {
            printMethodList(session, new PrintStream(baos), commands);
            return baos.toString();
        }
    }

    private SortedMap<String, String> getCommandDescriptions(Session session, String path) {
        // TODO: this is not really clean

        List<Command> commands = session.getRegistry().getCommands();

        String subshell = (String) session.get(Session.SUBSHELL);
        String completionMode = (String) session.get(Session.COMPLETION_MODE);

        SortedMap<String,String> descriptions = new TreeMap<String,String>();
        for (Command command : commands) {

            String name = command.getScope() + ":" + command.getName();

            if (command != null && !name.startsWith(path)) {
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

            String description = command.getDescription();
            if (name.startsWith("*:")) {
                name = name.substring(2);
            }
            if (subshell != null && !subshell.trim().isEmpty() && name.startsWith(subshell + ":")) {
                name = name.substring(subshell.length() + 1);
            }
            descriptions.put(name, description);
        }
        return descriptions;
    }

    protected void printMethodList(Session session, PrintStream out, SortedMap<String, String> commands) {
        Terminal term = session.getTerminal();
        int termWidth = term != null ? term.getWidth() : 80;
        out.println(SimpleAnsi.INTENSITY_BOLD + "COMMANDS" + SimpleAnsi.INTENSITY_NORMAL);
        ShellTable table = new ShellTable().noHeaders().separator(" ").size(termWidth);
        table.column(new Col("Command").maxSize(35));
        table.column(new Col("Description"));
        for (Map.Entry<String,String> entry : commands.entrySet()) {
            String key = NameScoping.getCommandNameWithoutGlobalPrefix(session, entry.getKey());
            table.addRow().addContent(key, entry.getValue());
        }
        table.print(out, true);
    }
    
}
