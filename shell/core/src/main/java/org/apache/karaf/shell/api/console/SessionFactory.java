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
package org.apache.karaf.shell.api.console;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * The <code>SessionFactory</code> can be used to create
 * {@link Session} to execute commands.
 *
 * The {@link org.apache.karaf.shell.api.console.Registry} associated
 * with this <code>SessionFactory</code> will contain: <ul>
 *     <li>{@link SessionFactory}</li>
 *     <li>{@link Registry}</li>
 *     <li>{@link Command}s</li>
 * </ul>
 */
public interface SessionFactory {

    /**
     * Retrieve the {@link Registry} used by this <code>SessionFactory</code>.
     *
     * @return a registry built by the factory.
     */
    Registry getRegistry();

    /**
     * Create new interactive session.
     *
     * @param in the input stream, can be <code>null</code> if the session is only used to execute a command using {@link Session#execute(CharSequence)}
     * @param out the output stream
     * @param err the error stream
     * @param term the {@link Terminal} to use, may be <code>null</code>
     * @param encoding the encoding to use for the input stream, may be <code>null</code>
     * @param closeCallback a callback to be called when the session is closed, may be <code>null</code>
     * @return the new session
     */
    Session create(InputStream in, PrintStream out, PrintStream err, Terminal term, String encoding, Runnable closeCallback);

    /**
     * Create a new headless session.
     * Headless session can only be used to execute commands, so that
     * {@link org.apache.karaf.shell.api.console.Session#run()} can not be used.
     *
     * @param in the input stream, can be <code>null</code> if the session is only used to execute a command using {@link Session#execute(CharSequence)}
     * @param out the output stream
     * @param err the error stream
     * @return the new session
     */
    Session create(InputStream in, PrintStream out, PrintStream err);

    /**
     * Create a new headless session inheriting from the parent session.
     * Headless session can only be used to execute commands, so that
     * {@link org.apache.karaf.shell.api.console.Session#run()} can not be used.
     * All variables and the terminal properties from the parent session will be available.
     *
     * @param in the input stream, can be <code>null</code> if the session is only used to execute a command using {@link Session#execute(CharSequence)}
     * @param out the output stream
     * @param err the error stream
     * @param session the parent session
     * @return the new session
     */
    Session create(InputStream in, PrintStream out, PrintStream err, Session session);

}
