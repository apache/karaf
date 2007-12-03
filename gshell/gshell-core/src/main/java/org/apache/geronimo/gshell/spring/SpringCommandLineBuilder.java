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
package org.apache.geronimo.gshell.spring;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.geronimo.gshell.CommandLine;
import org.apache.geronimo.gshell.CommandLineBuilder;
import org.apache.geronimo.gshell.ErrorNotification;
import org.apache.geronimo.gshell.ExecutingVisitor;
import org.apache.geronimo.gshell.command.CommandExecutor;
import org.apache.geronimo.gshell.parser.ASTCommandLine;
import org.apache.geronimo.gshell.parser.CommandLineParser;
import org.apache.geronimo.gshell.parser.ParseException;
import org.apache.geronimo.gshell.shell.Environment;

/**
 * A CommandLineBuilder that uses a single executor and environment, expecting
 * those to be proxies to some thread local instances.  Use setter injection to
 * avoid a circular dependency with the SpringCommandExecutor.
 */
public class SpringCommandLineBuilder implements CommandLineBuilder {

    private CommandLineParser parser = new CommandLineParser();
    private CommandExecutor executor;
    private Environment environment;

    public SpringCommandLineBuilder() {
    }

    public void setExecutor(CommandExecutor executor) {
        this.executor = executor;
    }

    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private ASTCommandLine parse(final String input) throws ParseException {
         assert input != null;

         Reader reader = new StringReader(input);
         ASTCommandLine cl;
         try {
             cl = parser.parse(reader);
         }
         finally {
             try {
                 reader.close();
             } catch (IOException e) {
                 // Ignore
             }
         }

         return cl;
     }

     public CommandLine create(final String commandLine) throws ParseException {
         assert commandLine != null;

         if (commandLine.trim().length() == 0) {
             throw new IllegalArgumentException("Command line is empty");
         }

         try {
             final ExecutingVisitor visitor = new ExecutingVisitor(executor, environment);
             final ASTCommandLine root = parse(commandLine);

             return new CommandLine() {
                 public Object execute() throws Exception {
                     return root.jjtAccept(visitor, null);
                 }
             };
         }
         catch (Exception e) {
             throw new ErrorNotification(e);
         }
     }

}
