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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.felix.gogo.commands.basic.AbstractCommand;
import org.apache.felix.gogo.runtime.shell.CommandProxy;
import org.apache.felix.gogo.runtime.shell.CommandSessionImpl;
import org.apache.karaf.shell.console.CompletableFunction;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.command.CommandSession;
import org.osgi.service.command.Function;

/**
 * Like the {@link org.apache.karaf.shell.console.completer.CommandsCompleter} but does not use OSGi but is
 * instead used from the non-OSGi {@link org.apache.karaf.shell.console.Main}
 */
public class CommandsCompleter implements Completer {

    private CommandSession session;
    private final List<Completer> completers = new ArrayList<Completer>();
    private final Set<String> commands = new HashSet<String>();

    public CommandsCompleter(CommandSession session) {
        this.session = session;
    }


    public int complete(String buffer, int cursor, List<String> candidates) {
        checkData();
        int res = new AggregateCompleter(completers).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

    protected synchronized void checkData() {
        Set<String> names = (Set<String>) session.get(CommandSessionImpl.COMMANDS);
        if (!names.equals(commands)) {
            commands.clear();
            completers.clear();
            for (String command : names) {
                Function function = (Function) session.get(command);
                function = unProxy(function);
                if (function instanceof AbstractCommand) {
                    completers.add(new ArgumentCompleter(session, (AbstractCommand) function, command));
                }
                commands.add(command);
            }
        }
    }

    protected Function unProxy(Function function) {
        try {
            if (function instanceof CommandProxy) {
                Field contextField = function.getClass().getDeclaredField("context");
                Field referenceField = function.getClass().getDeclaredField("reference");
                contextField.setAccessible(true);
                referenceField.setAccessible(true);
                BundleContext context = (BundleContext) contextField.get(function);
                ServiceReference reference = (ServiceReference) referenceField.get(function);
                Object target = context.getService(reference);
                try {
                    if (target instanceof Function) {
                        function = (Function) target;
                    }
                } finally {
                    context.ungetService(reference);
                }
            }
        } catch (Throwable t) {
        }
        return function;
    }

}

