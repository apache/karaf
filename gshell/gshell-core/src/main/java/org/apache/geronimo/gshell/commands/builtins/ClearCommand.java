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

import java.io.PrintWriter;

import jline.ConsoleReader;
import jline.Terminal;
import org.apache.geronimo.gshell.ansi.ANSI;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.command.annotation.Requirement;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * Clear the terminal screen.
 *
 * @version $Rev: 595891 $ $Date: 2007-11-17 02:23:47 +0100 (Sat, 17 Nov 2007) $
 */
@CommandComponent(id="gshell-builtins:clear", description="Clear the terminal screen")
public class ClearCommand
    extends OsgiCommandSupport
{
    @Requirement
    private Terminal terminal;

    public ClearCommand() {
    }

    public ClearCommand(Terminal terminal) {
        this.terminal = terminal;
    }

    protected OsgiCommandSupport createCommand() throws Exception {
        return new ClearCommand(terminal);
    }

    protected Object doExecute() throws Exception {
        ConsoleReader reader = new ConsoleReader(io.inputStream, new PrintWriter(io.outputStream, true), /*bindings*/ null, terminal);

        if (!ANSI.isEnabled()) {
        	io.out.println("ANSI is not enabled.  The clear command is not functional");
        }
        else {
        	reader.clearScreen();
        	return SUCCESS;
        }

        return SUCCESS;
    }
}
