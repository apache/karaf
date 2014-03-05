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
package org.apache.karaf.jndi.command;

import org.apache.karaf.jndi.JndiService;
import org.apache.karaf.jndi.command.completers.ContextsCompleter;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

import java.util.List;

@Command(scope = "jndi", name = "contexts", description = "List the JNDI sub-contexts.")
@Service
public class ContextsCommand implements Action {

    @Argument(index = 0, name = "context", description = "The base JNDI context", required = false, multiValued = false)
    @Completion(ContextsCompleter.class)
    String context;

    @Reference
    JndiService jndiService;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();

        table.column("JNDI Sub-Context");

        List<String> contexts;
        if (context == null) {
            contexts = jndiService.contexts();
        } else {
            contexts = jndiService.contexts(context);
        }

        for (String c : contexts) {
            table.addRow().addContent(c);
        }

        table.print(System.out);

        return null;
    }

}
