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

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.osgi.service.command.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Like the {@link org.apache.karaf.shell.console.completer.CommandsCompleter} but does not use OSGi but is
 * instead used from the non-OSGi {@link org.apache.karaf.shell.console.Main}
 */
public class SimpleCommandsCompleter implements Completer {

    private final Map<Command, Completer> completers = new ConcurrentHashMap<Command, Completer>();
    private String application;

    public SimpleCommandsCompleter(String application) {
        this.application = application;
    }


    public void addCommand(Command command, Function function) {
        Set<String> functions = getNames(command);
        if (functions != null) {
            List<Completer> cl = new ArrayList<Completer>();
            cl.add(new StringsCompleter(functions));
            if (function instanceof CompletableFunction) {
                List<Completer> fcl = ((CompletableFunction) function).getCompleters();
                if (fcl != null) {
                    for (Completer c : fcl) {
                        cl.add(c == null ? NullCompleter.INSTANCE : c);
                    }
                } else {
                    cl.add(NullCompleter.INSTANCE);
                }
            } else {
                cl.add(NullCompleter.INSTANCE);
            }
            ArgumentCompleter c = new ArgumentCompleter(cl);
            completers.put(command, c);
        }
    }

    public void removeCommand(Command cmd) {
        if (cmd != null) {
            completers.remove(cmd);
        }
    }

    private Set<String> getNames(Command command) {
        Set<String> names = new HashSet<String>();
        Object scope = command.scope();
        String function = command.name();
        if (function != null) {
            if (scope == null || scope.equals(application)) {
                names.add(function);
                return names;
            } else {
                names.add(scope + ":" + function);
                return names;
            }
        }
        return null;
    }

    public int complete(String buffer, int cursor, List<String> candidates) {
        int res = new AggregateCompleter(completers.values()).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }
}

