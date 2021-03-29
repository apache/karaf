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

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.History;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Signal;
import org.apache.karaf.shell.api.console.SignalListener;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.console.parsing.CommandLineParser;
import org.apache.karaf.shell.support.ShellUtil;
import org.jline.terminal.impl.DumbTerminal;

public class HeadlessSessionImpl implements Session {

    final Session parent;
    final Terminal terminal;
    final SessionFactory factory;
    final CommandSession session;
    final Registry registry;

    public HeadlessSessionImpl(SessionFactory factory, CommandProcessor processor, InputStream in, PrintStream out, PrintStream err) {
        this(factory, processor, in, out, err, null);
    }

    public HeadlessSessionImpl(SessionFactory factory, CommandProcessor processor, InputStream in, PrintStream out, PrintStream err, Session parent) {
        // Parent session
        this.parent = parent;
        // Terminal
        this.terminal = parent != null ? new ReadOnlyTerminal(parent.getTerminal()) : null;
        // Factory
        this.factory = factory;
        // Registry
        registry = new RegistryImpl(factory.getRegistry());
        registry.register(factory);
        registry.register(this);
        registry.register(registry);
        // Session
        if (in == null || out == null || err == null) {
            try {
                Terminal sessionTerminal = terminal != null ? terminal : 
                    new JLineTerminal(new DumbTerminal(in, out));
                session = processor.createSession(((org.jline.terminal.Terminal) sessionTerminal).input(),
                                                  ((org.jline.terminal.Terminal) sessionTerminal).output(),
                                                  ((org.jline.terminal.Terminal) sessionTerminal).output());
            } catch (IOException e) {
                throw new RuntimeException("Unable to create terminal", e);
            }
            
        } else {
            session = processor.createSession(in, out, err);
        }
        if (parent == null) {
            Properties sysProps = System.getProperties();
            // iterating over sysProps.keySet() directly is not thread-safe and may throw ConcurrentMod.Ex.,
            // so first get the keys and then query their values
            Set<String> keys = sysProps.stringPropertyNames();
            for (String key : keys) {
                session.put(key, sysProps.getProperty(key)); // value can be null if removed in meantime, but unlikely
            }
        }
        session.put(".processor", processor);
        session.put(".session", this);
        session.put(".commandSession", session);
        if (parent == null) {
            session.put(Session.SCOPE, "shell:bundle:*");
            session.put(Session.SUBSHELL, "");
            session.put("USER", ShellUtil.getCurrentUserName());
            session.put("APPLICATION", System.getProperty("karaf.name", "root"));
        }
        session.put(CommandSession.OPTION_NO_GLOB, Boolean.TRUE);
        session.currentDir(Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize());
    }

    public CommandSession getSession() {
        return session;
    }

    @Override
    public Object execute(CharSequence commandline) throws Exception {
        String command = CommandLineParser.parse(this, commandline.toString());
        return session.execute(command);
    }

    @Override
    public Object get(String name) {
        Object val = session.get(name);
        if (val == null && parent != null) {
            val = parent.get(name);
        }
        return val;
    }

    @Override
    public void put(String name, Object value) {
        session.put(name, value);
    }

    @Override
    public InputStream getKeyboard() {
        return session.getKeyboard();
    }

    @Override
    public PrintStream getConsole() {
        return session.getConsole();
    }

    @Override
    public String readLine(String prompt, Character mask) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Terminal getTerminal() {
        return terminal;
    }

    @Override
    public History getHistory() {
        return null;
    }

    @Override
    public Registry getRegistry() {
        return registry;
    }

    @Override
    public SessionFactory getFactory() {
        return factory;
    }

    @Override
    public Path currentDir() {
        return session.currentDir();
    }

    @Override
    public void currentDir(Path path) {
        session.currentDir(path);
    }

    @Override
    public void run() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String resolveCommand(String name) {
        // TODO: optimize
        if (!name.contains(":")) {
            String[] scopes = ((String) get(Session.SCOPE)).split(":");
            List<Command> commands = registry.getCommands();
            for (String scope : scopes) {
                for (Command command : commands) {
                    if ((Session.SCOPE_GLOBAL.equals(scope) || command.getScope().equals(scope)) && command.getName().equals(name)) {
                        return command.getScope() + ":" + name;
                    }
                }
            }
        }
        return name;
    }

    @Override
    public void close() {
        session.close();
    }

    static class ReadOnlyTerminal implements Terminal {

        private final Terminal terminal;

        public ReadOnlyTerminal(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public String getType() {
            return terminal.getType();
        }

        @Override
        public int getWidth() {
            return terminal.getWidth();
        }

        @Override
        public int getHeight() {
            return terminal.getHeight();
        }

        @Override
        public boolean isAnsiSupported() {
            return terminal.isAnsiSupported();
        }

        @Override
        public boolean isEchoEnabled() {
            return terminal.isEchoEnabled();
        }

        @Override
        public void setEchoEnabled(boolean enabled) {
        }

        @Override
        public void addSignalListener(SignalListener listener, Signal... signal) {
        }

        @Override
        public void addSignalListener(SignalListener listener, EnumSet<Signal> signals) {
        }

        @Override
        public void addSignalListener(SignalListener listener) {
        }

        @Override
        public void removeSignalListener(SignalListener listener) {
        }
    }

}
