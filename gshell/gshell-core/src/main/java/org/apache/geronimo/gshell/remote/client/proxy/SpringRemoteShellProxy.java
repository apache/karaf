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

package org.apache.geronimo.gshell.remote.client.proxy;

import java.util.concurrent.atomic.AtomicReference;

import jline.Terminal;
import org.apache.geronimo.gshell.ExitNotification;
import org.apache.geronimo.gshell.ErrorNotification;
import org.apache.geronimo.gshell.ansi.Renderer;
import org.apache.geronimo.gshell.command.IO;
import org.apache.geronimo.gshell.console.Console;
import org.apache.geronimo.gshell.console.JLineConsole;
import org.apache.geronimo.gshell.remote.RemoteShell;
import org.apache.geronimo.gshell.remote.client.RshClient;
import org.apache.geronimo.gshell.remote.client.proxy.RemoteBrandingProxy;
import org.apache.geronimo.gshell.remote.client.proxy.RemoteShellInfoProxy;
import org.apache.geronimo.gshell.remote.client.proxy.RemoteHistoryProxy;
import org.apache.geronimo.gshell.remote.client.proxy.RemoteEnvironmentProxy;
import org.apache.geronimo.gshell.shell.Environment;
import org.apache.geronimo.gshell.shell.InteractiveShell;
import org.apache.geronimo.gshell.shell.ShellInfo;
import org.apache.geronimo.gshell.whisper.stream.StreamFeeder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a shell interface which will proxy to a remote shell instance.
 *
 * @version $Rev: 638824 $ $Date: 2008-03-19 14:30:40 +0100 (Wed, 19 Mar 2008) $
 */
public class SpringRemoteShellProxy
    implements RemoteShell, InteractiveShell
{
    private final Logger log = LoggerFactory.getLogger(getClass());

    private RshClient client;

    private IO io;

    private Terminal terminal;

    private StreamFeeder outputFeeder;

    private boolean opened;

    private RemoteEnvironmentProxy env;

    private RemoteShellInfoProxy shellInfo;

    private RemoteHistoryProxy history;

    private RemoteBrandingProxy branding;

    public SpringRemoteShellProxy(final RshClient client, final IO io, final Terminal terminal) throws Exception {
        assert client != null;
        assert io != null;
        assert terminal != null;

        this.client = client;
        this.io = io;
        this.terminal = terminal;

        //
        // TODO: send over some client-side details, like the terminal features, etc, as well, verbosity too)
        //       If any problem or denial occurs, throw an exception, once created the proxy is considered valid.
        //

        client.openShell();

        // Setup other proxies
        env = new RemoteEnvironmentProxy(client);
        shellInfo = new RemoteShellInfoProxy(client);
        history = new RemoteHistoryProxy(client);
        branding = new RemoteBrandingProxy(client);

        // Copy the client's input stream to our outputstream so users see command output
        outputFeeder = new StreamFeeder(client.getInputStream(), io.outputStream);
        outputFeeder.createThread().start();

        opened = true;
    }

    public Environment getEnvironment() {
        ensureOpened();

        return env;
    }

    public ShellInfo getShellInfo() {
        ensureOpened();

        return shellInfo;
    }

    private void ensureOpened() {
        if (!opened) {
            throw new IllegalStateException("Remote shell proxy has been closed");
        }
    }

    public boolean isOpened() {
        return opened;
    }

    public void close() {
        try {
            client.closeShell();
        }
        catch (Exception ignore) {}

        try {
            outputFeeder.close();
        }
        catch (Exception ignore) {}

        opened = false;
    }

    //
    // Command Execution
    //

    public Object execute(final String line) throws Exception {
        ensureOpened();

        return client.execute(line);
    }

    public Object execute(final Object... args) throws Exception {
        ensureOpened();

        return client.execute((Object[])args);
    }

    public Object execute(final String path, final Object[] args) throws Exception {
        ensureOpened();

        return client.execute(path, args);
    }

    public Object execute(Object[][] commands) throws Exception {
        ensureOpened();

        return client.execute(commands);
    }

    //
    // Interactive Shell
    //

    public void run(final Object... args) throws Exception {
        assert args != null;

        ensureOpened();

        log.debug("Starting interactive console; args: {}", args);

        //
        // TODO: We need a hook into the session state here so that we can abort the console muck when the session closes
        //

        //
        // TODO: Request server to load...
        //
        // loadUserScript(branding.getInteractiveScriptName());

        final AtomicReference<ExitNotification> exitNotifHolder = new AtomicReference<ExitNotification>();
        final AtomicReference<Object> lastResultHolder = new AtomicReference<Object>();

        Console.Executor executor = new Console.Executor() {
            public Result execute(final String line) throws Exception {
                assert line != null;

                try {
                    Object result = SpringRemoteShellProxy.this.execute(line);

                    lastResultHolder.set(result);
                }
                catch (ExitNotification n) {
                    exitNotifHolder.set(n);

                    return Result.STOP;
                }

                return Result.CONTINUE;
            }
        };

        JLineConsole console = new JLineConsole(executor, io, terminal);

        console.setPrompter(new Console.Prompter() {
            Renderer renderer = new Renderer();

            public String prompt() {
                //
                // TODO: Get the real details and use them...
                //

                String userName = "user"; // shellInfo.getUserName();
                String hostName = "remote"; // shellInfo.getLocalHost().getHostName();
                String path = "/";

                return renderer.render("@|bold " + userName + "|@" + hostName + ":@|bold " + path + "|> ");
            }
        });

        console.setErrorHandler(new Console.ErrorHandler() {
            public Result handleError(final Throwable error) {
                assert error != null;

                displayError(error);

                return Result.CONTINUE;
            }
        });

        //
        // TODO: What are we to do with history here?  Really should be history on the server...
        //

        /*
        // Hook up a nice history file (we gotta hold on to the history object at some point so the 'history' command can get to it)
        History history = new History();
        console.setHistory(history);
        console.setHistoryFile(new File(branding.getUserDirectory(), branding.getHistoryFileName()));
        */

        // Unless the user wants us to shut up, then display a nice welcome banner
        /*
        if (!io.isQuiet()) {
            io.out.println(branding.getWelcomeBanner());
        }
        */

        // Check if there are args, and run them and then enter interactive
        if (args.length != 0) {
            execute(args);
        }

        // And then spin up the console and go for a jog
        console.run();

        // If any exit notification occured while running, then puke it up
        ExitNotification n = exitNotifHolder.get();
        if (n != null) {
            throw n;
        }
    }

    private void displayError(final Throwable error) {
        assert error != null;

        // Decode any error notifications
        Throwable cause = error;
        if (error instanceof ErrorNotification) {
            cause = error.getCause();
        }

        // Spit out the terse reason why we've failed
        io.err.print("@|bold,red ERROR| ");
        io.err.print(cause.getClass().getSimpleName());
        io.err.println(": @|bold,red " + cause.getMessage() + "|");

        // Determine if the stack trace flag is set
        String stackTraceProperty = System.getProperty("gshell.show.stacktrace");
        boolean stackTraceFlag = false;
        if (stackTraceProperty != null) {
        	stackTraceFlag = stackTraceProperty.trim().equals("true");
        }

        if (io.isDebug()) {
            // If we have debug enabled then skip the fancy bits below, and log the full error, don't decode shit
            log.debug(error.toString(), error);
        }
        else if (io.isVerbose() || stackTraceFlag) {
            // Render a fancy ansi colored stack trace
            StackTraceElement[] trace = cause.getStackTrace();
            StringBuffer buff = new StringBuffer();

            for (StackTraceElement e : trace) {
                buff.append("        @|bold at| ").
                        append(e.getClassName()).
                        append(".").
                        append(e.getMethodName()).
                        append(" (@|bold ");

                buff.append(e.isNativeMethod() ? "Native Method" :
                            (e.getFileName() != null && e.getLineNumber() != -1 ? e.getFileName() + ":" + e.getLineNumber() :
                                (e.getFileName() != null ? e.getFileName() : "Unknown Source")));

                buff.append("|)");

                io.err.println(buff);

                buff.setLength(0);
            }
        }
    }
}