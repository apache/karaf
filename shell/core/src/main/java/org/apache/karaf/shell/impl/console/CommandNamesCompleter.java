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

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.api.console.Command;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

public class CommandNamesCompleter extends org.apache.karaf.shell.support.completers.CommandNamesCompleter {

    @Override
    public int complete(Session session, CommandLine commandLine, List<String> candidates) {
        // TODO: optimize
        List<Command> list = session.getRegistry().getCommands();
        Set<String> names = new HashSet<>();
        for (Command command : list) {
            names.add(command.getScope() + ":" + command.getName());
            names.add(command.getName());
        }
        int res = new StringsCompleter(names).complete(session, commandLine, candidates);
        Collections.sort(candidates);
        return res;
    }

}
