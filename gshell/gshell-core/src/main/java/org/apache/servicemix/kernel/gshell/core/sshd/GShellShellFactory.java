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
package org.apache.servicemix.kernel.gshell.core.sshd;

import java.util.Map;
import java.util.List;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.Closeable;
import java.io.IOException;

import com.google.code.sshd.server.ShellFactory;
import org.apache.geronimo.gshell.shell.ShellContext;
import org.apache.geronimo.gshell.shell.ShellContextHolder;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.io.IO;
import org.apache.geronimo.gshell.command.Variables;
import org.apache.geronimo.gshell.console.Console;
import org.apache.geronimo.gshell.console.JLineConsole;
import org.apache.geronimo.gshell.console.completer.AggregateCompleter;
import org.apache.geronimo.gshell.notification.ExitNotification;
import org.apache.geronimo.gshell.notification.ErrorNotification;
import org.apache.geronimo.gshell.application.model.Branding;
import org.apache.geronimo.gshell.commandline.CommandLineExecutor;
import org.apache.geronimo.gshell.ansi.AnsiRenderer;
import org.apache.geronimo.gshell.wisdom.shell.ConsoleErrorHandlerImpl;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import jline.History;
import jline.Completor;

public class GShellShellFactory implements ShellFactory {

    private Logger logger = LoggerFactory.getLogger(getClass());
    private Branding branding;
    private Console.Prompter prompter;
    private CommandLineExecutor executor;
    private History history;
    private List<Completor> completers;
    private Console.ErrorHandler errorHandler;

    public Branding getBranding() {
        return branding;
    }

    public void setBranding(Branding branding) {
        this.branding = branding;
    }

    public Console.Prompter getPrompter() {
        return prompter;
    }

    public void setPrompter(Console.Prompter prompter) {
        this.prompter = prompter;
    }

    public CommandLineExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(CommandLineExecutor executor) {
        this.executor = executor;
    }

    public History getHistory() {
        return history;
    }

    public void setHistory(History history) {
        this.history = history;
    }

    public List<Completor> getCompleters() {
        return completers;
    }

    public void setCompleters(List<Completor> completers) {
        this.completers = completers;
    }

    public Console.ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public void setErrorHandler(Console.ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    public Shell createShell() {
        return new ShellImpl();
    }

    public class ShellImpl implements ShellFactory.DirectShell, org.apache.geronimo.gshell.shell.Shell, ShellContext, Runnable {

        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private IO io;
        private Variables variables;
        private boolean closed;

        public ShellImpl() {
        }

        public void setInputStream(InputStream in) {
            this.in = in;
        }

        public void setOutputStream(OutputStream out) {
            this.out = out;
        }

        public void setErrorStream(OutputStream err) {
            this.err = err;
        }

        public void start(Map<String,String> env) throws Exception {
            this.io = new IO(in, out, err, false);
            this.variables = new Variables((Map) env);
            new Thread(this).start();
        }

        public boolean isAlive() {
            return !closed;
        }

        public int exitValue() {
            if (!closed) {
                throw new IllegalThreadStateException();
            }
            return 0;
        }

        public void destroy() {
            close();
        }

        public ShellContext getContext() {
            return this;
        }

        public Object execute(String line) throws Exception {

            return executor.execute(getContext(), line);
        }

        public Object execute(String command, Object[] args) throws Exception {
            return executor.execute(getContext(), args);
        }

        public Object execute(Object... args) throws Exception {
            return executor.execute(getContext(), args);
        }

        public boolean isOpened() {
            return !closed;
        }

        public void close() {
            closed = true;
            close(in);
            close(out);
            close(err);
        }

        public boolean isInteractive() {
            return false;
        }

        public void run(Object... args) throws Exception {
            Console.Executor executor = new Console.Executor() {
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
            } catch (Exception e) {
                e.printStackTrace();
                // TODO: do something about this exception
            } finally {
                ShellContextHolder.set(ctx);
                close();
            }
        }

        private void close(Closeable c) {
            try {
                c.close();
            } catch (IOException e) {
                // Ignore
            }
        }

    }

}
