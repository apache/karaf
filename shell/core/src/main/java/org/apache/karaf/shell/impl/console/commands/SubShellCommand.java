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
package org.apache.karaf.shell.impl.console.commands;

import java.io.PrintStream;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.impl.console.commands.help.HelpCommand;

public class SubShellCommand extends TopLevelCommand {

    private final String name;
    private final AtomicInteger references = new AtomicInteger();

    public SubShellCommand(String name) {
        this.name = name;
    }

    public void increment() {
        references.incrementAndGet();
    }

    public int decrement() {
        return references.decrementAndGet();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Enter the subshell";
    }

    @Override
    protected void doExecute(Session session) throws Exception {
        session.put(Session.SUBSHELL, name);
        session.put(Session.SCOPE, name + ":" + session.get(Session.SCOPE));
    }

    @Override
    protected void printHelp(Session session, PrintStream out) {
        try {
            new HelpCommand(session.getFactory()).execute(session, Collections.singletonList("shell|" + name));
        } catch (Exception e) {
            throw new RuntimeException("Unable to print subshell help", e);
        }
    }
}
