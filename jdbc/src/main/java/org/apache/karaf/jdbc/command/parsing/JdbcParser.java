package org.apache.karaf.jdbc.command.parsing;

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
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.parsing.CommandLineImpl;
import org.apache.karaf.shell.support.parsing.GogoParser;

/**
 * A parser for JDBC commands using SQL.
 */
@Service
public class JdbcParser implements Parser {

    @Override
    public CommandLine parse(Session session, String command, int cursor) {
        GogoParser parser = new GogoParser(command, cursor);
        List<String> args = parser.statement();
        List<String> nargs = new ArrayList<>();
        int state = 0;
        for (String arg : args) {
            switch (state) {
            // command
            case 0:
                nargs.add(arg);
                state = 1;
                break;
            // option or target
            case 1:
                nargs.add(arg);
                if (!arg.startsWith("-")) {
                    state = 2;
                }
                break;
            // first sql fragment
            case 2:
                nargs.add(arg);
                state = 3;
                break;
            // addtional sql
            case 3:
                nargs.set(nargs.size() - 1, nargs.get(nargs.size() - 1) + " " + arg);
                break;
            }
        }
        nargs.set(nargs.size() - 1, "\"" + nargs.get(nargs.size() - 1) + "\"");
        return new CommandLineImpl(
                nargs.toArray(new String[nargs.size()]),
                parser.cursorArgumentIndex(),
                Math.max(parser.argumentPosition(), nargs.size()),
                cursor,
                command.substring(0, parser.position()));
    }

    @Override
    public String preprocess(Session session, CommandLine cmdLine) {
        StringBuilder parsed = new StringBuilder();
        for (int i = 0 ; i < cmdLine.getArguments().length; i++) {
            String arg = cmdLine.getArguments()[i];
            if (i > 0) {
                parsed.append(" ");
            }
            parsed.append(arg);
        }
        return parsed.toString();
    }
}
