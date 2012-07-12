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
package org.apache.karaf.shell.console.impl.jline;

import java.io.InputStream;
import java.io.PrintStream;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.security.auth.Subject;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.ConsoleFactory;

public class ConsoleFactoryService implements ConsoleFactory {
    
    @Override
    public Console createLocalAndStart(Subject user, CommandProcessor processor, final Terminal terminal,
            Runnable closeCallback) throws Exception {
        return createAndStart(user, 
                processor, 
                StreamWrapUtil.reWrapIn(terminal, System.in), 
                StreamWrapUtil.reWrap(System.out), 
                StreamWrapUtil.reWrap(System.err), 
                terminal, 
                closeCallback);
    }

    @Override
    public Console createAndStart(Subject subject, CommandProcessor processor, InputStream in, PrintStream out, PrintStream err, final Terminal terminal,
            Runnable closeCallback) throws Exception {
        ConsoleImpl console = new ConsoleImpl(processor, in, out, err, terminal, closeCallback);
        CommandSession session = console.getSession();
        String userName = getFirstPrincipalName(subject);
        session.put("USER", userName);
        session.put("APPLICATION", System.getProperty("karaf.name", "root"));
        session.put("#LINES", new Function() {
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return Integer.toString(terminal.getHeight());
            }
        });
        session.put("#COLUMNS", new Function() {
            public Object execute(CommandSession session, List<Object> arguments) throws Exception {
                return Integer.toString(terminal.getWidth());
            }
        });
        session.put(".jline.terminal", terminal);
        startConsoleAs(console, subject);
        return console;
    }
    
    private String getFirstPrincipalName(Subject user) {
        if (user != null) {
            Set<Principal> principals = user.getPrincipals();
            if (principals != null) {
                Iterator<Principal> it = principals.iterator();
                if (it.hasNext()) {
                    return it.next().getName();
                }
            }
        }
        return null;
    }

    private void startConsoleAs(final Console console, final Subject subject) {
        new Thread(console) {
            @Override
            public void run() {
                if (subject != null) {
                    Subject.doAs(subject, new PrivilegedAction<Object>() {
                        public Object run() {
                            doRun();
                            return null;
                        }
                    });
                } else {
                    doRun();
                }
            }
            protected void doRun() {
                super.run();
            }
        }.start();
    }
}
