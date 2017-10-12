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
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.util.process.PumpStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Execute system processes.
 */
@Command(scope = "shell", name = "exec", description = "Executes system processes.")
@Service
public class ExecuteAction implements Action {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Argument(index = 0, name = "command", description = "Execution command with arguments", required = true, multiValued = true)
    private List<String> args;

    @Reference
    Session session;

    @Override
    public Object execute() throws Exception {
        ProcessBuilder builder = new ProcessBuilder(args)
                .directory(session.currentDir().toFile());

        org.apache.felix.service.command.Process cp = org.apache.felix.service.command.Process.Utils.current();

        String cmd = String.join(" ", args);
        PumpStreamHandler handler = new PumpStreamHandler(cp.in(), cp.out(), cp.err(), "Command '" + cmd + "'");

        log.debug("Executing: {}", cmd);
        Process p = builder.start();

        handler.attach(p);
        handler.start();

        log.debug("Waiting for process to exit...");
        
        int status = p.waitFor();

        log.debug("Process exited w/status: {}", status);

        handler.stop();

        return null;
    }

}
