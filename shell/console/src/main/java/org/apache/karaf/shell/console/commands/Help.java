/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.shell.console.commands;

import org.apache.felix.gogo.commands.Action;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.completer.SimpleCommandsCompleter;
import org.osgi.service.command.CommandSession;

/**
 * Displays help on the available commands
 */
@Command(scope = "*", name = "help", description = "Displays this help or help about a command")
public class Help implements Action {
    @Argument(name = "command", required = false, description = "The command to get help for")
    private String command;
    private final SimpleCommandsCompleter completer;

    public Help(SimpleCommandsCompleter completer) {
        this.completer = completer;
    }

    public Object execute(CommandSession session) throws Exception {
        if (command == null) {
            return completer.printUsage(session);
        }
        else {
            return completer.printCommandUsage(session, command);
        }
    }
}
