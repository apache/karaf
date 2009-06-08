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
package org.apache.felix.karaf.gshell.core.sshd;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.apache.sshd.server.CommandFactory;
import org.apache.geronimo.gshell.commandline.CommandLineExecutor;
import org.apache.geronimo.gshell.shell.ShellContext;
import org.apache.geronimo.gshell.shell.Shell;
import org.apache.geronimo.gshell.io.IO;
import org.apache.geronimo.gshell.io.Closer;
import org.apache.geronimo.gshell.command.Variables;

public class ShellCommandFactory implements CommandFactory {

    private CommandLineExecutor executor;

    public CommandLineExecutor getExecutor() {
        return executor;
    }

    public void setExecutor(CommandLineExecutor executor) {
        this.executor = executor;
    }

    public Command createCommand(String command) {
        return new ShellCommand(command);
    }

    public class ShellCommand implements Command, ShellContext, Shell {

        private String command;
        private InputStream in;
        private OutputStream out;
        private OutputStream err;
        private ExitCallback callback;
        private Variables var;
        private IO io;
        private boolean closed;

        public ShellCommand(String command) {
            this.command = command;
        }

        public Shell getShell() {
            return this;
        }

        public IO getIo() {
            if (io == null) {
                io = new IO(in, out, err, true);
            }
            return io;
        }

        public Variables getVariables() {
            if (var == null) {
                var = new Variables();
            }
            return var;
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

        public void setExitCallback(ExitCallback callback) {
            this.callback = callback;
        }

        public void start() throws IOException {
            try {
                try {
                    executor.execute(this, command);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } finally {
                callback.onExit(0);
            }
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
            closed = true;
            Closer.close(in, out, err);
            callback.onExit(0);
        }

        public boolean isInteractive() {
            return false;
        }

        public ShellContext getContext() {
            return this;
        }

        public void run(Object... args) throws Exception {
            throw new UnsupportedOperationException();
        }
    }

}
