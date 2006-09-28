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
package org.apache.felix.shell;

import java.io.PrintStream;

import org.osgi.framework.ServiceReference;

/**
 * This interface defines the Felix impl service. The impl service
 * is an extensible, user interface neutral impl for controlling and
 * interacting with the framework. In general, the impl service assumes that
 * it is operating in a command line fashion, i.e., it receives a
 * complete command line, parses it, and executes the corresponding
 * command, but graphical interfaces are also possible.
 * <p>
 * All commands in the impl service are actually implemented as OSGi
 * services; these services implement the <tt>Command</tt> service
 * interface. Any bundle can implement custom commands by creating
 * command services and registering them with the OSGi framework.
**/
public interface ShellService
{
    /**
     * Returns an array of command names available in the impl service.
     * @return an array of available command names or an empty array.
    **/
    public String[] getCommands();

    /**
     * Returns the usage string associated with the specified command name.
     * @param name the name of the target command.
     * @return the usage string of the specified command or null.
    **/
    public String getCommandUsage(String name);

    /**
     * Returns the description associated with the specified command name.
     * @param name the name of the target command.
     * @return the description of the specified command or null.
    **/
    public String getCommandDescription(String name);

    /**
     * Returns the service reference associated with the specified
     * command name.
     * @param name the name of the target command.
     * @return the description of the specified command or null.
    **/
    public ServiceReference getCommandReference(String name);

    /**
     *
     * This method executes the supplied command line using the
     * supplied output and error print stream. The assumption of
     * this method is that a command line will be typed by the user
     * (or perhaps constructed by a GUI) and passed into it for
     * execution. The command line is interpretted in a very
     * simplistic fashion; it takes the leading string of characters
     * terminated by a space character (not including it) and
     * assumes that this leading token is the command name. For an
     * example, consider the following command line:
     * </p>
     * <pre>
     *     update 3 http://www.foo.com/bar.jar
     * </pre>
     * <p>
     * This is interpretted as an <tt>update</tt> command; as a
     * result, the entire command line (include command name) is
     * passed into the <tt>execute()</tt> method of the command
     * service with the name <tt>update</tt> if one exists. If the
     * corresponding command service is not found, then an error
     * message is printed to the error print stream.
     * @param commandLine the command line to execute.
     * @param out the standard output print stream.
     * @param err the standard error print stream.
    **/
    public void executeCommand(
        String commandLine, PrintStream out, PrintStream err)
        throws Exception;
}