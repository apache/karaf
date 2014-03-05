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
@Command(scope = "shell", name = "each", description = "Execute a closure on a list of arguments.")
@Service
public class EachAction implements Action {

    @Argument(name = "values", index = 0, multiValued = false, required = true, description = "The collection of arguments to iterate on")
    Collection<Object> values;

    @Argument(name = "function", index = 1, multiValued = false, required = true, description = "The function to execute")
    Function function;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        for (Object v : values) {
            function.execute(session, Collections.singletonList(v));
        }
        return null;
    }
}
