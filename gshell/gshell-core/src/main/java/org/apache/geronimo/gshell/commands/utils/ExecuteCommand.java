/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.geronimo.gshell.commands.utils;

import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.common.io.PumpStreamHandler;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * Execute system processes.
 *
 * @version $Rev: 593392 $ $Date: 2007-11-09 03:14:15 +0100 (Fri, 09 Nov 2007) $
 */
@CommandComponent(id="utils:exec", description="Execute system processes")
public class ExecuteCommand extends OsgiCommandSupport
{
    private ProcessBuilder builder;

    @Argument(description="Argument", required=true)
    private List<String> args;

    protected Object doExecute() throws Exception {
        assert builder != null;

        log.info("Executing: {}", builder.command());

        Process p = builder.start();

        PumpStreamHandler handler = new PumpStreamHandler(io.inputStream, io.outputStream, io.errorStream);
        handler.attach(p);
        handler.start();

        log.debug("Waiting for process to exit...");

        int status = p.waitFor();


        log.info("Process exited w/status: {}", status);

        handler.stop();

        return status;
    }
}
