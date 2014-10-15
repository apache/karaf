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

import java.util.Collection;
import java.util.Collections;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Function;
import org.apache.karaf.shell.api.console.Session;

/**
 * Execute a closure on a list of arguments.
 */
@Command(scope = "shell", name = "while", description = "Loop while the condition is true.")
@Service
public class WhileAction implements Action {

    @Argument(name = "condition", index = 0, multiValued = false, required = true, description = "The condition of the loop")
    Function condition;

    @Argument(name = "function", index = 1, multiValued = false, required = true, description = "The function to execute")
    Function function;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        while (isTrue(condition.execute(session, null))) {
            function.execute(session, null);
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
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
        if (result instanceof Number) {
            return ((Number) result).doubleValue() != 0.0d;
        }
        if (result instanceof Boolean) {
            return (Boolean) result;
        }
        return true;
    }

}
