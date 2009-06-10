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

package org.apache.geronimo.gshell.commands.ssh;

import org.apache.sshd.server.ShellFactory;
import jline.Completor;
import jline.History;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.commandline.CommandLineExecutor;
import org.apache.geronimo.gshell.console.Console;
import org.apache.geronimo.gshell.console.JLineConsole;
import org.apache.geronimo.gshell.console.completer.AggregateCompleter;
import org.apache.geronimo.gshell.io.Closer;
import org.apache.geronimo.gshell.io.IO;
import org.apache.geronimo.gshell.notification.ExitNotification;
import org.apache.geronimo.gshell.shell.ShellContext;
import org.apache.geronimo.gshell.shell.ShellContextHolder;
import org.apache.geronimo.gshell.registry.CommandResolver;
import org.apache.geronimo.gshell.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * SSHD {@link ShellFactory} which provides access to GShell.
 *
 * @version $Rev: 731517 $ $Date: 2009-01-05 11:25:19 +0100 (Mon, 05 Jan 2009) $
 */
public class ShellFactoryImpl
    implements ShellFactory
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Application application;

    private Console.Prompter prompter;

    private CommandLineExecutor executor;

    private History history;

    private List<Completor> completers;

    private Console.ErrorHandler errorHandler;

    public Console.Prompter getPrompter() {
        return prompter;
    }

    public void setPrompter(final Console.Prompter prompter) {
        this.prompter = prompter;
    }

    public CommandLineExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(final CommandLineExecutor executor) {
        this.executor = executor;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(final History history) {
        this.history = history;
    }

    public List<Completor> getCompleters() {
        return completers;
    }

    public void setCompleters(final List<Completor> completers) {
        this.completers = completers;
    }

    public Console.ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(final Console.ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Application getApplication() {
        return application;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public Shell createShell() {
        return new ShellImpl();
    }

    public class ShellImpl
        implements ShellFactory.Shell, org.apache.geronimo.gshell.shell.Shell, ShellContext, Runnable
    {
        private InputStream in;

        private OutputStream out;

        private OutputStream err;

        private ExitCallback callback;

        private IO io;

        private Variables variables;

        private boolean closed;

        public void setInputStream(final InputStream in) {
            this.in = in;
        }

        public void setOutputStream(final OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(final OutputStream err) {
            this.err = err;
        }

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void start(final Environment env) throws IOException {
            this.io = new IO(in, out, err, false);

            // Create variables, inheriting the application ones
            this.variables = new Variables(application.getVariables());
            // Set up additional env
            if (env != null) {
                for (Map.Entry<String,String> entry : env.getEnv().entrySet()) {
                    this.variables.set(entry.getKey(), entry.getValue());
                }
            }
            this.variables.set("gshell.prompt", application.getModel().getBranding().getPrompt());
            this.variables.set(CommandResolver.GROUP, "/");
            this.variables.set("gshell.username", env.getEnv().get("USER"));
            this.variables.set("gshell.hostname", application.getLocalHost());
            // HACK: Add history for the 'history' command, since its not part of the Shell intf it can't really access it
            this.variables.set("gshell.internal.history", getHistory(), true);
            new Thread(this).start();
        }

        public void destroy() {
            close();
        }

        public ShellContext getContext() {
            return this;
        }

        public Object execute(final String line) throws Exception {

            return executor.execute(getContext(), line);
        }

        public Object execute(final String command, final Object[] args) throws Exception {
            return executor.execute(getContext(), args);
        }

        public Object execute(final Object... args) throws Exception {
            return executor.execute(getContext(), args);
        }

        public boolean isOpened() {
            return !closed;
        }

        public void close() {
            if (!closed) {
                closed = true;
                Closer.close(in, out, err);
                callback.onExit(0);
            }
        }

        public boolean isInteractive() {
            return false;
        }

        public void run(final Object... args) throws Exception {
            Console.Executor executor = new Console.Executor()
            {
                public Result execute(final String line) throws Exception {
                    assert line != null;
                    try {
                        ShellImpl.this.execute(line);
                    }
                    catch (ExitNotification n) {
                        return Result.STOP;
                    }
                    return Result.CONTINUE;
                }
            };

            IO io = getContext().getIo();

            // Setup the console runner
            JLineConsole console = new JLineConsole(executor, io);
            console.setPrompter(getPrompter());
            console.setErrorHandler(getErrorHandler());
            console.setHistory(getHistory());

            if (completers != null) {
                // Have to use aggregate here to get the completion list to update properly
                console.addCompleter(new AggregateCompleter(completers));
            }
            
            console.run();
        }

        public org.apache.geronimo.gshell.shell.Shell getShell() {
            return this;
        }

        public IO getIo() {
            return io;
        }

        public Variables getVariables() {
            return variables;
        }

        public void run() {
            ShellContext ctx = ShellContextHolder.get(true);

            try {
                ShellContextHolder.set(getContext());
                run(new Object[0]);
            }
            catch (Exception e) {
                log.error("Unhandled failure: " + e, e);
            }
            finally {
                ShellContextHolder.set(ctx);
                close();
            }
        }
    }
}
