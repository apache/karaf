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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.security.KeyPair;
import java.util.Hashtable;
import javax.security.auth.Subject;

import jline.Terminal;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.apache.karaf.shell.console.Console;
import org.apache.karaf.shell.console.ConsoleFactory;
import org.apache.sshd.agent.SshAgent;
import org.apache.sshd.agent.local.AgentImpl;
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
    private CommandProcessor commandProcessor;
    private ServiceRegistration registration;
    private SshAgent local;

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
        String agentId = startAgent("karaf");
        this.console = consoleFactory.createLocalAndStart(subject, this.commandProcessor, terminal, callback);
        this.console.getSession().put(SshAgent.SSH_AUTHSOCKET_ENV_NAME, agentId);
    }

    protected String startAgent(String user) {
        try {
            local = new AgentImpl();
            URL url = bundleContext.getBundle().getResource("karaf.key");
            InputStream is = url.openStream();
            ObjectInputStream r = new ObjectInputStream(is);
            KeyPair keyPair = (KeyPair) r.readObject();
            local.addIdentity(keyPair, "karaf");
            String agentId = "local:" + user;
            Hashtable properties = new Hashtable();
            properties.put("id", agentId);
            registration = bundleContext.registerService(SshAgent.class.getName(), local, properties);
            return agentId;
        } catch (Throwable e) {
            LOGGER.warn("Error starting ssh agent for local console", e);
            return null;
        }
    }

    public void stop() throws Exception {
        if (registration != null) {
            registration.unregister();
        }
        if (console != null) {
            console.close();
        }
    }

}
