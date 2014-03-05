/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.commands.impl;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Command to change the completion mode while using the shell console.
 */
@Command(scope = "shell", name = "completion", description = "Display or change the completion mode on the current console session.")
@Service
public class CompletionAction implements Action {

    @Argument(index = 0, name = "mode", description = "The completion mode to set. The valid completion modes are: global, first, subshell.", required = false, multiValued = false)
    @Completion(value = StringsCompleter.class, values = { "global", "first", "subshell" })
    String mode;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        if (mode == null) {
            System.out.println(session.get(Session.COMPLETION_MODE));
        } else if (!mode.equalsIgnoreCase("global") && !mode.equalsIgnoreCase("first") && !mode.equalsIgnoreCase("subshell")) {
            System.err.println("The completion mode is not correct. The valid modes are: global, first, subshell. See documentation for details.");
        } else {
            session.put(Session.COMPLETION_MODE, mode.toLowerCase());
        }
        return null;
    }

}
