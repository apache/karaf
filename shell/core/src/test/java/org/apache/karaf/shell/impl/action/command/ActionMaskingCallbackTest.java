/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.impl.action.command;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.impl.console.HeadlessSessionImpl;
import org.apache.karaf.shell.impl.console.SessionFactoryImpl;
import org.apache.karaf.shell.impl.console.parsing.KarafParser;
import org.jline.reader.Parser;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;

import static org.junit.Assert.assertEquals;

public class ActionMaskingCallbackTest {

    private Parser parser;
    private ActionMaskingCallback cb;

    @Before
    public void setUp() {
        ThreadIO tio = new ThreadIOImpl();
        CommandProcessor cp = new CommandProcessorImpl(tio);
        SessionFactory sf = new SessionFactoryImpl(tio);
        InputStream is = new ByteArrayInputStream(new byte[0]);
        PrintStream os = new PrintStream(new ByteArrayOutputStream());
        Session session = new HeadlessSessionImpl(sf, cp, is, os, os);
        parser = new KarafParser(session);

        ActionCommand cmd = new ActionCommand(null, UserAddCommand.class);
        cb = ActionMaskingCallback.build(cmd);
    }

    @Test
    public void testJaasUserAdd() throws Exception {
        check("user-add user password ", "user-add user ######## ");
        check("user-add --help user password ", "user-add --help user ######## ");
        check("user-add --help user password foo", "user-add --help user ######## foo");
        check("user-add --opt1 user password foo", "user-add --opt1 user ######## foo");
        check("user-add --opt2 valOpt2 user password foo", "user-add --opt2 valOpt2 user ######## foo");
        check("user-add --opt2=valOpt2 user password foo", "user-add --opt2=valOpt2 user ######## foo");
        check("user-add --opt1 --opt2 valOpt2 --opt3=valOpt3 user password foo", "user-add --opt1 --opt2 valOpt2 --opt3=@@@@@@@ user ######## foo");
        check("user-add --opt1 --opt2 valOpt2 --opt3 valOpt3 user password foo", "user-add --opt1 --opt2 valOpt2 --opt3 @@@@@@@ user ######## foo");
        check("user-add --opt1 --opt2 valOpt2 --opt3 valOpt3 --opt4alias1 censorMe --opt4alias2 censorMeToo user password foo",
                "user-add --opt1 --opt2 valOpt2 --opt3 @@@@@@@ --opt4alias1 ******** --opt4alias2 *********** user ######## foo");
    }

    private void check(String input, String expected) {
        String output = cb.filter(input, parser.parse(input, input.length()));
        assertEquals(expected, output);
    }


    @Command(scope = "jaas", name = "user-add", description = "Add a user")
    @Service
    public static class UserAddCommand implements Action {

        @Option(name = "--opt1")
        private boolean opt1;

        @Option(name = "--opt2")
        private String opt2;

        @Option(name = "--opt3", censor = true, mask = '@')
        private String opt3;

        @Option(name = "--opt4", aliases = {"--opt4alias1", "--opt4alias2"}, censor = true, mask = '*')
        private String opt4;

        @Argument(index = 0, name = "username")
        private String username;

        @Argument(index = 1, name = "password", censor = true, mask = '#')
        private String password;

        @Argument(index = 2, name = "foo")
        private String foo;

        @Override
        public Object execute() throws Exception {
            return null;
        }
    }

}
