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
package org.apache.karaf.docker.command;

import org.apache.karaf.docker.Container;
import org.apache.karaf.docker.Port;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;

@Command(scope = "docker", name = "ps", description = "List containers")
@Service
public class PsCommand extends DockerCommandSupport {

    @Option(name = "-a", aliases = {"--all", "--showAll" }, description = "Display all containers or only running ones", required = false, multiValued = false)
    boolean showAll;

    @Override
    public Object execute() throws Exception {
        ShellTable table = new ShellTable();
        table.column("Id");
        table.column("Names");
        table.column("Command");
        table.column("Created");
        table.column("Image");
        table.column("Image ID");
        table.column("Status");
        table.column("State");
        table.column("Ports");
        table.column("Size");
        table.column("Size Root");
        for (Container container : getDockerService().ps(showAll, url)) {
            StringBuilder portBuffer = new StringBuilder();
            for (Port port : container.getPorts()) {
                portBuffer.append(port.getType()).append(":").append(port.getPrivatePort()).append(":").append(port.getPublicPort()).append(" ");
            }
            table.addRow().addContent(
                    container.getId(),
                    container.getNames(),
                    container.getCommand(),
                    container.getCreated(),
                    container.getImage(),
                    container.getImageId(),
                    container.getStatus(),
                    container.getState(),
                    portBuffer.toString(),
                    container.getSizeRw(),
                    container.getSizeRootFs()
            );
        }
        table.print(System.out);
        return null;
    }

}
