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
package org.apache.karaf.shell.impl.console.osgi.secured;

import java.util.List;

import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.gogo.runtime.Token;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Parser;
import org.apache.karaf.shell.api.console.Session;

public class SecuredCommand implements Command, Function {

    private final SecuredSessionFactoryImpl factory;
    private final Command command;

    public SecuredCommand(SecuredSessionFactoryImpl factory, Command command) {
        this.command = command;
        this.factory = factory;
    }

    public String getScope() {
        return command.getScope();
    }

    public String getName() {
        return command.getName();
    }

    @Override
    public String getDescription() {
        return command.getDescription();
    }

    @Override
    public Completer getCompleter(boolean scoped) {
        return command.getCompleter(scoped);
    }

    @Override
    public Parser getParser() {
        return null;
    }

    @Override
    public Object execute(Session session, List<Object> arguments) throws Exception {
        factory.checkSecurity(getScope(), getName(), arguments);
        return command.execute(session, arguments);
    }

    @Override
    public Object execute(final CommandSession commandSession, List<Object> arguments) throws Exception {
        // TODO: remove the hack for .session
        Session session = (Session) commandSession.get(".session");
        // When need to translate closures to a compatible type for the command
        for (int i = 0; i < arguments.size(); i++) {
            Object v = arguments.get(i);
            if (v instanceof Closure) {
                final Closure closure = (Closure) v;
                arguments.set(i, new VersatileFunction(closure));
            }
            if (v instanceof Token) {
                arguments.set(i, v.toString());
            }
        }
        return execute(session, arguments);
    }

    static class VersatileFunction implements org.apache.felix.service.command.Function,
            org.apache.karaf.shell.api.console.Function {

        private final Closure closure;

        VersatileFunction(Closure closure) {
            this.closure = closure;
        }


        @Override
        public Object execute(CommandSession commandSession, List<Object> list) throws Exception {
            return closure.execute(commandSession, list);
        }

        @Override
        public Object execute(Session session, List<Object> arguments) throws Exception {
            CommandSession commandSession = (CommandSession) session.get(".commandSession");
            return closure.execute(commandSession, arguments);
        }

        @Override
        public String toString() {
            return closure.toString();
        }
    }

}
