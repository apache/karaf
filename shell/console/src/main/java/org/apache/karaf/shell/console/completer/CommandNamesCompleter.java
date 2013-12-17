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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.CommandSessionHolder;
import org.apache.karaf.shell.console.Completer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;

/**
 * Completes command names
 */
public class CommandNamesCompleter implements Completer {

    public static final String COMMANDS = ".commands";

    private CommandSession session;
    private final Set<String> commands = new CopyOnWriteArraySet<String>();

    public CommandNamesCompleter() {
        this(CommandSessionHolder.getSession());
    }

    public CommandNamesCompleter(CommandSession session) {
        this.session = session;

        try {
            new CommandTracker();
        } catch (Throwable t) {
            // Ignore in case we're not in OSGi
        }
    }


    public int complete(String buffer, int cursor, List<String> candidates) {
        if (session == null) {
            session = CommandSessionHolder.getSession();
        }
        checkData();
        int res = new StringsCompleter(commands).complete(buffer, cursor, candidates);
        Collections.sort(candidates);
        return res;
    }

    @SuppressWarnings("unchecked")
    protected void checkData() {
        if (commands.isEmpty()) {
            Set<String> names = new HashSet<String>((Set<String>) session.get(COMMANDS));
            for (String name : names) {
                commands.add(name);
                if (name.indexOf(':') > 0) {
                    commands.add(name.substring(0, name.indexOf(':')));
                }
            }
        }
    }

    private class CommandTracker {
        public CommandTracker() throws Exception {
            BundleContext context = FrameworkUtil.getBundle(getClass()).getBundleContext();
            if (context == null) {
                throw new IllegalStateException("Bundle is stopped");
            }
            ServiceListener listener = new ServiceListener() {
                public void serviceChanged(ServiceEvent event) {
                    commands.clear();
                }
            };
            context.addServiceListener(listener,
                    String.format("(&(%s=*)(%s=*))",
                            CommandProcessor.COMMAND_SCOPE,
                            CommandProcessor.COMMAND_FUNCTION));
        }
    }

}

