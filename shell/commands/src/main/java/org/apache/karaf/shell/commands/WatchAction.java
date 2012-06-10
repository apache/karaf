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
package org.apache.karaf.shell.commands;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.apache.felix.service.command.CommandProcessor;
import org.apache.felix.service.command.CommandSession;
import org.apache.karaf.shell.console.AbstractAction;

@Command(scope = "shell", name = "command-watch", description = "Watches & refreshes the output of a command")
public class WatchAction extends AbstractAction {

    @Option(name = "-n", aliases = {"--interval"}, description = "The interval between executions of the command in seconds", required = false, multiValued = false)
    private long interval = 1;

    @Argument(index = 0, name = "command", description = "The command to watch / refresh", required = true, multiValued = true)
    private String[] arguments;

    CommandProcessor commandProcessor;
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    @Override
    protected Object doExecute() throws Exception {
        if (arguments == null || arguments.length == 0) {
            System.err.println("Argument expected");
            return null;
        } else {
            StringBuilder command = new StringBuilder();
            for (String arg:arguments) {
                command.append(arg).append(" ");
            }
            WatchTask watchTask = new WatchTask(commandProcessor, command.toString().trim());
            executorService.scheduleAtFixedRate(watchTask, 0, interval, TimeUnit.SECONDS);
            try {
                session.getKeyboard().read();
                watchTask.abort();
            } finally {
                executorService.shutdownNow();
                watchTask.close();
                return null;
            }
        }
    }

    public class WatchTask implements Runnable {

        private final CommandProcessor processor;
        private final String command;

        CommandSession session;
        ByteArrayOutputStream byteArrayOutputStream = null;
        PrintStream printStream = null;
        boolean doDisplay = true;

        public WatchTask(CommandProcessor processor, String command) {
            this.processor = processor;
            this.command = command;
        }

        public void run() {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream();
                printStream = new PrintStream(byteArrayOutputStream);
                session = commandProcessor.createSession(null, printStream, printStream);
                String output = "";
                session.execute(command);
                output = byteArrayOutputStream.toString();
                //Make sure before displaying that this is not a forgotten long running task.
                if (doDisplay) {
                    System.out.print("\33[2J");
                    System.out.print("\33[1;1H");
                    System.out.print(output);
                    System.out.flush();
                }
                byteArrayOutputStream.close();
                session.close();
            } catch (Exception e) {
                //Ingore
            }
        }

        /**
         * Aborts the task that it currently running.
         * It will prevent it from displaying anything.
         */
        public void abort() {
           doDisplay = false;
        }

        /**
         * Closes the the {@CommandSession} and the streams for I/O.
         * @throws IOException
         */
        public void close() throws IOException {
            if (this.session != null) {
                this.session.close();
            }
            if (this.byteArrayOutputStream != null) {
                this.byteArrayOutputStream.close();
            }
            if (this.printStream != null) {
                printStream.close();
            }
        }
    }

    public CommandProcessor getCommandProcessor() {
        return commandProcessor;
    }

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        this.commandProcessor = commandProcessor;
    }
}
