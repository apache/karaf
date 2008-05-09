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
package org.apache.geronimo.gshell.commands.builtins;

import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.clp.Option;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * A simple command to <em>echo</em> all given arguments to the commands standard output.
 *
 * @version $Rev: 593392 $ $Date: 2007-11-09 03:14:15 +0100 (Fri, 09 Nov 2007) $
 */
@CommandComponent(id="gshell-builtins:echo", description="Echo or print arguments to STDOUT")
public class EchoCommand
    extends OsgiCommandSupport
{
    @Option(name="-n", description="Do not print the trailing newline character")
    private boolean trailingNewline = true;

    @Argument(description="Arguments")
    private List<String> args;

    protected Object doExecute() throws Exception {
        if (args != null) {
            int c=0;

            for (String arg : args) {
                io.out.print(arg);
                if (++c + 1 < args.size()) {
                    io.out.print(" ");
                }
            }
        }

        if (trailingNewline) {
            io.out.println();
        }

        return SUCCESS;
    }
}