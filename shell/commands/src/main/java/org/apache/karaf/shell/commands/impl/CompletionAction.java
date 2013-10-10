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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.console.AbstractAction;
import org.apache.karaf.shell.console.SessionProperties;

/**
 * Command to change the completion mode while using the shell console.
 */
@Command(scope = "shell", name = "completion", description = "Display or change the completion mode on the current console session.")
public class CompletionAction extends AbstractAction {

    @Argument(index = 0, name = "mode", description = "The completion mode to set. The valid completion modes are: global, first, subshell.", required = false, multiValued = false)
    String mode;

    protected Object doExecute() throws Exception {
        if (mode == null) {
            System.out.println(session.get(SessionProperties.COMPLETION_MODE));
            return null;
        }

        if (!mode.equalsIgnoreCase("global") && !mode.equalsIgnoreCase("first") && !mode.equalsIgnoreCase("subshell")) {
            System.err.println("The completion mode is not correct. The valid modes are: global, first, subshell. See documentation for details.");
            return null;
        }

        mode = mode.toUpperCase();

        session.put(SessionProperties.COMPLETION_MODE, mode);
        return null;
    }

}
