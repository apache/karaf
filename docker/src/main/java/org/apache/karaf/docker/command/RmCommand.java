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

import java.util.List;
import org.apache.karaf.docker.command.completers.ContainersNameCompleter;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;

@Command(scope = "docker", name = "rm", description = "Remove one or more containers")
@Service
public class RmCommand extends DockerCommandSupport {

    @Argument(
            index = 0,
            name = "container",
            description = "Name or ID of the containers to remove",
            required = true,
            multiValued = true)
    @Completion(ContainersNameCompleter.class)
    List<String> containers;

    @Option(
            name = "--removeVolumes",
            description = "Remove the container volumes",
            required = false,
            multiValued = true)
    boolean removeVolumes;

    @Option(
            name = "--force",
            description = "Force remove container",
            required = false,
            multiValued = true)
    boolean force;

    @Override
    public Object execute() throws Exception {
        for (String container : containers) {
            getDockerService().rm(container, removeVolumes, force, url);
        }
        return null;
    }
}
