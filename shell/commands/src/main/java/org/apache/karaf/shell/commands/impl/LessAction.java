/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.commands.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.jline.builtins.Less;
import org.jline.builtins.Source;
import org.jline.builtins.Source.StdInSource;
import org.jline.builtins.Source.URLSource;
import org.jline.terminal.Terminal;

@Command(scope = "shell", name = "less", description = "File pager.")
@Service
public class LessAction implements Action {

    @Option(name = "-e", aliases = "--quit-at-eof")
    boolean quitAtSecondEof;

    @Option(name = "-E", aliases = "--QUIT-AT-EOF")
    boolean quitAtFirstEof;

    @Option(name = "-N", aliases = "--LINE-NUMBERS")
    boolean printLineNumbers;

    @Option(name = "-q", aliases = {"--quiet", "--silent"})
    boolean quiet;

    @Option(name = "-Q", aliases = {"--QUIET", "--SILENT"})
    boolean veryQuiet;

    @Option(name = "-S", aliases = "--chop-long-lines")
    boolean chopLongLines;

    @Option(name = "-i", aliases = "--ignore-case")
    boolean ignoreCaseCond;

    @Option(name = "-I", aliases = "--IGNORE-CASE")
    boolean ignoreCaseAlways;

    @Option(name = "-x", aliases = "--tabs")
    int tabs = 4;

    @Argument(multiValued = true)
    List<String> files;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        Terminal terminal = (Terminal) session.get(".jline.terminal");
        Less less = new Less(terminal);
        less.quitAtFirstEof = quitAtFirstEof;
        less.quitAtSecondEof = quitAtSecondEof;
        less.quiet = quiet;
        less.veryQuiet = veryQuiet;
        less.chopLongLines = chopLongLines;
        less.ignoreCaseAlways = ignoreCaseAlways;
        less.ignoreCaseCond = ignoreCaseCond;
        less.tabs = tabs;
        less.printLineNumbers = printLineNumbers;
        List<Source> sources = new ArrayList<>();
        if (files.isEmpty()) {
            files.add("-");
        }
        Path pwd = Paths.get(System.getProperty("karaf.home"));
        for (String arg : files) {
            if ("-".equals(arg)) {
                sources.add(new StdInSource());
            } else {
                sources.add(new URLSource(pwd.resolve(arg).toUri().toURL(), arg));
            }
        }
        less.run(sources);
        return null;
    }

}
