/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
// DWB14: parser loops if // comment at start of program
// DWB15: allow program to have trailing ';'
package org.apache.karaf.shell.support.parsing;

import java.util.List;

import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;

public class DefaultParser implements Parser {

    @Override
    public CommandLine parse(Session session, String command, int cursor) {
        GogoParser parser = new GogoParser(command, cursor);
        List<String> args = parser.statement();
        return new CommandLineImpl(
                        args.toArray(new String[args.size()]),
                        parser.cursorArgumentIndex(),
                        parser.argumentPosition(),
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
