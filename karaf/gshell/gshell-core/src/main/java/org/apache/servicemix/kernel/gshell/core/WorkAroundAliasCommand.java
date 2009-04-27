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
package org.apache.servicemix.kernel.gshell.core;

import java.util.Iterator;
import java.util.List;

import org.apache.geronimo.gshell.clp.Argument;
import org.apache.geronimo.gshell.command.CommandAction;
import org.apache.geronimo.gshell.command.CommandContext;
import org.apache.geronimo.gshell.command.CommandResult;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.commandline.CommandLineExecutor;
import org.apache.geronimo.gshell.io.IO;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.shell.ShellContext;
import org.apache.geronimo.gshell.shell.ShellContextHolder;
import org.apache.geronimo.gshell.wisdom.command.AliasCommand;

public class WorkAroundAliasCommand extends AliasCommand {

    private final CommandLineExecutor executor;

    public WorkAroundAliasCommand(CommandLineExecutor executor) {
        super(executor);
        this.executor = executor;
        setAction(new AliasCommandAction());
    }

    @Override
    protected void prepareAction(final ShellContext context, final Object[] args) {
        // HACK: Reset state for proper appendArgs muck
        assert context != null;
        assert args != null;

        setAction(new AliasCommandAction());
        log.trace("Preparing action");

        IO io = context.getIo();
        CommandAction action = getAction();

        // Setup the command action
        try {
            // Process command line options/arguments
            processArguments(io, action, args);
        }
        catch (Exception e) {
            // Abort if preparation caused a failure
            throw new AbortExecutionNotification(new CommandResult.FailureResult(e));
        }
    }

    private class AliasCommandAction
        implements CommandAction
    {
        @Argument
        private List<String> appendArgs = null;

        public Object execute(final CommandContext context) throws Exception {
            assert context != null;

            StringBuilder buff = new StringBuilder();
            buff.append(getAlias());

            // If we have args to append, then do it
            if (appendArgs != null && !appendArgs.isEmpty()) {
                buff.append(" ");

                // Append args quoted as they have already been processed by the parser
                Iterator iter = appendArgs.iterator();
                while (iter.hasNext()) {
                    //
                    // HACK: Using double quote instead of single quote for now as the parser's handling of single quote is broken
                    //

                    buff.append('"').append(iter.next()).append('"');
                    if (iter.hasNext()) {
                        buff.append(" ");
                    }
                }
            }

            log.debug("Executing alias: {}", buff);

            final Shell shell = ShellContextHolder.get().getShell();
            ShellContext shellContext = new ShellContext() {
                public Shell getShell() {
                    return shell;
                }
                public IO getIo() {
                    return context.getIo();
                }
                public Variables getVariables() {
                    return context.getVariables();
                }
            };
            Object result = executor.execute(shellContext, buff.toString());

            log.debug("Alias result: {}", result);

            return result;
        }
    }
}
