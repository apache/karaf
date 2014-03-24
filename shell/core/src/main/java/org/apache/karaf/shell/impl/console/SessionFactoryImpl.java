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
package org.apache.karaf.shell.impl.console;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.console.commands.ExitCommand;
import org.apache.karaf.shell.impl.console.commands.SubShellCommand;
import org.apache.karaf.shell.impl.console.commands.help.HelpCommand;

public class SessionFactoryImpl extends RegistryImpl implements SessionFactory, Registry {

    final CommandProcessorImpl commandProcessor;
    final ThreadIO threadIO;
    final List<Session> sessions = new ArrayList<Session>();
    final Map<String, SubShellCommand> subshells = new HashMap<String, SubShellCommand>();
    boolean closed;

    public SessionFactoryImpl(ThreadIO threadIO) {
        super(null);
        this.threadIO = threadIO;
        commandProcessor = new CommandProcessorImpl(threadIO);
        register(new ExitCommand());
        new HelpCommand(this);
    }

    public CommandProcessorImpl getCommandProcessor() {
        return commandProcessor;
    }

    @Override
    public Registry getRegistry() {
        return this;
    }

    @Override
    public void register(Object service) {
        synchronized (services) {
            if (service instanceof Command) {
                Command command = (Command) service;
                String scope = command.getScope();
                String name = command.getName();
                if (!Session.SCOPE_GLOBAL.equals(scope)) {
                    if (!subshells.containsKey(scope)) {
                        SubShellCommand subShell = new SubShellCommand(scope);
                        subshells.put(scope, subShell);
                        register(subShell);
                    }
                    subshells.get(scope).increment();
                }
                commandProcessor.addCommand(scope, wrap(command), name);
            }
            super.register(service);
        }
    }

    protected Function wrap(Command command) {
        return new CommandWrapper(command);
    }

    @Override
    public void unregister(Object service) {
        synchronized (services) {
            super.unregister(service);
            if (service instanceof Command) {
                Command command = (Command) service;
                String scope = command.getScope();
                String name = command.getName();
                commandProcessor.removeCommand(scope, name);
                if (!Session.SCOPE_GLOBAL.equals(scope)) {
                    if (subshells.get(scope).decrement() == 0) {
                        SubShellCommand subShell = subshells.remove(scope);
                        unregister(subShell);
                    }
                }
            }
        }
    }

    @Override
    public Session create(InputStream in, PrintStream out, PrintStream err, Terminal term, String encoding, Runnable closeCallback) {
        synchronized (sessions) {
            if (closed) {
                throw new IllegalStateException("SessionFactory has been closed");
            }
            final Session session = new ConsoleSessionImpl(this, commandProcessor, threadIO, in, out, err, term, encoding, closeCallback);
            sessions.add(session);
            return session;
        }
    }

    @Override
    public Session create(InputStream in, PrintStream out, PrintStream err) {
        synchronized (sessions) {
            if (closed) {
                throw new IllegalStateException("SessionFactory has been closed");
            }
            final Session session = new HeadlessSessionImpl(this, commandProcessor, in, out, err);
            sessions.add(session);
            return session;
        }
    }

    public void stop() {
        synchronized (sessions) {
            closed = true;
            for (Session session : sessions) {
                session.close();
            }
            commandProcessor.stop();
        }
    }

}
