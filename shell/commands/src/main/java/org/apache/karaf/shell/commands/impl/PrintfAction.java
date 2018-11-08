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
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "shell", name = "printf", description = "Formats and prints arguments.")
@Service
public class PrintfAction implements Action {

    @Argument(
            index = 0,
            name = "format",
            description = "The format pattern to use",
            required = true,
            multiValued = false)
    private String format;

    @Argument(
            index = 1,
            name = "arguments",
            description = "The arguments for the given format pattern",
            required = true,
            multiValued = true)
    private Collection<Object> arguments = null;

    @Override
    public Object execute() throws Exception {
        System.out.printf(format, arguments.toArray());
        return null;
    }
}
