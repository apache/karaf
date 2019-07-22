/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.karaf.tooling.client;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SortedCommand {

    @Test
    public void testSort() throws Exception {
        List<CommandDescriptor> commands = new ArrayList<>();
        CommandDescriptor command = new CommandDescriptor();
        command.setRank(2);
        command.setCommand("test2");
        commands.add(command);
        command = new CommandDescriptor();
        command.setRank(1);
        command.setCommand("test1");
        commands.add(command);

        // ranking the commands and scripts
        Comparator<CommandDescriptor> comparator = Comparator.comparingDouble(CommandDescriptor::getRank);
        SortedSet<CommandDescriptor> sortedCommands = new TreeSet<>(comparator);
        sortedCommands.addAll(commands);

        for (CommandDescriptor cmd : sortedCommands) {
            System.out.println("Rank: " + cmd.getRank());
            System.out.println("Command: " + cmd.getCommand());
            System.out.println();
        }
    }

}
