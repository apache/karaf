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

package org.apache.karaf.shell.groovy.commands;

import groovy.lang.GroovyShell;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

import java.io.File;

@Command(scope = "groovy", name = "exec-file", description = "Executes Groovy file")
@Service
public class ExecGroovyFileAction implements Action {

    @Argument(index = 0, name = "path", description = "Groovy script file path", required = true)
    private String filePath;

    @Override
    public Object execute() throws Exception {
        GroovyShell sh = new GroovyShell();
        System.out.println(sh.evaluate(new File(filePath)));
        return null;
    }
}
