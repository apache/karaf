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
package org.apache.karaf.shell.impl.action.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Parsing;
import org.apache.karaf.shell.api.console.Candidate;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;

public class ActionCommand implements org.apache.karaf.shell.api.console.Command {

    private final ManagerImpl manager;
    private final Class<? extends Action> actionClass;

    public ActionCommand(ManagerImpl manager, Class<? extends Action> actionClass) {
        this.manager = manager;
        this.actionClass = actionClass;
    }

    public Class<? extends Action> getActionClass() {
        return actionClass;
    }

    @Override
    public String getScope() {
        return actionClass.getAnnotation(Command.class).scope();
    }

    @Override
    public String getName() {
        return actionClass.getAnnotation(Command.class).name();
    }

    @Override
    public String getDescription() {
        return actionClass.getAnnotation(Command.class).description();
    }

    @Override
    public Completer getCompleter(boolean scoped) {
        return new ArgumentCompleter(this, scoped);
    }

    @Override
    public Parser getParser() {
        Parsing parsing = actionClass.getAnnotation(Parsing.class);
        if (parsing != null) {
            return new DelayedParser(parsing.value());
        }
        return null;
    }

    protected Completer getCompleter(Class<?> clazz) {
        return new DelayedCompleter(clazz);
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        Action action = createNewAction(session);
        try {
            if (new DefaultActionPreparator().prepare(action, session, arguments)) {
                return action.execute();
            }
        } finally {
            releaseAction(action);
        }
        return null;
    }

    protected Action createNewAction(Session session) {
        try {
            return manager.instantiate(actionClass, session.getRegistry());
        } catch (Exception e) {
            throw new RuntimeException("Unable to creation command action " + actionClass.getName(), e);
        }
    }

    protected void releaseAction(Action action) throws Exception {
        manager.release(action);
    }

    public static class DelayedCompleter implements Completer {
        private final Class<?> clazz;

        public DelayedCompleter(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public int complete(Session session, CommandLine commandLine, List<String> candidates) {
            Object service = session.getRegistry().getService(clazz);
            if (service instanceof Completer) {
                return ((Completer) service).complete(session, commandLine, candidates);
            }
            return -1;
        }

        @Override
        public void completeCandidates(Session session, CommandLine commandLine, List<Candidate> candidates) {
            Object service = session.getRegistry().getService(clazz);
            if (service instanceof Completer) {
                ((Completer) service).completeCandidates(session, commandLine, candidates);
            }
        }
    }

    public static class DelayedParser implements Parser {
        private final Class<?> clazz;

        public DelayedParser(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public CommandLine parse(Session session, String command, int cursor) {
            Object service = session.getRegistry().getService(clazz);
            if (service instanceof Parser) {
                return ((Parser) service).parse(session, command, cursor);
            }
            throw new IllegalStateException("Could not find specified parser");
        }

        @Override
        public String preprocess(Session session, CommandLine commandLine) {
            Object service = session.getRegistry().getService(clazz);
            if (service instanceof Parser) {
                return ((Parser) service).preprocess(session, commandLine);
            }
            throw new IllegalStateException("Could not find specified parser");
        }
    }

}
