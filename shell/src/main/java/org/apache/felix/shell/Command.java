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

/**
 * This interface is used to define commands for the Felix impl
 * service. Any bundle wishing to create commands for the
 * impl service simply needs to create a service object that
 * implements this interface and then register it with the OSGi
 * framework. The impl service automatically includes any
 * registered command services in its list of available commands.
**/
public interface Command
{
    /**
     * Returns the name of the command that is implemented by the
     * interface. The command name should not contain whitespace
     * and should also be unique.
     * @return the name of the command.
    **/
    public String getName();

    /**
     * Returns the usage string for the command. The usage string is
     * a short string that illustrates how to use the command on the
     * command line. This information is used when generating command
     * help information. An example usage string for the <tt>install</tt>
     * command is:
     * <pre>
     *     install <URL> [<URL> ...]
     * </pre>
     * @return the usage string for the command.
    **/
    public String getUsage();

    /**
     * Returns a short description of the command; this description
     * should be as short as possible. This information is used when
     * generating the command help information.
     * @return a short description of the command.
    **/
    public String getShortDescription();

    /**
     * Executes the command using the supplied command line, output
     * print stream, and error print stream.
     * @param line the complete command line, including the command name.
     * @param out the print stream to use for standard output.
     * @param err the print stream to use for standard error.
    **/
    public void execute(String line, PrintStream out, PrintStream err);
}
