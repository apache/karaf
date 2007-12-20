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
package org.apache.geronimo.gshell.commands.builtins;

import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.command.annotation.CommandComponent;
import org.apache.geronimo.gshell.layout.LayoutManager;
import org.apache.geronimo.gshell.support.OsgiCommandSupport;

/**
 * Exit the current shell.
 *
 * @version $Rev: 593392 $ $Date: 2007-11-09 03:14:15 +0100 (Fri, 09 Nov 2007) $
 */
@CommandComponent(id="gshell-builtins:exit", description="Exit the (sub)shell")
public class ExitCommand
    extends OsgiCommandSupport
{
    @Argument(description="System exit code")
    private int exitCode = 0;

    protected Object doExecute() throws Exception {
        if (context.getVariables().get(LayoutManager.CURRENT_NODE) != null)
        {
            log.info("Exiting subshell");
            Variables v = context.getVariables();
            while (v != null && v.get(LayoutManager.CURRENT_NODE) != null) {
                v.unset(LayoutManager.CURRENT_NODE);
                v = v.parent();
            }
            return SUCCESS;
        }
        else
        {
            log.info("Exiting w/code: {}", exitCode);

            //
            // DO NOT Call System.exit() !!!
            //

            throw new ExitNotification(exitCode);
        }
    }
}
