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
package org.apache.felix.karaf.shell.commands;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.karaf.shell.console.OsgiCommandSupport;
import org.osgi.service.command.Function;

/**
 * Execute a closure on a list of arguments.
 */
@Command(scope = "shell", name = "if", description = "If/Then/Else block.")
public class IfAction extends OsgiCommandSupport {

    @Argument(name = "condition", index = 0, multiValued = false, required = true, description = "The condition")
    Function condition;

    @Argument(name = "ifTrue", index = 1, multiValued = false, required = true, description = "The function to execute if the condition is true")
    Function ifTrue;

    @Argument(name = "ifFalse", index = 2, multiValued = false, required = false, description = "The function to execute if the condition is false")
    Function ifFalse;

    @Override
    protected Object doExecute() throws Exception {
        Object result = condition.execute(session, null);
        if (isTrue(result)) {
            return ifTrue.execute(session, null);
        } else {
            if (ifFalse != null) {
                return ifFalse.execute(session, null);
            }
        }
        return null;
    }

    private boolean isTrue(Object result) {
        if (result == null) {
            return false;
        }
        if (result instanceof String && ((String) result).equals("")) {
            return false;
        }
        if (result instanceof Boolean) {
            return ((Boolean) result).booleanValue();
        }
        return true;
    }

}