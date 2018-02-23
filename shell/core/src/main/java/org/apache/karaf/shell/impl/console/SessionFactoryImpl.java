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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.felix.gogo.jline.Builtin;
import org.apache.felix.gogo.jline.Posix;
import org.apache.felix.gogo.runtime.CommandProcessorImpl;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.gogo.runtime.Reflective;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.felix.service.threadio.ThreadIO;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Registry;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.apache.karaf.shell.api.console.Terminal;
import org.apache.karaf.shell.impl.console.commands.ExitCommand;
import org.apache.karaf.shell.impl.console.commands.Procedural;
import org.apache.karaf.shell.impl.console.commands.SubShellCommand;
import org.apache.karaf.shell.impl.console.commands.help.HelpCommand;

public class SessionFactoryImpl extends RegistryImpl implements SessionFactory, Registry {

    final CommandProcessorImpl commandProcessor;
    final ThreadIO threadIO;
    final Map<String, SubShellCommand> subshells = new HashMap<>();
    boolean closed;

    public SessionFactoryImpl(ThreadIO threadIO) {
        super(null);
        this.threadIO = threadIO;
        commandProcessor = new CommandProcessorImpl(threadIO) {
            @Override
            public Object invoke(CommandSessionImpl session, Object target, String name, List<Object> args) throws Exception {
                return SessionFactoryImpl.this.invoke(session, target, name, args);
            }

            @Override
            public Path redirect(CommandSessionImpl session, Path path, int mode) {
                return SessionFactoryImpl.this.redirect(session, path, mode);
            }
        };
        register(new ExitCommand());
        new HelpCommand(this);
        register(new ShellCommand("addCommand", "Add a command", commandProcessor, "addCommand"));
        register(new ShellCommand("removeCommand", "Remove a command", commandProcessor, "removeCommand"));
        register(new ShellCommand("eval", "Evaluate", commandProcessor, "eval"));

        Builtin builtin = new Builtin();
        for (String name : new String[]{"format", "getopt", "new", "set", "tac", "type", "jobs", "fg", "bg", "keymap", "setopt", "unsetopt", "complete", "history", "widget", "__files", "__directories", "__usage_completion"}) {
            register(new ShellCommand(name, null, builtin, name));
        }

        Procedural procedural = new Procedural();
        for (String name : new String[]{"each", "if", "not", "throw", "try", "until", "while", "break", "continue"}) {
            register(new ShellCommand(name, null, procedural, name));
        }

        Posix posix = new Posix(commandProcessor);
        for (String name : new String[]{"cat", "echo", "grep", "sort", "sleep", "cd", "pwd", "ls", "less", "nano", "head", "tail", "clear", "wc", "date", "tmux", "ttop"}) {
            register(new ShellCommand(name, null, posix, name));
        }
    }

    protected Object invoke(CommandSessionImpl session, Object target, String name, List<Object> args) throws Exception {
        return Reflective.invoke(session, target, name, args);
    }

    protected Path redirect(CommandSessionImpl session, Path path, int mode) {
        return session.currentDir().resolve(path);
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
        synchronized (commandProcessor) {
            if (closed) {
                throw new IllegalStateException("SessionFactory has been closed");
            }
            final Session session = new ConsoleSessionImpl(this, commandProcessor, threadIO, in, out, err, term, encoding, closeCallback);
            if (this.session == null) {
                this.session = session;
            }
            return session;
        }
    }

    @Override
    public Session create(InputStream in, PrintStream out, PrintStream err) {
        return create(in, out, err, null);
    }

    @Override
    public Session create(InputStream in, PrintStream out, PrintStream err, Session parent) {
        synchronized (commandProcessor) {
            if (closed) {
                throw new IllegalStateException("SessionFactory has been closed");
            }
            final Session session = new HeadlessSessionImpl(this, commandProcessor, in, out, err, parent);
            return session;
        }
    }

    public void stop() {
        synchronized (commandProcessor) {
            closed = true;
            commandProcessor.stop();
        }
    }

    private static class ShellCommand implements Command {
        private final String name;
        private final String desc;
        private final Executable consumer;

        interface Executable {
            Object execute(CommandSession session, List<Object> args) throws Exception;
        }

        interface ExecutableStr {
            void execute(CommandSession session, String[] args) throws Exception;
        }

        public ShellCommand(String name, String desc, Executable consumer) {
            this.name = name;
            this.desc = desc;
            this.consumer = consumer;
        }

        public ShellCommand(String name, String desc, ExecutableStr consumer) {
            this(name, desc, wrap(consumer));
        }

        public ShellCommand(String name, String desc, Object target, String method) {
            this(name, desc, wrap(target, method));
        }

        private static Executable wrap(Object target, String name) {
            return (session, args) -> Reflective.invoke(session, target, name, args);
        }

        private static Executable wrap(ExecutableStr command) {
            return (session, args) -> {
                command.execute(session, asStringArray(args));
                return null;
            };
        }

        private static String[] asStringArray(List<Object> args) {
            String[] argv = new String[args.size()];
            for (int i = 0; i < argv.length; i++) {
                argv[i] = Objects.toString(args.get(i));
            }
            return argv;
        }

        @Override
        public String getScope() {
            return "shell";
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String getDescription() {
            return desc;
        }

        @Override
        public Completer getCompleter(boolean scoped) {
            return null;
        }

        @Override
        public Parser getParser() {
            return null;
        }

        @Override
        public Object execute(Session session, List<Object> arguments) throws Exception {
            CommandSession cmdSession = (CommandSession) session.get(".commandSession");
            return consumer.execute(cmdSession, arguments);
        }
    }

}
