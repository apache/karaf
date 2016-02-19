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
package org.apache.karaf.shell.impl.console;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.felix.gogo.runtime.threadio.ThreadIOImpl;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CompleterAsCompletorTest {

    @Test
    public void testCompletionMultiCommand() {
        SessionFactoryImpl sessionFactory = new SessionFactoryImpl(new ThreadIOImpl());
        Session session = new HeadlessSessionImpl(sessionFactory, sessionFactory.getCommandProcessor(),
                new ByteArrayInputStream(new byte[0]), new PrintStream(new ByteArrayOutputStream()), new PrintStream(new ByteArrayOutputStream())
        );

        Completer completer = new Completer() {
            @Override
            public int complete(Session session, CommandLine commandLine, List<String> candidates) {
                assertEquals(" bundle:l", commandLine.getBuffer());
                candidates.add("bundle:list");
                return 1;
            }
        };
        jline.console.completer.Completer cmp = new CompleterAsCompletor(session, completer);

        String cmd = "bundle:list ; bundle:l";
        List<CharSequence> candidates = new ArrayList<>();
        int pos = cmp.complete(cmd, cmd.length(), candidates);

        assertEquals("bundle:list ; ".length(), pos);
    }
}
