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
package org.apache.karaf.shell.console.impl.jline;

import javax.security.auth.Subject;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.ConsoleFactory;
import org.osgi.framework.BundleContext;

public class LocalConsoleManager {

    private ConsoleFactory consoleFactory;
    private BundleContext bundleContext;
    private TerminalFactory terminalFactory;
    private Console console;
    private boolean start;
    private CommandProcessor commandProcessor;

    public LocalConsoleManager(boolean start, 
            BundleContext bundleContext, 
            TerminalFactory terminalFactory, 
            ConsoleFactory consoleFactory,
            CommandProcessor commandProcessor) throws Exception {
        this.start = start;
        this.bundleContext = bundleContext;
        this.terminalFactory = terminalFactory;
        this.consoleFactory = consoleFactory;
        this.commandProcessor = commandProcessor;
        start();
    }

    public void start() throws Exception {
        if (!start) {
            return;
        }
        final Subject subject = new Subject();
        subject.getPrincipals().add(new UserPrincipal("karaf"));

        final Terminal terminal = terminalFactory.getTerminal();
        Runnable callback = new Runnable() {
            public void run() {
                try {
                    bundleContext.getBundle(0).stop();
                } catch (Exception e) {
                    // Ignore
                }
            }
        };
        this.console = consoleFactory.createLocalAndStart(subject, this.commandProcessor, terminal, callback);
    }

    public void stop() throws Exception {
        if (console != null) {
            console.close();
        }
    }

}
