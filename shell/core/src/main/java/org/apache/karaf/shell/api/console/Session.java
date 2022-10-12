/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.shell.api.console;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Path;

/**
 * A <code>Session</code> can be used to execute commands.
 *
 * The {@link org.apache.karaf.shell.api.console.Registry} associated
 * with this <code>Session</code> will contain: <ul>
 *     <li>{@link SessionFactory}</li>
 *     <li>{@link Command}s</li>
 *     <li>{@link Session}</li>
 *     <li>{@link Registry}</li>
 *     <li>{@link History}</li>
 *     <li>{@link Terminal}</li>
 * </ul>
 */
public interface Session extends Runnable, Closeable {

    //
    // Session properties
    //
    // Property names starting with "karaf." are reserved for karaf
    //

    String SCOPE = "SCOPE";
    String SUBSHELL = "SUBSHELL";

    String PRINT_STACK_TRACES = "karaf.printStackTraces";
    String LAST_EXCEPTION = "karaf.lastException";
    String IGNORE_INTERRUPTS = "karaf.ignoreInterrupts";
    String IS_LOCAL = "karaf.shell.local";
    String COMPLETION_MODE = "karaf.completionMode";
    String DISABLE_EOF_EXIT = "karaf.disableEofExit";
    String DISABLE_LOGOUT = "karaf.disableLogout";

    String COMPLETION_MODE_GLOBAL = "global";
    String COMPLETION_MODE_SUBSHELL = "subshell";
    String COMPLETION_MODE_FIRST = "first";

    String SCOPE_GLOBAL = "*";

    /**
     * Execute a program in this session.
     *
     * @param commandline the provided command line
     * @return the result of the execution
     * @throws Exception in case of execution failure.
     */
    Object execute(CharSequence commandline) throws Exception;

    /**
     * Get the value of a variable.
     *
     * @param name the key name in the session
     * @return the corresponding object
     */
    Object get(String name);

    /**
     * Set the value of a variable.
     *
     * @param name  Name of the variable.
     * @param value Value of the variable
     */
    void put(String name, Object value);

    /**
     * Return the input stream that is the first of the pipeline. This stream is
     * sometimes necessary to communicate directly to the end user. For example,
     * a "less" or "more" command needs direct input from the keyboard to
     * control the paging.
     *
     * @return InputStream used closest to the user or null if input is from a
     *         file.
     */
    InputStream getKeyboard();

    /**
     * Return the PrintStream for the console. This must always be the stream
     * "closest" to the user. This stream can be used to post messages that
     * bypass the piping. If the output is piped to a file, then the object
     * returned must be null.
     *
     * @return the console stream
     */
    PrintStream getConsole();

    /**
     * Prompt the user for a line.
     *
     * @param prompt the session prompt
     * @param mask the session mask
     * @return the corresponding line
     * @throws java.io.IOException in case of prompting failure
     */
    String readLine(String prompt, final Character mask) throws IOException;

    /**
     * Retrieve the {@link org.apache.karaf.shell.api.console.Terminal} associated
     * with this <code>Session</code> or <code>null</code> if this <code>Session</code>
     * is headless.
     *
     * @return the session terminal
     */
    Terminal getTerminal();

    /**
     * Retrieve the {@link org.apache.karaf.shell.api.console.History} associated
     * with this <code>Session</code> or <code>null</code> if this <code>Session</code>
     * is headless.
     *
     * @return the session history
     */
    History getHistory();

    /**
     * Retrieve the {@link org.apache.karaf.shell.api.console.Registry} associated
     * with this <code>Session</code>.
     *
     * @return the session registry
     */
    Registry getRegistry();

    /**
     * Retrieve the {@link org.apache.karaf.shell.api.console.SessionFactory} associated
     * with this <code>Session</code>.
     *
     * @return the session factory
     */
    SessionFactory getFactory();

    /**
     * Resolve a command name.  If the command name has no specified scope, the fully
     * qualified command name will be returned, depending on the scopes and current
     * subshell.
     *
     * @param name the command name
     * @return the full qualified command name
     */
    String resolveCommand(String name);

    Path currentDir();

    void currentDir(Path path);

    /**
     * Close this session. After the session is closed, it will throw
     * IllegalStateException when it is used.
     */
    void close();
}
