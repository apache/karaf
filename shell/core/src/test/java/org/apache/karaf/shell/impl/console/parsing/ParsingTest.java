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
package org.apache.karaf.shell.impl.console.parsing;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Parsing;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.impl.action.command.ActionCommand;
import org.apache.karaf.shell.impl.action.command.ManagerImpl;
import org.apache.karaf.shell.impl.console.HeadlessSessionImpl;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf.shell.support.parsing.CommandLineImpl;
import org.apache.karaf.shell.support.parsing.DefaultParser;
import org.apache.karaf.shell.support.parsing.GogoParser;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ParsingTest {

    @Test
    public void testDefaultParser() {
        String command = " foo  bar (a +  b) ; another command ";
        String subCmd = " foo  bar (a +  b) ";
        DefaultParser parser = new DefaultParser();
        CommandLine line = parser.parse(null, command, command.length());
        assertEquals(3, line.getArguments().length);
        assertEquals("foo", line.getArguments()[0]);
        assertEquals("bar", line.getArguments()[1]);
        assertEquals("(a +  b)", line.getArguments()[2]);
        assertEquals(subCmd, line.getBuffer());

    }

    @Test
    public void testCommandLineParser() {

        SessionFactoryImpl sessionFactory = new SessionFactoryImpl(new ThreadIOImpl());
        ManagerImpl manager = new ManagerImpl(sessionFactory, sessionFactory);
        sessionFactory.getRegistry().register(new ActionCommand(manager, FooCommand.class));
        sessionFactory.getRegistry().register(new ActionCommand(manager, AnotherCommand.class));
        sessionFactory.getRegistry().register(new CustomParser());
        Session session = new HeadlessSessionImpl(sessionFactory, sessionFactory.getCommandProcessor(),
                new ByteArrayInputStream(new byte[0]), new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())
        );

        String parsed = CommandLineParser.parse(session, " foo bar (a + b); another   command with spaces ");
        assertEquals("foo bar (a + b) ; another \"command with spaces\"", parsed);
    }

    @Test
    public void testCommandLineParserMultiLine() {

        SessionFactoryImpl sessionFactory = new SessionFactoryImpl(new ThreadIOImpl());
        ManagerImpl manager = new ManagerImpl(sessionFactory, sessionFactory);
        sessionFactory.getRegistry().register(new ActionCommand(manager, FooCommand.class));
        sessionFactory.getRegistry().register(new ActionCommand(manager, AnotherCommand.class));
        sessionFactory.getRegistry().register(new CustomParser());
        Session session = new HeadlessSessionImpl(sessionFactory, sessionFactory.getCommandProcessor(),
                new ByteArrayInputStream(new byte[0]), new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())
        );

        String parsed = CommandLineParser.parse(session, "echo a\necho b");
        assertEquals("echo a\necho b", parsed);
    }

    @Command(scope = "scope", name = "foo")
    static class FooCommand implements Action {
        @Override
        public Object execute() throws Exception {
            return null;
        }
    }

    @Command(scope = "scope", name = "another")
    @Parsing(CustomParser.class)
    static class AnotherCommand implements Action {
        @Override
        public Object execute() throws Exception {
            return null;
        }
    }

    static class CustomParser implements Parser {

        @Override
        public CommandLine parse(Session session, String command, int cursor) {
            GogoParser parser = new GogoParser(command, cursor);
            List<String> args = new ArrayList<>();
            args.add(parser.value());
            parser.ws();
            StringBuilder sb = new StringBuilder();
            while (true) {
                char ch = parser.next();
                if (ch == 0 || ch == ';' || ch == '|') {
                    break;
                } else {
                    sb.append(ch);
                }
            }
            String arg = sb.toString().trim();
            if (!arg.isEmpty()) {
                args.add("\"" + arg + "\"");
            }
            return new CommandLineImpl(args.toArray(new String[args.size()]), args.size() - 1, sb.length(),
                            parser.position(),
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

}
