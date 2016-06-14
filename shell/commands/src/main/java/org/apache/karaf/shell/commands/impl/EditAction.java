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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.api.console.Terminal;

@Command(scope = "shell", name = "edit", description = "Calls a text editor.")
@Service
public class EditAction implements Action {

    private final Pattern URL_PATTERN = Pattern.compile("[^: ]+:[^ ]+");

    @Argument(index = 0, name = "url", description = "The url of the resource to edit.", required = true, multiValued = true)
    private List<String> urls;

    @Reference
    Session session;

    @Reference
    Terminal terminal;

    @Override
    public Object execute() throws Exception {
        org.jline.terminal.Terminal terminal = (org.jline.terminal.Terminal) session.get(".jline.terminal");
        Path pwd = Paths.get(System.getProperty("karaf.home"));
        org.jline.builtins.Commands.nano(terminal, System.out, System.err, pwd,
                urls.toArray(new String[urls.size()]));
        return null;
    }
}
