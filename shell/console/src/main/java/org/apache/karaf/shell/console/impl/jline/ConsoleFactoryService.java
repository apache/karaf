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
import java.security.PrivilegedAction;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.security.auth.Subject;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.ConsoleFactory;

public class ConsoleFactoryService implements ConsoleFactory {
    
    @Override
    public Console createLocal(CommandProcessor processor, final Terminal terminal, String encoding, Runnable closeCallback) {
        return create(processor, 
                StreamWrapUtil.reWrapIn(terminal, System.in), 
                StreamWrapUtil.reWrap(System.out), 
                StreamWrapUtil.reWrap(System.err), 
                terminal,
                encoding,
                closeCallback);
    }

    @Override
    public Console create(CommandProcessor processor, InputStream in, PrintStream out, PrintStream err, final Terminal terminal,
            String encoding, Runnable closeCallback) {
        ConsoleImpl console = new ConsoleImpl(processor, in, out, err, terminal, encoding, closeCallback);
        CommandSession session = console.getSession();
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
        addSystemProperties(session);
        return console;
    }

    private void addSystemProperties(CommandSession session) {
        Properties sysProps = System.getProperties();
        Iterator<Object> it = sysProps.keySet().iterator();
        while (it.hasNext()) {
            String key = (String) it.next();
            session.put(key, System.getProperty(key));
        }
    }
    
    @Override
    public void startConsoleAs(final Console console, final Subject subject, String consoleType) {
        final String userName = getUserName(subject);
        new Thread(console, "Karaf Console " + consoleType + " user " + userName) {
            @Override
            public void run() {
                if (subject != null) {
                    CommandSession session = console.getSession();
                    session.put("USER", userName);
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

    private String getUserName(final Subject subject) {
        if (subject != null && subject.getPrincipals().iterator().hasNext()) {
            return subject.getPrincipals(UserPrincipal.class).iterator().next().getName();
        } else {
            return null;
        }
    }
}
