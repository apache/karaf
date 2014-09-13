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
import java.security.PrivilegedAction;

import javax.security.auth.Subject;

import jline.Terminal;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.jaas.modules.JaasHelper;
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
    private ThreadIO threadIO;
    private ServiceRegistration registration;

    public LocalConsoleManager(boolean start, 
            String defaultStartLevel,
            BundleContext bundleContext, 
            TerminalFactory terminalFactory, 
            ConsoleFactory consoleFactory,
            CommandProcessor commandProcessor,
            ThreadIO threadIO) throws Exception {
        this.start = start;
        this.defaultStartLevel = Integer.parseInt(defaultStartLevel);
        this.bundleContext = bundleContext;
        this.terminalFactory = terminalFactory;
        this.consoleFactory = consoleFactory;
        this.commandProcessor = commandProcessor;
        this.threadIO = threadIO;
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
        final Runnable callback = new Runnable() {
            public void run() {
                try {
                    bundleContext.getBundle(0).stop();
                } catch (Exception e) {
                    // Ignore
                }
            }
        };
        String ctype = System.getenv("LC_CTYPE");
        final String encoding;
        if (ctype != null && ctype.indexOf('.') > 0) {
            encoding = ctype.substring(ctype.indexOf('.') + 1);
        } else {
            encoding = System.getProperty("input.encoding", Charset.defaultCharset().name());
        }

        Runnable consoleStarter = new Runnable() {
            public void run() {
                new Thread("Karaf Console Local for user " + JaasHelper.getUserName(subject)) {
                    @Override
                    public void run() {
                        JaasHelper.doAs(subject, new PrivilegedAction<Object>() {
                            @Override
                            public Object run() {
                                console = consoleFactory.createLocal(commandProcessor, threadIO, terminal, encoding, callback);
                                registration = bundleContext.registerService(CommandSession.class, console.getSession(), null);
                                CommandSession session = console.getSession();
                                session.put("USER", JaasHelper.getUserName(subject));
                                console.run();
                                return null;
                            }
                        });
                    }
                }.start();
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
