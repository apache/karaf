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

import java.nio.charset.Charset;

import javax.security.auth.Subject;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.ConsoleFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocalConsoleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LocalConsoleManager.class);

    private ConsoleFactory consoleFactory;
    private BundleContext bundleContext;
    private TerminalFactory terminalFactory;
    private Console console;
    private boolean start;
    private final int defaultStartLevel;
    private CommandProcessor commandProcessor;
    private ServiceRegistration registration;

    public LocalConsoleManager(boolean start,
            String defaultStartLevel,
            BundleContext bundleContext,
            TerminalFactory terminalFactory,
            ConsoleFactory consoleFactory,
            CommandProcessor commandProcessor) throws Exception {
        this.start = start;
        this.defaultStartLevel = Integer.parseInt(defaultStartLevel);
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

        String roles = System.getProperty("karaf.local.roles");
        if (roles != null) {
            for (String role : roles.split("[,]")) {
                subject.getPrincipals().add(new RolePrincipal(role.trim()));
            }
        }

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
        String ctype = System.getenv("LC_CTYPE");
        String encoding = ctype;
        if (encoding != null && encoding.indexOf('.') > 0) {
            encoding = encoding.substring(encoding.indexOf('.') + 1);
        } else {
            encoding = System.getProperty("input.encoding", Charset.defaultCharset().name());
        }
        this.console = consoleFactory.createLocal(this.commandProcessor, terminal, encoding, callback);

        registration = bundleContext.registerService(CommandSession.class, console.getSession(), null);

        Runnable consoleStarter = new Runnable() {
            public void run() {
                consoleFactory.startConsoleAs(console, subject, "Local");
            }
        };

        boolean delayconsole = Boolean.parseBoolean(System.getProperty("karaf.delay.console"));
        if (delayconsole) {
            DelayedStarted watcher = new DelayedStarted(consoleStarter, bundleContext, System.in);
            new Thread(watcher).start();
        } else {
            consoleStarter.run();
        }
    }

    public void stop() throws Exception {
        if (registration != null) {
            registration.unregister();
        }
        // The bundle is stopped
        // so close the console and remove the callback so that the
        // osgi framework isn't stopped
        if (console != null) {
            console.close(false);
        }
    }

}
