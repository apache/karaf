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

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "shell", name = "echo", description="Echoes or prints arguments to STDOUT.")
@Service
public class EchoAction implements Action
{
    @Option(name = "-n", aliases = {}, description = "Do not print the trailing newline character", required = false, multiValued = false)
    private boolean noTrailingNewline = false;

    @Argument(index = 0, name = "arguments", description="Arguments to display separated by whitespaces", required = false, multiValued = true)
    private List<String> args;

    @Override
    public Object execute() throws Exception {
        if (args != null) {
            boolean first = true;
            for (String arg : args) {
                if (first) {
                    first = false;
                } else {
                    System.out.print(" ");
                }
                System.out.print(arg);
            }
        }

        if (!noTrailingNewline) {
            System.out.println();
        }

        return null;
    }
}
