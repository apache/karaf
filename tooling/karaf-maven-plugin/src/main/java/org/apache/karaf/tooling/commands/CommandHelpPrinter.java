/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.karaf.tooling.commands;

import java.io.PrintStream;
import java.util.Map;
import java.util.Set;

import org.apache.karaf.shell.api.action.Action;

public interface CommandHelpPrinter {

    /**
     * Print help for a single action to the out stream.
     * 
     * @param action The command {@link Action}.
     * @param out The stream where to print the help.
     * @param includeHelpOption True to include the help option in the doc, false else.
     */
    void printHelp(Action action, PrintStream out, boolean includeHelpOption);
    
    /**
     * Print the overview of all given commands to the out stream.
     * 
     * @param commands The {@link Map} of commands to consider in the overview.
     * @param out The stream where to write the overview.
     */
    void printOverview(Map<String, Set<String>> commands, PrintStream out);

}
