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
package org.apache.felix.karaf.admin.command;

import org.apache.felix.gogo.commands.Option;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.karaf.admin.Instance;

/**
 * List available instances
 */
@Command(scope = "admin", name = "list", description = "List all existing container instances.")
public class ListCommand extends AdminCommandSupport {

    @Option(name = "-l", aliases = { "--location" }, description = "Displays the location of the container instances", required = false, multiValued = false)
    boolean location;

    protected Object doExecute() throws Exception {
        Instance[] instances = getAdminService().getInstances();
        if (location) {
            System.out.println("  Port   State       Pid  Location");
        } else {
            System.out.println("  Port   State       Pid  Name");
        }
        for (Instance instance : instances) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            String s = Integer.toString(instance.getPort());
            for (int i = s.length(); i < 5; i++) {
                sb.append(' ');
            }
            sb.append(s);
            sb.append("] [");
            String state = instance.getState();
            while (state.length() < "starting".length()) {
                state += " ";
            }
            sb.append(state);
            sb.append("] [");
            s = Integer.toString(instance.getPid());
            for (int i = s.length(); i < 5; i++) {
                sb.append(' ');
            }
            sb.append(s);
            sb.append("] ");
            if (location) {
                sb.append(instance.getLocation());
            } else {
                sb.append(instance.getName());
            }
            System.out.println(sb.toString());
        }
        return null;
    }

}
