/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.console;

import org.apache.karaf.shell.commands.Command;

/**
 * Exit from the current sub-shell and get back to the previous one.
 */
@Command(scope = "*", name = "exit", description = "Exit from the current shell")
public class ExitAction extends AbstractAction {
    
    public Object doExecute() throws Exception {
        // get the current sub-shell
        String currentSubShell = (String) session.get("SUBSHELL");
        if (!currentSubShell.isEmpty()) {
            if (currentSubShell.contains(":")) {
                int index = currentSubShell.lastIndexOf(":");
                session.put("SUBSHELL", currentSubShell.substring(0, index));
            } else {
                session.put("SUBSHELL", "");
            }
            String currentScope = (String) session.get("SCOPE");
            int index = currentScope.indexOf(":");
            session.put("SCOPE", currentScope.substring(index + 1));
        }
        return null;
    }
    
}
