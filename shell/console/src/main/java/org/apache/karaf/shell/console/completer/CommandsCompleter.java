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
package org.apache.karaf.shell.console.completer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.commands.CommandWithAction;
import org.apache.felix.gogo.runtime.Closure;
import org.apache.felix.gogo.runtime.CommandProxy;
import org.apache.felix.gogo.runtime.CommandSessionImpl;
import org.apache.felix.service.command.CommandSession;
import org.apache.felix.service.command.Function;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Like the {@link org.apache.karaf.shell.console.completer.CommandsCompleter} but does not use OSGi but is
 * instead used from the non-OSGi {@link org.apache.karaf.shell.console.impl.Main}
 */
public class CommandsCompleter implements Completer {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandsCompleter.class);

    private CommandSession session;
    private final List<Completer> completers = new ArrayList<Completer>();
    private final Set<String> commands = new HashSet<String>();

    public CommandsCompleter() {
        this(CommandSessionHolder.getSession());
    }

    public CommandsCompleter(CommandSession session) {
        this.session = session;
    }


    public int complete(String buffer, int cursor, List<String> candidates) {
        if (session == null) {
            session = CommandSessionHolder.getSession();
        }
        checkData();
        int res = new AggregateCompleter(completers).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

    @SuppressWarnings("unchecked")
    protected synchronized void checkData() {
        // Copy the set to avoid concurrent modification exceptions
        // TODO: fix that in gogo instead
        // get the current sub-shell
        String subshell = (String) session.get("SUBSHELL");
        Set<String> names = new HashSet<String>((Set<String>) session.get(CommandSessionImpl.COMMANDS));
        Set<String> filteredNames = new HashSet<String>();
        for (String command : names) {
            if (subshell == null || command.startsWith(subshell)) {
                filteredNames.add(command);
            }
        }
        
        if (!filteredNames.equals(commands)) {
            commands.clear();
            completers.clear();

            
            if (subshell == null || subshell.length() == 0) {
                // Add aliases if we are not in a subshell
                Set<String> aliases = this.getAliases();
                completers.add(new StringsCompleter(aliases));
            }

            // add argument completers for each command
            for (String command : names) {
                Function function = (Function) session.get(command);
                function = unProxy(function);
                if (function instanceof CommandWithAction) {
                    if (command.startsWith(subshell)) {
                        if (subshell.length() > 1 && command.length() > subshell.length()) {
                            command = command.substring(subshell.length() + 1);
                        }
                        try {
                            completers.add(new ArgumentCompleter(session, (CommandWithAction) function, command));
                            completers.add(new CommandNamesCompleter(session));
                        } catch (Throwable t) {
                            LOGGER.debug("Unable to create completers for command '" + command + "'", t);
                        }
                    }
                }
                commands.add(command);
            }
        }
    }

    /**
     * Get the aliases defined in the console session.
     *
     * @return the aliases set
     */
    @SuppressWarnings("unchecked")
    private Set<String> getAliases() {
        Set<String> vars = ((Set<String>) session.get(null));
        Set<String> aliases = new HashSet<String>();
        for (String var : vars) {
            Object content = session.get(var);
            if (content instanceof Closure)  {
                aliases.add(var);
            }
        }
        return aliases;
    }

    protected Function unProxy(Function function) {
        if (function instanceof CommandProxy) {
            CommandProxy proxy = (CommandProxy) function;
            Object target = proxy.getTarget();
            Function result;
            if (target instanceof Function) {
                result = (Function) target;
            } else {
                result = function;
            }
            proxy.ungetTarget();
            return result;
        } else {
            return function;
        }
    }

}

