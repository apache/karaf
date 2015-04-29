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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.action.command.ActionCommand;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.AnsiPrintingWikiVisitor;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.WikiParser;
import org.apache.karaf.shell.impl.console.commands.help.wikidoc.WikiVisitor;
import org.apache.karaf.shell.support.ansi.SimpleAnsi;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class ShellHelpProvider implements HelpProvider {

    public String getHelp(Session session, String path) {
        if (path.indexOf('|') > 0) {
            if (path.startsWith("shell|")) {
                path = path.substring("shell|".length());
            } else {
                return null;
            }
        }

        // Retrieve matching commands
        Set<Command> commands = getCommands(session, path);

        // Compute the scopes and matching bundles
        Set<Bundle> bundles = new HashSet<>();
        Set<String> scopes = new HashSet<>();
        for (Command command : commands) {
            if (command instanceof ActionCommand) {
                Class<? extends Action> action = ((ActionCommand) command).getActionClass();
                bundles.add(FrameworkUtil.getBundle(action));
            }
            scopes.add(command.getScope());
        }
        // When there is a single scope / bundle and a matching information file
        // use that one instead
        if (scopes.size() == 1 && bundles.size() == 1 && path.equals(scopes.iterator().next())) {
            Bundle bundle = bundles.iterator().next();
            URL resource = bundle.getResource("OSGI-INF/shell-" + path + ".info");
            if (resource != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream()))) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    PrintStream ps = new PrintStream(baos);
                    int maxSize = 80;
                    Terminal terminal = session.getTerminal();
                    if (terminal != null) {
                        maxSize = terminal.getWidth();
                    }
                    WikiVisitor visitor = new AnsiPrintingWikiVisitor(ps, maxSize);
                    WikiParser parser = new WikiParser(visitor);
                    parser.parse(reader);
                    return baos.toString();
                } catch (IOException e) {
                     // ignore
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            printShellHelp(session, new PrintStream(baos), path);
            return baos.toString();
        }

        return null;
    }

    private Set<Command> getCommands(Session session, String path) {
        // TODO: this is not really clean

        List<Command> commands = session.getRegistry().getCommands();

        String subshell = (String) session.get(Session.SUBSHELL);
        String completionMode = (String) session.get(Session.COMPLETION_MODE);

        Set<Command> matchingCommands = new HashSet<>();
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

            matchingCommands.add(command);
        }
        return matchingCommands;
    }

    protected void printShellHelp(Session session, PrintStream out, String path) {
        out.println(SimpleAnsi.INTENSITY_BOLD + "SUBSHELL" + SimpleAnsi.INTENSITY_NORMAL);
        out.println("\t" + SimpleAnsi.INTENSITY_BOLD + path + SimpleAnsi.INTENSITY_NORMAL);
        out.println();
        out.println(SimpleAnsi.INTENSITY_BOLD + "COMMANDS" + SimpleAnsi.INTENSITY_NORMAL);
        out.println("${command-list|" + path + "|indent}");
    }

}